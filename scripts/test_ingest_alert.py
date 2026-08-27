import urllib.request
import json
import time
import uuid

# Unique problem ID for this test run
problem_id = f"P-TEST-{int(time.time())}"

payload = {
    "PID": problem_id,
    "ProblemID": problem_id,
    "ProblemTitle": "Panne critique sur le serveur PostgreSQL Kraken",
    "ProblemImpact": "APPLICATION",
    "ProblemSeverity": "AVAILABILITY",
    "State": "OPEN",
    "ProblemURL": f"https://dynatrace.bank.local/#problems/problemdetails;pid={problem_id}",
    "ImpactedEntity": "Kraken Database Cluster",
    "ImpactedEntityNames": "Postgres-Primary-01",
    "ProblemDetailsText": "Disponibilité compromise: La base PostgreSQL de la solution Kraken ne répond plus aux requêtes de production.",
    "Tags": "Environment:PROD, Solution:Kraken, Tier:Database",
    "ProblemDetailsJSONv2": {
        "problemId": problem_id,
        "displayId": problem_id,
        "title": "Panne critique sur le serveur PostgreSQL Kraken",
        "impactLevel": "APPLICATION",
        "severityLevel": "AVAILABILITY",
        "status": "OPEN",
        "startTime": int(time.time() * 1000),
        "affectedEntities": [
            {
                "entityId": {"id": "SERVICE-12345", "type": "SERVICE"},
                "name": "Kraken Core Service"
            }
        ]
    }
}

url = "http://localhost:8080/api/v1/ingestion/dynatrace"
headers = {
    "Content-Type": "application/json",
    "X-Ingestion-Token": "dev-ingestion-token"
}

print(f"[*] Sending Dynatrace test alert {problem_id} to {url}...")
req = urllib.request.Request(url, data=json.dumps(payload).encode('utf-8'), headers=headers)

try:
    with urllib.request.urlopen(req) as resp:
        resp_data = json.loads(resp.read().decode())
        print(f"[+] Ingestion HTTP Status: {resp.status}")
        print(f"[+] Ingestion Response: {json.dumps(resp_data, indent=2)}")
except urllib.error.HTTPError as e:
    print(f"[-] HTTP Error: {e.code} {e.reason}")
    print(e.read().decode())
except Exception as e:
    print(f"[-] Error: {e}")
