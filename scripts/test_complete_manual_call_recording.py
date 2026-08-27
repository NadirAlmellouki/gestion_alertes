import subprocess
import time
import sys
import os
import glob

print("="*75)
print(" TEST ENREGISTREMENT APPEL MANUEL VOIP (SUPERVISEUR -> ADMIN 1002)")
print("="*75)

# Step 1: Start SIP Simulator for Extension 1002
print("\n[*] 1. Demarrage du simulateur SIP Softphone sur extension 1002...")
sim_proc = subprocess.Popen(
    [sys.executable, "C:\\Users\\DELL\\Desktop\\PFA\\gestion_alertes\\scripts\\sip_simulator.py"],
    stdout=subprocess.PIPE,
    stderr=subprocess.STDOUT,
    text=True,
    bufsize=1
)

time.sleep(2)

# Step 2: Trigger Manual Call via API
print("\n[*] 2. Declenchement de l'appel manuel superviseur via API...")
man_proc = subprocess.run(
    [sys.executable, "C:\\Users\\DELL\\Desktop\\PFA\\gestion_alertes\\scripts\\test_manual_call_recording.py"],
    capture_output=True,
    text=True
)
print(man_proc.stdout)
if man_proc.stderr:
    print(man_proc.stderr)

# Wait for call conversation to finish
time.sleep(6)

try:
    stdout, _ = sim_proc.communicate(timeout=3)
    print("\n--- SIP Softphone Simulator Output ---")
    print(stdout)
except Exception:
    sim_proc.kill()

# Step 3: Check Recordings Directory
print("\n[*] 3. Verification des fichiers enregistres sur disque :")
rec_dir = r"C:\Users\DELL\Desktop\PFA\gestion_alertes\docker\asterisk\sounds\recordings"
files = glob.glob(os.path.join(rec_dir, "*"))
if files:
    for f in files:
        print(f" -> FICHIER ENREGISTRE : {os.path.basename(f)} ({os.path.getsize(f)} octets)")
else:
    print(" -> Aucun fichier trouve.")

print("="*75)
