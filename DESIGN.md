# DESIGN.md

This file defines the visual and interaction contract for the SWM Teams web frontend.
Use it for user-facing work in `apps/web/**` and for shared frontend UI primitives under
`packages/**`.

## Product Fit

SWM Teams is an operational SaaS for Software Maestro teams. The primary jobs are team
onboarding, shared availability, Match campaign discovery, beta request tracking, and
feedback history. The interface should feel calm, fast to scan, and useful for repeated
work.

The design direction is inspired by scheduling-first products such as Cal.com and
structured workflow tools such as Airtable, adapted into a project-specific system. Do
not copy third-party logos, brand assets, proprietary fonts, or marketing layouts.

## Visual Theme

- Default to a light product interface: white canvas, soft gray surfaces, charcoal text,
  thin borders, and restrained accent colors.
- Treat the first screen as an authenticated product dashboard, not a landing page.
- Favor dense but readable operational layouts over hero sections, oversized cards, or
  decorative illustrations.
- Use actual product artifacts as visual emphasis: calendar grids, availability bands,
  campaign lists, request queues, feedback summaries, and state timelines.
- Keep decorative gradients, dark cinematic panels, glassmorphism, floating orbs, and
  bokeh backgrounds out of the app.

## Color Tokens

Use semantic tokens in code rather than hard-coded brand decisions scattered across
components.

| Token | Hex | Role |
| --- | --- | --- |
| `canvas` | `#ffffff` | Page background |
| `surface` | `#f7f8fa` | App shell and subtle section bands |
| `surfaceRaised` | `#ffffff` | Panels, cards, popovers |
| `surfaceSelected` | `#eef6ff` | Selected calendar slots and active rows |
| `border` | `#e5e7eb` | Default 1px border |
| `borderStrong` | `#cbd5e1` | Focus-adjacent or selected borders |
| `text` | `#111827` | Primary text |
| `textMuted` | `#4b5563` | Secondary text |
| `textSubtle` | `#6b7280` | Captions, metadata, placeholders |
| `primary` | `#111827` | Primary CTA and strongest active state |
| `primaryHover` | `#1f2937` | Primary pressed/hover state |
| `calendar` | `#2563eb` | Calendar feature accent |
| `match` | `#7c3aed` | Match feature accent |
| `success` | `#059669` | Successful states |
| `warning` | `#d97706` | Warnings and pending states |
| `danger` | `#dc2626` | Errors and destructive actions |
| `info` | `#0891b2` | Informational states |

Accent rules:

- Use `calendar` for availability, Google Calendar, When2meet, and schedule status.
- Use `match` for service profiles, campaigns, duplicate analysis, and beta matching.
- Use semantic colors for status only. Do not use red, yellow, or green as decorative
  section colors.
- Keep pages mostly neutral. A screen should not read as dominated by one accent hue.

## Typography

Use a system sans stack unless the chosen frontend framework already provides a local
font decision:

```css
font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
```

| Token | Size | Weight | Line Height | Use |
| --- | --- | --- | --- | --- |
| `display` | 32px | 650 | 1.2 | Page-level dashboard title only |
| `headingLg` | 24px | 650 | 1.25 | Major screen headings |
| `headingMd` | 20px | 650 | 1.3 | Panel and modal headings |
| `headingSm` | 16px | 650 | 1.35 | Card, table, and form section labels |
| `body` | 14px | 400 | 1.5 | Default UI copy |
| `bodyStrong` | 14px | 600 | 1.5 | Emphasized row or field text |
| `caption` | 12px | 500 | 1.4 | Badges, metadata, helper text |
| `mono` | 13px | 400 | 1.45 | Invite codes, ids, technical snippets |

Typography rules:

- Use letter spacing `0` across UI text.
- Do not use viewport-width font scaling.
- Keep long Korean and English labels wrapping cleanly inside buttons, tabs, and cards.
- Reserve large display text for true page headers. Dense product panels should use
  `headingSm` or `headingMd`.

## Layout

### Product Shell

- Desktop shell: left sidebar around 240px, top utility bar around 56px, main content on
  a light `surface` background.
- Mobile shell: top navigation with a menu sheet; content becomes a single column.
- Keep the primary workspace unframed. Use panels inside the workspace only when they
  group a real tool, list, form, or repeated item.
- Do not place cards inside cards.

### Spacing

Use a 4px spacing base.

| Token | Value | Use |
| --- | --- | --- |
| `xs` | 4px | Icon gaps, tight metadata |
| `sm` | 8px | Form label gaps, compact controls |
| `md` | 12px | Row gaps, badge groups |
| `lg` | 16px | Panel padding, form fields |
| `xl` | 24px | Screen section gaps |
| `xxl` | 32px | Dashboard group gaps |

Prefer compact vertical rhythm for lists and operations. Use whitespace to separate
responsibility, not to create a marketing feel.

### Responsive Behavior

- At widths below 768px, use one-column layouts, full-width controls, and bottom-safe
  spacing for primary actions.
- Tables should become stacked rows or horizontally scrollable data regions with stable
  headers.
- Calendar grids must keep stable cell dimensions. Dynamic labels, hover states, and
  loading text must not resize the grid.
- Touch targets should be at least 40px high, 44px where the control is isolated.

## Shape And Elevation

- Border radius: 6px for inputs and small controls, 8px for panels and cards, full radius
  only for avatars, status dots, and compact pills.
- Use 1px borders for hierarchy before shadows.
- Use shadows sparingly for menus, popovers, dialogs, and draggable overlays only.
- Avoid heavy shadows and layered decorative depth.

## Core Components

### Buttons

- Primary: `primary` background, white text, 40px height, 8px radius, 14px semibold.
- Secondary: white background, `border`, `text`, 40px height.
- Tertiary: transparent background with `textMuted`; use for low-emphasis actions.
- Destructive: use `danger` only for irreversible or high-risk actions.
- Icon buttons should use a recognizable icon with an accessible label and tooltip.

### Navigation

- Sidebar groups should map to user jobs: Dashboard, Calendar, Match, Requests,
  Feedback, Team Settings.
- Active navigation uses a soft selected background and a single accent indicator.
- Avoid marketing navigation labels such as Features, Solutions, or Pricing.

### Forms

- Inputs use white background, 1px `border`, 6px radius, 40px minimum height.
- Validation errors appear next to the field and use `danger` text plus a border state.
- Multi-step onboarding should show persistent progress and allow recovery from API
  errors without clearing valid input.

### Cards And Panels

- Cards represent repeated items such as teams, campaigns, requests, assignments, and
  feedback entries.
- Panels represent tool areas such as availability controls, filters, forms, and detail
  views.
- Card content order: title, status, key metadata, next action. Keep descriptions short
  and clamp long text in list views.

### Tables And Lists

- Use lists or tables for operational work: member management, campaign discovery,
  request queues, test history, and notifications.
- Include clear empty states, loading states, and error states.
- Filters should be visible near the list they affect. Prefer segmented controls,
  select menus, search inputs, checkboxes, and date controls over custom widgets.

### Badges And Status

Use status badges consistently:

- `pending`: warning tone
- `accepted`, `active`, `parsed`, `public`: success tone
- `rejected`, `canceled`, `failed`, `authRequired`: danger or warning tone based on
  required user action
- `private`, `draft`, `disabled`: neutral tone

Do not encode state by color alone. Pair badges with text and icons where useful.

### Calendar And Availability

- Calendar views should prioritize time scanning: fixed columns, clear date headers,
  current day indication, and compact event blocks.
- Availability should use simple intensity bands or count labels, not decorative heatmap
  effects that obscure details.
- Show source distinction for Google Calendar and When2meet without making the user
  decode raw provider names.
- Parsing failures should show the original link fallback and a clear repair action.

### Match Campaigns

- Campaign discovery should feel like a work queue, not a social feed.
- Use filter rails or compact toolbar filters for category, platform, mutual-beta
  availability, campaign status, and sorting.
- Campaign cards should expose service name, summary, platform tags, deadline,
  recruiting target, mutual-beta flag, and primary request action.
- Candidate ideas are private. Their UI must not resemble public campaign discovery.

### Requests, Assignments, And Feedback

- Use timeline rows for request lifecycle history.
- Make pending owner actions visually clear without overusing alerts.
- Feedback forms should be structured, scannable, and autosave-ready if the frontend
  stack supports it.
- Completed history should default to latest first and expose summaries before long text.

## Accessibility

- Meet WCAG AA contrast for text and interactive controls.
- Every icon-only control needs an accessible name.
- Keyboard focus must be visible and consistent.
- Form errors must be associated with the relevant input.
- Motion should be subtle and not required to understand state.

## Agent Implementation Rules

- Read this file before creating or modifying user-facing frontend UI.
- Prefer existing framework, component, and package-manager choices once `apps/web` is
  initialized.
- Use icon libraries already present in the app. If none exists and a React stack is
  introduced, prefer `lucide-react` for common UI icons.
- Build the usable product screen first. Do not create a marketing landing page unless
  the user explicitly asks for one.
- Keep API-dependent UI aligned with backend contracts and `swm-fullstack-contract`.
- Include loading, empty, error, disabled, and responsive states for substantial screens.
- If this design conflicts with `AGENTS.md`, follow `AGENTS.md` and update this file in
  the same change when appropriate.
