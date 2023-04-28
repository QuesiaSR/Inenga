package me.quesia.inenga.mixin;

import me.quesia.inenga.Inenga;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Unit;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Shadow @Final private Map<RegistryKey<World>, ServerWorld> worlds;
    @Shadow protected abstract void prepareStartRegion(WorldGenerationProgressListener worldGenerationProgressListener);
    @Shadow @Final private static Logger LOGGER;

    private ServerWorld getWorldAtIndex(int index) {
        return (ServerWorld) this.worlds.values().toArray()[index];
    }

    private void removeTicket() {
        if (Inenga.INDEX - 1 != 0) {
            // Remove spawn chunks for dimensions that are not the overworld.
            ServerWorld serverWorld = this.getWorldAtIndex(Inenga.INDEX - 1);
            LOGGER.info("Removing ticket for dimension {}", serverWorld.getRegistryKey().getValue());
            serverWorld.getChunkManager().removeTicket(ChunkTicketType.START, new ChunkPos(serverWorld.getSpawnPos()), 11, Unit.INSTANCE);
        }
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void inenga$resetIndex(CallbackInfo ci) {
        // Reset the index back to 0 whenever a new world is created.
        Inenga.INDEX = 0;
    }

    @Redirect(method = "prepareStartRegion", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getOverworld()Lnet/minecraft/server/world/ServerWorld;"))
    private ServerWorld inenga$getNextWorld(MinecraftServer instance) {
        return this.getWorldAtIndex(Inenga.INDEX);
    }

    @Inject(method = "prepareStartRegion", at = @At("TAIL"))
    private void inenga$incrementIndex(WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci) {
        // In this method, the index will never be 0.
        Inenga.INDEX++;
        this.removeTicket();
        if (Inenga.INDEX != this.worlds.size()) {
            // Generate next dimension.
            this.prepareStartRegion(worldGenerationProgressListener);
        }
    }

    @Redirect(method = "prepareStartRegion", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/WorldGenerationProgressListener;stop()V"))
    private void inenga$preventStop(WorldGenerationProgressListener instance) {
        if (Inenga.INDEX + 1 == this.worlds.size()) {
            instance.stop();
        }
    }
}
