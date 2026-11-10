// RecipeBookComponent.updateScreenPosition : le menu des homes décale l'inventaire comme le livre.
fn grade_menu_shift(game: &crate::game::Game<'_>, width: f32) -> core::ops::ControlFlow<f32> {
    match crate::game::recipe_book::grade_menu_glue::book_shift(game, width) {
        Some(shift) => core::ops::ControlFlow::Break(shift),
        None => core::ops::ControlFlow::Continue(()),
    }
}
