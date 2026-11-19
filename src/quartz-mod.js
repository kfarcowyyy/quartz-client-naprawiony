const fs = require('node:fs');
const path = require('node:path');
const { randomUUID } = require('node:crypto');
const paths = require('./paths');

function install(instance) {
  if (!['fabric', 'quilt'].includes(instance.loader)) return false;
  if (!/^(?:1\.(?:\d+)(?:\.\d+)?|26\.\d+(?:\.\d+)?)$/.test(instance.mcVersion)) return false;
  const root = path.join(__dirname, '..', 'quartz-mods', instance.loader, instance.mcVersion);
  const sources = ['quartz-client.jar', 'fabric-api.jar'].map((name) => path.join(root, name));
  if (!sources.every((file) => fs.existsSync(file))) return false;
  const folder = path.join(paths.instDir(instance.id), 'mods');
  fs.mkdirSync(folder, { recursive: true });
  for (const [index, source] of sources.entries()) {
    const destination = path.join(folder, index === 0 ? 'quartz-client-managed.jar' : 'quartz-fabric-api-managed.jar');
    if (index === 1 && fs.readdirSync(folder).some((name) => /^fabric-api[-.].*\.jar$/i.test(name))) {
      fs.rmSync(destination, { force: true });
      continue;
    }
    const pending = path.join(folder, randomUUID() + '.tmp');
    try {
      fs.copyFileSync(source, pending);
      fs.renameSync(pending, destination);
    } finally {
      fs.rmSync(pending, { force: true });
    }
  }
  return true;
}

module.exports = { install };
