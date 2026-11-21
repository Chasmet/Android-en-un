package fr.chk.alienlive;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/** Standalone on-device voice tester. The LIVE activity uses the same engine. */
public final class VoiceActivity extends Activity {
    private VoiceEngine engine;
    private TextView state;
    private Button toggle;
    private boolean requested;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);setContentView(R.layout.activity_voice);
        state=findViewById(R.id.voiceState);
        toggle=findViewById(R.id.voiceToggle);
        engine=new VoiceEngine(s->runOnUiThread(()->state.setText(s)));
        Spinner presets=findViewById(R.id.voicePresets);
        presets.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Pluthor — grave profond","Annunaki — métallique","Spectral — aigu","Normal — test"}));
        presets.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(android.widget.AdapterView<?> p,android.view.View v,int pos,long id){
                engine.setPreset(pos);AlienData.prefs(VoiceActivity.this).edit().putInt("voicePreset",pos).apply();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
        presets.setSelection(AlienData.prefs(this).getInt("voicePreset",0));
        toggle.setOnClickListener(v->{if(engine.isRunning()){
            requested=false;engine.stop();toggle.setText("Tester ma voix extraterrestre");
        }else {requested=true;startAudio();}});
    }
    private void startAudio(){
        if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},77);return;
        }
        if(engine.start())toggle.setText("Arrêter la voix");else requested=false;
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grantResults) {
        super.onRequestPermissionsResult(request,permissions,grantResults);
        if(request==77){if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED&&requested)startAudio();
        else {requested=false;state.setText("Permission microphone nécessaire");}}
    }
    @Override protected void onPause(){ if(engine!=null)engine.stop();super.onPause();}
    @Override protected void onResume(){super.onResume();if(requested&&engine!=null)startAudio();}
    @Override protected void onDestroy(){if(engine!=null)engine.stop();super.onDestroy();}
}
