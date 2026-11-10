// Grade Menu pour Horizon, côté jeu : `crate::game::recipe_book::grade_menu_glue`.
// Placé dans le fichier du livre de recettes pour pouvoir le fermer et le
// remplacer par le menu des homes. Dessine les boutons et le menu par-dessus
// l'inventaire, reçoit les clics et le clavier, lit la réponse de /homes,
// et déplace les effets de potion en haut à gauche.

use crate::game::grade_menu as m;
use crate::game::grade_menu_icons as icons;
use crate::game::{Game, Screen};
use horizon_client::hud::HudBatch;
use horizon_client::inventory_effects;
use horizon_client::mod_api::NativeScreenView;
use horizon_client::text::Text;
use std::time::Instant;

const DARK_TEXT: u32 = 0x40_4040;
const INFO_TEXT: u32 = 0x5A_5A5A;
const ERROR_TEXT: u32 = 0xB0_2E26;
const FACE: u32 = 0xFFC6_C6C6;
const FACE_HOVER: u32 = 0xFFDE_DEE4;
const FACE_DANGER: u32 = 0xFFE8_8074;
const ROW: u32 = 0xFF8B_8B8B;

/// Les réglages du mod (lus une seule fois, puis gardés en mémoire).
fn settings(game: &Game<'_>) -> m::Settings {
    m::with(|ui| ui.settings(|key| game.native_setting(m::ID, key)).clone())
}

/// L'inventaire du joueur (pas un coffre, ni une table de craft, ni une monture).
fn player_inventory(game: &Game<'_>) -> bool {
    game.screen == Screen::Inventory && game.container.is_none() && game.craft_size == 2 && !game.mount_active()
}

/// Sur un serveur (pas en solo), le mod activé et un grade qui donne des boutons.
fn enabled(game: &Game<'_>) -> bool {
    game.native_on(m::ID)
        && game.remote.is_some()
        && !game.has_singleplayer_server()
        && settings(game).grade != m::Grade::Joueur
}

fn active(game: &Game<'_>, screen: &NativeScreenView<'_>) -> bool {
    screen.id == "inventory" && player_inventory(game) && enabled(game)
}

fn book_open(game: &Game<'_>) -> bool {
    game.active_book().is_some_and(|b| game.book_open(b))
}

// ---- Dessin ------------------------------------------------------------------------------

fn fill(game: &Game<'_>, batch: &mut HudBatch, x: i32, y: i32, w: i32, h: i32, argb: u32) {
    if w > 0 && h > 0 {
        game.native_fill(batch, [x as f32, y as f32, w as f32, h as f32], argb);
    }
}

fn text(game: &Game<'_>, batch: &mut HudBatch, s: &str, x: i32, y: i32, rgb: u32, shadow: bool) {
    game.native_text(batch, s, [x as f32, y as f32], rgb, shadow);
}

fn width(game: &Game<'_>, s: &str) -> i32 {
    game.native_text_width(s).ceil() as i32
}

/// Une icône 16x16 (pixel art du mod), plus pâle quand le bouton est inactif.
fn icon(game: &Game<'_>, batch: &mut HudBatch, name: &str, x: i32, y: i32, dim: bool) {
    for &(dx, dy, w, argb) in icons::runs(name) {
        let argb = if dim { (argb & 0x00FF_FFFF) | 0x5A00_0000 } else { argb };
        fill(game, batch, x + dx as i32, y + dy as i32, w as i32, 1, argb);
    }
}

/// Bouton façon inventaire : contour noir aux coins arrondis, relief clair / sombre.
fn button(game: &Game<'_>, batch: &mut HudBatch, r: [i32; 4], face: u32) {
    let [x, y, w, h] = r;
    fill(game, batch, x + 1, y, w - 2, 1, 0xFF00_0000);
    fill(game, batch, x + 1, y + h - 1, w - 2, 1, 0xFF00_0000);
    fill(game, batch, x, y + 1, 1, h - 2, 0xFF00_0000);
    fill(game, batch, x + w - 1, y + 1, 1, h - 2, 0xFF00_0000);
    fill(game, batch, x + 1, y + 1, w - 2, h - 2, 0xFF55_5555);
    fill(game, batch, x + 1, y + 1, w - 3, h - 3, 0xFFFF_FFFF);
    fill(game, batch, x + 2, y + 2, w - 4, h - 4, face);
}

/// Fond d'un panneau, comme celui de l'inventaire.
fn panel(game: &Game<'_>, batch: &mut HudBatch, r: [i32; 4]) {
    let [x, y, w, h] = r;
    fill(game, batch, x + 2, y, w - 4, 1, 0xFF00_0000);
    fill(game, batch, x + 2, y + h - 1, w - 4, 1, 0xFF00_0000);
    fill(game, batch, x, y + 2, 1, h - 4, 0xFF00_0000);
    fill(game, batch, x + w - 1, y + 2, 1, h - 4, 0xFF00_0000);
    fill(game, batch, x + 1, y + 1, w - 2, h - 2, 0xFF00_0000);
    fill(game, batch, x + 2, y + 2, w - 4, h - 4, 0xFF55_5555);
    fill(game, batch, x + 2, y + 2, w - 5, h - 5, 0xFFFF_FFFF);
    fill(game, batch, x + 3, y + 3, w - 6, h - 6, FACE);
}

fn row_background(game: &Game<'_>, batch: &mut HudBatch, r: [i32; 4], face: u32) {
    let [x, y, w, h] = r;
    fill(game, batch, x, y, w, h, 0xFF00_0000);
    fill(game, batch, x + 1, y + 1, w - 2, h - 2, face);
}

/// Un champ de texte : cadre gris, fond noir, texte blanc, curseur clignotant.
fn field(game: &Game<'_>, batch: &mut HudBatch, r: [i32; 4], f: &m::Field, hint: &str) {
    let [x, y, w, h] = r;
    fill(game, batch, x, y, w, h, if f.focused { 0xFFFF_FFFF } else { 0xFFA0_A0A0 });
    fill(game, batch, x + 1, y + 1, w - 2, h - 2, 0xFF00_0000);
    let ty = y + (h - 8) / 2;
    if f.text.is_empty() && !f.focused {
        text(game, batch, hint, x + 4, ty, 0x70_7070, false);
    } else {
        text(game, batch, &f.text, x + 4, ty, 0xE0_E0E0, true);
    }
    if f.focused && (game.visual_ticks() / 6) % 2 == 0 {
        let before: String = f.text.chars().take(f.cursor).collect();
        let cx = x + 4 + width(game, &before);
        fill(game, batch, cx, ty - 1, 1, 10, 0xFFD0_D0D0);
    }
}

/// Raccourcit avec « … » pour tenir dans `max` pixels.
fn fit(game: &Game<'_>, s: &str, max: i32) -> String {
    if width(game, s) <= max {
        return s.to_owned();
    }
    let mut out = String::new();
    for c in s.chars() {
        let next = format!("{out}{c}…");
        if width(game, &next) > max {
            break;
        }
        out.push(c);
    }
    format!("{out}…")
}

/// Après tout l'écran : les boutons, le menu des homes et l'infobulle survolée.
pub(in crate::game) fn draw(game: &Game<'_>, batch: &mut HudBatch, screen: &NativeScreenView<'_>) {
    let on = active(game, screen);
    if !on {
        m::with(|ui| {
            ui.visible = false;
            ui.hits.clear();
            ui.areas.clear();
        });
        return;
    }
    let now = Instant::now();
    let mouse = screen.mouse;
    let size = [batch.width as i32, batch.height as i32];
    let narrow = size[0] < m::NARROW;
    let book = book_open(game);
    let settings = settings(game);
    let mut hits: Vec<([i32; 4], m::Hit)> = Vec::new();
    let mut areas: Vec<[i32; 4]> = Vec::new();
    let mut tooltip: Vec<Text> = Vec::new();

    m::with(|ui| {
        ui.visible = true;
        if ui.homes_open && (book || narrow || !settings.shown().contains(&m::Action::Homes)) {
            // Le livre de recettes a été ouvert (ou plus de place) : le menu se ferme.
            ui.close_homes();
        }
    });

    // Les boutons à côté de l'inventaire.
    let shown = settings.shown();
    let inventory = [screen.rect[0] as i32, screen.rect[1] as i32, screen.rect[2] as i32, screen.rect[3] as i32];
    let rects = m::button_rects(shown.len(), inventory, size, settings.offset);
    for (&action, &r) in shown.iter().zip(&rects) {
        let usable = !(action == m::Action::Homes && narrow);
        let hovered = m::inside(r, mouse);
        let face = if usable && hovered { FACE_HOVER } else { FACE };
        button(game, batch, r, face);
        icon(game, batch, action.id(), r[0] + 2, r[1] + 2, !usable);
        areas.push(r);
        if usable {
            hits.push((r, m::Hit::Action(action)));
        }
        if hovered {
            tooltip.push(Text::literal(action.title()));
            if usable {
                tooltip.push(Text::legacy(&format!("§7{}", action.description())));
                if let Some(c) = action.command() {
                    tooltip.push(Text::legacy(&format!("§8{c}")));
                }
            } else {
                tooltip.push(Text::legacy("§7Fenêtre trop étroite : agrandis-la ou baisse la taille de l'interface"));
            }
        }
    }
    // Les espaces entre les boutons font partie du mod (un clic là ne jette pas l'objet tenu).
    if let (Some(first), Some(last)) = (rects.iter().map(|r| r[0]).min(), rects.iter().map(|r| r[0] + r[2]).max()) {
        let top = rects.iter().map(|r| r[1]).min().unwrap_or(0);
        let bottom = rects.iter().map(|r| r[1] + r[3]).max().unwrap_or(0);
        areas.push([first, top, last - first, bottom - top]);
    }

    m::with(|ui| {
        if ui.homes_open {
            draw_homes(game, batch, ui, size, mouse, now, &mut hits, &mut areas, &mut tooltip);
        }
        ui.hits = hits;
        ui.areas = areas;
    });

    if !tooltip.is_empty() {
        game.native_gui_tooltip(batch, &tooltip, mouse);
    }
}

#[allow(clippy::too_many_arguments)]
fn draw_homes(
    game: &Game<'_>,
    batch: &mut HudBatch,
    ui: &mut m::Ui,
    size: [i32; 2],
    mouse: [f32; 2],
    now: Instant,
    hits: &mut Vec<([i32; 4], m::Hit)>,
    areas: &mut Vec<[i32; 4]>,
    tooltip: &mut Vec<Text>,
) {
    let p = m::homes_panel(size);
    let [px, py, pw, _] = p;
    areas.push(p);
    panel(game, batch, p);
    text(game, batch, "Mes homes", px + 8, py + 10, DARK_TEXT, false);

    let pages = ui.pages();
    ui.page = ui.page.min(pages - 1);
    let page_text = format!("{}/{}", ui.page + 1, pages);
    text(game, batch, &page_text, px + 110 - width(game, &page_text) / 2, py + 10, DARK_TEXT, false);
    for (dx, name, hit, usable, tip) in [
        (80, "page_prev", m::Hit::PreviousPage, ui.page > 0, "Page précédente"),
        (120, "page_next", m::Hit::NextPage, ui.page + 1 < pages, "Page suivante"),
    ] {
        let r = [px + dx, py + 4, m::BUTTON, m::BUTTON];
        let hovered = m::inside(r, mouse);
        button(game, batch, r, if usable && hovered { FACE_HOVER } else { FACE });
        icon(game, batch, name, r[0] + 2, r[1] + 2, !usable);
        if usable {
            hits.push((r, hit));
        }
        if hovered {
            tooltip.push(Text::literal(tip));
        }
    }

    for i in 0..m::ROWS {
        let Some(name) = ui.homes.get(ui.page * m::ROWS + i).cloned() else {
            break;
        };
        let bed = name.eq_ignore_ascii_case(m::BED);
        let y = py + 25 + i as i32 * m::STEP;
        let r = [px + 7, y, if bed { m::ROW_FULL_W } else { m::ROW_W }, m::BUTTON];
        let renaming = ui.renaming.as_deref() == Some(name.as_str());
        let hovered = !renaming && m::inside(r, mouse);
        row_background(game, batch, r, if hovered { FACE_HOVER } else { ROW });
        icon(game, batch, if bed { "home" } else { "homes" }, r[0] + 2, r[1] + 2, false);
        let mut max = r[2] - 24;
        let color = if hovered { DARK_TEXT } else { 0xFF_FFFF };
        if bed {
            let note = "lit auto";
            let w = width(game, note);
            text(game, batch, note, r[0] + r[2] - 4 - w, y + 6, if hovered { DARK_TEXT } else { 0xD8_D8D8 }, false);
            max -= w + 4;
        }
        if renaming {
            let fr = [r[0] + 20, y + 2, r[2] - 22, 16];
            field(game, batch, fr, &ui.rename_field, "");
            hits.push((fr, m::Hit::RenameField));
        } else {
            text(game, batch, &fit(game, &name, max), r[0] + 21, y + 6, color, !hovered);
            hits.push((r, m::Hit::Row(i)));
            if hovered {
                tooltip.push(Text::literal(name.clone()));
                if bed {
                    tooltip.push(Text::legacy("§7Ton dernier lit (mis à jour automatiquement)"));
                }
                tooltip.push(Text::legacy("§7Clic : se téléporter"));
            }
        }
        if !bed {
            let armed = ui.is_armed(&name, now);
            for (dx, icon_name, hit, tip) in [
                (m::ROW_W + 1, "home_rename", m::Hit::Rename(i), format!("Renommer « {name} »")),
                (
                    m::ROW_W + 20,
                    "home_delete",
                    m::Hit::Delete(i),
                    if armed {
                        format!("§cClique encore pour supprimer « {name} »")
                    } else {
                        format!("Supprimer « {name} »")
                    },
                ),
            ] {
                let br = [px + 7 + dx, y + 1, 18, 18];
                let hovered = m::inside(br, mouse);
                let face = if matches!(hit, m::Hit::Delete(_)) && armed {
                    FACE_DANGER
                } else if hovered {
                    FACE_HOVER
                } else {
                    FACE
                };
                button(game, batch, br, face);
                icon(game, batch, icon_name, br[0] + 1, br[1] + 1, false);
                hits.push((br, hit));
                if hovered {
                    tooltip.push(Text::legacy(&tip));
                }
            }
        }
    }

    if let Some(msg) = ui.center_message() {
        text(game, batch, msg, px + (pw - width(game, msg)) / 2, py + 70, INFO_TEXT, false);
    }
    if let Some(problem) = ui.status() {
        text(game, batch, &fit(game, problem, pw - 14), px + 8, py + 131, ERROR_TEXT, false);
    }

    let create = [px + 7, py + 141, 110, m::BUTTON];
    field(game, batch, create, &ui.create_field, "Nom du home…");
    hits.push((create, m::Hit::CreateField));
    let add = [px + 120, py + 141, m::BUTTON, m::BUTTON];
    let name = ui.create_field.text.trim().to_owned();
    let usable = m::name_problem(&name, None, &ui.homes).is_none();
    let hovered = m::inside(add, mouse);
    button(game, batch, add, if usable && hovered { FACE_HOVER } else { FACE });
    icon(game, batch, "home_add", add[0] + 2, add[1] + 2, !usable);
    if usable {
        hits.push((add, m::Hit::Add));
    }
    if hovered {
        tooltip.push(Text::literal(if usable {
            format!("Créer le home « {name} » ici")
        } else {
            "Écris un nom puis clique ici (ou Entrée)".to_owned()
        }));
    }
}

// ---- Souris et clavier ----------------------------------------------------------------------

/// Un clic sur l'inventaire : true pour l'arrêter (il était sur le mod).
pub(in crate::game) fn press(game: &Game<'_>, screen: &NativeScreenView<'_>, button: i32) -> bool {
    if !active(game, screen) {
        return false;
    }
    let mouse = screen.mouse;
    let now = Instant::now();
    m::with(|ui| {
        let hit = ui.hits.iter().find(|(r, _)| m::inside(*r, mouse)).map(|(_, h)| *h);
        let over = ui.areas.iter().any(|r| m::inside(*r, mouse));
        match hit {
            Some(h) if button == 0 => ui.click(h, now, false),
            _ => {
                // Un clic ailleurs quitte le champ en cours (et annule un renommage).
                ui.cancel_rename();
                ui.create_field.focused = false;
            }
        }
        over || hit.is_some()
    })
}

/// Une touche : pendant la saisie d'un nom, toutes les touches vont au champ (sinon
/// « e » fermerait l'inventaire, « q » jetterait un objet, 1-9 déplaceraient des objets).
pub(in crate::game) fn key(game: &Game<'_>, screen: &NativeScreenView<'_>, key: &str) -> bool {
    if !active(game, screen) {
        return false;
    }
    let now = Instant::now();
    m::with(|ui| {
        if ui.focused().is_none() {
            return false;
        }
        let renaming = ui.renaming.is_some() && ui.rename_field.focused;
        match key {
            "key.keyboard.escape" if renaming => ui.cancel_rename(),
            "key.keyboard.escape" => ui.create_field.focused = false,
            "key.keyboard.enter" | "key.keyboard.keypad.enter" if renaming => ui.submit_rename(now),
            "key.keyboard.enter" | "key.keyboard.keypad.enter" => ui.submit_create(now),
            _ => {
                if let Some(f) = ui.focused() {
                    f.key(key);
                }
            }
        }
        true
    })
}

/// Un caractère tapé : dans le champ s'il a le focus.
pub(in crate::game) fn char_typed(game: &Game<'_>, screen: &NativeScreenView<'_>, c: char) -> bool {
    if !active(game, screen) {
        return false;
    }
    m::with(|ui| match ui.focused() {
        Some(f) => {
            f.insert(c);
            true
        }
        None => false,
    })
}

// ---- Une fois par image : ce que l'interface a demandé ------------------------------------------

pub(in crate::game) fn frame(game: &mut Game<'_>) {
    let now = Instant::now();
    let pending = m::with(|ui| {
        ui.tick(now);
        std::mem::take(&mut ui.pending)
    });
    for p in pending {
        match p {
            m::Pending::Send(command) => {
                let _ = game.native_send_chat(&command);
            }
            m::Pending::CloseScreen => {
                if game.screen == Screen::Inventory {
                    game.close_container();
                    game.screen = Screen::Playing;
                    game.capture(true);
                }
            }
            m::Pending::CloseBook => {
                if let Some(b) = game.active_book()
                    && game.book_open(b)
                {
                    game.level.recipe_book.open[b.index()] = false;
                    game.send_book_settings(b);
                    game.recipe_ui.overlay = None;
                }
            }
            m::Pending::Click => game.audio.play("minecraft:ui.button.click", None, 0.25, 1.0),
        }
    }
}

// ---- Chat et commandes ------------------------------------------------------------------------------

/// Un message du serveur : true pour le cacher (la réponse à /homes demandée par le mod).
pub(in crate::game) fn chat(system: bool, overlay: bool, message: &Text) -> bool {
    // Système ou non (selon le serveur) : seule une ligne commençant par « Homes: » compte.
    let _ = system;
    !overlay && m::with(|ui| ui.chat(&message.plain()))
}

pub(in crate::game) fn commands(list: &mut Vec<&'static str>) {
    for c in m::COMMANDS {
        if !list.contains(&c) {
            list.push(c);
        }
    }
}

/// /grade-set, /grade-position, /grade-bouton : true quand c'est une commande du mod.
pub(in crate::game) fn command(game: &mut Game<'_>, command: &str) -> bool {
    let mut settings = settings(game);
    let Some(reply) = m::run_command(command, &mut settings) else {
        return false;
    };
    if reply.changed {
        for (key, value) in settings.entries() {
            game.native_set_setting(m::ID, key, &value);
        }
        m::with(|ui| ui.settings = Some(settings));
    }
    for line in reply.lines {
        game.native_chat(&line);
    }
    true
}

pub(in crate::game) fn listed(mods: &mut Vec<crate::game::native_hooks::NativeMod>) {
    mods.push(crate::game::native_hooks::NativeMod {
        id: m::ID.into(),
        title: format!("{} {}", m::NAME, m::VERSION),
        description: m::DESCRIPTION.into(),
        keys: Vec::new(),
    });
}

// ---- Effets de potion et décalage de l'inventaire ------------------------------------------------

/// Les effets en rangée en haut à gauche (la droite de l'inventaire est aux boutons).
/// None : l'inventaire n'a pas le mod, Horizon les dessine comme d'habitude.
pub(in crate::game) fn effects(game: &Game<'_>, batch: &mut HudBatch) -> Option<Option<[(String, u32); 2]>> {
    if !player_inventory(game) || !enabled(game) {
        return None;
    }
    let mut panels = inventory_effects::panels(
        100_000,
        [0, 0],
        0,
        &game.potions.effects.list,
        game.clock.rate(),
        &|key| game.pack.names.display_get(key),
        &|t| game.gui.atlas.font.width(t).ceil() as i32,
    );
    let boxes = m::effect_boxes(panels.len(), batch.width as i32);
    for (p, [x, y]) in panels.iter_mut().zip(boxes) {
        p.x = x;
        p.y = y;
        p.width = 32;
        p.spacing = 32;
    }
    let tip = inventory_effects::paint(
        batch,
        &game.gui.atlas,
        &panels,
        [game.cursor.0 as i32, game.cursor.1 as i32],
    );
    for p in &panels {
        let w = game.native_text_width(&p.duration);
        batch.text(&game.gui.atlas, &p.duration, p.x as f32 + (32.0 - w) / 2.0, p.y as f32 + 33.0, u32::MAX, true);
    }
    Some(tip)
}

/// Menu des homes ouvert : l'inventaire se décale comme pour le livre de recettes.
pub(in crate::game) fn book_shift(game: &Game<'_>, width: f32) -> Option<f32> {
    let open = m::with(|ui| ui.homes_open);
    (open && width >= m::NARROW as f32 && player_inventory(game) && enabled(game)).then_some(m::SHIFT)
}
