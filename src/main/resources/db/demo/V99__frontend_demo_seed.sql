-- Demo seed for frontend dashboards (idempotent via fixed UUIDs).

-- Demo admins with phone + email for availability KPIs
INSERT INTO person (id, first_name, last_name, full_name, email, phone, active, created_at, updated_at)
VALUES
  ('a1111111-0001-4000-8000-000000000001', 'Nadir', 'Almellouki', 'Nadir Almellouki', 'nadir.demo@alertops.local', '+212600000001', TRUE, NOW(), NOW()),
  ('a1111111-0002-4000-8000-000000000002', 'Nadia', 'Benchekroun', 'Nadia Benchekroun', 'nadia.demo@alertops.local', '+212600000002', TRUE, NOW(), NOW()),
  ('a1111111-0003-4000-8000-000000000003', 'Youssef', 'Tazi', 'Youssef Tazi', 'youssef.demo@alertops.local', '+212600000003', TRUE, NOW(), NOW()),
  ('a1111111-0004-4000-8000-000000000004', 'Sara', 'El Amrani', 'Sara El Amrani', 'sara.demo@alertops.local', NULL, TRUE, NOW(), NOW()),
  ('a1111111-0005-4000-8000-000000000005', 'Karim', 'Benali', 'Karim Benali', 'karim.demo@alertops.local', '+212600000005', FALSE, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET
  full_name = EXCLUDED.full_name,
  email = EXCLUDED.email,
  phone = EXCLUDED.phone,
  active = EXCLUDED.active,
  updated_at = NOW();

-- Fresh demo alerts (last 7 days)
INSERT INTO alert (
  id, problem_id, display_id, title, severity, impact_level, status, source,
  start_time, end_time, received_at, raw_payload, created_at, notification_state
) VALUES
(
  'b2222222-0001-4000-8000-000000000001',
  'DEMO-P-1001', 'P-DEMO-1001',
  'Saturation CPU détectée sur PayCore',
  'RESOURCE_CONTENTION', 'APPLICATION', 'OPEN', 'DYNATRACE',
  NOW() - INTERVAL '6 hours', NULL, NOW() - INTERVAL '6 hours',
  '{"demo":true,"title":"Saturation CPU"}'::jsonb, NOW() - INTERVAL '6 hours', 'ENVOYEE'
),
(
  'b2222222-0002-4000-8000-000000000002',
  'DEMO-P-1002', 'P-DEMO-1002',
  'Indisponibilité service Virement Instantané',
  'AVAILABILITY', 'SERVICE', 'OPEN', 'DYNATRACE',
  NOW() - INTERVAL '1 day', NULL, NOW() - INTERVAL '1 day',
  '{"demo":true,"title":"Indisponibilité"}'::jsonb, NOW() - INTERVAL '1 day', 'EN_COURS'
),
(
  'b2222222-0003-4000-8000-000000000003',
  'DEMO-P-1003', 'P-DEMO-1003',
  'Latence élevée sur Amplitude ABTchad',
  'PERFORMANCE', 'APPLICATION', 'CLOSED', 'DYNATRACE',
  NOW() - INTERVAL '3 days', NOW() - INTERVAL '2 days', NOW() - INTERVAL '3 days',
  '{"demo":true,"title":"Latence"}'::jsonb, NOW() - INTERVAL '3 days', 'ENVOYEE'
),
(
  'b2222222-0004-4000-8000-000000000004',
  'DEMO-P-1004', 'P-DEMO-1004',
  'Erreur batch Solarwinds',
  'ERROR', 'INFRASTRUCTURE', 'OPEN', 'DYNATRACE',
  NOW() - INTERVAL '12 hours', NULL, NOW() - INTERVAL '12 hours',
  '{"demo":true,"title":"Erreur batch"}'::jsonb, NOW() - INTERVAL '12 hours', 'ECHOUEE'
),
(
  'b2222222-0005-4000-8000-000000000005',
  'DEMO-P-1005', 'P-DEMO-1005',
  'Alerte custom FortiFy',
  'CUSTOM_ALERT', 'SECURITY', 'OPEN', 'DYNATRACE',
  NOW() - INTERVAL '2 days', NULL, NOW() - INTERVAL '2 days',
  '{"demo":true,"title":"FortiFy"}'::jsonb, NOW() - INTERVAL '2 days', 'EN_ATTENTE'
)
ON CONFLICT (problem_id) DO UPDATE SET
  title = EXCLUDED.title,
  severity = EXCLUDED.severity,
  status = EXCLUDED.status,
  notification_state = EXCLUDED.notification_state,
  received_at = EXCLUDED.received_at,
  end_time = EXCLUDED.end_time;

INSERT INTO alert_llm_analysis (
  id, alert_id, provider, prompt_version, duration_ms, input_tokens, output_tokens,
  status, category, problem_type, confidence, matched_solution, matched_domain,
  summary, probable_cause, requires_human_validation, created_at
) VALUES
(
  'c3333333-0001-4000-8000-000000000001', 'b2222222-0001-4000-8000-000000000001',
  'demo', 'v2', 1200, 800, 220, 'SUCCESS', 'INFRA', 'CPU_SATURATION', 0.910000,
  'PayCore', 'Monétique', 'CPU saturé sur PayCore', 'Charge transactionnelle', FALSE, NOW() - INTERVAL '6 hours'
),
(
  'c3333333-0002-4000-8000-000000000002', 'b2222222-0002-4000-8000-000000000002',
  'demo', 'v2', 1800, 900, 260, 'LOW_CONFIDENCE', 'PAIEMENT', 'SERVICE_DOWN', 0.540000,
  'BKS - Virements Instantanés  (BKS)', 'BKS Paiement', 'Service indisponible', 'Cause incertaine', TRUE, NOW() - INTERVAL '1 day'
),
(
  'c3333333-0003-4000-8000-000000000003', 'b2222222-0003-4000-8000-000000000003',
  'demo', 'v2', 950, 700, 180, 'SUCCESS', 'INFRA', 'LATENCY', 0.870000,
  'Amplitude ABTchad (AITA)', 'SI Filiales AITA', 'Latence élevée', 'Réseau filiale', FALSE, NOW() - INTERVAL '3 days'
),
(
  'c3333333-0004-4000-8000-000000000004', 'b2222222-0004-4000-8000-000000000004',
  'demo', 'v2', 2100, 1000, 300, 'FALLBACK', 'INFRA', 'BATCH_ERROR', 0.420000,
  'Solarwinds', 'Infrastructure', 'Erreur batch', 'Fallback LLM', TRUE, NOW() - INTERVAL '12 hours'
),
(
  'c3333333-0005-4000-8000-000000000005', 'b2222222-0005-4000-8000-000000000005',
  'demo', 'v2', 1100, 750, 190, 'SUCCESS', 'SECURITE', 'APP_SCAN', 0.780000,
  'FortiFy', 'Sécurité', 'Alerte sécurité applicative', 'Scan Fortify', FALSE, NOW() - INTERVAL '2 days'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO routing_execution (
  id, alert_id, routing_policy_id, selected_person_id, current_step, routing_status,
  started_at, finished_at, next_escalation_at, candidate_index
) VALUES
(
  'd4444444-0001-4000-8000-000000000001', 'b2222222-0001-4000-8000-000000000001',
  'eeeeeeee-0001-4000-8000-000000000001', 'a1111111-0001-4000-8000-000000000001',
  1, 'COMPLETED', NOW() - INTERVAL '5 hours 50 minutes', NOW() - INTERVAL '5 hours', NULL, 0
),
(
  'd4444444-0002-4000-8000-000000000002', 'b2222222-0002-4000-8000-000000000002',
  'eeeeeeee-0001-4000-8000-000000000001', 'a1111111-0002-4000-8000-000000000002',
  2, 'AWAITING_ESCALATION', NOW() - INTERVAL '23 hours', NULL, NOW() + INTERVAL '10 minutes', 1
),
(
  'd4444444-0003-4000-8000-000000000003', 'b2222222-0004-4000-8000-000000000004',
  'eeeeeeee-0001-4000-8000-000000000001', 'a1111111-0003-4000-8000-000000000003',
  1, 'IN_PROGRESS', NOW() - INTERVAL '11 hours', NULL, NOW() + INTERVAL '5 minutes', 0
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO routing_history (id, routing_execution_id, target_person_id, action, action_time, details) VALUES
  ('e5555555-0001-4000-8000-000000000001', 'd4444444-0001-4000-8000-000000000001', 'a1111111-0001-4000-8000-000000000001', 'NOTIFY', NOW() - INTERVAL '5 hours 45 minutes', 'Email envoyé'),
  ('e5555555-0002-4000-8000-000000000002', 'd4444444-0002-4000-8000-000000000002', 'a1111111-0001-4000-8000-000000000001', 'VOICE_CALL', NOW() - INTERVAL '22 hours', 'Appel VoIP initial'),
  ('e5555555-0003-4000-8000-000000000003', 'd4444444-0002-4000-8000-000000000002', 'a1111111-0002-4000-8000-000000000002', 'ESCALATE', NOW() - INTERVAL '20 hours', 'Escalade TAM'),
  ('e5555555-0004-4000-8000-000000000004', 'd4444444-0003-4000-8000-000000000003', 'a1111111-0003-4000-8000-000000000003', 'NOTIFY', NOW() - INTERVAL '10 hours', 'SMS Kafka produit')
ON CONFLICT (id) DO NOTHING;

INSERT INTO notification (
  id, alert_id, routing_execution_id, template_id, notification_type, notification_status,
  call_mode, priority, created_at
) VALUES
  ('f6666666-0001-4000-8000-000000000001', 'b2222222-0001-4000-8000-000000000001', 'd4444444-0001-4000-8000-000000000001', '11111111-0001-4000-8000-000000000001', 'EMAIL', 'SENT', 'AUTO', 1, NOW() - INTERVAL '5 hours 40 minutes'),
  ('f6666666-0002-4000-8000-000000000002', 'b2222222-0001-4000-8000-000000000001', 'd4444444-0001-4000-8000-000000000001', NULL, 'SMS', 'SENT', 'AUTO', 1, NOW() - INTERVAL '5 hours 35 minutes'),
  ('f6666666-0003-4000-8000-000000000003', 'b2222222-0002-4000-8000-000000000002', 'd4444444-0002-4000-8000-000000000002', NULL, 'VOIP', 'SENT', 'AUTO', 2, NOW() - INTERVAL '22 hours'),
  ('f6666666-0004-4000-8000-000000000004', 'b2222222-0002-4000-8000-000000000002', 'd4444444-0002-4000-8000-000000000002', NULL, 'VOIP', 'FAILED', 'AUTO', 2, NOW() - INTERVAL '20 hours'),
  ('f6666666-0005-4000-8000-000000000005', 'b2222222-0004-4000-8000-000000000004', 'd4444444-0003-4000-8000-000000000003', NULL, 'SMS', 'FAILED', 'AUTO', 1, NOW() - INTERVAL '10 hours'),
  ('f6666666-0006-4000-8000-000000000006', 'b2222222-0004-4000-8000-000000000004', 'd4444444-0003-4000-8000-000000000003', '11111111-0001-4000-8000-000000000001', 'EMAIL', 'DEFERRED', 'AUTO', 1, NOW() - INTERVAL '9 hours'),
  ('f6666666-0007-4000-8000-000000000007', 'b2222222-0003-4000-8000-000000000003', NULL, NULL, 'VOIP', 'SENT', 'AUTO', 0, NOW() - INTERVAL '2 days 12 hours'),
  ('f6666666-0008-4000-8000-000000000008', 'b2222222-0005-4000-8000-000000000005', NULL, '11111111-0001-4000-8000-000000000001', 'EMAIL', 'PENDING', 'AUTO', 0, NOW() - INTERVAL '1 day 20 hours')
ON CONFLICT (id) DO NOTHING;

INSERT INTO notification_recipient (id, notification_id, person_id, channel, destination, recipient_order) VALUES
  ('aa777777-0001-4000-8000-000000000001', 'f6666666-0001-4000-8000-000000000001', 'a1111111-0001-4000-8000-000000000001', 'EMAIL', 'nadir.demo@alertops.local', 0),
  ('aa777777-0002-4000-8000-000000000002', 'f6666666-0002-4000-8000-000000000002', 'a1111111-0001-4000-8000-000000000001', 'SMS', '+212600000001', 0),
  ('aa777777-0003-4000-8000-000000000003', 'f6666666-0003-4000-8000-000000000003', 'a1111111-0001-4000-8000-000000000001', 'VOIP', '+212600000001', 0),
  ('aa777777-0004-4000-8000-000000000004', 'f6666666-0004-4000-8000-000000000004', 'a1111111-0002-4000-8000-000000000002', 'VOIP', '+212600000002', 0),
  ('aa777777-0005-4000-8000-000000000005', 'f6666666-0005-4000-8000-000000000005', 'a1111111-0003-4000-8000-000000000003', 'SMS', '+212600000003', 0),
  ('aa777777-0006-4000-8000-000000000006', 'f6666666-0006-4000-8000-000000000006', 'a1111111-0004-4000-8000-000000000004', 'EMAIL', 'sara.demo@alertops.local', 0),
  ('aa777777-0007-4000-8000-000000000007', 'f6666666-0007-4000-8000-000000000007', 'a1111111-0002-4000-8000-000000000002', 'VOIP', '+212600000002', 0),
  ('aa777777-0008-4000-8000-000000000008', 'f6666666-0008-4000-8000-000000000008', 'a1111111-0004-4000-8000-000000000004', 'EMAIL', 'sara.demo@alertops.local', 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO notification_attempt (id, recipient_id, attempt_number, provider, provider_message_id, status, error_message, started_at, finished_at) VALUES
  ('bb888888-0001-4000-8000-000000000001', 'aa777777-0001-4000-8000-000000000001', 1, 'smtp', 'mail-1', 'SENT', NULL, NOW() - INTERVAL '5 hours 40 minutes', NOW() - INTERVAL '5 hours 40 minutes'),
  ('bb888888-0002-4000-8000-000000000002', 'aa777777-0003-4000-8000-000000000003', 1, 'local-voip', 'voip-1', 'SENT', NULL, NOW() - INTERVAL '22 hours', NOW() - INTERVAL '22 hours'),
  ('bb888888-0003-4000-8000-000000000003', 'aa777777-0004-4000-8000-000000000004', 1, 'local-voip', NULL, 'FAILED', 'No answer', NOW() - INTERVAL '20 hours', NOW() - INTERVAL '20 hours'),
  ('bb888888-0004-4000-8000-000000000004', 'aa777777-0005-4000-8000-000000000005', 1, 'kafka-sms', NULL, 'FAILED', 'Broker timeout', NOW() - INTERVAL '10 hours', NOW() - INTERVAL '10 hours')
ON CONFLICT (id) DO NOTHING;

INSERT INTO resolution_check (
  id, alert_id, external_problem_id, status, attempt_count, next_check_at, started_at, finished_at, last_dynatrace_state, last_error
) VALUES
  ('cc999999-0001-4000-8000-000000000001', 'b2222222-0001-4000-8000-000000000001', 'DEMO-P-1001', 'ACTIVE', 2, NOW() + INTERVAL '5 minutes', NOW() - INTERVAL '5 hours', NULL, 'OPEN', NULL),
  ('cc999999-0002-4000-8000-000000000002', 'b2222222-0003-4000-8000-000000000003', 'DEMO-P-1003', 'RESOLVED', 4, NULL, NOW() - INTERVAL '2 days 20 hours', NOW() - INTERVAL '2 days', 'CLOSED', NULL),
  ('cc999999-0003-4000-8000-000000000003', 'b2222222-0002-4000-8000-000000000002', 'DEMO-P-1002', 'EXPIRED', 8, NULL, NOW() - INTERVAL '20 hours', NOW() - INTERVAL '2 hours', 'OPEN', 'Max attempts reached')
ON CONFLICT (id) DO NOTHING;

INSERT INTO audit_log (
  id, actor_person_id, alert_id, llm_analysis_id, routing_execution_id, notification_id,
  action, entity_name, entity_id, description, correlation_id, created_at
) VALUES
  ('dd000000-0001-4000-8000-000000000001', NULL, 'b2222222-0001-4000-8000-000000000001', NULL, NULL, NULL, 'ALERT_RECEIVED', 'Alert', 'b2222222-0001-4000-8000-000000000001', 'Alerte reçue depuis Dynatrace', 'b2222222-0001-4000-8000-000000000001', NOW() - INTERVAL '6 hours'),
  ('dd000000-0002-4000-8000-000000000002', NULL, 'b2222222-0001-4000-8000-000000000001', 'c3333333-0001-4000-8000-000000000001', NULL, NULL, 'CLASSIFICATION_COMPLETED', 'AlertLlmAnalysis', 'c3333333-0001-4000-8000-000000000001', 'Classification IA réussie (INFRA)', 'b2222222-0001-4000-8000-000000000001', NOW() - INTERVAL '5 hours 55 minutes'),
  ('dd000000-0003-4000-8000-000000000003', NULL, 'b2222222-0001-4000-8000-000000000001', NULL, NULL, NULL, 'RULE_EVALUATED', 'BusinessRule', NULL, 'Règles métier évaluées', 'b2222222-0001-4000-8000-000000000001', NOW() - INTERVAL '5 hours 52 minutes'),
  ('dd000000-0004-4000-8000-000000000004', NULL, 'b2222222-0001-4000-8000-000000000001', NULL, 'd4444444-0001-4000-8000-000000000001', NULL, 'ROUTING_DECIDED', 'RoutingExecution', 'd4444444-0001-4000-8000-000000000001', 'Routage vers Nadir Almellouki', 'b2222222-0001-4000-8000-000000000001', NOW() - INTERVAL '5 hours 50 minutes'),
  ('dd000000-0005-4000-8000-000000000005', NULL, 'b2222222-0001-4000-8000-000000000001', NULL, NULL, 'f6666666-0001-4000-8000-000000000001', 'NOTIFICATION_ATTEMPTED', 'Notification', 'f6666666-0001-4000-8000-000000000001', 'Email technique envoyé', 'b2222222-0001-4000-8000-000000000001', NOW() - INTERVAL '5 hours 40 minutes'),
  ('dd000000-0006-4000-8000-000000000006', NULL, 'b2222222-0002-4000-8000-000000000002', NULL, 'd4444444-0002-4000-8000-000000000002', NULL, 'ESCALATION_PROCESSED', 'RoutingExecution', 'd4444444-0002-4000-8000-000000000002', 'Escalade vers superviseur', 'b2222222-0002-4000-8000-000000000002', NOW() - INTERVAL '20 hours'),
  ('dd000000-0007-4000-8000-000000000007', NULL, 'b2222222-0003-4000-8000-000000000003', NULL, NULL, NULL, 'RESOLUTION_CHECK_COMPLETED', 'ResolutionCheck', 'cc999999-0002-4000-8000-000000000002', 'Problème Dynatrace résolu', 'b2222222-0003-4000-8000-000000000003', NOW() - INTERVAL '2 days')
ON CONFLICT (id) DO NOTHING;

INSERT INTO system_event (
  id, alert_id, llm_analysis_id, source_module, severity, event_type, message, correlation_id, created_at
) VALUES
  ('ee111111-0001-4000-8000-000000000001', 'b2222222-0004-4000-8000-000000000004', NULL, 'notification', 'WARN', 'SMS_FAILED', 'Échec production Kafka SMS', 'b2222222-0004-4000-8000-000000000004', NOW() - INTERVAL '10 hours'),
  ('ee111111-0002-4000-8000-000000000002', 'b2222222-0002-4000-8000-000000000002', NULL, 'routing', 'INFO', 'ESCALATION_DUE', 'Prochaine escalade planifiée', 'b2222222-0002-4000-8000-000000000002', NOW() - INTERVAL '19 hours')
ON CONFLICT (id) DO NOTHING;
