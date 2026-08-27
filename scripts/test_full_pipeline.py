import urllib.request
import urllib.parse
import json
import time

# Send a Dynatrace alert matching an actual solution from the repository: ADFS
problem_id = f"P-ADFS-{int(time.time())}"

payload = {
    "PID": problem_id,
    "ProblemID": problem_id,
    "ProblemTitle": "Indisponibilité du service d'authentification ADFS en production",
    "ProblemImpact": "APPLICATION",
    "ProblemSeverity": "AVAILABILITY",
    "State": "OPEN",
    "ProblemURL": f"https://dynatrace.bank.local/#problems/problemdetails;pid={problem_id}",
    "ImpactedEntity": "ADFS Federation Server",
    "ImpactedEntityNames": "SRV-ADFS-PROD-01",
    "ProblemDetailsText": "Le service ADFS Federation Service ne répond plus aux requêtes d'authentification des applications.",
    "Tags": "Environment:PROD, Solution:ADFS, Tier:Security",
    "ProblemDetailsJSONv2": {
        "problemId": problem_id,
        "displayId": problem_id,
        "title": "Indisponibilité du service d'authentification ADFS en production",
        "impactLevel": "APPLICATION",
        "severityLevel": "AVAILABILITY",
        "status": "OPEN",
        "startTime": int(time.time() * 1000),
        "affectedEntities": [
            {
                "entityId": {"id": "SERVICE-ADFS-001", "type": "SERVICE"},
                "name": "ADFS Authentication Service"
            }
        ]
    }
}

ingest_url = "http://localhost:8080/api/v1/ingestion/dynatrace"
ingest_req = urllib.request.Request(
    ingest_url, 
    data=json.dumps(payload).encode('utf-8'),
    headers={
        "Content-Type": "application/json",
        "X-Ingestion-Token": "dev-ingestion-token"
    }
)

print(f"[*] 1. Sending Dynatrace Alert {problem_id} (Solution: ADFS) to Webhook...")
with urllib.request.urlopen(ingest_req) as resp:
    ingest_res = json.loads(resp.read().decode())
    alert_id = ingest_res['alertId']
    print(f"    [+] Alert ingested successfully! Alert UUID: {alert_id}")

# Step 2: Acquire Keycloak JWT token for user 'nadir' (Role OPS)
keycloak_url = "http://localhost:8083/realms/alertops/protocol/openid-connect/token"
token_data = urllib.parse.urlencode({
    "client_id": "alertops-api",
    "grant_type": "password",
    "username": "nadir",
    "password": "ops"
}).encode('utf-8')

print("\n[*] 2. Authenticating with Keycloak ('nadir' / 'ops')...")
token_req = urllib.request.Request(keycloak_url, data=token_data, headers={"Content-Type": "application/x-www-form-urlencoded"})
with urllib.request.urlopen(token_req) as resp:
    token_json = json.loads(resp.read().decode())
    access_token = token_json['access_token']
    print("    [+] JWT Bearer token acquired!")

# Step 3: Trigger the full Alert Processing Pipeline
pipe_url = f"http://localhost:8080/api/v1/alerts/{alert_id}/process"
pipe_req = urllib.request.Request(pipe_url, method='POST', headers={
    "Authorization": f"Bearer {access_token}",
    "Content-Type": "application/json"
})

print(f"\n[*] 3. Running Complete Pipeline with Google Gemini 3.6 Flash...")
with urllib.request.urlopen(pipe_req) as resp:
    pipeline_res = json.loads(resp.read().decode())
    print("\n" + "="*70)
    print(" PIPELINE EXECUTION RESULT (GEMINI LLM + ELEVENLABS TTS)")
    print("="*70)
    print(json.dumps(pipeline_res, indent=2))
