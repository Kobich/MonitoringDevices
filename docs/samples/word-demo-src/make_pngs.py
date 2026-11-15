"""Writes the pictures of the demo: banner.png, square.png, icon.png.

Usage: python3 make_pngs.py <folder>
"""
import sys, zlib, struct, math
out = sys.argv[1]
def png(path, w, h, pixel):
    raw = b''.join(b'\x00' + b''.join(bytes(pixel(x, y)) for x in range(w)) for y in range(h))
    chunk = lambda t, d: struct.pack('>I', len(d)) + t + d + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    open(path, 'wb').write(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 2, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))
# wide banner: blue -> teal gradient with soft waves
png(f'{out}/banner.png', 900, 260, lambda x, y: (int(30 + 20 * x / 900), int(90 + 110 * x / 900 + 20 * math.sin(y / 20 + x / 60)), int(200 - 40 * x / 900)))
# square: warm radial gradient
png(f'{out}/square.png', 320, 320, lambda x, y: (lambda d: (255, int(140 + 100 * (1 - d)), int(60 + 120 * d)))(min(1.0, math.hypot(x - 160, y - 160) / 226)))
# small icon: green circle on light background
png(f'{out}/icon.png', 128, 128, lambda x, y: (46, 160, 90) if math.hypot(x - 64, y - 64) < 52 else (235, 245, 238))
