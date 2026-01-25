# TICKET-009: UX with Qute + HTMX

## Goal
Deliver a server-rendered UI for incident list/detail, diagnosis panel, and runbook editing.

## Scope
- Qute templates for incident list/detail, diagnosis panel, runbook list/edit.
- HTMX actions for diagnose, verify, and runbook updates.
- Consistent layout and navigation across pages.

## Acceptance criteria
- Pages render with server-side data and progressive enhancement.
- HTMX actions return partials without full page reload.
- UI flows align with data-oriented service results.

## Dependencies
- TICKET-003, TICKET-004, TICKET-005, TICKET-008.
