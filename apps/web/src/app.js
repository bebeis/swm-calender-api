class SwmTeamsWebApp {
  constructor(documentRef, api) {
    this.document = documentRef;
    this.apiTools = api;
    this.storageKeys = {
      apiBaseUrl: "swm.web.apiBaseUrl",
      team: "swm.web.teamSummary",
      campaignFilters: "swm.web.campaignFilters",
    };
    this.state = {
      activeView: "dashboard",
      accessToken: "",
      apiBaseUrl: localStorage.getItem(this.storageKeys.apiBaseUrl) || api.DEFAULT_BASE_URL,
      team: this.loadJson(this.storageKeys.team),
      notifications: [],
      campaigns: [],
      candidateIdeas: [],
      history: [],
      lastRequest: null,
      lastCampaign: null,
      lastAssignment: null,
    };
    this.client = new api.SwmApiClient({
      baseUrl: this.state.apiBaseUrl,
      tokenProvider: () => this.state.accessToken,
    });
  }

  init() {
    this.cacheElements();
    this.populateEnums();
    this.setDateDefaults();
    this.bindEvents();
    this.elements.apiBaseUrl.value = this.state.apiBaseUrl;
    this.renderAll();
  }

  cacheElements() {
    this.elements = {
      pageTitle: this.document.getElementById("page-title"),
      apiBaseUrl: this.document.getElementById("api-base-url"),
      accessToken: this.document.getElementById("access-token"),
      authStatus: this.document.getElementById("auth-status"),
      teamStatus: this.document.getElementById("team-status"),
      activityMessage: this.document.getElementById("activity-message"),
      teamSummary: this.document.getElementById("team-summary"),
      notificationList: this.document.getElementById("notification-list"),
      campaignPreview: this.document.getElementById("campaign-preview"),
      availabilityPreview: this.document.getElementById("availability-preview"),
      membersTable: this.document.getElementById("members-table"),
      when2meetResult: this.document.getElementById("when2meet-result"),
      scheduleResult: this.document.getElementById("schedule-result"),
      availabilityGrid: this.document.getElementById("availability-grid"),
      profileResult: this.document.getElementById("profile-result"),
      campaignResult: this.document.getElementById("campaign-result"),
      campaignList: this.document.getElementById("campaign-list"),
      candidateIdeaList: this.document.getElementById("candidate-idea-list"),
      duplicateAnalysisResult: this.document.getElementById("duplicate-analysis-result"),
      requestResult: this.document.getElementById("request-result"),
      requestStatusResult: this.document.getElementById("request-status-result"),
      assignmentResult: this.document.getElementById("assignment-result"),
      feedbackResult: this.document.getElementById("feedback-result"),
      historyList: this.document.getElementById("history-list"),
      calendarToggle: this.document.getElementById("calendar-toggle"),
      matchToggle: this.document.getElementById("match-toggle"),
    };
  }

  populateEnums() {
    this.document.querySelectorAll("select[data-options='categories']").forEach((select) => {
      this.populateSelect(select, this.apiTools.CAMPAIGN_CATEGORIES);
    });
    this.document.querySelectorAll("select[data-options='platforms']").forEach((select) => {
      this.populateSelect(select, this.apiTools.PLATFORMS);
    });
  }

  populateSelect(select, options) {
    const emptyLabel = select.dataset.empty;
    select.replaceChildren();

    if (emptyLabel) {
      select.appendChild(new Option(emptyLabel, ""));
    }

    options.forEach((option) => {
      select.appendChild(new Option(this.humanizeEnum(option), option));
    });
  }

  setDateDefaults() {
    const now = new Date();
    const oneHourLater = new Date(now.getTime() + 60 * 60 * 1000);
    const oneWeekLater = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);
    const twoWeeksLater = new Date(now.getTime() + 14 * 24 * 60 * 60 * 1000);

    this.setInputValue("schedule-form", "startsAt", this.apiTools.toDateTimeLocalValue(now));
    this.setInputValue("schedule-form", "endsAt", this.apiTools.toDateTimeLocalValue(oneHourLater));
    this.setInputValue("availability-form", "startsAt", this.apiTools.toDateTimeLocalValue(now));
    this.setInputValue("availability-form", "endsAt", this.apiTools.toDateTimeLocalValue(oneWeekLater));
    this.setInputValue("campaign-form", "deadline", this.apiTools.toDateTimeLocalValue(twoWeeksLater));
  }

  bindEvents() {
    this.document.body.addEventListener("click", (event) => this.handleClick(event));
    this.document.body.addEventListener("change", (event) => this.handleChange(event));
    this.document.body.addEventListener("input", (event) => this.handleInput(event));

    const submitHandlers = {
      "connection-form": (form) => this.refreshWorkspace(form),
      "create-team-form": (form) => this.createTeam(form),
      "join-team-form": (form) => this.joinTeam(form),
      "when2meet-form": (form) => this.putWhen2meetLink(form),
      "schedule-form": (form) => this.bulkPushSchedule(form),
      "availability-form": (form) => this.loadAvailabilityFromForm(form),
      "service-profile-form": (form) => this.createServiceProfile(form),
      "campaign-form": (form) => this.createCampaign(form),
      "campaign-search-form": (form) => this.searchCampaigns(form),
      "candidate-idea-form": (form) => this.createCandidateIdea(form),
      "match-request-form": (form) => this.createMatchRequest(form),
      "request-status-form": (form) => this.changeMatchRequestStatus(form),
      "assignment-form": (form) => this.getAssignment(form),
      "feedback-form": (form) => this.submitFeedback(form),
    };

    Object.entries(submitHandlers).forEach(([formId, handler]) => {
      const form = this.document.getElementById(formId);
      form.addEventListener("submit", (event) => {
        event.preventDefault();
        this.withBusy(event.submitter, () => handler(form));
      });
    });
  }

  handleClick(event) {
    const viewButton = event.target.closest("[data-view]");
    if (viewButton) {
      this.switchView(viewButton.dataset.view);
      return;
    }

    const actionButton = event.target.closest("[data-action]");
    if (!actionButton) {
      return;
    }

    const actions = {
      "load-notifications": () => this.loadNotifications(),
      "load-default-availability": () => this.loadDefaultAvailability(),
      "load-members": () => this.loadMembers(),
      "load-ideas": () => this.loadCandidateIdeas(),
      "load-history": () => this.loadHistory(),
      "run-analysis": () => this.runDuplicateAnalysis(actionButton.dataset.ideaId),
      "fill-campaign-request": () => this.fillCampaignRequest(actionButton.dataset.campaignId),
      "change-member-role": () => this.changeMemberRole(actionButton.dataset.memberId),
      "remove-member": () => this.removeMember(actionButton.dataset.memberId),
    };

    if (actions[actionButton.dataset.action]) {
      this.withBusy(actionButton, actions[actionButton.dataset.action]);
    }
  }

  handleChange(event) {
    if (event.target === this.elements.apiBaseUrl) {
      this.updateBaseUrl(event.target.value);
      return;
    }

    if (event.target.matches("[data-action='toggle-service']")) {
      this.withBusy(event.target, () => this.toggleSubService(event.target.dataset.service, event.target.checked));
    }
  }

  handleInput(event) {
    if (event.target === this.elements.accessToken) {
      this.state.accessToken = event.target.value.trim();
      this.renderStatus();
      return;
    }

    if (event.target === this.elements.apiBaseUrl) {
      this.updateBaseUrl(event.target.value);
    }
  }

  async refreshWorkspace() {
    this.updateBaseUrl(this.elements.apiBaseUrl.value);
    this.state.accessToken = this.elements.accessToken.value.trim();
    this.renderStatus();

    if (!this.state.accessToken) {
      this.reportWarning("Bearer token이 없어 보호 API를 호출하지 않았습니다.");
      return;
    }

    await Promise.allSettled([
      this.loadNotifications(),
      this.searchCampaigns(),
      this.loadHistory({ silent: true }),
    ]);
    this.reportSuccess("워크스페이스 데이터를 새로고침했습니다.");
  }

  async createTeam(form) {
    const values = this.readForm(form);
    const team = await this.client.createTeam({
      name: values.name,
      description: values.description || null,
    });
    this.setTeam(team);
    form.reset();
    this.reportSuccess("팀을 생성했습니다.");
  }

  async joinTeam(form) {
    const values = this.readForm(form);
    const team = await this.client.joinTeam({ inviteCode: values.inviteCode });
    this.setTeam(team);
    form.reset();
    this.reportSuccess("팀에 합류했습니다.");
  }

  async toggleSubService(subService, enabled) {
    const teamId = this.requireTeamId();
    const activation = await this.client.updateSubService(teamId, subService, enabled);
    this.state.team = {
      ...this.state.team,
      calendarEnabled: activation.calendarEnabled,
      matchEnabled: activation.matchEnabled,
    };
    this.persistJson(this.storageKeys.team, this.state.team);
    this.renderStatus();
    this.renderTeamSummary();
    this.reportSuccess(`${subService} 상태를 변경했습니다.`);
  }

  async loadMembers() {
    const teamId = this.requireTeamId();
    const response = await this.client.listTeamMembers(teamId);
    this.renderMembers(response.items || []);
    this.reportSuccess("멤버 목록을 조회했습니다.");
  }

  async changeMemberRole(memberId) {
    const teamId = this.requireTeamId();
    const select = this.document.querySelector(`[data-member-role="${memberId}"]`);
    const member = await this.client.changeMemberRole(teamId, memberId, select.value);
    this.reportSuccess(`${member.name} 역할을 변경했습니다.`);
    await this.loadMembers();
  }

  async removeMember(memberId) {
    const teamId = this.requireTeamId();
    const member = await this.client.removeMember(teamId, memberId);
    this.reportSuccess(`${member.name} 멤버를 제거했습니다.`);
    await this.loadMembers();
  }

  async putWhen2meetLink(form) {
    const values = this.readForm(form);
    const response = await this.client.putWhen2meetLink(values.url);
    this.elements.when2meetResult.textContent = `${response.status} · ${response.url}${response.failureReason ? `\n${response.failureReason}` : ""}`;
    this.reportSuccess("When2meet 링크를 등록했습니다.");
  }

  async bulkPushSchedule(form) {
    const values = this.readForm(form);
    const schedule = {
      externalSourceId: values.externalSourceId,
      title: values.title,
      startsAt: this.apiTools.fromDateTimeLocalValue(values.startsAt),
      endsAt: this.apiTools.fromDateTimeLocalValue(values.endsAt),
      location: values.location || null,
      description: values.description || null,
    };
    const response = await this.client.bulkPushMentoringSchedules([schedule]);
    this.elements.scheduleResult.textContent = `생성 ${response.createdCount}건 · 중복 스킵 ${response.skippedDuplicateCount}건`;
    this.reportSuccess("멘토링 일정을 push했습니다.");
  }

  async loadAvailabilityFromForm(form) {
    const values = this.readForm(form);
    const startsAt = this.apiTools.fromDateTimeLocalValue(values.startsAt);
    const endsAt = this.apiTools.fromDateTimeLocalValue(values.endsAt);
    const response = await this.client.getUnifiedAvailability(startsAt, endsAt);
    this.renderAvailability(response, this.elements.availabilityGrid);
    this.renderAvailability(response, this.elements.availabilityPreview, 8);
    this.reportSuccess("통합 가용성을 조회했습니다.");
  }

  async loadDefaultAvailability() {
    const startsAt = new Date().toISOString();
    const endsAt = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString();
    const response = await this.client.getUnifiedAvailability(startsAt, endsAt);
    this.renderAvailability(response, this.elements.availabilityPreview, 8);
    this.reportSuccess("이번 주 가용성을 조회했습니다.");
  }

  async createServiceProfile(form) {
    const values = this.readForm(form);
    const response = await this.client.createServiceProfile({
      name: values.name,
      summary: values.summary,
      description: values.description,
      category: values.category,
      platforms: this.readSelectedValues(form, "platforms"),
      screenshotUrls: [],
      demoUrl: values.demoUrl || null,
      public: form.elements.public.checked,
    });
    this.elements.profileResult.textContent = `${response.name} · ${response.category} · ${response.platforms.join(", ")}`;
    this.reportSuccess("서비스 프로필을 저장했습니다.");
  }

  async createCampaign(form) {
    const values = this.readForm(form);
    const response = await this.client.createCampaign({
      title: values.title,
      description: values.description,
      targetTeamCount: Number(values.targetTeamCount),
      deadline: this.apiTools.fromDateTimeLocalValue(values.deadline),
      reciprocalAvailable: form.elements.reciprocalAvailable.checked,
      requirements: values.requirements || null,
    });
    this.state.lastCampaign = response;
    this.elements.campaignResult.textContent = `#${response.campaignId} ${response.title} · ${response.status}`;
    this.reportSuccess("캠페인을 생성했습니다.");
  }

  async searchCampaigns(form) {
    const values = form ? this.readForm(form) : this.loadJson(this.storageKeys.campaignFilters) || {};
    const filters = {
      category: values.category || undefined,
      platform: values.platform || undefined,
      reciprocalAvailable: values.reciprocalAvailable || undefined,
      sort: values.sort || "latest",
    };

    this.persistJson(this.storageKeys.campaignFilters, filters);

    const response = await this.client.searchCampaigns(filters);
    this.state.campaigns = response.items || [];
    this.renderCampaigns(this.state.campaigns, this.elements.campaignList);
    this.renderCampaigns(this.state.campaigns.slice(0, 4), this.elements.campaignPreview);
    this.reportSuccess("캠페인을 조회했습니다.");
  }

  async createCandidateIdea(form) {
    const values = this.readForm(form);
    const idea = await this.client.createCandidateIdea({
      title: values.title,
      summary: values.summary,
      problem: values.problem,
      targetUsers: values.targetUsers,
      solution: values.solution,
      category: values.category,
      platforms: this.readSelectedValues(form, "platforms"),
    });
    form.reset();
    this.reportSuccess(`${idea.title} 후보 아이디어를 저장했습니다.`);
    await this.loadCandidateIdeas();
  }

  async loadCandidateIdeas() {
    const response = await this.client.listCandidateIdeas();
    this.state.candidateIdeas = response.items || [];
    this.renderCandidateIdeas();
    this.reportSuccess("후보 아이디어 목록을 조회했습니다.");
  }

  async runDuplicateAnalysis(candidateIdeaId) {
    const analysis = await this.client.runDuplicateAnalysis(candidateIdeaId);
    this.renderDuplicateAnalysis(analysis);
    this.reportSuccess("중복 분석을 실행했습니다.");
  }

  async loadNotifications() {
    const response = await this.client.listNotifications();
    this.state.notifications = response.items || [];
    this.renderNotifications();
    this.reportSuccess("알림을 조회했습니다.");
  }

  async createMatchRequest(form) {
    const values = this.readForm(form);
    const request = await this.client.createMatchRequest(values.campaignId, {
      type: values.type,
      message: values.message || null,
    });
    this.state.lastRequest = request;
    this.elements.requestResult.textContent = `요청 #${request.requestId} · ${request.type} · ${request.status}`;
    this.reportSuccess("베타 요청을 생성했습니다.");
  }

  async changeMatchRequestStatus(form) {
    const values = this.readForm(form);
    const response = await this.client.changeMatchRequestStatus(values.requestId, values.status);
    this.elements.requestStatusResult.textContent = `요청 #${response.request.requestId} · ${response.request.status}\n할당 생성: ${response.assignmentCreated ? response.assignmentId : "없음"}`;
    this.reportSuccess("요청 상태를 변경했습니다.");
  }

  async getAssignment(form) {
    const values = this.readForm(form);
    const response = await this.client.getAssignment(values.assignmentId);
    this.state.lastAssignment = response;
    this.elements.assignmentResult.textContent = this.formatObject({
      assignmentId: response.assignmentId,
      requestId: response.requestId,
      testerTeamId: response.testerTeamId,
      targetTeamId: response.targetTeamId,
      status: response.status,
      feedback: response.feedback ? response.feedback.summary : null,
    });
    this.reportSuccess("할당 상세를 조회했습니다.");
  }

  async submitFeedback(form) {
    const values = this.readForm(form);
    const response = await this.client.submitFeedback(values.assignmentId, {
      scores: {
        usability: Number(values.usability),
        value: Number(values.value),
        reliability: Number(values.reliability),
        recommendation: Number(values.recommendation),
      },
      summary: values.summary,
      improvementSuggestion: values.improvementSuggestion || null,
    });
    this.elements.feedbackResult.textContent = `피드백 #${response.feedbackId} · ${this.apiTools.formatDateTime(response.submittedAt)}`;
    this.reportSuccess("피드백을 제출했습니다.");
    await this.loadHistory({ silent: true });
  }

  async loadHistory(options = {}) {
    const response = await this.client.getTeamTestHistory();
    this.state.history = response.items || [];
    this.renderHistory();

    if (!options.silent) {
      this.reportSuccess("테스트 이력을 조회했습니다.");
    }
  }

  fillCampaignRequest(campaignId) {
    const form = this.document.getElementById("match-request-form");
    form.elements.campaignId.value = campaignId;
    this.switchView("requests");
    this.reportSuccess(`#${campaignId} 캠페인을 요청 폼에 채웠습니다.`);
  }

  setTeam(team) {
    this.state.team = team;
    this.persistJson(this.storageKeys.team, team);
    this.renderStatus();
    this.renderTeamSummary();
  }

  switchView(viewName) {
    this.state.activeView = viewName;
    this.document.querySelectorAll(".view").forEach((view) => {
      view.classList.toggle("active", view.id === `view-${viewName}`);
    });
    this.document.querySelectorAll(".nav-item").forEach((button) => {
      button.classList.toggle("active", button.dataset.view === viewName);
    });
    this.elements.pageTitle.textContent = {
      dashboard: "대시보드",
      team: "팀",
      calendar: "Calendar",
      match: "Match",
      requests: "요청",
      feedback: "피드백",
    }[viewName] || "대시보드";
  }

  updateBaseUrl(baseUrl) {
    const normalized = String(baseUrl || this.apiTools.DEFAULT_BASE_URL).replace(/\/+$/, "");
    this.state.apiBaseUrl = normalized;
    this.client.setBaseUrl(normalized);
    localStorage.setItem(this.storageKeys.apiBaseUrl, normalized);
    this.elements.apiBaseUrl.value = normalized;
  }

  renderAll() {
    this.renderStatus();
    this.renderTeamSummary();
    this.renderNotifications();
    this.renderCampaigns(this.state.campaigns, this.elements.campaignList);
    this.renderCampaigns(this.state.campaigns, this.elements.campaignPreview);
    this.renderCandidateIdeas();
    this.renderHistory();
    this.renderMembers([]);
    this.renderAvailability({ slots: [] }, this.elements.availabilityGrid);
    this.renderAvailability({ slots: [] }, this.elements.availabilityPreview);
  }

  renderStatus() {
    this.elements.authStatus.textContent = this.state.accessToken ? "토큰 연결됨" : "토큰 없음";
    this.elements.authStatus.className = `status-pill ${this.state.accessToken ? "ready" : "warning"}`;
    this.elements.teamStatus.textContent = this.state.team ? `${this.state.team.name} · #${this.state.team.teamId}` : "현재 팀 없음";
    this.elements.calendarToggle.checked = Boolean(this.state.team && this.state.team.calendarEnabled);
    this.elements.matchToggle.checked = Boolean(this.state.team && this.state.team.matchEnabled);
  }

  renderTeamSummary() {
    const team = this.state.team;

    if (!team) {
      this.elements.teamSummary.innerHTML = this.emptyState("팀을 만들거나 초대 코드로 합류하면 현재 작업 팀이 설정됩니다.");
      return;
    }

    this.elements.teamSummary.innerHTML = [
      this.summaryRow("팀", `${this.escape(team.name)} (#${team.teamId})`),
      this.summaryRow("초대 코드", `<code>${this.escape(team.inviteCode)}</code>`),
      this.summaryRow("Calendar", this.badge(team.calendarEnabled ? "활성" : "비활성", team.calendarEnabled ? "success" : "")),
      this.summaryRow("Match", this.badge(team.matchEnabled ? "활성" : "비활성", team.matchEnabled ? "success" : "")),
    ].join("");
  }

  renderMembers(members) {
    if (!members.length) {
      this.elements.membersTable.innerHTML = this.emptyState("멤버 조회를 실행하면 목록이 표시됩니다.");
      return;
    }

    const rows = members.map((member) => `
      <tr>
        <td><strong>${this.escape(member.name)}</strong><div class="row-meta">${this.escape(member.email)}</div></td>
        <td>#${member.memberId}</td>
        <td>
          <select data-member-role="${member.memberId}">
            <option value="OWNER" ${member.role === "OWNER" ? "selected" : ""}>OWNER</option>
            <option value="MEMBER" ${member.role === "MEMBER" ? "selected" : ""}>MEMBER</option>
          </select>
        </td>
        <td>
          <div class="inline-actions">
            <button class="button secondary" type="button" data-action="change-member-role" data-member-id="${member.memberId}">변경</button>
            <button class="button tertiary" type="button" data-action="remove-member" data-member-id="${member.memberId}">제거</button>
          </div>
        </td>
      </tr>
    `);

    this.elements.membersTable.innerHTML = `
      <table>
        <thead><tr><th>멤버</th><th>ID</th><th>역할</th><th>작업</th></tr></thead>
        <tbody>${rows.join("")}</tbody>
      </table>
    `;
  }

  renderNotifications() {
    const notifications = this.state.notifications;

    if (!notifications.length) {
      this.elements.notificationList.innerHTML = this.emptyState("최근 알림이 없습니다.");
      return;
    }

    this.elements.notificationList.innerHTML = notifications.slice(0, 8).map((notification) => `
      <article class="list-row">
        <div class="row-top">
          <span class="row-title">${this.escape(notification.type)}</span>
          ${this.badge(notification.read ? "읽음" : "미확인", notification.read ? "" : "warning")}
        </div>
        <div>${this.escape(notification.message)}</div>
        <div class="row-meta">${this.apiTools.formatRelativeTime(notification.createdAt)}</div>
      </article>
    `).join("");
  }

  renderCampaigns(campaigns, container) {
    if (!campaigns.length) {
      container.innerHTML = this.emptyState("조건에 맞는 공개 캠페인이 없습니다.");
      return;
    }

    container.innerHTML = campaigns.map((campaign) => `
      <article class="campaign-card">
        <div class="row-top">
          <h4>${this.escape(campaign.serviceName || campaign.title || "캠페인")}</h4>
          ${this.badge(campaign.status || "OPEN", this.statusTone(campaign.status))}
        </div>
        <p class="muted">${this.escape(campaign.serviceSummary || "")}</p>
        <div class="badge-row">
          ${this.badge(this.humanizeEnum(campaign.category || "OTHER"), "info")}
          ${(campaign.platforms || []).map((platform) => this.badge(platform)).join("")}
          ${campaign.reciprocalAvailable ? this.badge("맞베타", "success") : ""}
        </div>
        <div class="row-meta">마감 ${this.apiTools.formatDateTime(campaign.deadline)} · 팀 #${campaign.teamId}</div>
        <button class="button secondary" type="button" data-action="fill-campaign-request" data-campaign-id="${campaign.campaignId}">요청 준비</button>
      </article>
    `).join("");
  }

  renderCandidateIdeas() {
    if (!this.state.candidateIdeas.length) {
      this.elements.candidateIdeaList.innerHTML = this.emptyState("비공개 후보 아이디어 목록이 비어 있습니다.");
      return;
    }

    this.elements.candidateIdeaList.innerHTML = this.state.candidateIdeas.map((idea) => `
      <article class="list-row">
        <div class="row-top">
          <span class="row-title">${this.escape(idea.title)}</span>
          ${this.badge("PRIVATE")}
        </div>
        <div>${this.escape(idea.summary)}</div>
        <div class="badge-row">
          ${this.badge(this.humanizeEnum(idea.category), "info")}
          ${(idea.platforms || []).map((platform) => this.badge(platform)).join("")}
        </div>
        <button class="button secondary" type="button" data-action="run-analysis" data-idea-id="${idea.candidateIdeaId}">중복 분석</button>
      </article>
    `).join("");
  }

  renderDuplicateAnalysis(analysis) {
    const matches = analysis.matches || [];
    const matchRows = matches.length
      ? matches.map((match) => `
          <article class="list-row">
            <div class="row-top">
              <span class="row-title">${this.escape(match.sourceTitle || this.privateSourceTitle(match))}</span>
              ${this.badge(match.similarityLevel, this.similarityTone(match.similarityLevel))}
            </div>
            <div>${this.escape(match.overlapSummary)}</div>
            <div class="badge-row">
              ${this.badge(match.sourceDisclosure)}
              ${(match.overlapDimensions || []).map((dimension) => this.badge(dimension)).join("")}
            </div>
          </article>
        `).join("")
      : this.emptyState("유사 항목이 없습니다.");

    this.elements.duplicateAnalysisResult.innerHTML = `
      <div class="row-top">
        <strong>분석 #${analysis.analysisId} · ${analysis.status}</strong>
        <span class="row-meta">${this.apiTools.formatDateTime(analysis.generatedAt)}</span>
      </div>
      <p class="muted">출시 서비스 ${analysis.scannedReleasedServiceCount}개, 후보 아이디어 ${analysis.scannedCandidateIdeaCount}개를 비교했습니다.</p>
      ${analysis.failureReason ? `<p class="badge danger">${this.escape(analysis.failureReason)}</p>` : ""}
      <div class="list">${matchRows}</div>
    `;
  }

  renderAvailability(response, container, limit) {
    const slots = (response.slots || []).slice(0, limit || response.slots.length);

    if (!slots.length) {
      container.innerHTML = this.emptyState("가용성 데이터가 없습니다.");
      return;
    }

    container.innerHTML = slots.map((slot) => {
      const available = Number(slot.availableMemberCount || 0);
      const busy = Number(slot.busyMemberCount || 0);
      const total = Math.max(available + busy, 1);
      const ratio = Math.round((available / total) * 100);

      return `
        <article class="slot">
          <div class="row-title">${this.apiTools.formatDateTime(slot.startsAt)}</div>
          <div class="row-meta">${this.apiTools.formatDateTime(slot.endsAt)}까지</div>
          <div class="badge-row">
            ${this.badge(`가능 ${available}`, "success")}
            ${this.badge(`바쁨 ${busy}`)}
          </div>
          <div class="slot-bar" aria-hidden="true"><span style="width: ${ratio}%"></span></div>
        </article>
      `;
    }).join("");
  }

  renderHistory() {
    if (!this.state.history.length) {
      this.elements.historyList.innerHTML = this.emptyState("아직 테스트 이력이 없습니다.");
      return;
    }

    this.elements.historyList.innerHTML = this.state.history.map((item) => `
      <article class="list-row">
        <div class="row-top">
          <span class="row-title">${this.escape(item.serviceName)}</span>
          ${this.badge(item.assignmentStatus, this.statusTone(item.assignmentStatus))}
        </div>
        <div class="row-meta">할당 #${item.assignmentId} · 캠페인 #${item.campaignId} · 테스터 팀 #${item.testerTeamId}</div>
        <div>${this.escape(item.feedbackSummary || "피드백 요약 없음")}</div>
      </article>
    `).join("");
  }

  requireTeamId() {
    if (!this.state.team || !this.state.team.teamId) {
      throw new this.apiTools.SwmApiError("현재 작업 팀이 없습니다. 팀을 만들거나 합류하세요.", {
        code: "TEAM_REQUIRED",
      });
    }

    return this.state.team.teamId;
  }

  async withBusy(control, task) {
    const target = control || null;

    try {
      if (target) {
        target.disabled = true;
      }
      await task();
    } catch (error) {
      this.reportError(error);
    } finally {
      if (target) {
        target.disabled = false;
      }
    }
  }

  readForm(form) {
    const values = {};
    const data = new FormData(form);

    data.forEach((value, key) => {
      values[key] = typeof value === "string" ? value.trim() : value;
    });

    return values;
  }

  readSelectedValues(form, name) {
    const select = form.elements[name];
    return Array.from(select.selectedOptions).map((option) => option.value).filter(Boolean);
  }

  setInputValue(formId, name, value) {
    const form = this.document.getElementById(formId);

    if (form && form.elements[name] && !form.elements[name].value) {
      form.elements[name].value = value;
    }
  }

  loadJson(key) {
    try {
      const value = localStorage.getItem(key);
      return value ? JSON.parse(value) : null;
    } catch (_error) {
      return null;
    }
  }

  persistJson(key, value) {
    localStorage.setItem(key, JSON.stringify(value));
  }

  reportSuccess(message) {
    this.elements.activityMessage.textContent = message;
  }

  reportWarning(message) {
    this.elements.activityMessage.textContent = message;
  }

  reportError(error) {
    const message = error && error.message ? error.message : "요청을 처리하지 못했습니다.";
    const code = error && error.code ? ` (${error.code})` : "";
    this.elements.activityMessage.textContent = `${message}${code}`;
  }

  summaryRow(label, value) {
    return `<div class="summary-row"><span>${label}</span><strong>${value}</strong></div>`;
  }

  badge(label, tone = "") {
    return `<span class="badge ${tone}">${this.escape(label)}</span>`;
  }

  emptyState(message) {
    return `<div class="empty-state">${this.escape(message)}</div>`;
  }

  statusTone(status) {
    return {
      OPEN: "success",
      ACTIVE: "success",
      ACCEPTED: "success",
      COMPLETED: "success",
      FEEDBACK_SUBMITTED: "success",
      PENDING: "warning",
      DRAFT: "",
      ASSIGNED: "info",
      CLOSED: "",
      CANCELED: "danger",
      REJECTED: "danger",
      FAILED: "danger",
    }[status] || "";
  }

  similarityTone(level) {
    return {
      HIGH: "danger",
      MEDIUM: "warning",
      LOW: "info",
    }[level] || "";
  }

  privateSourceTitle(match) {
    if (match.sourceDisclosure === "REDACTED") {
      return "다른 팀의 비공개 후보 아이디어";
    }

    return match.sourceType || "유사 항목";
  }

  humanizeEnum(value) {
    return String(value || "")
      .split("_")
      .map((part) => part.charAt(0) + part.slice(1).toLowerCase())
      .join(" ");
  }

  formatObject(value) {
    return JSON.stringify(value, null, 2);
  }

  escape(value) {
    return String(value ?? "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");
  }
}

document.addEventListener("DOMContentLoaded", () => {
  const app = new SwmTeamsWebApp(document, window.SwmApi);
  app.init();
});
