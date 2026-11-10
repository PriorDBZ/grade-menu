// /grade-set, /grade-position, /grade-bouton.
fn grade_menu_command(game: &mut crate::game::Game<'_>, command: &str, ran: &mut bool) {
    if !*ran && crate::game::recipe_book::grade_menu_glue::command(game, command) {
        *ran = true;
    }
}
