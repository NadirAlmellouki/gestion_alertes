import socket
import threading
import time
import hashlib
import re
import urllib.request
import urllib.parse
import json
import glob
import os

ASTERISK_HOST = "127.0.0.1"
ASTERISK_PORT = 5060
SIP_USER = "1002"
SIP_PASS = "alertops"
LOCAL_IP = "127.0.0.1"
LOCAL_SIP_PORT = 5090

def build_md5_response(username, realm, password, nonce, uri, method="REGISTER"):
    ha1 = hashlib.md5(f"{username}:{realm}:{password}".encode()).hexdigest()
    ha2 = hashlib.md5(f"{method}:{uri}".encode()).hexdigest()
    response = hashlib.md5(f"{ha1}:{nonce}:{ha2}".encode()).hexdigest()
    return response

class SingleProcessCallTester:
    def __init__(self):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.sock.bind((LOCAL_IP, LOCAL_SIP_PORT))
        self.sock.settimeout(1.0)
        self.cseq = 1
        self.call_id = f"sip-sim-{int(time.time())}@127.0.0.1"
        self.registered = False
        self.running = True
        self.answered = False

    def send_register(self, auth_header=None):
        self.cseq += 1
        auth_line = f"\r\n{auth_header}" if auth_header else ""
        msg = (
            f"REGISTER sip:{ASTERISK_HOST}:{ASTERISK_PORT} SIP/2.0\r\n"
            f"Via: SIP/2.0/UDP {LOCAL_IP}:{LOCAL_SIP_PORT};branch=z9hG4bK{int(time.time()*1000)}\r\n"
            f"Max-Forwards: 70\r\n"
            f"From: <sip:{SIP_USER}@{ASTERISK_HOST}>;tag=fromtag{int(time.time())}\r\n"
            f"To: <sip:{SIP_USER}@{ASTERISK_HOST}>\r\n"
            f"Call-ID: {self.call_id}\r\n"
            f"CSeq: {self.cseq} REGISTER\r\n"
            f"Contact: <sip:{SIP_USER}@{LOCAL_IP}:{LOCAL_SIP_PORT}>\r\n"
            f"Expires: 300{auth_line}\r\n"
            f"Content-Length: 0\r\n\r\n"
        )
        self.sock.sendto(msg.encode(), (ASTERISK_HOST, ASTERISK_PORT))

    def register(self):
        print(f"[*] Enregistrement SIP de l'extension {SIP_USER} sur Asterisk...")
        self.send_register()
        start = time.time()
        while self.running and not self.registered and (time.time() - start < 10):
            try:
                data, addr = self.sock.recvfrom(4096)
                resp = data.decode(errors='ignore')
                if "401 Unauthorized" in resp or "407 Proxy Authentication Required" in resp:
                    realm_m = re.search(r'realm="([^"]+)"', resp)
                    nonce_m = re.search(r'nonce="([^"]+)"', resp)
                    if realm_m and nonce_m:
                        realm = realm_m.group(1)
                        nonce = nonce_m.group(1)
                        uri = f"sip:{ASTERISK_HOST}:{ASTERISK_PORT}"
                        digest = build_md5_response(SIP_USER, realm, SIP_PASS, nonce, uri, "REGISTER")
                        auth_header = (
                            f'Authorization: Digest username="{SIP_USER}", realm="{realm}", '
                            f'nonce="{nonce}", uri="{uri}", response="{digest}", algorithm=MD5'
                        )
                        self.send_register(auth_header)
                elif "200 OK" in resp:
                    print(f"[OK] Extension {SIP_USER} enregistrée avec succès !")
                    self.registered = True
                    break
            except socket.timeout:
                self.send_register()

    def run_sip_listener(self):
        print(f"[*] Écoute des appels entrants sur {SIP_USER}...")
        call_start = None
        current_dialog = {}

        while self.running:
            try:
                data, addr = self.sock.recvfrom(4096)
                msg = data.decode(errors='ignore')

                if msg.startswith("INVITE"):
                    print("[SIP] >>> INVITE REÇU ! Décrochage automatique (200 OK)...")
                    via_lines = re.findall(r'(Via:[^\r\n]+)', msg)
                    from_h = re.search(r'From:[^\r\n]+', msg).group(0)
                    to_h = re.search(r'To:[^\r\n]+', msg).group(0)
                    callid_h = re.search(r'Call-ID:[^\r\n]+', msg).group(0)
                    cseq_h = re.search(r'CSeq:[^\r\n]+', msg).group(0)

                    current_dialog = {
                        "from": from_h,
                        "to": to_h + ";tag=answeertag" + str(int(time.time())),
                        "callid": callid_h,
                        "cseq": int(re.search(r'CSeq:\s*(\d+)', msg).group(1)),
                        "addr": addr
                    }

                    via_block = "\r\n".join(via_lines)
                    sdp = (
                        "v=0\r\n"
                        "o=sipclient 123456 123456 IN IP4 127.0.0.1\r\n"
                        "s=Talk\r\n"
                        "c=IN IP4 127.0.0.1\r\n"
                        "t=0 0\r\n"
                        "m=audio 20000 RTP/AVP 0 8 101\r\n"
                        "a=rtpmap:0 PCMU/8000\r\n"
                        "a=rtpmap:8 PCMA/8000\r\n"
                        "a=rtpmap:101 telephone-event/8000\r\n"
                        "a=sendrecv\r\n"
                    )

                    ok_resp = (
                        f"SIP/2.0 200 OK\r\n"
                        f"{via_block}\r\n"
                        f"{from_h}\r\n"
                        f"{current_dialog['to']}\r\n"
                        f"{callid_h}\r\n"
                        f"{cseq_h}\r\n"
                        f"Contact: <sip:{SIP_USER}@{LOCAL_IP}:{LOCAL_SIP_PORT}>\r\n"
                        f"Content-Type: application/sdp\r\n"
                        f"Content-Length: {len(sdp)}\r\n\r\n"
                        f"{sdp}"
                    )
                    self.sock.sendto(ok_resp.encode(), addr)
                    self.answered = True
                    call_start = time.time()
                    print("[SIP] <<< 200 OK envoyé ! Conversation live en cours...")

                elif msg.startswith("ACK"):
                    print("[SIP] >>> ACK reçu ! Canal live établi.")

                elif msg.startswith("BYE"):
                    print("[SIP] >>> BYE reçu. Fin d'appel.")
                    via_block = "\r\n".join(re.findall(r'(Via:[^\r\n]+)', msg))
                    from_h = re.search(r'From:[^\r\n]+', msg).group(0)
                    to_h = re.search(r'To:[^\r\n]+', msg).group(0)
                    callid_h = re.search(r'Call-ID:[^\r\n]+', msg).group(0)
                    cseq_h = re.search(r'CSeq:[^\r\n]+', msg).group(0)
                    resp = f"SIP/2.0 200 OK\r\n{via_block}\r\n{from_h}\r\n{to_h}\r\n{callid_h}\r\n{cseq_h}\r\nContent-Length: 0\r\n\r\n"
                    self.sock.sendto(resp.encode(), addr)
                    break

            except socket.timeout:
                if self.answered and call_start and (time.time() - call_start > 5):
                    print("[SIP] Durée de conversation de 5 secondes atteinte. Raccrochage...")
                    if current_dialog:
                        bye_cseq = current_dialog["cseq"] + 1
                        bye_msg = (
                            f"BYE sip:{ASTERISK_HOST}:{ASTERISK_PORT} SIP/2.0\r\n"
                            f"Via: SIP/2.0/UDP {LOCAL_IP}:{LOCAL_SIP_PORT};branch=z9hG4bKbyetag{int(time.time()*1000)}\r\n"
                            f"Max-Forwards: 70\r\n"
                            f"{current_dialog['to']}\r\n"
                            f"{current_dialog['from']}\r\n"
                            f"{current_dialog['callid']}\r\n"
                            f"CSeq: {bye_cseq} BYE\r\n"
                            f"Content-Length: 0\r\n\r\n"
                        )
                        self.sock.sendto(bye_msg.encode(), current_dialog["addr"])
                    break

def main():
    tester = SingleProcessCallTester()
    tester.register()
    if not tester.registered:
        print("[FAIL] Impossible de s'enregistrer sur Asterisk")
        return

    # Start listener thread
    t = threading.Thread(target=tester.run_sip_listener, daemon=True)
    t.start()

    time.sleep(1)

    # Step 1: Token
    print("[*] Authentification Keycloak (rôle SUPERVISOR)...")
    token_data = urllib.parse.urlencode({
        'client_id': 'alertops-api',
        'grant_type': 'password',
        'username': 'superviseur',
        'password': 'superviseur'
    }).encode()
    token_req = urllib.request.Request('http://localhost:8083/realms/alertops/protocol/openid-connect/token', data=token_data)
    with urllib.request.urlopen(token_req) as resp:
        token = json.loads(resp.read().decode())['access_token']

    # Step 2: Get target person
    adm_req = urllib.request.Request('http://localhost:8080/api/v1/dashboard/admins', headers={'Authorization': 'Bearer ' + token})
    with urllib.request.urlopen(adm_req) as resp:
        admins = json.loads(resp.read().decode())['admins']
        target_person = admins[0]
        print(f"[TEST] Destinataire : {target_person['fullName']} (ID: {target_person['personId']}, tel: {target_person['phone']})")

    # Step 3: Trigger manual call
    print("[*] Déclenchement de l'appel manuel POST /api/v1/dashboard/manual-calls...")
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
    with urllib.request.urlopen(manual_req) as resp:
        call_res = json.loads(resp.read().decode())
        print(f"[TEST] Résultat appel API : status={call_res.get('status')}, callId={call_res.get('providerMessageId')}")

    # Wait for SIP conversation and hangup
    t.join(timeout=15)
    tester.running = False

    time.sleep(2)

    # Step 4: Verify recording file
    rec_dir = r"C:\Users\DELL\Desktop\PFA\gestion_alertes\docker\asterisk\sounds\recordings"
    files = glob.glob(os.path.join(rec_dir, "*"))
    print("\n" + "="*60)
    print(f"[*] Vérification des fichiers dans {rec_dir} :")
    if files:
        for f in files:
            print(f" -> FICHIER ENREGISTRÉ : {os.path.basename(f)} ({os.path.getsize(f)} octets)")
    else:
        print(" -> Aucun fichier trouvé.")
    print("="*60)

if __name__ == "__main__":
    main()
