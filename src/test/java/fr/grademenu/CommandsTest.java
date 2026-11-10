package fr.grademenu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

class CommandsTest {
	@Test
	void toutesLesCommandesCommencentParGrade() {
		CommandDispatcher<FabricClientCommandSource> dispatcher = new CommandDispatcher<>();
		GradeMenuClient.registerCommands(dispatcher);
		Set<String> names = new TreeSet<>();
		for (CommandNode<FabricClientCommandSource> node : dispatcher.getRoot().getChildren()) {
			names.add(node.getName());
		}
		assertEquals(Set.of("grade-bouton", "grade-position", "grade-set"), names);
		names.forEach(name -> assertTrue(name.startsWith("grade-"), name));
	}
}
