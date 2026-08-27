import subprocess
import time
import sys
import os

print("="*75)
print(" ALERTOPS COMPLETE END-TO-END DEMO (GEMINI LLM + ELEVENLABS TTS + ASTERISK CALL)")
print("="*75)

# Step 1: Start SIP Simulator for Extension 1002 (Yassine Moustiphi)
print("\n[*] Step 1: Starting SIP Softphone for Extension 1002...")
sim_proc = subprocess.Popen(
    [sys.executable, "C:\\Users\\DELL\\Desktop\\PFA\\gestion_alertes\\scripts\\sip_simulator.py"],
    stdout=subprocess.PIPE,
    stderr=subprocess.STDOUT,
    text=True,
    bufsize=1
)

# Wait for registration
time.sleep(2)

# Step 2: Trigger the Alert Processing Pipeline
print("\n[*] Step 2: Ingesting Dynatrace Alert & Executing Complete Pipeline...")
pipe_proc = subprocess.run(
    [sys.executable, "C:\\Users\\DELL\\Desktop\\PFA\\gestion_alertes\\scripts\\test_full_pipeline.py"],
    capture_output=True,
    text=True
)

print(pipe_proc.stdout)

# Wait for call processing
time.sleep(3)

# Read simulator output
try:
    stdout, _ = sim_proc.communicate(timeout=5)
    print("\n--- SIP Softphone Output ---")
    print(stdout)
except Exception:
    sim_proc.kill()

print("="*75)
print(" END OF DEMO")
print("="*75)
