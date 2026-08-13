package fengliu.cloudmusic.mixin;

import fengliu.cloudmusic.music163.Shares;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ChatComponent.class)
public class ChatHudMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("cloudmusic");

    @Shadow
    private List<GuiMessage.Line> trimmedMessages;

    @Unique
    private int chatTickCounter = 0;

    @Unique
    private static final Pattern SHAR_PATTERN = Pattern.compile("CloudMusic#.+\\sid:\\s[^\\sid:a-zA-Z]\\w[^\\sa-zA-Z]+$", Pattern.CASE_INSENSITIVE);

    /**
     * 判断是否为分享消息
     * @param sharMatcher matcher
     * @return bool
     */
    @Unique
    private boolean isSharMessage(Matcher sharMatcher){
        if (!sharMatcher.find()) {
            return false;
        }

        return sharMatcher.groupCount() <= 1;
    }

    /**
     * 设置分享消息样式
     * @param message 消息
     */
    @Unique
    private void setShar(Component message){
        Matcher sharMatcher = SHAR_PATTERN.matcher(message.getString());
        if (!isSharMessage(sharMatcher)) {
            return;
        }

        String[] keyValuePair = sharMatcher.group(0).replace("CloudMusic# ", "").split(" id: ");
        if (keyValuePair.length > 2){
            return;
        }

        for (Shares shar: Shares.values()) {
            if(!shar.isShar(keyValuePair[0])){
                continue;
            }

            try {
                ((MutableComponent) message).setStyle(
                    Style.EMPTY.withClickEvent(new ClickEvent.SuggestCommand(
                            shar.getCommand(Long.parseLong(keyValuePair[1]))
                        ))
                        .withColor(0x87CEEB)
                        .withUnderlined(true)
                );
            } catch (NumberFormatException err) {
                return;
            }

            return;
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        if (++chatTickCounter % 100 == 0) {
            LOGGER.info("[CloudMusic][Chat] tick 渲染数据: trimmedMessages={}", trimmedMessages.size());
        }
    }

    @Inject(method = "addClientSystemMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), cancellable = true)
    public void addClientSystemMessage(Component message, CallbackInfo info){
        LOGGER.info("[CloudMusic][Chat] addClientSystemMessage 线程={} 内容={}", Thread.currentThread().getName(), message.getString());
        if (!Minecraft.getInstance().isSameThread()) {
            Minecraft.getInstance().execute(() -> ((ChatComponent) (Object) this).addClientSystemMessage(message));
            info.cancel();
            return;
        }
        try {
            setShar(message);
        } catch (Exception e) {
            LOGGER.info("[CloudMusic][Chat] setShar 异常", e);
        }
    }

    @Inject(method = "addServerSystemMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), cancellable = true)
    public void addServerSystemMessage(Component message, CallbackInfo info){
        LOGGER.info("[CloudMusic][Chat] addServerSystemMessage 线程={} 内容={}", Thread.currentThread().getName(), message.getString());
        if (!Minecraft.getInstance().isSameThread()) {
            Minecraft.getInstance().execute(() -> ((ChatComponent) (Object) this).addServerSystemMessage(message));
            info.cancel();
            return;
        }
        try {
            setShar(message);
        } catch (Exception e) {
            LOGGER.info("[CloudMusic][Chat] setShar 异常", e);
        }
    }

    @Inject(method = "addPlayerMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V", at = @At("HEAD"), cancellable = true)
    public void addPlayerMessage(Component message, @Nullable MessageSignature signature, @Nullable GuiMessageTag indicator, CallbackInfo info){
        LOGGER.info("[CloudMusic][Chat] addPlayerMessage 线程={} 内容={}", Thread.currentThread().getName(), message.getString());
        if (!Minecraft.getInstance().isSameThread()) {
            Minecraft.getInstance().execute(() -> ((ChatComponent) (Object) this).addPlayerMessage(message, signature, indicator));
            info.cancel();
            return;
        }
        try {
            setShar(message);
        } catch (Exception e) {
            LOGGER.info("[CloudMusic][Chat] setShar 异常", e);
        }
    }
}