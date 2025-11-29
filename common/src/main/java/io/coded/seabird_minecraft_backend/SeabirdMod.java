package io.coded.seabird_minecraft_backend;

import com.google.gson.Gson;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import dev.architectury.event.EventResult;
import io.coded.seabird.chat_ingest.ChatIngestGrpc;
import io.coded.seabird.chat_ingest.SeabirdChatIngest;
import io.coded.seabird.common.Common;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import dev.architectury.event.events.common.ChatEvent;
import dev.architectury.event.events.common.CommandPerformEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.platform.Platform;
import dev.architectury.utils.GameInstance;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class SeabirdMod {
    public static final String MOD_ID = "seabird_minecraft_backend";
    static final Logger LOGGER = LogManager.getLogger();
    static Config config = SeabirdMod.readConfig();
    static LinkedBlockingDeque<Object> outgoingQueue = new LinkedBlockingDeque<>();

    public static void init() {
        Thread grpcThread = new Thread(SeabirdMod::runGrpc, "Seabird gRPC Client");
        grpcThread.start();

        CommandPerformEvent.EVENT.register((command) -> {
            SeabirdMod.onCommand(command);
            return EventResult.pass();
        });

        PlayerEvent.PLAYER_JOIN.register(SeabirdMod::onPlayerJoined);
        PlayerEvent.PLAYER_QUIT.register(SeabirdMod::onPlayerLeft);
        PlayerEvent.PLAYER_ADVANCEMENT.register(SeabirdMod::onAdvancement);

        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (entity instanceof Player) {
                SeabirdMod.onPlayerDied((Player) entity, source);
            }
            return EventResult.pass();
        });

        ChatEvent.RECEIVED.register((player, component) -> {
            SeabirdMod.onMessage(player, component);
            return EventResult.pass();
        });
    }

    private static Config readConfig() {
        try {
            Gson gson = new Gson();

            Path configPath = Platform.getConfigFolder().resolve("seabird-minecraft-backend.json");

            Config config = gson.fromJson(new FileReader(configPath.toString()), Config.class);

            if (config.seabirdHost == null) {
                config.seabirdHost = "seabird-core.elwert.cloud";
            }
            if (config.seabirdPort == 0) {
                config.seabirdPort = 443;
            }
            if (config.seabirdToken == null) {
                throw new Error("cannot start without seabirdToken");
            }
            if (config.backendId == null) {
                config.backendId = "minecraft";
            }
            if (config.backendChannel == null) {
                config.backendChannel = "minecraft";
            }
            if (config.systemDisplayName == null) {
                config.systemDisplayName = "Minecraft";
            }

            return config;
        } catch (Exception e) {
            LOGGER.fatal("failed to load config: {}", (Object) e);
            return null;
        }
    }

    private static void onAdvancement(ServerPlayer player, AdvancementHolder advancementHolder) {
        Advancement advancement = advancementHolder.value();

        // Recipes are reported as advancements, but we don't care about them - the easiest way to check for them is to
        // look for an id starting with minecraft:recipes/ or check if the DisplayInfo is null.
        //
        // Additionally, we need to make sure this is something we should announce in chat, otherwise VanillaTweaks
        // displays empty advancements all the time.
        Component advancementName = advancement.name().orElse(null);
        DisplayInfo display = advancement.display().orElse(null);
        if (advancementName == null || display == null || !display.shouldAnnounceChat()) {
            return;
        }

        SeabirdChatIngest.ChatEvent event = SeabirdChatIngest.ChatEvent.newBuilder()
                .setMessage(Common.MessageEvent.newBuilder()
                        .setSource(Common.ChannelSource.newBuilder()
                                .setChannelId(SeabirdMod.config.backendChannel)
                                .setUser(Common.User.newBuilder()
                                        .setId("SYSTEM")
                                        .setDisplayName(SeabirdMod.config.systemDisplayName)))
                        .setText(String.format(
                                "%s has made the advancement %s.",
                                player.getGameProfile().getName(),
                                advancementName.getString()
                        ))).build();

        SeabirdMod.outgoingQueue.push(event);
    }

    private static void onPlayerDied(Player player, DamageSource source) {
        Component message = source.getLocalizedDeathMessage(player);

        SeabirdChatIngest.ChatEvent event = SeabirdChatIngest.ChatEvent.newBuilder()
                .setMessage(Common.MessageEvent.newBuilder()
                        .setSource(Common.ChannelSource.newBuilder()
                                .setChannelId(SeabirdMod.config.backendChannel)
                                .setUser(Common.User.newBuilder()
                                        .setId("SYSTEM")
                                        .setDisplayName(SeabirdMod.config.systemDisplayName)))
                        .setText(message.getString())).build();

        SeabirdMod.outgoingQueue.push(event);
    }

    private static void onPlayerJoined(ServerPlayer player) {
        SeabirdChatIngest.ChatEvent event = SeabirdChatIngest.ChatEvent.newBuilder()
                .setMessage(Common.MessageEvent.newBuilder()
                        .setSource(Common.ChannelSource.newBuilder()
                                .setChannelId(SeabirdMod.config.backendChannel)
                                .setUser(Common.User.newBuilder()
                                        .setId("SYSTEM")
                                        .setDisplayName(SeabirdMod.config.systemDisplayName)))
                        .setText(String.format("%s joined the server.", player.getGameProfile().getName()))).build();

        SeabirdMod.outgoingQueue.push(event);
    }

    private static void onPlayerLeft(ServerPlayer player) {
        SeabirdChatIngest.ChatEvent event = SeabirdChatIngest.ChatEvent.newBuilder()
                .setMessage(Common.MessageEvent.newBuilder()
                        .setSource(Common.ChannelSource.newBuilder()
                                .setChannelId(SeabirdMod.config.backendChannel)
                                .setUser(Common.User.newBuilder()
                                        .setId("SYSTEM")
                                        .setDisplayName(SeabirdMod.config.systemDisplayName)))
                        .setText(String.format("%s left the server.", player.getGameProfile().getName()))).build();

        SeabirdMod.outgoingQueue.push(event);
    }

    public static void onMessage(ServerPlayer player, Component component) {
        SeabirdChatIngest.ChatEvent event = SeabirdChatIngest.ChatEvent.newBuilder()
                .setMessage(Common.MessageEvent.newBuilder()
                        .setSource(Common.ChannelSource.newBuilder()
                                .setChannelId(SeabirdMod.config.backendChannel)
                                .setUser(Common.User.newBuilder()
                                        .setId(player.getStringUUID())
                                        .setDisplayName(player.getGameProfile().getName())))
                        .setText(component.getString())).build();

        SeabirdMod.outgoingQueue.push(event);
    }

    private static void onCommand(CommandPerformEvent event) {
        ParseResults<CommandSourceStack> results = event.getResults();
        CommandContextBuilder<CommandSourceStack> ctx = results.getContext();

        List<ParsedCommandNode<CommandSourceStack>> nodes = ctx.getNodes();

        // Normally we'd check for 1, but because what we need only has 2 arguments (a literal and a command argument),
        // we can take a shortcut.
        if (nodes.size() < 2) {
            return;
        }

        CommandSourceStack src = ctx.getSource();

        String commandName = nodes.get(0).getNode().getName();
        String argument = nodes.get(1).getRange().get(results.getReader());

        switch (commandName) {
            case "me" -> {
                Entity entity = src.getEntity();
                if (!(entity instanceof ServerPlayer player)) {
                    return;
                }
                onEmote(player, argument);
            }
            case "say" -> {
                onSystemMessage(src.getDisplayName().getString(), argument);
            }
        }
    }

    public static void onEmote(ServerPlayer player, String message) {
        SeabirdChatIngest.ChatEvent event = SeabirdChatIngest.ChatEvent.newBuilder()
                .setAction(Common.ActionEvent.newBuilder()
                        .setSource(Common.ChannelSource.newBuilder()
                                .setChannelId(SeabirdMod.config.backendChannel)
                                .setUser(Common.User.newBuilder()
                                        .setId(player.getStringUUID())
                                        .setDisplayName(player.getGameProfile().getName())))
                        .setText(message)).build();

        SeabirdMod.outgoingQueue.push(event);
    }

    public static void onSystemMessage(String sender, String text) {
        SeabirdChatIngest.ChatEvent event = SeabirdChatIngest.ChatEvent.newBuilder()
                .setMessage(Common.MessageEvent.newBuilder()
                        .setSource(Common.ChannelSource.newBuilder()
                                .setChannelId(SeabirdMod.config.backendChannel)
                                .setUser(Common.User.newBuilder()
                                        .setId("SYSTEM")
                                        .setDisplayName(sender)))
                        .setText(text)).build();

        SeabirdMod.outgoingQueue.push(event);
    }

    public static void runGrpc() {
        //noinspection InfiniteLoopStatement
        while (true) {
            try {
                LOGGER.info("Connecting to Seabird Core at {}:{}", SeabirdMod.config.seabirdHost, SeabirdMod.config.seabirdPort);
                ManagedChannel channel = ManagedChannelBuilder.forAddress(SeabirdMod.config.seabirdHost, SeabirdMod.config.seabirdPort).useTransportSecurity().build();

                ChatIngestGrpc.ChatIngestStub stub = ChatIngestGrpc.newStub(channel)
                        .withCallCredentials(new AccessTokenCallCredentials(config.seabirdToken));

                StreamObserver<SeabirdChatIngest.ChatEvent> output = stub.ingestEvents(new StreamObserver<SeabirdChatIngest.ChatRequest>() {
                    @Override
                    public void onNext(SeabirdChatIngest.ChatRequest event) {
                        boolean success = false;

                        // This backend only supports SEND_MESSAGE.
                        if (event.getInnerCase() == SeabirdChatIngest.ChatRequest.InnerCase.SEND_MESSAGE) {
                            SeabirdChatIngest.SendMessageChatRequest req = event.getSendMessage();
                            SeabirdMod.sendToAll("chat.type.text", "seabird", req.getText());
                            success = true;
                        } else {
                            LOGGER.warn("Unknown or unsupported request type");
                        }

                        // If the event needed a response, make sure we respond.
                        String id = event.getId();
                        if (!id.equals("")) {
                            if (success) {
                                SeabirdMod.outgoingQueue.push(SeabirdChatIngest.ChatEvent.newBuilder().setId(id).setSuccess(SeabirdChatIngest.SuccessChatEvent.newBuilder()).build());
                            } else {
                                SeabirdMod.outgoingQueue.push(SeabirdChatIngest.ChatEvent.newBuilder().setId(id).setFailed(SeabirdChatIngest.FailedChatEvent.newBuilder()).build());
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        SeabirdMod.outgoingQueue.push(t);
                    }

                    @Override
                    public void onCompleted() {
                        SeabirdMod.outgoingQueue.push(new Error("seabird-core ended the stream"));
                    }
                });

                try {
                    // Send the first hello event.
                    output.onNext(
                            SeabirdChatIngest.ChatEvent.newBuilder()
                                    .setHello(SeabirdChatIngest.HelloChatEvent.newBuilder()
                                            .setBackendInfo(Common.Backend.newBuilder()
                                                    .setType("minecraft").setId(config.backendId))).build());

                    while (true) {
                        // Now that we're here, we need to wait for items on the queue, and push them out as chat events.
                        Object event = SeabirdMod.outgoingQueue.take();

                        if (event instanceof SeabirdChatIngest.ChatEvent) {
                            output.onNext((SeabirdChatIngest.ChatEvent) event);
                        } else if (event instanceof Throwable) {
                            throw (Throwable) event;
                        } else {
                            throw new Error(String.format("Unknown event type: %s", event));
                        }
                    }
                } finally {
                    channel.shutdown();
                    channel.awaitTermination(1, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                LOGGER.warn("Lost connection to Seabird Core, restarting connection in 1 second.");
                SeabirdMod.sleepNoFail(1000);
            } catch (Throwable e) {
                LOGGER.warn("Exception while handling gRPC connection, restarting connection in 1 second: {}", (Object) e);
                SeabirdMod.sleepNoFail(1000);
            }
        }
    }

    private static void sendToAll(String key, Object... args) {
        // TODO: try to do the "right" thing re PlayerChatMessages
        MinecraftServer server = GameInstance.getServer();
        Component component = Component.translatable(key, args);
        server.getPlayerList().broadcastSystemMessage(component, false);
    }

    static void sleepNoFail(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception e) {
            LOGGER.warn("Exception while sleeping: %s", e);
        }
    }

    static class Config {
        String seabirdHost;
        int seabirdPort;
        String seabirdToken;
        String backendId;
        String backendChannel;
        String systemDisplayName;
    }
}

