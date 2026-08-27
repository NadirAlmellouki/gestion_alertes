package FST.MST_RSI.PFA.voicemessage.infrastructure.tts;

import FST.MST_RSI.PFA.voicemessage.domain.model.TtsAudio;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class PcmWavTone {

    private PcmWavTone() {
    }

    public static TtsAudio fromPcm16le(byte[] pcm, int sampleRate) {
        return new TtsAudio(wrapWav(pcm, sampleRate), "audio/wav");
    }

    public static TtsAudio shortAlertTone() {
        int sampleRate = 8000;
        int durationMs = 1200;
        int samples = sampleRate * durationMs / 1000;
        byte[] pcm = new byte[samples * 2];
        ByteBuffer buffer = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < samples; i++) {
            double t = i / (double) sampleRate;
            short value = (short) (Math.sin(2 * Math.PI * 880 * t) * 12000);
            buffer.putShort(value);
        }
        return new TtsAudio(wrapWav(pcm, sampleRate), "audio/wav");
    }

    private static byte[] wrapWav(byte[] pcm, int sampleRate) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            int byteRate = sampleRate * 2;
            out.write("RIFF".getBytes());
            writeInt(out, 36 + pcm.length);
            out.write("WAVE".getBytes());
            out.write("fmt ".getBytes());
            writeInt(out, 16);
            writeShort(out, (short) 1);
            writeShort(out, (short) 1);
            writeInt(out, sampleRate);
            writeInt(out, byteRate);
            writeShort(out, (short) 2);
            writeShort(out, (short) 16);
            out.write("data".getBytes());
            writeInt(out, pcm.length);
            out.write(pcm);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return out.toByteArray();
    }

    private static void writeInt(ByteArrayOutputStream out, int value) {
        out.write(value & 0xff);
        out.write((value >> 8) & 0xff);
        out.write((value >> 16) & 0xff);
        out.write((value >> 24) & 0xff);
    }

    private static void writeShort(ByteArrayOutputStream out, short value) {
        out.write(value & 0xff);
        out.write((value >> 8) & 0xff);
    }
}
