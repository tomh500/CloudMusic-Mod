/*
 * Copyright 漏 2025 RTAkland
 * Author: RTAkland
 * Date: 2025/2/6
 */


package fengliu.cloudmusic.mixin;

import com.mojang.authlib.GameProfile;
import fengliu.cloudmusic.command.MusicCommand;
import fengliu.cloudmusic.config.Configs;
import fengliu.cloudmusic.util.MusicPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

/**
 * 附近有敌对生物时减小音量
 */
@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayer {

    public ClientPlayerEntityMixin(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    @Unique
    private boolean isVolumeAdjusted = false;

    @Unique
    private int previousVolume = 0;

    @Unique
    private void runDownVolume(Consumer<Boolean> run){
        if (!Configs.ENABLE.ENABLE_NEARBY_MONSTER_DECREASE_VOLUME.getBooleanValue()) {
            return;
        }

        if (!MusicCommand.getPlayer().isPlaying()) {
            return;
        }

        LocalPlayer player = (LocalPlayer) (Object) this;
        if (Configs.PLAY.NEARBY_MONSTER_IS_SURVIVAL.getBooleanValue() && player.isCreative()){
            return;
        }

        List<Entity> nearbyMobs = player.level().getEntities(
                player,
                player.getBoundingBox().inflate(Configs.ALL.NEARBY_MONSTER_DECREASE_VOLUME_RADIUS.getIntegerValue()),
                entity -> entity instanceof Monster
        );

        run.accept(nearbyMobs.isEmpty());
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    public void aiStep(CallbackInfo ci) {
        this.runDownVolume(mobsIsNot -> {
            if (mobsIsNot || isVolumeAdjusted){
                return;
            }

            MusicPlayer musicPlayer = MusicCommand.getPlayer();
            int currentVolume = musicPlayer.getVolumePercentage();
            this.previousVolume = currentVolume;
            musicPlayer.volumeSet(currentVolume - Configs.ALL.NEARBY_MONSTER_DECREASE_VOLUME_VALUE.getIntegerValue());
            isVolumeAdjusted = true;
        });
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        this.runDownVolume(mobsIsNot -> {
            if (!mobsIsNot || !isVolumeAdjusted){
                return;
            }

            isVolumeAdjusted = false;
            MusicCommand.getPlayer().volumeSet(this.previousVolume);
        });
    }
}