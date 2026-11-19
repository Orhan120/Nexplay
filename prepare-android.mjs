import fs from 'node:fs';
import path from 'node:path';

const projectRoot = process.cwd();
const javaRoot = path.join(projectRoot, 'android', 'app', 'src', 'main', 'java');
if (!fs.existsSync(javaRoot)) {
  throw new Error('Android project not found. Run `npx cap add android` first.');
}

function findFile(dir, filename) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      const found = findFile(full, filename);
      if (found) return found;
    } else if (entry.name === filename) return full;
  }
  return null;
}

const activityPath = findFile(javaRoot, 'MainActivity.java');
if (!activityPath) throw new Error('Capacitor MainActivity.java not found.');

const packageDir = path.dirname(activityPath);

// Add the official Unity Ads Android SDK to the generated app module.
const appGradle = path.join(projectRoot, 'android', 'app', 'build.gradle');
const appGradleKts = path.join(projectRoot, 'android', 'app', 'build.gradle.kts');
const gradlePath = fs.existsSync(appGradle) ? appGradle : appGradleKts;
if (!fs.existsSync(gradlePath)) throw new Error('Android app Gradle file not found.');
let gradle = fs.readFileSync(gradlePath, 'utf8');
const unityDependency = gradlePath.endsWith('.kts')
  ? 'implementation("com.unity3d.ads:unity-ads:4.12.5")'
  : "implementation 'com.unity3d.ads:unity-ads:4.12.5'";
if (!gradle.includes('com.unity3d.ads:unity-ads')) {
  if (/dependencies\s*\{/.test(gradle)) {
    gradle = gradle.replace(/dependencies\s*\{/, match => `${match}\n    ${unityDependency}`);
  } else {
    gradle += `\n\ndependencies {\n    ${unityDependency}\n}\n`;
  }
  fs.writeFileSync(gradlePath, gradle);
}

const pluginSource = path.join(projectRoot, 'native-ads', 'android', 'NexplayAdsPlugin.java');
fs.copyFileSync(pluginSource, path.join(packageDir, 'NexplayAdsPlugin.java'));

let activity = fs.readFileSync(activityPath, 'utf8');
if (!activity.includes('NexplayAdsPlugin.class')) {
  const patched = activity.replace(
    /public class MainActivity extends BridgeActivity\s*\{\s*\}/,
    'public class MainActivity extends BridgeActivity {\n    @Override\n    public void onCreate(android.os.Bundle savedInstanceState) {\n        registerPlugin(NexplayAdsPlugin.class);\n        super.onCreate(savedInstanceState);\n    }\n}'
  );
  if (!patched.includes('NexplayAdsPlugin.class')) {
    throw new Error('Could not safely register NexplayAdsPlugin in MainActivity.java.');
  }
  fs.writeFileSync(activityPath, patched);
}
console.log('NEXPLAY native bridge copied and registered with Unity Ads SDK.');
