package com.steamcraft.client.mixin;

import com.steamcraft.steam.SteamManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShareToLanScreen.class)
public abstract class ShareToLanScreenMixin extends Screen {

    protected ShareToLanScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        boolean steamOk = SteamManager.getInstance().isInitialized();
        String message = steamOk
                ? "§a✔ Steam Spacewar Activo§r - Tu partida se publicará en Steam automáticamente"
                : "§6⚠ Steam no detectado§r - Inicia Steam para permitir que tus amigos se unan";

        graphics.drawCenteredString(this.font, Component.literal(message), this.width / 2, this.height - 58, 0xFFFFFF);
    }
}
