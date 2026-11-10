// Une touche : au champ de texte du mod quand il a le focus.
fn grade_menu_key(
    game: &crate::game::Game<'_>,
    screen: &horizon_client::mod_api::NativeScreenView<'_>,
    key: &str,
    canceled: &mut bool,
) {
    if !*canceled && crate::game::recipe_book::grade_menu_glue::key(game, screen, key) {
        *canceled = true;
    }
}
