/*
 * Run AFTER `npx cap add android` / `npx cap sync android`.
 * - adds the Unity LevelPlay SDK dependency
 * - adds ad-related manifest permissions
 * - copies NexplayAdsPlugin.java next to MainActivity and registers it
 * Safe to run multiple times.
 */
import fs from 'node:fs';
import path from 'node:path';

const LEVELPLAY_VERSION = '9.8.0';

const root = process.cwd();
const androidDir = path.join(root, 'android');
const javaRoot = path.join(androidDir, 'app', 'src', 'main', 'java');

if (!fs.existsSync(javaRoot)) {
  throw new Error('Android project not found. Run "npx cap add android" first.');
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

// ---- 1) SDK dependency (Capacitor template already uses google() + mavenCentral())
const gradleGroovy = path.join(androidDir, 'app', 'build.gradle');
const gradleKts = path.join(androidDir, 'app', 'build.gradle.kts');
const gradlePath = fs.existsSync(gradleGroovy) ? gradleGroovy : gradleKts;
if (!fs.existsSync(gradlePath)) throw new Error('Android app Gradle file not found.');

let gradle = fs.readFileSync(gradlePath, 'utf8');
const isKts = gradlePath.endsWith('.kts');
const dependency = isKts
  ? `implementation("com.unity3d.ads-mediation:mediation-sdk:${LEVELPLAY_VERSION}")`
  : `implementation 'com.unity3d.ads-mediation:mediation-sdk:${LEVELPLAY_VERSION}'`;

if (!gradle.includes('com.unity3d.ads-mediation:mediation-sdk')) {
  // Last top-level "dependencies {" of the app module.
  const idx = gradle.lastIndexOf('dependencies {');
  if (idx >= 0) {
    const at = idx + 'dependencies {'.length;
    gradle = gradle.slice(0, at) + `\n    ${dependency}` + gradle.slice(at);
  } else {
    gradle += `\n\ndependencies {\n    ${dependency}\n}\n`;
  }
  fs.writeFileSync(gradlePath, gradle);
  console.log('Added LevelPlay dependency.');
}

// ---- 2) Manifest permissions ---------------------------------------------
const manifestPath = path.join(androidDir, 'app', 'src', 'main', 'AndroidManifest.xml');
if (fs.existsSync(manifestPath)) {
  let manifest = fs.readFileSync(manifestPath, 'utf8');
  const perms = [
    'android.permission.INTERNET',
    'android.permission.ACCESS_NETWORK_STATE',
    'com.google.android.gms.permission.AD_ID'
  ];
  let changed = false;
  for (const perm of perms) {
    if (!manifest.includes(`"${perm}"`)) {
      manifest = manifest.replace(
        /<\/manifest>/,
        `    <uses-permission android:name="${perm}" />\n</manifest>`
      );
      changed = true;
    }
  }
  if (changed) fs.writeFileSync(manifestPath, manifest);
}

// ---- 3) Plugin source -----------------------------------------------------
const pluginSource = path.join(root, 'native-ads', 'android', 'NexplayAdsPlugin.java');
if (!fs.existsSync(pluginSource)) {
  throw new Error('Missing native-ads/android/NexplayAdsPlugin.java');
}
// The plugin must live in the same package as MainActivity.
const pkgMatch = fs.readFileSync(activityPath, 'utf8').match(/^\s*package\s+([\w.]+)\s*;/m);
if (!pkgMatch) throw new Error('Could not read MainActivity package name.');
let pluginCode = fs.readFileSync(pluginSource, 'utf8')
  .replace(/^\s*package\s+[\w.]+\s*;/m, `package ${pkgMatch[1]};`);
fs.writeFileSync(path.join(packageDir, 'NexplayAdsPlugin.java'), pluginCode);

// ---- 4) Register the plugin in MainActivity -------------------------------
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
