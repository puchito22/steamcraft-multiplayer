package com.steamcraft.client;

import com.steamcraft.net.SteamClientBridge;
import com.steamcraft.net.SteamHostBridge;
import com.steamcraft.steam.SteamManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SteamCraftClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("SteamCraft/Client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing SteamCraft Client...");

        // Initialize Steamworks API (AppID 480 - Spacewar)
        SteamManager.getInstance().init();

        // Register tick event to process Steam callbacks
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            SteamManager.getInstance().tick();
        });

        // Clean up on Minecraft shutdown
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            LOGGER.info("Minecraft stopping, cleaning up SteamCraft...");
            SteamHostBridge.getInstance().stopHosting();
            SteamClientBridge.getInstance().disconnect();
            SteamManager.getInstance().shutdown();
        });
    }
}
