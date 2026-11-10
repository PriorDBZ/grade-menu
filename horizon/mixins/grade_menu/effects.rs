// EffectsInInventory : avec le mod, les effets passent en haut à gauche.
fn grade_menu_effects(
    game: &crate::game::Game<'_>,
    batch: &mut horizon_client::hud::HudBatch,
) -> core::ops::ControlFlow<Option<[(String, u32); 2]>> {
    match crate::game::recipe_book::grade_menu_glue::effects(game, batch) {
        Some(tip) => core::ops::ControlFlow::Break(tip),
        None => core::ops::ControlFlow::Continue(()),
    }
}
