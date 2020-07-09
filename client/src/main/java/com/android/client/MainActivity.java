package com.android.client;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    final static String TAG = "client";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void scan(View view) {
        try {
            Thread mthread = new Thread(new Runnable() {
                @Override
                public  void run(){
                    opendatabase(true);
//                    myProviderQuery();
                }
            });
            mthread.setPriority(Thread.MAX_PRIORITY);
            mthread.start();
        }catch (SQLException e) {
            Log.e(TAG, " opendatabase SQLException :"+e);
        }catch (Exception e) {
            Log.e(TAG, " opendatabase Exception :"+e);
        }

    }
    private void opendatabase(boolean isAudio) {
        Log.e(TAG, " opendatabase");
        ContentValues values = new ContentValues();
        String URL;
        if (isAudio == true)
            URL = "content://media.scan/audio";
        else
            URL = "content://media.scan/video";

        Uri music = Uri.parse(URL);


        values.put("_name", "open database");
        Uri uri = getContentResolver().insert(
                music, values);

        Toast.makeText(getBaseContext(),
                uri.toString(), Toast.LENGTH_LONG).show();
    }

    private void myProviderQuery() {
        Log.e("MainThread", " myProviderQuery");
        String URL = "content://media.scan/audio";
        String URLV = "content://media.scan/video";
        Uri music = Uri.parse(URL);
        Uri video = Uri.parse(URLV);
        Cursor c = getContentResolver().query(music, null, null,null,"_name COLLATE LOCALIZED ASC");
        Cursor v = getContentResolver().query(video, null, null,null,"_name COLLATE LOCALIZED ASC");
        Log.e("query", "============audio================ ");
        if (c == null)
            Log.e("query", "c == null ");
        long  startTime = System.currentTimeMillis();
        if (c.moveToFirst()) {
            do{
                Log.e("query",  "id is :"+c.getInt(c.getColumnIndex( "_id"))+ ", name is :" + c.getString(c.getColumnIndex( "_name")) + ", path is :" + c.getString(c.getColumnIndex( "_path")));
            } while (c.moveToNext());
        }
        Log.e("query", "============video================ ");
        if (v == null)
            Log.e("query", "v == null ");
        if (v.moveToFirst()) {
            do{
                Log.e("query",  "id is :"+v.getInt(v.getColumnIndex("_id"))+ ", name is :" + v.getString(v.getColumnIndex("_name")) + ", path is :" + v.getString(v.getColumnIndex("_path")));
            } while (v.moveToNext());
        }
        c.close();
        v.close();

    }
}
