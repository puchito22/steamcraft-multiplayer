package com.steamcraft;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SteamCraft implements ModInitializer {
    public static final String MOD_ID = "steamcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("SteamCraft (Spacewar P2P Multiplayer) initialized!");

        // Hook dedicated server lifecycle so the mod works out-of-the-box on dedicated servers
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (server.isDedicatedServer()) {
                int port = server.getPort();
                LOGGER.info("[SteamCraft] Dedicated server detected on port {}. Initializing Steam P2P host...", port);
                try {
                    com.steamcraft.steam.SteamManager.getInstance().init();
                    com.steamcraft.net.SteamHostBridge.getInstance().startHosting(port);
                    LOGGER.info("[SteamCraft] Dedicated server Steam lobby created successfully!");
                } catch (Throwable t) {
                    LOGGER.error("[SteamCraft] Failed to initialize Steam host on dedicated server: ", t);
                }
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (server.isDedicatedServer()) {
                LOGGER.info("[SteamCraft] Dedicated server stopping, cleaning up Steam host...");
                try {
                    com.steamcraft.net.SteamHostBridge.getInstance().stopHosting();
                    com.steamcraft.steam.SteamManager.getInstance().shutdown();
                } catch (Throwable ignored) {
                }
            }
        });
    }
}
