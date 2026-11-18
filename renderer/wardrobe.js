const wardrobeGroups = {
  skin: ['Skins', 'Skiny'],
  cape: ['Capes', 'Peleryny'],
  accessory: ['Accessories', 'Dodatki'],
};
let wardrobe;
let wardrobeBusy = false;
const wardrobeCopy = (en, pl) => state.settings.language === 'pl' ? pl : en;

function viewWardrobe() {
  if (!state.account) return `<div class="page"><h1>${wardrobeCopy('Cosmetics', 'Kosmetyki')}</h1>
    <section class="card"><p>${wardrobeCopy('Sign in or select an offline name to save your look.', 'Zaloguj się lub wybierz nick offline, aby zapisać wygląd.')}</p>
    <button class="btn primary" id="wardrobe-account">${wardrobeCopy('Account', 'Konto')}</button></section></div>`;

  return `<div class="page wardrobe"><h1>${wardrobeCopy('Cosmetics', 'Kosmetyki')}</h1>
    <p class="sub">${wardrobeCopy('Free looks for the Quartz mod. Accessories use a Quartz skin.', 'Darmowy wygląd w modzie Quartz. Dodatki wymagają skina Quartz.')}</p>
    <div id="wardrobe-content">${wardrobe ? wardrobeMarkup() : wardrobeCopy('Loading…', 'Wczytywanie…')}</div></div>`;
}

function wardrobeMarkup() {
  return Object.entries(wardrobeGroups).map(([group, titles]) => {
    const items = wardrobe.catalog[group].map((item, index) => {
      const selected = wardrobe.equipped[group] === item.id;
      const name = wardrobeCopy(item.en, item.pl);
      const image = index === 0 ? icon('user', 30) : `<img src="cosmetics/${group}-${item.id}.png" alt="">`;
      return `<button class="wardrobe-item${selected ? ' equipped' : ''}" data-group="${group}" data-item="${item.id}"
        aria-pressed="${selected}" ${wardrobeBusy ? 'disabled' : ''}>${image}<strong>${esc(name)}</strong>
        <small>${selected ? wardrobeCopy('Equipped', 'Założono') : wardrobeCopy('Equip', 'Załóż')}</small></button>`;
    }).join('');
    return `<section class="card"><h2>${wardrobeCopy(...titles)}</h2><div class="wardrobe-grid">${items}</div></section>`;
  }).join('');
}

function bindWardrobe() {
  $('#wardrobe-account')?.addEventListener('click', () => go('account'));
  const panel = $('#wardrobe-content');
  if (!panel) return;
  const accountId = state.account.uuid;
  const active = () => panel.isConnected && state.account?.uuid === accountId;
  const paint = () => { if (active() && wardrobe) panel.innerHTML = wardrobeMarkup(); };
  wardrobe = null;
  panel.textContent = wardrobeCopy('Loading…', 'Wczytywanie…');
  api('cosmetics:get').then((data) => {
    if (active()) { wardrobe = data; paint(); }
  }).catch((error) => { if (active()) panel.textContent = error.message; });
  panel.addEventListener('click', async (event) => {
    const button = event.target.closest('[data-item]');
    if (!button || !wardrobe || wardrobeBusy) return;
    wardrobeBusy = true;
    paint();
    try {
      const patch = { [button.dataset.group]: button.dataset.item };
      if (patch.skin === 'account') patch.accessory = 'none';
      wardrobe.equipped = await api('cosmetics:set', patch);
    } catch (error) {
      if (active()) toast(error.message, true);
    } finally {
      wardrobeBusy = false;
      paint();
    }
  });
}
