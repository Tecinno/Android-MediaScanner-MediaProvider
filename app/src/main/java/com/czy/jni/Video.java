package com.czy.jni;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;
import androidx.core.view.MotionEventCompat;

import android.app.ActivityOptions;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaRouter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

public class Video extends AppCompatActivity {
    private VideoView videoViewSys;
    private int videoId;
    final String TAG = "Scanner";
    private static Context mcontext;
    private GestureDetectorCompat mDetector;
    private LinearLayout linearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_menu);
        Log.e("VIDEO", "Video oncreate ");
        initView();
        initDetector();
    }

    protected void onResume() {
        super.onResume();
        onNewIntent(this.getIntent());
        mcontext = this;
        Intent intent = getIntent();
        String path = intent.getStringExtra("path");
        videoId = intent.getIntExtra("_id",-1);
        MainActivity.videoData.path = path;
        MainActivity.videoData.id = videoId;
        video_init();
    }

    //播放视频
    private void video_init() {
        Log.e("VIDEO", "Video ONRESUME PATH:"+MainActivity.videoData.path);
        new Thread(){
            @Override
            public void run() {
                super.run();
                videoViewSys = (VideoView) findViewById(R.id.videoView);
                videoViewSys.setVideoPath(MainActivity.videoData.path);
                MediaController mMediaController = new MediaController(mcontext);
                mMediaController.setPrevNextListeners(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //下一首,实现具体的切换逻辑
                    }
                }, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //上一首
                    }
                });
                videoViewSys.setMediaController(mMediaController);
                videoViewSys.start();
            }
        }.start();
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
    public void subDisplay() {
        //===========================================
        ActivityOptions options = ActivityOptions.makeBasic();
        MediaRouter mediaRouter = (MediaRouter) getSystemService(Context.MEDIA_ROUTER_SERVICE);
        MediaRouter.RouteInfo routeInfo = mediaRouter.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_VIDEO);
        if(routeInfo != null) {
            Display presentationDisplay = routeInfo.getPresentationDisplay();
            options.setLaunchDisplayId(presentationDisplay.getDisplayId());
            // 启动客显屏Activity
            Intent intent = new Intent(this, Video.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("path",MainActivity.videoData.path);
            intent.putExtra("_id",MainActivity.videoData.id);
            startActivity(intent, options.toBundle());
        }
    }

    private void initView() {
        linearLayout = (LinearLayout) findViewById(R.id.video_layout);
    }

    // 初始化手势监听器Detector
    private void initDetector() {
        mDetector = new GestureDetectorCompat(this,
                new MytGestureListener());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO Auto-generated method stub
        return mDetector.onTouchEvent(event);
    }
    //手势监听
    private class MytGestureListener extends GestureDetector.SimpleOnGestureListener {

        // Touch down时触发
        @Override
        public boolean onDown(MotionEvent e) {
            Log.e(TAG, "onDown");
            return super.onDown(e);
        }

        // 在Touch down之后一定时间（115ms）触发
        @Override
        public void onShowPress(MotionEvent e) {
            Log.e(TAG, "onShowPress");
        }

        // 用户（轻触触摸屏后）松开，由一个1个MotionEvent ACTION_UP触发
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Log.e(TAG, "onSingleTapUp");
            return super.onSingleTapUp(e);
        }

        // 滑动时触发
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
            Log.e(TAG, "onScroll");
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        // 抛掷
        // 滑动一段距离，up时触发
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            Log.i(TAG, "onFling : e1.x : "+e1.getX()+", e1.y : "+e1.getY()+", e2.x"+e2.getX()+" ,e2.y"+e2.getY());
            if (e1.getX() - e2.getX() > 300) {//向左滑动，视频在副屏中播放
                Log.i(TAG, "display in second screen");
                subDisplay();
            } else if (e1.getY() - e2.getY() > 200) {//向上滑动，视频退出
                Log.i(TAG, "exit");
                finish();
            }
            return super.onFling(e1, e2, velocityX, velocityY);
        }

        // 长按后触发(Touch down之后一定时间（500ms）)
        @Override
        public void onLongPress(MotionEvent e) {
            Log.i(TAG, "onLongPress");
        }

        // 双击
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.d(TAG, "onDoubleTap");
            return super.onDoubleTap(e);
        }

    }
}
