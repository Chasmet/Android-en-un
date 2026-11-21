package fr.chk.alienlive;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private AlienStageView stage;
    private FaceTracker tracker;
    private View controls,header,hint;
    private boolean full=false;
    private TextView status;
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        setContentView(R.layout.activity_main);
        stage=findViewById(R.id.alienStage);
        controls=findViewById(R.id.controlPanel);
        header=findViewById(R.id.header);
        hint=findViewById(R.id.liveHint);
        status=findViewById(R.id.status);
        findViewById(R.id.buttonLive).setOnClickListener(v->setLive(true));
        findViewById(R.id.buttonAlien).setOnClickListener(v->openLibrary(false));
        findViewById(R.id.buttonScene).setOnClickListener(v->openLibrary(true));
        findViewById(R.id.buttonSettings).setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));
        stage.setOnLongClickListener(v->{
            if(full){setLive(false);return true;}
            return false;
        });
        tracker=new FaceTracker(this,new FaceTracker.Listener(){
            @Override public void onPose(float x,float y,float roll,float mouth){runOnUiThread(()->stage.updatePose(x,y,roll,mouth));}
            @Override public void onStatus(String message){runOnUiThread(()->status.setText(message));}
        });
    }
    private void openLibrary(boolean scenes){
        Intent intent=new Intent(this,LibraryActivity.class);
        intent.putExtra("scenes",scenes);startActivity(intent);
    }
    private void setLive(boolean enabled){
        full=enabled;
        stage.setCleanMode(enabled);
        controls.setVisibility(enabled?View.GONE:View.VISIBLE);
        header.setVisibility(enabled?View.GONE:View.VISIBLE);
        hint.setVisibility(enabled?View.GONE:View.VISIBLE);
        if(enabled){
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }else{
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        applySystemUi();
    }
    private void applySystemUi(){
        int flags = full ? (View.SYSTEM_UI_FLAG_FULLSCREEN
            |View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            |View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            |View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            |View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            |View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN) : 0;
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }
    @Override public void onWindowFocusChanged(boolean hasFocus){
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus)applySystemUi();
    }
    @Override protected void onResume(){
        super.onResume();stage.reload();
        tracker.setSaver(AlienData.prefs(this).getBoolean("saveBattery",true));
        if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.CAMERA},100);
        }else{tracker.start();}
        long now=System.currentTimeMillis();
        long previousCheck=AlienData.prefs(this).getLong("lastUpdateCheck",0L);
        if(AlienData.prefs(this).getBoolean("autoUpdate",true)&&now-previousCheck>86400000L){
            AlienData.prefs(this).edit().putLong("lastUpdateCheck",now).apply();
            UpdateManager.check(this,false);
        }
    }
    @Override public void onRequestPermissionsResult(int request,String[] perms,int[] results){
        super.onRequestPermissionsResult(request,perms,results);
        if(request==100&&results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)tracker.start();
        else status.setText("Autorise la caméra pour animer le personnage");
    }
    @Override protected void onPause(){tracker.stop();super.onPause();}
    @Override public void onBackPressed(){if(full)setLive(false);else super.onBackPressed();}
}
