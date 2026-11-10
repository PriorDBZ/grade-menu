package fr.grademenu.ui;

import java.util.function.Consumer;

import fr.grademenu.Homes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

/** Une ligne de la liste des homes : clic = téléportation. */
public class HomeRow extends AbstractWidget {
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int TEXT_COLOR_HIGHLIGHTED = 0xFF404040;
	private static final int NOTE_COLOR = 0xFFD8D8D8;

	private final Consumer<String> onClick;
	private String name = "";

	public HomeRow(Consumer<String> onClick) {
		super(0, 0, 0, 20, Component.empty());
		this.onClick = onClick;
	}

	public String name() {
		return name;
	}

	public boolean isBed() {
		return name.equalsIgnoreCase(Homes.BED);
	}

	public void show(String newName) {
		if (newName.equals(name)) {
			return;
		}
		name = newName;
		setMessage(Component.literal(newName));
		Component tip = Component.literal(newName).append("\n")
				.append(Component.literal("Clic : se téléporter").withStyle(ChatFormatting.GRAY));
		if (isBed()) {
			tip = Component.literal(newName).append("\n")
					.append(Component.literal("Ton dernier lit (mis à jour automatiquement)")
							.withStyle(ChatFormatting.GRAY)).append("\n")
					.append(Component.literal("Clic : se téléporter").withStyle(ChatFormatting.GRAY));
		}
		setTooltip(Tooltip.create(tip));
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		onClick.accept(name);
	}

	@Override
	public boolean shouldTakeFocusAfterInteraction() {
		return false;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		boolean highlighted = isHovered();
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, highlighted ? Sprites.ROW_HIGHLIGHTED : Sprites.ROW,
				getX(), getY(), width, height);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Sprites.icon(isBed() ? "home" : "homes"),
				getX() + 2, getY() + 2, 16, 16);

		Font font = Minecraft.getInstance().font;
		int textX = getX() + 21;
		int textY = getY() + (height - 8) / 2;
		int maxWidth = width - 24;
		if (isBed()) {
			String note = "lit auto";
			int noteWidth = font.width(note);
			graphics.text(font, note, getRight() - 4 - noteWidth, textY,
					highlighted ? TEXT_COLOR_HIGHLIGHTED : NOTE_COLOR, false);
			maxWidth -= noteWidth + 4;
		}
		String shown = name;
		if (font.width(shown) > maxWidth) {
			shown = font.plainSubstrByWidth(shown, maxWidth - font.width("…")) + "…";
		}
		graphics.text(font, shown, textX, textY, highlighted ? TEXT_COLOR_HIGHLIGHTED : TEXT_COLOR, !highlighted);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
