// La réponse de /homes demandée par le mod ne s'affiche pas dans le chat.
fn grade_menu_chat(system: bool, overlay: bool, message: &horizon_client::text::Text, cancel: &mut bool) {
    if !*cancel && crate::game::recipe_book::grade_menu_glue::chat(system, overlay, message) {
        *cancel = true;
    }
}
