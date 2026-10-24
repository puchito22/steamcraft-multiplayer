package com.steamcraft;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SteamCraft implements ModInitializer {
    public static final String MOD_ID = "steamcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("SteamCraft (Spacewar P2P Multiplayer) initialized!");
    }
}
