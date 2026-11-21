package fr.chk.alienlive;

import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Process;

/** Local offline voice effect: granular two-tap pitch shifter and subtle robotic texture.
 *  Plays through the Android media output. It does NOT replace another app's microphone.
 *  This output is capturable ONLY if the screen-sharing app supports internal media audio.
 */
public final class VoiceEngine {
    public interface Listener { void onState(String message); }
    public static final int SAMPLE_RATE = 22050;
    private static final int RING = 1 << 15;
    private final Listener listener;
    private volatile boolean running;
    private volatile int preset = 0;
    private Thread worker;
    private AudioRecord input;
    private AudioTrack output;
    private final float[] history = new float[RING];
    private final float[] echoHistory = new float[4096];
    private long wrote = 0;
    private float phase = 0f;
    private int echoAt = 0;

    public VoiceEngine(Listener listener) { this.listener = listener; }
    public synchronized boolean isRunning() { return running; }
    public void setPreset(int p) { preset = Math.max(0,Math.min(3,p)); }
    private void state(String s) { if(listener!=null)listener.onState(s); }

    /** Caller must have requested and received RECORD_AUDIO runtime permission. */
    @SuppressWarnings("MissingPermission")
    public synchronized boolean start() {
        if(running) return true;
        try {
            int minIn = AudioRecord.getMinBufferSize(SAMPLE_RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
            int minOut = AudioTrack.getMinBufferSize(SAMPLE_RATE,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);
            if(minIn<=0 || minOut<=0) {state("Audio non pris en charge par ce téléphone");return false;}
            input = new AudioRecord(MediaRecorder.AudioSource.MIC,SAMPLE_RATE,AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, Math.max(8192,minIn*2));
            if(input.getState()!=AudioRecord.STATE_INITIALIZED){release();state("Microphone indisponible");return false;}
            AudioAttributes.Builder a = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH);
            if(Build.VERSION.SDK_INT>=29) a.setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_ALL);
            AudioFormat fmt = new AudioFormat.Builder().setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();
            output = new AudioTrack(a.build(),fmt,Math.max(8192,minOut*2),AudioTrack.MODE_STREAM,AudioManager.AUDIO_SESSION_ID_GENERATE);
            if(output.getState()!=AudioTrack.STATE_INITIALIZED){release();state("Sortie audio indisponible");return false;}
            wrote=0;phase=0;echoAt=0;
            java.util.Arrays.fill(history,0f);java.util.Arrays.fill(echoHistory,0f);
            input.startRecording();
            output.play();
            running=true;
            worker = new Thread(this::loop,"CHK-Voice-FX");
            worker.start();
            state("Voix transformée dans CHK • sortie audio média");
            return true;
        } catch (SecurityException e) {release();state("Autorisation microphone refusée");return false;}
          catch (Exception e) {release();state("Audio non initialisé : "+e.getClass().getSimpleName());return false;}
    }
    private void loop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
        final short[] in = new short[512];
        final short[] out = new short[512];
        long silentSamples=0;
        while(running) {
            int count;
            try{ count=input.read(in,0,in.length); }
            catch(Exception e){ if(running) state("Enregistrement interrompu");break; }
            if(count<=0){if(count<0)state("Le microphone a été interrompu");break;}
            int energy=0;
            for(int i=0;i<count;i++){
                energy |= Math.abs((int)in[i]);
                out[i] = transform(in[i]);
            }
            if(energy==0){silentSamples+=count;}
            else {silentSamples=0;}
            if(silentSamples>SAMPLE_RATE*4){
                state("Aucun son du micro : vérifie si TikTok le monopolise");
                silentSamples=0;
            }
            try {int written=0;while(written<count&&running){int n=output.write(out,written,count-written);if(n<=0)break;written+=n;}}
            catch(Exception e){if(running)state("Lecture audio interrompue");break;}
        }
        running=false;
    }
    private float read(float delay) {
        float index = (float)wrote-delay;
        int lo=(int)Math.floor(index);
        float sub=index-lo;
        float a=history[lo&(RING-1)];
        float b=history[(lo+1)&(RING-1)];
        return a+(b-a)*sub;
    }
    private short transform(short sample) {
        float inputSample=sample/32768f;
        history[(int)(wrote&(RING-1))]=inputSample;
        int mode=preset;
        float pitch=mode==0?.68f:(mode==1?.83f:(mode==2?1.21f:1f));
        float v;
        if(mode==3 || wrote<5500){v=inputSample;}
        else {
            float sweep=2600f;
            phase+=(1f-pitch)/sweep;
            if(phase>=1f)phase-=1f;
            if(phase<0f)phase+=1f;
            float other=phase+.5f;
            if(other>=1f)other-=1f;
            float w=.5f-.5f*(float)Math.cos(2f*Math.PI*phase);
            float a=read(1350f+sweep*phase);
            float b=read(1350f+sweep*other);
            v=w*a+(1f-w)*b;
        }
        if(mode!=3) {
            float old=echoHistory[echoAt];
            echoHistory[echoAt]=v;
            echoAt=(echoAt+1)&(echoHistory.length-1);
            float mix=mode==2?0.09f:0.17f;
            v=v*.92f+old*mix;
            if(mode==1) v+=.10f*v*(float)Math.sin(2*Math.PI*120*wrote/SAMPLE_RATE);
            if(mode==2) v+=.07f*v*(float)Math.sin(2*Math.PI*48*wrote/SAMPLE_RATE);
        }
        wrote++;
        float clipped = v*1.3f;
        clipped=clipped/(1f+Math.abs(clipped)*.4f);
        return (short)(Math.max(-1,Math.min(1,clipped))*32767f);
    }
    public synchronized void stop() {
        running=false;
        if(input!=null){try{input.stop();}catch(Exception ignored){}}
        Thread t=worker;
        if(t!=null&&t!=Thread.currentThread()){try{t.join(250);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
        worker=null;
        release();
        state("Effet vocal arrêté");
    }
    private void release() {
        if(input!=null){try{input.release();}catch(Exception ignored){} input=null;}
        if(output!=null){try{output.pause();output.flush();output.release();}catch(Exception ignored){}output=null;}
    }
}
