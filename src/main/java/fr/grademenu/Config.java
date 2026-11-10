package fr.grademenu;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import net.fabricmc.loader.api.FabricLoader;

/** Réglages du mod, gardés dans config/grademenu.properties (conservés après un redémarrage). */
public final class Config {
	private static Grade grade = Grade.JOUEUR;
	/** Décalage des boutons, en pixels d'interface : x positif = à droite, y positif = vers le haut. */
	private static int offsetX;
	private static int offsetY;
	/**
	 * Boutons masqués (identifiants d'Action). Gardés à part du grade : un bouton masqué
	 * reste masqué quand on change de grade.
	 */
	private static final Set<String> disabled = new TreeSet<>();

	private Config() {
	}

	public static Grade grade() {
		return grade;
	}

	public static void setGrade(Grade newGrade) {
		grade = newGrade;
		save();
	}

	/** Pour les tests : change les valeurs sans écrire le fichier. */
	static void set(Grade newGrade, int x, int y) {
		grade = newGrade;
		offsetX = x;
		offsetY = y;
		disabled.clear();
	}

	public static boolean isDisabled(Action action) {
		return disabled.contains(action.id);
	}

	public static void setDisabled(Action action, boolean off) {
		setDisabledInMemory(action, off);
		save();
	}

	static void setDisabledInMemory(Action action, boolean off) {
		if (off) {
			disabled.add(action.id);
		} else {
			disabled.remove(action.id);
		}
	}

	public static void enableAll() {
		disabled.clear();
		save();
	}

	public static int offsetX() {
		return offsetX;
	}

	public static int offsetY() {
		return offsetY;
	}

	public static void setOffset(int x, int y) {
		offsetX = x;
		offsetY = y;
		save();
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("grademenu.properties");
	}

	public static void load() {
		load(file());
	}

	private static void save() {
		save(file());
	}

	static void load(Path file) {
		if (!Files.exists(file)) {
			return;
		}
		Properties p = new Properties();
		try (Reader r = Files.newBufferedReader(file)) {
			p.load(r);
			grade = Grade.parse(p.getProperty("grade", "")).orElse(Grade.JOUEUR);
			offsetX = parseInt(p.getProperty("decalage_x"));
			offsetY = parseInt(p.getProperty("decalage_y"));
			disabled.clear();
			Arrays.stream(p.getProperty("boutons_masques", "").split(","))
					.map(String::strip)
					.filter(id -> Action.byId(id).isPresent())
					.forEach(disabled::add);
		} catch (IOException e) {
			GradeMenuClient.LOGGER.warn("Lecture de {} impossible", file, e);
		}
	}

	private static int parseInt(String value) {
		if (value == null) {
			return 0;
		}
		try {
			return Integer.parseInt(value.strip());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	static void save(Path file) {
		Properties p = new Properties();
		p.setProperty("grade", grade.id);
		p.setProperty("decalage_x", Integer.toString(offsetX));
		p.setProperty("decalage_y", Integer.toString(offsetY));
		p.setProperty("boutons_masques", String.join(",", disabled));
		try {
			Files.createDirectories(file.getParent());
			try (Writer w = Files.newBufferedWriter(file)) {
				p.store(w, "Grade Menu : /grade-set, /grade-position <x> <y>, /grade-bouton <bouton> on|off");
			}
		} catch (IOException e) {
			GradeMenuClient.LOGGER.warn("Écriture de {} impossible", file, e);
		}
	}
}
