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
    rowDensity: "normal", // compact | normal | comfortable
    wrapLongText: false,
    maxColumnWidthPx: null, // number | null
    jsonMode: "raw", // raw | wrapped | pretty
    stickyHeader: false,
    pageLength: 10 // 10 | 25 | 50 | 100
  };

  var allowedTableWidths = { fixed: true, full: true, fluid: true };
  var allowedRowDensities = { compact: true, normal: true, comfortable: true };
  var allowedJsonModes = { raw: true, wrapped: true, pretty: true };
  var allowedPageLengths = { 10: true, 25: true, 50: true, 100: true };

  var currentSettings = null;
  var currentDataTable = null;
  var currentDataTableDrawHandler = null;
  var pendingDataTableRender = false;
  var jsonCleanupNeeded = false;

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

  function applyBodyClasses(settings) {
    var body = $("body");

    body.removeClass("addb-density-compact addb-density-normal addb-density-comfortable");
    body.addClass("addb-density-" + settings.rowDensity);

    body.toggleClass("addb-wrap-long-text", !!settings.wrapLongText);

    body.removeClass("addb-json-mode-raw addb-json-mode-wrapped addb-json-mode-pretty");
    body.addClass("addb-json-mode-" + settings.jsonMode);

    body.toggleClass("addb-sticky-header", !!settings.stickyHeader);
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

  function cleanupJsonClassesForCurrentPage(dataTableApi) {
    if (!dataTableApi) return;

    var rows = dataTableApi.rows({ page: "current" }).nodes();
    for (var rowIndex = 0; rowIndex < rows.length; rowIndex++) {
      var cells = rows[rowIndex].cells;
      for (var cellIndex = 0; cellIndex < cells.length; cellIndex++) {
        var td = cells[cellIndex];
        td.classList.remove("addb-json-cell");
      }
    }
  }

  function applyJsonFormattingToCurrentPage(dataTableApi, settings) {
    if (!dataTableApi) return;

    if (settings.jsonMode === "raw") {
      if (jsonCleanupNeeded) {
        cleanupJsonClassesForCurrentPage(dataTableApi);
        jsonCleanupNeeded = false;
      }
      return;
    }

    jsonCleanupNeeded = false;

    var rows = dataTableApi.rows({ page: "current" }).nodes();
    for (var rowIndex = 0; rowIndex < rows.length; rowIndex++) {
      var cells = rows[rowIndex].cells;
      for (var cellIndex = 0; cellIndex < cells.length; cellIndex++) {
        var td = cells[cellIndex];
        td.classList.remove("addb-json-cell");

        var cellData = dataTableApi.cell(td).data();
        if (!looksLikeJson(cellData)) continue;

        var parsed = safeParseJson(cellData);
        if (parsed === null) continue;

        td.classList.add("addb-json-cell");

        if (settings.jsonMode === "pretty") {
          td.textContent = JSON.stringify(parsed, null, 2);
        }
      }
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
    applyBodyClasses(currentSettings);
    applyMaxColumnWidth(currentSettings);

    if (previousSettings.jsonMode !== currentSettings.jsonMode && currentSettings.jsonMode === "raw") {
      jsonCleanupNeeded = true;
    }

    var dataTableReason = reason;
    if (previousSettings.jsonMode === "pretty" && currentSettings.jsonMode !== "pretty") {
      dataTableReason = "jsonMode";
    }

    applyDataTableSettings(currentDataTable, currentSettings, dataTableReason);
  }

  function setCurrentDataTable(dataTableApi) {
    var previousTableNode = currentDataTable ? $(currentDataTable.table().node()) : null;
    if (previousTableNode && currentDataTableDrawHandler) {
      previousTableNode.off("draw.dt", currentDataTableDrawHandler);
    }

    currentDataTable = dataTableApi || null;
    if (!currentDataTable) return;

    var tableNode = $(currentDataTable.table().node());
    currentDataTableDrawHandler = function () {
      applyJsonFormattingToCurrentPage(currentDataTable, currentSettings || DEFAULT_SETTINGS);
    };
    tableNode.on("draw.dt", currentDataTableDrawHandler);

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
