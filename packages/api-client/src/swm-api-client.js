(function bindSwmApi(global) {
  const DEFAULT_BASE_URL = "http://localhost:8080";
  const API_PREFIX = "/api/v1";

  const CAMPAIGN_CATEGORIES = [
    "PRODUCTIVITY",
    "EDUCATION",
    "COMMUNITY",
    "HEALTH",
    "FINANCE",
    "DEVELOPER_TOOL",
    "ENTERTAINMENT",
    "LIFESTYLE",
    "OTHER",
  ];

  const PLATFORMS = [
    "WEB",
    "ANDROID",
    "IOS",
    "CHROME_EXTENSION",
    "DESKTOP",
    "API",
    "OTHER",
  ];

  class SwmApiError extends Error {
    constructor(message, options = {}) {
      super(message);
      this.name = "SwmApiError";
      this.status = options.status || 0;
      this.code = options.code || "CLIENT_ERROR";
      this.data = options.data || null;
    }
  }

  class SwmApiClient {
    constructor(options = {}) {
      this.baseUrl = normalizeBaseUrl(options.baseUrl || DEFAULT_BASE_URL);
      this.tokenProvider = options.tokenProvider || (() => "");
      this.fetcher = options.fetcher || global.fetch;
    }

    setBaseUrl(baseUrl) {
      this.baseUrl = normalizeBaseUrl(baseUrl || DEFAULT_BASE_URL);
    }

    async createTeam(request) {
      return this.post("/teams", request);
    }

    async joinTeam(request) {
      return this.post("/teams/join", request);
    }

    async listTeamMembers(teamId) {
      return this.get(`/teams/${encodeURIComponent(teamId)}/members`);
    }

    async updateSubService(teamId, subService, enabled) {
      return this.patch(`/teams/${encodeURIComponent(teamId)}/sub-services/${subService}`, {
        enabled,
      });
    }

    async changeMemberRole(teamId, memberId, role) {
      return this.patch(`/teams/${encodeURIComponent(teamId)}/members/${encodeURIComponent(memberId)}/role`, {
        role,
      });
    }

    async removeMember(teamId, memberId) {
      return this.delete(`/teams/${encodeURIComponent(teamId)}/members/${encodeURIComponent(memberId)}`);
    }

    async bulkPushMentoringSchedules(schedules) {
      return this.post("/calendar/mentoring-schedules:bulk-push", { schedules });
    }

    async putWhen2meetLink(url) {
      return this.put("/calendar/when2meet-link", { url });
    }

    async getUnifiedAvailability(startsAt, endsAt) {
      return this.get("/calendar/availability", { startsAt, endsAt });
    }

    async createServiceProfile(request) {
      return this.post("/match/service-profiles", request);
    }

    async listCandidateIdeas() {
      return this.get("/match/candidate-ideas");
    }

    async createCandidateIdea(request) {
      return this.post("/match/candidate-ideas", request);
    }

    async runDuplicateAnalysis(candidateIdeaId) {
      return this.post(`/match/candidate-ideas/${encodeURIComponent(candidateIdeaId)}/duplicate-analysis`);
    }

    async searchCampaigns(filters = {}) {
      return this.get("/match/campaigns", filters);
    }

    async createCampaign(request) {
      return this.post("/match/campaigns", request);
    }

    async changeCampaignStatus(campaignId, status) {
      return this.patch(`/match/campaigns/${encodeURIComponent(campaignId)}/status`, { status });
    }

    async createMatchRequest(campaignId, request) {
      return this.post(`/match/campaigns/${encodeURIComponent(campaignId)}/requests`, request);
    }

    async changeMatchRequestStatus(requestId, status) {
      return this.patch(`/match/requests/${encodeURIComponent(requestId)}/status`, { status });
    }

    async getAssignment(assignmentId) {
      return this.get(`/match/assignments/${encodeURIComponent(assignmentId)}`);
    }

    async submitFeedback(assignmentId, request) {
      return this.post(`/match/assignments/${encodeURIComponent(assignmentId)}/feedback`, request);
    }

    async getTeamTestHistory() {
      return this.get("/match/test-history");
    }

    async listNotifications() {
      return this.get("/notifications");
    }

    async get(path, query) {
      return this.request("GET", path, { query });
    }

    async post(path, body) {
      return this.request("POST", path, { body });
    }

    async put(path, body) {
      return this.request("PUT", path, { body });
    }

    async patch(path, body) {
      return this.request("PATCH", path, { body });
    }

    async delete(path) {
      return this.request("DELETE", path);
    }

    async request(method, path, options = {}) {
      if (!this.fetcher) {
        throw new SwmApiError("Fetch API is not available in this runtime.");
      }

      const url = this.buildUrl(path, options.query);
      const headers = {
        Accept: "application/json",
      };
      const token = this.tokenProvider();

      if (token) {
        headers.Authorization = `Bearer ${token}`;
      }

      const init = {
        method,
        headers,
      };

      if (options.body !== undefined && method !== "GET") {
        headers["Content-Type"] = "application/json";
        init.body = JSON.stringify(options.body);
      }

      const response = await this.fetcher(url, init);
      const envelope = await parseJsonResponse(response);

      if (!response.ok) {
        throw toApiError(envelope, response.status);
      }

      if (envelope && envelope.result === "ERROR") {
        throw toApiError(envelope, response.status);
      }

      return envelope ? envelope.data : null;
    }

    buildUrl(path, query = {}) {
      const url = new URL(`${this.baseUrl}${API_PREFIX}${path}`);

      Object.entries(query || {}).forEach(([key, value]) => {
        if (value === undefined || value === null || value === "") {
          return;
        }
        url.searchParams.set(key, String(value));
      });

      return url.toString();
    }
  }

  function normalizeBaseUrl(baseUrl) {
    return String(baseUrl || DEFAULT_BASE_URL).replace(/\/+$/, "");
  }

  async function parseJsonResponse(response) {
    const text = await response.text();

    if (!text) {
      return null;
    }

    try {
      return JSON.parse(text);
    } catch (error) {
      throw new SwmApiError("API response was not valid JSON.", {
        status: response.status,
        code: "INVALID_JSON",
        data: { responseText: text.slice(0, 500), cause: error.message },
      });
    }
  }

  function toApiError(envelope, status) {
    const error = envelope && envelope.error ? envelope.error : null;
    return new SwmApiError(error && error.message ? error.message : "API request failed.", {
      status,
      code: error && error.code ? error.code : "API_ERROR",
      data: error ? error.data : null,
    });
  }

  function toDateTimeLocalValue(date) {
    const value = date instanceof Date ? date : new Date(date);

    if (Number.isNaN(value.getTime())) {
      return "";
    }

    const offset = value.getTimezoneOffset();
    const local = new Date(value.getTime() - offset * 60 * 1000);
    return local.toISOString().slice(0, 16);
  }

  function fromDateTimeLocalValue(value) {
    if (!value) {
      return "";
    }

    return new Date(value).toISOString();
  }

  function formatDateTime(value, locale = "ko-KR") {
    if (!value) {
      return "-";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
      return String(value);
    }

    return new Intl.DateTimeFormat(locale, {
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    }).format(date);
  }

  function formatRelativeTime(value, locale = "ko-KR") {
    if (!value) {
      return "-";
    }

    const date = new Date(value);
    const diffMs = date.getTime() - Date.now();
    const absMs = Math.abs(diffMs);
    const units = [
      ["day", 24 * 60 * 60 * 1000],
      ["hour", 60 * 60 * 1000],
      ["minute", 60 * 1000],
    ];
    const formatter = new Intl.RelativeTimeFormat(locale, { numeric: "auto" });

    for (const [unit, unitMs] of units) {
      if (absMs >= unitMs) {
        return formatter.format(Math.round(diffMs / unitMs), unit);
      }
    }

    return formatter.format(0, "minute");
  }

  global.SwmApi = {
    SwmApiClient,
    SwmApiError,
    CAMPAIGN_CATEGORIES,
    PLATFORMS,
    DEFAULT_BASE_URL,
    formatDateTime,
    formatRelativeTime,
    fromDateTimeLocalValue,
    toDateTimeLocalValue,
  };
})(globalThis);
