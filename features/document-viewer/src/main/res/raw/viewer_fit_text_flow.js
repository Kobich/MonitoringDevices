// Word documents are reflowed to the screen width (`.odr-text-flow`), but pictures, charts and
// tables keep their size in the document, so a 16 cm picture sticks out of a phone screen.
// Each one that sticks out is scaled down with css `zoom`, which shrinks its layout box too.
// Text keeps its size. Other documents have no `.odr-text-flow`, so the script does nothing.
(function () {
  "use strict";
  if (window.viewerFitTextFlow) {
    return;
  }
  window.viewerFitTextFlow = true;

  var FITTED = "data-viewer-fit";
  var CANDIDATES = "img,svg,video,canvas,table,[style*='width']";
  var flow = null;
  var scheduled = false;
  var lastWidth = 0;

  // The right edge text may reach, in the same coordinates as `getBoundingClientRect`.
  function contentRight() {
    var box = flow.getBoundingClientRect();
    var padding = parseFloat(getComputedStyle(flow).paddingRight) || 0;
    // the padding is in css px, the box may carry the body zoom
    var scale = flow.offsetWidth ? box.width / flow.offsetWidth : 1;
    return box.right - padding * scale;
  }

  // Inside a chunk the library hasn't laid out yet: measuring would force its layout.
  function inHiddenChunk(element) {
    var chunk = element.closest(".odr-chunk");
    return chunk !== null && !chunk.classList.contains("odr-chunk-near");
  }

  function fit() {
    scheduled = false;
    var width = flow.clientWidth;
    if (!width) {
      return;
    }
    if (width !== lastWidth) {
      var fitted = flow.querySelectorAll("[" + FITTED + "]");
      for (var i = 0; i < fitted.length; i++) {
        fitted[i].style.zoom = "";
        fitted[i].removeAttribute(FITTED);
      }
      lastWidth = width;
    }
    var right = contentRight();
    var candidates = flow.querySelectorAll(CANDIDATES);
    for (var j = 0; j < candidates.length; j++) {
      var element = candidates[j];
      // An outer element already scaled down carries its content with it.
      if (element.closest("[" + FITTED + "]") || inHiddenChunk(element)) {
        continue;
      }
      var box = element.getBoundingClientRect();
      if (box.width <= 0 || box.right <= right + 1) {
        continue;
      }
      var room = right - box.left;
      if (room <= 0) {
        continue;
      }
      element.style.zoom = String(room / box.width);
      element.setAttribute(FITTED, "");
    }
  }

  function schedule() {
    if (!scheduled) {
      scheduled = true;
      requestAnimationFrame(fit);
    }
  }

  function start() {
    flow = document.querySelector(".odr-text-flow");
    if (!flow) {
      return;
    }
    fit();
    window.addEventListener("resize", schedule);
    // Pictures get their size once loaded; chunks are filled and laid out as they come near.
    flow.addEventListener("load", schedule, true);
    new MutationObserver(function (records) {
      for (var i = 0; i < records.length; i++) {
        var target = records[i].target;
        // Our own zoom changes `style`, which isn't observed; a chunk's class says it is near.
        if (records[i].type === "childList" || (target.classList && target.classList.contains("odr-chunk"))) {
          schedule();
          return;
        }
      }
    }).observe(flow, { childList: true, subtree: true, attributes: true, attributeFilter: ["class"] });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", start);
  } else {
    start();
  }
})();
