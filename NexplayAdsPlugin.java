package com.omstudio.nexplay;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * Safe placeholder bridge for NEXPLAY.
 * It never simulates an ad or grants a reward. Replace this guarded method
 * with the real LevelPlay SDK flow after App Key and Rewarded Ad Unit exist.
 */
@CapacitorPlugin(name = "NexplayAds")
public class NexplayAdsPlugin extends Plugin {
    @PluginMethod
    public void showRewarded(PluginCall call) {
        JSObject result = new JSObject();
        result.put("rewarded", false);
        call.resolve(result);
    }
}
