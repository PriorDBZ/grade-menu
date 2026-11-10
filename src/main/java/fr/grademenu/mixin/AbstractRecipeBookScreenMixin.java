package fr.grademenu.mixin;

import fr.grademenu.ui.InventoryOverlay;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Protège l'inventaire quand on clique sur les éléments du mod. */
@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin {
	/** Aucune case d'inventaire n'est survolée/cliquée à travers le menu des homes. */
	@Inject(method = "isHovering", at = @At("HEAD"), cancellable = true)
	private void grademenu$noSlotUnderPanel(int x, int y, int width, int height, double mouseX, double mouseY,
			CallbackInfoReturnable<Boolean> cir) {
		InventoryOverlay overlay = InventoryOverlay.of(this);
		if (overlay != null && overlay.isOverPanel(mouseX, mouseY)) {
			cir.setReturnValue(false);
		}
	}

	/** Un clic sur le menu ou entre deux boutons n'est pas « en dehors » : l'objet tenu n'est pas jeté. */
	@Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
	private void grademenu$keepCarriedItem(double mouseX, double mouseY, int leftPos, int topPos,
			CallbackInfoReturnable<Boolean> cir) {
		InventoryOverlay overlay = InventoryOverlay.of(this);
		if (overlay != null && overlay.isOverOverlay(mouseX, mouseY)) {
			cir.setReturnValue(false);
		}
	}
}
