import urllib.request
import urllib.parse
import json

# Step 1: Login as Supervisor in Keycloak
keycloak_url = "http://localhost:8083/realms/alertops/protocol/openid-connect/token"
token_data = urllib.parse.urlencode({
    "client_id": "alertops-api",
    "grant_type": "password",
    "username": "superviseur",
    "password": "superviseur"
}).encode('utf-8')

token_req = urllib.request.Request(keycloak_url, data=token_data, headers={"Content-Type": "application/x-www-form-urlencoded"})
with urllib.request.urlopen(token_req) as resp:
    token_json = json.loads(resp.read().decode())
    access_token = token_json['access_token']
    print("[+] Keycloak token acquired for Supervisor 'superviseur'")

# Step 2: Trigger Manual VoIP Call to Nadir Almellouki
call_url = "http://localhost:8080/api/v1/dashboard/manual-calls"
call_payload = {
    "personId": "a1111111-0001-4000-8000-000000000001",
    "alertId": "8da18b7c-ac1e-42b7-af3c-61fe133a71fb"
}

call_req = urllib.request.Request(
    call_url,
    data=json.dumps(call_payload).encode('utf-8'),
    headers={
        "Authorization": f"Bearer {access_token}",
        "Content-Type": "application/json"
    }
)

print(f"[*] Placing manual VoIP call via {call_url}...")
try:
    with urllib.request.urlopen(call_req) as resp:
        res = json.loads(resp.read().decode())
        print("\n" + "="*70)
        print(" MANUAL CALL RESULT")
        print("="*70)
        print(json.dumps(res, indent=2))
except urllib.error.HTTPError as e:
    print(f"[-] HTTP Error: {e.code} {e.reason}")
    print(e.read().decode())
