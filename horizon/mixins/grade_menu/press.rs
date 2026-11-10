// Un clic sur l'inventaire : arrêté quand il est sur un élément du mod.
fn grade_menu_press(
    game: &crate::game::Game<'_>,
    screen: &horizon_client::mod_api::NativeScreenView<'_>,
    button: i32,
    canceled: &mut bool,
) {
    if !*canceled && crate::game::recipe_book::grade_menu_glue::press(game, screen, button) {
        *canceled = true;
    }
}
