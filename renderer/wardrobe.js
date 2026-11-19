const wardrobeTabs = {
  skins: ['Skins', 'Skiny'], capes: ['Capes', 'Peleryny'], models: ['3D cosmetics', 'Kosmetyki 3D'],
};
let wardrobeTab = 'skins';
const wardrobeCopy = (en, pl) => state.settings.language === 'pl' ? pl : en;

function viewWardrobe() {
  if (!state.account) return `<div class="page"><h1>${wardrobeCopy('Wardrobe', 'Garderoba')}</h1>
    <section class="card"><p>${wardrobeCopy('Sign in or choose an offline name.', 'Zaloguj się lub wybierz nick offline.')}</p>
    <button class="btn primary" id="wardrobe-account">${wardrobeCopy('Account', 'Konto')}</button></section></div>`;
  return `<div class="page wardrobe"><h1>${wardrobeCopy('Wardrobe', 'Garderoba')}</h1>
    <div id="wardrobe-tabs" class="wardrobe-tabs" aria-label="${wardrobeCopy('Wardrobe categories', 'Kategorie garderoby')}"></div>
    <div id="wardrobe-content" class="card wardrobe-content" aria-live="polite"></div></div>`;
}

function bindWardrobe() {
  $('#wardrobe-account')?.addEventListener('click', () => go('account'));
  const panel = $('#wardrobe-content');
  const tabs = $('#wardrobe-tabs');
  if (!panel || !tabs) return;
  const account = state.account;
  const active = () => panel.isConnected && state.account?.uuid === account.uuid;
  let skins, capes, busy = false, request = 0;

  function paintTabs() {
    tabs.innerHTML = Object.entries(wardrobeTabs).map(([key, title]) =>
      `<button class="btn" data-wardrobe-tab="${key}" aria-pressed="${key === wardrobeTab}">${wardrobeCopy(...title)}</button>`).join('');
  }

  function paint() {
    if (!active()) return;
    paintTabs();
    if (wardrobeTab === 'models') {
      panel.innerHTML = `<div class="wardrobe-empty">${wardrobeCopy('Your 3D cosmetics will appear here.', 'Tu pojawią się Twoje kosmetyki 3D.')}</div>`;
      return;
    }
    if (wardrobeTab === 'capes') {
      if (account.type !== 'microsoft') {
        panel.textContent = wardrobeCopy('Sign in with Microsoft to view your Minecraft capes.', 'Zaloguj się przez Microsoft, aby zobaczyć swoje peleryny Minecraft.');
        return;
      }
      if (!capes) { panel.textContent = wardrobeCopy('Loading capes…', 'Wczytywanie peleryn…'); return; }
      panel.innerHTML = capes.length ? `<div class="wardrobe-grid">${capes.map(cape =>
        `<article class="wardrobe-item${cape.active ? ' equipped' : ''}">
        ${cape.url ? `<img src="${esc(cape.url)}" alt="">` : icon('sparkle', 28)}
        <strong>${esc(cape.name)}</strong>${cape.active ? `<small>${wardrobeCopy('Active on account', 'Aktywna na koncie')}</small>` : ''}</article>`
      ).join('')}</div>` : `<p class="wardrobe-empty">${wardrobeCopy('Your Minecraft account has no capes.', 'Twoje konto Minecraft nie ma peleryn.')}</p>`;
      return;
    }
    if (!skins) { panel.textContent = wardrobeCopy('Loading skins…', 'Wczytywanie skinów…'); return; }
    const disabled = busy ? 'disabled' : '';
    panel.innerHTML = `<div class="wardrobe-tools">
      <button class="btn primary" data-wardrobe-action="add" ${disabled}>${wardrobeCopy('Add skin', 'Dodaj skina')}</button>
      <label>${wardrobeCopy('Model for new skin', 'Model nowego skina')}
        <select id="wardrobe-model"><option value="wide">Classic</option><option value="slim">Slim</option></select>
      </label>
      ${skins.equipped.skin !== 'account' ? `<button class="btn" data-wardrobe-action="reset" ${disabled}>${wardrobeCopy('Use account skin', 'Użyj skina konta')}</button>` : ''}
    </div>
    ${skins.skins.length ? `<div class="wardrobe-grid">${skins.skins.map(skin => {
      const on = skin.id === skins.equipped.skin;
      return `<article class="wardrobe-item${on ? ' equipped' : ''}">
        ${skin.preview ? `<img src="${esc(skin.preview)}" alt="">` : icon('user', 28)}
        <strong>${esc(skin.name)}</strong><small>${skin.slim ? 'Slim' : 'Classic'}</small>
        <div class="wardrobe-item-actions">
          <button class="btn" data-wardrobe-action="equip" data-skin="${esc(skin.id)}" ${busy || on ? 'disabled' : ''}>${on ? wardrobeCopy('Equipped', 'Założony') : wardrobeCopy('Equip', 'Załóż')}</button>
          <button class="btn danger" data-wardrobe-action="remove" data-skin="${esc(skin.id)}" ${disabled}>${wardrobeCopy('Remove', 'Usuń')}</button>
        </div>
      </article>`;
    }).join('')}</div>` : `<p class="wardrobe-empty">${wardrobeCopy('Add a 64 × 64 PNG to start your skin collection.', 'Dodaj PNG 64 × 64, aby zacząć kolekcję skinów.')}</p>`}`;
  }

  async function refresh() {
    const id = ++request;
    const tab = wardrobeTab;
    paint();
    try {
      if (tab === 'skins') {
        const response = await api('cosmetics:get');
        if (id === request) skins = response;
      } else if (tab === 'capes' && account.type === 'microsoft' && !capes) {
        const response = await api('cosmetics:capes');
        if (id === request) capes = response;
      }
      if (id === request) paint();
    } catch (error) { if (id === request && active()) panel.textContent = error.message; }
  }

  tabs.addEventListener('click', event => {
    const next = event.target.closest('[data-wardrobe-tab]')?.dataset.wardrobeTab;
    if (next && next !== wardrobeTab) { wardrobeTab = next; refresh(); }
  });
  panel.addEventListener('click', async event => {
    const button = event.target.closest('[data-wardrobe-action]');
    if (!button || busy || !active()) return;
    const { wardrobeAction, skin } = button.dataset;
    const slim = $('#wardrobe-model', panel)?.value === 'slim';
    busy = true;
    paint();
    try {
      if (wardrobeAction === 'add') await api('cosmetics:add-skin', slim);
      if (wardrobeAction === 'equip') await api('cosmetics:equip-skin', skin);
      if (wardrobeAction === 'reset') await api('cosmetics:equip-skin', 'account');
      if (wardrobeAction === 'remove') await api('cosmetics:remove-skin', skin);
    } catch (error) { if (active()) toast(error.message, true); }
    finally { busy = false; if (active()) refresh(); }
  });

  refresh();
}
