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
    private MyBroadcastReceiver broad;
    private IntentFilter intentFilter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu);
        //注册广播
        intentRegister();
//        show();
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
    }

    public void back_button(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void audiolist_button(View view) {
        Intent intent = new Intent(this, FolderList.class);
        intent.putExtra("menuType", AudioList);
        startActivity(intent);
    }
    public void videolist_button(View view) {
        Intent intent = new Intent(this, FolderList.class);
        intent.putExtra("menuType", VideoList);
        startActivity(intent);
    }
    public void genre_button(View view) {
        Log.e(TAG, "Menu genre_button ");
        sendBroad();
//        show();
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
//    @Override
//    public void finish() {
//        Log.e(TAG, "Menu finish ");
////        super.finish();
////        overridePendingTransition(R.style.mystyle, R.style.mystyle);
//    }
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
