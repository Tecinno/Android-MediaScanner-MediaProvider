package com.czy.jni;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    final static String TAG = "ScannerProvider";
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        TextView tv = findViewById(R.id.sample_text);
        tv.setText("hellow");
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

    private void myProviderQuery() {
        Log.e(TAG, " myProviderQuery");
        String URL = "content://media.scan/audio";
        String URLV = "content://media.scan/video";
        Uri music = Uri.parse(URL);
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
        String[] title = {"上海","伤害","上海","伤害"};
        int[] result ={0};
        result = stringFromJNI(title);
        if (result == null) {
            Log.e(TAG,"end jni result == null");
        } else
            Log.e(TAG,"end jni result != null");
    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native int[] stringFromJNI(String[] title);
}
