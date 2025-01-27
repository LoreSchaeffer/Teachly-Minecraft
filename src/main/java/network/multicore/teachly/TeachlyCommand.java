package network.multicore.teachly;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.dejvokep.boostedyaml.YamlDocument;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import network.multicore.mbcore.Text;
import org.bukkit.command.CommandSender;

public class TeachlyCommand {
    public static final LiteralCommandNode<CommandSourceStack> command = Commands.literal("teachly")
            .then(Commands.literal("reload").executes(ctx -> {
                Teachly plugin = Teachly.getInstance();
                YamlDocument config = plugin.config();

                CommandSender sender = ctx.getSource().getSender();
                if (!sender.hasPermission("teachly.reload")) {
                    Text.send(config.getString("messages.insufficient-perms"), sender);
                    return Command.SINGLE_SUCCESS;
                }

                plugin.reload();

                Text.send(config.getString("messages.reload-success"), sender);

                return Command.SINGLE_SUCCESS;
            }))
            .build();
}
