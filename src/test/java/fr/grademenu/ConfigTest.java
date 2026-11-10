package fr.grademenu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigTest {
	@TempDir
	Path dir;

	@Test
	void gradeEtDecalageConservesApresRechargement() throws Exception {
		Path file = dir.resolve("grademenu.properties");
		Config.set(Grade.PREMIUM, 10, 3);
		Config.save(file);

		Config.set(Grade.JOUEUR, 0, 0); // « redémarrage »
		Config.load(file);

		assertEquals(Grade.PREMIUM, Config.grade());
		assertEquals(10, Config.offsetX());
		assertEquals(3, Config.offsetY());
		String content = Files.readString(file);
		assertEquals(true, content.contains("decalage_x=10") && content.contains("decalage_y=3"), content);
	}

	@Test
	void boutonMasqueResteMasqueEnChangeantDeGradeEtApresRedemarrage() {
		Path file = dir.resolve("grademenu.properties");
		Config.set(Grade.CONFIRME, 0, 0);
		Config.setDisabledInMemory(Action.CRAFT, true);
		assertEquals(List.of(Action.HOME, Action.HAT), Action.shownFor(Grade.CONFIRME));

		Config.set(Grade.PREMIUM, 0, 0);          // efface tout en mémoire...
		Config.setDisabledInMemory(Action.CRAFT, true);
		Config.save(file);
		Config.set(Grade.JOUEUR, 0, 0);           // ... « redémarrage »
		Config.load(file);

		assertEquals(true, Config.isDisabled(Action.CRAFT));
		List<Action> premium = Action.shownFor(Grade.PREMIUM);
		assertEquals(10, premium.size());
		assertEquals(false, premium.contains(Action.CRAFT));
		// sans trou : le bouton suivant prend la place
		assertEquals(Action.CARTOGRAPHY, premium.get(6));
		assertEquals(List.of(Action.HOME, Action.HAT), Action.shownFor(Grade.CONFIRME));
	}

	@Test
	void boutonInconnuDansLaConfigIgnore() throws Exception {
		Path file = dir.resolve("grademenu.properties");
		Files.writeString(file, "grade=premium\nboutons_masques=craft, trash ,inconnu,loom\n");
		Config.set(Grade.JOUEUR, 0, 0);
		Config.load(file);
		assertEquals(true, Config.isDisabled(Action.CRAFT));
		assertEquals(true, Config.isDisabled(Action.LOOM));
		assertEquals(9, Action.shownFor(Grade.PREMIUM).size());
	}

	@Test
	void valeursAbimeesIgnorees() throws Exception {
		Path file = dir.resolve("grademenu.properties");
		Files.writeString(file, "grade=premium\ndecalage_x=abc\n");
		Config.set(Grade.JOUEUR, 5, 5);
		Config.load(file);
		assertEquals(Grade.PREMIUM, Config.grade());
		assertEquals(0, Config.offsetX());
		assertEquals(0, Config.offsetY());
	}

	@Test
	void descriptionDuDecalage() {
		assertEquals("10 vers la droite, 3 vers le haut", PositionCommand.describe(10, 3));
		assertEquals("4 vers la gauche, 2 vers le bas", PositionCommand.describe(-4, -2));
		assertEquals("7 vers le haut", PositionCommand.describe(0, 7));
		assertEquals("aucun (place d'origine)", PositionCommand.describe(0, 0));
	}
}
