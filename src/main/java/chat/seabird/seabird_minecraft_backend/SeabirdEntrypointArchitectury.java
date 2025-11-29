package chat.seabird.seabird_minecraft_backend;

import chat.seabird.seabird_minecraft_backend.events.*;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.ChatEvent;
import dev.architectury.event.events.common.CommandPerformEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class SeabirdEntrypointArchitectury {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void init() {
        LOGGER.info("Bootstrapping SeabirdMod from Architectury");
        SeabirdMod.init();

        CommandPerformEvent.EVENT.register((commandPerformEvent) -> {
            ParseResults results = commandPerformEvent.getResults();
            CommandContextBuilder<CommandSourceStack> ctx = results.getContext();

            List<ParsedCommandNode<CommandSourceStack>> nodes = ctx.getNodes();

            // Normally we'd check for 1, but because what we need only has 2 arguments (a literal and a command argument),
            // we can take a shortcut.
            if (nodes.size() < 2) {
                return EventResult.pass();
            }

            CommandSourceStack src = ctx.getSource();

            String commandName = nodes.get(0).getNode().getName();

            // Extract arguments from nodes (starting from index 1)
            List<String> arguments = new java.util.ArrayList<>();
            for (int i = 1; i < nodes.size(); i++) {
                String arg = nodes.get(i).getRange().get(results.getReader());
                arguments.add(arg);
            }

            // Try to get player information if the entity is a player
            String playerId = null;
            String playerName = null;
            Entity entity = src.getEntity();
            if (entity instanceof ServerPlayer player) {
                playerId = player.getStringUUID();
                playerName = player.getGameProfile().getName();
            }

            String senderDisplayName = src.getDisplayName().getString();

            CommandEvent event = new CommandEvent(commandName, arguments, playerId, playerName, senderDisplayName);
            SeabirdMod.onCommand(event);
            return EventResult.pass();
        });

        PlayerEvent.PLAYER_JOIN.register((player) -> {
            String playerName = player.getGameProfile().getName();
            PlayerJoinedEvent event = new PlayerJoinedEvent(playerName);
            SeabirdMod.onPlayerJoined(event);
        });
        PlayerEvent.PLAYER_QUIT.register((player) -> {
            String playerName = player.getGameProfile().getName();
            PlayerLeftEvent event = new PlayerLeftEvent(playerName);
            SeabirdMod.onPlayerLeft(event);
        });
        PlayerEvent.PLAYER_ADVANCEMENT.register((player, advancementHolder) -> {
            Advancement advancement = advancementHolder.value();

            // Recipes are reported as advancements, but we don't care about them - the easiest way to check for them is to
            // look for an id starting with minecraft:recipes/ or check if the DisplayInfo is null.
            //
            // Additionally, we need to make sure this is something we should announce in chat, otherwise VanillaTweaks
            // displays empty advancements all the time.
            Component advancementName = advancement.name().orElse(null);
            DisplayInfo display = advancement.display().orElse(null);

            boolean shouldAnnounce = display != null && display.shouldAnnounceChat();
            if (!shouldAnnounce || advancementName == null) {
                return;
            }

            String playerName = player.getGameProfile().getName();
            String advancementNameStr = advancementName.getString();

            AdvancementEvent event = new AdvancementEvent(playerName, advancementNameStr);
            SeabirdMod.onAdvancement(event);
        });

        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (entity instanceof Player) {
                Component message = source.getLocalizedDeathMessage(entity);
                PlayerDiedEvent event = new PlayerDiedEvent(message.getString());
                SeabirdMod.onPlayerDied(event);
            }
            return EventResult.pass();
        });

        ChatEvent.RECEIVED.register((player, component) -> {
            String playerId = player.getStringUUID();
            String playerName = player.getGameProfile().getName();
            String messageText = component.getString();
            MessageReceivedEvent event = new MessageReceivedEvent(playerId, playerName, messageText);
            SeabirdMod.onMessage(event);
            return EventResult.pass();
        });
    }
}
