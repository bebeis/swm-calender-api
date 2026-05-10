# Tasks: SWM Teams MVP

**Input**: Design documents from `/specs/001-swm-teams-mvp/`
**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md),
[data-model.md](./data-model.md), [contracts/openapi.yaml](./contracts/openapi.yaml), [quickstart.md](./quickstart.md)

**Tests**: Included because repository rules require Kotest, mockk, RestDocs, and repository integration coverage.

**Organization**: Tasks are grouped by user story so each story can be implemented and tested independently after
the foundational phase.

## Phase 1: Setup

**Purpose**: Add the module skeleton and shared build structure required by the MVP domains.

- [X] T001 Add `core:core-common`, `core:core-team-domain`, `calendar:calendar-domain`, `match:match-domain`, `clients:google-calendar`, and `clients:when2meet` includes in `settings.gradle.kts`
- [X] T002 [P] Create `apps/backend/core/core-common/build.gradle.kts` with Kotlin library settings and no runnable Boot jar
- [X] T003 [P] Create `apps/backend/core/core-team-domain/build.gradle.kts` with dependency on `core:core-common`
- [X] T004 [P] Create `apps/backend/calendar/calendar-domain/build.gradle.kts` with dependency on `core:core-team-domain`
- [X] T005 [P] Create `apps/backend/match/match-domain/build.gradle.kts` with dependency on `core:core-team-domain`
- [X] T006 [P] Create `apps/backend/clients/google-calendar/build.gradle.kts` for the Calendar API client module
- [X] T007 [P] Create `apps/backend/clients/when2meet/build.gradle.kts` for the When2meet parser client module
- [X] T008 Update `apps/backend/core/core-api/build.gradle.kts` to depend on the new domain/client modules and use `runtimeOnly` for `storage:db-core`
- [X] T009 Update `apps/backend/storage/db-core/build.gradle.kts` to compile against the new domain modules in `apps/backend/storage/db-core/build.gradle.kts`

**Checkpoint**: Gradle recognizes all feature modules.

## Phase 2: Foundational

**Purpose**: Shared types, security context, error conventions, and persistence conventions that block all stories.

- [X] T010 [P] Add shared id value objects in `apps/backend/core/core-common/src/main/kotlin/swm/calender/core/common/id/DomainIds.kt`
- [X] T011 [P] Add common time range value object in `apps/backend/core/core-common/src/main/kotlin/swm/calender/core/common/time/DateTimeRange.kt`
- [X] T012 [P] Add MVP enum types in `apps/backend/core/core-enum/src/main/kotlin/swm/calender/core/enums/TeamsEnums.kt`
- [X] T013 Add authenticated user context model in `apps/backend/core/core-api/src/main/kotlin/swm/calender/core/api/security/AuthenticatedUser.kt`
- [X] T014 Add team-scope authorization component in `apps/backend/core/core-api/src/main/kotlin/swm/calender/core/api/security/TeamAuthorization.kt`
- [X] T015 Add feature error codes and messages in `apps/backend/core/core-api/src/main/kotlin/swm/calender/core/support/error/ErrorType.kt`
- [X] T016 Add API version base path convention tests in `apps/backend/core/core-api/src/test/kotlin/swm/calender/core/api/controller/v1/ApiVersioningTest.kt`
- [X] T017 Add Exposed transaction configuration for repositories in `apps/backend/storage/db-core/src/main/kotlin/swm/calender/storage/db/core/config/ExposedTransactionConfig.kt`
- [X] T018 Add shared repository test support in `apps/backend/storage/db-core/src/test/kotlin/swm/calender/storage/db/core/RepositoryTestSupport.kt`

**Checkpoint**: Shared domain, API, and persistence foundations are ready for story work.

## Phase 3: User Story 1 - Team Onboarding And Sub-Service Activation (Priority: P1)

**Goal**: Users can create a team, join with an invite code, and OWNER can independently activate Calendar and Match.

**Independent Test**: Create a team as one user, join as another user, then toggle each sub-service and verify team state.

### Tests for User Story 1

- [X] T019 [P] [US1] Add Team domain behavior tests in `apps/backend/core/core-team-domain/src/test/kotlin/swm/calender/core/team/domain/TeamTest.kt`
- [X] T020 [P] [US1] Add TeamService mockk tests in `apps/backend/core/core-team-domain/src/test/kotlin/swm/calender/core/team/service/TeamServiceTest.kt`
- [X] T021 [P] [US1] Add TeamController RestDocs tests in `apps/backend/core/core-api/src/test/kotlin/swm/calender/core/api/controller/v1/team/TeamControllerTest.kt`
- [X] T022 [P] [US1] Add Team repository integration tests in `apps/backend/storage/db-core/src/test/kotlin/swm/calender/storage/db/core/team/TeamExposedRepositoryIT.kt`

### Implementation for User Story 1

- [X] T023 [P] [US1] Add Team domain models in `apps/backend/core/core-team-domain/src/main/kotlin/swm/calender/core/team/domain/model/Team.kt`
- [X] T024 [P] [US1] Add TeamMember domain models in `apps/backend/core/core-team-domain/src/main/kotlin/swm/calender/core/team/domain/model/TeamMember.kt`
- [X] T025 [P] [US1] Add SubServiceActivation domain model in `apps/backend/core/core-team-domain/src/main/kotlin/swm/calender/core/team/domain/model/SubServiceActivation.kt`
- [X] T026 [US1] Add TeamRepository interface in `apps/backend/core/core-team-domain/src/main/kotlin/swm/calender/core/team/domain/TeamRepository.kt`
- [X] T027 [US1] Add TeamReader and TeamWriter implement-layer components in `apps/backend/core/core-team-domain/src/main/kotlin/swm/calender/core/team/implement/TeamReader.kt`
- [X] T028 [US1] Add TeamService onboarding and activation flow in `apps/backend/core/core-team-domain/src/main/kotlin/swm/calender/core/team/service/TeamService.kt`
- [X] T029 [US1] Add team controller request DTOs in `apps/backend/core/core-api/src/main/kotlin/swm/calender/core/api/controller/v1/team/request/TeamCreateRequest.kt`
- [X] T030 [US1] Add team controller response DTOs in `apps/backend/core/core-api/src/main/kotlin/swm/calender/core/api/controller/v1/team/response/TeamResponse.kt`
- [X] T031 [US1] Add TeamController endpoints in `apps/backend/core/core-api/src/main/kotlin/swm/calender/core/api/controller/v1/team/TeamController.kt`
- [X] T032 [US1] Add TeamTable and TeamMemberTable in `apps/backend/storage/db-core/src/main/kotlin/swm/calender/storage/db/core/team/TeamTable.kt`
- [X] T033 [US1] Add TeamExposedRepository in `apps/backend/storage/db-core/src/main/kotlin/swm/calender/storage/db/core/team/TeamExposedRepository.kt`
- [X] T034 [US1] Add team Flyway migration in `apps/backend/storage/db-core/src/main/resources/db/migration/V2__create_team_tables.sql`

**Checkpoint**: US1 works independently through API, domain, and repository tests.

## Phase 4: User Story 2 - Unified Team Availability (Priority: P1)

**Goal**: Calendar-enabled teams can push mentoring schedules, register When2meet links, and view unified availability.

**Independent Test**: Enable Calendar, bulk push schedules twice, register a When2meet link, and query availability.

### Tests for User Story 2

- [ ] T035 [P] [US2] Add Calendar domain tests in `apps/backend/calendar/calendar-domain/src/test/kotlin/swm/calender/calendar/domain/CalendarDomainTest.kt`
- [ ] T036 [P] [US2] Add CalendarService mockk tests in `apps/backend/calendar/calendar-domain/src/test/kotlin/swm/calender/calendar/service/CalendarServiceTest.kt`
- [ ] T037 [P] [US2] Add CalendarController RestDocs tests in `apps/backend/core/core-api/src/test/kotlin/swm/calender/core/api/controller/v1/calendar/CalendarControllerTest.kt`
- [ ] T038 [P] [US2] Add Calendar repository integration tests in `apps/backend/storage/db-core/src/test/kotlin/swm/calender/storage/db/core/calendar/CalendarExposedRepositoryIT.kt`
- [ ] T039 [P] [US2] Add When2meet parser tests in `apps/backend/clients/when2meet/src/test/kotlin/swm/calender/client/when2meet/When2meetParserTest.kt`

### Implementation for User Story 2

- [ ] T040 [P] [US2] Add Calendar domain models in `apps/backend/calendar/calendar-domain/src/main/kotlin/swm/calender/calendar/domain/model/TeamCalendar.kt`
- [ ] T041 [P] [US2] Add MentoringSchedule domain model in `apps/backend/calendar/calendar-domain/src/main/kotlin/swm/calender/calendar/domain/model/MentoringSchedule.kt`
- [ ] T042 [P] [US2] Add When2meetLink and AvailabilitySlot models in `apps/backend/calendar/calendar-domain/src/main/kotlin/swm/calender/calendar/domain/model/When2meetLink.kt`
- [ ] T043 [US2] Add CalendarRepository interface in `apps/backend/calendar/calendar-domain/src/main/kotlin/swm/calender/calendar/domain/CalendarRepository.kt`
- [ ] T044 [US2] Add CalendarReader and CalendarWriter in `apps/backend/calendar/calendar-domain/src/main/kotlin/swm/calender/calendar/implement/CalendarReader.kt`
- [ ] T045 [US2] Add CalendarService in `apps/backend/calendar/calendar-domain/src/main/kotlin/swm/calender/calendar/service/CalendarService.kt`
- [ ] T046 [US2] Add GoogleCalendarClient port in `apps/backend/clients/google-calendar/src/main/kotlin/swm/calender/client/google/calendar/GoogleCalendarClient.kt`
- [ ] T047 [US2] Add When2meetParser client in `apps/backend/clients/when2meet/src/main/kotlin/swm/calender/client/when2meet/When2meetParser.kt`
- [ ] T048 [US2] Add CalendarController request DTOs in `apps/backend/core/core-api/src/main/kotlin/swm/calender/core/api/controller/v1/calendar/request/MentoringScheduleBulkPushRequest.kt`
- [ ] T049 [US2] Add CalendarController response DTOs in `apps/backend/core/core-api/src/main/kotlin/swm/calender/core/api/controller/v1/calendar/response/UnifiedAvailabilityResponse.kt`
- [ ] T050 [US2] Add CalendarController endpoints in `apps/backend/core/core-api/src/main/kotlin/swm/calender/core/api/controller/v1/calendar/CalendarController.kt`
- [ ] T051 [US2] Add Calendar Exposed tables in `apps/backend/storage/db-core/src/main/kotlin/swm/calender/storage/db/core/calendar/CalendarTable.kt`
- [ ] T052 [US2] Add CalendarExposedRepository in `apps/backend/storage/db-core/src/main/kotlin/swm/calender/storage/db/core/calendar/CalendarExposedRepository.kt`
- [ ] T053 [US2] Add calendar Flyway migration in `apps/backend/storage/db-core/src/main/resources/db/migration/V3__create_calendar_tables.sql`

**Checkpoint**: US2 works independently for Calendar-enabled teams and protects team scope.

## Phase 5: User Story 3 - Service Profile, Candidate Ideas, And Duplicate Discovery (Priority: P1)

**Goal**: Match-enabled teams can publish service profiles, keep candidate ideas private, run duplicate analysis
against released services and candidate ideas, open campaigns, and search public campaigns.

**Independent Test**: Publish a profile, create a private candidate idea, run duplicate analysis, verify private
candidate redaction for other teams, create/open a campaign, and find only the public campaign through filters
from another team.

### Tests for User Story 3

- [ ] T054 [P] [US3] Add ServiceProfile domain tests in `apps/backend/match/match-domain/src/test/kotlin/swm/calender/match/domain/ServiceProfileTest.kt`
- [ ] T055 [P] [US3] Add BetaCampaign domain tests in `apps/backend/match/match-domain/src/test/kotlin/swm/calender/match/domain/BetaCampaignTest.kt`
- [ ] T056 [P] [US3] Add MatchService campaign tests in `apps/backend/match/match-domain/src/test/kotlin/swm/calender/match/service/MatchCampaignServiceTest.kt`
- [ ] T057 [P] [US3] Add MatchController profile/campaign RestDocs tests in `apps/backend/core/core-api/src/test/kotlin/swm/calender/core/api/controller/v1/match/MatchCampaignControllerTest.kt`
- [ ] T058 [P] [US3] Add Match campaign repository integration tests in `apps/backend/storage/db-core/src/test/kotlin/swm/calender/storage/db/core/match/MatchCampaignExposedRepositoryIT.kt`
- [ ] T059 [P] [US3] Add CandidateIdea domain tests in `apps/backend/match/match-domain/src/test/kotlin/swm/calender/match/domain/CandidateIdeaTest.kt`
- [ ] T060 [P] [US3] Add DuplicateAnalysis service tests with redacted private-source results in `apps/backend/match/match-domain/src/test/kotlin/swm/calender/match/service/DuplicateAnalysisServiceTest.kt`
- [ ] T061 [P] [US3] Add CandidateIdeaController RestDocs tests for candidate ideas and duplicate analysis in `apps/backend/core/core-api/src/test/kotlin/swm/calender/core/api/controller/v1/match/CandidateIdeaControllerTest.kt`
- [ ] T062 [P] [US3] Add candidate idea and duplicate analysis repository integration tests in `apps/backend/storage/db-core/src/test/kotlin/swm/calender/storage/db/core/match/CandidateIdeaExposedRepositoryIT.kt`

### Implementation for User Story 3

- [ ] T063 [P] [US3] Add ServiceProfile model in `apps/backend/match/match-domain/src/main/kotlin/swm/calender/match/domain/model/ServiceProfile.kt`
- [ ] T064 [P] [US3] Add BetaCampaign model in `apps/backend/match/match-domain/src/main/kotlin/swm/calender/match/domain/model/BetaCampaign.kt`
- [ ] T065 [P] [US3] Add CandidateIdea model in `apps/backend/match/match-domain/src/main/kotlin/swm/calender/match/domain/model/CandidateIdea.kt`
- [ ] T066 [P] [US3] Add DuplicateAnalysis and DuplicateAnalysisMatch models in `apps/backend/match/match-domain/src/main/kotlin/swm/calender/match/domain/model/DuplicateAnalysis.kt`
- [ ] T067 [US3] Add MatchCampaignRepository interface in `apps/backend/match/match-domain/src/main/kotlin/swm/calender/match/domain/MatchCampaignRepository.kt`
- [ ] T068 [US3] Add CandidateIdeaRepository and DuplicateAnalysisRepository interfaces in `apps/backend/match/match-domain/src/main/kotlin/swm/calender/match/domain/`
- [ ] T069 [US3] Add MatchCampaignReader and MatchCampaignWriter in `apps/backend/match/match-domain/src/main/kotlin/swm/calender/match/implement/MatchCampaignReader.kt`
- [ ] T070 [US3] Add CandidateIdeaReader/Writer and DuplicateAnalysisReader/Writer implement-layer components in `apps/backend/match/match-domain/src/main/kotlin/swm/calender/match/implement/`
- [ ] T071 [US3] Add AI duplicate analyzer port in `apps/backend/match/match-domain/src/main/kotlin/swm/calender/match/implement/DuplicateIdeaAnalyzer.kt`
- [ ] T072 [US3] Add MatchCampaignService in `apps/backend/match/match-domain/src/main/kotlin/swm/calender/match/service/MatchCampaignService.kt`
- [ ] T073 [US3] Add CandidateIdeaService and DuplicateAnalysisService in `apps/backend/match/match-domain/src/main/kotlin/swm/calender/match/service/`
- [ ] T074 [US3] Add Match campaign request DTOs in `apps/backend/core/core-api/src/main/kotlin/swm/calender/core/api/controller/v1/match/request/CampaignCreateRequest.kt`
- [ ] T075 [US3] Add Match campaign response DTOs in `apps/backend/core/core-api/src/main/kotlin/swm/calender/core/api/controller/v1/match/response/CampaignResponse.kt`
- [ ] T076 [US3] Add candidate idea and duplicate analysis DTOs in `apps/backend/core/core-api/src/main/kotlin/swm/calender/core/api/controller/v1/match/`
- [ ] T077 [US3] Add MatchCampaignController endpoints in `apps/backend/core/core-api/src/main/kotlin/swm/calender/core/api/controller/v1/match/MatchCampaignController.kt`
- [ ] T078 [US3] Add CandidateIdeaController endpoints for create/list and duplicate analysis in `apps/backend/core/core-api/src/main/kotlin/swm/calender/core/api/controller/v1/match/CandidateIdeaController.kt`
- [ ] T079 [US3] Add Match campaign Exposed tables in `apps/backend/storage/db-core/src/main/kotlin/swm/calender/storage/db/core/match/MatchCampaignTable.kt`
- [ ] T080 [US3] Add CandidateIdeaTable and DuplicateAnalysisTable in `apps/backend/storage/db-core/src/main/kotlin/swm/calender/storage/db/core/match/CandidateIdeaTable.kt`
  and `apps/backend/storage/db-core/src/main/kotlin/swm/calender/storage/db/core/match/DuplicateAnalysisTable.kt`
- [ ] T081 [US3] Add MatchCampaignExposedRepository in `apps/backend/storage/db-core/src/main/kotlin/swm/calender/storage/db/core/match/MatchCampaignExposedRepository.kt`
- [ ] T082 [US3] Add CandidateIdeaExposedRepository and DuplicateAnalysisExposedRepository in `apps/backend/storage/db-core/src/main/kotlin/swm/calender/storage/db/core/match/`
- [ ] T083 [US3] Add match campaign Flyway migration in `apps/backend/storage/db-core/src/main/resources/db/migration/V4__create_match_campaign_tables.sql`
- [ ] T084 [US3] Add candidate idea and duplicate analysis Flyway migration in `apps/backend/storage/db-core/src/main/resources/db/migration/V5__create_candidate_idea_tables.sql`

**Checkpoint**: US3 works independently for profile publishing, private candidate ideas, duplicate analysis, and
public campaign discovery.

## Phase 6: User Story 4 - Beta Request And Assignment Lifecycle (Priority: P2)

**Goal**: Teams can send one-way or reciprocal requests, and target OWNER can accept, reject, or cancel requests.

**Independent Test**: Send a request to an open campaign, accept it as target OWNER, and verify assignment creation.

### Tests for User Story 4

- [ ] T085 [P] [US4] Add MatchRequest domain tests in `apps/backend/match/match-domain/src/test/kotlin/swm/calender/match/domain/MatchRequestTest.kt`
- [ ] T086 [P] [US4] Add Assignment domain tests in `apps/backend/match/match-domain/src/test/kotlin/swm/calender/match/domain/AssignmentTest.kt`
- [ ] T087 [P] [US4] Add MatchRequestService mockk tests in `apps/backend/match/match-domain/src/test/kotlin/swm/calender/match/service/MatchRequestServiceTest.kt`
- [ ] T088 [P] [US4] Add MatchRequestController RestDocs tests in `apps/backend/core/core-api/src/test/kotlin/swm/calender/core/api/controller/v1/match/MatchRequestControllerTest.kt`
- [ ] T089 [P] [US4] Add Match request repository integration tests in `apps/backend/storage/db-core/src/test/kotlin/swm/calender/storage/db/core/match/MatchRequestExposedRepositoryIT.kt`

### Implementation for User Story 4

- [ ] T090 [P] [US4] Add MatchRequest model in `apps/backend/match/match-domain/src/main/kotlin/swm/calender/match/domain/model/MatchRequest.kt`
- [ ] T091 [P] [US4] Add Assignment model in `apps/backend/match/match-domain/src/main/kotlin/swm/calender/match/domain/model/Assignment.kt`
- [ ] T092 [P] [US4] Add Notification model in `apps/backend/match/match-domain/src/main/kotlin/swm/calender/match/domain/model/Notification.kt`
- [ ] T093 [US4] Add MatchRequestRepository interface in `apps/backend/match/match-domain/src/main/kotlin/swm/calender/match/domain/MatchRequestRepository.kt`
- [ ] T094 [US4] Add MatchRequestReader and MatchRequestWriter in `apps/backend/match/match-domain/src/main/kotlin/swm/calender/match/implement/MatchRequestReader.kt`
- [ ] T095 [US4] Add MatchRequestService in `apps/backend/match/match-domain/src/main/kotlin/swm/calender/match/service/MatchRequestService.kt`
- [ ] T096 [US4] Add Match request DTOs in `apps/backend/core/core-api/src/main/kotlin/swm/calender/core/api/controller/v1/match/request/MatchRequestCreateRequest.kt`
- [ ] T097 [US4] Add Match request response DTOs in `apps/backend/core/core-api/src/main/kotlin/swm/calender/core/api/controller/v1/match/response/MatchRequestResponse.kt`
- [ ] T098 [US4] Add MatchRequestController endpoints in `apps/backend/core/core-api/src/main/kotlin/swm/calender/core/api/controller/v1/match/MatchRequestController.kt`
- [ ] T099 [US4] Add Match request Exposed tables in `apps/backend/storage/db-core/src/main/kotlin/swm/calender/storage/db/core/match/MatchRequestTable.kt`
- [ ] T100 [US4] Add MatchRequestExposedRepository in `apps/backend/storage/db-core/src/main/kotlin/swm/calender/storage/db/core/match/MatchRequestExposedRepository.kt`
- [ ] T101 [US4] Add match request Flyway migration in `apps/backend/storage/db-core/src/main/resources/db/migration/V6__create_match_request_tables.sql`

**Checkpoint**: US4 request lifecycle works and accepted requests create assignments exactly once.

## Phase 7: User Story 5 - Structured Feedback And Test History (Priority: P2)

**Goal**: Assigned teams can submit structured feedback and retrieve assignment/test history.

**Independent Test**: Submit feedback for an assignment and verify assignment detail and team test history include it.

### Tests for User Story 5

- [ ] T102 [P] [US5] Add Feedback domain tests in `apps/backend/match/match-domain/src/test/kotlin/swm/calender/match/domain/FeedbackTest.kt`
- [ ] T103 [P] [US5] Add FeedbackService mockk tests in `apps/backend/match/match-domain/src/test/kotlin/swm/calender/match/service/FeedbackServiceTest.kt`
- [ ] T104 [P] [US5] Add FeedbackController RestDocs tests in `apps/backend/core/core-api/src/test/kotlin/swm/calender/core/api/controller/v1/match/FeedbackControllerTest.kt`
- [ ] T105 [P] [US5] Add feedback repository integration tests in `apps/backend/storage/db-core/src/test/kotlin/swm/calender/storage/db/core/match/FeedbackExposedRepositoryIT.kt`

### Implementation for User Story 5

- [ ] T106 [P] [US5] Add Feedback model in `apps/backend/match/match-domain/src/main/kotlin/swm/calender/match/domain/model/Feedback.kt`
- [ ] T107 [US5] Add FeedbackRepository interface in `apps/backend/match/match-domain/src/main/kotlin/swm/calender/match/domain/FeedbackRepository.kt`
- [ ] T108 [US5] Add FeedbackReader and FeedbackWriter in `apps/backend/match/match-domain/src/main/kotlin/swm/calender/match/implement/FeedbackReader.kt`
- [ ] T109 [US5] Add FeedbackService in `apps/backend/match/match-domain/src/main/kotlin/swm/calender/match/service/FeedbackService.kt`
- [ ] T110 [US5] Add feedback request DTOs in `apps/backend/core/core-api/src/main/kotlin/swm/calender/core/api/controller/v1/match/request/FeedbackSubmitRequest.kt`
- [ ] T111 [US5] Add feedback response DTOs in `apps/backend/core/core-api/src/main/kotlin/swm/calender/core/api/controller/v1/match/response/FeedbackResponse.kt`
- [ ] T112 [US5] Add FeedbackController endpoints in `apps/backend/core/core-api/src/main/kotlin/swm/calender/core/api/controller/v1/match/FeedbackController.kt`
- [ ] T113 [US5] Add FeedbackTable in `apps/backend/storage/db-core/src/main/kotlin/swm/calender/storage/db/core/match/FeedbackTable.kt`
- [ ] T114 [US5] Add FeedbackExposedRepository in `apps/backend/storage/db-core/src/main/kotlin/swm/calender/storage/db/core/match/FeedbackExposedRepository.kt`
- [ ] T115 [US5] Add feedback Flyway migration in `apps/backend/storage/db-core/src/main/resources/db/migration/V7__create_feedback_tables.sql`

**Checkpoint**: US5 feedback and test history work without exposing other teams' private data.

## Phase 8: User Story 6 - Service Pivot And Team Administration (Priority: P3)

**Goal**: OWNER can replace active service profile and manage exceptional team membership changes.

**Independent Test**: Activate a replacement service profile and change membership while preserving history.

### Tests for User Story 6

- [ ] T116 [P] [US6] Add service profile pivot tests in `apps/backend/match/match-domain/src/test/kotlin/swm/calender/match/service/ServiceProfilePivotServiceTest.kt`
- [ ] T117 [P] [US6] Add team administration tests in `apps/backend/core/core-team-domain/src/test/kotlin/swm/calender/core/team/service/TeamAdministrationServiceTest.kt`
- [ ] T118 [P] [US6] Add administration RestDocs tests in `apps/backend/core/core-api/src/test/kotlin/swm/calender/core/api/controller/v1/team/TeamAdministrationControllerTest.kt`

### Implementation for User Story 6

- [ ] T119 [US6] Add service profile active replacement behavior in `apps/backend/match/match-domain/src/main/kotlin/swm/calender/match/service/ServiceProfilePivotService.kt`
- [ ] T120 [US6] Add team member removal and role-change behavior in `apps/backend/core/core-team-domain/src/main/kotlin/swm/calender/core/team/service/TeamAdministrationService.kt`
- [ ] T121 [US6] Add administration request DTOs in `apps/backend/core/core-api/src/main/kotlin/swm/calender/core/api/controller/v1/team/request/TeamAdministrationRequest.kt`
- [ ] T122 [US6] Add TeamAdministrationController endpoints in `apps/backend/core/core-api/src/main/kotlin/swm/calender/core/api/controller/v1/team/TeamAdministrationController.kt`
- [ ] T123 [US6] Add membership history persistence support in `apps/backend/storage/db-core/src/main/kotlin/swm/calender/storage/db/core/team/TeamMemberHistoryTable.kt`
- [ ] T124 [US6] Add team administration Flyway migration in `apps/backend/storage/db-core/src/main/resources/db/migration/V8__create_team_member_history_tables.sql`

**Checkpoint**: US6 preserves profile and membership history while enforcing OWNER safety rules.

## Final Phase: Polish And Cross-Cutting Concerns

**Purpose**: Validate contracts, docs, security, and performance across all selected stories.

- [ ] T125 Update generated API docs index in `apps/backend/core/core-api/src/docs/asciidoc/index.adoc`
- [ ] T126 Validate OpenAPI contract against implemented DTOs in `specs/001-swm-teams-mvp/contracts/openapi.yaml`
- [ ] T127 Add quickstart validation notes in `specs/001-swm-teams-mvp/quickstart.md`
- [ ] T128 Run full test suite with `./gradlew test` and record result in `specs/001-swm-teams-mvp/quickstart.md`
- [ ] T129 Run ktlint with `./gradlew ktlintCheck` and record result in `specs/001-swm-teams-mvp/quickstart.md`

## Dependencies And Execution Order

### Phase Dependencies

- Setup must complete before Foundational.
- Foundational must complete before any user story.
- P1 stories are US1, US2, and US3.
- US2 and US3 both require US1 team activation and team-scope authorization.
- US4 requires US3 campaigns and team-scope authorization. Duplicate analysis itself does not block request
  lifecycle, but the same private-data authorization rules must be in place first.
- US5 requires US4 assignments.
- US6 can start after US1 and US3.
- Final phase runs after the selected stories are complete.

### Story Dependency Order

1. US1 Team onboarding and sub-service activation
2. US2 Calendar availability and US3 Match campaign/candidate discovery can proceed after US1
3. US4 request lifecycle after US3
4. US5 feedback and test history after US4
5. US6 service pivot and team administration after US1 and US3

### Parallel Opportunities

- T002-T007 can run in parallel.
- T010-T012 and T017-T018 can run in parallel.
- Tests marked `[P]` in each story can be written before implementation in separate files.
- Domain model tasks marked `[P]` within the same story can run in parallel.
- US2 and US3 can run in parallel after US1 if teams coordinate shared `TeamId` and authorization contracts.

## Implementation Strategy

### MVP First

1. Complete Setup and Foundational phases.
2. Complete US1 and verify team onboarding independently.
3. Complete US2 and US3 as the P1 product slice.
4. Stop and validate quickstart sections 1-5 before moving to P2 stories.

### Incremental Delivery

1. US1 delivers the common team root.
2. US2 delivers Calendar value without Match dependency.
3. US3 delivers Match discovery, private candidate ideas, and duplicate analysis without request lifecycle dependency.
4. US4 adds request and assignment workflow.
5. US5 adds structured feedback and test history.
6. US6 adds lower-priority lifecycle administration.

## Notes

- `[P]` tasks must touch different files and avoid blocking dependencies.
- Story labels map to user stories in `spec.md`.
- Keep controller DTO validation separate from service/domain business rules.
- Use Exposed/Flyway only for production persistence.
- Use Kotest for all new tests.
