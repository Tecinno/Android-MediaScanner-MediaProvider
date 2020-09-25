package com.czy.jni;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

public class Video extends AppCompatActivity {
    private VideoView videoViewSys;
    private int videoId;
    final String TAG = "Scanner";
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
        videoId = intent.getIntExtra("_id",-1);
        Log.e("VIDEO", "Video ONRESUME PATH:"+path);
        videoViewSys = (VideoView) findViewById(R.id.videoView);
        videoViewSys.setVideoPath(path);
        videoViewSys.setMediaController(new MediaController(this));
        videoViewSys.start();
    }
    public void videoFavorite_button(View view) {
        Log.e("VIDEO", "Video video_back_button");
            if(videoId == -1)
                return;
            Uri video = Uri.parse( "content://media.scan/video");
            Cursor c = getContentResolver().query(video, new String[]{"is_favorite"}, "_id = ?",new String[]{String.valueOf(videoId)},null);
            Log.e("Scanner", "============isFavorite_button================ ");
            if (c == null)
                Log.e(TAG, "c == null ");
            if (c.moveToFirst()) {
                do{
                    int oldFavorite = c.getInt(c.getColumnIndex( "is_favorite"));
                    Log.e(TAG,  "id is :"+videoId+ ", is_favorite is :" + oldFavorite);
                    ContentValues value = new ContentValues();
                    if (oldFavorite == 1) {
                        value.put("is_favorite","0");
                    } else {
                        value.put("is_favorite","1");
                    }
                    getContentResolver().update(video,value,"_id = ?", new String[]{String.valueOf(videoId)});
                    if (oldFavorite == 1) {
                        Toast.makeText(this, "取消收藏", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(this, "添加收藏", Toast.LENGTH_SHORT).show();
                    }
                } while (c.moveToNext());
            }
    }
    public void video_back_button(View view) {
        Log.e("VIDEO", "Video video_back_button");

        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fate_enter,R.anim.fate_out);
    }
}
