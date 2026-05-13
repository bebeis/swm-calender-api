const STORAGE_KEYS = {
  apiBaseUrl: "swm.extension.apiBaseUrl",
};

const DEFAULT_API_BASE_URL = "http://localhost:8080";
const ALLOWED_API_BASE_URLS = [DEFAULT_API_BASE_URL];

document.addEventListener("DOMContentLoaded", async () => {
  const form = document.getElementById("settings-form");
  const apiBaseUrl = document.getElementById("api-base-url");
  const statusLine = document.getElementById("status-line");

  const values = await chrome.storage.local.get([STORAGE_KEYS.apiBaseUrl]);
  apiBaseUrl.value = values[STORAGE_KEYS.apiBaseUrl] || DEFAULT_API_BASE_URL;
  statusLine.textContent = `현재 API origin: ${apiBaseUrl.value}`;

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const nextApiBaseUrl = normalizeApiBaseUrl(apiBaseUrl.value);

    await chrome.storage.local.set({
      [STORAGE_KEYS.apiBaseUrl]: nextApiBaseUrl,
    });
    apiBaseUrl.value = nextApiBaseUrl;
    statusLine.textContent = `저장됨: ${nextApiBaseUrl}`;
  });
});

function normalizeApiBaseUrl(value) {
  const candidate = String(value || DEFAULT_API_BASE_URL).trim().replace(/\/+$/, "");
  return ALLOWED_API_BASE_URLS.includes(candidate) ? candidate : DEFAULT_API_BASE_URL;
}
