package fr.grademenu.mixin;

import fr.grademenu.ui.EffectsRow;
import fr.grademenu.ui.InventoryOverlay;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Quand les boutons du mod sont affichés, les effets passent en haut à gauche de l'écran. */
@Mixin(EffectsInInventory.class)
public abstract class EffectsInInventoryMixin {
	@Shadow
	@Final
	private AbstractContainerScreen<?> screen;

	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void grademenu$drawTopLeft(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
		if (InventoryOverlay.of(screen) != null) {
			EffectsRow.extract(graphics, screen.width, mouseX, mouseY);
			ci.cancel();
		}
	}

	/** Toujours visibles (en haut à gauche) : le HUD n'a pas à les redessiner derrière l'inventaire. */
	@Inject(method = "canSeeEffects", at = @At("HEAD"), cancellable = true)
	private void grademenu$alwaysVisible(CallbackInfoReturnable<Boolean> cir) {
		if (InventoryOverlay.of(screen) != null) {
			cir.setReturnValue(true);
		}
	}
}
