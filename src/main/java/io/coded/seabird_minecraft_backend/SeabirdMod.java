package io.coded.seabird_minecraft_backend;

import io.coded.seabird.chat_ingest.ChatIngestGrpc;
import io.coded.seabird.chat_ingest.SeabirdChatIngest;
import io.coded.seabird.common.Common;
import io.coded.seabird_minecraft_backend.event.*;

import com.google.gson.Gson;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.server.ServerStartCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.MessageType;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class SeabirdMod implements ModInitializer, Runnable {
    class Config {
        String seabirdHost;
        int seabirdPort;
        String seabirdToken;
        String backendId;
        String backendChannel;
        String systemDisplayName;
    }

    private static final Logger LOGGER = LogManager.getLogger();

    LinkedBlockingDeque<Object> outgoingQueue = new LinkedBlockingDeque();

    MinecraftDedicatedServer server;

    Config config;

    @Override
    public void onInitialize() {
        this.readConfig();

        // Create a new thread which will run the gRPC chat ingest stream.
        Thread newThread = new Thread(this, "Seabird gRPC Client");
        newThread.start();

        // We only need ServerStartCallback when the FabricLoader is broken.
        if (!this.setServer(FabricLoader.getInstance().getGameInstance())) {
            ServerStartCallback.EVENT.register((server) -> {
                if (!this.setServer(server)) {
                    LOGGER.fatal("server start didn't return a usable server");
                }
            });
        }

        AdvancementCallback.EVENT.register(this::onAdvancement);
        EmoteCallback.EVENT.register(this::onEmote);
        MessageCallback.EVENT.register(this::onMessage);
        PlayerDiedCallback.EVENT.register(this::onPlayerDied);
        PlayerJoinedCallback.EVENT.register(this::onPlayerJoined);
        PlayerLeftCallback.EVENT.register(this::onPlayerLeft);
        SystemMessageCallback.EVENT.register(this::onSystemMessage);
    }

    private void onAdvancement(UUID uuid, String sender, String achievement) {
        SeabirdChatIngest.ChatEvent event = SeabirdChatIngest.ChatEvent.newBuilder()
                .setMessage(Common.MessageEvent.newBuilder()
                        .setSource(Common.ChannelSource.newBuilder()
                                .setChannelId(this.config.backendChannel)
                                .setUser(Common.User.newBuilder()
                                        .setId("SYSTEM")
                                        .setDisplayName(this.config.systemDisplayName)))
                        .setText(achievement)).build();

        this.outgoingQueue.push(event);
    }


    private void onEmote(UUID uuid, String sender, String text) {
        SeabirdChatIngest.ChatEvent event = SeabirdChatIngest.ChatEvent.newBuilder()
                .setAction(Common.ActionEvent.newBuilder()
                        .setSource(Common.ChannelSource.newBuilder()
                                .setChannelId(this.config.backendChannel)
                                .setUser(Common.User.newBuilder()
                                        .setId(uuid.toString())
                                        .setDisplayName(sender)))
                        .setText(text)).build();

        this.outgoingQueue.push(event);
    }

    public void onMessage(UUID uuid, String sender, String text) {
        SeabirdChatIngest.ChatEvent event = SeabirdChatIngest.ChatEvent.newBuilder()
                .setMessage(Common.MessageEvent.newBuilder()
                        .setSource(Common.ChannelSource.newBuilder()
                                .setChannelId(this.config.backendChannel)
                                .setUser(Common.User.newBuilder()
                                        .setId(uuid.toString())
                                        .setDisplayName(sender)))
                        .setText(text)).build();

        this.outgoingQueue.push(event);
    }

    private void onPlayerDied(UUID uuid, String sender, String message) {
        SeabirdChatIngest.ChatEvent event = SeabirdChatIngest.ChatEvent.newBuilder()
                .setMessage(Common.MessageEvent.newBuilder()
                        .setSource(Common.ChannelSource.newBuilder()
                                .setChannelId(this.config.backendChannel)
                                .setUser(Common.User.newBuilder()
                                        .setId("SYSTEM")
                                        .setDisplayName(this.config.systemDisplayName)))
                        .setText(message)).build();

        this.outgoingQueue.push(event);
    }

    private void onPlayerJoined(UUID uuid, String sender) {
        SeabirdChatIngest.ChatEvent event = SeabirdChatIngest.ChatEvent.newBuilder()
                .setMessage(Common.MessageEvent.newBuilder()
                        .setSource(Common.ChannelSource.newBuilder()
                                .setChannelId(this.config.backendChannel)
                                .setUser(Common.User.newBuilder()
                                        .setId("SYSTEM")
                                        .setDisplayName(this.config.systemDisplayName)))
                        .setText(String.format("%s joined the server.", sender))).build();

        this.outgoingQueue.push(event);
    }

    private void onPlayerLeft(UUID uuid, String sender) {
        SeabirdChatIngest.ChatEvent event = SeabirdChatIngest.ChatEvent.newBuilder()
                .setMessage(Common.MessageEvent.newBuilder()
                        .setSource(Common.ChannelSource.newBuilder()
                                .setChannelId(this.config.backendChannel)
                                .setUser(Common.User.newBuilder()
                                        .setId("SYSTEM")
                                        .setDisplayName(this.config.systemDisplayName)))
                        .setText(String.format("%s left the server.", sender))).build();

        this.outgoingQueue.push(event);
    }

    public void onSystemMessage(String sender, String text) {
        SeabirdChatIngest.ChatEvent event = SeabirdChatIngest.ChatEvent.newBuilder()
                .setMessage(Common.MessageEvent.newBuilder()
                        .setSource(Common.ChannelSource.newBuilder()
                                .setChannelId(this.config.backendChannel)
                                .setUser(Common.User.newBuilder()
                                        .setId("SYSTEM")
                                        .setDisplayName(sender)))
                        .setText(text)).build();

        this.outgoingQueue.push(event);
    }

    @Override
    public void run() {
        SeabirdMod plugin = this;

        while (true) {
            try {
                LOGGER.info("Connecting to Seabird Core at {}:{}", this.config.seabirdHost, this.config.seabirdPort);
                ManagedChannel channel = ManagedChannelBuilder.forAddress(this.config.seabirdHost, this.config.seabirdPort).usePlaintext().build();

                ChatIngestGrpc.ChatIngestStub stub = ChatIngestGrpc.newStub(channel)
                        .withCallCredentials(new AccessTokenCallCredentials(config.seabirdToken));

                StreamObserver<SeabirdChatIngest.ChatEvent> output = stub.ingestEvents(new StreamObserver<SeabirdChatIngest.ChatRequest>() {
                    @Override
                    public void onNext(SeabirdChatIngest.ChatRequest event) {
                        boolean success = false;

                        switch (event.getInnerCase()) {
                            // This backend only supports SEND_MESSAGE.
                            case SEND_MESSAGE:
                                SeabirdChatIngest.SendMessageChatRequest req = event.getSendMessage();

                                Text text = new TranslatableText("chat.type.announcement", "seabird", req.getText());
                                Packet<ClientPlayPacketListener> packet = new GameMessageS2CPacket(text, MessageType.CHAT, new UUID(0, 0));
                                plugin.server.getPlayerManager().sendToAll(packet);

                                success = true;
                                break;
                            default:
                                LOGGER.warn("Unknown or unsupported request type");
                        }

                        // If the event needed a response, make sure we respond.
                        String id = event.getId();
                        if (!id.equals("")) {
                            if (success) {
                                plugin.outgoingQueue.push(SeabirdChatIngest.ChatEvent.newBuilder().setId(id).setSuccess(SeabirdChatIngest.SuccessChatEvent.newBuilder()).build());
                            } else {
                                plugin.outgoingQueue.push(SeabirdChatIngest.ChatEvent.newBuilder().setId(id).setFailed(SeabirdChatIngest.FailedChatEvent.newBuilder()).build());
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        plugin.outgoingQueue.push(t);
                    }

                    @Override
                    public void onCompleted() {
                        plugin.outgoingQueue.push(new Error("seabird-core ended the stream"));
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
                        Object event = this.outgoingQueue.take();

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
                LOGGER.warn("Exception while handling gRPC connection, restarting connection in 1 second: {}", e);
                SeabirdMod.sleepNoFail(1000);
            }
        }
    }

    private void readConfig() {
        try {
            Gson gson = new Gson();

            Path configPath = Paths.get(FabricLoader.getInstance().getConfigDirectory().getAbsolutePath(), "seabird-minecraft-backend.json");

            Config config = gson.fromJson(new FileReader(configPath.toString()), Config.class);

            if (config.seabirdHost == null) {
                config.seabirdHost = "localhost";
            }
            if (config.seabirdPort == 0) {
                config.seabirdPort = 11235;
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

            this.config = config;
        } catch (Exception e) {
            LOGGER.fatal("failed to load config: {}", e);
        }
    }

    public boolean setServer(Object server) {
        if (server == null) {
            return false;
        }

        if (!(server instanceof MinecraftDedicatedServer)) {
            LOGGER.fatal("seabird-minecraft-backend only runs on the server: got type {}", server.getClass().getName());
            return false;
        }

        this.server = (MinecraftDedicatedServer) server;
        return true;
    }

    static void sleepNoFail(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception e) {
            LOGGER.warn("Exception while sleeping: %s", e);
        }
    }
}
