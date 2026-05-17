## ADDED Requirements

### Requirement: Incident response runbooks

The system SHALL provide runbooks for common production incidents. Runbooks SHALL be maintained in the repository alongside the code and updated after each incident.

#### Scenario: Runbook structure

- **WHEN** an incident runbook is created
- **THEN** it SHALL contain: symptoms, severity classification, step-by-step recovery steps, escalation path, verification checklist, post-mortem template

#### Scenario: Edge node batch offline runbook

- **WHEN** multiple edge nodes go offline simultaneously
- **THEN** the runbook SHALL define: verify network connectivity → confirm scope of impact → failover to standby nodes → investigate root cause per node → verify recovery → document findings

#### Scenario: AI false alarm storm runbook

- **WHEN** AI behavior detection produces an abnormally high false positive rate
- **THEN** the runbook SHALL define: activate Kill Switch for the affected AI feature → collect misclassified samples → rollback AI model → validate fix → gradually re-enable via feature flag

### Requirement: Synthetic monitoring

The system SHALL implement synthetic monitoring that simulates end-to-end user flows from an external perspective. Failed checks SHALL trigger immediate alerts.

#### Scenario: E2E health check

- **WHEN** the synthetic monitor runs
- **THEN** it SHALL execute the flow: login → fetch camera list → start video stream → query recordings → logout
- **AND** it SHALL run every 5 minutes
- **AND** any step failure SHALL trigger a P0 (critical) alert
