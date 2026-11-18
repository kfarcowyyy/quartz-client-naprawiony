const fs = require('node:fs');
const path = require('node:path');
const { randomUUID } = require('node:crypto');
const paths = require('./paths');
const catalog = require('../renderer/cosmetics/catalog.json');

function profileFile(account) {
  const id = String(account?.uuid || '').replace(/-/g, '').toLowerCase();
  return /^[a-f0-9]{32}$/.test(id) ? path.join(paths.base(), 'wardrobe', id + '.json') : null;
}

function normalize(value) {
  const result = {};
  for (const [category, items] of Object.entries(catalog)) {
    result[category] = items.some((item) => item.id === value?.[category]) ? value[category] : items[0].id;
  }
  if (result.skin === 'account' && result.accessory !== 'none') result.skin = 'amethyst';
  return result;
}

function read(account) {
  const file = profileFile(account);
  if (!file) return normalize(null);
  try {
    return normalize(JSON.parse(fs.readFileSync(file, 'utf8')));
  } catch (error) {
    if (error.code === 'ENOENT' || error instanceof SyntaxError) return normalize(null);
    throw error;
  }
}

function save(account, patch) {
  const file = profileFile(account);
  if (!file) throw new Error('Sign in to save your wardrobe.');
  const next = normalize({ ...read(account), ...patch });
  fs.mkdirSync(path.dirname(file), { recursive: true });
  const pending = file + '.' + randomUUID() + '.tmp';
  try {
    fs.writeFileSync(pending, JSON.stringify(next), { flag: 'wx' });
    fs.renameSync(pending, file);
  } finally {
    fs.rmSync(pending, { force: true });
  }
  return next;
}

module.exports = { profileFile, read, save, catalog };
