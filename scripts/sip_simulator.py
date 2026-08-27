import socket
import threading
import time
import hashlib
import re
import sys

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

class SipSoftphoneSimulator:
    def __init__(self):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.sock.bind((LOCAL_IP, LOCAL_SIP_PORT))
        self.sock.settimeout(1.0)
        self.cseq = 1
        self.call_id = f"sip-sim-{int(time.time())}@127.0.0.1"
        self.registered = False
        self.running = True
        self.call_received = False

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
        print(f"[*] Sending REGISTER for {SIP_USER} to Asterisk...", flush=True)
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
                    print(f"[OK] Extension {SIP_USER} successfully REGISTERED on Asterisk!", flush=True)
                    self.registered = True
                    break
            except socket.timeout:
                self.send_register()

    def listen_and_auto_answer(self, duration_seconds=45):
        print(f"[*] Listening for incoming calls on extension {SIP_USER} for {duration_seconds}s...", flush=True)
        end_time = time.time() + duration_seconds
        
        while time.time() < end_time and self.running:
            try:
                data, addr = self.sock.recvfrom(4096)
                msg = data.decode(errors='ignore')
                
                if msg.startswith("OPTIONS"):
                    # Answer 200 OK to keepalive OPTIONS
                    via_lines = re.findall(r'(Via:[^\r\n]+)', msg)
                    from_h = re.search(r'From:[^\r\n]+', msg).group(0)
                    to_h = re.search(r'To:[^\r\n]+', msg).group(0)
                    callid_h = re.search(r'Call-ID:[^\r\n]+', msg).group(0)
                    cseq_h = re.search(r'CSeq:[^\r\n]+', msg).group(0)
                    via_block = "\r\n".join(via_lines)
                    ok_resp = (
                        f"SIP/2.0 200 OK\r\n"
                        f"{via_block}\r\n"
                        f"{from_h}\r\n"
                        f"{to_h}\r\n"
                        f"{callid_h}\r\n"
                        f"{cseq_h}\r\n"
                        f"Content-Length: 0\r\n\r\n"
                    )
                    self.sock.sendto(ok_resp.encode(), addr)
                
                elif msg.startswith("INVITE"):
                    print("\n[CALL] INCOMING CALL DETECTED FROM ASTERISK!", flush=True)
                    self.call_received = True
                    
                    via_lines = re.findall(r'(Via:[^\r\n]+)', msg)
                    from_h = re.search(r'From:[^\r\n]+', msg).group(0)
                    to_h = re.search(r'To:[^\r\n]+', msg).group(0)
                    callid_h = re.search(r'Call-ID:[^\r\n]+', msg).group(0)
                    cseq_h = re.search(r'CSeq:[^\r\n]+', msg).group(0)
                    
                    via_block = "\r\n".join(via_lines)
                    to_tag = f"{to_h};tag=ans_{int(time.time())}"
                    
                    # 180 Ringing
                    ringing = (
                        f"SIP/2.0 180 Ringing\r\n"
                        f"{via_block}\r\n"
                        f"{from_h}\r\n"
                        f"{to_tag}\r\n"
                        f"{callid_h}\r\n"
                        f"{cseq_h}\r\n"
                        f"Content-Length: 0\r\n\r\n"
                    )
                    self.sock.sendto(ringing.encode(), addr)
                    time.sleep(0.3)
                    
                    # 200 OK with SDP
                    sdp = (
                        "v=0\r\n"
                        f"o={SIP_USER} 123456 654321 IN IP4 {LOCAL_IP}\r\n"
                        "s=AlertOps-Simulator\r\n"
                        f"c=IN IP4 {LOCAL_IP}\r\n"
                        "t=0 0\r\n"
                        "m=audio 40000 RTP/AVP 0 8\r\n"
                        "a=rtpmap:0 PCMU/8000\r\n"
                        "a=rtpmap:8 PCMA/8000\r\n"
                        "a=sendrecv\r\n"
                    )
                    
                    ok_resp = (
                        f"SIP/2.0 200 OK\r\n"
                        f"{via_block}\r\n"
                        f"{from_h}\r\n"
                        f"{to_tag}\r\n"
                        f"{callid_h}\r\n"
                        f"{cseq_h}\r\n"
                        f"Contact: <sip:{SIP_USER}@{LOCAL_IP}:{LOCAL_SIP_PORT}>\r\n"
                        f"Content-Type: application/sdp\r\n"
                        f"Content-Length: {len(sdp)}\r\n\r\n"
                        f"{sdp}"
                    )
                    self.sock.sendto(ok_resp.encode(), addr)
                    print("[OK] Call ANSWERED (200 OK sent)! Audio stream active from Asterisk/ElevenLabs.", flush=True)
                    time.sleep(5)
                    break
            except socket.timeout:
                continue
            except Exception as e:
                print(f"Error: {e}", flush=True)

if __name__ == "__main__":
    phone = SipSoftphoneSimulator()
    phone.register()
    if phone.registered:
        phone.listen_and_auto_answer(duration_seconds=45)
