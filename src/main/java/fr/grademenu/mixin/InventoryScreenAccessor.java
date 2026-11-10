package fr.grademenu.mixin;

import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(InventoryScreen.class)
public interface InventoryScreenAccessor {
	/** Position du bouton du livre de recettes pour le leftPos actuel. */
	@Invoker("getRecipeBookButtonPosition")
	ScreenPosition grademenu$getRecipeBookButtonPosition();
}
