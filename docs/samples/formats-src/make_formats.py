"""Builds docs/samples/formats: one small file for every format the file manager opens.

Usage: python3 make_formats.py <output folder> <word-demo.docx>
Needs ffmpeg, LibreOffice (soffice), Pillow with pillow-heif, openpyxl and python-pptx.
"""
import io
import json
import math
import shutil
import struct
import subprocess
import sys
import tempfile
import wave
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

OUT = Path(sys.argv[1])
DEMO_DOCX = Path(sys.argv[2])
FONT = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
FONT_BOLD = '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
WORK = Path(tempfile.mkdtemp(prefix='formats-'))

RUSSIAN = ('Мониторинг устройств: проверка кодировки.\n'
           'Съешь же ещё этих мягких французских булок, да выпей чаю.\n'
           'Температура 45 °C, напряжение 220 В, ток 3,75 А.\n')


def run(*command):
    subprocess.run(command, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def soffice(source, extension, filter_name=None):
    target = f'{extension}:{filter_name}' if filter_name else extension
    run('soffice', '--headless', '--convert-to', target, '--outdir', str(WORK), str(source))
    return WORK / f'{source.stem}.{extension}'


# Text --------------------------------------------------------------------------------------------

def texts():
    (OUT / 'text-utf8.txt').write_text(RUSSIAN + 'English line: the quick brown fox jumps over the lazy dog.\n', encoding='utf-8')
    (OUT / 'text-cp1251.txt').write_bytes(RUSSIAN.encode('windows-1251'))
    lines = [f'2026-11-16 09:{minute:02d}:00 INFO  sensor-{minute % 4 + 1} temperature={40 + minute % 7} C' for minute in range(0, 60, 3)]
    lines.insert(7, '2026-11-16 09:21:30 WARN  sensor-2 voltage dropped to 198 V')
    lines.insert(12, '2026-11-16 09:36:10 ERROR sensor-4 no response for 30 s, reconnecting')
    (OUT / 'events.log').write_text('\n'.join(lines) + '\n', encoding='utf-8')
    device = {
        'id': 'MD-0042', 'name': 'Шкаф управления №3', 'online': True,
        'sensors': [
            {'type': 'temperature', 'value': 45.2, 'unit': 'C', 'limits': [5, 60]},
            {'type': 'voltage', 'value': 220.5, 'unit': 'V', 'limits': [198, 242]},
        ],
        'updatedAt': '2026-11-16T09:45:00+03:00',
    }
    (OUT / 'device.json').write_text(json.dumps(device, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    (OUT / 'config.xml').write_text(
        '<?xml version="1.0" encoding="UTF-8"?>\n'
        '<monitoring interval="30">\n'
        '  <!-- Пороги срабатывания -->\n'
        '  <sensor id="temperature" min="5" max="60" unit="C"/>\n'
        '  <sensor id="voltage" min="198" max="242" unit="V"/>\n'
        '  <notify email="duty@example.com" sms="false"/>\n'
        '</monitoring>\n', encoding='utf-8')
    (OUT / 'notes.md').write_text(
        '# Заметки по объекту\n\n'
        'Обычный абзац с **жирным**, *курсивом* и `кодом`. Ссылка: [пример](https://example.com).\n\n'
        '## Что проверить\n\n'
        '1. Датчики температуры\n2. Питание\n   - основное\n   - резервное\n3. Связь\n\n'
        '| Датчик | Значение | Норма |\n|---|---:|---|\n| Температура | 45 °C | 5–60 |\n| Напряжение | 220 В | 198–242 |\n\n'
        '> Цитата: проверять раз в смену.\n\n'
        '```\nstatus = read_sensor("temperature")\n```\n', encoding='utf-8')
    (OUT / 'readme.markdown').write_text('# Readme\n\nКороткий файл с расширением `.markdown`.\n\n- пункт один\n- пункт два\n', encoding='utf-8')


# Images ------------------------------------------------------------------------------------------

def card(label, size=(800, 600), transparent=False):
    width, height = size
    mode = 'RGBA' if transparent else 'RGB'
    image = Image.new(mode, size)
    pixels = image.load()
    for y in range(height):
        for x in range(width):
            r = int(40 + 60 * x / width)
            g = int(110 + 90 * y / height)
            b = int(200 - 60 * x / width)
            if transparent:
                inside = (x - width / 2) ** 2 / (width / 2) ** 2 + (y - height / 2) ** 2 / (height / 2) ** 2 <= 1
                pixels[x, y] = (r, g, b, 255 if inside else 0)
            else:
                pixels[x, y] = (r, g, b)
    draw = ImageDraw.Draw(image)
    draw.ellipse((width * 0.62, height * 0.12, width * 0.88, height * 0.12 + width * 0.26), fill=(255, 200, 60, 255))
    draw.rectangle((width * 0.1, height * 0.62, width * 0.5, height * 0.8), fill=(255, 255, 255, 220))
    big = ImageFont.truetype(FONT_BOLD, int(height / 7))
    small = ImageFont.truetype(FONT, int(height / 18))
    draw.text((width * 0.1, height * 0.2), label, font=big, fill=(255, 255, 255, 255))
    draw.text((width * 0.12, height * 0.66), f'{width} × {height}', font=small, fill=(30, 60, 110, 255))
    return image


def images():
    card('PNG').save(OUT / 'image.png')
    card('PNG · alpha', transparent=True).save(OUT / 'transparent.png')
    card('BMP', (400, 300)).save(OUT / 'image.bmp')
    card('WEBP').save(OUT / 'image.webp', quality=85)
    card('JPEG').save(OUT / 'photo.jpeg', quality=85)
    # Stored lying on its side with EXIF orientation 6: a viewer that applies it shows the text upright.
    rotated = card('JPG · EXIF', (600, 800)).rotate(90, expand=True)
    exif = Image.Exif()
    exif[0x0112] = 6
    rotated.save(OUT / 'photo-exif-rotated.jpg', quality=85, exif=exif)

    import pillow_heif
    pillow_heif.register_heif_opener()
    card('HEIC').save(OUT / 'image.heic', quality=80)

    frames = []
    for step in range(12):
        frame = Image.new('RGB', (320, 200), (245, 247, 250))
        draw = ImageDraw.Draw(frame)
        angle = step / 12 * 2 * math.pi
        cx, cy = 160 + 90 * math.cos(angle), 100 + 50 * math.sin(angle)
        draw.ellipse((cx - 22, cy - 22, cx + 22, cy + 22), fill=(46, 160, 90))
        draw.text((12, 10), 'GIF / WEBP', font=ImageFont.truetype(FONT_BOLD, 22), fill=(40, 70, 120))
        frames.append(frame)
    frames[0].save(OUT / 'animation.gif', save_all=True, append_images=frames[1:], duration=80, loop=0)
    frames[0].save(OUT / 'animation.webp', save_all=True, append_images=frames[1:], duration=80, loop=0, quality=80)

    (OUT / 'diagram.svg').write_text(
        '<svg xmlns="http://www.w3.org/2000/svg" width="480" height="300" viewBox="0 0 480 300">\n'
        '  <rect width="480" height="300" fill="#f5f7fa"/>\n'
        '  <text x="24" y="40" font-family="sans-serif" font-size="22" font-weight="bold" fill="#2f5597">SVG: схема подключения</text>\n'
        '  <rect x="30" y="90" width="120" height="70" rx="10" fill="#4472c4"/>\n'
        '  <text x="90" y="131" font-family="sans-serif" font-size="16" fill="#fff" text-anchor="middle">Датчик</text>\n'
        '  <rect x="330" y="90" width="120" height="70" rx="10" fill="#ed7d31"/>\n'
        '  <text x="390" y="131" font-family="sans-serif" font-size="16" fill="#fff" text-anchor="middle">Сервер</text>\n'
        '  <line x1="150" y1="125" x2="322" y2="125" stroke="#555" stroke-width="3" marker-end="url(#arrow)"/>\n'
        '  <defs><marker id="arrow" markerWidth="10" markerHeight="10" refX="8" refY="5" orient="auto">'
        '<path d="M0,0 L10,5 L0,10 z" fill="#555"/></marker></defs>\n'
        '  <circle cx="240" cy="230" r="34" fill="#2e7d32"/>\n'
        '  <text x="240" y="236" font-family="sans-serif" font-size="14" fill="#fff" text-anchor="middle">OK</text>\n'
        '</svg>\n', encoding='utf-8')


# Audio and video ---------------------------------------------------------------------------------

def melody(path, seconds=8.0, rate=44100):
    notes = [0, 4, 7, 12, 7, 4, 0, 5, 9, 12, 9, 5, 0, 4, 7, 0]
    beat = seconds / len(notes)
    samples = []
    for index in range(int(seconds * rate)):
        t = index / rate
        note = notes[min(int(t / beat), len(notes) - 1)]
        local = t % beat
        frequency = 261.63 * 2 ** (note / 12)
        envelope = min(1.0, local / 0.02) * math.exp(-3 * local)
        value = envelope * (0.6 * math.sin(2 * math.pi * frequency * t) + 0.25 * math.sin(4 * math.pi * frequency * t))
        samples.append(int(max(-1, min(1, value)) * 26000))
    with wave.open(str(path), 'wb') as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(rate)
        out.writeframes(struct.pack(f'<{len(samples)}h', *samples))


def audio():
    source = WORK / 'melody.wav'
    melody(source)
    cover = WORK / 'cover.png'
    card('♪ Cover', (500, 500)).save(cover)
    tags = ['-metadata', 'title=Тестовая мелодия', '-metadata', 'artist=MonitoringDevices', '-metadata', 'album=Образцы форматов']
    shutil.copy(source, OUT / 'audio.wav')
    with_cover = ['-i', str(cover), '-map', '0:a', '-map', '1:v', '-disposition:v', 'attached_pic']
    run('ffmpeg', '-y', '-i', str(source), *with_cover, '-c:a', 'libmp3lame', '-b:a', '96k', '-c:v', 'mjpeg', *tags, '-id3v2_version', '3', str(OUT / 'audio.mp3'))
    run('ffmpeg', '-y', '-i', str(source), *with_cover, '-c:a', 'aac', '-b:a', '96k', '-c:v', 'mjpeg', *tags, str(OUT / 'audio.m4a'))
    run('ffmpeg', '-y', '-i', str(source), *with_cover, '-c:a', 'flac', '-c:v', 'png', *tags, str(OUT / 'audio.flac'))
    run('ffmpeg', '-y', '-i', str(source), '-c:a', 'aac', '-b:a', '96k', '-f', 'adts', str(OUT / 'audio.aac'))
    run('ffmpeg', '-y', '-i', str(source), '-c:a', 'libvorbis', '-q:a', '3', *tags, str(OUT / 'audio.ogg'))
    run('ffmpeg', '-y', '-i', str(source), '-c:a', 'libvorbis', '-q:a', '3', *tags, '-f', 'ogg', str(OUT / 'audio.oga'))
    run('ffmpeg', '-y', '-i', str(source), '-c:a', 'libopus', '-b:a', '48k', *tags, str(OUT / 'audio.opus'))
    return source


def video(sound):
    def clip(name, label, size, codecs, extra=()):
        text = f"drawtext=fontfile={FONT_BOLD}:text='{label}':fontsize=h/9:fontcolor=white:box=1:boxcolor=black@0.5:x=(w-tw)/2:y=h-th-h/10"
        run('ffmpeg', '-y', '-f', 'lavfi', '-i', f'testsrc2=size={size}:rate=25:duration=6', '-i', str(sound),
            '-vf', text, '-t', '6', '-shortest', *codecs, *extra, str(OUT / name))
    h264 = ['-c:v', 'libx264', '-pix_fmt', 'yuv420p', '-crf', '30', '-preset', 'veryfast']
    clip('video.mp4', 'MP4 · H.264', '480x270', h264 + ['-c:a', 'aac', '-b:a', '64k'], ['-movflags', '+faststart'])
    clip('video.m4v', 'M4V · H.264', '480x270', h264 + ['-c:a', 'aac', '-b:a', '64k'], ['-f', 'mp4'])
    clip('video.mov', 'MOV · H.264', '480x270', h264 + ['-c:a', 'aac', '-b:a', '64k'])
    clip('video.3gp', '3GP · H.263', '352x288', ['-c:v', 'h263', '-q:v', '20', '-c:a', 'aac', '-b:a', '32k', '-ar', '22050'])
    clip('video.webm', 'WEBM · VP9', '480x270', ['-c:v', 'libvpx-vp9', '-crf', '40', '-b:v', '0', '-c:a', 'libopus', '-b:a', '48k'])
    clip('video.mkv', 'MKV · H.264', '480x270', h264 + ['-c:a', 'libvorbis', '-q:a', '3'])
    # An MP4 with no video track: the viewer should open it as audio.
    run('ffmpeg', '-y', '-i', str(sound), '-c:a', 'aac', '-b:a', '64k', '-metadata', 'title=MP4 без видео', str(OUT / 'sound-only.mp4'))


# Office ------------------------------------------------------------------------------------------

def documents():
    shutil.copy(DEMO_DOCX, WORK / 'document.docx')
    shutil.copy(DEMO_DOCX, OUT / 'document.docx')
    for extension, filter_name in [('doc', 'MS Word 97'), ('odt', None), ('rtf', None), ('pdf', None)]:
        shutil.copy(soffice(WORK / 'document.docx', extension, filter_name), OUT / f'document.{extension}')


def spreadsheet():
    from openpyxl import Workbook
    from openpyxl.styles import Alignment, Font, PatternFill

    book = Workbook()
    readings = book.active
    readings.title = 'Показания'
    readings.append(['Дата', 'Датчик', 'Температура, °C', 'Напряжение, В'])
    for row, (day, sensor, temperature, voltage) in enumerate([
        ('2026-11-10', 'Шкаф 1', 41.5, 221), ('2026-11-11', 'Шкаф 1', 43.0, 219), ('2026-11-12', 'Шкаф 2', 47.8, 224),
        ('2026-11-13', 'Шкаф 2', 52.1, 205), ('2026-11-14', 'Шкаф 3', 39.9, 230)], start=2):
        readings.append([day, sensor, temperature, voltage])
    readings.append(['Среднее', '', '=AVERAGE(C2:C6)', '=AVERAGE(D2:D6)'])
    readings.append(['Максимум', '', '=MAX(C2:C6)', '=MAX(D2:D6)'])
    for cell in readings[1]:
        cell.font = Font(bold=True, color='FFFFFF')
        cell.fill = PatternFill('solid', fgColor='4472C4')
        cell.alignment = Alignment(horizontal='center')
    readings['C5'].fill = PatternFill('solid', fgColor='F8CBAD')
    for column, width in zip('ABCD', (14, 12, 18, 16)):
        readings.column_dimensions[column].width = width

    devices = book.create_sheet('Устройства')
    devices.merge_cells('A1:C1')
    devices['A1'] = 'Список устройств'
    devices['A1'].font = Font(bold=True, size=14)
    devices.append(['Идентификатор', 'Название', 'Статус'])
    for row in [('MD-0040', 'Шкаф управления №1', 'в работе'), ('MD-0041', 'Шкаф управления №2', 'в работе'), ('MD-0042', 'Шкаф управления №3', 'авария')]:
        devices.append(row)
    devices['C5'].font = Font(color='C00000', bold=True)
    for column, width in zip('ABC', (16, 26, 12)):
        devices.column_dimensions[column].width = width

    raw = WORK / 'spreadsheet.xlsx'
    book.save(raw)
    # openpyxl stores formulas without values, and viewers show the stored values. LibreOffice calculates
    # them when it reads the file as ODS, not as XLSX, hence the round trip through ODS.
    calculated_ods = soffice(raw, 'ods')
    calculated = WORK / 'calculated' / 'spreadsheet.xlsx'
    calculated.parent.mkdir()
    run('soffice', '--headless', '--convert-to', 'xlsx:Calc MS Excel 2007 XML', '--outdir', str(calculated.parent), str(calculated_ods))
    shutil.copy(calculated, OUT / 'spreadsheet.xlsx')
    for extension, filter_name in [('xls', 'MS Excel 97'), ('ods', None), ('csv', 'Text - txt - csv (StarCalc):44,34,76,1')]:
        shutil.copy(soffice(calculated, extension, filter_name), OUT / f'spreadsheet.{extension}')


def presentation():
    from pptx import Presentation
    from pptx.chart.data import CategoryChartData
    from pptx.enum.chart import XL_CHART_TYPE, XL_LEGEND_POSITION
    from pptx.util import Inches, Pt

    deck = Presentation()
    deck.slide_width, deck.slide_height = Inches(13.333), Inches(7.5)
    title = deck.slides.add_slide(deck.slide_layouts[0])
    title.shapes.title.text = 'Мониторинг устройств'
    title.placeholders[1].text = 'Презентация для проверки просмотра: текст, таблица, картинка, диаграмма'

    bullets = deck.slides.add_slide(deck.slide_layouts[1])
    bullets.shapes.title.text = 'Что умеет система'
    body = bullets.placeholders[1].text_frame
    body.text = 'Собирает показания датчиков'
    for text, level in [('Температура и напряжение', 1), ('Состояние связи', 1), ('Сравнивает с порогами', 0), ('Сообщает дежурному', 0)]:
        paragraph = body.add_paragraph()
        paragraph.text, paragraph.level = text, level

    table_slide = deck.slides.add_slide(deck.slide_layouts[5])
    table_slide.shapes.title.text = 'Показания и схема'
    rows = [('Датчик', 'Значение'), ('Температура', '45 °C'), ('Напряжение', '220 В'), ('Ток', '3,75 А')]
    table = table_slide.shapes.add_table(len(rows), 2, Inches(0.7), Inches(1.8), Inches(5.5), Inches(2.4)).table
    for r, row in enumerate(rows):
        for c, value in enumerate(row):
            table.cell(r, c).text = value
            table.cell(r, c).text_frame.paragraphs[0].runs[0].font.size = Pt(20)
    picture = io.BytesIO()
    card('PPTX', (640, 400)).save(picture, format='PNG')
    picture.seek(0)
    table_slide.shapes.add_picture(picture, Inches(7), Inches(1.8), width=Inches(5.5))

    chart_slide = deck.slides.add_slide(deck.slide_layouts[5])
    chart_slide.shapes.title.text = 'Срабатывания по кварталам'
    data = CategoryChartData()
    data.categories = ['I кв.', 'II кв.', 'III кв.', 'IV кв.']
    data.add_series('Температура', (12, 18, 9, 15))
    data.add_series('Напряжение', (7, 5, 11, 8))
    chart = chart_slide.shapes.add_chart(XL_CHART_TYPE.COLUMN_CLUSTERED, Inches(1.5), Inches(1.6), Inches(10), Inches(5.4), data).chart
    chart.has_legend = True
    chart.legend.position = XL_LEGEND_POSITION.BOTTOM
    chart.legend.include_in_layout = False

    source = WORK / 'presentation.pptx'
    deck.save(source)
    shutil.copy(source, OUT / 'presentation.pptx')
    for extension, filter_name in [('ppt', 'MS PowerPoint 97'), ('odp', None)]:
        shutil.copy(soffice(source, extension, filter_name), OUT / f'presentation.{extension}')


def drawing():
    """Native Draw shapes, not a picture: LibreOffice would embed an SVG as an image."""
    import zipfile
    def box(x, y, width, height, style, text):
        return (f'<draw:rect draw:style-name="{style}" svg:x="{x}cm" svg:y="{y}cm" svg:width="{width}cm" svg:height="{height}cm">'
                f'<text:p text:style-name="label">{text}</text:p></draw:rect>')
    shapes = (
        '<draw:frame svg:x="1cm" svg:y="0.6cm" svg:width="14cm" svg:height="1.2cm"><draw:text-box>'
        '<text:p text:style-name="title">ODG: схема подключения</text:p></draw:text-box></draw:frame>'
        + box(1, 3, 4, 2, 'blue', 'Датчик') + box(11, 3, 4, 2, 'orange', 'Сервер')
        + '<draw:line draw:style-name="arrow" svg:x1="5cm" svg:y1="4cm" svg:x2="11cm" svg:y2="4cm"/>'
        + '<draw:ellipse draw:style-name="green" svg:x="7cm" svg:y="6.5cm" svg:width="2cm" svg:height="2cm"><text:p text:style-name="label">OK</text:p></draw:ellipse>'
    )
    content = (
        '<?xml version="1.0" encoding="UTF-8"?>'
        '<office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" '
        'xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0" xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0" '
        'xmlns:draw="urn:oasis:names:tc:opendocument:xmlns:drawing:1.0" xmlns:svg="urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0" '
        'xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0" office:version="1.2">'
        '<office:automatic-styles>'
        + ''.join(f'<style:style style:name="{name}" style:family="graphic"><style:graphic-properties draw:fill="solid" draw:fill-color="{color}" draw:stroke="none"/></style:style>'
                  for name, color in [('blue', '#4472c4'), ('orange', '#ed7d31'), ('green', '#2e7d32')])
        + '<style:style style:name="arrow" style:family="graphic"><style:graphic-properties draw:stroke="solid" svg:stroke-color="#555555" svg:stroke-width="0.08cm" draw:marker-end="Arrow" draw:marker-end-width="0.4cm"/></style:style>'
        '<style:style style:name="label" style:family="paragraph"><style:paragraph-properties fo:text-align="center"/><style:text-properties fo:color="#ffffff" fo:font-size="16pt"/></style:style>'
        '<style:style style:name="title" style:family="paragraph"><style:text-properties fo:color="#2f5597" fo:font-size="20pt" fo:font-weight="bold"/></style:style>'
        '</office:automatic-styles><office:body><office:drawing><draw:page draw:name="page1">'
        + shapes + '</draw:page></office:drawing></office:body></office:document-content>'
    )
    manifest = ('<?xml version="1.0" encoding="UTF-8"?><manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0" manifest:version="1.2">'
                '<manifest:file-entry manifest:full-path="/" manifest:media-type="application/vnd.oasis.opendocument.graphics"/>'
                '<manifest:file-entry manifest:full-path="content.xml" manifest:media-type="text/xml"/><manifest:file-entry manifest:full-path="styles.xml" manifest:media-type="text/xml"/></manifest:manifest>')
    styles = ('<?xml version="1.0" encoding="UTF-8"?><office:document-styles xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" '
              'xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0" xmlns:draw="urn:oasis:names:tc:opendocument:xmlns:drawing:1.0" '
              'xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0" office:version="1.2">'
              '<office:automatic-styles><style:page-layout style:name="sheet"><style:page-layout-properties fo:page-width="16cm" '
              'fo:page-height="10cm" fo:margin-top="0cm" fo:margin-bottom="0cm" fo:margin-left="0cm" fo:margin-right="0cm"/></style:page-layout>'
              '<style:style style:name="white" style:family="drawing-page"><style:drawing-page-properties draw:fill="solid" draw:fill-color="#ffffff"/></style:style>'
              '</office:automatic-styles><office:master-styles><style:master-page style:name="Default" style:page-layout-name="sheet" '
              'draw:style-name="white"/></office:master-styles></office:document-styles>')
    content = content.replace('<draw:page draw:name="page1">', '<draw:page draw:name="page1" draw:master-page-name="Default">')
    with zipfile.ZipFile(OUT / 'drawing.odg', 'w', zipfile.ZIP_DEFLATED) as odg:
        odg.writestr(zipfile.ZipInfo('mimetype'), 'application/vnd.oasis.opendocument.graphics')
        odg.writestr('content.xml', content)
        odg.writestr('styles.xml', styles)
        odg.writestr('META-INF/manifest.xml', manifest)


OUT.mkdir(parents=True, exist_ok=True)
texts()
images()
sound = audio()
video(sound)
documents()
spreadsheet()
presentation()
drawing()
shutil.rmtree(WORK)
