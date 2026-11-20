/* NEXPLAY JS bridge -> native Capacitor "NexplayAds" plugin (Start.io rewarded video). Reward only when native returns rewarded:true. */
(function () {
  "use strict";

  const JS_TIMEOUT_MS = 180000;

  function getPlugin() {
    const cap = window.Capacitor;
    if (!cap || typeof cap.isNativePlatform !== "function" || !cap.isNativePlatform()) {
      return null;
    }
    if (cap.Plugins && cap.Plugins.NexplayAds) return cap.Plugins.NexplayAds;
    if (typeof cap.registerPlugin === "function") return cap.registerPlugin("NexplayAds");
    return null;
  }

  window.NEX_AD = {
    show(callback) {
      let finished = false;
      let timer = null;
      const finish = (earned, reason) => {
        if (finished) return;
        finished = true;
        clearTimeout(timer);
        if (typeof callback === "function") callback(earned === true);
        if (earned !== true && reason && typeof window.toast === "function") {
          setTimeout(() => window.toast("Reklam hatası: " + reason), 2500);
        }
      };

      const plugin = getPlugin();
      if (!plugin || typeof plugin.showRewarded !== "function") {
        console.warn("[NEXPLAY] Native ads plugin is unavailable.");
        finish(false, "eklenti yok");
        return;
      }

      timer = setTimeout(() => finish(false, "JS zaman asimi"), JS_TIMEOUT_MS);
      plugin.showRewarded()
        .then(result => finish(!!(result && result.rewarded === true), result && result.reason))
        .catch(error => {
          console.error("[NEXPLAY] Rewarded ad error:", error);
          finish(false, String((error && error.message) || error));
        });
    }
  };
})();
