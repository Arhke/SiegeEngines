package com.github.arhke.siegeengines.command;

import com.github.arhke.siegeengines.reinforcement.ReinforcementView;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.List;

public class ReinforcementViewCommand {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static void register(Commands commands) {
        commands.register(Commands.literal("reinforcementview")
            .requires(source -> source.getSender().hasPermission("siegeengines.reinforcement.view") && source.getExecutor() instanceof Player)
            .executes(ReinforcementViewCommand::onExecute)
            .build(), "Toggles seeing how many breaks damaged blocks have left.", List.of("rv"));
    }

    private static int onExecute(CommandContext<CommandSourceStack> ctx) {
        final Player player = (Player) ctx.getSource().getExecutor();
        if (ReinforcementView.toggle(player)) {
            player.sendMessage(MM.deserialize("<yellow>[Reinforcement]</yellow> Visibility mode <green>on"));
        } else {
            player.sendMessage(MM.deserialize("<yellow>[Reinforcement]</yellow> Visibility mode <red>off"));
        }
        return Command.SINGLE_SUCCESS;
    }
}
