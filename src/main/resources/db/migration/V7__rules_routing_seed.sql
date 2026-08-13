-- Origin discriminant for default vs Ops-configured rules/policies + V0 default seeds.

ALTER TABLE business_rule
    ADD COLUMN IF NOT EXISTS rule_origin VARCHAR(20) NOT NULL DEFAULT 'CONFIGURED';

ALTER TABLE routing_policy
    ADD COLUMN IF NOT EXISTS policy_origin VARCHAR(20) NOT NULL DEFAULT 'CONFIGURED';

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_business_rule_origin') THEN
        ALTER TABLE business_rule
            ADD CONSTRAINT chk_business_rule_origin
                CHECK (rule_origin IN ('DEFAULT', 'CONFIGURED'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_routing_policy_origin') THEN
        ALTER TABLE routing_policy
            ADD CONSTRAINT chk_routing_policy_origin
                CHECK (policy_origin IN ('DEFAULT', 'CONFIGURED'));
    END IF;
END $$;

-- Default business rule: low LLM confidence → human validation
INSERT INTO business_rule (id, code, name, description, rule_evaluation_priority, enabled, stop_on_match, rule_origin)
SELECT 'aaaaaaaa-0001-4000-8000-000000000001', 'DEFAULT-LOW-CONFIDENCE',
       'Faible confiance IA → validation humaine',
       'Si la confiance de classification est inférieure à 70 %, demander une validation humaine avant routage.',
       10, true, true, 'DEFAULT'
WHERE NOT EXISTS (SELECT 1 FROM business_rule WHERE code = 'DEFAULT-LOW-CONFIDENCE');

INSERT INTO rule_condition_group (id, rule_id, block_type, logical_operator, execution_order)
SELECT 'bbbbbbbb-0001-4000-8000-000000000001', 'aaaaaaaa-0001-4000-8000-000000000001', 'CONDITION', 'AND', 0
WHERE NOT EXISTS (SELECT 1 FROM rule_condition_group WHERE id = 'bbbbbbbb-0001-4000-8000-000000000001');

INSERT INTO rule_condition (id, group_id, field_name, operator, expected_value, value_type, condition_order)
SELECT 'cccccccc-0001-4000-8000-000000000001', 'bbbbbbbb-0001-4000-8000-000000000001',
       'llm.confidence', 'LESS_THAN', '0.70', 'NUMBER', 0
WHERE NOT EXISTS (SELECT 1 FROM rule_condition WHERE id = 'cccccccc-0001-4000-8000-000000000001');

INSERT INTO rule_action (id, rule_id, action_type, action_value, execution_order)
SELECT 'dddddddd-0001-4000-8000-000000000001', 'aaaaaaaa-0001-4000-8000-000000000001',
       'REQUEST_HUMAN_VALIDATION', null, 0
WHERE NOT EXISTS (SELECT 1 FROM rule_action WHERE id = 'dddddddd-0001-4000-8000-000000000001');

-- Default business rule: LLM fallback
INSERT INTO business_rule (id, code, name, description, rule_evaluation_priority, enabled, stop_on_match, rule_origin)
SELECT 'aaaaaaaa-0002-4000-8000-000000000002', 'DEFAULT-LLM-FALLBACK',
       'Classification fallback → validation humaine',
       'Si la classification LLM est en fallback, demander une validation humaine.',
       20, true, true, 'DEFAULT'
WHERE NOT EXISTS (SELECT 1 FROM business_rule WHERE code = 'DEFAULT-LLM-FALLBACK');

INSERT INTO rule_condition_group (id, rule_id, block_type, logical_operator, execution_order)
SELECT 'bbbbbbbb-0002-4000-8000-000000000002', 'aaaaaaaa-0002-4000-8000-000000000002', 'CONDITION', 'AND', 0
WHERE NOT EXISTS (SELECT 1 FROM rule_condition_group WHERE id = 'bbbbbbbb-0002-4000-8000-000000000002');

INSERT INTO rule_condition (id, group_id, field_name, operator, expected_value, value_type, condition_order)
SELECT 'cccccccc-0002-4000-8000-000000000002', 'bbbbbbbb-0002-4000-8000-000000000002',
       'llm.status', 'EQUALS', 'FALLBACK', 'STRING', 0
WHERE NOT EXISTS (SELECT 1 FROM rule_condition WHERE id = 'cccccccc-0002-4000-8000-000000000002');

INSERT INTO rule_action (id, rule_id, action_type, action_value, execution_order)
SELECT 'dddddddd-0002-4000-8000-000000000002', 'aaaaaaaa-0002-4000-8000-000000000002',
       'REQUEST_HUMAN_VALIDATION', null, 0
WHERE NOT EXISTS (SELECT 1 FROM rule_action WHERE id = 'dddddddd-0002-4000-8000-000000000002');

-- Default catch-all business rule: trigger routing
INSERT INTO business_rule (id, code, name, description, rule_evaluation_priority, enabled, stop_on_match, rule_origin)
SELECT 'aaaaaaaa-0003-4000-8000-000000000003', 'DEFAULT-STANDARD-ROUTING',
       'Routage standard par défaut',
       'Comportement normal V0 : sélectionner le contexte métier résolu et déclencher le routage.',
       1000, true, true, 'DEFAULT'
WHERE NOT EXISTS (SELECT 1 FROM business_rule WHERE code = 'DEFAULT-STANDARD-ROUTING');

INSERT INTO rule_condition_group (id, rule_id, block_type, logical_operator, execution_order)
SELECT 'bbbbbbbb-0003-4000-8000-000000000003', 'aaaaaaaa-0003-4000-8000-000000000003', 'CONDITION', 'AND', 0
WHERE NOT EXISTS (SELECT 1 FROM rule_condition_group WHERE id = 'bbbbbbbb-0003-4000-8000-000000000003');

INSERT INTO rule_action (id, rule_id, action_type, action_value, execution_order)
SELECT 'dddddddd-0003-4000-8000-000000000001', 'aaaaaaaa-0003-4000-8000-000000000003',
       'SELECT_BUSINESS_CONTEXT', null, 0
WHERE NOT EXISTS (SELECT 1 FROM rule_action WHERE id = 'dddddddd-0003-4000-8000-000000000001');

INSERT INTO rule_action (id, rule_id, action_type, action_value, execution_order)
SELECT 'dddddddd-0003-4000-8000-000000000002', 'aaaaaaaa-0003-4000-8000-000000000003',
       'TRIGGER_ROUTING', null, 1
WHERE NOT EXISTS (SELECT 1 FROM rule_action WHERE id = 'dddddddd-0003-4000-8000-000000000002');

-- Default routing policy
INSERT INTO routing_policy (id, code, name, description, enabled, priority, policy_origin)
SELECT 'eeeeeeee-0001-4000-8000-000000000001', 'DEFAULT-VOICE-ESCALATION',
       'Escalade voix par défaut (V0)',
       'Appel TAM → relance → personne suivante → superviseur domaine.',
       true, 1000, 'DEFAULT'
WHERE NOT EXISTS (SELECT 1 FROM routing_policy WHERE code = 'DEFAULT-VOICE-ESCALATION');

INSERT INTO routing_step (id, routing_policy_id, step_order, action_type, target_role, target_unit_type, channel, delay_after_seconds)
SELECT 'ffffffff-0001-4000-8000-000000000001', 'eeeeeeee-0001-4000-8000-000000000001',
       1, 'VOICE_CALL', 'TAM', 'SOLUTION', 'VOIP', 300
WHERE NOT EXISTS (SELECT 1 FROM routing_step WHERE id = 'ffffffff-0001-4000-8000-000000000001');

INSERT INTO routing_step (id, routing_policy_id, step_order, action_type, target_role, target_unit_type, channel, delay_after_seconds)
SELECT 'ffffffff-0002-4000-8000-000000000002', 'eeeeeeee-0001-4000-8000-000000000001',
       2, 'VOICE_RETRY', 'TAM', 'SOLUTION', 'VOIP', 120
WHERE NOT EXISTS (SELECT 1 FROM routing_step WHERE id = 'ffffffff-0002-4000-8000-000000000002');

INSERT INTO routing_step (id, routing_policy_id, step_order, action_type, target_role, target_unit_type, channel, delay_after_seconds)
SELECT 'ffffffff-0003-4000-8000-000000000003', 'eeeeeeee-0001-4000-8000-000000000001',
       3, 'NEXT_PERSON', 'TAM', 'SOLUTION', 'VOIP', 60
WHERE NOT EXISTS (SELECT 1 FROM routing_step WHERE id = 'ffffffff-0003-4000-8000-000000000003');

INSERT INTO routing_step (id, routing_policy_id, step_order, action_type, target_role, target_unit_type, channel, delay_after_seconds)
SELECT 'ffffffff-0004-4000-8000-000000000004', 'eeeeeeee-0001-4000-8000-000000000001',
       4, 'NOTIFY_SUPERVISOR', 'MANAGER', 'DOMAIN', 'VOIP', 0
WHERE NOT EXISTS (SELECT 1 FROM routing_step WHERE id = 'ffffffff-0004-4000-8000-000000000004');
