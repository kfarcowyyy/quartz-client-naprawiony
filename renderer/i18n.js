/* Original UI copy translations for Quartz Client. */
(() => {
  const en = {
    Kosmetyki: 'Cosmetics',
    Graj: 'Play', Instancje: 'Instances', Przeglądaj: 'Browse', Konto: 'Account', Ustawienia: 'Settings',
    Niezalogowano: 'Not signed in', 'Wejdź w zakładkę Konto': 'Open the Account tab',
    'Gotowy do gry?': 'Ready to play?', 'Wybierz instancję i uruchom. Wszystko pobierze się samo.': 'Choose an instance and launch. The required files download automatically.',
    'Utwórz pierwszą instancję': 'Create your first instance', Dziennik: 'Log', 'Instancje': 'Instances',
    'Kliknij instancję, żeby zarządzać modami, resourcepackami i shaderami.': 'Select an instance to manage mods, resource packs, and shaders.',
    'Nowa instancja': 'New instance', Folder: 'Folder', Usuń: 'Delete',
    'Brak instancji - utwórz pierwszą.': 'No instances yet. Create one to get started.',
    Nazwa: 'Name', 'Moja instancja': 'My instance', 'Pokaż snapshoty': 'Show snapshots', 'Wersja Minecrafta': 'Minecraft version',
    Loader: 'Loader', 'Fabric (mody)': 'Fabric (mods)', 'Quilt (mody)': 'Quilt (mods)', 'Vanilla (bez modów)': 'Vanilla (no mods)',
    'Simple Voice Chat': 'Simple Voice Chat', 'Paczka optymalizacji (Sodium, Lithium, Iris, Mod Menu...)': 'Performance mod pack (Sodium, Lithium, Iris, Mod Menu...)',
    Anuluj: 'Cancel', Utwórz: 'Create', 'Tworzę...': 'Creating...', 'Instancja gotowa': 'Instance ready',
    'Modrinth i CurseForge w jednym miejscu.': 'Modrinth and CurseForge in one place.', Mody: 'Mods', Modpacki: 'Modpacks',
    'Resource packi': 'Resource packs', Shadery: 'Shaders', 'Szukaj...': 'Search...', Szukaj: 'Search',
    'Zainstaluj': 'Install', 'Nowa instancja': 'New instance', 'Najpierw utwórz instancję.': 'Create an instance first.',
    'Wpisz frazę albo kliknij Szukaj, żeby zobaczyć popularne.': 'Enter a search term or click Search to browse popular projects.',
    'Modpack zainstaluje się jako nowa instancja.': 'The modpack will be installed as a new instance.',
    'Zainstalowano:': 'Installed:', 'Konto': 'Account', 'Wybierz sposób logowania.': 'Choose a sign-in method.',
    'Konto Microsoft / Xbox (premium)': 'Microsoft / Xbox account (premium)',
    'Logowanie kontem Xbox lub Microsoft - to to samo konto. Pełne wejście na serwery premium.': 'Sign in with your Microsoft account to access premium servers.',
    'Zaloguj przez Xbox / Microsoft': 'Sign in with Microsoft', 'Tryb offline': 'Offline mode',
    'Wpisz sam nick. Działa na serwerach z wyłączonym online-mode i w grze jednoosobowej.': 'Enter a player name for single-player or servers with online mode disabled.',
    Nick: 'Player name', 'Użyj nicku': 'Use player name', Wyloguj: 'Sign out', Zalogowano: 'Signed in', 'Zapisano nick': 'Player name saved',
    'Zapisują się automatycznie.': 'Changes are saved automatically.', 'Pamięć RAM:': 'Memory (RAM):',
    'Automatycznie dodawaj Simple Voice Chat (każda instancja i modpack)': 'Install Simple Voice Chat automatically in new instances and modpacks',
    'Paczka optymalizacji domyślnie przy nowych instancjach': 'Install performance mods by default in new instances',
    'Klucz API CurseForge (darmowy z console.curseforge.com)': 'CurseForge API key (free at console.curseforge.com)',
    'Własna ścieżka Javy (puste = automatycznie)': 'Custom Java path (leave blank for automatic selection)',
    'Wszystko': 'All', Mods: 'Mods', Resourcepacks: 'Resource packs', folder: 'Folder',
    'Otwórz folder': 'Open folder', Graj: 'Play', 'Szukaj w plikach...': 'Search files...',
    'Znajdź online': 'Browse online', 'Dodaj plik': 'Add file', 'Nie znaleziono instancji.': 'Instance not found.',
    'Wczytuję...': 'Loading...', 'Nic nie znaleziono.': 'No results found.',
    'Tu jeszcze nic nie ma. Dodaj plik albo znajdź coś online.': 'Nothing here yet. Add a file or browse online.',
    'wyłączony': 'disabled', 'Usunąć tę instancję razem z jej plikami?': 'Delete this instance and all its files?',
    'Najpierw utwórz instancję': 'Create an instance first', 'Nie znaleziono instancji': 'Instance not found',
    'Wybierz instancję': 'Select an instance', 'Najpierw zaloguj się w zakładce Konto': 'Sign in from the Account tab first',
    'Gra się nie uruchomiła - sprawdź dziennik': 'The game did not start. Check the log.',
    'Utwórz instancję': 'Create an instance', 'Instaluję dodatki...': 'Installing add-ons...',
    'Logowanie...': 'Signing in...', 'Przygotowuję loader...': 'Preparing the loader...',
    'Sprawdzam Simple Voice Chat...': 'Checking Simple Voice Chat...', 'Pobieram i uruchamiam grę...': 'Downloading and launching the game...',
    'Sprawdzam Quartz Client mod...': 'Checking the Quartz Client mod...',
    'Gra uruchomiona': 'Game launched', 'Lista wersji:': 'Version list:', 'Gra zamknięta': 'Game closed',
    'Dodano plików:': 'Files added:',
  };
  const attrs = ['placeholder', 'title', 'aria-label'];
  let language = 'en';
  const pl = Object.fromEntries(Object.entries(en).map(([source, translated]) => [translated, source]));
  const dict = () => language === 'en' ? en : pl;
  const translate = (value) => {
    const map = dict();
    if (!map) return value;
    const trimmed = value.trim();
    if (Object.prototype.hasOwnProperty.call(map, trimmed)) {
      return value.replace(trimmed, map[trimmed]);
    }
    return value;
  };
  function translateNode(node) {
    const translated = translate(node.nodeValue);
    if (translated !== node.nodeValue) node.nodeValue = translated;
  }
  function apply(root = document.body) {
    if (root.nodeType === Node.TEXT_NODE) {
      translateNode(root);
      return;
    }
    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
    let node;
    while ((node = walker.nextNode())) translateNode(node);
    if (root.nodeType === Node.ELEMENT_NODE) {
      for (const attr of attrs) if (root.hasAttribute(attr)) root.setAttribute(attr, translate(root.getAttribute(attr)));
    }
    root.querySelectorAll?.('*').forEach((el) => {
      for (const attr of attrs) if (el.hasAttribute(attr)) el.setAttribute(attr, translate(el.getAttribute(attr)));
    });
    document.documentElement.lang = language;
  }
  window.quartzI18n = {
    setLanguage(value) { language = value === 'pl' ? 'pl' : 'en'; apply(); },
    apply,
  };
  new MutationObserver((records) => {
    for (const record of records) {
      if (record.type === 'characterData') translateNode(record.target);
      for (const node of record.addedNodes || []) if (node.nodeType === Node.ELEMENT_NODE || node.nodeType === Node.TEXT_NODE) apply(node);
    }
  }).observe(document.documentElement, { subtree: true, childList: true, characterData: true });
})();
