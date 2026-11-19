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
console.log('NEXPLAY native bridge copied and registered. Real LevelPlay SDK is not active yet.');
