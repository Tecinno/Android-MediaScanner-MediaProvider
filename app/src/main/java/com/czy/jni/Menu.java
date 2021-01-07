package com.czy.jni;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;


public class Menu extends AppCompatActivity {
    private RecyclerView recycleview;
    private FolderList.FileAdapter mAdapter;
    private static  String TAG = "Scanner";
    final static private int Folder = 0;
    final static private int AudioList = 1;
    final static private int VideoList = 2;
    final static private int VideoFavoriteList = 3;
    final static private int audioFavoriteList = 4;
    private MyBroadcastReceiver broad;
    private IntentFilter intentFilter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu);
        intentRegister();
    }

    //广播测试
    public void sendBroad() {
        Intent intent = new Intent();
        intent.setAction("android.net.conn.media");
        sendBroadcast(intent);
        Log.e(TAG,"sendBroad OK");
    }
    public void folder_button(View view) {
        Intent intent = new Intent(this, FolderList.class);
        intent.putExtra("menuType", Folder);
        startActivity(intent);
        overridePendingTransition(R.anim.fate_enter,R.anim.fate_out);
    }

    public void back_button(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fate_enter,R.anim.fate_out);
    }

    public void audiolist_button(View view) {
        Intent intent = new Intent(this, FolderList.class);
        intent.putExtra("menuType", AudioList);
        startActivity(intent);
        overridePendingTransition(R.anim.fate_enter,R.anim.fate_out);
    }
    public void videolist_button(View view) {
        Intent intent = new Intent(this, FolderList.class);
        intent.putExtra("menuType", VideoList);
        startActivity(intent);
        overridePendingTransition(R.anim.fate_enter,R.anim.fate_out);
    }
//    public void genre_button(View view) {
//        Log.e(TAG, "Menu genre_button ");
//        sendBroad();
////        show();
//    }
    public void audioFavorite_button(View view) {
        Intent intent = new Intent(this, FolderList.class);
        intent.putExtra("menuType", audioFavoriteList);
        startActivity(intent);
        overridePendingTransition(R.anim.fate_enter,R.anim.fate_out);
    }
    public void videoFavorite_button(View view) {
        Intent intent = new Intent(this, FolderList.class);
        intent.putExtra("menuType", VideoFavoriteList);
        startActivity(intent);
        overridePendingTransition(R.anim.fate_enter,R.anim.fate_out);
    }
    private void intentRegister() {
        intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.media");
//        intentFilter.addAction("android.intent.action.MEDIA_MOUNTED");
//        intentFilter.addDataScheme("file");
        broad = new MyBroadcastReceiver();
        registerReceiver(broad, intentFilter);
    }
    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context content, Intent intent){
            final String action = intent.getAction();
            Log.e(TAG, "Menu MyBroadcastReceiver + "+ action);
//            show();
//            finish();
        }
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "Menu onDestroy ");
        unregisterReceiver(broad);

//        show();
        super.onDestroy();

    }
    private void show() {
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
}
