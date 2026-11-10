// Une fois par image : envoie les commandes demandées par les clics.
fn grade_menu_frame(game: &mut crate::game::Game<'_>) {
    crate::game::recipe_book::grade_menu_glue::frame(game);
}
