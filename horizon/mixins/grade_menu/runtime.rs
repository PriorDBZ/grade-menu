// Grade Menu pour Horizon : la version native (Rust) du mod Fabric Grade Menu.
// Des boutons à côté de l'inventaire pour les commandes de grade du serveur,
// un menu des homes à la place du livre de recettes, et les effets de potion
// en haut à gauche. Commandes locales : /grade-set, /grade-position, /grade-bouton.
//
// Ce fichier est la partie indépendante du jeu : c'est à la fois la bibliothèque
// Cargo (`cargo test`) et le module `crate::game::grade_menu`. Le côté jeu
// (dessin, clics, clavier, chat) est dans glue.rs.

use std::cell::RefCell;
use std::time::{Duration, Instant};

pub const ID: &str = "grade_menu";
pub const NAME: &str = "Grade Menu";
pub const VERSION: &str = "1.0.0-horizon.1";
pub const DESCRIPTION: &str = "Boutons dans l'inventaire pour les commandes de grade (Confirmé·e, Premium) \
     et menu des homes. Commandes : /grade-set, /grade-position, /grade-bouton.";

// ---- Grades et boutons -------------------------------------------------------------

#[derive(Clone, Copy, Debug, PartialEq, Eq, PartialOrd, Ord)]
pub enum Grade {
    Joueur,
    Confirme,
    Premium,
}

impl Grade {
    pub const ALL: [Grade; 3] = [Grade::Joueur, Grade::Confirme, Grade::Premium];

    pub fn id(self) -> &'static str {
        match self {
            Grade::Joueur => "joueur",
            Grade::Confirme => "confirme",
            Grade::Premium => "premium",
        }
    }

    pub fn label(self) -> &'static str {
        match self {
            Grade::Joueur => "Joueur·euse",
            Grade::Confirme => "₪ Confirmé·e",
            Grade::Premium => "★ Premium",
        }
    }

    /// Accepte les variantes (« confirmé », « confirmée », « joueuse »...) : les 4 premières lettres suffisent.
    pub fn parse(text: &str) -> Option<Grade> {
        let t = text.trim().to_lowercase().replace('é', "e");
        if t.chars().count() < 4 {
            return None;
        }
        Grade::ALL.into_iter().find(|g| t.starts_with(&g.id()[..4]))
    }
}

/// Les boutons, dans l'ordre d'affichage. /trash n'existe volontairement pas :
/// trop dangereux depuis l'inventaire.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum Action {
    Home,
    Homes,
    Spawn,
    Hat,
    Skull,
    Blocks,
    Craft,
    Cartography,
    Loom,
    Smithing,
    Stonecutter,
}

impl Action {
    pub const ALL: [Action; 11] = [
        Action::Home,
        Action::Homes,
        Action::Spawn,
        Action::Hat,
        Action::Skull,
        Action::Blocks,
        Action::Craft,
        Action::Cartography,
        Action::Loom,
        Action::Smithing,
        Action::Stonecutter,
    ];

    /// Identifiant (/grade-bouton, réglages) = nom de l'icône.
    pub fn id(self) -> &'static str {
        match self {
            Action::Home => "home",
            Action::Homes => "homes",
            Action::Spawn => "spawn",
            Action::Hat => "hat",
            Action::Skull => "skull",
            Action::Blocks => "blocks",
            Action::Craft => "craft",
            Action::Cartography => "cartographytable",
            Action::Loom => "loom",
            Action::Smithing => "smithingtable",
            Action::Stonecutter => "stonecutter",
        }
    }

    pub fn title(self) -> &'static str {
        match self {
            Action::Home => "Mon lit",
            Action::Homes => "Mes homes",
            Action::Spawn => "Spawn",
            Action::Hat => "Chapeau",
            Action::Skull => "Ma tête",
            Action::Blocks => "Compression",
            Action::Craft => "Table de craft",
            Action::Cartography => "Table de cartographie",
            Action::Loom => "Métier à tisser",
            Action::Smithing => "Table de forge",
            Action::Stonecutter => "Tailleur de pierre",
        }
    }

    pub fn description(self) -> &'static str {
        match self {
            Action::Home => "Téléportation à ton lit",
            Action::Homes => "Ouvre ou ferme le menu des homes",
            Action::Spawn => "Téléportation au spawn",
            Action::Hat => "Met l'objet tenu sur ta tête",
            Action::Skull => "Te donne ta tête",
            Action::Blocks => "Compresse minerais et lingots en blocs",
            Action::Craft => "Ouvre une table de craft",
            Action::Cartography => "Ouvre une table de cartographie",
            Action::Loom => "Ouvre un métier à tisser",
            Action::Smithing => "Ouvre une table de forge",
            Action::Stonecutter => "Ouvre un tailleur de pierre",
        }
    }

    /// La commande envoyée au serveur, avec son « / » ; None pour le menu des homes.
    pub fn command(self) -> Option<&'static str> {
        match self {
            Action::Home => Some("/home bed"),
            Action::Homes => None,
            Action::Spawn => Some("/spawn"),
            Action::Hat => Some("/hat"),
            Action::Skull => Some("/skull"),
            Action::Blocks => Some("/blocks"),
            Action::Craft => Some("/craft"),
            Action::Cartography => Some("/cartographytable"),
            Action::Loom => Some("/loom"),
            Action::Smithing => Some("/smithingtable"),
            Action::Stonecutter => Some("/stonecutter"),
        }
    }

    pub fn min_grade(self) -> Grade {
        match self {
            Action::Home | Action::Hat | Action::Craft => Grade::Confirme,
            _ => Grade::Premium,
        }
    }

    /// Les téléportations ferment l'inventaire.
    pub fn closes_screen(self) -> bool {
        matches!(self, Action::Home | Action::Spawn)
    }

    pub fn from_id(id: &str) -> Option<Action> {
        Action::ALL.into_iter().find(|a| a.id().eq_ignore_ascii_case(id.trim()))
    }
}

// ---- Réglages (un fichier par clé dans le dossier de données du mod) -----------------

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct Settings {
    pub grade: Grade,
    /// Décalage des boutons : x vers la droite, y vers le haut.
    pub offset: [i32; 2],
    /// Boutons masqués, indépendamment du grade.
    pub hidden: Vec<Action>,
}

impl Default for Settings {
    fn default() -> Self {
        Settings { grade: Grade::Joueur, offset: [0, 0], hidden: Vec::new() }
    }
}

pub const KEYS: [&str; 4] = ["grade", "decalage_x", "decalage_y", "boutons_masques"];

impl Settings {
    pub fn read(get: impl Fn(&str) -> Option<String>) -> Settings {
        let int = |key: &str| get(key).and_then(|v| v.trim().parse().ok()).unwrap_or(0);
        let mut hidden = Vec::new();
        for id in get("boutons_masques").unwrap_or_default().split(',') {
            if let Some(a) = Action::from_id(id)
                && !hidden.contains(&a)
            {
                hidden.push(a);
            }
        }
        Settings {
            grade: get("grade").and_then(|g| Grade::parse(&g)).unwrap_or(Grade::Joueur),
            offset: [int("decalage_x"), int("decalage_y")],
            hidden,
        }
    }

    /// Les valeurs à écrire, clé par clé.
    pub fn entries(&self) -> [(&'static str, String); 4] {
        let hidden: Vec<&str> =
            Action::ALL.into_iter().filter(|a| self.hidden.contains(a)).map(Action::id).collect();
        [
            ("grade", self.grade.id().to_owned()),
            ("decalage_x", self.offset[0].to_string()),
            ("decalage_y", self.offset[1].to_string()),
            ("boutons_masques", hidden.join(",")),
        ]
    }

    pub fn is_hidden(&self, action: Action) -> bool {
        self.hidden.contains(&action)
    }

    pub fn set_hidden(&mut self, action: Action, hide: bool) {
        self.hidden.retain(|a| *a != action);
        if hide {
            self.hidden.push(action);
        }
    }

    /// Boutons auxquels le grade donne droit.
    pub fn available(&self) -> Vec<Action> {
        Action::ALL.into_iter().filter(|a| self.grade >= a.min_grade()).collect()
    }

    /// Boutons affichés : ceux du grade, sauf les masqués, sans trou.
    pub fn shown(&self) -> Vec<Action> {
        self.available().into_iter().filter(|a| !self.is_hidden(*a)).collect()
    }
}

// ---- Homes -----------------------------------------------------------------------------

/// Home posé par le serveur sur le dernier lit : ni renommable ni supprimable.
pub const BED: &str = "bed";

pub fn valid_name(name: &str) -> bool {
    let n = name.chars().count();
    (1..=32).contains(&n) && name.chars().all(|c| c.is_ascii_alphanumeric() || c == '_' || c == '-')
}

/// Caractères acceptés dans un nom de home.
pub fn name_char(c: char) -> bool {
    c.is_ascii_alphanumeric() || c == '_' || c == '-'
}

fn strip_legacy(text: &str) -> String {
    let mut out = String::with_capacity(text.len());
    let mut chars = text.chars();
    while let Some(c) = chars.next() {
        if c == '§' {
            chars.next();
        } else {
            out.push(c);
        }
    }
    out
}

/// La réponse de /homes (« Homes: redtop, mizuland, base, bed »), triée : le lit
/// d'abord, puis l'ordre alphabétique. None pour tout autre message.
pub fn parse_homes_message(text: &str) -> Option<Vec<String>> {
    let plain = strip_legacy(text);
    let rest = plain.trim().strip_prefix("Homes:")?;
    let mut names: Vec<String> =
        rest.split(',').map(|s| s.trim().to_owned()).filter(|s| !s.is_empty()).collect();
    names.sort_by_key(|n| (!n.eq_ignore_ascii_case(BED), n.to_lowercase()));
    Some(names)
}

/// Pourquoi ce nom ne convient pas (None s'il convient, Some("") s'il est vide).
/// `current` : nom actuel du home renommé (on peut changer seulement la casse).
pub fn name_problem(name: &str, current: Option<&str>, homes: &[String]) -> Option<&'static str> {
    if name.is_empty() {
        return Some("");
    }
    if !valid_name(name) {
        return Some("Lettres, chiffres, _ et - seulement");
    }
    if name.eq_ignore_ascii_case(BED) {
        return Some("« bed » est réservé à ton lit");
    }
    let same = current.is_some_and(|c| c.eq_ignore_ascii_case(name));
    if !same && homes.iter().any(|h| h.eq_ignore_ascii_case(name)) {
        return Some("Ce nom existe déjà");
    }
    None
}

// ---- Commandes locales (/grade-...) -------------------------------------------------

pub const COMMANDS: [&str; 3] = ["grade-set", "grade-position", "grade-bouton"];
const PREFIX: &str = "[Grade Menu] ";
const LIMIT: i32 = 2000;

/// Ce qu'une commande du mod a produit : des lignes pour le chat, et si les réglages ont changé.
#[derive(Debug, PartialEq, Eq)]
pub struct Reply {
    pub lines: Vec<String>,
    pub changed: bool,
}

fn say(lines: Vec<String>, changed: bool) -> Option<Reply> {
    Some(Reply { lines, changed })
}

/// Une commande tapée (sans « / »). None si ce n'est pas une commande du mod.
pub fn run_command(command: &str, s: &mut Settings) -> Option<Reply> {
    let mut words = command.split_whitespace();
    let name = words.next()?;
    let args: Vec<&str> = words.collect();
    match name {
        "grade-set" => grade_set(&args, s),
        "grade-position" => grade_position(&args, s),
        "grade-bouton" => grade_bouton(&args, s),
        _ => None,
    }
}

fn grade_set(args: &[&str], s: &mut Settings) -> Option<Reply> {
    let Some(arg) = args.first() else {
        return say(
            vec![
                format!("{PREFIX}grade actuel : {}", s.grade.label()),
                "Utilisation : /grade-set joueur | confirme | premium".into(),
            ],
            false,
        );
    };
    let Some(grade) = Grade::parse(arg) else {
        return say(vec![format!("{PREFIX}grade inconnu « {arg} » : joueur, confirme ou premium.")], false);
    };
    s.grade = grade;
    let shown = s.shown().len();
    let hidden = s.available().len() - shown;
    let mut line = format!("{PREFIX}grade réglé sur {} : ", grade.label());
    if shown == 0 {
        line.push_str("aucun bouton dans l'inventaire.");
    } else {
        line.push_str(&format!("{shown} boutons dans l'inventaire (sur un serveur)."));
    }
    if hidden > 0 {
        line.push_str(&format!(" {hidden} masqué{} avec /grade-bouton.", if hidden > 1 { "s" } else { "" }));
    }
    say(vec![line], true)
}

/// « 10 vers la droite, 3 vers le haut », ou « aucun (place d'origine) ».
pub fn describe_offset(x: i32, y: i32) -> String {
    if x == 0 && y == 0 {
        return "aucun (place d'origine)".into();
    }
    let mut parts = Vec::new();
    if x != 0 {
        parts.push(format!("{} vers la {}", x.abs(), if x > 0 { "droite" } else { "gauche" }));
    }
    if y != 0 {
        parts.push(format!("{} vers le {}", y.abs(), if y > 0 { "haut" } else { "bas" }));
    }
    parts.join(", ")
}

fn grade_position(args: &[&str], s: &mut Settings) -> Option<Reply> {
    match args {
        [] => say(
            vec![
                format!("{PREFIX}décalage actuel : {}", describe_offset(s.offset[0], s.offset[1])),
                "/grade-position <x> <y> : x vers la droite, y vers le haut (en pixels)".into(),
                "/grade-position reset : place d'origine".into(),
            ],
            false,
        ),
        ["reset"] => {
            s.offset = [0, 0];
            say(vec![format!("{PREFIX}boutons remis à leur place d'origine.")], true)
        }
        [x, y] => match (x.parse::<i32>(), y.parse::<i32>()) {
            (Ok(x), Ok(y)) if x.abs() <= LIMIT && y.abs() <= LIMIT => {
                s.offset = [x, y];
                let text = if x == 0 && y == 0 {
                    "boutons remis à leur place d'origine.".to_owned()
                } else {
                    format!("boutons décalés : {}.", describe_offset(x, y))
                };
                say(vec![format!("{PREFIX}{text} Ouvre ton inventaire pour voir.")], true)
            }
            _ => say(vec![format!("{PREFIX}deux nombres entiers entre -{LIMIT} et {LIMIT}, ou reset.")], false),
        },
        _ => say(vec![format!("{PREFIX}utilisation : /grade-position <x> <y>, ou /grade-position reset")], false),
    }
}

fn grade_bouton(args: &[&str], s: &mut Settings) -> Option<Reply> {
    match args {
        [] => {
            let available = s.available();
            let mut lines = vec![format!("{PREFIX}boutons (grade {}) :", s.grade.label())];
            for a in Action::ALL {
                let mut line = format!(
                    " {} {} ({})",
                    if s.is_hidden(a) { "✘" } else { "✔" },
                    a.title(),
                    a.id()
                );
                if !available.contains(&a) {
                    line.push_str(&format!(" · {}", a.min_grade().label()));
                }
                lines.push(line);
            }
            lines.push("/grade-bouton <bouton> on|off · /grade-bouton reset".into());
            say(lines, false)
        }
        ["reset"] => {
            s.hidden.clear();
            say(vec![format!("{PREFIX}tous les boutons sont de nouveau affichés.")], true)
        }
        [id, rest @ ..] => {
            let Some(action) = Action::from_id(id) else {
                let ids: Vec<&str> = Action::ALL.into_iter().map(Action::id).collect();
                return say(vec![format!("{PREFIX}bouton inconnu « {id} ». Boutons : {}", ids.join(", "))], false);
            };
            let hide = match rest {
                [] => !s.is_hidden(action),
                ["off"] => true,
                ["on"] => false,
                _ => return say(vec![format!("{PREFIX}utilisation : /grade-bouton {} on|off", action.id())], false),
            };
            s.set_hidden(action, hide);
            say(
                vec![format!(
                    "{PREFIX}« {} » {}. Gardé même si tu changes de grade.",
                    action.title(),
                    if hide { "masqué" } else { "affiché" }
                )],
                true,
            )
        }
    }
}

// ---- Disposition (en pixels d'interface) ---------------------------------------------

pub const BUTTON: i32 = 20;
pub const STEP: i32 = 21;
pub const PER_COLUMN: i32 = 6;
pub const PANEL_W: i32 = 147;
pub const PANEL_H: i32 = 166;
pub const ROWS: usize = 5;
pub const ROW_W: i32 = 95;
pub const ROW_FULL_W: i32 = 133;
/// Comme le livre de recettes : en dessous, le menu passerait sous les objets.
pub const NARROW: i32 = 379;
/// Décalage de l'inventaire quand le menu des homes est ouvert (celui du livre).
pub const SHIFT: f32 = 77.0;

/// Position des boutons : en colonnes de 6 à droite de l'inventaire, sinon en
/// rangées au-dessus, puis le décalage de /grade-position (y positif = vers le haut).
pub fn button_rects(count: usize, panel: [i32; 4], screen: [i32; 2], offset: [i32; 2]) -> Vec<[i32; 4]> {
    let count = count as i32;
    let [left, top, width, _] = panel;
    let columns = (count + PER_COLUMN - 1) / PER_COLUMN;
    let x0 = left + width + 3;
    let fits_right = x0 + columns * STEP - 1 <= screen[0] - 2;
    let per_row = ((width + 1) / STEP).max(1);
    let rows_above = (count + per_row - 1) / per_row;
    let y_above = top - rows_above * STEP - 2;
    let above = !fits_right && y_above >= 0;
    (0..count)
        .map(|i| {
            let (x, y) = if above {
                (left + (i % per_row) * STEP, y_above + (i / per_row) * STEP)
            } else {
                (x0 + (i / PER_COLUMN) * STEP, top + (i % PER_COLUMN) * STEP)
            };
            [x + offset[0], y - offset[1], BUTTON, BUTTON]
        })
        .collect()
}

/// Le menu des homes, à la place du livre de recettes.
pub fn homes_panel(screen: [i32; 2]) -> [i32; 4] {
    [(screen[0] - PANEL_W) / 2 - 86, (screen[1] - PANEL_H) / 2, PANEL_W, PANEL_H]
}

/// Effets de potion : cases de 32 en rangée en haut à gauche (durée dessous).
pub fn effect_boxes(count: usize, screen_width: i32) -> Vec<[i32; 2]> {
    let per_row = ((screen_width - 8) / 34).max(1);
    (0..count as i32).map(|i| [4 + (i % per_row) * 34, 4 + (i / per_row) * 44]).collect()
}

pub fn inside(r: [i32; 4], p: [f32; 2]) -> bool {
    p[0] >= r[0] as f32 && p[0] < (r[0] + r[2]) as f32 && p[1] >= r[1] as f32 && p[1] < (r[1] + r[3]) as f32
}

// ---- Champ de texte -------------------------------------------------------------------

#[derive(Clone, Debug, Default, PartialEq, Eq)]
pub struct Field {
    pub text: String,
    /// Position du curseur, en caractères.
    pub cursor: usize,
    pub focused: bool,
}

impl Field {
    pub fn set(&mut self, text: &str) {
        self.text = text.to_owned();
        self.cursor = self.text.chars().count();
    }

    fn byte(&self, chars: usize) -> usize {
        self.text.char_indices().nth(chars).map_or(self.text.len(), |(i, _)| i)
    }

    pub fn insert(&mut self, c: char) {
        if self.text.chars().count() >= 32 || !name_char(c) {
            return;
        }
        let at = self.byte(self.cursor);
        self.text.insert(at, c);
        self.cursor += 1;
    }

    /// Une touche d'édition ; false si ce n'en est pas une.
    pub fn key(&mut self, key: &str) -> bool {
        let len = self.text.chars().count();
        match key {
            "key.keyboard.backspace" if self.cursor > 0 => {
                let (a, b) = (self.byte(self.cursor - 1), self.byte(self.cursor));
                self.text.replace_range(a..b, "");
                self.cursor -= 1;
            }
            "key.keyboard.delete" if self.cursor < len => {
                let (a, b) = (self.byte(self.cursor), self.byte(self.cursor + 1));
                self.text.replace_range(a..b, "");
            }
            "key.keyboard.left" => self.cursor = self.cursor.saturating_sub(1),
            "key.keyboard.right" => self.cursor = (self.cursor + 1).min(len),
            "key.keyboard.home" => self.cursor = 0,
            "key.keyboard.end" => self.cursor = len,
            "key.keyboard.backspace" | "key.keyboard.delete" => {}
            _ => return false,
        }
        true
    }
}

// ---- État de l'interface (fil du jeu) ----------------------------------------------------

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum HomesState {
    NotLoaded,
    Loading,
    Loaded,
    NoResponse,
}

/// Ce que l'interface a demandé et que le côté jeu fera à l'image suivante
/// (les hooks d'écran n'ont pas le droit de modifier le jeu).
#[derive(Clone, Debug, PartialEq, Eq)]
pub enum Pending {
    Send(String),
    CloseScreen,
    CloseBook,
    Click,
}

/// Ce qu'il y a sous un point de l'interface (mémorisé à chaque image pour les clics).
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum Hit {
    Action(Action),
    Row(usize),
    Rename(usize),
    Delete(usize),
    PreviousPage,
    NextPage,
    CreateField,
    RenameField,
    Add,
}

pub const DELETE_CONFIRM: Duration = Duration::from_secs(3);
const TIMEOUT: Duration = Duration::from_secs(5);
const REFRESH_DELAY: Duration = Duration::from_millis(750);

#[derive(Debug)]
pub struct Ui {
    pub settings: Option<Settings>,
    pub homes_open: bool,
    pub page: usize,
    pub homes: Vec<String>,
    pub homes_state: HomesState,
    pub requested_at: Option<Instant>,
    pub refresh_at: Option<Instant>,
    pub renaming: Option<String>,
    pub rename_field: Field,
    pub create_field: Field,
    pub armed: Option<(String, Instant)>,
    pub pending: Vec<Pending>,
    /// Zones cliquables de la dernière image.
    pub hits: Vec<([i32; 4], Hit)>,
    /// Zones du mod de la dernière image (un clic là ne va pas à l'inventaire).
    pub areas: Vec<[i32; 4]>,
    /// Le mod était affiché sur l'inventaire à la dernière image.
    pub visible: bool,
}

impl Default for Ui {
    fn default() -> Self {
        Ui {
            settings: None,
            homes_open: false,
            page: 0,
            homes: Vec::new(),
            homes_state: HomesState::NotLoaded,
            requested_at: None,
            refresh_at: None,
            renaming: None,
            rename_field: Field::default(),
            create_field: Field::default(),
            armed: None,
            pending: Vec::new(),
            hits: Vec::new(),
            areas: Vec::new(),
            visible: false,
        }
    }
}

thread_local! {
    static UI: RefCell<Ui> = RefCell::new(Ui::default());
}

pub fn with<R>(f: impl FnOnce(&mut Ui) -> R) -> R {
    UI.with(|ui| f(&mut ui.borrow_mut()))
}

impl Ui {
    pub fn settings(&mut self, load: impl Fn(&str) -> Option<String>) -> &Settings {
        self.settings.get_or_insert_with(|| Settings::read(load))
    }

    pub fn request_homes(&mut self, now: Instant) {
        if self.homes_state == HomesState::Loading
            || self.requested_at.is_some_and(|t| now.duration_since(t) < Duration::from_secs(2))
        {
            return;
        }
        self.pending.push(Pending::Send("/homes".into()));
        self.homes_state = HomesState::Loading;
        self.requested_at = Some(now);
    }

    /// Une commande qui change les homes, puis une relecture de la liste un peu après.
    pub fn send_and_refresh(&mut self, command: String, now: Instant) {
        self.pending.push(Pending::Send(command));
        self.refresh_at = Some(now + REFRESH_DELAY);
    }

    pub fn tick(&mut self, now: Instant) {
        if self.homes_state == HomesState::Loading
            && self.requested_at.is_some_and(|t| now.duration_since(t) > TIMEOUT)
        {
            self.homes_state = HomesState::NoResponse;
        }
        if self.refresh_at.is_some_and(|t| now >= t) {
            self.refresh_at = None;
            self.requested_at = None;
            if self.homes_state == HomesState::Loading {
                self.homes_state = HomesState::Loaded;
            }
            self.request_homes(now);
        }
    }

    /// Un message du serveur : true pour le cacher (la réponse à /homes demandée par le mod).
    pub fn chat(&mut self, text: &str) -> bool {
        if self.homes_state != HomesState::Loading {
            return false;
        }
        match parse_homes_message(text) {
            Some(homes) => {
                self.homes = homes;
                self.homes_state = HomesState::Loaded;
                if self.renaming.as_ref().is_some_and(|r| !self.homes.contains(r)) {
                    self.cancel_rename();
                }
                true
            }
            None => false,
        }
    }

    pub fn pages(&self) -> usize {
        self.homes.len().div_ceil(ROWS).max(1)
    }

    pub fn is_armed(&self, name: &str, now: Instant) -> bool {
        self.armed.as_ref().is_some_and(|(n, t)| n == name && now.duration_since(*t) < DELETE_CONFIRM)
    }

    /// Premier clic : arme la suppression ; second clic dans les 3 s : supprime.
    pub fn delete(&mut self, name: &str, now: Instant) {
        if self.is_armed(name, now) {
            self.armed = None;
            self.send_and_refresh(format!("/delhome {name}"), now);
        } else {
            self.armed = Some((name.to_owned(), now));
        }
    }

    pub fn start_rename(&mut self, name: &str) {
        self.renaming = Some(name.to_owned());
        self.rename_field.set(name);
        self.rename_field.focused = true;
        self.create_field.focused = false;
    }

    pub fn cancel_rename(&mut self) {
        self.renaming = None;
        self.rename_field.focused = false;
    }

    pub fn submit_rename(&mut self, now: Instant) {
        let Some(old) = self.renaming.clone() else {
            return;
        };
        let new = self.rename_field.text.trim().to_owned();
        if new == old {
            self.cancel_rename();
        } else if name_problem(&new, Some(&old), &self.homes).is_none() {
            self.send_and_refresh(format!("/renamehome {old} {new}"), now);
            self.cancel_rename();
        }
    }

    pub fn submit_create(&mut self, now: Instant) {
        let name = self.create_field.text.trim().to_owned();
        if name_problem(&name, None, &self.homes).is_none() {
            self.send_and_refresh(format!("/sethome {name}"), now);
            self.create_field = Field::default();
        }
    }

    /// Le champ qui reçoit le clavier, s'il y en a un.
    pub fn focused(&mut self) -> Option<&mut Field> {
        if self.renaming.is_some() && self.rename_field.focused {
            Some(&mut self.rename_field)
        } else if self.homes_open && self.create_field.focused {
            Some(&mut self.create_field)
        } else {
            None
        }
    }

    pub fn close_homes(&mut self) {
        self.homes_open = false;
        self.cancel_rename();
        self.create_field.focused = false;
    }

    /// Le message sous la liste : le problème du nom en cours de saisie.
    pub fn status(&self) -> Option<&'static str> {
        let problem = match &self.renaming {
            Some(old) => name_problem(self.rename_field.text.trim(), Some(old), &self.homes),
            None => name_problem(self.create_field.text.trim(), None, &self.homes),
        };
        problem.filter(|p| !p.is_empty())
    }

    /// Le message au milieu de la liste quand elle est vide.
    pub fn center_message(&self) -> Option<&'static str> {
        if !self.homes.is_empty() {
            return None;
        }
        Some(match self.homes_state {
            HomesState::NotLoaded | HomesState::Loading => "Chargement…",
            HomesState::NoResponse => "Pas de réponse du serveur",
            HomesState::Loaded => "Aucun home",
        })
    }

    /// Un clic gauche sur `hit`.
    pub fn click(&mut self, hit: Hit, now: Instant, narrow: bool) {
        // Un clic ailleurs que sur le champ en cours le quitte (et annule un renommage).
        if hit != Hit::RenameField && self.renaming.is_some() {
            self.cancel_rename();
        }
        if hit != Hit::CreateField {
            self.create_field.focused = false;
        }
        let row_name = |ui: &Ui, i: usize| ui.homes.get(ui.page * ROWS + i).cloned();
        match hit {
            Hit::Action(Action::Homes) => {
                if self.homes_open {
                    self.close_homes();
                } else if !narrow {
                    self.homes_open = true;
                    self.pending.push(Pending::CloseBook);
                    self.request_homes(now);
                }
            }
            Hit::Action(a) => {
                if let Some(c) = a.command() {
                    self.pending.push(Pending::Send(c.to_owned()));
                }
                if a.closes_screen() {
                    self.pending.push(Pending::CloseScreen);
                }
            }
            Hit::Row(i) => {
                if let Some(name) = row_name(self, i) {
                    self.pending.push(Pending::Send(format!("/home {name}")));
                    self.pending.push(Pending::CloseScreen);
                }
            }
            Hit::Rename(i) => {
                if let Some(name) = row_name(self, i) {
                    self.start_rename(&name);
                }
            }
            Hit::Delete(i) => {
                if let Some(name) = row_name(self, i) {
                    self.delete(&name, now);
                }
            }
            Hit::PreviousPage => self.page = self.page.saturating_sub(1),
            Hit::NextPage => self.page = (self.page + 1).min(self.pages() - 1),
            Hit::CreateField => self.create_field.focused = true,
            Hit::RenameField => self.rename_field.focused = true,
            Hit::Add => self.submit_create(now),
        }
        self.pending.push(Pending::Click);
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn settings(map: &[(&str, &str)]) -> Settings {
        Settings::read(|k| map.iter().find(|(key, _)| *key == k).map(|(_, v)| v.to_string()))
    }

    #[test]
    fn grades_et_variantes() {
        assert_eq!(Grade::parse("confirmé"), Some(Grade::Confirme));
        assert_eq!(Grade::parse("Confirmée"), Some(Grade::Confirme));
        assert_eq!(Grade::parse("joueuse"), Some(Grade::Joueur));
        assert_eq!(Grade::parse("PREMIUM"), Some(Grade::Premium));
        assert_eq!(Grade::parse("vip"), None);
    }

    #[test]
    fn boutons_par_grade_sans_trash() {
        let mut s = Settings::default();
        assert!(s.shown().is_empty());
        s.grade = Grade::Confirme;
        assert_eq!(s.shown(), vec![Action::Home, Action::Hat, Action::Craft]);
        s.grade = Grade::Premium;
        assert_eq!(s.shown().len(), 11);
        assert_eq!(Action::Home.command(), Some("/home bed"));
        assert!(Action::ALL.iter().all(|a| a.id() != "trash" && a.command() != Some("/trash")));
    }

    #[test]
    fn reglages_relus_et_masquage_independant_du_grade() {
        let s = settings(&[("grade", "premium"), ("decalage_x", "10"), ("decalage_y", "3"), ("boutons_masques", "craft, trash ,loom")]);
        assert_eq!(s.grade, Grade::Premium);
        assert_eq!(s.offset, [10, 3]);
        assert_eq!(s.hidden, vec![Action::Craft, Action::Loom]);
        assert_eq!(s.shown().len(), 9);
        assert_eq!(s.shown()[6], Action::Cartography);
        let mut confirme = s.clone();
        confirme.grade = Grade::Confirme;
        assert_eq!(confirme.shown(), vec![Action::Home, Action::Hat]);
        let entries = s.entries();
        assert_eq!(entries[3], ("boutons_masques", "craft,loom".to_owned()));
        let relu = settings(&entries.iter().map(|(k, v)| (*k, v.as_str())).collect::<Vec<_>>());
        assert_eq!(relu, s);
        assert_eq!(settings(&[("decalage_x", "abc")]).offset, [0, 0]);
    }

    #[test]
    fn reponse_homes() {
        assert_eq!(
            parse_homes_message("Homes: redtop, mizuland, base, bed"),
            Some(vec!["bed".into(), "base".into(), "mizuland".into(), "redtop".into()])
        );
        assert_eq!(parse_homes_message("§6Homes: §rbase, §cbed"), Some(vec!["bed".into(), "base".into()]));
        assert_eq!(parse_homes_message("<Bob> Homes: lol"), None);
        assert_eq!(parse_homes_message("Bienvenue"), None);
    }

    #[test]
    fn la_reponse_n_est_cachee_que_si_le_mod_l_a_demandee() {
        let mut ui = Ui::default();
        assert!(!ui.chat("Homes: base, bed"));
        ui.request_homes(Instant::now());
        assert_eq!(ui.pending, vec![Pending::Send("/homes".into())]);
        assert!(!ui.chat("Bienvenue"));
        assert!(ui.chat("Homes: base, bed"));
        assert_eq!(ui.homes_state, HomesState::Loaded);
    }

    #[test]
    fn noms_de_homes() {
        let homes = vec!["bed".to_owned(), "base".to_owned()];
        assert_eq!(name_problem("", None, &homes), Some(""));
        assert_eq!(name_problem("ma base", None, &homes), Some("Lettres, chiffres, _ et - seulement"));
        assert_eq!(name_problem("BED", None, &homes), Some("« bed » est réservé à ton lit"));
        assert_eq!(name_problem("Base", None, &homes), Some("Ce nom existe déjà"));
        assert_eq!(name_problem("Base", Some("base"), &homes), None);
        assert_eq!(name_problem("nord_2", None, &homes), None);
    }

    #[test]
    fn suppression_en_deux_clics() {
        let mut ui = Ui::default();
        let t = Instant::now();
        ui.delete("base", t);
        assert!(ui.pending.is_empty());
        ui.delete("base", t + Duration::from_secs(1));
        assert_eq!(ui.pending, vec![Pending::Send("/delhome base".into())]);
        let mut ui = Ui::default();
        ui.delete("base", t);
        ui.delete("base", t + Duration::from_secs(4));
        assert!(ui.pending.is_empty(), "trop tard : le second clic réarme");
    }

    #[test]
    fn creation_et_renommage() {
        let t = Instant::now();
        let mut ui = Ui { homes: vec!["bed".into(), "base".into()], ..Ui::default() };
        ui.create_field.set("nord");
        ui.submit_create(t);
        assert_eq!(ui.pending, vec![Pending::Send("/sethome nord".into())]);
        assert!(ui.create_field.text.is_empty());
        ui.pending.clear();
        ui.start_rename("base");
        ui.rename_field.set("maison");
        ui.submit_rename(t);
        assert_eq!(ui.pending, vec![Pending::Send("/renamehome base maison".into())]);
        assert_eq!(ui.renaming, None);
    }

    #[test]
    fn clics_sur_les_boutons() {
        let t = Instant::now();
        let mut ui = Ui::default();
        ui.click(Hit::Action(Action::Home), t, false);
        assert_eq!(ui.pending, vec![Pending::Send("/home bed".into()), Pending::CloseScreen, Pending::Click]);
        let mut ui = Ui::default();
        ui.click(Hit::Action(Action::Homes), t, false);
        assert!(ui.homes_open);
        assert!(ui.pending.contains(&Pending::CloseBook));
        assert!(ui.pending.contains(&Pending::Send("/homes".into())));
        let mut ui = Ui::default();
        ui.click(Hit::Action(Action::Homes), t, true);
        assert!(!ui.homes_open, "fenêtre trop étroite");
    }

    #[test]
    fn commandes() {
        let mut s = Settings::default();
        assert!(run_command("homes", &mut s).is_none());
        let r = run_command("grade-set premium", &mut s).unwrap();
        assert!(r.changed);
        assert_eq!(s.grade, Grade::Premium);
        let r = run_command("grade-position 10 3", &mut s).unwrap();
        assert!(r.changed && r.lines[0].contains("10 vers la droite, 3 vers le haut"));
        assert_eq!(s.offset, [10, 3]);
        run_command("grade-position reset", &mut s);
        assert_eq!(s.offset, [0, 0]);
        assert!(!run_command("grade-position 1 x", &mut s).unwrap().changed);
        run_command("grade-bouton craft off", &mut s);
        assert!(s.is_hidden(Action::Craft));
        run_command("grade-set confirme", &mut s);
        assert!(s.is_hidden(Action::Craft), "masqué même après un changement de grade");
        run_command("grade-bouton craft", &mut s);
        assert!(!s.is_hidden(Action::Craft));
        run_command("grade-bouton loom off", &mut s);
        run_command("grade-bouton reset", &mut s);
        assert!(s.hidden.is_empty());
        assert_eq!(run_command("grade-bouton", &mut s).unwrap().lines.len(), 13);
        assert!(COMMANDS.iter().all(|c| c.starts_with("grade-")));
    }

    #[test]
    fn disposition() {
        let r = button_rects(11, [152, 52, 176, 166], [480, 270], [0, 0]);
        assert_eq!(r[0], [331, 52, 20, 20]);
        assert_eq!(r[5], [331, 157, 20, 20]);
        assert_eq!(r[6], [352, 52, 20, 20]);
        let r = button_rects(3, [152, 52, 176, 166], [480, 270], [10, 3]);
        assert_eq!(r[0], [341, 49, 20, 20]);
        // pas de place à droite : au-dessus de l'inventaire
        let r = button_rects(11, [200, 60, 176, 166], [390, 290], [0, 0]);
        assert_eq!(r[0], [200, 60 - 2 * 21 - 2, 20, 20]);
        assert_eq!(homes_panel([480, 270]), [80, 52, 147, 166]);
        assert_eq!(effect_boxes(3, 480), vec![[4, 4], [38, 4], [72, 4]]);
    }

    #[test]
    fn champ_de_texte() {
        let mut f = Field::default();
        for c in "ba se".chars() {
            f.insert(c);
        }
        assert_eq!(f.text, "base");
        assert!(f.key("key.keyboard.left"));
        assert!(f.key("key.keyboard.backspace"));
        assert_eq!(f.text, "bae");
        assert!(f.key("key.keyboard.home"));
        assert!(f.key("key.keyboard.delete"));
        assert_eq!(f.text, "ae");
        assert!(!f.key("key.keyboard.e"));
        for _ in 0..40 {
            f.insert('x');
        }
        assert_eq!(f.text.chars().count(), 32);
    }
}
