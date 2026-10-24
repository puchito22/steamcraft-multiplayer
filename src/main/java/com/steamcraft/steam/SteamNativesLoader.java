package com.steamcraft.steam;

import com.codedisaster.steamworks.SteamLibraryLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class SteamNativesLoader implements SteamLibraryLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger("SteamCraft/Natives");
    private final Path extractDir;

    public SteamNativesLoader() {
        // Store in a dedicated natives folder in current directory or user home
        Path dir = Paths.get("steamworks-natives");
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            LOGGER.error("Failed to create natives directory: {}", dir, e);
        }
        this.extractDir = dir;
    }

    @Override
    public void setLibraryPath(String path) {
    }

    @Override
    public boolean loadLibrary(String libraryName) {
        String os = System.getProperty("os.name").toLowerCase();
        boolean is64 = System.getProperty("os.arch").contains("64");
        String filename;

        if (os.contains("win")) {
            filename = libraryName + (is64 ? "64.dll" : ".dll");
        } else if (os.contains("linux")) {
            filename = "lib" + libraryName + ".so";
        } else if (os.contains("mac") || os.contains("darwin")) {
            filename = "lib" + libraryName + ".dylib";
        } else {
            filename = libraryName;
        }

        Path target = extractDir.resolve(filename);
        try {
            // If already extracted, try loading or overwrite if needed
            boolean extracted = false;
            try (InputStream in = SteamNativesLoader.class.getResourceAsStream("/" + filename)) {
                if (in != null) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                    extracted = true;
                }
            } catch (Exception ignored) {
                // In case the file is already locked by the running process
            }

            if (!extracted && !Files.exists(target)) {
                // Try classloader resource
                try (InputStream in = SteamNativesLoader.class.getClassLoader().getResourceAsStream(filename)) {
                    if (in != null) {
                        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

            if (Files.exists(target)) {
                System.load(target.toAbsolutePath().toString());
                LOGGER.info("Steam native library loaded successfully: {}", target.toAbsolutePath());
                return true;
            } else {
                LOGGER.error("Could not find native library {} to extract", filename);
                return false;
            }
        } catch (Throwable t) {
            LOGGER.error("Failed to load native library: {}", filename, t);
            return false;
        }
    }
}
