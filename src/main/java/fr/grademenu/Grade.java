package fr.grademenu;

import java.util.Locale;
import java.util.Optional;

/** Niveaux de grade du serveur. Chaque grade débloque aussi ceux d'en dessous. */
public enum Grade {
	JOUEUR("joueur", "Joueur·euse"),
	CONFIRME("confirme", "₪ Confirmé·e"),
	PREMIUM("premium", "★ Premium");

	/** Nom tapé dans /grade-set et enregistré dans la configuration. */
	public final String id;
	/** Nom affiché au joueur. */
	public final String label;

	Grade(String id, String label) {
		this.id = id;
		this.label = label;
	}

	public boolean includes(Grade other) {
		return this.ordinal() >= other.ordinal();
	}

	/** Accepte aussi les variantes (« confirmé », « confirmée », « joueuse »...) : les 4 premières lettres suffisent. */
	public static Optional<Grade> parse(String text) {
		String t = text.trim().toLowerCase(Locale.ROOT).replace('é', 'e');
		if (t.length() < 4) {
			return Optional.empty();
		}
		for (Grade g : values()) {
			if (t.startsWith(g.id.substring(0, 4))) {
				return Optional.of(g);
			}
		}
		return Optional.empty();
	}
}
