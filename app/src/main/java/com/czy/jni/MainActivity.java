package com.czy.jni;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    final static String TAG = "ScannerProvider";
    private MediaPlayer player = new MediaPlayer();
    private SeekBar seekbar;
    private Button play_pause, reset, last, next;
    private boolean ifplay = false;
    private String musicName = "blueflawer.mp3";
    private boolean iffirst = false;
    private Timer mTimer;
    private TimerTask mTimerTask;
    private boolean isChanging=false;//互斥变量，防止定时器与SeekBar拖动时进度冲突
    private CurrentAudioData audioData;
    private CurrentVideoData videoData;
    private TextView titleText ;
    private TextView genreText ;
    private TextView albumText ;
    private TextView artistText ;
    private String scanPath = "/sdcard/android_ubuntu/不同媒体类型";
    private enum PlayType {
        DEFAULT,LAST,NEXT
    }
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate");
        setContentView(R.layout.activity_main);
        findViews();
        audioData = new CurrentAudioData();
        videoData = new CurrentVideoData();
        getPermission();
    }

    public void getPermission(){
        Log.e(TAG, "getPermission");
        int permissionread = this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        int permissionwrite = this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permissionread != PackageManager.PERMISSION_GRANTED && permissionwrite != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "getPermission no permission !!!");
            this.requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }
    private void showAudio(PlayType type) {
        Log.e(TAG, "showAudio");
        Uri music = Uri.parse( "content://media.scan/audio");
        Cursor c;
        music = music.buildUpon().appendQueryParameter("limit", "1").build();
        if (audioData.id == -1) {
            try {
                c = getContentResolver().query(music, new String[]{"_id"}, null, null,"_id");
                if (c != null && c.moveToFirst()) {
                    audioData.id = c.getInt(c.getColumnIndex("_id"));
                }else {
                    Log.e(TAG, "showAudio no data");
                    c.close();
                    return;
                }
            }catch (Exception e){
                Log.e(TAG, "showAudio query error : " + e);
                return;
            }
        }
        String ID = audioData.id.toString();
        switch (type) {
            case LAST:c = getContentResolver().query(music, new String[]{"_id, _path"}, "_id < ?",new String[]{ID},"_id desc");
                if (c == null || !c.moveToFirst()) {
                    Log.e(TAG, "showAudio audi cursor is null");
                    c = getContentResolver().query(music, new String[]{"_id, _path"}, null, null,"_id desc");
                }
                break;
            case NEXT:
                c = getContentResolver().query(music, new String[]{"_id, _path"}, "_id > ?",new String[]{ID},"_id");
                if (c == null || !c.moveToFirst())
                    c = getContentResolver().query(music, new String[]{"_id, _path"}, null, null,"_id");
                break;
            case DEFAULT:c = getContentResolver().query(music, new String[]{"_id, _path"}, "_id = ?",new String[]{ID},null);
                break;
            default:c = getContentResolver().query(music, new String[]{"_id, _path"}, "_id = ?",new String[]{ID},null);
                break;
        }

        if (c != null && c.moveToFirst()) {
            Log.e(TAG, "showAudio c != null && c.moveToFirst()");
            audioData.path = c.getString(c.getColumnIndex("_path"));
            audioData.id = c.getInt(c.getColumnIndex("_id"));
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            try {
                Log.e(TAG, "showAudio try , path : "+audioData.path);
                mmr.setDataSource(audioData.path);
                Log.e(TAG, "showAudio after setDataSource "+audioData.path);
                audioData.title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                titleText.setText(audioData.title);
                audioData.artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                artistText.setText(audioData.artist);
                audioData.album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                albumText.setText(audioData.album);
                audioData.genre = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
                genreText.setText(audioData.genre);
                Log.e(TAG, "ID :" + audioData.id + " ,PATH : "+audioData.path+" , artist : "+audioData.artist+" , album : "+audioData.album+" , genre : "+audioData.genre);
            }catch (Exception e) {

            }
        }else {
            Log.e(TAG, "showAudio cursor is null");
        }
        c.close();
    }

    private void findViews() {
        play_pause = (Button) findViewById(R.id.play_pause);
        reset = (Button) findViewById(R.id.reset);
        last = (Button) findViewById(R.id.last);
        next = (Button) findViewById(R.id.next);
        titleText = this.findViewById(R.id.title_text);
        genreText = this.findViewById(R.id.genre_text);
        albumText = this.findViewById(R.id.album_text);
        artistText = this.findViewById(R.id.artist_text);
//        play_pause.setOnClickListener(new MyClick());
//        reset.setOnClickListener(new MyClick());
//        last.setOnClickListener(new MyClick());
//        next.setOnClickListener(new MyClick());
        seekbar = (SeekBar) findViewById(R.id.seekBar);
        seekbar.setOnSeekBarChangeListener(new MySeekbar());
    }

    public void videoMenu(View view) {
        setContentView(R.layout.video_menu);
    }

    public void audioMenu(View view) {
        setContentView(R.layout.activity_main);
    }

    public void buttonlisten(View view) {
//        opendatabase(true);
//        myProviderQuery();
//        mediascanner();
        Thread mthread = new Thread(new Runnable() {
            @Override
            public  void run(){
                mediascanner();
                myProviderQuery();
//                opendatabase(true);
            }
        });mthread.start();

    }
    private void opendatabase(boolean isAudio) {
        Log.e("MainThread", " myProviderInsert");
        ContentValues values = new ContentValues();
        String URL;
        if (isAudio == true)
            URL = "content://media.scan/audio";
        else
            URL = "content://media.scan/video";

        Uri music = Uri.parse(URL);


        values.put(MediaProvider.NAME, "open database");

//        values.put(MediaProvider.PATH,);
        Uri uri = getContentResolver().insert(
                music, values);

        Toast.makeText(getBaseContext(),
                uri.toString(), Toast.LENGTH_LONG).show();
    }
    //当前音乐信息存储
    class CurrentAudioData {
        public CurrentAudioData() {
            id = -1;
        }
        public Integer id;
        public String path;
        public String artist;
        public String album;
        public String genre;
        public String title;
    }
    //当前视频信息存储
    class CurrentVideoData {
        public int id;
        public String path;
    }
    //进度条处理
    class MySeekbar implements SeekBar.OnSeekBarChangeListener {
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
        }

        public void onStartTrackingTouch(SeekBar seekBar) {
            isChanging=true;
        }

        public void onStopTrackingTouch(SeekBar seekBar) {
            player.seekTo(seekbar.getProgress());
            isChanging=false;
        }

    }

//    class MyClick implements View.OnClickListener {
//        public void onClick(View v) {
//            if (audioData.path == null) {
//                showAudio(PlayType.DEFAULT);
//                if (audioData.path == null) {
//                    Log.e(TAG, "MyClick audioData.path is null");
//                    return;
//                }
//            }
//            File file = new File(audioData.path);
//            // 判断有没有要播放的文件
//            if (file.exists()) {
//                Log.e(TAG, "file exist : "+file);
//                switch (v.getId()) {
//                    case R.id.play_pause:
//                        if (player != null && !ifplay) {
//                            play_pause.setText("暂停");
//                            if (!iffirst) {
//                                player.reset();
//                                try {
//                                    player.setDataSource(file.getAbsolutePath());
//                                    player.prepare();// 准备
//
//                                } catch (IllegalArgumentException e) {
//                                    e.printStackTrace();
//                                } catch (IllegalStateException e) {
//                                    e.printStackTrace();
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//                                seekbar.setMax(player.getDuration());//设置进度条
//                                //----------定时器记录播放进度---------//
//                                mTimer = new Timer();
//                                mTimerTask = new TimerTask() {
//                                    @Override
//                                    public void run() {
//                                        if(isChanging==true) {
//                                            return;
//                                        }
//                                        seekbar.setProgress(player.getCurrentPosition());
//                                    }
//                                };
//                                mTimer.schedule(mTimerTask, 0, 10);
//                                iffirst=true;
//                            }
//                            player.start();// 开始
//                            ifplay = true;
//                        } else if (ifplay) {
//                            play_pause.setText("播放");
//                            player.pause();
//                            ifplay = false;
//                        }
//                        break;
//                    case R.id.reset:
//                        if (ifplay) {
//                            player.seekTo(0);
//                        } else {
//                            player.reset();
//                            try {
//                                player.setDataSource(file.getAbsolutePath());
//                                player.prepare();// 准备
//                                player.start();// 开始
//                                player.seekTo(0);
//                            } catch (IllegalArgumentException e) {
//                                e.printStackTrace();
//                            } catch (IllegalStateException e) {
//                                e.printStackTrace();
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                        break;
//                    case R.id.next:
//                        Log.e(TAG, "MyClick next");
//                        showAudio(PlayType.NEXT);
//                        if (audioData.path == null) {
//                            Log.e(TAG, "MyClick next audioData.path is null");
//                            return;
//                        }else {
//                            Log.e(TAG, "MyClick next audioData.path is "+ audioData.path);
//                        }
//                        if(ifplay) {
//                            player.pause();
//                        }
//                        ifplay = true;
//                        file = new File(audioData.path);
//                        try {
//                            player.reset();
//                            player.setDataSource(file.getAbsolutePath());
//                            player.prepare();// 准备
//                            player.start();// 开始
//                            player.seekTo(0);
//                        } catch (IllegalArgumentException e) {
//                            e.printStackTrace();
//                        } catch (IllegalStateException e) {
//                            e.printStackTrace();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                        break;
//                    case R.id.last:
//                        Log.e(TAG, "MyClick last");
//                        showAudio(PlayType.LAST);
//                        if (audioData.path == null) {
//                            Log.e(TAG, "MyClick last audioData.path is null");
//                            return;
//                        } else {
//                            Log.e(TAG, "MyClick last audioData.path is "+ audioData.path);
//                        }
//                        if(ifplay) {
//                            player.pause();
//                        }
//                        ifplay = true;
//                        file = new File(audioData.path);
//                        try {
//                            player.reset();
////                            player.release();
//                            player.setDataSource(file.getAbsolutePath());
//                            player.prepare();// 准备
//                            player.start();// 开始
//                        } catch (IllegalArgumentException e) {
//                            e.printStackTrace();
//                        } catch (IllegalStateException e) {
//                            e.printStackTrace();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                        break;
//                }
//            } else {
//                Log.e(TAG, "file no exist : "+file);
//            }
//        }
//    }
    public void play_pause_button(View view) {
        Log.e(TAG, " play_pause_button");
        if (audioData.path == null) {
            showAudio(PlayType.DEFAULT);
            if (audioData.path == null) {
                Log.e(TAG, "play_pause_button audioData.path is null");
                return;
            }
        }
        File file = new File(audioData.path);
        if (file.exists()) {
            Log.e(TAG, "file exist : " + file);
            if (player != null && !ifplay) {
                play_pause.setText("暂停");
                if (!iffirst) {
                    player.reset();
                    try {
                        player.setDataSource(file.getAbsolutePath());
                        player.prepare();// 准备

                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    seekbar.setMax(player.getDuration());//设置进度条
                    //----------定时器记录播放进度---------//
                    mTimer = new Timer();
                    mTimerTask = new TimerTask() {
                        @Override
                        public void run() {
                            if (isChanging == true) {
                                return;
                            }
                            seekbar.setProgress(player.getCurrentPosition());
                        }
                    };
                    mTimer.schedule(mTimerTask, 0, 10);
                    iffirst = true;
                }
                player.start();// 开始
                ifplay = true;
            }else if (ifplay) {
                play_pause.setText("播放");
                player.pause();
                ifplay = false;
            }
        }
    }

    public void next_button(View view) {
        Log.e(TAG, " next_button");
        showAudio(PlayType.NEXT);
        if (audioData.path == null) {
            Log.e(TAG, "MyClick next audioData.path is null");
            return;
        }else {
            Log.e(TAG, "MyClick next audioData.path is "+ audioData.path);
        }
        File file = new File(audioData.path);
        if (!file.exists()) {
            Log.e(TAG, "next_button file is not exist");
            return;
        }
        if(ifplay) {
            player.pause();
        }
        ifplay = true;
        file = new File(audioData.path);
        try {
            player.reset();
            player.setDataSource(file.getAbsolutePath());
            player.prepare();// 准备
            player.start();// 开始
            player.seekTo(0);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void reset_button(View view) {
        Log.e(TAG, " reset_button");
        if (audioData.path == null) {
            showAudio(PlayType.DEFAULT);
            if (audioData.path == null) {
                Log.e(TAG, "MyClick audioData.path is null");
                return;
            }
        }
        File file = new File(audioData.path);
        if (!file.exists()) {
            Log.e(TAG, "reset_button file is not exist");
            return;
        }
        if (ifplay) {
            player.seekTo(0);
        } else {
            player.reset();
            try {
                player.setDataSource(file.getAbsolutePath());
                player.prepare();// 准备
                player.start();// 开始
                player.seekTo(0);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void last_button(View view) {
        Log.e(TAG, " last_button");
        showAudio(PlayType.LAST);
        if (audioData.path == null) {
            Log.e(TAG, "MyClick last audioData.path is null");
            return;
        } else {
            Log.e(TAG, "MyClick last audioData.path is "+ audioData.path);
        }
        File file = new File(audioData.path);
        if (!file.exists()) {
            Log.e(TAG, "last_button file is not exist");
            return;
        }
        if(ifplay) {
            player.pause();
        }
        ifplay = true;

        try {
            player.reset();
            player.setDataSource(file.getAbsolutePath());
            player.prepare();// 准备
            player.start();// 开始
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void audiomenulist(View view) {
        Log.e(TAG, " audiomenulist");
    }
    private void myProviderQuery() {
        Log.e(TAG, " myProviderQuery");
        String URL = "content://media.scan/audio";
        String URLV = "content://media.scan/video";
        Uri music = Uri.parse(URL);
        music = music.buildUpon().appendQueryParameter("limit", "10").build();
        Uri video = Uri.parse(URLV);

        Cursor c = getContentResolver().query(music, null, null,null,null);
        Cursor v = getContentResolver().query(video, null, null,null,null);
        Log.e("query", "============audio================ ");
        if (c == null)
            Log.e(TAG, "c == null ");
        long  startTime = System.currentTimeMillis();
        if (c.moveToFirst()) {
            do{
                Log.e(TAG,  "id is :"+c.getInt(c.getColumnIndex( MediaProvider.ID))+ ", name is :" + c.getString(c.getColumnIndex( MediaProvider.NAME)) + ", path is :" + c.getString(c.getColumnIndex( MediaProvider.PATH)));
            } while (c.moveToNext());
        }
        Log.e(TAG, "============video================ ");
        if (v == null)
            Log.e(TAG, "v == null ");
        if (v.moveToFirst()) {
            do{
                Log.e(TAG,  "id is :"+v.getInt(v.getColumnIndex( MediaProvider.ID))+ ", name is :" + v.getString(v.getColumnIndex( MediaProvider.NAME)) + ", path is :" + v.getString(v.getColumnIndex( MediaProvider.PATH)));
            } while (v.moveToNext());
        }

    }
    public void mediascanner(){
        long startTime = System.nanoTime();
        scan(scanPath);
        long endTime = System.nanoTime();
        Log.e(TAG,"scan resume time : " + (endTime-startTime));
    }

    protected void onPause() {
        if(player != null){
            if(player.isPlaying()){
                player.pause();
            }
        }
        super.onPause();
    }

    protected void onResume() {
        if(player != null){
            if(!player.isPlaying()){
                player.start();
            }
        }
        super.onResume();
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native int[] scan(String title);
}
