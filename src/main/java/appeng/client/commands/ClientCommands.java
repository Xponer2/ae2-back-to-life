package appeng.client.commands;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.ClientCommandHandler;

import appeng.core.AEConfig;

/**
 * The client side {@code /ae2client} command. Its debug sub-commands are only available while the debug tools are
 * enabled in the config.
 */
public final class ClientCommands extends CommandBase {

    public static final Map<String, DebugCommand> DEBUG_COMMANDS = ImmutableMap.of(
            "highlight_gui_areas", ClientCommands::highlightGuiAreas);

    @FunctionalInterface
    public interface DebugCommand {
        void run(ICommandSender sender);
    }

    private ClientCommands() {
    }

    /**
     * Registers the command with the client command handler of Forge.
     */
    public static void register() {
        ClientCommandHandler.instance.registerCommand(new ClientCommands());
    }

    private static void highlightGuiAreas(ICommandSender sender) {
        var toggle = !AEConfig.instance().isShowDebugGuiOverlays();
        AEConfig.instance().setShowDebugGuiOverlays(toggle);
        AEConfig.instance().save();
        sender.sendMessage(new TextComponentString("GUI Overlays: " + toggle));
    }

    @Override
    public String getName() {
        return "ae2client";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/ae2client <" + String.join("|", DEBUG_COMMANDS.keySet()) + ">";
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        // Only runs on the client of the player
        return true;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws WrongUsageException {
        DebugCommand command = args.length == 1 && AEConfig.instance().isDebugToolsEnabled()
                ? DEBUG_COMMANDS.get(args[0])
                : null;
        if (command == null) {
            throw new WrongUsageException(getUsage(sender));
        }
        command.run(sender);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
            @Nullable BlockPos targetPos) {
        if (args.length == 1 && AEConfig.instance().isDebugToolsEnabled()) {
            return getListOfStringsMatchingLastWord(args, DEBUG_COMMANDS.keySet());
        }
        return Collections.emptyList();
    }
}
