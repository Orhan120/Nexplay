/*
 * NEXPLAY Capacitor ↔ native ads bridge.
 * The native plugin must be installed and configured before real ads can show.
 * Rewards are granted only after the native SDK reports onAdRewarded.
 */
(function () {
  const getPlugin = () => {
    const cap = window.Capacitor;
    if (!cap || !cap.isNativePlatform || !cap.isNativePlatform()) return null;
    return cap.Plugins && cap.Plugins.NexplayAds
      ? cap.Plugins.NexplayAds
      : null;
  };

  window.NEX_AD = {
    async show(callback) {
      let settled = false;
      const finish = (earned) => {
        if (settled) return;
        settled = true;
        if (typeof callback === "function") callback(earned === true);
      };

      try {
        const plugin = getPlugin();
        if (!plugin || typeof plugin.showRewarded !== "function") {
          console.warn("[NEXPLAY] Native LevelPlay plugin is not installed/configured.");
          finish(false);
          return;
        }
        const result = await plugin.showRewarded();
        finish(!!(result && result.rewarded === true));
      } catch (error) {
        console.error("[NEXPLAY] Rewarded ad failed:", error);
        finish(false);
      }
    }
  };
})();
