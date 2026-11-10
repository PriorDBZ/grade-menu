package fr.grademenu.ui;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Ordering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;

/**
 * Effets de potion affichés en rangée tout en haut à gauche de l'écran d'inventaire
 * (à la place de leur position habituelle, à droite, où sont les boutons du mod).
 */
public final class EffectsRow {
	private static final int MARGIN = 4;
	private static final int HEIGHT = 34;
	private static final int MIN_WIDTH = 30;
	private static final int GAP = 2;
	private static final int TEXT_COLOR = 0xFF404040;

	private EffectsRow() {
	}

	public static void extract(GuiGraphicsExtractor graphics, int screenWidth, int mouseX, int mouseY) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return;
		}
		Collection<MobEffectInstance> active = minecraft.player.getActiveEffects();
		if (active.isEmpty()) {
			return;
		}
		Font font = minecraft.font;
		float tickRate = minecraft.level != null ? minecraft.level.tickRateManager().tickrate() : 20.0F;
		int x = MARGIN;
		int y = MARGIN;
		for (MobEffectInstance effect : Ordering.natural().sortedCopy(active)) {
			Component duration = MobEffectUtil.formatDuration(effect, 1.0F, tickRate);
			int width = Math.max(MIN_WIDTH, font.width(duration) + 8);
			if (x > MARGIN && x + width > screenWidth - MARGIN) {
				x = MARGIN;
				y += HEIGHT + GAP;
			}
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Sprites.PANEL, x, y, width, HEIGHT);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Hud.getMobEffectSprite(effect.getEffect()),
					x + (width - 18) / 2, y + 4, 18, 18);
			graphics.text(font, duration, x + (width - font.width(duration)) / 2, y + 23, TEXT_COLOR, false);
			if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + HEIGHT) {
				graphics.setTooltipForNextFrame(font, List.of(name(effect), duration), Optional.empty(), mouseX, mouseY);
			}
			x += width + GAP;
		}
	}

	/** Nom de l'effet avec son niveau (« Vitesse II »), comme dans l'inventaire de base. */
	private static Component name(MobEffectInstance effect) {
		MutableComponent name = effect.getEffect().value().getDisplayName().copy();
		int amplifier = effect.getAmplifier();
		if (amplifier >= 1 && amplifier <= 9) {
			name.append(CommonComponents.SPACE).append(Component.translatable("enchantment.level." + (amplifier + 1)));
		}
		return name;
	}
}
