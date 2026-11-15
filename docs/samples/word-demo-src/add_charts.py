"""Adds two Word charts (bar and pie) in place of the {{CHART_*}} paragraphs and fixes what Word's schema rejects.

Usage: python3 add_charts.py base.docx word-demo.docx
"""
import re, sys, zipfile
src, dst = sys.argv[1], sys.argv[2]
C = 'http://schemas.openxmlformats.org/drawingml/2006/chart'
A = 'http://schemas.openxmlformats.org/drawingml/2006/main'
R = 'http://schemas.openxmlformats.org/officeDocument/2006/relationships'

def str_cache(values):
    pts = ''.join(f'<c:pt idx="{i}"><c:v>{v}</c:v></c:pt>' for i, v in enumerate(values))
    return f'<c:strRef><c:f>Sheet1!$A$2:$A${len(values)+1}</c:f><c:strCache><c:ptCount val="{len(values)}"/>{pts}</c:strCache></c:strRef>'
def num_cache(values, column):
    pts = ''.join(f'<c:pt idx="{i}"><c:v>{v}</c:v></c:pt>' for i, v in enumerate(values))
    return f'<c:numRef><c:f>Sheet1!${column}$2:${column}${len(values)+1}</c:f><c:numCache><c:formatCode>General</c:formatCode><c:ptCount val="{len(values)}"/>{pts}</c:numCache></c:numRef>'
def series(index, name, column, categories, values, color=None, point_colors=()):
    fill = f'<c:spPr><a:solidFill><a:srgbClr val="{color}"/></a:solidFill></c:spPr>' if color else ''
    fill += ''.join(f'<c:dPt><c:idx val="{i}"/><c:bubble3D val="0"/><c:spPr><a:solidFill><a:srgbClr val="{c}"/></a:solidFill></c:spPr></c:dPt>' for i, c in enumerate(point_colors))
    return (f'<c:ser><c:idx val="{index}"/><c:order val="{index}"/>'
            f'<c:tx><c:strRef><c:f>Sheet1!${column}$1</c:f><c:strCache><c:ptCount val="1"/><c:pt idx="0"><c:v>{name}</c:v></c:pt></c:strCache></c:strRef></c:tx>'
            f'{fill}<c:cat>{str_cache(categories)}</c:cat><c:val>{num_cache(values, column)}</c:val></c:ser>')
def title(t): return f'<c:title><c:tx><c:rich><a:bodyPr/><a:p><a:r><a:t>{t}</a:t></a:r></a:p></c:rich></c:tx><c:overlay val="0"/></c:title><c:autoTitleDeleted val="0"/>'
def chart_space(plot, legend='b'):
    return (f'<?xml version="1.0" encoding="UTF-8" standalone="yes"?><c:chartSpace xmlns:c="{C}" xmlns:a="{A}" xmlns:r="{R}">'
            f'<c:chart>{plot}<c:legend><c:legendPos val="{legend}"/><c:overlay val="0"/></c:legend><c:plotVisOnly val="1"/></c:chart></c:chartSpace>')

quarters = ['I кв.', 'II кв.', 'III кв.', 'IV кв.']
bar = chart_space(title('Срабатывания датчиков по кварталам') +
    '<c:plotArea><c:barChart><c:barDir val="col"/><c:grouping val="clustered"/><c:varyColors val="0"/>' +
    series(0, 'Температура', 'B', quarters, [12, 18, 9, 15], '4472C4') +
    series(1, 'Напряжение', 'C', quarters, [7, 5, 11, 8], 'ED7D31') +
    series(2, 'Связь', 'D', quarters, [3, 6, 4, 10], 'A5A5A5') +
    '<c:gapWidth val="150"/><c:axId val="1"/><c:axId val="2"/></c:barChart>'
    '<c:catAx><c:axId val="1"/><c:scaling><c:orientation val="minMax"/></c:scaling><c:delete val="0"/><c:axPos val="b"/><c:crossAx val="2"/></c:catAx>'
    '<c:valAx><c:axId val="2"/><c:scaling><c:orientation val="minMax"/></c:scaling><c:delete val="0"/><c:axPos val="l"/><c:majorGridlines/><c:crossAx val="1"/></c:valAx>'
    '</c:plotArea>')
parts = ['Датчики', 'Связь', 'Питание', 'Прочее']
pie = chart_space(title('Причины остановки оборудования') +
    '<c:plotArea><c:pieChart><c:varyColors val="1"/>' + series(0, 'Доля', 'B', parts, [45, 25, 18, 12], point_colors=['4472C4', 'ED7D31', 'A5A5A5', 'FFC000']) +
    '<c:firstSliceAng val="0"/></c:pieChart></c:plotArea>', legend='r')

def drawing(rel_id, doc_id, name, cx, cy):
    return (f'<w:r><w:drawing><wp:inline distT="0" distB="0" distL="0" distR="0"><wp:extent cx="{cx}" cy="{cy}"/>'
            f'<wp:effectExtent l="0" t="0" r="0" b="0"/><wp:docPr id="{doc_id}" name="{name}"/><wp:cNvGraphicFramePr/>'
            f'<a:graphic xmlns:a="{A}"><a:graphicData uri="{C}"><c:chart xmlns:c="{C}" xmlns:r="{R}" r:id="{rel_id}"/></a:graphicData></a:graphic>'
            f'</wp:inline></w:drawing></w:r>')

zin = zipfile.ZipFile(src)
with zipfile.ZipFile(dst, 'w', zipfile.ZIP_DEFLATED) as zout:
    for info in zin.infolist():
        if info.filename.endswith('/'):
            continue
        data = zin.read(info.filename)
        if info.filename == 'word/document.xml':
            d = data.decode()
            # docx-js writes w:highlightCs next to w:highlight; Word's schema has no such element there.
            d = re.sub(r'<w:highlightCs [^>]*/>', '', d)
            # docx-js writes paragraph borders as top, bottom, left, right; the schema wants top, left, bottom, right.
            def reorder(match):
                sides = {name: element for element, name in re.findall(r'(<w:(top|left|bottom|right|between|bar) [^>]*/>)', match.group(1))}
                return '<w:pBdr>' + ''.join(sides[k] for k in ('top', 'left', 'bottom', 'right', 'between', 'bar') if k in sides) + '</w:pBdr>'
            d = re.sub(r'<w:pBdr>(.*?)</w:pBdr>', reorder, d)
            for placeholder, rel, doc_id, name, size in [('{{CHART_BAR}}', 'rIdDemoChart1', 9001, 'Диаграмма 1', (5486400, 3200400)),
                                                         ('{{CHART_PIE}}', 'rIdDemoChart2', 9002, 'Диаграмма 2', (4572000, 2743200))]:
                run = f'<w:r><w:t xml:space="preserve">{placeholder}</w:t></w:r>'
                assert d.count(run) == 1, placeholder
                d = d.replace(run, drawing(rel, doc_id, name, *size))
            data = d.encode()
        elif info.filename == 'word/_rels/document.xml.rels':
            d = data.decode()
            rels = ''.join(f'<Relationship Id="rIdDemoChart{i}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/chart" Target="charts/chart{i}.xml"/>' for i in (1, 2))
            data = d.replace('</Relationships>', rels + '</Relationships>').encode()
        elif info.filename == '[Content_Types].xml':
            d = data.decode()
            ov = ''.join(f'<Override PartName="/word/charts/chart{i}.xml" ContentType="application/vnd.openxmlformats-officedocument.drawingml.chart+xml"/>' for i in (1, 2))
            data = d.replace('</Types>', ov + '</Types>').encode()
        zout.writestr(info, data)
    zout.writestr('word/charts/chart1.xml', bar)
    zout.writestr('word/charts/chart2.xml', pie)
