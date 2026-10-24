package com.steamcraft.net;

import com.codedisaster.steamworks.*;
import com.steamcraft.steam.SteamManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class SteamHostBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("SteamCraft/HostBridge");
    private static final int CHANNEL_DATA = 0;
    private static final int CHANNEL_CONTROL = 1;
    private static final byte CONTROL_DISCONNECT = 1;

    private static SteamHostBridge instance;

    private int lanPort = -1;
    private boolean hosting = false;
    private SteamID lobbyId;
    private Thread hostLoopThread;
    private final Map<Long, ClientSession> clientSessions = new ConcurrentHashMap<>();

    public static synchronized SteamHostBridge getInstance() {
        if (instance == null) {
            instance = new SteamHostBridge();
        }
        return instance;
    }

    private SteamHostBridge() {
    }

    public synchronized void startHosting(int localPort) {
        if (!SteamManager.getInstance().isInitialized()) {
            LOGGER.warn("Cannot start Steam host bridge: Steamworks is not initialized!");
            return;
        }

        this.lanPort = localPort;
        this.hosting = true;
        LOGGER.info("Starting Steam host bridge on LAN port {}...", localPort);

        // Create friends-only Steam lobby
        SteamMatchmaking matchmaking = SteamManager.getInstance().getMatchmaking();
        if (matchmaking != null) {
            matchmaking.createLobby(SteamMatchmaking.LobbyType.FriendsOnly, 8);
        }

        // Start P2P listener thread
        if (hostLoopThread != null && hostLoopThread.isAlive()) {
            hostLoopThread.interrupt();
        }
        hostLoopThread = new Thread(this::runHostP2PLoop, "SteamHostBridge-P2PListener");
        hostLoopThread.setDaemon(true);
        hostLoopThread.start();
    }

    public void onLobbyCreated(SteamID newLobbyId) {
        this.lobbyId = newLobbyId;
        SteamMatchmaking matchmaking = SteamManager.getInstance().getMatchmaking();
        if (matchmaking != null && newLobbyId != null && newLobbyId.isValid()) {
            String playerName = "Server";
            try {
                if (SteamManager.getInstance().getFriends() != null) {
                    playerName = SteamManager.getInstance().getFriends().getPersonaName();
                }
            } catch (Throwable ignored) {
            }

            matchmaking.setLobbyData(newLobbyId, "game", "MinecraftSteamCraft");
            matchmaking.setLobbyData(newLobbyId, "name", playerName + "'s World");
            matchmaking.setLobbyData(newLobbyId, "version", "1.21.1");
            matchmaking.setLobbyJoinable(newLobbyId, true);
            LOGGER.info("Steam Lobby published: '{}' (ID: {})", playerName + "'s World", newLobbyId);
        }
    }

    private void runHostP2PLoop() {
        SteamNetworking networking = SteamManager.getInstance().getNetworking();
        int[] msgSize = new int[1];
        ByteBuffer directBuf = ByteBuffer.allocateDirect(65536);
        SteamID remoteSender = new SteamID();

        while (hosting && !Thread.currentThread().isInterrupted()) {
            boolean hadActivity = false;

            try {
                // Check Data Channel (0)
                while (networking != null && networking.isP2PPacketAvailable(CHANNEL_DATA, msgSize)) {
                    hadActivity = true;
                    directBuf.clear();
                    int bytesRead = networking.readP2PPacket(remoteSender, directBuf, CHANNEL_DATA);
                    if (bytesRead > 0) {
                        directBuf.limit(bytesRead);
                        directBuf.position(0);

                        byte[] data = new byte[bytesRead];
                        directBuf.get(data);

                        long clientHandle = SteamNativeHandle.getNativeHandle(remoteSender);
                        ClientSession session = clientSessions.computeIfAbsent(clientHandle, handle -> {
                            LOGGER.info("New client connecting via Steam P2P: {}", remoteSender);
                            ClientSession s = new ClientSession(SteamID.createFromNativeHandle(handle), lanPort);
                            s.start();
                            return s;
                        });

                        session.sendToMinecraft(data);
                    }
                }

                // Check Control Channel (1)
                while (networking != null && networking.isP2PPacketAvailable(CHANNEL_CONTROL, msgSize)) {
                    hadActivity = true;
                    directBuf.clear();
                    int bytesRead = networking.readP2PPacket(remoteSender, directBuf, CHANNEL_CONTROL);
                    if (bytesRead > 0) {
                        byte ctrl = directBuf.get(0);
                        if (ctrl == CONTROL_DISCONNECT) {
                            long clientHandle = SteamNativeHandle.getNativeHandle(remoteSender);
                            ClientSession s = clientSessions.remove(clientHandle);
                            if (s != null) {
                                LOGGER.info("Client disconnected from Steam: {}", remoteSender);
                                s.close();
                            }
                        }
                    }
                }

            } catch (Throwable t) {
                LOGGER.error("Error in host P2P loop", t);
            }

            if (!hadActivity) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    public synchronized void stopHosting() {
        if (!hosting) return;
        this.hosting = false;
        LOGGER.info("Stopping Steam host bridge...");

        if (hostLoopThread != null) {
            hostLoopThread.interrupt();
            hostLoopThread = null;
        }

        // Close all client sessions
        for (ClientSession session : clientSessions.values()) {
            session.close();
        }
        clientSessions.clear();

        // Leave Steam Lobby
        if (lobbyId != null && SteamManager.getInstance().getMatchmaking() != null) {
            SteamManager.getInstance().getMatchmaking().leaveLobby(lobbyId);
            lobbyId = null;
            SteamManager.getInstance().setCurrentLobby(null);
        }
    }

    public boolean isHosting() {
        return hosting;
    }

    public SteamID getLobbyId() {
        return lobbyId;
    }

    private class ClientSession {
        private final SteamID clientSteamId;
        private final int port;
        private final AtomicBoolean running = new AtomicBoolean(true);
        private Socket socket;
        private OutputStream out;
        private Thread s2cThread;

        public ClientSession(SteamID clientSteamId, int port) {
            this.clientSteamId = clientSteamId;
            this.port = port;
        }

        public void start() {
            try {
                this.socket = new Socket("127.0.0.1", port);
                this.socket.setTcpNoDelay(true);
                this.out = socket.getOutputStream();

                // Thread reading from Minecraft Server -> sending to Steam Client
                this.s2cThread = new Thread(this::readFromMinecraftLoop, "SteamHost-MCReader-" + clientSteamId);
                this.s2cThread.setDaemon(true);
                this.s2cThread.start();
            } catch (Exception e) {
                LOGGER.error("Failed to connect local socket for client {}", clientSteamId, e);
                close();
            }
        }

        public void sendToMinecraft(byte[] data) {
            if (!running.get() || out == null) return;
            try {
                out.write(data);
                out.flush();
            } catch (Exception e) {
                LOGGER.warn("Failed to write to Minecraft server for client {}: {}", clientSteamId, e.getMessage());
                close();
            }
        }

        private void readFromMinecraftLoop() {
            byte[] buffer = new byte[16384];
            ByteBuffer directBuf = ByteBuffer.allocateDirect(16384);
            SteamNetworking networking = SteamManager.getInstance().getNetworking();

            try (InputStream in = socket.getInputStream()) {
                int read;
                while (running.get() && (read = in.read(buffer)) != -1) {
                    if (networking != null) {
                        directBuf.clear();
                        directBuf.put(buffer, 0, read);
                        directBuf.flip();
                        networking.sendP2PPacket(clientSteamId, directBuf, SteamNetworking.P2PSend.Reliable, CHANNEL_DATA);
                    }
                }
            } catch (Exception e) {
                if (running.get()) {
                    LOGGER.info("Connection closed by local Minecraft server for client {}", clientSteamId);
                }
            } finally {
                // Send disconnect signal to client
                if (networking != null && running.get()) {
                    try {
                        directBuf.clear();
                        directBuf.put(CONTROL_DISCONNECT);
                        directBuf.flip();
                        networking.sendP2PPacket(clientSteamId, directBuf, SteamNetworking.P2PSend.Reliable, CHANNEL_CONTROL);
                    } catch (Throwable ignored) {
                    }
                }
                close();
            }
        }

        public void close() {
            if (running.compareAndSet(true, false)) {
                long handle = SteamNativeHandle.getNativeHandle(clientSteamId);
                clientSessions.remove(handle);
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (Exception ignored) {
                }
                if (s2cThread != null) {
                    s2cThread.interrupt();
                }
            }
        }
    }
}
