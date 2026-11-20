/*
 * NEXPLAY JavaScript bridge for the native Capacitor LevelPlay plugin.
 * The app grants in-game rewards only when the native SDK returns rewarded: true.
 */
(function () {
  "use strict";

  function getPlugin() {
    const cap = window.Capacitor;
    if (!cap || typeof cap.isNativePlatform !== "function" || !cap.isNativePlatform()) {
      return null;
    }
    return cap.Plugins && cap.Plugins.NexplayAds
      ? cap.Plugins.NexplayAds
      : null;
  }

  window.NEX_AD = {
    show(callback) {
      let finished = false;
      const finish = (earned) => {
        if (finished) return;
        finished = true;
        if (typeof callback === "function") callback(earned === true);
      };

      const plugin = getPlugin();
      if (!plugin || typeof plugin.showRewarded !== "function") {
        console.warn("[NEXPLAY] Native LevelPlay plugin is unavailable.");
        finish(false);
        return;
      }

      plugin.showRewarded()
        .then(result => finish(!!(result && result.rewarded === true)))
        .catch(error => {
          console.error("[NEXPLAY] LevelPlay rewarded ad error:", error);
          finish(false);
        });
    }
  };
})();
