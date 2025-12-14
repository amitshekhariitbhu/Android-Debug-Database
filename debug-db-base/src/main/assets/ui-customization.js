/**
 * UI customization for Android Debug Database.
 *
 * - Stored in browser localStorage (per-browser).
 * - Visual-only settings.
 * - Safe defaults that replicate existing UI.
 */
(function (global) {
  "use strict";

  var STORAGE_KEY = "addb.uiCustomization.v1";

  var DEFAULT_SETTINGS = {
    tableWidth: "fixed", // fixed | full | fluid
    sidebarWidth: "standard", // standard | compact
    rowDensity: "normal", // compact | normal | comfortable
    wrapLongText: false,
    maxColumnWidthPx: null, // number | null
    jsonMode: "raw", // raw | wrapped | pretty
    stickyHeader: false,
    pageLength: 10 // 10 | 25 | 50 | 100
  };

  var allowedTableWidths = { fixed: true, full: true, fluid: true };
  var allowedSidebarWidths = { standard: true, compact: true };
  var allowedRowDensities = { compact: true, normal: true, comfortable: true };
  var allowedJsonModes = { raw: true, wrapped: true, pretty: true };
  var allowedPageLengths = { 10: true, 25: true, 50: true, 100: true };

  var currentSettings = null;
  var currentDataTable = null;
  var currentDataTableDrawHandler = null;
  var currentDataTableContainer = null;
  var pendingDataTableRender = false;
  var pendingColumnsAdjust = false;

  var JSON_INDENT_PX = 16;

  function safeParseJson(json) {
    try {
      return JSON.parse(json);
    } catch (e) {
      return null;
    }
  }

  function safeGetLocalStorageItem(key) {
    try {
      if (!global.localStorage) return null;
      return global.localStorage.getItem(key);
    } catch (e) {
      return null;
    }
  }

  function safeSetLocalStorageItem(key, value) {
    try {
      if (!global.localStorage) return false;
      global.localStorage.setItem(key, value);
      return true;
    } catch (e) {
      return false;
    }
  }

  function safeRemoveLocalStorageItem(key) {
    try {
      if (!global.localStorage) return false;
      global.localStorage.removeItem(key);
      return true;
    } catch (e) {
      return false;
    }
  }

  function normalizeBoolean(value, fallback) {
    if (value === true || value === false) return value;
    if (value === "true") return true;
    if (value === "false") return false;
    return fallback;
  }

  function clampInt(value, min, max) {
    if (typeof value !== "number") return null;
    if (value < min) return min;
    if (value > max) return max;
    return value;
  }

  function normalizeSettings(raw) {
    var settings = $.extend({}, DEFAULT_SETTINGS);
    if (!raw || typeof raw !== "object") return settings;

    if (allowedTableWidths[raw.tableWidth]) settings.tableWidth = raw.tableWidth;
    if (allowedSidebarWidths[raw.sidebarWidth]) settings.sidebarWidth = raw.sidebarWidth;
    if (allowedRowDensities[raw.rowDensity]) settings.rowDensity = raw.rowDensity;
    if (allowedJsonModes[raw.jsonMode]) settings.jsonMode = raw.jsonMode;

    settings.wrapLongText = normalizeBoolean(raw.wrapLongText, DEFAULT_SETTINGS.wrapLongText);
    settings.stickyHeader = normalizeBoolean(raw.stickyHeader, DEFAULT_SETTINGS.stickyHeader);

    var pageLength = parseInt(raw.pageLength, 10);
    if (allowedPageLengths[pageLength]) settings.pageLength = pageLength;

    var maxColumnWidthPx = raw.maxColumnWidthPx;
    if (maxColumnWidthPx === "" || maxColumnWidthPx === null || typeof maxColumnWidthPx === "undefined") {
      settings.maxColumnWidthPx = null;
    } else {
      var parsedMaxWidth = parseInt(maxColumnWidthPx, 10);
      if (!isNaN(parsedMaxWidth)) {
        settings.maxColumnWidthPx = clampInt(parsedMaxWidth, 120, 2000);
      }
    }

    return settings;
  }

  function loadSettings() {
    var raw = safeGetLocalStorageItem(STORAGE_KEY);
    if (!raw) return normalizeSettings(null);
    return normalizeSettings(safeParseJson(raw));
  }

  function saveSettings(settings) {
    return safeSetLocalStorageItem(STORAGE_KEY, JSON.stringify(settings));
  }

  function clearSettings() {
    return safeRemoveLocalStorageItem(STORAGE_KEY);
  }

  function applyContainerWidth(settings) {
    var container = $("#app-container");
    if (!container.length) return;

    container.removeClass("container container-fluid addb-container-fluid-max");

    if (settings.tableWidth === "fixed") {
      container.addClass("container");
    } else {
      container.addClass("container-fluid");
      if (settings.tableWidth === "fluid") {
        container.addClass("addb-container-fluid-max");
      }
    }
  }

  function setBootstrapSmColumn(el, span) {
    if (!el || !el.length) return;
    if (typeof span !== "number") return;
    if (span < 1) span = 1;
    if (span > 12) span = 12;

    var className = el.attr("class") || "";
    var classes = className.split(/\s+/).filter(Boolean);
    var updated = [];
    for (var i = 0; i < classes.length; i++) {
      if (/^col-sm-\d+$/.test(classes[i])) continue;
      updated.push(classes[i]);
    }
    updated.push("col-sm-" + span);
    el.attr("class", updated.join(" "));
  }

  function applySidebarWidth(settings) {
    var dbCol = $("#addb-db-column");
    var tableCol = $("#addb-table-column");
    var dataCol = $("#parent-data-div");
    if (!dbCol.length || !tableCol.length || !dataCol.length) return;

    var isCompact = settings.sidebarWidth === "compact";
    setBootstrapSmColumn(dbCol, isCompact ? 1 : 2);
    setBootstrapSmColumn(tableCol, isCompact ? 1 : 2);
    setBootstrapSmColumn(dataCol, isCompact ? 10 : 8);
  }

  function applyBodyClasses(settings) {
    var body = $("body");

    body.removeClass("addb-density-compact addb-density-normal addb-density-comfortable");
    body.addClass("addb-density-" + settings.rowDensity);

    body.toggleClass("addb-wrap-long-text", !!settings.wrapLongText);

    body.removeClass("addb-json-mode-raw addb-json-mode-wrapped addb-json-mode-pretty");
    body.addClass("addb-json-mode-" + settings.jsonMode);

    body.toggleClass("addb-sticky-header", !!settings.stickyHeader);

    body.toggleClass("addb-sidebar-compact", settings.sidebarWidth === "compact");
  }

  function applyMaxColumnWidth(settings) {
    var shouldApplyMaxWidth =
      typeof settings.maxColumnWidthPx === "number" &&
      settings.maxColumnWidthPx > 0 &&
      (settings.wrapLongText || settings.jsonMode !== "raw");

    var body = $("body");
    if (shouldApplyMaxWidth) {
      document.documentElement.style.setProperty("--addb-max-col-width", settings.maxColumnWidthPx + "px");
      body.addClass("addb-max-col-width");
    } else {
      document.documentElement.style.removeProperty("--addb-max-col-width");
      body.removeClass("addb-max-col-width");
    }
  }

  function looksLikeJson(value) {
    if (typeof value !== "string") return false;
    var trimmed = value.trim();
    if (trimmed.length < 2) return false;
    var startsOk = trimmed[0] === "{" || trimmed[0] === "[";
    var endsOk = trimmed[trimmed.length - 1] === "}" || trimmed[trimmed.length - 1] === "]";
    return startsOk && endsOk;
  }

  function formatJsonPrimitive(value) {
    if (value === null) return "null";
    var t = typeof value;
    if (t === "string" || t === "number" || t === "boolean") return JSON.stringify(value);
    try {
      return JSON.stringify(value);
    } catch (e) {
      return String(value);
    }
  }

  function createJsonLine(tagName, depth, extraClassName) {
    var el = document.createElement(tagName || "div");
    el.className = "addb-json-line" + (extraClassName ? " " + extraClassName : "");
    el.style.paddingLeft = depth * JSON_INDENT_PX + "px";

    var gutter = document.createElement("span");
    gutter.className = "addb-json-gutter";
    el.appendChild(gutter);

    return { el: el, gutter: gutter };
  }

  function createJsonDetails(depth, openByDefault) {
    var details = document.createElement("details");
    details.className = "addb-json-node";
    if (openByDefault) details.open = true;

    var summaryLine = createJsonLine("summary", depth, "addb-json-summary");

    var closedSpan = document.createElement("span");
    closedSpan.className = "addb-json-summary-closed";

    var openSpan = document.createElement("span");
    openSpan.className = "addb-json-summary-open";

    summaryLine.el.appendChild(closedSpan);
    summaryLine.el.appendChild(openSpan);

    var children = document.createElement("div");
    children.className = "addb-json-children";

    details.appendChild(summaryLine.el);
    details.appendChild(children);

    return {
      details: details,
      closedSpan: closedSpan,
      openSpan: openSpan,
      children: children
    };
  }

  function jsonContainerInfo(value) {
    if (Array.isArray(value)) {
      return { kind: "array", open: "[", close: "]", count: value.length };
    }
    var keys = Object.keys(value);
    return { kind: "object", open: "{", close: "}", count: keys.length, keys: keys };
  }

  function appendJsonValue(parent, value, depth, isLast, keyPrefix) {
    var comma = isLast ? "" : ",";
    var prefix = keyPrefix || "";

    var isContainer = value !== null && typeof value === "object";
    if (!isContainer) {
      var primitiveLine = createJsonLine("div", depth);
      primitiveLine.el.appendChild(document.createTextNode(prefix + formatJsonPrimitive(value) + comma));
      parent.appendChild(primitiveLine.el);
      return;
    }

    var info = jsonContainerInfo(value);
    var isEmpty = info.count === 0;
    if (isEmpty) {
      var emptyLine = createJsonLine("div", depth);
      emptyLine.el.appendChild(document.createTextNode(prefix + info.open + info.close + comma));
      parent.appendChild(emptyLine.el);
      return;
    }

    // Collapse all 2nd-level (depth=1) container values by default.
    var openByDefault = depth !== 1;
    var node = createJsonDetails(depth, openByDefault);

    var collapsedPreview = info.kind === "array" ? "[\u2026] (" + info.count + ")" : "{\u2026} (" + info.count + ")";
    node.closedSpan.textContent = prefix + collapsedPreview + comma;
    node.openSpan.textContent = prefix + info.open;

    if (info.kind === "array") {
      for (var index = 0; index < value.length; index++) {
        appendJsonValue(node.children, value[index], depth + 1, index === value.length - 1, "");
      }
    } else {
      var keys = info.keys;
      for (var keyIndex = 0; keyIndex < keys.length; keyIndex++) {
        var key = keys[keyIndex];
        var childPrefix = JSON.stringify(key) + ": ";
        appendJsonValue(node.children, value[key], depth + 1, keyIndex === keys.length - 1, childPrefix);
      }
    }

    var closingLine = createJsonLine("div", depth);
    closingLine.el.appendChild(document.createTextNode(info.close + comma));
    node.children.appendChild(closingLine.el);

    parent.appendChild(node.details);
  }

  function buildJsonTree(parsedJson) {
    var root = document.createElement("div");
    root.className = "addb-json-tree";

    var info = jsonContainerInfo(parsedJson);

    var openLine = createJsonLine("div", 0);
    openLine.el.appendChild(document.createTextNode(info.open));
    root.appendChild(openLine.el);

    if (info.kind === "array") {
      for (var index = 0; index < parsedJson.length; index++) {
        appendJsonValue(root, parsedJson[index], 1, index === parsedJson.length - 1, "");
      }
    } else {
      var keys = info.keys;
      for (var keyIndex = 0; keyIndex < keys.length; keyIndex++) {
        var key = keys[keyIndex];
        var childPrefix = JSON.stringify(key) + ": ";
        appendJsonValue(root, parsedJson[key], 1, keyIndex === keys.length - 1, childPrefix);
      }
    }

    var closeLine = createJsonLine("div", 0);
    closeLine.el.appendChild(document.createTextNode(info.close));
    root.appendChild(closeLine.el);

    return root;
  }

  function scheduleColumnsAdjust(dataTableApi) {
    if (!dataTableApi) return;
    if (pendingColumnsAdjust) return;
    pendingColumnsAdjust = true;

    global.setTimeout(function () {
      pendingColumnsAdjust = false;
      try {
        var tableNode = $(dataTableApi.table().node());
        if (!tableNode.is(":visible")) return;
        dataTableApi.columns.adjust();
      } catch (e) {
        // Best-effort only.
      }
    }, 0);
  }

  function applyJsonFormattingToCurrentPage(dataTableApi, settings) {
    if (!dataTableApi) return;

    var mode = settings.jsonMode;
    var didMutateDom = false;

    var rows = dataTableApi.rows({ page: "current" }).nodes();
    for (var rowIndex = 0; rowIndex < rows.length; rowIndex++) {
      var cells = rows[rowIndex].cells;
      for (var cellIndex = 0; cellIndex < cells.length; cellIndex++) {
        var td = cells[cellIndex];
        td.classList.remove("addb-json-cell");

        var cellData = dataTableApi.cell(td).data();
        if (!looksLikeJson(cellData)) continue;

        if (mode === "raw") {
          // Restore raw JSON text (undo tree view, if present).
          if (td.textContent !== cellData || td.childNodes.length !== 1 || td.firstChild.nodeType !== 3) {
            td.textContent = cellData;
            didMutateDom = true;
          }
          continue;
        }

        var parsed = safeParseJson(cellData);
        if (parsed === null) continue;

        td.classList.add("addb-json-cell");

        if (mode === "wrapped") {
          // Keep raw JSON text, but apply JSON cell styling.
          if (td.textContent !== cellData || td.childNodes.length !== 1 || td.firstChild.nodeType !== 3) {
            td.textContent = cellData;
            didMutateDom = true;
          }
          continue;
        }

        // Pretty mode: render a collapsible JSON tree with 2nd-level containers collapsed.
        td.textContent = "";
        td.appendChild(buildJsonTree(parsed));
        didMutateDom = true;
      }
    }

    if (didMutateDom) {
      scheduleColumnsAdjust(dataTableApi);
    }
  }

  function applyDataTableSettings(dataTableApi, settings, reason) {
    if (!dataTableApi) return;

    try {
      if (typeof dataTableApi.page === "function" && typeof dataTableApi.page.len === "function") {
        var currentLen = dataTableApi.page.len();
        if (currentLen !== settings.pageLength) {
          dataTableApi.page.len(settings.pageLength);
        }
      }

      if (reason === "jsonMode") {
        dataTableApi.rows().invalidate();
      }

      var tableNode = $(dataTableApi.table().node());
      var isVisible = tableNode.is(":visible");
      if (!isVisible) {
        pendingDataTableRender = true;
        return;
      }

      dataTableApi.columns.adjust();
      dataTableApi.draw(false);
    } catch (e) {
      // Best-effort only; never break the main UI.
    }
  }

  function applySettings(settings, reason) {
    var previousSettings = currentSettings || DEFAULT_SETTINGS;
    currentSettings = normalizeSettings(settings);

    applyContainerWidth(currentSettings);
    applySidebarWidth(currentSettings);
    applyBodyClasses(currentSettings);
    applyMaxColumnWidth(currentSettings);

    var dataTableReason = reason;
    if (previousSettings.jsonMode !== currentSettings.jsonMode) dataTableReason = "jsonMode";

    applyDataTableSettings(currentDataTable, currentSettings, dataTableReason);
  }

  function setCurrentDataTable(dataTableApi) {
    var previousTableNode = currentDataTable ? $(currentDataTable.table().node()) : null;
    if (previousTableNode && currentDataTableDrawHandler) {
      previousTableNode.off("draw.dt", currentDataTableDrawHandler);
    }
    if (currentDataTableContainer) {
      currentDataTableContainer.off("click.addb-json");
      currentDataTableContainer = null;
    }

    currentDataTable = dataTableApi || null;
    if (!currentDataTable) return;

    var tableNode = $(currentDataTable.table().node());
    currentDataTableDrawHandler = function () {
      applyJsonFormattingToCurrentPage(currentDataTable, currentSettings || DEFAULT_SETTINGS);
    };
    tableNode.on("draw.dt", currentDataTableDrawHandler);

    currentDataTableContainer = $(currentDataTable.table().container());
    currentDataTableContainer.on("click.addb-json", "details.addb-json-node > summary", function () {
      scheduleColumnsAdjust(currentDataTable);
    });

    applyDataTableSettings(currentDataTable, currentSettings || DEFAULT_SETTINGS, "init");
  }

  function getQueryValue(key) {
    var query = global.location.search;
    if (!query) return null;
    var keyEscaped = key.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    var matches = query.match(new RegExp("[?&]" + keyEscaped + "=([^&]*)"));
    return matches ? decodeURIComponent(matches[1].replace(/\+/g, " ")) : null;
  }

  function setQueryValue(key, value) {
    var currentSearch = global.location.search || "";
    var hash = global.location.hash || "";
    var base = global.location.pathname;

    var query = currentSearch.replace(/^\?/, "");
    var parts = query ? query.split("&") : [];
    var updated = [];
    var found = false;

    for (var i = 0; i < parts.length; i++) {
      if (!parts[i]) continue;
      var kv = parts[i].split("=");
      var k = decodeURIComponent(kv[0] || "");
      if (k !== key) {
        updated.push(parts[i]);
        continue;
      }
      found = true;
      if (value !== null && value !== "") {
        updated.push(encodeURIComponent(key) + "=" + encodeURIComponent(value));
      }
    }

    if (!found && value !== null && value !== "") {
      updated.push(encodeURIComponent(key) + "=" + encodeURIComponent(value));
    }

    var newSearch = updated.length ? "?" + updated.join("&") : "";
    return base + newSearch + hash;
  }

  function showView(viewName) {
    var mainView = $("#addb-main-view");
    var customizeView = $("#addb-customize-view");

    if (viewName === "customize") {
      mainView.addClass("display-none");
      customizeView.removeClass("display-none");
      global.scrollTo(0, 0);
    } else {
      customizeView.addClass("display-none");
      mainView.removeClass("display-none");
      if (pendingDataTableRender && currentDataTable) {
        pendingDataTableRender = false;
        applyDataTableSettings(currentDataTable, currentSettings || DEFAULT_SETTINGS, "resume");
      }
      global.scrollTo(0, 0);
    }
  }

  function syncViewFromUrl() {
    var view = getQueryValue("view");
    showView(view === "customize" ? "customize" : "main");
  }

  function navigateTo(viewName) {
    if (!global.history || typeof global.history.pushState !== "function") {
      if (viewName === "customize") global.location.href = setQueryValue("view", "customize");
      else global.location.href = setQueryValue("view", null);
      return;
    }

    var url = viewName === "customize" ? setQueryValue("view", "customize") : setQueryValue("view", null);
    global.history.pushState({ view: viewName }, "", url);
    showView(viewName);
  }

  function setActiveCategory(category) {
    var categories = $("#addb-customize-categories .list-group-item");
    categories.removeClass("active");
    categories.filter("[data-category='" + category + "']").addClass("active");

    $(".addb-customize-category").each(function () {
      var el = $(this);
      el.toggleClass("display-none", el.attr("data-category") !== category);
    });
  }

  function readDraftFromForm() {
    var maxWidthRaw = $("#addb-max-column-width").val();
    var settings = {
      tableWidth: $("input[name='addb-table-width']:checked").val(),
      sidebarWidth: $("input[name='addb-sidebar-width']:checked").val(),
      rowDensity: $("input[name='addb-row-density']:checked").val(),
      wrapLongText: $("#addb-wrap-long-text").is(":checked"),
      maxColumnWidthPx: maxWidthRaw === "" ? null : maxWidthRaw,
      jsonMode: $("#addb-json-mode").val(),
      stickyHeader: $("#addb-sticky-header").is(":checked"),
      pageLength: $("#addb-page-length").val()
    };
    return normalizeSettings(settings);
  }

  function writeFormFromSettings(settings) {
    $("input[name='addb-table-width'][value='" + settings.tableWidth + "']").prop("checked", true);
    $("input[name='addb-sidebar-width'][value='" + settings.sidebarWidth + "']").prop("checked", true);
    $("input[name='addb-row-density'][value='" + settings.rowDensity + "']").prop("checked", true);
    $("#addb-wrap-long-text").prop("checked", !!settings.wrapLongText);
    $("#addb-max-column-width").val(settings.maxColumnWidthPx === null ? "" : settings.maxColumnWidthPx);
    $("#addb-json-mode").val(settings.jsonMode);
    $("#addb-sticky-header").prop("checked", !!settings.stickyHeader);
    $("#addb-page-length").val(String(settings.pageLength));
  }

  function initCustomizationUi() {
    $("#open-ui-customization").on("click", function (e) {
      e.preventDefault();
      navigateTo("customize");
    });

    $("#addb-customize-back").on("click", function (e) {
      e.preventDefault();
      navigateTo("main");
    });

    $("#addb-customize-categories").on("click", ".list-group-item", function (e) {
      e.preventDefault();
      setActiveCategory($(this).attr("data-category"));
    });

    $("#addb-customize-form").on("change input", "input, select", function () {
      var draft = readDraftFromForm();
      applySettings(draft, "preview");
    });

    $("#addb-ui-apply").on("click", function () {
      var draft = readDraftFromForm();
      applySettings(draft, "apply");

      var saved = saveSettings(currentSettings);
      if (saved && typeof global.showSuccessInfo === "function") {
        global.showSuccessInfo("UI settings saved");
      } else if (!saved && typeof global.showErrorInfo === "function") {
        global.showErrorInfo("Could not save UI settings");
      }
    });

    $("#addb-ui-reset").on("click", function () {
      clearSettings();
      applySettings(DEFAULT_SETTINGS, "reset");
      writeFormFromSettings(DEFAULT_SETTINGS);
      if (typeof global.showSuccessInfo === "function") {
        global.showSuccessInfo("UI settings reset to defaults");
      }
    });

    // Initial form + view state
    writeFormFromSettings(currentSettings || DEFAULT_SETTINGS);
    setActiveCategory("layout");
    syncViewFromUrl();

    $(global).on("popstate", function () {
      syncViewFromUrl();
    });
  }

  function init() {
    currentSettings = loadSettings();
    applySettings(currentSettings, "init");
    initCustomizationUi();
  }

  global.AddbUiCustomization = {
    defaults: DEFAULT_SETTINGS,
    getSettings: function () {
      return $.extend({}, currentSettings || DEFAULT_SETTINGS);
    },
    apply: function (settings) {
      applySettings(settings, "external");
    },
    save: function (settings) {
      applySettings(settings, "external");
      return saveSettings(currentSettings);
    },
    clear: function () {
      clearSettings();
      applySettings(DEFAULT_SETTINGS, "external");
    },
    onDataTableCreated: function (dataTableApi) {
      setCurrentDataTable(dataTableApi);
      applyJsonFormattingToCurrentPage(currentDataTable, currentSettings || DEFAULT_SETTINGS);
    },
    init: init
  };
})(window);
