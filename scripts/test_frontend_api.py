import urllib.request, urllib.parse, json

token_data = urllib.parse.urlencode({
    'client_id': 'alertops-api',
    'grant_type': 'password',
    'username': 'nadir',
    'password': 'ops'
}).encode()
token_req = urllib.request.Request('http://localhost:8083/realms/alertops/protocol/openid-connect/token', data=token_data)
with urllib.request.urlopen(token_req) as resp:
    token = json.loads(resp.read().decode())['access_token']

voip_req = urllib.request.Request('http://localhost:8080/api/v1/dashboard/voip', headers={'Authorization': 'Bearer ' + token})
with urllib.request.urlopen(voip_req) as resp:
    data = json.loads(resp.read().decode())
    print('[OK] Dashboard VoIP Summary:')
    print(json.dumps(data.get('summary', {}), indent=2))
    print(f"[OK] Recent VoIP calls count: {len(data.get('recentCalls', []))}")
    for c in data.get('recentCalls', [])[:3]:
        print(f" - To {c.get('personName')} ({c.get('destination')}): status={c.get('status')}, step={c.get('escalationStep')}")
