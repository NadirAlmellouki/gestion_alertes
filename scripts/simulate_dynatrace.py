#!/usr/bin/env python3
"""
Simulateur de Webhooks Dynatrace réalistes pour AlertOps
=========================================================
Ce script génère et envoie des payloads d'alertes Dynatrace réalistes
au point d'ingestion AlertOps (/api/v1/ingestion/dynatrace).

Fonctionnalités :
- Récupère dynamiquement les solutions, domaines, entités et contacts réels de la BDD PostgreSQL.
- Génère 6 scénarios réalistes (P0 Critique, Performance, Contention CPU/Mémoire, Sécurité, Réseau, Résolution).
- Sélection aléatoire par défaut ou choix interactif/spécifique (--scenario).
- Format conforme aux webhooks Dynatrace API v2 (affectedEntities, rootCause, entityTags, evidenceDetails).
- Compatible sans dépendance externe (utilise la bibliothèque standard Python 3).


/////////////////////////////////////////////
***comment lancer le script://///////////////
/////////////////////////////////////////////

// Depuis le dossier du backend (gestion_alertes)
// Scénario aléatoire (recommandé pour les tests répétitifs)

python scripts/simulate_dynatrace.py

// Forcer un scénario précis
python scripts/simulate_dynatrace.py --scenario 1   # P0 Indisponibilité critique
python scripts/simulate_dynatrace.py --scenario 2   # Dégradation de performance
python scripts/simulate_dynatrace.py --scenario 3   # Saturation CPU / Mémoire
python scripts/simulate_dynatrace.py --scenario 4   # Anomalie Sécurité (SECOPS)
python scripts/simulate_dynatrace.py --scenario 5   # Perte réseau (Télécom)
python scripts/simulate_dynatrace.py --scenario 6   # Résolution d'incident

// Cibler une solution applicative spécifique
python scripts/simulate_dynatrace.py --scenario 1 --solution PayCore
python scripts/simulate_dynatrace.py --scenario 4 --solution CyberArk
python scripts/simulate_dynatrace.py --scenario 3 --solution MUREX

// Stress test : 5 alertes espacées de 3s
python scripts/simulate_dynatrace.py --repeat 5 --interval 3

// Voir le payload JSON sans l'envoyer (dry-run)
python scripts/simulate_dynatrace.py --scenario 1 --dry-run

// Lister tous les scénarios et solutions disponibles
python scripts/simulate_dynatrace.py --list

"""

import argparse
import json
import os
import random
import subprocess
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

DEFAULT_ENDPOINT = "http://localhost:8085/api/v1/ingestion/dynatrace"
DEFAULT_TOKEN = os.environ.get("DYNATRACE_INGESTION_TOKEN", "dev-ingestion-token")

# -----------------------------------------------------------------------------
# Catalogue de secours (utilisé si PostgreSQL n'est pas joignable directement)
# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------
# Catalogue de secours (solutions réelles de la BDD alertops)
# -----------------------------------------------------------------------------
FALLBACK_SOLUTIONS = [
    {
        "name": "MUREX",
        "domain": "Salle des Marchés",
        "pole": "Banque Financement d'Investissement Groupe (BFIG)",
        "entity": "Core Processing & Services",
        "psi": "P0",
        "tam": "Brahim Ben Khadaj",
        "tech_admin": "Achraf Jerraf",
        "host": "srv-murex-prod-01.bank.internal",
    },
    {
        "name": "SWIFT",
        "domain": "Paiement international",
        "pole": "Trade Finance & Cash Management",
        "entity": "Core Processing & Services",
        "psi": "P1",
        "tam": "Yassine Moustiphi",
        "tech_admin": "Kamal Morjane",
        "host": "swift-gateway-prod-01.bank.internal",
    },
    {
        "name": "ADFS",
        "domain": "Administration & Expertise",
        "pole": "SAI",
        "entity": "Infrastructure & Production",
        "psi": "P0",
        "tam": "Yassine Moustiphi",
        "tech_admin": "Hicham Haddad",
        "host": "srv-adfs-auth-01.bank.internal",
    },
    {
        "name": "Wallix",
        "domain": "Sécurité Des Accès",
        "pole": "Sécurité Données, Accès & Applications",
        "entity": "Sécurité Opérationnelle Groupe",
        "psi": "P0",
        "tam": "Kamal Morjane",
        "tech_admin": "Idriss Elmoussaouiti",
        "host": "vault-wallix-prod.bank.internal",
    },
    {
        "name": "Solarwinds",
        "domain": "Télécom",
        "pole": "Infrastructure",
        "entity": "Infrastructure & Production",
        "psi": "P0",
        "tam": "Yassine Moustiphi",
        "tech_admin": "Hicham Haddad",
        "host": "switch-core-datacenter.awb.ma",
    },
    {
        "name": "EXCHANGE",
        "domain": "Administration & Expertise",
        "pole": "SAI",
        "entity": "Infrastructure & Production",
        "psi": "P0",
        "tam": "Yassine Moustiphi",
        "tech_admin": "Hamza Glioui",
        "host": "mail-cas-prod-01.bank.internal",
    },
    {
        "name": "Keycloak Interne",
        "domain": "Intégration",
        "pole": "SAI",
        "entity": "Infrastructure & Production",
        "psi": "P0",
        "tam": "Yassine Moustiphi",
        "tech_admin": "Amine Sadik",
        "host": "iam-keycloak-prod-01.bank.internal",
    },
    {
        "name": "Tenable Identity Exposure",
        "domain": "Sécurité Des Accès",
        "pole": "Sécurité Données, Accès & Applications",
        "entity": "Sécurité Opérationnelle Groupe",
        "psi": "P0",
        "tam": "Kamal Morjane",
        "tech_admin": "Idriss Elmoussaouiti",
        "host": "sec-tenable-prod-01.bank.internal",
    },
]


def fetch_database_context():
    """Tente d'extraire les solutions réelles depuis PostgreSQL via docker exec."""
    containers_to_try = [
        "alertops-postgres",
        "gestion_alertes-postgres-1",
        "postgres",
        "alertops_postgres_1"
    ]
    
    query = """
    SELECT s.name,
           COALESCE(d.name, 'Général') AS domain,
           COALESCE(p.name, 'Infrastructure') AS pole,
           COALESCE(e.name, 'Production') AS entity,
           COALESCE(sa.psi, 'P2') AS psi,
           COALESCE(uaa.role, ''),
           COALESCE(pers.full_name, '')
      FROM organizational_unit s
      LEFT JOIN organizational_unit d ON d.id = s.parent_unit_id AND d.unit_type = 'DOMAIN'
      LEFT JOIN organizational_unit p ON p.id = d.parent_unit_id AND p.unit_type = 'POLE'
      LEFT JOIN organizational_unit e ON e.id = p.parent_unit_id AND e.unit_type = 'ENTITY'
      LEFT JOIN solution_attribute sa ON sa.unit_id = s.id
      LEFT JOIN unit_admin_assignment uaa ON uaa.unit_id = s.id
      LEFT JOIN person pers ON pers.id = uaa.person_id
     WHERE s.unit_type = 'SOLUTION'
       AND s.active = TRUE
     LIMIT 400;
    """

    for container in containers_to_try:
        try:
            cmd = [
                "docker", "exec", "-i", container,
                "psql", "-U", "alertops", "-d", "alertops", "-t", "-A", "-F", "|",
                "-c", query
            ]
            proc = subprocess.run(cmd, capture_output=True, text=True, timeout=5)
            if proc.returncode == 0 and proc.stdout.strip():
                lines = proc.stdout.strip().split("\n")
                solutions_map = {}
                for line in lines:
                    parts = line.split("|")
                    if len(parts) >= 5:
                        name = parts[0].strip()
                        domain = parts[1].strip()
                        pole = parts[2].strip()
                        entity = parts[3].strip()
                        psi = parts[4].strip() or "P2"
                        role = parts[5].strip() if len(parts) > 5 else ""
                        person = parts[6].strip() if len(parts) > 6 else ""

                        if not name:
                            continue

                        if name not in solutions_map:
                            solutions_map[name] = {
                                "name": name,
                                "domain": domain,
                                "pole": pole,
                                "entity": entity,
                                "psi": psi,
                                "tam": "Brahim Ben Khadaj",
                                "tech_admin": "Hicham Haddad",
                                "host": f"srv-{name.lower().replace(' ', '-').replace('/', '-')}-prod.bank.internal",
                            }
                        if role == "TAM" and person:
                            solutions_map[name]["tam"] = person
                        elif role in ("TECHNICAL_ADMIN", "FUNCTIONAL_ADMIN") and person:
                            solutions_map[name]["tech_admin"] = person

                if solutions_map:
                    return list(solutions_map.values())
        except Exception:
            continue

    return FALLBACK_SOLUTIONS


# -----------------------------------------------------------------------------
# Définition des Scénarios Dynatrace
# -----------------------------------------------------------------------------
SCENARIOS = {
    "1": {
        "id": "p0_availability",
        "code": "P0_AVAILABILITY",
        "name": "Panne Critique d'Indisponibilité (P0 - AVAILABILITY)",
        "description": "Crash de service transactionnel ou indisponibilité totale avec impact client majeur. Déclenche appel VoIP d'urgence (TAM -> Admin Technique -> Escalade).",
        "severity": "AVAILABILITY",
        "impact": "SERVICES",
        "status": "OPEN",
        "generate": lambda sol, now_ms: {
            "title": f"Indisponibilité critique du service {sol['name']} en Production",
            "impactLevel": "SERVICES",
            "severityLevel": "AVAILABILITY",
            "evidenceDisplayName": "Service unavailable (HTTP 503 / Connection Refused)",
            "usersImpacted": random.randint(1500, 12000),
            "k8s_ns": sol["name"].lower().replace(" ", "-"),
        },
    },
    "2": {
        "id": "performance_degradation",
        "code": "PERFORMANCE_DEGRADATION",
        "name": "Dégradation Sévère de Performance (PERFORMANCE)",
        "description": "Latence anormale du pool de threads ou temps de réponse x5 sur les requêtes bancaires. Notification SMS & E-mail.",
        "severity": "PERFORMANCE",
        "impact": "APPLICATION",
        "status": "OPEN",
        "generate": lambda sol, now_ms: {
            "title": f"Dégradation du temps de réponse (Response Time Slowdown) sur {sol['name']}",
            "impactLevel": "APPLICATION",
            "severityLevel": "PERFORMANCE",
            "evidenceDisplayName": "Response time degradation (+450% above baseline)",
            "usersImpacted": random.randint(200, 1500),
            "k8s_ns": sol["name"].lower().replace(" ", "-"),
        },
    },
    "3": {
        "id": "resource_contention",
        "code": "RESOURCE_CONTENTION",
        "name": "Saturation Ressources & Contention CPU/Mémoire (RESOURCE_CONTENTION)",
        "description": "Saturation CPU à 98% ou mémoire heap saturée sur le cluster applicatif. Déclenche le routage vers l'équipe Infrastructure.",
        "severity": "RESOURCE_CONTENTION",
        "impact": "INFRASTRUCTURE",
        "status": "OPEN",
        "generate": lambda sol, now_ms: {
            "title": f"Saturation CPU et épuisement mémoire heap sur le cluster {sol['name']}",
            "impactLevel": "INFRASTRUCTURE",
            "severityLevel": "RESOURCE_CONTENTION",
            "evidenceDisplayName": "CPU saturation > 95% & Memory pool exhaustion",
            "usersImpacted": random.randint(50, 600),
            "k8s_ns": sol["name"].lower().replace(" ", "-"),
        },
    },
    "4": {
        "id": "security_anomaly",
        "code": "SECURITY_ERROR",
        "name": "Alerte de Sécurité & Accès Privilégiés (ERROR / SECOPS)",
        "description": "Échecs répétés d'authentification ou expiration imminente de certificat mTLS. Règle métier SECOPS avec demande de validation humaine.",
        "severity": "ERROR",
        "impact": "APPLICATION",
        "status": "OPEN",
        "generate": lambda sol, now_ms: {
            "title": f"Anomalie de sécurité / Échec d'authentification en chaîne sur {sol['name']}",
            "impactLevel": "APPLICATION",
            "severityLevel": "ERROR",
            "evidenceDisplayName": "Authentication Failure Spike & Certificate Handshake Error",
            "usersImpacted": random.randint(10, 300),
            "k8s_ns": "secops-" + sol["name"].lower().replace(" ", "-"),
        },
    },
    "5": {
        "id": "network_interface",
        "code": "NETWORK_INTERFACE",
        "name": "Perte de Connectivité Réseau & Interface Télécom (ERROR)",
        "description": "Perte de lien fibre ou paquets droppés sur les commutateurs du Datacenter.",
        "severity": "ERROR",
        "impact": "INFRASTRUCTURE",
        "status": "OPEN",
        "generate": lambda sol, now_ms: {
            "title": f"Liaison réseau principale coupée (Link Down) pour {sol['name']}",
            "impactLevel": "INFRASTRUCTURE",
            "severityLevel": "ERROR",
            "evidenceDisplayName": "Interface Link Down / Packet Drop Rate > 40%",
            "usersImpacted": random.randint(500, 3000),
            "k8s_ns": "telecom-core",
        },
    },
    "6": {
        "id": "problem_resolved",
        "code": "RESOLVED_EVENT",
        "name": "Résolution d'Incident Dynatrace (STATUS: RESOLVED)",
        "description": "Notification de fin d'incident. Interrompt l'escalade active et marque l'alerte comme résolue.",
        "severity": "AVAILABILITY",
        "impact": "SERVICES",
        "status": "RESOLVED",
        "generate": lambda sol, now_ms: {
            "title": f"Résolution de l'incident : Indisponibilité résolue sur {sol['name']}",
            "impactLevel": "SERVICES",
            "severityLevel": "AVAILABILITY",
            "evidenceDisplayName": "Service health restored / All probes nominal",
            "usersImpacted": 0,
            "k8s_ns": sol["name"].lower().replace(" ", "-"),
        },
    },
}


def build_dynatrace_payload(scenario_def, solution, custom_problem_id=None):
    """Construit un payload JSON conforme à la spécification Webhook Dynatrace API v2."""
    now_ms = int(time.time() * 1000)
    pid_random = random.randint(100000, 999999)
    today_str = datetime.now(timezone.utc).strftime("%Y%m%d")

    problem_id = custom_problem_id or f"PID-{today_str}-{pid_random}"
    display_id = f"P-{today_str[-6:]}{random.randint(10, 99)}"

    scenario_data = scenario_def["generate"](solution, now_ms)
    is_resolved = scenario_def["status"] == "RESOLVED"

    start_time_ms = now_ms - (300_000 if not is_resolved else 1_800_000)
    end_time_ms = now_ms if is_resolved else 0

    host_name = solution.get("host", f"srv-{solution['name'].lower()}-prod-01.bank.internal")
    app_name = solution["name"]

    payload = {
        "problemId": problem_id,
        "displayId": display_id,
        "title": scenario_data["title"],
        "impactLevel": scenario_data["impactLevel"],
        "severityLevel": scenario_data["severityLevel"],
        "status": scenario_def["status"],
        "startTime": start_time_ms,
        "endTime": end_time_ms,
        "problemUrl": f"https://dynatrace.attijariwafa.com/#problems/problemdetails;pid={problem_id}",
        "affectedEntities": [
            {
                "entityId": {
                    "id": f"SERVICE-{pid_random}",
                    "type": "SERVICE"
                },
                "name": app_name
            }
        ],
        "impactedEntities": [
            {
                "entityId": {
                    "id": f"APPLICATION-{pid_random}",
                    "type": "APPLICATION"
                },
                "name": app_name
            },
            {
                "entityId": {
                    "id": f"HOST-{pid_random}",
                    "type": "HOST"
                },
                "name": host_name
            }
        ],
        "rootCauseEntity": {
            "entityId": {
                "id": f"HOST-{pid_random}",
                "type": "HOST"
            },
            "name": host_name
        },
        "entityTags": [
            {"context": "CONTEXTLESS", "key": "environment", "value": "Production", "stringRepresentation": "environment:Production"},
            {"context": "CONTEXTLESS", "key": "solution", "value": app_name, "stringRepresentation": f"solution:{app_name}"},
            {"context": "CONTEXTLESS", "key": "psi", "value": solution.get("psi", "P0"), "stringRepresentation": f"psi:{solution.get('psi', 'P0')}"},
            {"context": "CONTEXTLESS", "key": "domaine", "value": solution.get("domain", "Monétique"), "stringRepresentation": f"domaine:{solution.get('domain', 'Monétique')}"},
            {"context": "CONTEXTLESS", "key": "pole", "value": solution.get("pole", "Monétique"), "stringRepresentation": f"pole:{solution.get('pole', 'Monétique')}"},
            {"context": "CONTEXTLESS", "key": "entity", "value": solution.get("entity", "IT"), "stringRepresentation": f"entity:{solution.get('entity', 'IT')}"},
            {"context": "CONTEXTLESS", "key": "tam", "value": solution.get("tam", "Brahim Ben Khadaj"), "stringRepresentation": f"tam:{solution.get('tam', 'Brahim Ben Khadaj')}"},
        ],
        "managementZones": [
            {"id": "mz-bank-prod", "name": "Production Bancaire Transverse"}
        ],
        "k8s.cluster.name": ["k8s-prod-datacenter-01"],
        "k8s.namespace.name": [scenario_data.get("k8s_ns", "default")],
        "evidenceDetails": {
            "totalCount": 1,
            "details": [
                {
                    "displayName": scenario_data["evidenceDisplayName"],
                    "rootCauseRelevant": True,
                    "startTime": start_time_ms,
                    "entity": {
                        "entityId": {"id": f"HOST-{pid_random}", "type": "HOST"},
                        "name": host_name
                    }
                }
            ]
        },
        "impactAnalysis": {
            "impacts": [
                {
                    "estimatedAffectedUsers": scenario_data["usersImpacted"],
                    "impactedEntity": {
                        "entityId": {"id": f"APPLICATION-{pid_random}", "type": "APPLICATION"},
                        "name": app_name
                    }
                }
            ]
        }
    }

    return payload, problem_id


def send_webhook(endpoint, token, payload):
    """Envoie le payload JSON au webhook Dynatrace AlertOps avec les headers adéquats."""
    body_bytes = json.dumps(payload, ensure_ascii=False, indent=2).encode("utf-8")
    req = urllib.request.Request(
        endpoint,
        data=body_bytes,
        headers={
            "Content-Type": "application/json; charset=utf-8",
            "X-Ingestion-Token": token,
            "User-Agent": "Dynatrace/1.280.0 (WebhookNotification)",
        },
        method="POST"
    )

    start_time = time.time()
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            elapsed = time.time() - start_time
            status_code = resp.status
            response_text = resp.read().decode("utf-8")
            return True, status_code, response_text, elapsed
    except urllib.error.HTTPError as e:
        elapsed = time.time() - start_time
        err_body = e.read().decode("utf-8") if e.fp else str(e)
        return False, e.code, err_body, elapsed
    except urllib.error.URLError as e:
        elapsed = time.time() - start_time
        return False, 0, f"Erreur de connexion : {e.reason}", elapsed


def print_banner():
    print("\n" + "=" * 76)
    print(" 🚀  SIMULATEUR DE WEBHOOK DYNATRACE — ALERTOPS")
    print("=" * 76)


def print_summary(scenario, solution, payload, success, status, resp_text, elapsed, endpoint):
    print("\n" + "─" * 76)
    print(f"📌 SCÉNARIO     : [{scenario['code']}] {scenario['name']}")
    print(f"🏢 APPLICATION  : {solution['name']} ({solution.get('domain', '—')} · PSI: {solution.get('psi', '—')})")
    print(f"👤 CONTACT TAM  : {solution.get('tam', '—')}")
    print(f"🔧 ADMIN TECH   : {solution.get('tech_admin', '—')}")
    print(f"🌐 ENDPOINT     : {endpoint}")
    print("─" * 76)
    print(f"📋 TITRE ALERTE : {payload['title']}")
    print(f"🆔 PROBLEM ID   : {payload['problemId']} ({payload['displayId']})")
    print(f"⚡ SÉVÉRITÉ     : {payload['severityLevel']} | IMPACT : {payload['impactLevel']} | ÉTAT : {payload['status']}")
    print("─" * 76)

    if success:
        print(f"✅ RÉSULTAT HTTP : {status} CREATED (en {elapsed:.2f}s)")
        try:
            resp_json = json.loads(resp_text)
            print(f"   ├─ ID Alerte Interne : {resp_json.get('alertId', '—')}")
            print(f"   ├─ ID Dynatrace      : {resp_json.get('externalProblemId') or resp_json.get('problemId') or payload.get('problemId')}")
            print(f"   └─ Statut Ingestion  : {'Nouvelle alerte créée' if resp_json.get('created') else 'Alerte mise à jour'}")
        except Exception:
            print(f"   Réponse : {resp_text}")

        print("\n⚡ ÉTAPES DU PIPELINE DÉCLENCHÉES EN ARRIÈRE-PLAN :")
        print("   1. Ingestion & Normalisation     ➔ Alerte persistée dans la table `alert`")
        print("   2. Classification LLM Gemini     ➔ Analyse sémantique & attribution PSI")
        print("   3. RAG / Résolution Organisation ➔ Rattachement Solution ➔ Domaine ➔ Pôle ➔ Entité")
        print("   4. Évaluation Règles Métier      ➔ Vérification des branches conditionnelles (RuleEngine)")
        print("   5. Moteur de Routage & Escalade  ➔ Exécution du workflow (VoiceCall, SMS, Email)")
        if scenario['code'] == 'P0_AVAILABILITY':
            print("   📞 [VOIP ACTIF] Appel téléphonique automatique en cours via Asterisk ARI...")
        print("─" * 76)
        print("👉 Visualisez l'alerte en direct sur l'interface : http://localhost:5173/alerts")
    else:
        print(f"❌ ERREUR HTTP {status} (en {elapsed:.2f}s)")
        print(f"   Détail : {resp_text}")
        if status == 401:
            print("   💡 Vérifiez que le token d'ingestion correspond à `app.ingestion.dynatrace.token`.")
        elif status == 0:
            print("   💡 Le serveur AlertOps ne semble pas démarré sur le port 8085.")
    print("=" * 76 + "\n")


def main():
    parser = argparse.ArgumentParser(
        description="Simulateur réaliste de webhooks Dynatrace pour AlertOps",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Exemples d'utilisation :
  python simulate_dynatrace.py                         # Scénario aléatoire
  python simulate_dynatrace.py --scenario 1            # Forcer le scénario P0 Indisponibilité
  python simulate_dynatrace.py --scenario 6            # Simuler la résolution d'une alerte
  python simulate_dynatrace.py --solution PayCore      # Cibler une solution spécifique
  python simulate_dynatrace.py --list                  # Lister les scénarios disponibles
  python simulate_dynatrace.py --dry-run               # Afficher le JSON sans l'envoyer
  python simulate_dynatrace.py --repeat 5 --interval 3 # Envoyer 5 alertes espacées de 3s
        """
    )
    parser.add_argument("-s", "--scenario", help="Numéro ou code du scénario (1 à 6, p0, performance, security, etc.)")
    parser.add_argument("--solution", help="Nom de la solution applicative ciblée (ex: PayCore, AttijariPay, SWIFT, MUREX)")
    parser.add_argument("-u", "--url", default=DEFAULT_ENDPOINT, help=f"URL du webhook AlertOps (défaut: {DEFAULT_ENDPOINT})")
    parser.add_argument("-t", "--token", default=DEFAULT_TOKEN, help="Token X-Ingestion-Token")
    parser.add_argument("-l", "--list", action="store_true", help="Lister tous les scénarios et solutions disponibles")
    parser.add_argument("-d", "--dry-run", action="store_true", help="Afficher le payload JSON sans l'envoyer au serveur")
    parser.add_argument("-r", "--repeat", type=int, default=1, help="Nombre d'alertes à envoyer consécutivement")
    parser.add_argument("-i", "--interval", type=float, default=2.0, help="Intervalle en secondes entre chaque envoi en mode repeat")
    parser.add_argument("--pid", help="ID problème personnalisé (ex: PID-CUSTOM-001)")

    args = parser.parse_args()

    print_banner()
    print("🔍 Chargement du contexte organisationnel (Base de données PostgreSQL)...")
    solutions = fetch_database_context()
    print(f"✓ {len(solutions)} solutions applicatives disponibles pour les scénarios.\n")

    if args.list:
        print("📋 SCÉNARIOS DISPONIBLES :")
        for key, sc in SCENARIOS.items():
            print(f"  [{key}] {sc['code']:<25} ➔ {sc['name']}")
            print(f"      {sc['description']}\n")
        print("🏢 EXEMPLES DE SOLUTIONS DISPONIBLES :")
        for s in solutions[:12]:
            print(f"  • {s['name']:<25} ({s.get('domain', '—')} | PSI: {s.get('psi', '—')} | TAM: {s.get('tam', '—')})")
        print("\n" + "=" * 76)
        return 0

    # Résolution de la solution ciblée
    if args.solution:
        matched = [s for s in solutions if args.solution.lower() in s["name"].lower()]
        if not matched:
            print(f"⚠️ Solution '{args.solution}' non trouvée dans le référentiel. Utilisation d'une solution par défaut.")
            target_solution = {"name": args.solution, "domain": "Production Bancaire", "pole": "Monétique", "entity": "IT", "psi": "P0", "tam": "Brahim Ben Khadaj", "tech_admin": "Hicham Haddad"}
        else:
            target_solution = matched[0]
    else:
        target_solution = None

    # Résolution du scénario
    scenario_keys = list(SCENARIOS.keys())

    for idx in range(args.repeat):
        if args.scenario:
            # Recherche par numéro ou par id/code
            sc_key = str(args.scenario)
            if sc_key in SCENARIOS:
                selected_scenario = SCENARIOS[sc_key]
            else:
                matched_sc = [sc for sc in SCENARIOS.values() if sc_key.lower() in sc["id"] or sc_key.upper() in sc["code"]]
                selected_scenario = matched_sc[0] if matched_sc else SCENARIOS["1"]
        else:
            selected_scenario = SCENARIOS[random.choice(scenario_keys)]

        current_solution = target_solution or random.choice(solutions)
        payload, pid = build_dynatrace_payload(selected_scenario, current_solution, args.pid)

        if args.dry_run:
            print(f"🔬 [DRY-RUN] Payload généré pour [{selected_scenario['code']}] sur {current_solution['name']} :")
            print(json.dumps(payload, indent=2, ensure_ascii=False))
            print("=" * 76)
            continue

        print(f"📡 Envoi du webhook {idx + 1}/{args.repeat} vers {args.url}...")
        success, status, resp_text, elapsed = send_webhook(args.url, args.token, payload)
        print_summary(selected_scenario, current_solution, payload, success, status, resp_text, elapsed, args.url)

        if idx < args.repeat - 1:
            time.sleep(args.interval)

    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print("\nArrêt du simulateur.")
        sys.exit(0)
