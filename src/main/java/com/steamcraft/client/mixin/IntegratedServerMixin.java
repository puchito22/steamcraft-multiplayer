package com.steamcraft.client.mixin;

import com.steamcraft.net.SteamHostBridge;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IntegratedServer.class)
public abstract class IntegratedServerMixin {

    @Inject(method = "publishServer", at = @At("RETURN"))
    private void onPublishServer(GameType gameType, boolean cheats, int port, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            IntegratedServer server = (IntegratedServer) (Object) this;
            int actualPort = server.getPort();
            SteamHostBridge.getInstance().startHosting(actualPort);
        }
    }

    @Inject(method = "stopServer", at = @At("HEAD"))
    private void onStopServer(CallbackInfo ci) {
        SteamHostBridge.getInstance().stopHosting();
    }

    @Inject(method = "halt", at = @At("HEAD"))
    private void onHalt(boolean waitForServer, CallbackInfo ci) {
        SteamHostBridge.getInstance().stopHosting();
    }
}
