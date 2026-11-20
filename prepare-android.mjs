import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const androidDir = path.join(root, 'android');
const javaRoot = path.join(androidDir, 'app', 'src', 'main', 'java');

if (!fs.existsSync(javaRoot)) {
  throw new Error('Android project not found. Run npx cap add android first.');
}

function findFile(dir, filename) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      const found = findFile(full, filename);
      if (found) return found;
    } else if (entry.name === filename) {
      return full;
    }
  }
  return null;
}

const activityPath = findFile(javaRoot, 'MainActivity.java');
if (!activityPath) throw new Error('Capacitor MainActivity.java not found.');
const packageDir = path.dirname(activityPath);

// LevelPlay SDK is hosted on Maven Central.
const settingsGradle = path.join(androidDir, 'settings.gradle');
const settingsKts = path.join(androidDir, 'settings.gradle.kts');
const settingsPath = fs.existsSync(settingsGradle) ? settingsGradle : settingsKts;

if (fs.existsSync(settingsPath)) {
  let settings = fs.readFileSync(settingsPath, 'utf8');
  if (!settings.includes('mavenCentral()')) {
    const match = settings.match(/repositories\s*\{/);
    if (match) {
      const at = match.index + match[0].length;
      settings = settings.slice(0, at) + '\n        mavenCentral()' + settings.slice(at);
      fs.writeFileSync(settingsPath, settings);
    } else {
      console.warn('Could not automatically add mavenCentral() to settings.gradle.');
    }
  }
}

const gradleGroovy = path.join(androidDir, 'app', 'build.gradle');
const gradleKts = path.join(androidDir, 'app', 'build.gradle.kts');
const gradlePath = fs.existsSync(gradleGroovy) ? gradleGroovy : gradleKts;
if (!fs.existsSync(gradlePath)) throw new Error('Android app Gradle file not found.');

let gradle = fs.readFileSync(gradlePath, 'utf8');
const isKts = gradlePath.endsWith('.kts');
const dependency = isKts
  ? 'implementation("com.unity3d.ads-mediation:mediation-sdk:9.8.0")'
  : "implementation 'com.unity3d.ads-mediation:mediation-sdk:9.8.0'";

gradle = gradle.replace(
  /^\s*implementation\s*(?:\(?\s*)?['"]com\.unity3d\.ads:unity-ads:[^'"]+['"]\)?\s*;?\s*$/gm,
  ''
);

if (!gradle.includes('com.unity3d.ads-mediation:mediation-sdk')) {
  if (/dependencies\s*\{/.test(gradle)) {
    gradle = gradle.replace(/dependencies\s*\{/, match => `${match}\n    ${dependency}`);
  } else {
    gradle += `\n\ndependencies {\n    ${dependency}\n}\n`;
  }
  fs.writeFileSync(gradlePath, gradle);
}

const pluginSource = path.join(root, 'native-ads', 'android', 'NexplayAdsPlugin.java');
if (!fs.existsSync(pluginSource)) {
  throw new Error('Missing native-ads/android/NexplayAdsPlugin.java');
}
fs.copyFileSync(pluginSource, path.join(packageDir, 'NexplayAdsPlugin.java'));

let activity = fs.readFileSync(activityPath, 'utf8');
if (!activity.includes('NexplayAdsPlugin.class')) {
  const classPattern = /(public\s+class\s+MainActivity\s+extends\s+BridgeActivity\s*\{)/;
  if (!classPattern.test(activity)) {
    throw new Error('Could not safely find MainActivity extends BridgeActivity.');
  }
  activity = activity.replace(
    classPattern,
    `$1\n    @Override\n    public void onCreate(android.os.Bundle savedInstanceState) {\n        registerPlugin(NexplayAdsPlugin.class);\n        super.onCreate(savedInstanceState);\n    }\n`
  );
  fs.writeFileSync(activityPath, activity);
}

console.log('NEXPLAY LevelPlay plugin copied and registered.');
