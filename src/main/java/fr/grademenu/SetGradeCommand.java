package fr.grademenu;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * /grade-set [joueur|confirme|premium] : commande locale (rien n'est envoyé au serveur)
 * qui choisit quels boutons s'affichent dans l'inventaire.
 */
public final class SetGradeCommand {
	private SetGradeCommand() {
	}

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		LiteralArgumentBuilder<FabricClientCommandSource> root = literal("grade-set").executes(SetGradeCommand::show);
		for (Grade grade : Grade.values()) {
			root.then(literal(grade.id).executes(context -> set(context, grade)));
		}
		dispatcher.register(root);
	}

	private static int show(CommandContext<FabricClientCommandSource> context) {
		context.getSource().sendFeedback(prefix()
				.append(Component.literal("grade actuel : " + Config.grade().label).withStyle(ChatFormatting.WHITE))
				.append(Component.literal("\nUtilisation : /grade-set joueur | confirme | premium")
						.withStyle(ChatFormatting.GRAY)));
		return 1;
	}

	private static int set(CommandContext<FabricClientCommandSource> context, Grade grade) {
		Config.setGrade(grade);
		int buttons = Action.shownFor(grade).size();
		int hidden = Action.availableFor(grade).size() - buttons;
		String detail = buttons == 0
				? "aucun bouton dans l'inventaire."
				: buttons + " boutons dans l'inventaire (sur un serveur).";
		if (hidden > 0) {
			detail += " " + hidden + " masqué" + (hidden > 1 ? "s" : "") + " avec /grade-bouton.";
		}
		context.getSource().sendFeedback(prefix()
				.append(Component.literal("grade réglé sur " + grade.label + " : ").withStyle(ChatFormatting.WHITE))
				.append(Component.literal(detail).withStyle(ChatFormatting.GRAY)));
		return 1;
	}

	private static MutableComponent prefix() {
		return Component.literal("[Grade Menu] ").withStyle(ChatFormatting.GOLD);
	}
}
