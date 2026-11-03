package com.engboost.monitoringdevices.core.document.impl.chart

/** Minimal parts the way Word writes them. */
internal object ChartXmlFixtures {
    private const val CHART_NAMESPACES =
        """xmlns:c="http://schemas.openxmlformats.org/drawingml/2006/chart" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships""""

    private fun series(index: Int, name: String, values: List<String>, color: String) = """
        <c:ser>
          <c:idx val="$index"/><c:order val="$index"/>
          <c:tx><c:strRef><c:f>Sheet1!${'$'}B${'$'}1</c:f><c:strCache><c:ptCount val="1"/><c:pt idx="0"><c:v>$name</c:v></c:pt></c:strCache></c:strRef></c:tx>
          <c:spPr><a:solidFill>$color</a:solidFill></c:spPr>
          <c:cat><c:strRef><c:f>Sheet1!${'$'}A${'$'}2:${'$'}A${'$'}4</c:f><c:strCache><c:ptCount val="3"/>
            <c:pt idx="0"><c:v>Q1</c:v></c:pt><c:pt idx="1"><c:v>Q2</c:v></c:pt><c:pt idx="2"><c:v>Q3</c:v></c:pt>
          </c:strCache></c:strRef></c:cat>
          <c:val><c:numRef><c:f>Sheet1!${'$'}B${'$'}2:${'$'}B${'$'}4</c:f><c:numCache><c:formatCode>General</c:formatCode><c:ptCount val="3"/>
            ${values.mapIndexedNotNull { i, v -> if (v.isEmpty()) null else """<c:pt idx="$i"><c:v>$v</c:v></c:pt>""" }.joinToString("")}
          </c:numCache></c:numRef></c:val>
        </c:ser>
    """

    val COLUMN_CHART = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <c:chartSpace $CHART_NAMESPACES>
          <c:chart>
            <c:title><c:tx><c:rich><a:bodyPr/><a:p><a:r><a:t>Sales</a:t></a:r><a:r><a:t> 2026</a:t></a:r></a:p></c:rich></c:tx></c:title>
            <c:autoTitleDeleted val="0"/>
            <c:plotArea>
              <c:layout/>
              <c:barChart>
                <c:barDir val="col"/><c:grouping val="clustered"/><c:varyColors val="0"/>
                ${series(0, "North", listOf("4.3", "2.5", "3.5"), """<a:schemeClr val="accent1"/>""")}
                ${series(1, "South", listOf("2.4", "", "1.8"), """<a:srgbClr val="FF0000"/>""")}
                <c:gapWidth val="219"/>
                <c:axId val="1"/><c:axId val="2"/>
              </c:barChart>
              <c:catAx><c:axId val="1"/></c:catAx>
              <c:valAx><c:axId val="2"/></c:valAx>
            </c:plotArea>
            <c:legend><c:legendPos val="b"/></c:legend>
          </c:chart>
        </c:chartSpace>
    """.trimIndent()

    val PIE_CHART = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <c:chartSpace $CHART_NAMESPACES>
          <c:chart>
            <c:plotArea>
              <c:pieChart>
                <c:varyColors val="1"/>
                ${series(0, "Share", listOf("8.2", "3.2", "1.4"), """<a:schemeClr val="accent2"/>""")}
                <c:firstSliceAng val="30"/>
              </c:pieChart>
            </c:plotArea>
          </c:chart>
        </c:chartSpace>
    """.trimIndent()

    val UNSUPPORTED_CHART = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <c:chartSpace $CHART_NAMESPACES>
          <c:chart><c:plotArea><c:radarChart/></c:plotArea></c:chart>
        </c:chartSpace>
    """.trimIndent()

    val THEME = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="Custom">
          <a:themeElements>
            <a:clrScheme name="Custom">
              <a:dk1><a:sysClr val="windowText" lastClr="000000"/></a:dk1>
              <a:accent1><a:srgbClr val="112233"/></a:accent1>
              <a:accent2><a:srgbClr val="445566"/></a:accent2>
            </a:clrScheme>
          </a:themeElements>
        </a:theme>
    """.trimIndent()
}
