package com.steamcraft.client.mixin;

import com.steamcraft.gui.SteamFriendsScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public abstract class JoinMultiplayerScreenMixin extends Screen {

    protected JoinMultiplayerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        this.addRenderableWidget(
            Button.builder(Component.literal("§bAmigos de Steam§r"), btn -> {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new SteamFriendsScreen(this));
                }
            }).bounds(this.width - 135, 8, 125, 20).build()
        );
    }
}
