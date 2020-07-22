package com.czy.jni;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;


public class Menu extends AppCompatActivity {
    private RecyclerView recycleview;
    private FolderList.FileAdapter mAdapter;
    private String TAG = "Scanner";
    final static private int Folder = 0;
    final static private int AudioList = 1;
    final static private int VideoList = 2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu);
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

}
