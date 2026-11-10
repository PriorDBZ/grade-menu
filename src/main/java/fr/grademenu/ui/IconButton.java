package fr.grademenu.ui;

import java.util.Objects;
import java.util.function.BooleanSupplier;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Bouton carré avec une icône 16x16, dans le style de l'inventaire. */
public class IconButton extends AbstractButton {
	private final Identifier icon;
	private final Runnable action;
	private BooleanSupplier danger = () -> false;
	private Component tooltipText;

	public IconButton(int size, Identifier icon, Component title, Runnable action) {
		super(0, 0, size, size, title);
		this.icon = icon;
		this.action = action;
	}

	/** Affiche le bouton en rouge tant que la condition est vraie (confirmation de suppression). */
	public IconButton danger(BooleanSupplier danger) {
		this.danger = danger;
		return this;
	}

	/** Change l'infobulle seulement si le texte a changé (appelé à chaque image). */
	public void tooltip(Component text) {
		if (!Objects.equals(text, tooltipText)) {
			tooltipText = text;
			setTooltip(Tooltip.create(text));
		}
	}

	@Override
	public void onPress(InputWithModifiers input) {
		action.run();
	}

	/** Ne garde pas le focus après un clic (sinon le crayon volerait le focus du champ de renommage). */
	@Override
	public boolean shouldTakeFocusAfterInteraction() {
		return false;
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		Identifier background;
		if (danger.getAsBoolean()) {
			background = Sprites.BUTTON_DANGER;
		} else if (active && isHovered()) {
			background = Sprites.BUTTON_HIGHLIGHTED;
		} else {
			background = Sprites.BUTTON;
		}
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, background, getX(), getY(), width, height);
		int offset = (width - 16) / 2;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, getX() + offset, getY() + offset, 16, 16,
				active ? 1.0F : 0.35F);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
