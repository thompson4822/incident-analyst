# TICKET-009: UX with Qute + HTMX (COMPLETED)

## Goal
Deliver a server-rendered UI for incident list/detail, diagnosis panel, and runbook editing.

## Scope
- [x] Qute templates for incident list/detail, diagnosis panel, runbook list/edit.
- [x] HTMX actions for diagnose, verify, and runbook updates.
- [x] Consistent layout and navigation across pages.

## Acceptance criteria
- [x] Pages render with server-side data and progressive enhancement.
- [x] HTMX actions return partials without full page reload.
- [x] UI flows align with data-oriented service results.

## Progress
- ✅ Base layout with HTMX 1.9.12, Tailwind CSS, DaisyUI 4.12.10
- ✅ Navbar with consistent navigation (Incidents, Diagnoses, Runbooks)
- ✅ Incident list and detail templates with filtering
- ✅ Diagnosis panel template with verify/apply actions
- ✅ Runbook list and edit templates
- ✅ Home dashboard with auto-refresh fragments
- ✅ Remediation progress panel with polling
- ✅ Toast notification system
- ✅ HTMX polling for real-time updates

## Implementation Details

### Template Files

#### Layout
- `src/main/resources/templates/layout/base.html` - Base layout with HTMX, Tailwind, DaisyUI
- `src/main/resources/templates/layout/navbar.html` - Navigation bar

#### Incident
- `src/main/resources/templates/incident/list.html` - Incident list with filtering
- `src/main/resources/templates/incident/detail.html` - Incident detail with actions

#### Diagnosis
- `src/main/resources/templates/diagnosis/diagnosis-panel.html` - Diagnosis display panel

#### Runbook
- `src/main/resources/templates/runbook/runbook-list.html` - Runbook list
- `src/main/resources/templates/runbook/runbook-edit.html` - Runbook editor

#### Home Dashboard
- `src/main/resources/templates/home.html` - Main dashboard
- `src/main/resources/templates/home/fragments/stats.html` - Stats fragment
- `src/main/resources/templates/home/fragments/active-incident.html` - Active incident
- `src/main/resources/templates/home/fragments/incident-table.html` - Incident table
- `src/main/resources/templates/home/fragments/recent-incidents.html` - Recent incidents
- `src/main/resources/templates/home/fragments/runbook-sidebar.html` - Runbook sidebar

#### Remediation
- `src/main/resources/templates/remediation/progress-panel.html` - Progress with polling

### HTMX Actions

| Action | Endpoint | Trigger |
|--------|----------|---------|
| Diagnose | `POST /incidents/{id}/diagnose` | Button click |
| Verify | `POST /incidents/{id}/verify-diagnosis` | Button click |
| Acknowledge | `POST /incidents/{id}/acknowledge` | Button click |
| Resolve | `POST /incidents/{id}/resolve` | Form submit |
| Remediate | `POST /incidents/{id}/remediate` | Button click |
| Filter/Search | `GET /incidents` | Input change |
| Progress Poll | `GET /incidents/{id}/remediation/progress` | Every 2s |

### Auto-Refresh Polling

| Fragment | Interval |
|----------|----------|
| Stats | 30s |
| Incident Table | 30s |
| Active Incident | 15s |
| Runbook Sidebar | 60s |
| Remediation Progress | 2s |

### UI Features
- DaisyUI components (buttons, cards, badges, modals)
- Toast notifications for action feedback
- HTMX transition effects
- Responsive design with Tailwind CSS
- Content negotiation (HTML for browsers, JSON for API)

## Dependencies
- TICKET-003, TICKET-004, TICKET-005, TICKET-008.
