Demo VoIP locale AlertOps
=========================

Image
- andrius/asterisk:22 (Asterisk 22, Debian Trixie)
- Modules verifies: chan_pjsip, res_pjsip_transport_websocket,
  res_http_websocket, res_ari*, res_rtp_asterisk, res_srtp, format_wav, codec_opus
- ffmpeg ajoute dans l'image pour conversion audio si besoin
- Pas de compilation depuis les sources

Ports
- 8088        HTTP ARI (Spring Boot) + WS SIP (navigateur)
- 8089        HTTPS / WSS SIP (certificat auto-signe)
- 5060/udp    SIP UDP
- 10000-10100/udp  RTP / SRTP WebRTC

Extensions de test (mot de passe: alertops)
- 1001, 1002, 1003, 1004

ARI
- URL: http://localhost:8088
- user: alertops
- password: alertops

WebSocket SIP
- WS  (dev local, recommande): ws://localhost:8088/ws
- WSS (TLS): wss://localhost:8089/ws
  Accepter d'abord le certificat: ouvrir https://localhost:8089 dans le navigateur.

Volumes
- docker/asterisk/config/*.conf  montes dans /etc/asterisk
- docker/asterisk/sounds         -> /var/lib/asterisk/sounds/alertops (fichiers TTS)
- volume asterisk-keys           -> /etc/asterisk/keys (PEM TLS)

Reseau
- Service compose `asterisk` sur le reseau par defaut du projet (avec postgres/kafka).
- Spring Boot tourne sur l'hote et appelle ARI via localhost:8088.

Procedure de demonstration
1. docker compose up -d --build asterisk
2. Definir ELEVENLABS_API_KEY dans .env.local (jamais dans git)
3. Demarrer le backend Spring Boot (profil dev)
4. npm run dev dans frontEnd
5. Ouvrir http://localhost:5173/phone
6. Choisir l'extension 1001 et cliquer Enregistrer (autoriser le micro)
7. Declencher une alerte / Executer le pipeline
8. L'appel SIP sonne dans le navigateur
9. Repondre, entendre le message TTS, raccrocher
10. Controler audit, notification et (si refus/timeout) l'escalade
