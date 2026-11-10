package fr.grademenu;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Décale les boutons du mod autour de l'inventaire (commande locale, enregistrée dans la config).
 * <ul>
 *   <li>/grade-position &lt;x&gt; &lt;y&gt; : x pixels vers la droite (négatif = gauche),
 *       y pixels vers le haut (négatif = bas), par rapport à la place d'origine ;</li>
 *   <li>/grade-position reset : retour à la place d'origine ;</li>
 *   <li>/grade-position : affiche le décalage actuel.</li>
 * </ul>
 */
public final class PositionCommand {
	private static final int LIMIT = 2000;

	private PositionCommand() {
	}

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(literal("grade-position")
				.executes(PositionCommand::show)
				.then(literal("reset").executes(context -> set(context, 0, 0)))
				.then(argument("x", IntegerArgumentType.integer(-LIMIT, LIMIT))
						.then(argument("y", IntegerArgumentType.integer(-LIMIT, LIMIT))
								.executes(context -> set(context,
										IntegerArgumentType.getInteger(context, "x"),
										IntegerArgumentType.getInteger(context, "y"))))));
	}

	private static int show(CommandContext<FabricClientCommandSource> context) {
		context.getSource().sendFeedback(prefix()
				.append(Component.literal("décalage actuel : " + describe(Config.offsetX(), Config.offsetY()))
						.withStyle(ChatFormatting.WHITE))
				.append(Component.literal("\n/grade-position <x> <y> : x vers la droite, y vers le haut (en pixels)"
						+ "\n/grade-position reset : place d'origine").withStyle(ChatFormatting.GRAY)));
		return 1;
	}

	private static int set(CommandContext<FabricClientCommandSource> context, int x, int y) {
		Config.setOffset(x, y);
		context.getSource().sendFeedback(prefix()
				.append(Component.literal(x == 0 && y == 0
						? "boutons remis à leur place d'origine."
						: "boutons décalés : " + describe(x, y) + ".").withStyle(ChatFormatting.WHITE))
				.append(Component.literal(" Ouvre ton inventaire pour voir.").withStyle(ChatFormatting.GRAY)));
		return 1;
	}

	/** « 10 vers la droite, 3 vers le haut », ou « aucun ». */
	static String describe(int x, int y) {
		if (x == 0 && y == 0) {
			return "aucun (place d'origine)";
		}
		StringBuilder text = new StringBuilder();
		if (x != 0) {
			text.append(Math.abs(x)).append(x > 0 ? " vers la droite" : " vers la gauche");
		}
		if (y != 0) {
			if (!text.isEmpty()) {
				text.append(", ");
			}
			text.append(Math.abs(y)).append(y > 0 ? " vers le haut" : " vers le bas");
		}
		return text.toString();
	}

	private static MutableComponent prefix() {
		return Component.literal("[Grade Menu] ").withStyle(ChatFormatting.GOLD);
	}
}
