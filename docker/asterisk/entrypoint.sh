#!/bin/bash
set -euo pipefail

KEYS=/etc/asterisk/keys
SOUNDS=/var/lib/asterisk/sounds/alertops
mkdir -p "$KEYS" "$SOUNDS"

if [ ! -f "$KEYS/asterisk.pem" ]; then
  openssl req -x509 -nodes -days 3650 -newkey rsa:2048 \
    -keyout "$KEYS/asterisk.key" \
    -out "$KEYS/asterisk.crt" \
    -subj "/CN=localhost" \
    -addext "subjectAltName=DNS:localhost,DNS:asterisk,DNS:alertops-asterisk,IP:127.0.0.1"
  cat "$KEYS/asterisk.key" "$KEYS/asterisk.crt" > "$KEYS/asterisk.pem"
fi

chmod 640 "$KEYS"/asterisk.key "$KEYS"/asterisk.pem 2>/dev/null || true
chown -R asterisk:asterisk "$KEYS" "$SOUNDS" 2>/dev/null || true

exec /usr/local/bin/entrypoint.sh "$@"
