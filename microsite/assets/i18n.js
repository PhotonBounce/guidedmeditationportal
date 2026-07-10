/* Power of Mind microsite i18n engine.
   Applies window.POM_STRINGS to [data-i18n] / [data-i18n-html] elements,
   drives the nav language <select>, mirrors to RTL for Hebrew/Yiddish, and
   remembers the choice in localStorage. English is the fallback for any gap. */
(function () {
  var LANGS = [
    { tag: "en", name: "English" },
    { tag: "uk", name: "Українська" },
    { tag: "es", name: "Español" },
    { tag: "fr", name: "Français" },
    { tag: "de", name: "Deutsch" },
    { tag: "ru", name: "Русский" },
    { tag: "he", name: "עברית", rtl: true },
    { tag: "yi", name: "ייִדיש", rtl: true }
  ];
  var STR = window.POM_STRINGS || { en: {} };
  var BASE = STR.en || {};

  function meta(tag) {
    for (var i = 0; i < LANGS.length; i++) if (LANGS[i].tag === tag) return LANGS[i];
    return LANGS[0];
  }

  function pick() {
    try {
      var saved = localStorage.getItem("pom_lang");
      if (saved && STR[saved]) return saved;
    } catch (e) {}
    var nav = (navigator.language || "en").slice(0, 2).toLowerCase();
    return STR[nav] ? nav : "en";
  }

  function apply(tag) {
    var dict = STR[tag] || BASE;
    document.querySelectorAll("[data-i18n]").forEach(function (el) {
      var k = el.getAttribute("data-i18n");
      var v = dict[k] != null ? dict[k] : BASE[k];
      if (v != null) el.textContent = v;
    });
    document.querySelectorAll("[data-i18n-html]").forEach(function (el) {
      var k = el.getAttribute("data-i18n-html");
      var v = dict[k] != null ? dict[k] : BASE[k];
      if (v != null) el.innerHTML = v;
    });
    var m = meta(tag);
    document.documentElement.lang = tag;
    document.documentElement.dir = m.rtl ? "rtl" : "ltr";
    try { localStorage.setItem("pom_lang", tag); } catch (e) {}
    var sel = document.getElementById("langSelect");
    if (sel) sel.value = tag;
  }

  function initSelect(cur) {
    var sel = document.getElementById("langSelect");
    if (!sel) return;
    sel.innerHTML = "";
    LANGS.forEach(function (l) {
      // Only offer languages that actually have a string table.
      if (l.tag !== "en" && !STR[l.tag]) return;
      var o = document.createElement("option");
      o.value = l.tag;
      o.textContent = l.name;
      sel.appendChild(o);
    });
    sel.value = cur;
    sel.addEventListener("change", function () { apply(sel.value); });
  }

  function boot() {
    var cur = pick();
    initSelect(cur);
    apply(cur);
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", boot);
  } else {
    boot();
  }
})();
