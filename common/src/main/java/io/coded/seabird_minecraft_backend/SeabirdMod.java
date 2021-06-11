package io.coded.seabird_minecraft_backend;

import com.google.gson.Gson;
import io.coded.seabird.chat_ingest.ChatIngestGrpc;
import io.coded.seabird.chat_ingest.SeabirdChatIngest;
import io.coded.seabird.common.Common;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import me.shedaniel.architectury.event.events.ChatEvent;
import me.shedaniel.architectury.event.events.EntityEvent;
import me.shedaniel.architectury.event.events.PlayerEvent;
import me.shedaniel.architectury.platform.Platform;
import me.shedaniel.architectury.utils.GameInstance;
import net.minecraft.Util;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileReader;
import java.nio.file.Path;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class SeabirdMod {
    public static final String MOD_ID = "seabird_minecraft_backend";

    static class Config {
        String seabirdHost;
        int seabirdPort;
        String seabirdToken;
        String backendId;
        String backendChannel;
        String systemDisplayName;
    }

    static final Logger LOGGER = LogManager.getLogger();
    static Config config = SeabirdMod.readConfig();
    static LinkedBlockingDeque<Object> outgoingQueue = new LinkedBlockingDeque<>();

    public static void init() {
        System.out.println("Hello World");

        Thread grpcThread = new Thread(SeabirdMod::runGrpc, "Seabird gRPC Client");
        grpcThread.start();

        PlayerEvent.PLAYER_JOIN.register(SeabirdMod::onPlayerJoined);
        PlayerEvent.PLAYER_QUIT.register(SeabirdMod::onPlayerLeft);
        PlayerEvent.PLAYER_ADVANCEMENT.register(SeabirdMod::onAdvancement);

        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (entity instanceof Player) {
                SeabirdMod.onPlayerDied((Player) entity, source);
            }
            return InteractionResult.PASS;
        });

        ChatEvent.SERVER.register((player, message, component) -> {
            SeabirdMod.onMessage(player, message, component);
            return InteractionResultHolder.pass(component);
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

    private static void onAdvancement(ServerPlayer player, Advancement advancement) {
        SeabirdChatIngest.ChatEvent event = SeabirdChatIngest.ChatEvent.newBuilder()
                .setMessage(Common.MessageEvent.newBuilder()
                        .setSource(Common.ChannelSource.newBuilder()
                                .setChannelId(SeabirdMod.config.backendChannel)
                                .setUser(Common.User.newBuilder()
                                        .setId("SYSTEM")
                                        .setDisplayName(SeabirdMod.config.systemDisplayName)))
                        .setText(advancement.getChatComponent().getString())).build();

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
                        .setText(String.format("%s joined the server.", player.getScoreboardName()))).build();

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
                        .setText(String.format("%s left the server.", player.getScoreboardName()))).build();

        SeabirdMod.outgoingQueue.push(event);
    }

    public static void onMessage(ServerPlayer player, String message, Component component) {
        LOGGER.warn("Message: {}", message);
        LOGGER.warn("Component: {}", component);

        SeabirdChatIngest.ChatEvent event = SeabirdChatIngest.ChatEvent.newBuilder()
                .setMessage(Common.MessageEvent.newBuilder()
                        .setSource(Common.ChannelSource.newBuilder()
                                .setChannelId(SeabirdMod.config.backendChannel)
                                .setUser(Common.User.newBuilder()
                                        .setId(player.getStringUUID())
                                        .setDisplayName(player.getScoreboardName())))
                        .setText(message)).build();

        SeabirdMod.outgoingQueue.push(event);
    }

    public static void onEmote(ServerPlayer player, String message, Component component) {
        SeabirdChatIngest.ChatEvent event = SeabirdChatIngest.ChatEvent.newBuilder()
                .setAction(Common.ActionEvent.newBuilder()
                        .setSource(Common.ChannelSource.newBuilder()
                                .setChannelId(SeabirdMod.config.backendChannel)
                                .setUser(Common.User.newBuilder()
                                        .setId(player.getStringUUID())
                                        .setDisplayName(player.getScoreboardName())))
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
                            SeabirdMod.sendToAll("chat.type.announcement", "seabird", req.getText());
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
        MinecraftServer server = GameInstance.getServer();
        Component component = new TranslatableComponent(key, args);
        Packet<ClientGamePacketListener> packet = new ClientboundChatPacket(component, ChatType.CHAT, Util.NIL_UUID);
        server.getPlayerList().broadcastAll(packet);
    }

    static void sleepNoFail(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception e) {
            LOGGER.warn("Exception while sleeping: %s", e);
        }
    }
}

