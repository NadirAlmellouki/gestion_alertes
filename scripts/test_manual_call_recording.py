import urllib.request, urllib.parse, json, time, socket, os, glob

# Step 1: Token
token_data = urllib.parse.urlencode({
    'client_id': 'alertops-api',
    'grant_type': 'password',
    'username': 'superviseur',
    'password': 'superviseur'
}).encode()
token_req = urllib.request.Request('http://localhost:8083/realms/alertops/protocol/openid-connect/token', data=token_data)
with urllib.request.urlopen(token_req) as resp:
    token = json.loads(resp.read().decode())['access_token']

# Step 2: Get first person ID
adm_req = urllib.request.Request('http://localhost:8080/api/v1/dashboard/admins', headers={'Authorization': 'Bearer ' + token})
with urllib.request.urlopen(adm_req) as resp:
    admins = json.loads(resp.read().decode())['admins']
    target_person = admins[0]
    print(f"[TEST] Target person: {target_person['fullName']} (ID: {target_person['personId']}, phone: {target_person['phone']})")

# Step 3: Trigger manual call
req_body = json.dumps({
    "personId": target_person['personId'],
    "alertId": None
}).encode()

manual_req = urllib.request.Request(
    'http://localhost:8080/api/v1/dashboard/manual-calls',
    data=req_body,
    headers={
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json'
    }
)

print("[TEST] Triggering manual call via API...")
try:
    with urllib.request.urlopen(manual_req) as resp:
        call_res = json.loads(resp.read().decode())
        print(f"[TEST] Manual call result: {call_res}")
except Exception as e:
    print(f"[FAIL] Manual call: {e}")

time.sleep(2)

# Check files in recordings directory
recordings_dir = r"C:\Users\DELL\Desktop\PFA\gestion_alertes\docker\asterisk\sounds\recordings"
files = glob.glob(os.path.join(recordings_dir, "*"))
print(f"[TEST] Files in {recordings_dir}:")
for f in files:
    print(f" - {os.path.basename(f)} ({os.path.getsize(f)} bytes)")
