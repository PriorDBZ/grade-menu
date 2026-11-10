package fr.grademenu.ui;

import java.util.ArrayList;
import java.util.List;

import fr.grademenu.Action;
import fr.grademenu.Config;
import fr.grademenu.Grade;
import fr.grademenu.Homes;
import fr.grademenu.mixin.AbstractContainerScreenAccessor;
import fr.grademenu.mixin.AbstractRecipeBookScreenAccessor;
import fr.grademenu.mixin.InventoryScreenAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

/**
 * Ajoute à l'inventaire (InventoryScreen) les boutons du grade et, en Premium, le menu des homes.
 * Le menu des homes prend la place du livre de recettes : ouvrir l'un ferme l'autre.
 */
public final class InventoryOverlay {
	private static final int BUTTON_SIZE = 20;
	private static final int STEP = 21;
	private static final int PER_COLUMN = 6;
	private static final int PANEL_WIDTH = 147;
	private static final int PANEL_HEIGHT = 166;
	private static final int ROWS = 5;
	private static final int ROW_FULL_WIDTH = 133;
	private static final int ROW_WIDTH = 95;
	/** Même seuil et même décalage que le livre de recettes. */
	private static final int NARROW_WIDTH = 379;
	private static final int BOOK_OFFSET = 86;
	private static final long DELETE_CONFIRM_MS = 3000;
	private static final int TEXT_COLOR = 0xFF404040;
	private static final int INFO_COLOR = 0xFF5A5A5A;
	private static final int ERROR_COLOR = 0xFFB02E26;

	// État gardé entre deux ouvertures de l'inventaire.
	private static boolean homesOpen;
	private static int page;
	private static String armedDelete;
	private static long armedAt;

	/** Overlay de l'inventaire actuellement affiché (null si aucun). */
	private static InventoryOverlay current;

	private final InventoryScreen screen;
	private final AbstractContainerScreenAccessor container;
	private final RecipeBookComponent<?> recipeBook;
	private final List<Action> actions;
	private final boolean homesAllowed;
	private final List<IconButton> actionButtons = new ArrayList<>();

	private PanelBackground background;
	private final HomeRow[] rows = new HomeRow[ROWS];
	private final IconButton[] renameButtons = new IconButton[ROWS];
	private final IconButton[] deleteButtons = new IconButton[ROWS];
	private IconButton previousPage;
	private IconButton nextPage;
	private EditBox renameBox;
	private EditBox createBox;
	private IconButton addButton;
	private String renaming;
	private int pages = 1;
	private int panelX;
	private int panelY;
	private int buttonsLeft;
	private int buttonsTop;
	private int buttonsRight;
	private int buttonsBottom;

	private InventoryOverlay(InventoryScreen screen, Grade grade) {
		this.screen = screen;
		this.container = (AbstractContainerScreenAccessor) screen;
		this.recipeBook = ((AbstractRecipeBookScreenAccessor) screen).grademenu$getRecipeBookComponent();
		this.actions = Action.shownFor(grade);
		this.homesAllowed = actions.contains(Action.HOMES);
	}

	/** Les boutons ne s'affichent que sur un serveur, avec un grade Confirmé·e ou Premium. */
	public static boolean isEnabled(Minecraft minecraft) {
		return Config.grade() != Grade.JOUEUR
				&& minecraft.getConnection() != null
				&& !minecraft.hasSingleplayerServer();
	}

	/** Appelé après chaque init() de l'inventaire (ouverture, redimensionnement). */
	public static void attach(InventoryScreen screen) {
		InventoryOverlay overlay = new InventoryOverlay(screen, Config.grade());
		current = overlay;
		overlay.build();
		ScreenEvents.beforeExtract(screen).register((s, graphics, mouseX, mouseY, partialTick) -> overlay.update());
		ScreenEvents.remove(screen).register(s -> {
			if (current == overlay) {
				current = null;
			}
		});
		ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> overlay.allowKeyPress(event));
		ScreenMouseEvents.beforeMouseClick(screen).register((s, event) -> overlay.beforeMouseClick(event));
		if (homesOpen && overlay.homesAllowed && !overlay.isNarrow()) {
			overlay.setHomesOpen(true);
		}
		overlay.update();
	}

	/** Overlay attaché à cet écran, ou null. */
	public static InventoryOverlay of(Object screen) {
		InventoryOverlay overlay = current;
		return overlay != null && overlay.screen == screen ? overlay : null;
	}

	/** Le point est sur le menu des homes (les cases d'inventaire dessous ne doivent pas réagir). */
	public boolean isOverPanel(double x, double y) {
		return homesAllowed && homesOpen
				&& x >= panelX && x < panelX + PANEL_WIDTH && y >= panelY && y < panelY + PANEL_HEIGHT;
	}

	/** Le point est sur une partie du mod : un clic là ne doit pas jeter l'objet tenu. */
	public boolean isOverOverlay(double x, double y) {
		return isOverPanel(x, y)
				|| (x >= buttonsLeft && x < buttonsRight && y >= buttonsTop && y < buttonsBottom);
	}

	// --- Construction -----------------------------------------------------------

	private void build() {
		List<AbstractWidget> widgets = Screens.getWidgets(screen);
		Font font = Minecraft.getInstance().font;
		if (homesAllowed) {
			background = new PanelBackground();
			widgets.add(background);
			for (int i = 0; i < ROWS; i++) {
				final int index = i;
				renameButtons[i] = new IconButton(18, Sprites.icon("home_rename"), Component.literal("Renommer"),
						() -> startRename(rows[index].name()));
				deleteButtons[i] = new IconButton(18, Sprites.icon("home_delete"), Component.literal("Supprimer"),
						() -> delete(rows[index].name()))
						.danger(() -> rows[index].visible && isArmed(rows[index].name()));
				widgets.add(renameButtons[i]);
				widgets.add(deleteButtons[i]);
			}
			for (int i = 0; i < ROWS; i++) {
				rows[i] = new HomeRow(this::teleport);
				widgets.add(rows[i]);
			}
			previousPage = new IconButton(BUTTON_SIZE, Sprites.icon("page_prev"), Component.literal("Page précédente"),
					() -> page--);
			nextPage = new IconButton(BUTTON_SIZE, Sprites.icon("page_next"), Component.literal("Page suivante"),
					() -> page++);
			previousPage.tooltip(Component.literal("Page précédente"));
			nextPage.tooltip(Component.literal("Page suivante"));
			// Ajouté après les lignes : dessiné par-dessus la ligne en cours de renommage.
			renameBox = new EditBox(font, 0, 0, ROW_WIDTH - 22, 16, Component.literal("Nouveau nom"));
			renameBox.setMaxLength(32);
			renameBox.visible = false;
			createBox = new EditBox(font, 0, 0, 110, BUTTON_SIZE, Component.literal("Nom du home"));
			createBox.setMaxLength(32);
			createBox.setHint(Component.literal("Nom du home…").withStyle(ChatFormatting.DARK_GRAY));
			addButton = new IconButton(BUTTON_SIZE, Sprites.icon("home_add"), Component.literal("Créer le home"),
					this::submitCreate);
			widgets.add(previousPage);
			widgets.add(nextPage);
			widgets.add(renameBox);
			widgets.add(createBox);
			widgets.add(addButton);
		}
		for (Action action : actions) {
			IconButton button = new IconButton(BUTTON_SIZE, Sprites.icon(action.icon), Component.literal(action.title),
					() -> run(action));
			button.tooltip(tooltip(action));
			widgets.add(button);
			actionButtons.add(button);
		}
	}

	private static Component tooltip(Action action) {
		Component text = Component.literal(action.title).append("\n")
				.append(Component.literal(action.description).withStyle(ChatFormatting.GRAY));
		if (action.command != null) {
			text = text.copy().append("\n").append(Component.literal("/" + action.command).withStyle(ChatFormatting.DARK_GRAY));
		}
		return text;
	}

	// --- Mise à jour à chaque image ---------------------------------------------------

	private void update() {
		if (homesOpen && recipeBook.isVisible()) {
			// Le joueur a ouvert le livre de recettes : le menu des homes se ferme
			// (le livre a déjà décalé l'inventaire à la bonne place).
			homesOpen = false;
			closeEditing();
		}
		if (homesOpen && isNarrow()) {
			// Fenêtre trop étroite : le menu passerait sous les objets de l'inventaire.
			setHomesOpen(false);
		}
		layoutActionButtons();
		if (homesAllowed) {
			layoutPanel();
		}
	}

	private void layoutActionButtons() {
		int count = actionButtons.size();
		int left = container.grademenu$getLeftPos();
		int top = container.grademenu$getTopPos();
		int imageWidth = container.grademenu$getImageWidth();
		int columns = (count + PER_COLUMN - 1) / PER_COLUMN;
		int x0 = left + imageWidth + 3;
		boolean fitsRight = x0 + columns * STEP - 1 <= screen.width - 2;

		int perRow = Math.max(1, (imageWidth + 1) / STEP);
		int rowsAbove = (count + perRow - 1) / perRow;
		int yAbove = top - rowsAbove * STEP - 2;
		boolean above = !fitsRight && yAbove >= 0;
		// Décalage choisi avec /grade-position (y positif = vers le haut, donc vers les y plus petits).
		int offsetX = Config.offsetX();
		int offsetY = -Config.offsetY();

		for (int i = 0; i < count; i++) {
			if (actions.get(i) == Action.HOMES) {
				IconButton homes = actionButtons.get(i);
				homes.active = !isNarrow();
				homes.tooltip(homes.active ? tooltip(Action.HOMES)
						: Component.literal("Mes homes").append("\n").append(Component.literal(
								"Fenêtre trop étroite : agrandis-la ou baisse la taille de l'interface")
								.withStyle(ChatFormatting.GRAY)));
			}
			int x;
			int y;
			if (above) {
				x = left + (i % perRow) * STEP;
				y = yAbove + (i / perRow) * STEP;
			} else {
				x = x0 + (i / PER_COLUMN) * STEP;
				y = top + (i % PER_COLUMN) * STEP;
			}
			actionButtons.get(i).setPosition(x + offsetX, y + offsetY);
		}
		if (above) {
			buttonsLeft = left;
			buttonsTop = yAbove;
			buttonsRight = left + Math.min(count, perRow) * STEP;
			buttonsBottom = yAbove + rowsAbove * STEP;
		} else {
			buttonsLeft = x0;
			buttonsTop = top;
			buttonsRight = x0 + columns * STEP;
			buttonsBottom = top + Math.min(count, PER_COLUMN) * STEP;
		}
		buttonsLeft += offsetX;
		buttonsRight += offsetX;
		buttonsTop += offsetY;
		buttonsBottom += offsetY;
	}

	private void layoutPanel() {
		boolean open = homesOpen;
		panelX = (screen.width - PANEL_WIDTH) / 2 - (isNarrow() ? 0 : BOOK_OFFSET);
		panelY = (screen.height - PANEL_HEIGHT) / 2;
		background.visible = open;
		background.setPosition(panelX, panelY);

		List<String> names = Homes.names();
		pages = Math.max(1, (names.size() + ROWS - 1) / ROWS);
		page = Math.clamp(page, 0, pages - 1);
		if (renaming != null && !Homes.exists(renaming)) {
			cancelRename();
		}

		for (int i = 0; i < ROWS; i++) {
			int index = page * ROWS + i;
			HomeRow row = rows[i];
			IconButton rename = renameButtons[i];
			IconButton delete = deleteButtons[i];
			if (!open || index >= names.size()) {
				row.visible = false;
				rename.visible = false;
				delete.visible = false;
				continue;
			}
			int y = panelY + 25 + i * STEP;
			row.show(names.get(index));
			String name = row.name();
			boolean bed = row.isBed();
			row.setRectangle(bed ? ROW_FULL_WIDTH : ROW_WIDTH, BUTTON_SIZE, panelX + 7, y);
			row.visible = true;
			row.active = !name.equals(renaming);
			rename.visible = !bed;
			delete.visible = !bed;
			rename.setPosition(panelX + 7 + ROW_WIDTH + 1, y + 1);
			delete.setPosition(panelX + 7 + ROW_WIDTH + 20, y + 1);
			rename.tooltip(Component.literal("Renommer « " + name + " »"));
			delete.tooltip(isArmed(name)
					? Component.literal("Clique encore pour supprimer « " + name + " »").withStyle(ChatFormatting.RED)
					: Component.literal("Supprimer « " + name + " »"));
			if (name.equals(renaming)) {
				renameBox.setPosition(panelX + 7 + 20, y + 2);
			}
		}

		previousPage.visible = open;
		nextPage.visible = open;
		previousPage.setPosition(panelX + 80, panelY + 4);
		nextPage.setPosition(panelX + 120, panelY + 4);
		previousPage.active = page > 0;
		nextPage.active = page < pages - 1;

		renameBox.visible = open && renaming != null;
		createBox.visible = open;
		createBox.setPosition(panelX + 7, panelY + 141);
		addButton.visible = open;
		addButton.setPosition(panelX + 120, panelY + 141);
		String newName = createBox.getValue().strip();
		addButton.active = nameProblem(newName, null) == null;
		addButton.tooltip(addButton.active
				? Component.literal("Créer le home « " + newName + " » ici")
				: Component.literal("Écris un nom puis clique ici (ou Entrée)"));
	}

	private boolean isNarrow() {
		return screen.width < NARROW_WIDTH;
	}

	// --- Actions ------------------------------------------------------------------

	private void run(Action action) {
		if (action == Action.HOMES) {
			setHomesOpen(!homesOpen);
			return;
		}
		Homes.send(action.command);
		if (action == Action.HOME || action == Action.SPAWN) {
			screen.onClose();
		}
	}

	private void teleport(String name) {
		Homes.teleport(name);
		screen.onClose();
	}

	private void setHomesOpen(boolean open) {
		if (open) {
			if (recipeBook.isVisible()) {
				recipeBook.toggleVisibility();
			}
			homesOpen = true;
			Homes.requestIfStale();
		} else {
			homesOpen = false;
			closeEditing();
		}
		applyLeftPos();
	}

	/** Décale l'inventaire comme le fait le livre de recettes, et déplace le bouton du livre avec lui. */
	private void applyLeftPos() {
		InventoryScreenAccessor inventory = (InventoryScreenAccessor) screen;
		ScreenPosition before = inventory.grademenu$getRecipeBookButtonPosition();
		ImageButton bookButton = null;
		for (GuiEventListener child : screen.children()) {
			if (child instanceof ImageButton button && button.getX() == before.x() && button.getY() == before.y()) {
				bookButton = button;
				break;
			}
		}
		int imageWidth = container.grademenu$getImageWidth();
		int left = homesOpen && !isNarrow()
				? 177 + (screen.width - imageWidth - 200) / 2
				: recipeBook.updateScreenPosition(screen.width, imageWidth);
		container.grademenu$setLeftPos(left);
		if (bookButton != null) {
			ScreenPosition after = inventory.grademenu$getRecipeBookButtonPosition();
			bookButton.setPosition(after.x(), after.y());
		}
	}

	private void delete(String name) {
		if (isArmed(name)) {
			armedDelete = null;
			Homes.delete(name);
		} else {
			armedDelete = name;
			armedAt = System.currentTimeMillis();
		}
	}

	private static boolean isArmed(String name) {
		return name.equals(armedDelete) && System.currentTimeMillis() - armedAt < DELETE_CONFIRM_MS;
	}

	private void startRename(String name) {
		renaming = name;
		renameBox.setValue(name);
		renameBox.visible = true;
		screen.setFocused(renameBox);
		renameBox.moveCursorToEnd(false);
		renameBox.setHighlightPos(0);
	}

	private void submitRename() {
		String oldName = renaming;
		if (oldName == null) {
			return;
		}
		String newName = renameBox.getValue().strip();
		if (newName.equals(oldName)) {
			cancelRename();
		} else if (nameProblem(newName, oldName) == null) {
			Homes.rename(oldName, newName);
			cancelRename();
		}
	}

	private void cancelRename() {
		renaming = null;
		if (renameBox != null) {
			renameBox.visible = false;
			unfocus(renameBox);
		}
	}

	private void submitCreate() {
		String name = createBox.getValue().strip();
		if (nameProblem(name, null) == null) {
			Homes.create(name);
			createBox.setValue("");
			unfocus(createBox);
		}
	}

	private void closeEditing() {
		cancelRename();
		if (createBox != null) {
			unfocus(createBox);
		}
	}

	private void unfocus(EditBox box) {
		box.setFocused(false);
		if (screen.getFocused() == box) {
			screen.setFocused(null);
		}
	}

	/**
	 * Pourquoi ce nom ne convient pas (null s'il convient, "" s'il est vide).
	 * {@code current} : nom actuel du home renommé (on peut changer seulement la casse).
	 */
	private static String nameProblem(String name, String current) {
		if (name.isEmpty()) {
			return "";
		}
		if (!Homes.VALID_NAME.matcher(name).matches()) {
			return "Lettres, chiffres, _ et - seulement";
		}
		if (name.equalsIgnoreCase(Homes.BED)) {
			return "« bed » est réservé à ton lit";
		}
		if (Homes.exists(name) && !name.equalsIgnoreCase(current)) {
			return "Ce nom existe déjà";
		}
		return null;
	}

	// --- Clavier / souris -----------------------------------------------------------

	private EditBox focusedBox() {
		if (renameBox != null && renameBox.visible && renameBox.isFocused()) {
			return renameBox;
		}
		if (createBox != null && createBox.visible && createBox.isFocused()) {
			return createBox;
		}
		return null;
	}

	/**
	 * Pendant la saisie d'un nom, les touches vont au champ et pas à l'inventaire
	 * (sinon « e » fermerait l'inventaire, « q » jetterait un objet, 1-9 déplaceraient des objets).
	 */
	private boolean allowKeyPress(KeyEvent event) {
		EditBox box = focusedBox();
		if (box == null) {
			return true;
		}
		if (event.isEscape()) {
			if (box == renameBox) {
				cancelRename();
			} else {
				unfocus(box);
			}
		} else if (event.isConfirmation()) {
			if (box == renameBox) {
				submitRename();
			} else {
				submitCreate();
			}
		} else {
			box.keyPressed(event);
		}
		return false;
	}

	/** Un clic ailleurs que sur le champ en cours de saisie le quitte (et annule un renommage). */
	private void beforeMouseClick(MouseButtonEvent event) {
		EditBox box = focusedBox();
		if (box != null && !box.isMouseOver(event.x(), event.y())) {
			if (box == renameBox) {
				cancelRename();
			} else {
				unfocus(box);
			}
		}
	}

	// --- Fond du menu des homes ---------------------------------------------------------

	/** Fond, titre, numéro de page et messages du menu. Inactif : ne capte aucun clic. */
	private final class PanelBackground extends AbstractWidget {
		PanelBackground() {
			super(0, 0, PANEL_WIDTH, PANEL_HEIGHT, Component.literal("Mes homes"));
			this.active = false;
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
			Font font = Minecraft.getInstance().font;
			int x = getX();
			int y = getY();
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Sprites.PANEL, x, y, width, height);
			graphics.text(font, "Mes homes", x + 8, y + 10, TEXT_COLOR, false);
			String pageText = (page + 1) + "/" + pages;
			graphics.text(font, pageText, x + 110 - font.width(pageText) / 2, y + 10, TEXT_COLOR, false);

			String center = centerMessage();
			if (center != null) {
				graphics.text(font, center, x + (width - font.width(center)) / 2, y + 70, INFO_COLOR, false);
			}
			String status = statusMessage();
			if (status != null && !status.isEmpty()) {
				graphics.text(font, font.plainSubstrByWidth(status, width - 14), x + 8, y + 131, ERROR_COLOR, false);
			}
		}

		private String centerMessage() {
			if (!Homes.names().isEmpty()) {
				return null;
			}
			return switch (Homes.state()) {
				case NOT_LOADED, LOADING -> "Chargement…";
				case NO_RESPONSE -> "Pas de réponse du serveur";
				case LOADED -> "Aucun home";
			};
		}

		private String statusMessage() {
			if (renaming != null) {
				return nameProblem(renameBox.getValue().strip(), renaming);
			}
			return nameProblem(createBox.getValue().strip(), null);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			defaultButtonNarrationText(output);
		}
	}
}
