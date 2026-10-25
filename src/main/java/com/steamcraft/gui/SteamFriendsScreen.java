package com.steamcraft.gui;

import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamMatchmaking;
import com.steamcraft.net.SteamClientBridge;
import com.steamcraft.steam.SteamManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class SteamFriendsScreen extends Screen {
    private final Screen parent;
    private final List<LobbyEntry> lobbies = new ArrayList<>();
    private boolean loading = true;
    private String statusMessage = "Buscando partidas de Steam...";

    public SteamFriendsScreen(Screen parent) {
        super(Component.literal("SteamCraft - Multijugador Steam (Spacewar)"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        refreshLobbies();
    }

    private void refreshLobbies() {
        this.lobbies.clear();
        this.loading = true;
        this.statusMessage = "Buscando partidas de Steam (Spacewar 480)...";

        this.clearWidgets();
        int centerX = this.width / 2;
        int bottomY = this.height - 35;
        this.addRenderableWidget(Button.builder(Component.literal("Refrescar"), btn -> refreshLobbies()).bounds(centerX - 155, bottomY, 95, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Abrir Steam Overlay"), btn -> SteamManager.getInstance().openInviteOverlay()).bounds(centerX - 50, bottomY, 110, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Volver"), btn -> onClose()).bounds(centerX + 65, bottomY, 90, 20).build());

        if (!SteamManager.getInstance().isInitialized()) {
            this.loading = false;
            this.statusMessage = "Steam no está en ejecución. Inicia el cliente de Steam y vuelve a intentar.";
            return;
        }

        SteamManager.getInstance().requestLobbies(foundLobbies -> {
            this.lobbies.clear();
            this.loading = false;

            SteamMatchmaking mm = SteamManager.getInstance().getMatchmaking();
            if (mm != null) {
                for (SteamID id : foundLobbies) {
                    String name = mm.getLobbyData(id, "name");
                    String version = mm.getLobbyData(id, "version");
                    if (name == null || name.isEmpty()) name = "Partida de Steam";
                    if (version == null || version.isEmpty()) version = "1.21.1";
                    lobbies.add(new LobbyEntry(id, name, version));
                }
            }

            if (lobbies.isEmpty()) {
                this.statusMessage = "No se encontraron partidas abiertas de amigos en Steam.";
            } else {
                this.statusMessage = "";
                // Add join buttons for found lobbies
                int startY = 65;
                for (int i = 0; i < Math.min(lobbies.size(), 5); i++) {
                    LobbyEntry lobby = lobbies.get(i);
                    int y = startY + (i * 26);
                    this.addRenderableWidget(Button.builder(Component.literal("Unirse: " + lobby.name), btn -> {
                        SteamClientBridge.getInstance().joinLobby(lobby.id);
                    }).bounds(this.width / 2 - 140, y, 280, 20).build());
                }
            }
        });
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        // Header Title
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        // Subtitle / Info
        String steamStatus = SteamManager.getInstance().isInitialized()
                ? "§aConectado a Steam (Spacewar AppID 480)§r"
                : "§cSteam Desconectado§r";
        graphics.drawCenteredString(this.font, Component.literal(steamStatus), this.width / 2, 38, 0xAAAAAA);

        // Status or empty message
        if (!statusMessage.isEmpty()) {
            graphics.drawCenteredString(this.font, Component.literal(statusMessage), this.width / 2, 90, 0xDDDDDD);
            if (!SteamManager.getInstance().isInitialized()) {
                graphics.drawCenteredString(this.font, Component.literal("Para jugar, abre Steam en segundo plano."), this.width / 2, 110, 0xAAAAAA);
            } else if (lobbies.isEmpty() && !loading) {
                graphics.drawCenteredString(this.font, Component.literal("Dile a tu amigo que abra su mundo en LAN con Steam activado."), this.width / 2, 110, 0x888888);
                graphics.drawCenteredString(this.font, Component.literal("O puedes unirte directamente haciendo clic derecho en su perfil de Steam -> Unirse a partida."), this.width / 2, 125, 0x888888);
            }
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    private record LobbyEntry(SteamID id, String name, String version) {}
}
