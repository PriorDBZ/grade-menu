// La réponse de /homes demandée par le mod ne s'affiche pas dans le chat (false = message annulé).
fn grade_menu_chat(system: bool, overlay: bool, message: &horizon_client::text::Text) -> core::ops::ControlFlow<bool> {
    if crate::game::recipe_book::grade_menu_glue::chat(system, overlay, message) {
        core::ops::ControlFlow::Break(false)
    } else {
        core::ops::ControlFlow::Continue(())
    }
}
