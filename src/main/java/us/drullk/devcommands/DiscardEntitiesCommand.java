package us.drullk.devcommands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTypeTest;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.function.Predicate;

public final class DiscardEntitiesCommand {
    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Kills all mobs except for players, because typing out /kill @e[type=!player] gets fucking tiring
        dispatcher.register(Commands.literal("kill_mobs").requires(p -> p.hasPermission(2)).executes(DiscardEntitiesCommand::discardAllMobs).then(Commands.argument("mobs", EntityArgument.entities()).executes(DiscardEntitiesCommand::discardSelectedMobs)));
        dispatcher.register(Commands.literal("km").requires(p -> p.hasPermission(2)).executes(DiscardEntitiesCommand::discardAllMobs).then(Commands.argument("mobs", EntityArgument.entities()).executes(DiscardEntitiesCommand::discardSelectedMobs)));

        // Variant of above command, except actually targets all entities (except players). Useful if to really kill all entities including minecarts, paintings, etc. Except players, of course.
        dispatcher.register(Commands.literal("kill_entities").requires(p -> p.hasPermission(2)).executes(DiscardEntitiesCommand::discardAllEntities).then(Commands.argument("entities", EntityArgument.entities()).executes(DiscardEntitiesCommand::discardSelectedEntities)));
        dispatcher.register(Commands.literal("ke").requires(p -> p.hasPermission(2)).executes(DiscardEntitiesCommand::discardAllEntities).then(Commands.argument("entities", EntityArgument.entities()).executes(DiscardEntitiesCommand::discardSelectedEntities)));
    }

    private static int discardSelectedEntities(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return discardEntities(context, e -> !(e instanceof Player), "entities", EntityArgument.getOptionalEntities(context, "entities"));
    }

    private static int discardAllEntities(CommandContext<CommandSourceStack> context) {
        return discardEntities(context, e -> !(e instanceof Player), "entities", allEntities(context));
    }

    private static int discardSelectedMobs(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return discardEntities(context, e -> e instanceof Mob, "mobs", EntityArgument.getOptionalEntities(context, "mobs"));
    }

    private static int discardAllMobs(CommandContext<CommandSourceStack> context) {
        return discardEntities(context, e -> e instanceof Mob, "mobs", allEntities(context));
    }

    private static Iterable<? extends Entity> allEntities(CommandContext<CommandSourceStack> context) {
        return context.getSource().getLevel().getEntities(EntityTypeTest.forClass(Entity.class), e -> true);
    }

    private static int discardEntities(CommandContext<CommandSourceStack> context, Predicate<Entity> doNotKill, String label, Iterable<? extends Entity> entities) {
        int mobsDiscarded = discardEntities(doNotKill, entities);

        if (mobsDiscarded > 0) {
            context.getSource().sendSuccess(() -> Component.literal("Discarded " + mobsDiscarded + " " + label), true);
        } else {
            context.getSource().sendFailure(Component.literal("No " + label + " matched filters, none discarded"));
        }

        return Mth.clamp(mobsDiscarded, 0, 1);
    }

    // Hardcoded filter acts as a secondary fallback, to match intention of command used
    private static int discardEntities(Predicate<Entity> hardcodedFilter, Iterable<? extends Entity> entities) {
        MutableInt count = new MutableInt(0);

        entities.forEach(entity -> {
            if (!hardcodedFilter.test(entity))
                return;

            if (entity.isAlive())
                entity.discard();

            count.increment();
        });

        return count.getValue();
    }
}
