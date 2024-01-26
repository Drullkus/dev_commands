package us.drullk.devcommands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(DevCommandsNeoForged.MOD_ID)
public class DevCommandsNeoForged {
    public static final String MOD_ID = "dev_commands";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DevCommandsNeoForged() {
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
    }

    public void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("speed").requires(p -> p.hasPermission(2)).then(Commands.argument("fly_speed", FloatArgumentType.floatArg(0.0000001f, 20)).executes(DevCommandsNeoForged::runFlySpeed)));

        DiscardEntitiesCommand.register(dispatcher);
        SendStructureDebugCommand.register(dispatcher);
    }

    private static int runFlySpeed(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        // Default flying speed is 0.05f or 1/20, normalize value accordingly
        player.getAbilities().setFlyingSpeed(FloatArgumentType.getFloat(context, "fly_speed") * 0.05f);
        // Send updated value to client
        player.onUpdateAbilities();

        return 1;
    }
}
