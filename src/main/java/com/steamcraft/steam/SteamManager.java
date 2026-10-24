package com.steamcraft.steam;

import com.codedisaster.steamworks.*;
import com.steamcraft.net.SteamClientBridge;
import com.steamcraft.net.SteamHostBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SteamManager {
    public static final int SPACEWAR_APP_ID = 480;
    private static final Logger LOGGER = LoggerFactory.getLogger("SteamCraft/Steam");
    private static SteamManager instance;

    private boolean initialized = false;
    private SteamFriends friends;
    private SteamMatchmaking matchmaking;
    private SteamNetworking networking;
    private SteamID currentLobby;
    private Consumer<List<SteamID>> lobbyListCallback;

    public static synchronized SteamManager getInstance() {
        if (instance == null) {
            instance = new SteamManager();
        }
        return instance;
    }

    private SteamManager() {
    }

    public void init() {
        if (initialized) return;

        try {
            ensureSteamAppIdFile();

            LOGGER.info("Loading Steamworks native libraries...");
            SteamNativesLoader loader = new SteamNativesLoader();
            if (!SteamAPI.loadLibraries(loader)) {
                LOGGER.error("Failed to load Steamworks native libraries!");
                return;
            }

            if (!SteamAPI.init()) {
                LOGGER.warn("SteamAPI.init() returned false! Ensure Steam is running.");
                return;
            }

            LOGGER.info("Steamworks initialized successfully with AppID {} (Spacewar)!", SPACEWAR_APP_ID);
            setupInterfaces();
            initialized = true;

        } catch (Throwable t) {
            LOGGER.error("Exception during Steamworks initialization", t);
        }
    }

    private void ensureSteamAppIdFile() {
        // Create steam_appid.txt in current working dir and mc game dir
        writeAppIdFile(new File("steam_appid.txt"));
        try {
            Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
            Object mcInst = mcClass.getMethod("getInstance").invoke(null);
            if (mcInst != null) {
                File gameDir = (File) mcClass.getField("gameDirectory").get(mcInst);
                if (gameDir != null) {
                    writeAppIdFile(new File(gameDir, "steam_appid.txt"));
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void runOnClientThread(Runnable r) {
        try {
            Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
            Object mcInst = mcClass.getMethod("getInstance").invoke(null);
            if (mcInst != null) {
                mcClass.getMethod("execute", Runnable.class).invoke(mcInst, r);
                return;
            }
        } catch (Throwable ignored) {
        }
        new Thread(r).start();
    }

    private void writeAppIdFile(File file) {
        try {
            if (!file.exists()) {
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(String.valueOf(SPACEWAR_APP_ID) + "\n");
                }
                LOGGER.info("Created steam_appid.txt at {}", file.getAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.warn("Could not create steam_appid.txt at {}", file.getAbsolutePath(), e);
        }
    }

    private void setupInterfaces() {
        // Friends
        this.friends = new SteamFriends(new SteamFriendsCallback() {
            @Override
            public void onGameLobbyJoinRequested(SteamID steamIDLobby, SteamID steamIDFriend) {
                LOGGER.info("Steam invite / join requested: Lobby={}, Friend={}", steamIDLobby, steamIDFriend);
                runOnClientThread(() -> {
                    SteamClientBridge.getInstance().joinLobby(steamIDLobby);
                });
            }
        });

        // Matchmaking
        this.matchmaking = new SteamMatchmaking(new SteamMatchmakingCallback() {
            @Override
            public void onLobbyCreated(SteamResult result, SteamID steamIDLobby) {
                LOGGER.info("Lobby created: Result={}, LobbyID={}", result, steamIDLobby);
                if (result == SteamResult.OK) {
                    currentLobby = steamIDLobby;
                    SteamHostBridge.getInstance().onLobbyCreated(steamIDLobby);
                }
            }

            @Override
            public void onLobbyEnter(SteamID steamIDLobby, int chatPermissions, boolean blocked, SteamMatchmaking.ChatRoomEnterResponse response) {
                LOGGER.info("Entered lobby: {}, response: {}", steamIDLobby, response);
                if (response == SteamMatchmaking.ChatRoomEnterResponse.Success) {
                    currentLobby = steamIDLobby;
                    SteamClientBridge.getInstance().onLobbyEntered(steamIDLobby);
                }
            }

            @Override
            public void onLobbyMatchList(int lobbiesMatching) {
                LOGGER.info("Found {} Steam lobbies matching search", lobbiesMatching);
                List<SteamID> lobbies = new ArrayList<>();
                for (int i = 0; i < lobbiesMatching; i++) {
                    SteamID lobby = matchmaking.getLobbyByIndex(i);
                    if (lobby != null && lobby.isValid()) {
                        lobbies.add(lobby);
                    }
                }
                if (lobbyListCallback != null) {
                    Consumer<List<SteamID>> cb = lobbyListCallback;
                    lobbyListCallback = null;
                    runOnClientThread(() -> cb.accept(lobbies));
                }
            }
        });

        // Networking
        this.networking = new SteamNetworking(new SteamNetworkingCallback() {
            @Override
            public void onP2PSessionRequest(SteamID steamIDRemote) {
                LOGGER.info("Received Steam P2P session request from: {}", steamIDRemote);
                if (networking != null) {
                    networking.acceptP2PSessionWithUser(steamIDRemote);
                }
            }

            @Override
            public void onP2PSessionConnectFail(SteamID steamIDRemote, SteamNetworking.P2PSessionError sessionError) {
                LOGGER.warn("P2P session connect failed with {}: {}", steamIDRemote, sessionError);
            }
        });

        // Enable Valve's Steam Datagram Relay (SDR)
        this.networking.allowP2PPacketRelay(true);
    }

    public void tick() {
        if (initialized) {
            SteamAPI.runCallbacks();
        }
    }

    public void openInviteOverlay() {
        if (initialized && friends != null && currentLobby != null) {
            friends.activateGameOverlayInviteDialog(currentLobby);
        } else if (initialized && friends != null) {
            friends.activateGameOverlay(SteamFriends.OverlayDialog.Friends);
        }
    }

    public void requestLobbies(Consumer<List<SteamID>> callback) {
        if (!initialized || matchmaking == null) {
            if (callback != null) callback.accept(new ArrayList<>());
            return;
        }
        this.lobbyListCallback = callback;
        matchmaking.addRequestLobbyListStringFilter("game", "MinecraftSteamCraft", SteamMatchmaking.LobbyComparison.Equal);
        matchmaking.requestLobbyList();
    }

    public boolean isInitialized() {
        return initialized;
    }

    public SteamFriends getFriends() {
        return friends;
    }

    public SteamMatchmaking getMatchmaking() {
        return matchmaking;
    }

    public SteamNetworking getNetworking() {
        return networking;
    }

    public SteamID getCurrentLobby() {
        return currentLobby;
    }

    public void setCurrentLobby(SteamID currentLobby) {
        this.currentLobby = currentLobby;
    }

    public void shutdown() {
        if (initialized) {
            LOGGER.info("Shutting down SteamAPI...");
            SteamAPI.shutdown();
            initialized = false;
        }
    }
}
