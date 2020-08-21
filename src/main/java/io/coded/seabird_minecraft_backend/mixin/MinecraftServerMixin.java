package io.coded.seabird_minecraft_backend.mixin;

import io.coded.seabird_minecraft_backend.event.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    private static final Logger LOGGER = LogManager.getLogger();

    @Inject(at = @At("RETURN"), method = "sendSystemMessage")
    protected void sendSystemMessage(Text rawText, UUID senderUUID, CallbackInfo info) {
        if (!(rawText instanceof TranslatableText)) {
            return;
        }
        TranslatableText text = (TranslatableText) rawText;

        String key = text.getKey();

        // We special case player death messages because there are so many of them.
        //
        // NOTE: death event handling should be bulletproofed.
        if (key.startsWith("death.")) {
            this.handleDeath(text);
            return;
        }

        switch (key) {
            case "chat.type.advancement.task":
                this.handleAdvancement(text);
                break;
            case "chat.type.announcement":
                this.handleAnnouncement(text);
                break;
            case "chat.type.emote":
                this.handleEmote(text);
                break;
            case "chat.type.text":
                this.handleMessage(text);
                return;
            case "multiplayer.player.joined":
                this.handlePlayerJoin(text);
                break;
            case "multiplayer.player.left":
                this.handlePlayerLeft(text);
                break;
            default:
                System.out.println("UNKNOWN KEY: " + text.getKey());
                this.printArgs(text);
                break;
        }
    }

    MinecraftServer getServer() {
        return (MinecraftServer) (Object) this;
    }

    ServerPlayerEntity lookupPlayer(String name) {
        MinecraftServer server = this.getServer();
        return server.getPlayerManager().getPlayer(name);
    }

    void printArgs(TranslatableText text) {
        for (Object o : text.getArgs()) {
            System.out.println(o.getClass().getName() + " : " + o);
        }
    }

    private void handlePlayerLeft(TranslatableText text) {
        System.out.println("PLAYER LEFT : " + text.getString());

        Object[] args = text.getArgs();
        if (args.length != 1) {
            LOGGER.warn("Unexpected number of args");
            return;
        }

        if (!(args[0] instanceof LiteralText)) {
            LOGGER.warn("Invalid arg");
            return;
        }

        String sender = ((LiteralText) args[0]).getString();

        ServerPlayerEntity player = this.lookupPlayer(sender);

        PlayerLeftCallback.EVENT.invoker().left(player.getUuid(), player.getGameProfile().getName());

        // this.printArgs(text);
    }

    private void handlePlayerJoin(TranslatableText text) {
        System.out.println("PLAYER JOINED: " + text.getString());

        Object[] args = text.getArgs();
        if (args.length != 1) {
            LOGGER.warn("Unexpected number of args");
            return;
        }

        if (!(args[0] instanceof LiteralText)) {
            LOGGER.warn("Invalid arg");
            return;
        }

        String sender = ((LiteralText) args[0]).getString();

        // ServerPlayerEntity player = this.lookupPlayer(sender);

        PlayerJoinedCallback.EVENT.invoker().joined(sender);

        // this.printArgs(text);
    }

    private void handleDeath(TranslatableText text) {
        Object[] args = text.getArgs();
        if (args.length < 1) {
            LOGGER.warn("Unexpected number of args");
            return;
        }

        if (!(args[0] instanceof LiteralText)) {
            LOGGER.warn("Invalid arg");
            return;
        }

        String sender = ((LiteralText) args[0]).getString();
        String message = text.getString();

        ServerPlayerEntity player = this.lookupPlayer(sender);

        PlayerDiedCallback.EVENT.invoker().death(player.getUuid(), player.getGameProfile().getName(), message);

        // this.printArgs(text);
    }

    private void handleEmote(TranslatableText text) {
        Object[] args = text.getArgs();
        if (args.length != 2) {
            LOGGER.warn("Unexpected number of args");
            return;
        }

        if (!(args[0] instanceof LiteralText && args[1] instanceof String)) {
            LOGGER.warn("Invalid arg");
            return;
        }

        String sender = ((LiteralText) args[0]).getString();
        String action = (String) args[1];

        ServerPlayerEntity player = this.lookupPlayer(sender);

        EmoteCallback.EVENT.invoker().emote(player.getUuid(), player.getGameProfile().getName(), action);

        // this.printArgs(text);
    }

    private void handleAdvancement(TranslatableText text) {
        Object[] args = text.getArgs();
        if (args.length != 2) {
            LOGGER.warn("Unexpected number of args");
            return;
        }

        if (!(args[0] instanceof LiteralText)) {
            LOGGER.warn("Invalid arg");
            return;
        }

        String sender = ((LiteralText) args[0]).getString();
        String advancement = text.getString();
        //String advancement = args[1];

        ServerPlayerEntity player = this.lookupPlayer(sender);

        AdvancementCallback.EVENT.invoker().advancement(player.getUuid(), player.getGameProfile().getName(), advancement);

        // this.printArgs(text);
    }

    void handleAnnouncement(TranslatableText text) {
        Object[] args = text.getArgs();
        if (args.length != 2) {
            LOGGER.warn("Unexpected number of args");
            return;
        }

        if (!(args[0] instanceof LiteralText && args[1] instanceof LiteralText)) {
            LOGGER.warn("Invalid arg");
            return;
        }

        String sender = ((LiteralText) args[0]).getString();
        String message = ((LiteralText) args[1]).getString();

        // We don't want to send our own internal messages as outgoing messages as well.
        if (sender.equals("seabird")) {
            return;
        }

        SystemMessageCallback.EVENT.invoker().systemMessage(sender, message);

        // this.printArgs(text);
    }

    void handleMessage(TranslatableText text) {
        Object[] args = text.getArgs();
        if (args.length != 2) {
            LOGGER.warn("Unexpected number of args");
            return;
        }

        if (!(args[0] instanceof LiteralText && args[1] instanceof String)) {
            LOGGER.warn("Invalid arg");
            return;
        }

        LiteralText literalText = (LiteralText) args[0];
        String message = (String) args[1];
        String sender = literalText.getString();

        ServerPlayerEntity player = this.lookupPlayer(sender);

        MessageCallback.EVENT.invoker().message(player.getUuid(), player.getGameProfile().getName(), message);

        // this.printArgs(text);
    }
}
