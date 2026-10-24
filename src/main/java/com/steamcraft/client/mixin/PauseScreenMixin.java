package com.steamcraft.client.mixin;

import com.steamcraft.net.SteamHostBridge;
import com.steamcraft.steam.SteamManager;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {

    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (SteamHostBridge.getInstance().isHosting()) {
            this.addRenderableWidget(
                Button.builder(Component.literal("§6Invitar a Steam§r"), btn -> SteamManager.getInstance().openInviteOverlay())
                    .bounds(this.width - 150, 10, 140, 20)
                    .build()
            );
        }
    }
}
