// Un caractère : au champ de texte du mod quand il a le focus.
fn grade_menu_char(
    game: &crate::game::Game<'_>,
    screen: &horizon_client::mod_api::NativeScreenView<'_>,
    c: char,
    canceled: &mut bool,
) {
    if !*canceled && crate::game::recipe_book::grade_menu_glue::char_typed(game, screen, c) {
        *canceled = true;
    }
}
