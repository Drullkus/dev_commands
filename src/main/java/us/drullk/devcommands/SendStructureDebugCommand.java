package us.drullk.devcommands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.StructuresDebugPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SendStructureDebugCommand {
	static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("structure_vision").requires(p -> p.isPlayer() && p.hasPermission(2)).executes(SendStructureDebugCommand::defaultStructureVision).then(
				Commands.argument("structure", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE)).executes(SendStructureDebugCommand::parametricStructureVision)
		));
	}

	private static final DynamicCommandExceptionType ERROR_STRUCTURE_INVALID = new DynamicCommandExceptionType(
			info -> Component.translatableEscape("commands.locate.structure.invalid", info)
	);

	private static int defaultStructureVision(CommandContext<CommandSourceStack> context) {
		ServerPlayer player = context.getSource().getPlayer();

		if (player != null) {
			return visualizeNearestStructure(player, player.serverLevel(), context.getSource());
		}

		context.getSource().sendFailure(Component.literal("Bad source to send Structure Debug data"));

		return 0;
	}

	private static int parametricStructureVision(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayer();

		ResourceOrTagKeyArgument.Result<Structure> structureKeyParam = ResourceOrTagKeyArgument.getResourceOrTagKey(context, "structure", Registries.STRUCTURE, ERROR_STRUCTURE_INVALID);

		if (player != null) {
			return visualizeFilteredStructure(structureKeyParam, player, player.serverLevel(), context.getSource());
		}

		context.getSource().sendFailure(Component.literal("Bad source to send Structure Debug data"));

		return 0;
	}

	private static int visualizeNearestStructure(ServerPlayer player, ServerLevel serverLevel, CommandSourceStack source) {
		return sendNearestStructureVisualized(player, serverLevel, source, s -> true);
	}

	private static int visualizeFilteredStructure(ResourceOrTagKeyArgument.Result<Structure> structureKeyArg, ServerPlayer player, ServerLevel serverLevel, CommandSourceStack source) {
		// Convert the structure argument into a stream of structure holders. It may: not exist, be singular, or be multiple.
		final Registry<Structure> structureRegistry = serverLevel.registryAccess().registryOrThrow(Registries.STRUCTURE);
		Stream<Holder<Structure>> flattenedHolderStream = structureKeyArg
				.unwrap()
				.map(key -> structureRegistry.getHolder(key).map(HolderSet::direct), structureRegistry::getTag)
				.stream()
				.flatMap(HolderSet.ListBacked::stream);

		// Unbox nonempty holders into a set
		Set<Structure> structureFilterSet = flattenedHolderStream.filter(Holder::isBound).map(Holder::value).collect(Collectors.toUnmodifiableSet());
		// The set is for filtering relevant structureStarts that overlap the player's chunk
		return sendNearestStructureVisualized(player, serverLevel, source, structureFilterSet::contains);
	}

	private static int sendNearestStructureVisualized(ServerPlayer player, ServerLevel serverLevel, CommandSourceStack source, Predicate<Structure> filter) {
		List<StructureStart> structureStarts = serverLevel.structureManager().startsForStructure(player.chunkPosition(), filter);

		final BlockPos playerPos = player.blockPosition();
		// Is the player actually inside the structures' maximum bounds?
		Stream<StructureStart> playerOverlappedStructures = structureStarts.stream().filter(s -> s.getBoundingBox().isInside(playerPos));

		// Sort for closest structure center, if there are multiple
		Optional<StructureStart> closestPossibleStart = playerOverlappedStructures.min(Comparator.comparingDouble(start -> playerPos.distSqr(start.getBoundingBox().getCenter())));

		if (closestPossibleStart.isPresent()) {
			StructureStart structureStart = closestPossibleStart.get();

			List<StructuresDebugPayload.PieceInfo> pieces = structureStart.getPieces().stream().map(SendStructureDebugCommand::convertPiece).toList();

			if (pieces.isEmpty()) {
				source.sendFailure(Component.literal("Structure has no pieces!"));

				return 0;
			}

			source.sendSuccess(() -> Component.literal("Visualizing " + structureStart.getStructure()), false);

			player.connection.send(new ClientboundCustomPayloadPacket(new StructuresDebugPayload(serverLevel.dimension(), structureStart.getBoundingBox(), pieces)));

			return pieces.size();
		}

		source.sendFailure(Component.literal("Not currently inside a structure"));

		return 0;
	}

	private static StructuresDebugPayload.PieceInfo convertPiece(StructurePiece structurePiece) {
		return new StructuresDebugPayload.PieceInfo(structurePiece.getBoundingBox(), false);
	}

	private SendStructureDebugCommand() {}
}
