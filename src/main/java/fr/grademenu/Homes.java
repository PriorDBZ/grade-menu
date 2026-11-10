package fr.grademenu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Liste des homes du joueur, lue en envoyant /homes au serveur.
 * La réponse attendue est une ligne « Homes: redtop, mizuland, base, bed » ;
 * quand c'est le mod qui a demandé la liste, cette ligne est cachée du chat.
 */
public final class Homes {
	/** Home créé automatiquement par le serveur sur le dernier lit : ni renommable ni supprimable. */
	public static final String BED = "bed";
	/** Caractères acceptés dans un nom de home. */
	public static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_-]{1,32}");

	private static final String PREFIX = "Homes:";
	private static final long TIMEOUT_MS = 5000;
	/** Délai avant de relire la liste après une création / suppression / renommage (en ticks). */
	private static final int REFRESH_DELAY_TICKS = 15;

	public enum State { NOT_LOADED, LOADING, LOADED, NO_RESPONSE }

	private static List<String> names = List.of();
	private static State state = State.NOT_LOADED;
	private static long requestedAt;
	private static int refreshIn = -1;

	private Homes() {
	}

	public static List<String> names() {
		return names;
	}

	public static State state() {
		return state;
	}

	public static boolean exists(String name) {
		return names.stream().anyMatch(n -> n.equalsIgnoreCase(name));
	}

	/** Demande la liste au serveur (la réponse sera cachée du chat). */
	public static void request() {
		if (send("homes")) {
			state = State.LOADING;
			requestedAt = System.currentTimeMillis();
		}
	}

	/** Comme request(), sauf si une demande vient d'être faite (évite de spammer /homes). */
	public static void requestIfStale() {
		if (state == State.LOADING || System.currentTimeMillis() - requestedAt < 2000) {
			return;
		}
		request();
	}

	public static void teleport(String name) {
		send("home " + name);
	}

	public static void create(String name) {
		send("sethome " + name);
		refreshLater();
	}

	public static void delete(String name) {
		send("delhome " + name);
		refreshLater();
	}

	public static void rename(String oldName, String newName) {
		send("renamehome " + oldName + " " + newName);
		refreshLater();
	}

	/** Envoie une commande au serveur (sans « / »). Renvoie false si on n'est pas connecté. */
	public static boolean send(String command) {
		var connection = Minecraft.getInstance().getConnection();
		if (connection == null) {
			return false;
		}
		connection.sendCommand(command);
		return true;
	}

	private static void refreshLater() {
		refreshIn = REFRESH_DELAY_TICKS;
	}

	/** Appelé à chaque tick client. */
	public static void tick() {
		if (state == State.LOADING && System.currentTimeMillis() - requestedAt > TIMEOUT_MS) {
			state = State.NO_RESPONSE;
		}
		if (refreshIn > 0 && --refreshIn == 0) {
			refreshIn = -1;
			request();
		}
	}

	/**
	 * Message système reçu du serveur. Renvoie false pour le cacher du chat
	 * (uniquement la réponse à une demande faite par le mod).
	 */
	public static boolean onGameMessage(Component message, boolean overlay) {
		if (overlay || state != State.LOADING) {
			return true;
		}
		String text = message.getString();
		String plain = ChatFormatting.stripFormatting(text);
		text = (plain != null ? plain : text).strip();
		if (!text.startsWith(PREFIX)) {
			return true;
		}
		names = parse(text.substring(PREFIX.length()));
		state = State.LOADED;
		return false;
	}

	/** « redtop, mizuland, base, bed » -> [bed, base, mizuland, redtop] (le lit d'abord, puis ordre alphabétique). */
	static List<String> parse(String list) {
		List<String> result = new ArrayList<>();
		for (String part : list.split(",")) {
			String name = part.strip();
			if (!name.isEmpty()) {
				result.add(name);
			}
		}
		result.sort(Comparator.<String, Boolean>comparing(n -> !n.equalsIgnoreCase(BED))
				.thenComparing(n -> n.toLowerCase(Locale.ROOT)));
		return List.copyOf(result);
	}

	/** Oublie tout à la déconnexion. */
	public static void reset() {
		names = List.of();
		state = State.NOT_LOADED;
		refreshIn = -1;
	}
}
