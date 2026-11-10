package fr.grademenu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.network.chat.Component;

class HomesTest {
	@BeforeEach
	void reset() {
		Homes.reset();
	}

	@Test
	void parseMetLeLitEnPremierPuisOrdreAlphabetique() {
		assertEquals(List.of("bed", "base", "mizuland", "redtop"), Homes.parse(" redtop, mizuland, base, bed"));
	}

	@Test
	void parseIgnoreLesMorceauxVides() {
		assertEquals(List.of("bed"), Homes.parse(" bed, ,"));
	}

	@Test
	void reponseDemandeeParLeModEstLueEtCachee() {
		forceLoading();
		boolean shown = Homes.onGameMessage(Component.literal("Homes: redtop, mizuland, base, bed"), false);
		assertFalse(shown, "la réponse doit être cachée du chat");
		assertEquals(Homes.State.LOADED, Homes.state());
		assertEquals(List.of("bed", "base", "mizuland", "redtop"), Homes.names());
	}

	@Test
	void codesCouleurRetires() {
		forceLoading();
		assertFalse(Homes.onGameMessage(Component.literal("§6Homes: §rbase, §cbed"), false));
		assertEquals(List.of("bed", "base"), Homes.names());
	}

	@Test
	void reponseNonDemandeeResteDansLeChat() {
		assertTrue(Homes.onGameMessage(Component.literal("Homes: base, bed"), false));
		assertTrue(Homes.names().isEmpty());
	}

	@Test
	void autresMessagesNonTouches() {
		forceLoading();
		assertTrue(Homes.onGameMessage(Component.literal("<Bob> Homes: lol"), false));
		assertTrue(Homes.onGameMessage(Component.literal("Bienvenue !"), false));
		assertTrue(Homes.onGameMessage(Component.literal("Homes: base"), true), "barre d'action ignorée");
		assertEquals(Homes.State.LOADING, Homes.state());
	}

	@Test
	void nomsValides() {
		assertTrue(Homes.VALID_NAME.matcher("base_2-nord").matches());
		assertFalse(Homes.VALID_NAME.matcher("ma base").matches());
		assertFalse(Homes.VALID_NAME.matcher("maïs").matches());
		assertFalse(Homes.VALID_NAME.matcher("").matches());
	}

	/** Simule « le mod a envoyé /homes et attend la réponse » sans connexion. */
	private static void forceLoading() {
		try {
			var field = Homes.class.getDeclaredField("state");
			field.setAccessible(true);
			field.set(null, Homes.State.LOADING);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}
}
