package com.steamcraft.net;

import com.codedisaster.steamworks.*;
import com.steamcraft.steam.SteamManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class SteamClientBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("SteamCraft/ClientBridge");
    private static final int CHANNEL_DATA = 0;
    private static final int CHANNEL_CONTROL = 1;
    private static final byte CONTROL_DISCONNECT = 1;

    private static SteamClientBridge instance;

    private final AtomicBoolean connectingOrConnected = new AtomicBoolean(false);
    private ServerSocket proxyServer;
    private Socket clientSocket;
    private SteamID currentHostSteamId;
    private Thread proxyAcceptThread;
    private Thread p2pReceiveThread;

    public static synchronized SteamClientBridge getInstance() {
        if (instance == null) {
            instance = new SteamClientBridge();
        }
        return instance;
    }

    private SteamClientBridge() {
    }

    public void joinLobby(SteamID lobbyId) {
        if (!SteamManager.getInstance().isInitialized()) {
            LOGGER.warn("Cannot join Steam lobby: Steamworks is not initialized!");
            return;
        }

        SteamMatchmaking matchmaking = SteamManager.getInstance().getMatchmaking();
        if (matchmaking != null) {
            LOGGER.info("Joining Steam lobby {}...", lobbyId);
            matchmaking.joinLobby(lobbyId);
        }
    }

    public void onLobbyEntered(SteamID lobbyId) {
        SteamMatchmaking matchmaking = SteamManager.getInstance().getMatchmaking();
        if (matchmaking == null) return;

        SteamID hostSteamId = matchmaking.getLobbyOwner(lobbyId);
        String lobbyName = matchmaking.getLobbyData(lobbyId, "name");
        if (lobbyName == null || lobbyName.isEmpty()) {
            lobbyName = "Steam Host";
        }

        LOGGER.info("Entered lobby {}. Host SteamID: {}, Name: '{}'", lobbyId, hostSteamId, lobbyName);
        connectToHost(hostSteamId, lobbyName);
    }

    public synchronized void connectToHost(SteamID hostSteamId, String hostDisplayName) {
        disconnect();

        this.currentHostSteamId = hostSteamId;
        this.connectingOrConnected.set(true);

        SteamNetworking networking = SteamManager.getInstance().getNetworking();
        if (networking != null) {
            networking.acceptP2PSessionWithUser(hostSteamId);
            networking.allowP2PPacketRelay(true);
        }

        try {
            // Bind to a free local loopback port
            this.proxyServer = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
            int localPort = proxyServer.getLocalPort();
            LOGGER.info("Started local client loopback proxy on port {}", localPort);

            // Start proxy acceptor thread
            this.proxyAcceptThread = new Thread(() -> runProxyAcceptLoop(hostSteamId), "SteamClient-ProxyAcceptor");
            this.proxyAcceptThread.setDaemon(true);
            this.proxyAcceptThread.start();

            // Tell Minecraft to connect to local port
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                Screen currentScreen = mc.screen;
                ServerAddress address = new ServerAddress("127.0.0.1", localPort);
                ServerData serverData = new ServerData(hostDisplayName, "127.0.0.1:" + localPort, ServerData.Type.LAN);
                LOGGER.info("Instructing Minecraft client to connect to 127.0.0.1:{}", localPort);
                ConnectScreen.startConnecting(currentScreen, mc, address, serverData, false, null);
            });

        } catch (Exception e) {
            LOGGER.error("Failed to start local client bridge proxy", e);
            disconnect();
        }
    }

    private void runProxyAcceptLoop(SteamID hostSteamId) {
        try {
            this.clientSocket = proxyServer.accept();
            this.clientSocket.setTcpNoDelay(true);
            LOGGER.info("Minecraft client successfully connected to local Steam bridge proxy!");

            // Thread 1: Steam P2P -> Minecraft Socket
            this.p2pReceiveThread = new Thread(() -> runSteamToSocketLoop(hostSteamId), "SteamClient-P2PReceiver");
            this.p2pReceiveThread.setDaemon(true);
            this.p2pReceiveThread.start();

            // Thread 2 (Current thread): Minecraft Socket -> Steam P2P
            runSocketToSteamLoop(hostSteamId);

        } catch (Exception e) {
            if (connectingOrConnected.get()) {
                LOGGER.error("Error in proxy accept loop", e);
            }
        } finally {
            disconnect();
        }
    }

    private void runSocketToSteamLoop(SteamID hostSteamId) {
        byte[] buffer = new byte[16384];
        ByteBuffer directBuf = ByteBuffer.allocateDirect(16384);
        SteamNetworking networking = SteamManager.getInstance().getNetworking();

        try (InputStream in = clientSocket.getInputStream()) {
            int read;
            while (connectingOrConnected.get() && (read = in.read(buffer)) != -1) {
                if (networking != null) {
                    directBuf.clear();
                    directBuf.put(buffer, 0, read);
                    directBuf.flip();
                    networking.sendP2PPacket(hostSteamId, directBuf, SteamNetworking.P2PSend.Reliable, CHANNEL_DATA);
                }
            }
        } catch (Exception e) {
            if (connectingOrConnected.get()) {
                LOGGER.info("Minecraft connection closed by client: {}", e.getMessage());
            }
        } finally {
            // Notify host of disconnect
            if (networking != null && connectingOrConnected.get()) {
                try {
                    directBuf.clear();
                    directBuf.put(CONTROL_DISCONNECT);
                    directBuf.flip();
                    networking.sendP2PPacket(hostSteamId, directBuf, SteamNetworking.P2PSend.Reliable, CHANNEL_CONTROL);
                } catch (Throwable ignored) {
                }
            }
            disconnect();
        }
    }

    private void runSteamToSocketLoop(SteamID hostSteamId) {
        int[] msgSize = new int[1];
        ByteBuffer directBuf = ByteBuffer.allocateDirect(65536);
        SteamID remoteSender = new SteamID();
        SteamNetworking networking = SteamManager.getInstance().getNetworking();

        try {
            OutputStream out = clientSocket.getOutputStream();

            while (connectingOrConnected.get() && !Thread.currentThread().isInterrupted()) {
                boolean hadActivity = false;

                // Data channel
                while (networking != null && networking.isP2PPacketAvailable(CHANNEL_DATA, msgSize)) {
                    hadActivity = true;
                    directBuf.clear();
                    int bytesRead = networking.readP2PPacket(remoteSender, directBuf, CHANNEL_DATA);
                    if (bytesRead > 0) {
                        directBuf.limit(bytesRead);
                        directBuf.position(0);

                        byte[] data = new byte[bytesRead];
                        directBuf.get(data);
                        out.write(data);
                        out.flush();
                    }
                }

                // Control channel
                while (networking != null && networking.isP2PPacketAvailable(CHANNEL_CONTROL, msgSize)) {
                    hadActivity = true;
                    directBuf.clear();
                    int bytesRead = networking.readP2PPacket(remoteSender, directBuf, CHANNEL_CONTROL);
                    if (bytesRead > 0) {
                        byte ctrl = directBuf.get(0);
                        if (ctrl == CONTROL_DISCONNECT) {
                            LOGGER.info("Received disconnect packet from host {}", hostSteamId);
                            disconnect();
                            return;
                        }
                    }
                }

                if (!hadActivity) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            if (connectingOrConnected.get()) {
                LOGGER.info("Steam receiver loop ended: {}", e.getMessage());
            }
        } finally {
            disconnect();
        }
    }

    public synchronized void disconnect() {
        if (!connectingOrConnected.compareAndSet(true, false)) {
            return;
        }

        LOGGER.info("Disconnecting Steam client bridge...");

        if (proxyAcceptThread != null) {
            proxyAcceptThread.interrupt();
            proxyAcceptThread = null;
        }
        if (p2pReceiveThread != null) {
            p2pReceiveThread.interrupt();
            p2pReceiveThread = null;
        }

        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (Exception ignored) {
        }
        clientSocket = null;

        try {
            if (proxyServer != null && !proxyServer.isClosed()) {
                proxyServer.close();
            }
        } catch (Exception ignored) {
        }
        proxyServer = null;

        currentHostSteamId = null;
    }

    public boolean isConnected() {
        return connectingOrConnected.get();
    }
}
