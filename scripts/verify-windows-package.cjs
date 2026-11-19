const fs = require('node:fs');
const path = require('node:path');

const archivePath = process.argv[2] || path.join('dist', 'win-unpacked', 'resources', 'app.asar');
const read = fs.openSync(archivePath, 'r');
const missing = new Set();

try {
  const prefix = Buffer.alloc(16);
  if (fs.readSync(read, prefix, 0, prefix.length, 0) !== prefix.length) {
    throw new Error('Truncated app.asar header.');
  }
  const headerSize = prefix.readUInt32LE(4);
  const jsonSize = prefix.readUInt32LE(12);
  const payloadStart = 8 + headerSize;
  const fileSize = fs.fstatSync(read).size;
  if (jsonSize > headerSize - 8 || payloadStart > fileSize) {
    throw new Error('Invalid app.asar header size.');
  }
  const header = Buffer.alloc(jsonSize);
  if (fs.readSync(read, header, 0, jsonSize, 16) !== jsonSize) {
    throw new Error('Could not read app.asar file table.');
  }
  const archive = JSON.parse(header.toString('utf8'));

  function entry(name) {
    return name.split('/').reduce((value, part) => value?.files?.[part], archive);
  }

  function fileEntry(name) {
    const file = entry(name);
    if (!Number.isSafeInteger(file?.size) || file.size <= 0) return null;
    if (file.unpacked) {
      asUnpacked(name, file);
    } else {
      const start = payloadStart + Number(file.offset);
      if (!Number.isSafeInteger(start) || start < payloadStart || start + file.size > fileSize) return null;
    }
    return file;
  }

  function asUnpacked(name, file) {
    const unpackedPath = path.join(archivePath + '.unpacked', ...name.split('/'));
    if (fs.statSync(unpackedPath, { throwIfNoEntry: false })?.size !== file.size) missing.add(name);
    return unpackedPath;
  }

  function readText(name, file) {
    if (file.unpacked) return fs.readFileSync(asUnpacked(name, file), 'utf8');
    const buffer = Buffer.alloc(file.size);
    fs.readSync(read, buffer, 0, buffer.length, payloadStart + Number(file.offset));
    return buffer.toString('utf8');
  }

  function checkFile(name) {
    const file = fileEntry(name);
    if (!file) missing.add(name);
    return file;
  }

  const checked = new Set();
  function checkModule(name) {
    if (checked.has(name)) return;
    checked.add(name);
    const file = checkFile(name);
    if (!file || !/\.[cm]?js$/.test(name)) return;

    const source = readText(name, file);
    for (const [, target] of source.matchAll(/\brequire\(\s*['"](\.[^'"]+)['"]\s*\)/g)) {
      const base = path.posix.join(path.posix.dirname(name), target);
      const resolved = [base, base + '.js', base + '.json', base + '/index.js'].find(fileEntry);
      if (resolved) checkModule(resolved);
      else missing.add(name + ' -> ' + target);
    }
  }

  const metadata = checkFile('package.json');
  if (metadata) checkModule(JSON.parse(readText('package.json', metadata)).main);
  checkModule('src/preload.js');

  for (const version of ['1.21.11', '26.3']) {
    for (const name of ['quartz-client.jar', 'fabric-api.jar']) {
      checkFile('quartz-mods/fabric/' + version + '/' + name);
    }
  }

  if (missing.size) {
    throw new Error('Missing files in the packaged app:\n' + [...missing].map(file => ' - ' + file).join('\n'));
  }
  console.log('Checked startup modules and both bundled Fabric versions in ' + archivePath);
} finally {
  fs.closeSync(read);
}
