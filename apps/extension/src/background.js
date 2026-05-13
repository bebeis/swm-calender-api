importScripts("../vendor/swm-api-client.js");

chrome.runtime.onMessage.addListener((message, _sender, sendResponse) => {
  if (!message || message.type !== "SWM_TEAMS_API_REQUEST") {
    return false;
  }

  handleApiRequest(message.payload)
    .then((data) => sendResponse({ ok: true, data }))
    .catch((error) => {
      sendResponse({
        ok: false,
        error: {
          message: error && error.message ? error.message : "API request failed.",
          code: error && error.code ? error.code : "API_ERROR",
          status: error && error.status ? error.status : 0,
        },
      });
    });

  return true;
});

async function handleApiRequest(payload) {
  const client = new self.SwmApi.SwmApiClient({
    baseUrl: payload.baseUrl,
    tokenProvider: () => payload.token,
    fetcher: fetch.bind(self),
  });

  switch (payload.operation) {
    case "bulkPushMentoringSchedules":
      return client.bulkPushMentoringSchedules(payload.schedules || []);
    case "putWhen2meetLink":
      return client.putWhen2meetLink(payload.url);
    case "getUnifiedAvailability":
      return client.getUnifiedAvailability(payload.startsAt, payload.endsAt);
    default:
      throw new self.SwmApi.SwmApiError("Unsupported extension API operation.", {
        code: "UNSUPPORTED_OPERATION",
      });
  }
}
