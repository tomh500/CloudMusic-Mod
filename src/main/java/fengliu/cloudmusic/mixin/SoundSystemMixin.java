package fengliu.cloudmusic.mixin;

import fengliu.cloudmusic.command.MusicCommand;
import fengliu.cloudmusic.config.Configs;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在播放音乐的时候如果MC需要播放背景音乐的话
 * 就取消播放背景音乐的事件
 */
@Mixin(SoundManager.class)
public abstract class SoundSystemMixin {

    @Unique
    public SoundSource currentCategory;

    /**
     * 判断是否需要停止播放背景音乐
     * @param soundCategory 音乐类
     * @return false 不播放
     */
    @Unique
    private static boolean canStopGameMusic(SoundSource soundCategory){
        if (!Configs.PLAY.NOT_PLAY_GAME_MUSIC.getBooleanValue()){
            return false;
        }

        if (!MusicCommand.getPlayer().isPlaying()) {
            return false;
        }

        return soundCategory == SoundSource.MUSIC;
    }

    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;", at = @At("HEAD"), cancellable = true)
    public void play(SoundInstance soundInstance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        currentCategory = soundInstance.getSource();
        if (!canStopGameMusic(soundInstance.getSource())){
            return;
        }
        cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
    }

    @Inject(method = "tick(Z)V", at = @At("HEAD"), cancellable = true)
    public void tick(CallbackInfo ci) {
        if (!canStopGameMusic(currentCategory)){
            return;
        }
        ci.cancel();
    }
}