package fr.grademenu;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

/**
 * Masquer / afficher des boutons (commande locale, enregistrée dans la config) :
 * <ul>
 *   <li>/grade-bouton : liste des boutons, avec [masquer] / [afficher] cliquables ;</li>
 *   <li>/grade-bouton &lt;bouton&gt; off|on : masque ou ré-affiche un bouton ;</li>
 *   <li>/grade-bouton &lt;bouton&gt; : bascule ;</li>
 *   <li>/grade-bouton reset : ré-affiche tous les boutons.</li>
 * </ul>
 * Le réglage ne dépend pas du grade : un bouton masqué reste masqué quand on change de grade.
 */
public final class ButtonCommand {
	private static final String NAME = "grade-bouton";

	private ButtonCommand() {
	}

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		LiteralArgumentBuilder<FabricClientCommandSource> root = literal(NAME)
				.executes(ButtonCommand::list)
				.then(literal("reset").executes(ButtonCommand::reset));
		for (Action action : Action.values()) {
			root.then(literal(action.id)
					.executes(context -> set(context, action, !Config.isDisabled(action)))
					.then(literal("off").executes(context -> set(context, action, true)))
					.then(literal("on").executes(context -> set(context, action, false))));
		}
		dispatcher.register(root);
	}

	private static int list(CommandContext<FabricClientCommandSource> context) {
		Grade grade = Config.grade();
		List<Action> available = Action.availableFor(grade);
		MutableComponent text = prefix()
				.append(Component.literal("boutons (grade " + grade.label + ") :").withStyle(ChatFormatting.WHITE));
		for (Action action : Action.values()) {
			boolean off = Config.isDisabled(action);
			text.append("\n ")
					.append(Component.literal(off ? "✘ " : "✔ ").withStyle(off ? ChatFormatting.RED : ChatFormatting.GREEN))
					.append(Component.literal(action.title).withStyle(off ? ChatFormatting.GRAY : ChatFormatting.WHITE))
					.append(Component.literal(" (" + action.id + ")").withStyle(ChatFormatting.DARK_GRAY));
			if (!available.contains(action)) {
				text.append(Component.literal(" · " + action.minGrade.label).withStyle(ChatFormatting.DARK_GRAY));
			}
			text.append(" ").append(toggleLink(action, off));
		}
		text.append(Component.literal("\n/" + NAME + " <bouton> on|off · /" + NAME + " reset")
				.withStyle(ChatFormatting.GRAY));
		context.getSource().sendFeedback(text);
		return 1;
	}

	/** Lien cliquable [masquer] / [afficher] qui lance la commande correspondante. */
	private static Component toggleLink(Action action, boolean currentlyOff) {
		String command = "/" + NAME + " " + action.id + (currentlyOff ? " on" : " off");
		String label = currentlyOff ? "[afficher]" : "[masquer]";
		return Component.literal(label).withStyle(style -> style
				.withColor(currentlyOff ? ChatFormatting.GREEN : ChatFormatting.GOLD)
				.withClickEvent(new ClickEvent.RunCommand(command))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal(command))));
	}

	private static int set(CommandContext<FabricClientCommandSource> context, Action action, boolean off) {
		Config.setDisabled(action, off);
		context.getSource().sendFeedback(prefix()
				.append(Component.literal("« " + action.title + " » " + (off ? "masqué" : "affiché") + ".")
						.withStyle(ChatFormatting.WHITE))
				.append(Component.literal(" Gardé même si tu changes de grade.").withStyle(ChatFormatting.GRAY)));
		return 1;
	}

	private static int reset(CommandContext<FabricClientCommandSource> context) {
		Config.enableAll();
		context.getSource().sendFeedback(prefix()
				.append(Component.literal("tous les boutons sont de nouveau affichés.").withStyle(ChatFormatting.WHITE)));
		return 1;
	}

	private static MutableComponent prefix() {
		return Component.literal("[Grade Menu] ").withStyle(ChatFormatting.GOLD);
	}
}
