// Options > Mods : le mod dans la liste.
fn grade_menu_listed(mods: &mut Vec<crate::game::native_hooks::NativeMod>) {
    crate::game::recipe_book::grade_menu_glue::listed(mods);
}
