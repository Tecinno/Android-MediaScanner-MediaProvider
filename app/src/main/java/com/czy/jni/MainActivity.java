package com.czy.jni;

import androidx.appcompat.app.AppCompatActivity;
//import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import android.os.Trace;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    final static String TAG = "Scanner";
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
    private MyBroadcastReceiver broad;
    private enum PlayType {
        DEFAULT,LAST,NEXT
    }
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");

        setContentView(R.layout.activity_main);
        findViews();
        audioData = new CurrentAudioData();
        videoData = new CurrentVideoData();
        //注册广播
//        intentRegister();
        //获取文件读写权限
        getPermission();
    }
    //注册广播
    private void intentRegister() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.media");
        broad = new MyBroadcastReceiver();
        registerReceiver(broad, intentFilter);
    }
    //广播接收测试
    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context content, Intent intent){
            final String action = intent.getAction();
            Log.i(TAG, "MainActivity MyBroadcastReceiver + "+ action);
//            startMyActivity();
//            showDiaglog();
        }
    }
    //启动测试
    private void startMyActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("event",1);
        startActivity(intent);
        overridePendingTransition(R.anim.fate_enter,R.anim.fate_out);
    }
    //广播测试
    public void sendBroad() {
        Intent intent = new Intent();
        intent.setAction("android.net.conn.media");
        sendBroadcast(intent);
        Log.i(TAG,"sendBroad OK");
    }
    //弹窗测试
    private void showDiaglog() {
        HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
        map.put(0,0);
        int a = map.get(1).intValue();
        Log.i(TAG, "MainActivity showDiaglog   ");
        Dialog dialog = new Dialog(this);
        //去掉标题线
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog);
        //背景透明
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        Window window = dialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
//        lp.gravity = Gravity.CENTER; // 居中位置
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(lp);
        window.setWindowAnimations(R.style.mystyle);  //添加动画
    }
    public void getPermission(){
        Log.i(TAG, "getPermission");
        int permissionread = this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        int permissionwrite = this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permissionread != PackageManager.PERMISSION_GRANTED && permissionwrite != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "getPermission no permission !!!");
            this.requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }
    private void showAudio(PlayType type) {
        Trace.beginSection("showAudio");
        Log.i(TAG, "showAudio");
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
            case LAST:c = getContentResolver().query(music, new String[]{"_id", "_path", "_name"}, "_id < ?",new String[]{ID},"_id desc");
                if (c == null || !c.moveToFirst()) {
                    Log.e(TAG, "showAudio audi cursor is null");
                    c = getContentResolver().query(music, new String[]{"_id", "_path", "_name"}, null, null,"_id desc");
                }
                break;
            case NEXT:
                c = getContentResolver().query(music, new String[]{"_id", "_path", "_name"}, "_id > ?",new String[]{ID},"_id");
                if (c == null || !c.moveToFirst())
                    c = getContentResolver().query(music, new String[]{"_id", "_path", "_name"}, null, null,"_id");
                break;
            case DEFAULT:c = getContentResolver().query(music, new String[]{"_id", "_path", "_name"}, "_id = ?",new String[]{ID},null);
                break;
            default:c = getContentResolver().query(music, new String[]{"_id", "_path", "_name"}, "_id = ?",new String[]{ID},null);
                break;
        }

        if (c != null && c.moveToFirst()) {
            Log.e(TAG, "showAudio c != null && c.moveToFirst()");
            audioData.path = c.getString(c.getColumnIndex("_path"));
            audioData.id = c.getInt(c.getColumnIndex("_id"));
            audioData.name = c.getString(c.getColumnIndex("_name"));
            if (audioData.name == null && audioData.path != null) {

                audioData.name = audioData.path.substring(audioData.path.lastIndexOf("/")+1);
                Log.e(TAG, "audioData name null : "+audioData.name);
            }
        }
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            Log.i(TAG, "showAudio try , path : "+audioData.path);
            mmr.setDataSource(audioData.path);
            Log.i(TAG, "showAudio after setDataSource "+audioData.path);
            audioData.title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            titleText.setText(audioData.title == null ? audioData.name : audioData.title);
            audioData.artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            artistText.setText(audioData.artist);
            audioData.album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            albumText.setText(audioData.album);
            audioData.genre = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
            genreText.setText(audioData.genre);
            Log.i(TAG, "title : " +  audioData.title + " ,ID :" + audioData.id + " ,PATH : "+audioData.path+" , artist : "+audioData.artist+" , album : "+audioData.album+" , genre : "+audioData.genre);
        }catch (Exception e) {
            Log.e(TAG, "setDataSource error : " + e);
        }
        if (c != null) c.close();
        Trace.endSection();
    }

    private void findViews() {
        play_pause = (Button) findViewById(R.id.play_pause);
        last = (Button) findViewById(R.id.last);
        next = (Button) findViewById(R.id.next);
        titleText = this.findViewById(R.id.title_text);
        genreText = this.findViewById(R.id.genre_text);
        albumText = this.findViewById(R.id.album_text);
        artistText = this.findViewById(R.id.artist_text);
        seekbar = (SeekBar) findViewById(R.id.seekBar);
        seekbar.setOnSeekBarChangeListener(new MySeekbar());
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                int progress = seekbar.getProgress();
                Log.d("tag", "播放完毕" + progress);
                if (progress != 0)
                    playnext();
            }
        });
    }

    public void menu_button(View view) {
        Intent intent = new Intent(this, Menu.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fate_enter,R.anim.fate_out);
    }
    public void delete_button(View view) {
        Log.i("Scanner", "============delete_button================ ");
        Uri music = Uri.parse( "content://media.scan/audio");
        ContentValues values = new ContentValues();
        values.put(MediaProvider.NAME, "delete database");
        getContentResolver().insert(music, values);
        Toast.makeText(MainActivity.this, "删除完成", Toast.LENGTH_SHORT).show();
    }

    public void isFavorite_button(View view) {
        if(audioData.id == -1)
            return;
        Uri music = Uri.parse( "content://media.scan/audio");
        Cursor c = getContentResolver().query(music, new String[]{"is_favorite"}, "_id = ?",new String[]{String.valueOf(audioData.id)},null);
        Log.i("Scanner", "============isFavorite_button================ ");
        if (c == null)
            Log.e(TAG, "c == null ");
        if (c.moveToFirst()) {
            do{
                int oldFavorite = c.getInt(c.getColumnIndex( "is_favorite"));
                Log.i(TAG,  "id is :"+audioData.id+ ", is_favorite is :" + oldFavorite);
                ContentValues value = new ContentValues();
                if (oldFavorite == 1) {
                    value.put("is_favorite","0");
                } else {
                    value.put("is_favorite","1");
                }
                getContentResolver().update(music,value,"_id = ?",new String[]{String.valueOf(audioData.id)});
                if (oldFavorite == 1) {
                    Toast.makeText(MainActivity.this, "取消收藏", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(MainActivity.this, "添加收藏", Toast.LENGTH_SHORT).show();
                }
            } while (c.moveToNext());
        }
        if (c != null) c.close();
    }

    public void scan_button(View view) {
        Thread mthread = new Thread(new Runnable() {
            @Override
            public  void run(){
            Looper.prepare();
            opendatabase();
            Cursor a = getContentResolver().query(Uri.parse( "content://media.scan/audio"), new String[]{"_id"}, null,null,null);
            Cursor v = getContentResolver().query(Uri.parse( "content://media.scan/video"), new String[]{"_id"}, null,null,null);
            Cursor d = getContentResolver().query(Uri.parse( "content://media.scan/folder_dir"), new String[]{"_id"}, null,null,null);
            Toast.makeText(MainActivity.this, "扫描完成", Toast.LENGTH_SHORT).show();
            Toast.makeText(MainActivity.this, "音乐："+a.getCount()+",视频："+v.getCount()+"，文件夹："+d.getCount(), Toast.LENGTH_SHORT).show();
            a.close();v.close();d.close();
            Looper.loop();
            }
        });mthread.start();

    }

    public void back_button(View view) {
        Log.i(TAG, " back_button");
        finish();
    }
    //打开数据库，开始扫描
    private void opendatabase() {
        Log.i(TAG, " myProviderInsert");
        ContentValues values = new ContentValues();
        String URL;
        URL = "content://media.scan/audio";
        Uri music = Uri.parse(URL);
        values.put(MediaProvider.NAME, "open database");
        Uri uri = getContentResolver().insert(
                music, values);
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
        public String name;
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
            if(!ifplay){
                player.start();
                ifplay = true;
            }
            isChanging=false;

        }
    }
    public void play() {
        if (audioData.path == null) {
            showAudio(PlayType.DEFAULT);
            if (audioData.path == null) {
                Log.e(TAG, "play_pause_button audioData.path is null");
                return;
            }
        }
        File file = new File(audioData.path);
        if (file.exists()) {
            Log.i(TAG, "file exist : " + file);
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
    public void play_button(View view) {
        Log.i(TAG, " play_pause_button");
        play();
    }

    public void next_button(View view) {
        Log.i(TAG, " next_button");
        playnext();
    }
    private void playnext() {
        showAudio(PlayType.NEXT);
        if (audioData.path == null) {
            Log.e(TAG, "MyClick next audioData.path is null");
            return;
        }else {
            Log.i(TAG, "MyClick next audioData.path is "+ audioData.path);
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
        try {
            player.reset();
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
        player.start();// 开始
        ifplay = true;
        play_pause.setText("暂停");
    }
    public void last_button(View view) {
        Log.i(TAG, " last_button");
        playlast();
    }
    private void playlast() {
        showAudio(PlayType.LAST);
        if (audioData.path == null) {
            Log.e(TAG, "MyClick last audioData.path is null");
            return;
        } else {
            Log.i(TAG, "MyClick last audioData.path is "+ audioData.path);
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
        player.start();// 开始
        ifplay = true;
        play_pause.setText("暂停");
    }
    private void myProviderQuery() {
        Log.i(TAG, " myProviderQuery");
        String URL = "content://media.scan/audio";
        String URLV = "content://media.scan/video";
        Uri music = Uri.parse(URL);
        music = music.buildUpon().appendQueryParameter("limit", "10").build();
        Uri video = Uri.parse(URLV);

        Cursor c = getContentResolver().query(music, null, null,null,null);
        Cursor v = getContentResolver().query(video, null, null,null,null);
        Log.i(TAG, "============audio================ ");
        if (c == null)
            Log.e(TAG, "c == null ");
        long  startTime = System.currentTimeMillis();
        if (c.moveToFirst()) {
            do{
                Log.i(TAG,  "id is :"+c.getInt(c.getColumnIndex( MediaProvider.ID))+ ", name is :" + c.getString(c.getColumnIndex( MediaProvider.NAME)) + ", path is :" + c.getString(c.getColumnIndex( MediaProvider.PATH)));
            } while (c.moveToNext());
        }
        Log.i(TAG, "============video================ ");
        if (v == null)
            Log.e(TAG, "v == null ");
        if (v.moveToFirst()) {
            do{
                Log.i(TAG,  "id is :"+v.getInt(v.getColumnIndex( MediaProvider.ID))+ ", name is :" + v.getString(v.getColumnIndex( MediaProvider.NAME)) + ", path is :" + v.getString(v.getColumnIndex( MediaProvider.PATH)));
            } while (v.moveToNext());
        }
        if (c != null) c.close();
        if (v != null) v.close();

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
        onNewIntent(this.getIntent());
        Intent intent = getIntent();
        audioData.id = intent.getIntExtra("id",audioData.id);
        showAudio(PlayType.DEFAULT);
        play();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "MainActivity onDestroy ");
        unregisterReceiver(broad);
        super.onDestroy();
    }
}
