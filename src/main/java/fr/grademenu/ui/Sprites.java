package fr.grademenu.ui;

import fr.grademenu.GradeMenuClient;
import net.minecraft.resources.Identifier;

/** Sprites de l'interface (assets/grademenu/textures/gui/sprites/...). */
public final class Sprites {
	public static final Identifier BUTTON = widget("button");
	public static final Identifier BUTTON_HIGHLIGHTED = widget("button_highlighted");
	public static final Identifier BUTTON_DANGER = widget("button_danger");
	public static final Identifier PANEL = widget("panel");
	public static final Identifier ROW = widget("row");
	public static final Identifier ROW_HIGHLIGHTED = widget("row_highlighted");

	private Sprites() {
	}

	public static Identifier icon(String name) {
		return Identifier.fromNamespaceAndPath(GradeMenuClient.MOD_ID, "icon/" + name);
	}

	private static Identifier widget(String name) {
		return Identifier.fromNamespaceAndPath(GradeMenuClient.MOD_ID, "widget/" + name);
	}
}
