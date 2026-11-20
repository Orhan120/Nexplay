/*
 * NEXPLAY JavaScript bridge for the native Capacitor "NexplayAds" plugin
 * (Unity LevelPlay rewarded ads).
 * The game grants rewards only when the native SDK returns rewarded: true.
 */
(function () {
  "use strict";

  const JS_TIMEOUT_MS = 180000; // safety net: never leave the game waiting forever

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
      const finish = (earned) => {
        if (finished) return;
        finished = true;
        clearTimeout(timer);
        if (typeof callback === "function") callback(earned === true);
      };

      const plugin = getPlugin();
      if (!plugin || typeof plugin.showRewarded !== "function") {
        console.warn("[NEXPLAY] Native ads plugin is unavailable (not running in the Android app?).");
        finish(false);
        return;
      }

      timer = setTimeout(() => finish(false), JS_TIMEOUT_MS);
      plugin.showRewarded()
        .then(result => finish(!!(result && result.rewarded === true)))
        .catch(error => {
          console.error("[NEXPLAY] Rewarded ad error:", error);
          finish(false);
        });
    }
  };
})();
