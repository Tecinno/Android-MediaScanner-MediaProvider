package com.czy.jni;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;

public class Video extends AppCompatActivity {
    private VideoView videoViewSys;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_menu);
        Log.e("VIDEO", "Video oncreate ");
    }

    protected void onResume() {
        super.onResume();
        onNewIntent(this.getIntent());
        Intent intent = getIntent();
        String path = intent.getStringExtra("path");
        Log.e("VIDEO", "Video ONRESUME PATH:"+path);
        videoViewSys = (VideoView) findViewById(R.id.videoView);
        videoViewSys.setVideoPath(path);
        videoViewSys.setMediaController(new MediaController(this));
        videoViewSys.start();


    }
    public void video_back_button(View view) {
        Log.e("VIDEO", "Video video_back_button");
        finish();
    }

}
