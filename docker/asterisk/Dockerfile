# Prebuilt Asterisk 22 (andrius/asterisk) already ships PJSIP, WebRTC/WSS,
# ARI, RTP/SRTP and WAV playback. Do not compile from source.
FROM andrius/asterisk:22

USER root

RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        ffmpeg \
        openssl \
        ca-certificates \
    && rm -rf /var/lib/apt/lists/*

COPY config/http.conf config/ari.conf config/pjsip.conf \
     config/rtp.conf config/extensions.conf config/logger.conf \
     /etc/asterisk/
COPY entrypoint.sh /alertops-entrypoint.sh

RUN chmod +x /alertops-entrypoint.sh \
    && mkdir -p /var/lib/asterisk/sounds/alertops /etc/asterisk/keys

EXPOSE 5060/udp 5060/tcp 8088 8089 10000-10100/udp

ENTRYPOINT ["/alertops-entrypoint.sh"]
CMD ["/usr/sbin/asterisk", "-vvvdddf", "-T", "-W", "-U", "asterisk", "-p"]
