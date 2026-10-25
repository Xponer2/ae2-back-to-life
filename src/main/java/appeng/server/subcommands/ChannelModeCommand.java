package appeng.server.subcommands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import appeng.api.networking.pathing.ChannelMode;
import appeng.core.AEConfig;
import appeng.core.AELog;
import appeng.core.localization.PlayerMessages;
import appeng.hooks.ticking.TickHandler;
import appeng.me.Grid;
import appeng.server.AECommand;
import appeng.server.ISubCommand;

/**
 * Command to easily change {@link AEConfig#getChannelMode()} at runtime.
 */
public class ChannelModeCommand implements ISubCommand {
    @Override
    public String getArgumentUsage() {
        return "[" + String.join("|", modeNames()) + "]";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer srv, ICommandSender sender, String[] args,
            @Nullable BlockPos targetPos) {
        return args.length == 1 ? CommandBase.getListOfStringsMatchingLastWord(args, modeNames()) : Collections.emptyList();
    }

    @Override
    public void call(MinecraftServer srv, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            var mode = AEConfig.instance().getChannelMode();
            AECommand.sendSuccess(sender, PlayerMessages.ChannelModeCurrent.text(mode.name().toLowerCase(Locale.ROOT)));
            return;
        }
        for (var mode : ChannelMode.values()) {
            if (mode.name().toLowerCase(Locale.ROOT).equals(args[0])) {
                setChannelMode(sender, mode);
                return;
            }
        }
        throw new WrongUsageException("/ae2 channelmode " + getArgumentUsage());
    }

    private static List<String> modeNames() {
        var names = new ArrayList<String>();
        for (var mode : ChannelMode.values()) {
            names.add(mode.name().toLowerCase(Locale.ROOT));
        }
        return names;
    }

    private void setChannelMode(ICommandSender sender, ChannelMode mode) {
        AELog.info("%s is changing channel mode to %s", sender.getName(), mode);

        AEConfig.instance().setChannelModel(mode);
        AEConfig.instance().save();

        var gridCount = 0;
        for (Grid grid : TickHandler.instance().getGridList()) {
            grid.getPathingService().repath();
            gridCount++;
        }

        var modeName = mode.name().toLowerCase(Locale.ROOT);
        AECommand.sendSuccess(sender, PlayerMessages.ChannelModeSet.text(modeName, gridCount));
    }
}
