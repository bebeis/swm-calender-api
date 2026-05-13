(() => {
  const PANEL_ID = "swm-teams-calendar-panel";
  const DEFAULT_API_BASE_URL = "http://localhost:8080";
  const ALLOWED_API_BASE_URLS = [DEFAULT_API_BASE_URL];
  const STORAGE_KEYS = {
    apiBaseUrl: "swm.extension.apiBaseUrl",
  };

  class SwmMentoringHistoryPanel {
    constructor(documentRef) {
      this.document = documentRef;
      this.state = {
        apiBaseUrl: DEFAULT_API_BASE_URL,
        accessToken: "",
        lectures: [],
        availability: null,
      };
      this.elements = {};
    }

    async init() {
      if (this.document.getElementById(PANEL_ID)) {
        return;
      }

      await this.restoreSettings();
      this.mount();
      await this.refreshLectures();
    }

    async restoreSettings() {
      const values = await chrome.storage.local.get([STORAGE_KEYS.apiBaseUrl]);
      this.state.apiBaseUrl = this.normalizeApiBaseUrl(values[STORAGE_KEYS.apiBaseUrl]);
    }

    mount() {
      const panel = this.createElement("section", "swm-panel");
      panel.id = PANEL_ID;
      panel.innerHTML = `
        <div class="swm-panel__header">
          <div>
            <p class="swm-eyebrow">SWM Teams Calendar</p>
            <h2>멘토링 신청 내역을 팀 일정으로 보내기</h2>
          </div>
          <span class="swm-badge" data-role="count">읽는 중</span>
        </div>
        <div class="swm-panel__controls">
          <label>
            API origin
            <select data-role="api-base-url">
              <option value="http://localhost:8080">Local API</option>
            </select>
          </label>
          <label>
            Bearer token
            <input data-role="access-token" type="password" autocomplete="off" placeholder="현재 페이지 세션에만 유지" />
          </label>
          <button type="button" class="swm-button swm-button--secondary" data-action="refresh">목록 다시 읽기</button>
          <button type="button" class="swm-button swm-button--primary" data-action="push-selected">선택 일정 API push</button>
          <button type="button" class="swm-button swm-button--secondary" data-action="load-availability">팀 가용성 조회</button>
        </div>
        <div class="swm-panel__external">
          <label>
            When2meet URL
            <input data-role="when2meet-url" type="url" inputmode="url" autocomplete="off" placeholder="https://when2meet.com/..." />
          </label>
          <button type="button" class="swm-button swm-button--secondary" data-action="register-when2meet">When2meet 등록</button>
        </div>
        <div class="swm-panel__status" data-role="status">멘토링 신청 내역 페이지를 분석하고 있습니다.</div>
        <div class="swm-layout">
          <div class="swm-calendar" data-role="lecture-calendar"></div>
          <aside class="swm-side-panel">
            <h3>팀 가용성</h3>
            <div data-role="availability"></div>
          </aside>
        </div>
      `;

      this.elements.panel = panel;
      this.elements.count = panel.querySelector("[data-role='count']");
      this.elements.status = panel.querySelector("[data-role='status']");
      this.elements.calendar = panel.querySelector("[data-role='lecture-calendar']");
      this.elements.availability = panel.querySelector("[data-role='availability']");
      this.elements.apiBaseUrl = panel.querySelector("[data-role='api-base-url']");
      this.elements.accessToken = panel.querySelector("[data-role='access-token']");
      this.elements.when2meetUrl = panel.querySelector("[data-role='when2meet-url']");

      this.elements.apiBaseUrl.value = this.state.apiBaseUrl;
      this.bindEvents(panel);
      this.insertPanel(panel);
    }

    bindEvents(panel) {
      panel.addEventListener("input", (event) => {
        if (event.target === this.elements.accessToken) {
          this.state.accessToken = event.target.value.trim();
        }
      });

      panel.addEventListener("change", async (event) => {
        if (event.target === this.elements.apiBaseUrl) {
          this.state.apiBaseUrl = this.normalizeApiBaseUrl(event.target.value);
          event.target.value = this.state.apiBaseUrl;
          await chrome.storage.local.set({
            [STORAGE_KEYS.apiBaseUrl]: this.state.apiBaseUrl,
          });
          this.report(`API origin을 ${this.state.apiBaseUrl}로 저장했습니다.`);
        }
      });

      panel.addEventListener("click", (event) => {
        const action = event.target.closest("[data-action]")?.dataset.action;

        if (!action) {
          return;
        }

        const actions = {
          refresh: () => this.refreshLectures(),
          "push-selected": () => this.pushSelectedSchedules(),
          "register-when2meet": () => this.registerWhen2meetLink(),
          "load-availability": () => this.loadAvailability(),
        };

        if (actions[action]) {
          this.withBusy(event.target, actions[action]);
        }
      });
    }

    insertPanel(panel) {
      const target =
        this.document.querySelector("#contentsList > div > div > ul.tabs-st1") ||
        this.document.querySelector("#contentsList") ||
        this.document.body;

      if (target === this.document.body) {
        target.prepend(panel);
        return;
      }

      target.after(panel);
    }

    async refreshLectures() {
      this.report("멘토링 신청 내역을 읽는 중입니다.");
      const lectures = await this.readAllLectures();
      this.state.lectures = lectures;
      this.elements.count.textContent = `${lectures.length}개 일정`;
      this.renderLectures();
      this.report(lectures.length ? "신청 내역을 읽었습니다." : "접수 완료된 멘토링 신청 내역을 찾지 못했습니다.");
    }

    async readAllLectures() {
      const historyPath = this.createHistoryPath();
      const totalPages = await this.readTotalPages(historyPath).catch(() => 1);
      const lectures = [];

      for (let page = 1; page <= Math.max(totalPages, 1); page += 1) {
        const response = await fetch(`${historyPath}&pageIndex=${page}`, { credentials: "include" });
        const html = await response.text();
        lectures.push(...this.extractLecturesFromHtml(html));
      }

      if (!lectures.length) {
        lectures.push(...this.extractLecturesFromDocument(this.document));
      }

      return this.dedupeLectures(lectures)
        .map((lecture) => this.normalizeLectureTimes(lecture))
        .filter((lecture) => lecture.startsAt && lecture.endsAt)
        .sort((a, b) => a.startsAt.getTime() - b.startsAt.getTime());
    }

    createHistoryPath() {
      const prefix = location.pathname.match(/^(.*)\/sw(?:\/|$)/)?.[1] || "";
      return `${prefix}/sw/mypage/userAnswer/history.do?menuNo=200047`;
    }

    async readTotalPages(historyPath) {
      const response = await fetch(historyPath, { credentials: "include" });
      const html = await response.text();
      const documentRef = new DOMParser().parseFromString(html, "text/html");
      const totalText = documentRef.querySelector(".bbs-total strong.color-blue")?.nextSibling?.textContent || "";
      const total = Number.parseInt(totalText.replace(":", "").trim(), 10);
      return Number.isFinite(total) && total > 0 ? Math.ceil(total / 10) : 1;
    }

    extractLecturesFromHtml(html) {
      const documentRef = new DOMParser().parseFromString(html, "text/html");
      return this.extractLecturesFromDocument(documentRef);
    }

    extractLecturesFromDocument(documentRef) {
      const rows = Array.from(
        documentRef.querySelectorAll("#contentsList div.boardlist table tbody tr, #contentsList table tbody tr"),
      );

      return rows.map((row) => this.extractLectureFromRow(row)).filter(Boolean);
    }

    extractLectureFromRow(row) {
      const cells = Array.from(row.querySelectorAll("td"));

      if (cells.length < 5) {
        return null;
      }

      const rowText = this.compactText(row.textContent);

      if (!rowText.includes("접수완료")) {
        return null;
      }

      const link = row.querySelector("a[href*='view.do'], a[href*='qustnrSn'], a[href]");
      const url = link ? this.toAbsoluteUrl(link.getAttribute("href")) : null;
      const title = this.compactText((link || cells[2])?.textContent || "");
      const mentor = this.compactText(cells[3]?.textContent || "");
      const dateTimeText = this.compactText(cells[4]?.textContent || rowText);
      const parsedDateTime = this.parseDateTimeText(dateTimeText);

      if (!url || !title || !parsedDateTime) {
        return null;
      }

      const urlObject = new URL(url);
      urlObject.searchParams.set("pageIndex", "1");

      return {
        id: urlObject.searchParams.get("qustnrSn") || this.hashText(urlObject.toString()),
        url: urlObject.toString(),
        title,
        mentor,
        dateLabel: parsedDateTime.dateLabel,
        timeLabel: parsedDateTime.timeLabel,
        startsAt: parsedDateTime.startsAt,
        endsAt: parsedDateTime.endsAt,
        approved: rowText.includes("OK"),
        location: null,
        detailLoaded: false,
      };
    }

    parseDateTimeText(text) {
      const dateMatch = text.match(/(\d{4})[.-](\d{1,2})[.-](\d{1,2})/);
      const timeMatch = text.match(/(\d{1,2}):(\d{2})(?::\d{2})?\s*~\s*(\d{1,2}):(\d{2})(?::\d{2})?/);

      if (!dateMatch || !timeMatch) {
        return null;
      }

      const [, year, month, day] = dateMatch;
      const [, startHour, startMinute, endHour, endMinute] = timeMatch;
      const datePrefix = `${year}-${month.padStart(2, "0")}-${day.padStart(2, "0")}`;
      const startsAt = new Date(`${datePrefix}T${startHour.padStart(2, "0")}:${startMinute}:00`);
      const endsAt = new Date(`${datePrefix}T${endHour.padStart(2, "0")}:${endMinute}:00`);

      if (Number.isNaN(startsAt.getTime()) || Number.isNaN(endsAt.getTime())) {
        return null;
      }

      return {
        dateLabel: datePrefix,
        timeLabel: `${startHour.padStart(2, "0")}:${startMinute} ~ ${endHour.padStart(2, "0")}:${endMinute}`,
        startsAt,
        endsAt,
      };
    }

    normalizeLectureTimes(lecture) {
      if (lecture.endsAt <= lecture.startsAt) {
        lecture.endsAt = new Date(lecture.endsAt.getTime() + 24 * 60 * 60 * 1000);
      }

      return lecture;
    }

    dedupeLectures(lectures) {
      const seen = new Set();
      return lectures.filter((lecture) => {
        const key = `${lecture.id}:${lecture.startsAt?.toISOString()}`;

        if (seen.has(key)) {
          return false;
        }

        seen.add(key);
        return true;
      });
    }

    renderLectures() {
      if (!this.state.lectures.length) {
        this.elements.calendar.innerHTML = `<div class="swm-empty">접수 완료된 멘토링 신청 내역이 없습니다.</div>`;
        return;
      }

      const groups = this.groupLecturesByDate(this.state.lectures);
      this.elements.calendar.innerHTML = Array.from(groups.entries()).map(([date, lectures]) => `
        <section class="swm-day">
          <div class="swm-day__title">${this.escape(this.formatDateTitle(date))}</div>
          ${lectures.map((lecture, index) => this.renderLecture(lecture, this.isConflicted(lectures, index))).join("")}
        </section>
      `).join("");
    }

    renderLecture(lecture, conflicted) {
      const ended = lecture.endsAt < new Date();

      return `
        <article class="swm-lecture ${conflicted ? "swm-lecture--conflict" : ""} ${ended ? "swm-lecture--ended" : ""}">
          <label class="swm-lecture__check">
            <input type="checkbox" data-lecture-id="${this.escape(lecture.id)}" ${ended ? "" : "checked"} />
            <span>
              <strong>${this.escape(lecture.title)}</strong>
              <small>${this.escape(lecture.mentor || "멘토 정보 없음")} · ${this.escape(lecture.timeLabel)}</small>
            </span>
          </label>
          <div class="swm-lecture__meta">
            <a href="${this.escape(lecture.url)}" target="_blank" rel="noreferrer">상세</a>
            ${this.badge(lecture.approved ? "개설 확정" : "승인 대기", lecture.approved ? "success" : "warning")}
            ${conflicted ? this.badge("시간 겹침", "danger") : ""}
          </div>
        </article>
      `;
    }

    async pushSelectedSchedules() {
      this.requireToken();
      const selectedLectures = this.getSelectedLectures();

      if (!selectedLectures.length) {
        throw new Error("선택된 일정이 없습니다.");
      }

      this.report("선택한 일정 상세를 보강하고 API로 전송합니다.");
      const enrichedLectures = await this.enrichLectures(selectedLectures);
      const schedules = enrichedLectures.map((lecture) => this.toMentoringSchedule(lecture));
      const result = await this.callApi("bulkPushMentoringSchedules", { schedules });
      this.report(`API push 완료: 생성 ${result.createdCount}건, 중복 스킵 ${result.skippedDuplicateCount}건`);
    }

    async loadAvailability() {
      this.requireToken();
      const range = this.resolveAvailabilityRange();
      const result = await this.callApi("getUnifiedAvailability", {
        startsAt: range.startsAt.toISOString(),
        endsAt: range.endsAt.toISOString(),
      });
      this.state.availability = result;
      this.renderAvailability(result);
      this.report("팀 가용성을 조회했습니다.");
    }

    async registerWhen2meetLink() {
      this.requireToken();
      const url = this.elements.when2meetUrl.value.trim();

      if (!url) {
        throw new Error("When2meet URL을 입력하세요.");
      }

      const result = await this.callApi("putWhen2meetLink", { url });
      const failureReason = result.failureReason ? `: ${result.failureReason}` : "";
      this.report(`When2meet 링크 저장됨: ${result.status}${failureReason}`);
    }

    async enrichLectures(lectures) {
      const enriched = [];

      for (const lecture of lectures) {
        enriched.push(await this.enrichLecture(lecture));
      }

      return enriched;
    }

    async enrichLecture(lecture) {
      if (lecture.detailLoaded) {
        return lecture;
      }

      try {
        const response = await fetch(lecture.url, { credentials: "include" });
        const html = await response.text();
        const documentRef = new DOMParser().parseFromString(html, "text/html");
        lecture.location = this.readTopValue(documentRef, "장소");
        lecture.peopleSummary = this.readTopValue(documentRef, "모집인원");
        lecture.detailLoaded = true;
      } catch (_error) {
        lecture.detailLoaded = true;
      }

      return lecture;
    }

    readTopValue(documentRef, label) {
      const group = Array.from(documentRef.querySelectorAll("div.top .group")).find((item) => {
        return this.compactText(item.querySelector(".t")?.textContent || "") === label;
      });
      return this.compactText(group?.querySelector(".c")?.textContent || "") || null;
    }

    toMentoringSchedule(lecture) {
      return {
        externalSourceId: `swmaestro:${lecture.id}`,
        title: lecture.title,
        startsAt: lecture.startsAt.toISOString(),
        endsAt: lecture.endsAt.toISOString(),
        location: lecture.location,
        description: [
          lecture.mentor ? `멘토: ${lecture.mentor}` : null,
          lecture.peopleSummary ? `모집인원: ${lecture.peopleSummary}` : null,
          lecture.approved ? "개설 확정" : "승인 대기",
          lecture.url,
        ].filter(Boolean).join("\n"),
      };
    }

    renderAvailability(result) {
      const slots = result.slots || [];

      if (!slots.length) {
        this.elements.availability.innerHTML = `<div class="swm-empty">조회된 가용성 슬롯이 없습니다.</div>`;
        return;
      }

      this.elements.availability.innerHTML = slots.slice(0, 18).map((slot) => {
        const available = Number(slot.availableMemberCount || 0);
        const busy = Number(slot.busyMemberCount || 0);
        const total = Math.max(available + busy, 1);
        const ratio = Math.round((available / total) * 100);

        return `
          <article class="swm-availability-slot">
            <strong>${this.escape(this.formatDateTime(slot.startsAt))}</strong>
            <small>${this.escape(this.formatDateTime(slot.endsAt))}까지</small>
            <div class="swm-slot-bar"><span style="width: ${ratio}%"></span></div>
            <div class="swm-availability-slot__counts">
              ${this.badge(`가능 ${available}`, "success")}
              ${this.badge(`바쁨 ${busy}`)}
            </div>
          </article>
        `;
      }).join("");
    }

    getSelectedLectures() {
      const selectedIds = new Set(
        Array.from(this.elements.calendar.querySelectorAll("input[data-lecture-id]:checked"))
          .map((input) => input.dataset.lectureId),
      );
      return this.state.lectures.filter((lecture) => selectedIds.has(lecture.id));
    }

    resolveAvailabilityRange() {
      const selectedLectures = this.getSelectedLectures();
      const lectures = selectedLectures.length ? selectedLectures : this.state.lectures;
      const starts = lectures.map((lecture) => lecture.startsAt.getTime());
      const ends = lectures.map((lecture) => lecture.endsAt.getTime());
      const fallbackStart = new Date();
      const fallbackEnd = new Date(Date.now() + 14 * 24 * 60 * 60 * 1000);

      return {
        startsAt: starts.length ? new Date(Math.min(...starts)) : fallbackStart,
        endsAt: ends.length ? new Date(Math.max(...ends)) : fallbackEnd,
      };
    }

    async callApi(operation, payload) {
      const response = await chrome.runtime.sendMessage({
        type: "SWM_TEAMS_API_REQUEST",
        payload: {
          ...payload,
          operation,
          baseUrl: this.state.apiBaseUrl,
          token: this.state.accessToken,
        },
      });

      if (!response || !response.ok) {
        throw new Error(response?.error?.message || "API 요청에 실패했습니다.");
      }

      return response.data;
    }

    requireToken() {
      if (!this.state.accessToken) {
        throw new Error("Bearer token을 먼저 입력하세요.");
      }
    }

    groupLecturesByDate(lectures) {
      const groups = new Map();

      lectures.forEach((lecture) => {
        if (!groups.has(lecture.dateLabel)) {
          groups.set(lecture.dateLabel, []);
        }
        groups.get(lecture.dateLabel).push(lecture);
      });

      return groups;
    }

    isConflicted(lectures, index) {
      const current = lectures[index];
      return lectures.some((lecture, otherIndex) => {
        if (otherIndex === index) {
          return false;
        }
        return current.startsAt < lecture.endsAt && lecture.startsAt < current.endsAt;
      });
    }

    async withBusy(control, task) {
      try {
        control.disabled = true;
        await task();
      } catch (error) {
        this.report(error && error.message ? error.message : "작업을 처리하지 못했습니다.", true);
      } finally {
        control.disabled = false;
      }
    }

    report(message, failed = false) {
      this.elements.status.textContent = message;
      this.elements.status.classList.toggle("swm-panel__status--error", failed);
    }

    createElement(tagName, className) {
      const element = this.document.createElement(tagName);
      element.className = className;
      return element;
    }

    toAbsoluteUrl(href) {
      try {
        return new URL(href, location.origin).toString();
      } catch (_error) {
        return null;
      }
    }

    compactText(value) {
      return String(value || "").replace(/\u00a0/g, " ").replace(/\s+/g, " ").trim();
    }

    normalizeApiBaseUrl(value) {
      const candidate = String(value || DEFAULT_API_BASE_URL).trim().replace(/\/+$/, "");
      return ALLOWED_API_BASE_URLS.includes(candidate) ? candidate : DEFAULT_API_BASE_URL;
    }

    hashText(value) {
      let hash = 0;

      for (let index = 0; index < value.length; index += 1) {
        hash = (hash * 31 + value.charCodeAt(index)) >>> 0;
      }

      return String(hash);
    }

    formatDateTitle(dateLabel) {
      const date = new Date(`${dateLabel}T00:00:00`);
      const weekday = ["일", "월", "화", "수", "목", "금", "토"][date.getDay()];
      return `${date.getMonth() + 1}월 ${date.getDate()}일 (${weekday})`;
    }

    formatDateTime(value) {
      const date = new Date(value);

      if (Number.isNaN(date.getTime())) {
        return String(value);
      }

      return new Intl.DateTimeFormat("ko-KR", {
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      }).format(date);
    }

    badge(label, tone = "") {
      return `<span class="swm-badge ${tone ? `swm-badge--${tone}` : ""}">${this.escape(label)}</span>`;
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

  new SwmMentoringHistoryPanel(document).init().catch((error) => {
    console.error("[SWM Teams] failed to initialize mentoring history panel", error);
  });
})();
