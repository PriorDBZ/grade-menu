package fr.grademenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.CommandDispatcher;

import fr.grademenu.ui.InventoryOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

public class GradeMenuClient implements ClientModInitializer {
	public static final String MOD_ID = "grademenu";
	public static final Logger LOGGER = LoggerFactory.getLogger("Grade Menu");

	@Override
	public void onInitializeClient() {
		Config.load();
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> registerCommands(dispatcher));
		ScreenEvents.AFTER_INIT.register((minecraft, screen, width, height) -> {
			if (screen instanceof InventoryScreen inventory && InventoryOverlay.isEnabled(minecraft)) {
				InventoryOverlay.attach(inventory);
			}
		});
		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> Homes.tick());
		ClientReceiveMessageEvents.ALLOW_GAME.register(Homes::onGameMessage);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, minecraft) -> Homes.reset());
		LOGGER.info("Grade Menu prêt (grade : {})", Config.grade().id);
	}

	/** Toutes les commandes du mod commencent par « grade- » (regroupées dans l'aide à la saisie). */
	static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		SetGradeCommand.register(dispatcher);
		PositionCommand.register(dispatcher);
		ButtonCommand.register(dispatcher);
	}
}
