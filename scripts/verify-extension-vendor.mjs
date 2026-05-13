import { readFileSync } from "node:fs";

const sourcePath = "packages/api-client/src/swm-api-client.js";
const vendorPath = "apps/extension/vendor/swm-api-client.js";
const manifestPath = "apps/extension/manifest.json";

const source = readFileSync(sourcePath, "utf8");
const vendor = readFileSync(vendorPath, "utf8");

if (source !== vendor) {
  console.error(`${vendorPath} is out of sync with ${sourcePath}.`);
  process.exit(1);
}

const manifest = JSON.parse(readFileSync(manifestPath, "utf8"));

if (manifest.manifest_version !== 3) {
  console.error(`${manifestPath} must use Manifest V3.`);
  process.exit(1);
}

if (!manifest.action || manifest.action.default_popup !== "popup.html") {
  console.error(`${manifestPath} must point the extension action to popup.html.`);
  process.exit(1);
}

if (!manifest.background || manifest.background.service_worker !== "src/background.js") {
  console.error(`${manifestPath} must define src/background.js as the service worker.`);
  process.exit(1);
}

const historyContentScript = (manifest.content_scripts || []).find((contentScript) => {
  return (contentScript.js || []).includes("src/history-content.js");
});

if (!historyContentScript) {
  console.error(`${manifestPath} must inject src/history-content.js on SW Maestro history pages.`);
  process.exit(1);
}

const approvedContentMatches = [
  "https://swmaestro.ai/sw/mypage/userAnswer/history.do*",
  "https://www.swmaestro.ai/sw/mypage/userAnswer/history.do*",
  "https://swmaestro.ai/busan/sw/mypage/userAnswer/history.do*",
  "https://www.swmaestro.ai/busan/sw/mypage/userAnswer/history.do*",
];

if (JSON.stringify(historyContentScript.matches || []) !== JSON.stringify(approvedContentMatches)) {
  console.error(`${manifestPath} content script matches must stay limited to SW Maestro mentoring history pages.`);
  process.exit(1);
}

if (JSON.stringify(manifest.permissions || []) !== JSON.stringify(["storage"])) {
  console.error(`${manifestPath} must keep permissions limited to storage.`);
  process.exit(1);
}

const approvedHostPermissions = [
  "http://localhost:8080/*",
];

if (JSON.stringify(manifest.host_permissions || []) !== JSON.stringify(approvedHostPermissions)) {
  console.error(`${manifestPath} host_permissions must stay limited to ${approvedHostPermissions.join(", ")}.`);
  process.exit(1);
}

const broadHostPattern = /^(<all_urls>|\*:\/\/|\w+:\/\/\*)/;

if ((manifest.host_permissions || []).some((permission) => broadHostPattern.test(permission))) {
  console.error(`${manifestPath} must not use broad host permission patterns.`);
  process.exit(1);
}
