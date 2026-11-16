package com.android.core.document.impl.office

import com.android.core.document.impl.ooxml.OoxmlXml.NS_WORD
import com.android.core.document.impl.ooxml.OoxmlXml.REL_SETTINGS
import com.android.core.document.impl.ooxml.OoxmlXml.REL_STYLES
import com.android.core.document.impl.ooxml.OoxmlXml.child
import com.android.core.document.impl.ooxml.OoxmlXml.children
import com.android.core.document.impl.ooxml.OoxmlXml.descendants
import com.android.core.document.impl.ooxml.editWordMainPart
import org.w3c.dom.Element
import java.io.File
import kotlin.math.roundToInt

/**
 * OpenDocument.core keeps a tab as a tab character, which the browser moves to the next half inch: custom
 * tab stops of Word (right-aligned page numbers with dot leaders, centered columns, numbers aligned on the
 * decimal point) are lost. Each paragraph that has tabs and tab stops gets a hidden bookmark that lists
 * the stops (see [WordBookmarks]), and [script] lays its tabs out in the page.
 */
internal class DocxTabStops : DocumentPatch {

    override fun apply(input: File, output: File): Boolean = editWordMainPart(input, output) { word ->
        val styles = WordParagraphStyles(word.relationships.firstTargetOfType(REL_STYLES)?.let(word.ooxml::partOrNull))
        val defaultStep = word.relationships.firstTargetOfType(REL_SETTINGS)?.let(word.ooxml::partOrNull)
            ?.documentElement?.child(NS_WORD, "defaultTabStop")?.wordAttribute("val")?.toIntOrNull()
            ?.takeIf { it > 0 } ?: DEFAULT_TAB_STOP
        val bookmarks = WordBookmarks(word.document)

        var marked = 0
        word.document.documentElement.descendants(NS_WORD, "p").forEach { paragraph ->
            if (!paragraph.hasTabs()) return@forEach
            val stops = tabStops(styles.sources(paragraph)).takeIf { it.isNotEmpty() } ?: return@forEach
            bookmarks.insertAtStart(paragraph, bookmarkName(defaultStep, stops, marked))
            marked++
        }
        marked
    }

    /** The tab the script sizes; its bottom sits on the baseline, where the leader is drawn. */
    override fun css(outputs: Sequence<String>): String = ".odr-tab{display:inline-block;height:.25em}"

    override val script: String
        get() = SCRIPT

    /** Word writes a tab as `w:tab` in a run (not the stop definitions in `w:tabs`); other tools as a tab character. */
    private fun Element.hasTabs(): Boolean {
        val tabElement = descendants(NS_WORD, "tab").any { tab ->
            val parent = tab.parentNode
            parent is Element && parent.namespaceURI == NS_WORD && parent.localName == "r"
        }
        return tabElement || descendants(NS_WORD, "t").any { text -> '\t' in text.textContent }
    }

    /**
     * Like Word, stops of the paragraph add to and override those of its styles, from the most general
     * source to the paragraph itself; `clear` removes an inherited stop.
     */
    private fun tabStops(sources: List<Element>): List<TabStop> {
        val stops = sortedMapOf<Int, TabStop>()
        sources.asReversed().forEach { source ->
            source.child(NS_WORD, "tabs")?.children(NS_WORD, "tab")?.forEach { tab ->
                val position = tab.wordAttribute("pos")?.toDoubleOrNull()?.roundToInt() ?: return@forEach
                val kind = tab.wordAttribute("val")
                if (kind == "clear") {
                    stops.remove(position)
                } else {
                    val letter = KINDS[kind] ?: return@forEach
                    stops[position] = TabStop(letter, position, LEADERS[tab.wordAttribute("leader")] ?: 'n')
                }
            }
        }
        return stops.values.filter { stop -> stop.position > 0 }
    }

    private class TabStop(val kind: Char, val position: Int, val leader: Char)

    private companion object {
        /** Word's own default when settings.xml doesn't state one: 1.25 cm. */
        const val DEFAULT_TAB_STOP = 708

        /** Left, centered, right and decimal; a bar tab draws a line instead of moving the text and is left out. */
        val KINDS = mapOf(
            "left" to 'L', "start" to 'L', "num" to 'L', "center" to 'C', "right" to 'R', "end" to 'R', "decimal" to 'D'
        )

        /** Dots, hyphens, a line; anything else draws no leader. */
        val LEADERS = mapOf("dot" to 'd', "middleDot" to 'm', "hyphen" to 'h', "underscore" to 'u', "heavy" to 'u')

        /** `_OdrTabs_<default step>_<kind><position><leader>..._<index>`, positions in twips. */
        fun bookmarkName(defaultStep: Int, stops: List<TabStop>, index: Int): String {
            val encoded = stops.joinToString(separator = "") { stop -> "${stop.kind}${stop.position}${stop.leader}" }
            return "_OdrTabs_${defaultStep}_${encoded}_$index"
        }

        /**
         * Replaces each tab of a marked paragraph with a span as wide as Word's tab: up to the next stop, minus
         * the following text for a right stop, half of it for a centered one, up to its decimal point for a
         * decimal one. Stops count from the left edge of the text, or of the cell in a table. Paragraphs in
         * parts of a long document that aren't laid out yet are handled when they come near.
         */
        const val SCRIPT = """(function () {
  'use strict';
  var MARK = /^_OdrTabs_(\d+)_((?:[LCRD]\d+[ndhum])+)_\d+$/;
  var STOP = /([LCRD])(\d+)([ndhum])/g;
  var TWIPS_PER_PX = 15;
  var DOTS = 'radial-gradient(circle, currentColor 0.6px, transparent 0.9px) 0 100% / 4px 3px repeat-x';
  var LINES = { h: 'dashed', u: 'solid' };

  function parse(id) {
    var match = MARK.exec(id);
    if (!match) return null;
    var stops = [];
    var stop;
    STOP.lastIndex = 0;
    while ((stop = STOP.exec(match[2])) !== null) {
      stops.push({ kind: stop[1], position: Number(stop[2]) / TWIPS_PER_PX, leader: stop[3] });
    }
    return { step: Number(match[1]) / TWIPS_PER_PX, stops: stops };
  }

  function originOf(paragraph) {
    var cell = paragraph.closest('td');
    var box = cell || paragraph.closest('.odr-page-inner') || paragraph.parentElement;
    var rect = box.getBoundingClientRect();
    var scale = box.offsetWidth ? rect.width / box.offsetWidth : 1;
    var left = rect.left;
    if (cell) {
      var style = getComputedStyle(cell);
      left += (parseFloat(style.borderLeftWidth) + parseFloat(style.paddingLeft)) * scale;
    }
    return { left: left, scale: scale };
  }

  function splitTabs(paragraph) {
    var walker = document.createTreeWalker(paragraph, NodeFilter.SHOW_TEXT);
    var nodes = [];
    while (walker.nextNode()) {
      if (walker.currentNode.nodeValue.indexOf('\t') >= 0) nodes.push(walker.currentNode);
    }
    var tabs = [];
    nodes.forEach(function (node) {
      var fragment = document.createDocumentFragment();
      node.nodeValue.split('\t').forEach(function (part, index) {
        if (index > 0) {
          var tab = document.createElement('span');
          tab.className = 'odr-tab';
          fragment.appendChild(tab);
          tabs.push(tab);
        }
        if (part) fragment.appendChild(document.createTextNode(part));
      });
      node.parentNode.replaceChild(fragment, node);
    });
    return tabs;
  }

  function decimalPoint(tab, next, paragraph) {
    var walker = document.createTreeWalker(paragraph, NodeFilter.SHOW_TEXT);
    walker.currentNode = tab;
    while (walker.nextNode()) {
      var node = walker.currentNode;
      if (next && (next.compareDocumentPosition(node) & Node.DOCUMENT_POSITION_FOLLOWING)) return null;
      var index = node.nodeValue.search(/[.,]/);
      if (index >= 0) return { node: node, offset: index };
    }
    return null;
  }

  function textWidth(tab, next, paragraph, decimal) {
    var range = document.createRange();
    range.setStartAfter(tab);
    var point = decimal ? decimalPoint(tab, next, paragraph) : null;
    if (point) range.setEnd(point.node, point.offset);
    else if (next) range.setEndBefore(next);
    else range.setEnd(paragraph, paragraph.childNodes.length);
    var top = tab.getBoundingClientRect().bottom;
    var left = Infinity;
    var right = -Infinity;
    [].forEach.call(range.getClientRects(), function (rect) {
      if (rect.width > 0 && rect.top < top && rect.bottom > top - 1) {
        left = Math.min(left, rect.left);
        right = Math.max(right, rect.right);
      }
    });
    return right > left ? right - left : 0;
  }

  function layout(paragraph, settings) {
    var tabs = splitTabs(paragraph);
    if (!tabs.length) return;
    var origin = originOf(paragraph);
    tabs.forEach(function (tab, index) {
      var x = (tab.getBoundingClientRect().left - origin.left) / origin.scale;
      var stop = settings.stops.filter(function (candidate) { return candidate.position > x + 0.5; })[0] ||
        { kind: 'L', position: (Math.floor(x / settings.step) + 1) * settings.step, leader: 'n' };
      var next = tabs[index + 1];
      var width = stop.position - x;
      if (stop.kind === 'R') width -= textWidth(tab, next, paragraph, false) / origin.scale;
      else if (stop.kind === 'C') width -= textWidth(tab, next, paragraph, false) / origin.scale / 2;
      else if (stop.kind === 'D') width -= textWidth(tab, next, paragraph, true) / origin.scale;
      tab.style.width = Math.max(0, width) + 'px';
      if (stop.leader === 'd' || stop.leader === 'm') tab.style.background = DOTS;
      else if (LINES[stop.leader]) tab.style.borderBottom = '1px ' + LINES[stop.leader] + ' currentColor';
    });
  }

  function run() {
    [].forEach.call(document.querySelectorAll('x-p > a[id^="_OdrTabs_"]'), function (mark) {
      var paragraph = mark.parentElement;
      if (paragraph.hasAttribute('data-odr-tabs')) return;
      var chunk = paragraph.closest('.odr-chunk');
      if (chunk && !chunk.classList.contains('odr-chunk-near')) return;
      var settings = parse(mark.id);
      if (!settings) return;
      paragraph.setAttribute('data-odr-tabs', '');
      layout(paragraph, settings);
    });
  }

  var scheduled = false;
  function schedule() {
    if (scheduled) return;
    scheduled = true;
    requestAnimationFrame(function () {
      scheduled = false;
      run();
    });
  }

  function start() {
    (document.fonts ? document.fonts.ready : Promise.resolve()).then(function () {
      run();
      new MutationObserver(schedule).observe(document.body, {
        childList: true, subtree: true, attributes: true, attributeFilter: ['class']
      });
    });
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', start);
  else start();
})();"""
    }
}
