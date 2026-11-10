// Après l'écran : les boutons et le menu des homes sur l'inventaire.
fn grade_menu_render(
    game: &crate::game::Game<'_>,
    batch: &mut horizon_client::hud::HudBatch,
    screen: &horizon_client::mod_api::NativeScreenView<'_>,
) {
    crate::game::recipe_book::grade_menu_glue::draw(game, batch, screen);
}
