package com.github.arhke.siegeengines.command;

import com.github.arhke.siegeengines.SiegeEngine;
import com.github.arhke.siegeengines.config.Config;
import com.github.arhke.siegeengines.util.SiegeEnginesUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static com.github.arhke.siegeengines.SiegeEngines.*;

public class SiegeEnginesCommand {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static void register(Commands commands) {
        LiteralCommandNode<CommandSourceStack> root = Commands.literal("siegeengines")
            .requires(source -> source.getSender().hasPermission("siegeengines.command") && source.getExecutor() instanceof Player)
            .executes(SiegeEnginesCommand::onExecute)
            .then(commandGet())
            .then(Commands.literal("getAll")
                .requires(source -> source.getSender().hasPermission("siegeengines.command.getAll"))
                .executes(SiegeEnginesCommand::onGetAll))
            .then(Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission("siegeengines.command.reload"))
                .executes(SiegeEnginesCommand::onReload))
            .build();

        commands.register(root, "Give siege engines or reload the configuration.");
    }

    private static int onExecute(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().getSender().sendMessage(MM.deserialize("<yellow>Incorrect usage, /siegeengines get, /siegeengines getAll, /siegeengines reload"));
        return Command.SINGLE_SUCCESS;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> commandGet() {
        return Commands.literal("get")
            .requires(source -> source.getSender().hasPermission("siegeengines.command.get"))
            .then(Commands.argument("equipmentid", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    definedSiegeEngines.values().stream().map(SiegeEngine::getId).forEach(builder::suggest);
                    return builder.buildFuture();
                })
                .executes(ctx -> onGet(ctx, null))
                .then(Commands.argument("target", ArgumentTypes.player())
                    .executes(ctx -> onGet(ctx, ctx.getArgument("target", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst()))));
    }

    private static int onGet(CommandContext<CommandSourceStack> ctx, Player target) throws CommandSyntaxException {
        final String equipmentId = StringArgumentType.getString(ctx, "equipmentid");
        final Player player = target != null ? target : (Player) ctx.getSource().getExecutor();

        for (SiegeEngine siegeEquipment : definedSiegeEngines.values()) {
            if (siegeEquipment.equals(equipmentId)) {
                giveSiegeEngine(player, siegeEquipment);
                return Command.SINGLE_SUCCESS;
            }
        }

        ctx.getSource().getSender().sendMessage(MM.deserialize("<red>Invalid SiegeEngine id specified."));
        return 0;
    }

    private static int onGetAll(CommandContext<CommandSourceStack> ctx) {
        final Player player = (Player) ctx.getSource().getExecutor();

        for (SiegeEngine i : definedSiegeEngines.values()) {
            giveSiegeEngine(player, i);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int onReload(CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();
        Config.reload();
        activeSiegeEngines.clear();
        siegeEngineEntitiesPerPlayer.clear();
        definedSiegeEngines.clear();
        addDefaults();
        for (SiegeEngine i : definedSiegeEngines.values()) {
            if (Config.doDebug) {
                sender.sendMessage(MM.deserialize("<yellow>Enabled SiegeEngine : <name>", Placeholder.unparsed("name", i.getEngineName())));
                for (ItemStack proj : i.getProjectiles().keySet()) {
                    sender.sendMessage(MM.deserialize("<yellow>SiegeEngine Projectile ItemStacks : <item>", Placeholder.unparsed("item", String.valueOf(proj))));
                }
            }
        }
        sender.sendMessage(MM.deserialize("<yellow>SiegeEngine configs reloaded"));
        return Command.SINGLE_SUCCESS;
    }

    private static void giveSiegeEngine(Player player, SiegeEngine siegeEngine) {
        ItemStack item = new ItemStack(Material.CARVED_PUMPKIN);
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(siegeEngine.getReadyModelNumber());
        meta.displayName(siegeEngine.getItemName());
        meta.lore(siegeEngine.getItemLore());
        item.setItemMeta(meta);
        player.getInventory().addItem(SiegeEnginesUtil.makeUnwearable(item));
    }
}
