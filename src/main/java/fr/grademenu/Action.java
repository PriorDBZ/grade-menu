package fr.grademenu;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Boutons affichés à côté de l'inventaire, dans l'ordre d'affichage.
 * /trash n'existe volontairement pas : trop dangereux depuis l'inventaire.
 */
public enum Action {
	HOME("home", "Mon lit", "Téléportation à ton lit", "home bed", Grade.CONFIRME),
	HOMES("homes", "Mes homes", "Ouvre ou ferme le menu des homes", null, Grade.PREMIUM),
	SPAWN("spawn", "Spawn", "Téléportation au spawn", "spawn", Grade.PREMIUM),
	HAT("hat", "Chapeau", "Met l'objet tenu sur ta tête", "hat", Grade.CONFIRME),
	SKULL("skull", "Ma tête", "Te donne ta tête", "skull", Grade.PREMIUM),
	BLOCKS("blocks", "Compression", "Compresse minerais et lingots en blocs", "blocks", Grade.PREMIUM),
	CRAFT("craft", "Table de craft", "Ouvre une table de craft", "craft", Grade.CONFIRME),
	CARTOGRAPHY("cartographytable", "Table de cartographie", "Ouvre une table de cartographie",
			"cartographytable", Grade.PREMIUM),
	LOOM("loom", "Métier à tisser", "Ouvre un métier à tisser", "loom", Grade.PREMIUM),
	SMITHING("smithingtable", "Table de forge", "Ouvre une table de forge", "smithingtable", Grade.PREMIUM),
	STONECUTTER("stonecutter", "Tailleur de pierre", "Ouvre un tailleur de pierre", "stonecutter", Grade.PREMIUM);

	/** Identifiant (utilisé par /grade-bouton et dans la config) = nom du sprite de l'icône. */
	public final String id;
	/** Nom du sprite de l'icône (assets/grademenu/textures/gui/sprites/icon/&lt;icon&gt;.png). */
	public final String icon;
	public final String title;
	public final String description;
	/** Commande envoyée au serveur, sans « / » ; null pour le menu des homes. */
	public final String command;
	public final Grade minGrade;

	Action(String icon, String title, String description, String command, Grade minGrade) {
		this.id = icon;
		this.icon = icon;
		this.title = title;
		this.description = description;
		this.command = command;
		this.minGrade = minGrade;
	}

	/** Boutons auxquels le grade donne droit. */
	public static List<Action> availableFor(Grade grade) {
		return Arrays.stream(values()).filter(a -> grade.includes(a.minGrade)).toList();
	}

	/** Boutons à afficher : ceux du grade, sauf ceux masqués avec /grade-bouton (sans trou, dans l'ordre). */
	public static List<Action> shownFor(Grade grade) {
		return availableFor(grade).stream().filter(a -> !Config.isDisabled(a)).toList();
	}

	public static Optional<Action> byId(String id) {
		return Arrays.stream(values()).filter(a -> a.id.equalsIgnoreCase(id)).findFirst();
	}
}
