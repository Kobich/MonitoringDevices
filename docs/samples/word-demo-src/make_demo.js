// Builds the demo document without charts. Needs the docx package (npm install docx).
// Usage: node make_demo.js <folder with the pictures> base.docx
const fs = require('fs');
const path = require('path');
const {
  Document, Packer, Paragraph, TextRun, HeadingLevel, AlignmentType, LevelFormat, Table, TableRow, TableCell,
  WidthType, BorderStyle, ShadingType, VerticalAlign, ImageRun, TabStopType, Header, Footer, PageNumber,
  ExternalHyperlink, Tab, FootnoteReferenceRun, UnderlineType, HighlightColor, SectionType,
  LineRuleType, HorizontalPositionRelativeFrom, VerticalPositionRelativeFrom, HorizontalPositionAlign, TextWrappingType, TextWrappingSide,
} = require('docx');

const [imagesDir, outFile] = process.argv.slice(2);
const image = (name) => fs.readFileSync(path.join(imagesDir, name));

const FONT = 'Calibri';
const PAGE_TEXT_WIDTH = 9638; // A4 (11906) minus 1134 margins on both sides
const LOREM = 'Мониторинг устройств собирает данные о состоянии оборудования, сравнивает их с заданными порогами ' +
  'и сообщает оператору, если показатель выходит за допустимые пределы. Абзац нарочно длинный: так видно, ' +
  'как переносятся строки и как выравнивается их правый и левый край.';

const p = (text, options = {}) => new Paragraph({ ...options, children: [new TextRun(text)] });
const runs = (children, options = {}) => new Paragraph({ ...options, children });
const check = (text) => new Paragraph({
  spacing: { before: 60, after: 160 },
  shading: { type: ShadingType.CLEAR, color: 'auto', fill: 'EEF3FA' },
  border: { left: { style: BorderStyle.SINGLE, size: 18, color: '4A78C2', space: 6 } },
  children: [new TextRun({ text: 'Что проверить: ', bold: true, color: '2F5597' }), new TextRun({ text, color: '2F5597' })],
});
const h1 = (text) => new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun(text)] });
const h2 = (text) => new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun(text)] });

const thin = { style: BorderStyle.SINGLE, size: 4, color: '8C8C8C' };
const borders = { top: thin, left: thin, bottom: thin, right: thin };
const cell = (text, width, options = {}) => new TableCell({
  borders,
  width: { size: width, type: WidthType.DXA },
  margins: { top: 80, bottom: 80, left: 120, right: 120 },
  verticalAlign: options.vertical,
  columnSpan: options.columnSpan,
  rowSpan: options.rowSpan,
  shading: options.fill ? { type: ShadingType.CLEAR, color: 'auto', fill: options.fill } : undefined,
  children: [new Paragraph({ alignment: options.align, children: [new TextRun({ text, bold: options.bold })] })],
});

const children = [];

// Title
children.push(
  new Paragraph({ heading: HeadingLevel.TITLE, alignment: AlignmentType.CENTER, children: [new TextRun('Демонстрация просмотра Word')] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 240 }, children: [new TextRun({ text: 'Документ для проверки и показа: шрифты, выделение, выравнивание, отступы, списки, таблицы, рисунки и диаграммы', italics: true, color: '595959' })] }),
  check('у каждого раздела такая плашка: она говорит, на что смотреть. Сравнивайте с тем, как документ выглядит в Word (кнопка «Открыть в другом приложении»).'),
);

// 1. Alignment
children.push(
  h1('1. Выравнивание абзацев'),
  check('строки каждого абзаца прижаты к своему краю; у «по ширине» оба края ровные, последняя строка — влево.'),
  h2('По левому краю'), p(LOREM, { alignment: AlignmentType.LEFT }),
  h2('По центру'), p(LOREM, { alignment: AlignmentType.CENTER }),
  h2('По правому краю'), p(LOREM, { alignment: AlignmentType.RIGHT }),
  h2('По ширине'), p(LOREM, { alignment: AlignmentType.JUSTIFIED }),
  h2('Короткие строки'),
  p('Слева', { alignment: AlignmentType.LEFT }),
  p('По центру', { alignment: AlignmentType.CENTER }),
  p('Справа', { alignment: AlignmentType.RIGHT }),
);

// 2. Tabs
children.push(
  h1('2. Табуляция'),
  check('столбцы встают по позициям табуляции: левой, по центру, правой и по десятичной точке; у правой — заполнитель точками.'),
  runs([new TextRun('Слева'), new TextRun({ children: [new Tab(), 'По центру'] }), new TextRun({ children: [new Tab(), 'Справа'] })], {
    tabStops: [{ type: TabStopType.CENTER, position: PAGE_TEXT_WIDTH / 2 }, { type: TabStopType.RIGHT, position: PAGE_TEXT_WIDTH }],
  }),
  ...[['Введение', '3'], ['Установка датчиков', '12'], ['Журнал событий', '27']].map(([title, page]) =>
    runs([new TextRun(title), new TextRun({ children: [new Tab(), page] })], { tabStops: [{ type: TabStopType.RIGHT, position: PAGE_TEXT_WIDTH, leader: 'dot' }] })),
  ...[['Напряжение', '220.5'], ['Ток', '3.75'], ['Мощность', '826.875']].map(([name, value]) =>
    runs([new TextRun(name), new TextRun({ children: [new Tab(), value] })], { tabStops: [{ type: TabStopType.DECIMAL, position: 4000 }] })),
);

// 3. Indents and spacing
children.push(
  h1('3. Отступы и интервалы'),
  check('красная строка, отступ слева и справа, выступ первой строки; разный межстрочный интервал и интервалы между абзацами.'),
  h2('Красная строка 1,25 см'), p(LOREM, { indent: { firstLine: 709 }, alignment: AlignmentType.JUSTIFIED }),
  h2('Отступ слева 2 см и справа 2 см'), p(LOREM, { indent: { left: 1134, right: 1134 } }),
  h2('Выступ 1 см'), p(LOREM, { indent: { left: 567, hanging: 567 } }),
  h2('Межстрочный 1,0 / 1,5 / 2,0'),
  p('Одинарный интервал. ' + LOREM, { spacing: { line: 240, lineRule: LineRuleType.AUTO } }),
  p('Полуторный интервал. ' + LOREM, { spacing: { line: 360, lineRule: LineRuleType.AUTO } }),
  p('Двойной интервал. ' + LOREM, { spacing: { line: 480, lineRule: LineRuleType.AUTO } }),
  h2('Интервал перед и после абзаца 18 пт'),
  p('Абзац с интервалом 18 пт перед ним и после него.', { spacing: { before: 360, after: 360 }, shading: { type: ShadingType.CLEAR, color: 'auto', fill: 'F2F2F2' } }),
  p('Следующий абзац без интервалов.', { spacing: { before: 0, after: 0 } }),
);

// 4. Fonts and sizes
children.push(
  h1('4. Шрифты и размеры'),
  check('у каждой строки свой шрифт и размер. Шрифтов Word на телефоне нет, их заменяют системные, — форма букв будет другой, размер должен совпасть.'),
  ...['Calibri', 'Arial', 'Times New Roman', 'Courier New', 'Georgia', 'Verdana'].map((font) =>
    runs([new TextRun({ text: `${font}: Съешь же ещё этих мягких французских булок. 0123456789`, font })])),
  ...[8, 10, 12, 14, 18, 24, 32].map((size) => runs([new TextRun({ text: `${size} пт — Мониторинг`, size: size * 2 })])),
);

// 5. Character formatting
children.push(
  h1('5. Выделение текста'),
  check('каждое слово оформлено так, как написано в нём самом.'),
  runs([
    new TextRun({ text: 'Жирный', bold: true }), new TextRun(', '),
    new TextRun({ text: 'курсив', italics: true }), new TextRun(', '),
    new TextRun({ text: 'жирный курсив', bold: true, italics: true }), new TextRun(', '),
    new TextRun({ text: 'подчёркнутый', underline: {} }), new TextRun(', '),
    new TextRun({ text: 'двойное подчёркивание', underline: { type: UnderlineType.DOUBLE } }), new TextRun(', '),
    new TextRun({ text: 'волнистое подчёркивание', underline: { type: UnderlineType.WAVE, color: 'C00000' } }), new TextRun(', '),
    new TextRun({ text: 'зачёркнутый', strike: true }), new TextRun(', '),
    new TextRun({ text: 'двойное зачёркивание', doubleStrike: true }), new TextRun('.'),
  ]),
  runs([
    new TextRun('Индексы: H'), new TextRun({ text: '2', subScript: true }), new TextRun('O, E = mc'), new TextRun({ text: '2', superScript: true }),
    new TextRun(', '), new TextRun({ text: 'малые прописные', smallCaps: true }), new TextRun(', '),
    new TextRun({ text: 'все прописные', allCaps: true }), new TextRun(', '),
    new TextRun({ text: 'разрежённый', characterSpacing: 60 }), new TextRun(', '),
    new TextRun({ text: 'уплотнённый', characterSpacing: -20 }), new TextRun('.'),
  ]),
  runs([
    new TextRun('Цвет: '), new TextRun({ text: 'красный', color: 'C00000' }), new TextRun(', '),
    new TextRun({ text: 'зелёный', color: '2E7D32' }), new TextRun(', '), new TextRun({ text: 'синий', color: '1F4E9C' }),
    new TextRun('. Выделение маркером: '), new TextRun({ text: 'жёлтым', highlight: HighlightColor.YELLOW }), new TextRun(', '),
    new TextRun({ text: 'голубым', highlight: HighlightColor.CYAN }), new TextRun('. Заливка: '),
    new TextRun({ text: 'серая заливка', shading: { type: ShadingType.CLEAR, color: 'auto', fill: 'D9D9D9' } }), new TextRun('.'),
  ]),
  runs([
    new TextRun('Ссылка: '),
    new ExternalHyperlink({ link: 'https://example.com', children: [new TextRun({ text: 'example.com', style: 'Hyperlink' })] }),
    new TextRun('. Сноска в конце предложения'), new FootnoteReferenceRun(1), new TextRun('.'),
  ]),
  h2('Абзац в рамке с заливкой'),
  new Paragraph({
    shading: { type: ShadingType.CLEAR, color: 'auto', fill: 'FFF4E5' },
    border: { top: { style: BorderStyle.SINGLE, size: 6, color: 'E69138', space: 4 }, left: { style: BorderStyle.SINGLE, size: 6, color: 'E69138', space: 4 },
      bottom: { style: BorderStyle.SINGLE, size: 6, color: 'E69138', space: 4 }, right: { style: BorderStyle.SINGLE, size: 6, color: 'E69138', space: 4 } },
    children: [new TextRun('Важное замечание в рамке: оранжевая граница со всех сторон и светлая заливка.')],
  }),
);

// 6. Lists
children.push(
  h1('6. Списки'),
  check('маркеры и номера стоят слева, текст длинных пунктов переносится под текст, а не под маркер; у вложенных уровней свой отступ и свой вид номера.'),
  p('Маркированный список:'),
  runs([new TextRun('Первый уровень: короткий пункт')], { numbering: { reference: 'bullets', level: 0 } }),
  runs([new TextRun('Длинный пункт списка, который не помещается в одну строку и должен переноситься так, чтобы вторая строка начиналась под текстом пункта, а не под маркером.')], { numbering: { reference: 'bullets', level: 0 } }),
  runs([new TextRun('Второй уровень вложенности')], { numbering: { reference: 'bullets', level: 1 } }),
  runs([new TextRun('Третий уровень вложенности')], { numbering: { reference: 'bullets', level: 2 } }),
  p('Нумерованный список:'),
  runs([new TextRun('Подготовить оборудование')], { numbering: { reference: 'numbers', level: 0 } }),
  runs([new TextRun('Проверить датчики')], { numbering: { reference: 'numbers', level: 0 } }),
  runs([new TextRun('Датчик температуры')], { numbering: { reference: 'numbers', level: 1 } }),
  runs([new TextRun('Датчик напряжения')], { numbering: { reference: 'numbers', level: 1 } }),
  runs([new TextRun('Запустить мониторинг и сравнить показания с паспортными значениями: этот пункт длинный, чтобы был виден перенос.')], { numbering: { reference: 'numbers', level: 0 } }),
);

// 7. Tables
const col3 = [1600, 5038, 3000];
children.push(
  h1('7. Таблицы'),
  check('шапка с заливкой, границы, ширина колонок как в Word; объединённые ячейки; выравнивание внутри ячеек по горизонтали и вертикали.'),
  new Table({
    width: { size: PAGE_TEXT_WIDTH, type: WidthType.DXA },
    columnWidths: col3,
    rows: [
      new TableRow({ tableHeader: true, children: [
        cell('№', col3[0], { bold: true, fill: 'DCE6F2', align: AlignmentType.CENTER }),
        cell('Показатель', col3[1], { bold: true, fill: 'DCE6F2' }),
        cell('Значение', col3[2], { bold: true, fill: 'DCE6F2', align: AlignmentType.RIGHT }),
      ] }),
      ...[['1', 'Температура корпуса в рабочем режиме', '45 °C'], ['2', 'Напряжение питания', '220 В'], ['3', 'Идентификатор устройства', 'ABCDEF0123456789XYZ']]
        .map(([n, name, value]) => new TableRow({ children: [cell(n, col3[0], { align: AlignmentType.CENTER }), cell(name, col3[1]), cell(value, col3[2], { align: AlignmentType.RIGHT })] })),
    ],
  }),
  h2('Объединённые ячейки'),
  new Table({
    width: { size: PAGE_TEXT_WIDTH, type: WidthType.DXA },
    columnWidths: [3212, 3213, 3213],
    rows: [
      new TableRow({ children: [cell('Объединено на три колонки', 9638, { columnSpan: 3, fill: 'E2EFDA', align: AlignmentType.CENTER, bold: true })] }),
      new TableRow({ children: [cell('Объединено на две строки', 3212, { rowSpan: 2, vertical: VerticalAlign.CENTER, align: AlignmentType.CENTER }), cell('Ячейка B2', 3213), cell('Ячейка C2', 3213)] }),
      new TableRow({ children: [cell('Ячейка B3', 3213), cell('Ячейка C3', 3213)] }),
    ],
  }),
  h2('Выравнивание в ячейках'),
  new Table({
    width: { size: PAGE_TEXT_WIDTH, type: WidthType.DXA },
    columnWidths: [3212, 3213, 3213],
    rows: [VerticalAlign.TOP, VerticalAlign.CENTER, VerticalAlign.BOTTOM].map((vertical, row) => new TableRow({
      height: { value: 900, rule: 'atLeast' },
      children: [AlignmentType.LEFT, AlignmentType.CENTER, AlignmentType.RIGHT].map((align, column) =>
        cell(`${['верх', 'центр', 'низ'][row]} / ${['лево', 'центр', 'право'][column]}`, [3212, 3213, 3213][column], { vertical, align })),
    })),
  }),
);

// 8. Images
children.push(
  h1('8. Рисунки'),
  check('картинки по левому краю, по центру и по правому; размеры как в Word; текст обтекает картинку справа.'),
  runs([new ImageRun({ type: 'png', data: image('icon.png'), transformation: { width: 64, height: 64 } })], { alignment: AlignmentType.LEFT }),
  runs([new ImageRun({ type: 'png', data: image('banner.png'), transformation: { width: 450, height: 130 } })], { alignment: AlignmentType.CENTER }),
  p('Рисунок 1. Картинка по центру шириной 450 пикселей.', { alignment: AlignmentType.CENTER }),
  runs([new ImageRun({ type: 'png', data: image('icon.png'), transformation: { width: 64, height: 64 } })], { alignment: AlignmentType.RIGHT }),
  h2('Обтекание текстом'),
  runs([
    new ImageRun({
      type: 'png', data: image('square.png'), transformation: { width: 140, height: 140 },
      floating: {
        horizontalPosition: { relative: HorizontalPositionRelativeFrom.COLUMN, align: HorizontalPositionAlign.LEFT },
        verticalPosition: { relative: VerticalPositionRelativeFrom.PARAGRAPH, offset: 0 },
        wrap: { type: TextWrappingType.SQUARE, side: TextWrappingSide.RIGHT },
        margins: { right: 114300, bottom: 57150 },
      },
    }),
    new TextRun(LOREM + ' ' + LOREM),
  ]),
);

// 9. Charts (filled in after generation)
children.push(
  h1('9. Диаграммы'),
  check('диаграммы Word отображаются картинками: столбцы, подписи осей, легенда; круговая — с долями.'),
  p('{{CHART_BAR}}', { alignment: AlignmentType.CENTER }),
  p('{{CHART_PIE}}', { alignment: AlignmentType.CENTER }),
);

// 10. Long words
children.push(
  h1('10. Длинные слова и неразрывные пробелы'),
  check('длинное слово и текст, склеенный неразрывными пробелами, переносятся внутри страницы, а не вылезают за край.'),
  runs([new TextRun({ text: 'ОченьДлинноеСловоБезПробеловКотороеНеПомещаетсяВСтроку', bold: true, size: 40 })]),
  p('Текст скопирован с сайта, и все пробелы в нём неразрывные, поэтому для браузера это одно длинное слово, которое всё равно должно переноситься.'),
);

const columnsSection = {
  properties: { type: SectionType.CONTINUOUS, column: { count: 2, space: 708 } },
  children: [
    h1('11. Две колонки'),
    check('текст идёт в две колонки, как газета.'),
    p(LOREM + ' ' + LOREM, { alignment: AlignmentType.JUSTIFIED }),
    p(LOREM, { alignment: AlignmentType.JUSTIFIED }),
  ],
};

const header = new Header({ children: [new Paragraph({ alignment: AlignmentType.RIGHT, children: [new TextRun({ text: 'Демонстрация просмотра Word · колонтитул', color: '808080', size: 18 })] })] });
const footer = new Footer({ children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: 'Страница ', size: 18 }), new TextRun({ children: [PageNumber.CURRENT], size: 18 }), new TextRun({ text: ' из ', size: 18 }), new TextRun({ children: [PageNumber.TOTAL_PAGES], size: 18 })] })] });
const page = { size: { width: 11906, height: 16838 }, margin: { top: 1134, right: 1134, bottom: 1134, left: 1134 } };

const doc = new Document({
  creator: 'MonitoringDevices',
  title: 'Демонстрация просмотра Word',
  styles: {
    default: { document: { run: { font: FONT, size: 22 }, paragraph: { spacing: { after: 120, line: 264, lineRule: LineRuleType.AUTO } } } },
    paragraphStyles: [
      { id: 'Title', name: 'Title', basedOn: 'Normal', next: 'Normal', run: { font: FONT, size: 48, bold: true, color: '1F3864' }, paragraph: { spacing: { before: 0, after: 120 }, keepNext: true } },
      { id: 'Heading1', name: 'Heading 1', basedOn: 'Normal', next: 'Normal', quickFormat: true, run: { font: FONT, size: 32, bold: true, color: '2F5597' }, paragraph: { spacing: { before: 360, after: 120 }, keepNext: true, outlineLevel: 0 } },
      { id: 'Heading2', name: 'Heading 2', basedOn: 'Normal', next: 'Normal', quickFormat: true, run: { font: FONT, size: 24, bold: true, color: '404040' }, paragraph: { spacing: { before: 200, after: 80 }, keepNext: true, outlineLevel: 1 } },
    ],
    characterStyles: [{ id: 'Hyperlink', name: 'Hyperlink', run: { color: '0563C1', underline: { type: UnderlineType.SINGLE } } }],
  },
  numbering: {
    config: [
      { reference: 'bullets', levels: [
        { level: 0, format: LevelFormat.BULLET, text: '•', alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 720, hanging: 360 } } } },
        { level: 1, format: LevelFormat.BULLET, text: '–', alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 1440, hanging: 360 } } } },
        { level: 2, format: LevelFormat.BULLET, text: '▪', alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 2160, hanging: 360 } } } },
      ] },
      { reference: 'numbers', levels: [
        { level: 0, format: LevelFormat.DECIMAL, text: '%1.', alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 720, hanging: 360 } } } },
        { level: 1, format: LevelFormat.LOWER_LETTER, text: '%2)', alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 1440, hanging: 360 } } } },
      ] },
    ],
  },
  footnotes: { 1: { children: [new Paragraph({ children: [new TextRun('Текст сноски: она должна оказаться внизу страницы или в конце документа.')] })] } },
  sections: [
    { properties: { page }, headers: { default: header }, footers: { default: footer }, children },
    { ...columnsSection, properties: { ...columnsSection.properties, page } },
  ],
});

Packer.toBuffer(doc).then((buffer) => fs.writeFileSync(outFile, buffer));
