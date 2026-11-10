package fr.grademenu.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Position et taille de la fenêtre d'inventaire (champs protégés). */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Accessor("leftPos")
	int grademenu$getLeftPos();

	@Accessor("leftPos")
	void grademenu$setLeftPos(int leftPos);

	@Accessor("topPos")
	int grademenu$getTopPos();

	@Accessor("imageWidth")
	int grademenu$getImageWidth();
}
