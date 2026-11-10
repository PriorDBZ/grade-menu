package fr.grademenu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class GradeTest {
	@Test
	void variantesAcceptees() {
		assertEquals(Optional.of(Grade.CONFIRME), Grade.parse("confirmé"));
		assertEquals(Optional.of(Grade.CONFIRME), Grade.parse("Confirmée"));
		assertEquals(Optional.of(Grade.JOUEUR), Grade.parse("joueuse"));
		assertEquals(Optional.of(Grade.PREMIUM), Grade.parse("PREMIUM"));
		assertEquals(Optional.empty(), Grade.parse("vip"));
		assertEquals(Optional.empty(), Grade.parse(""));
	}

	@Test
	void boutonsParGrade() {
		assertTrue(Action.availableFor(Grade.JOUEUR).isEmpty());
		assertEquals(List.of(Action.HOME, Action.HAT, Action.CRAFT), Action.availableFor(Grade.CONFIRME));
		assertEquals(11, Action.availableFor(Grade.PREMIUM).size());
	}

	@Test
	void monLitVaAuLit() {
		assertEquals("home bed", Action.HOME.command);
	}

	@Test
	void pasDeTrash() {
		for (Action action : Action.values()) {
			assertFalse("trash".equals(action.command) || "trash".equals(action.icon),
					"/trash ne doit jamais être un bouton");
		}
	}
}
