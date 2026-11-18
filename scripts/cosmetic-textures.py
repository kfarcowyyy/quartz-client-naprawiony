"""Rebuild the original 64x64 Minecraft skins, capes, and wardrobe thumbnails."""
from pathlib import Path
from struct import pack
from zlib import compress, crc32

ROOT = Path(__file__).resolve().parents[1]
PREVIEWS = ROOT / 'renderer/cosmetics'
TEXTURES = ROOT / 'quartz-mod/src/main/resources/assets/quartz/textures/wardrobe'
SKINS = {
    'amethyst': ('#4c315d', '#a56de4', '#daafff', '#d6a587', '#38243b'),
    'aurora': ('#163d4d', '#32bdac', '#a6ffdf', '#936445', '#102d39'),
    'ember': ('#4e2929', '#e67142', '#ffd28a', '#d39271', '#2c1b25'),
}
CAPES = {
    'crystal': ('#28204e', '#bcb1ff', '#fff1ff'),
    'night': ('#11192e', '#81b9ee', '#e2f5ff'),
    'sunrise': ('#943e61', '#ff8358', '#ffdc91'),
}


class Canvas:
    def __init__(self, width=64, height=64):
        self.width, self.height = width, height
        self.rows = [bytearray(width * 4) for _ in range(height)]

    def fill(self, x, y, width, height, color):
        rgba = bytes.fromhex(color.lstrip('#')) + b'\xff'
        for row in self.rows[y:y + height]:
            row[x * 4:(x + width) * 4] = rgba * width

    def copy(self, source, bounds, position):
        x, y, width, height = bounds
        dx, dy = position
        for row in range(height):
            self.rows[dy + row][dx * 4:(dx + width) * 4] = source.rows[y + row][x * 4:(x + width) * 4]

    def save(self, path):
        def chunk(kind, body):
            return pack('>I', len(body)) + kind + body + pack('>I', crc32(kind + body))
        header = pack('>IIBBBBB', self.width, self.height, 8, 6, 0, 0, 0)
        pixels = b''.join(b'\0' + row for row in self.rows)
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', header) + chunk(b'IDAT', compress(pixels)) + chunk(b'IEND', b''))


def gem(canvas, x, y, highlight, edge):
    for row, width in enumerate([1, 3, 5, 5, 3, 1]):
        canvas.fill(x - width // 2, y + row, width, 1, edge)
        canvas.fill(x, y + row, 1, 1, highlight)


def skin(colors, accessory):
    shadow, cloth, highlight, face, hair = colors
    c = Canvas()
    c.fill(0, 0, 32, 16, hair)
    c.fill(8, 9, 8, 7, face)
    c.fill(9, 12, 2, 1, '#f5f1ff')
    c.fill(13, 12, 2, 1, '#f5f1ff')
    c.fill(10, 12, 1, 1, shadow)
    c.fill(13, 12, 1, 1, shadow)
    c.fill(11, 14, 2, 1, hair)
    for x, y, w in [(0, 16, 16), (16, 16, 24), (40, 16, 16), (16, 48, 16), (32, 48, 16)]:
        c.fill(x, y, w, 16, shadow)
        c.fill(x + 4, y + 4, 8 if w == 24 else 4, 10, cloth)
        c.fill(x + 4, y + 5, 8 if w == 24 else 4, 1, highlight)
        if x in (32, 40):
            c.fill(x, y + 13, w, 3, face)
    gem(c, 23, 24, '#ffffff', highlight)
    if accessory == 'crown':
        c.fill(32, 10, 32, 2, '#ab8fea')
        c.fill(40, 8, 8, 1, '#e8dfff')
        c.fill(40, 9, 8, 1, '#c5b4ff')
        c.fill(43, 8, 2, 1, '#ffffff')
    if accessory == 'visor':
        c.fill(39, 10, 10, 3, '#342755')
        c.fill(40, 11, 8, 1, '#94f9ff')
        c.fill(41, 11, 2, 1, '#f8ffff')
    return c


def cape(colors):
    base, edge, highlight = colors
    c = Canvas(64, 32)
    c.fill(0, 0, 22, 17, base)
    for x in (1, 12):
        c.fill(x, 1, 10, 1, edge)
        c.fill(x, 16, 10, 1, edge)
        c.fill(x, 1, 1, 16, edge)
        c.fill(x + 9, 1, 1, 16, edge)
        gem(c, x + 4, 5, highlight, edge)
    return c


if __name__ == '__main__':
    crystal = Canvas(16, 24)
    for y in range(1, 23):
        edge = max(1, 7 - min(y, 23 - y) * 2)
        crystal.fill(edge, y, 16 - edge * 2, 1, '#9e8ed5')
        if edge < 5:
            crystal.fill(5, y, 6, 1, '#ece6fc')
        if edge < 4:
            crystal.fill(3, y, 2, 1, '#c5b7ed')
    crystal.fill(7, 7, 4, 10, '#fbf8ff')
    crystal.save(TEXTURES / 'quartz-crystal.png')
    for name, colors in SKINS.items():
        for accessory in ('none', 'crown', 'visor'):
            texture = skin(colors, accessory)
            texture.save(TEXTURES / f'skin-{name}-{accessory}.png')
            if accessory == 'none':
                preview = Canvas(10, 10)
                preview.copy(texture, (8, 8, 8, 8), (1, 1))
                preview.save(PREVIEWS / f'skin-{name}.png')
    for name, colors in CAPES.items():
        texture = cape(colors)
        texture.save(TEXTURES / f'cape-{name}.png')
        preview = Canvas(16, 16)
        preview.copy(texture, (1, 1, 10, 16), (3, 0))
        preview.save(PREVIEWS / f'cape-{name}.png')
    for accessory in ('crown', 'visor'):
        preview = Canvas(10, 10)
        preview.copy(skin(SKINS['amethyst'], accessory), (39, 8, 10, 8), (0, 1))
        preview.save(PREVIEWS / f'accessory-{accessory}.png')
