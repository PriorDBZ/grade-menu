// Les commandes du mod, pour que le chat les complète.
fn grade_menu_commands(commands: &mut Vec<&'static str>) {
    crate::game::recipe_book::grade_menu_glue::commands(commands);
}
