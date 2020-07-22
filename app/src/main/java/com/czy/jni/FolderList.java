package com.czy.jni;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class FolderList extends AppCompatActivity {

    private RecyclerView recycleview;
    private FileAdapter mAdapter;
    private int index;
    private String TAG = "Scanner";
    final static private int Folder = 0;
    final static private int AudioList = 1;
    final static private int VideoList = 2;
    private ContentResolver contentResolver;//getContentResolver
//    private enum MenuType {
//        FOLDER,ALLAUDIO,NEXT
//    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.folder);
        Context context = this;
        contentResolver = context.getContentResolver();
        Intent intent = getIntent();
        int menuType = intent.getIntExtra("menuType", Folder);
        setView(menuType);
    }
    public void folderInit() {
        Thread mthread = new Thread(new Runnable() {
            @Override
            public  void run(){

            }
        });mthread.start();

    }

    public List<ListData> querydata(int  parentId, int menuType) {
        Log.e(TAG, " querydata sun");
        String URL = "content://media.scan/folder_dir";
        Uri folderuri = Uri.parse(URL);
        String AURL = "content://media.scan/audio";
        Uri audio = Uri.parse(AURL);
        List<ListData> list = new ArrayList();
        String parent_id = String.valueOf(parentId);
        if (menuType == Folder) {
            Cursor c = getContentResolver().query(folderuri, new String[]{"_id" , "_name", "_path"}, "parent_id = ?",new String[] {parent_id},null);
            Log.e("query", "============folder================ ");
            if (c == null)
                Log.e(TAG, "c == null ");
            if (c.moveToFirst()) {
                do{
                    Log.e(TAG,  "id is :"+c.getInt(c.getColumnIndex( "_id"))+ ", name is :" + c.getString(c.getColumnIndex( MediaProvider.NAME)) + ", path is :" + c.getString(c.getColumnIndex( MediaProvider.PATH)));
                    ListData fol = new ListData(ListData.FOLDER);
                    fol.setId(c.getInt(c.getColumnIndex( "_id")));
                    fol.setName(c.getString(c.getColumnIndex( MediaProvider.NAME)));
                    fol.setPath(c.getString(c.getColumnIndex( MediaProvider.PATH)));
                    fol.fileTypte = ListData.FOLDER;
                    list.add(fol);
                    Log.e("query", " folder list count "+list.size());
                } while (c.moveToNext());
                c.close();
            }
        }
        Log.e("query", "============audio================ ");
        Cursor a;
        if (menuType == AudioList) {
            a = getContentResolver().query(audio, new String[]{"_id" , "_name", "_path"}, null, null,null);
        } else
            a = getContentResolver().query(audio, new String[]{"_id" , "_name", "_path"}, "parent_id = ?",new String[] {parent_id},null);
        if (a == null)
            Log.e(TAG, "a == null ");
        if (a.moveToFirst()) {
            do{
                Log.e(TAG,  "id is :"+a.getInt(a.getColumnIndex( MediaProvider.ID))+ ", name is :" + a.getString(a.getColumnIndex( MediaProvider.NAME)) + ", path is :" + a.getString(a.getColumnIndex( MediaProvider.PATH)));
                ListData fol = new ListData(ListData.FOLDER);
                fol.setId(a.getInt(a.getColumnIndex( "_id")));
                fol.setName(a.getString(a.getColumnIndex( MediaProvider.NAME)));
                fol.setPath(a.getString(a.getColumnIndex( MediaProvider.PATH)));
                fol.fileTypte = ListData.AUDIO;
                list.add(fol);
                Log.e("query", "  list count "+list.size());
            } while (a.moveToNext());
            a.close();
        }

        return list;
    }

    private void  setView(int menuType)
    {
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        recycleview = (RecyclerView)findViewById(R.id.recycleview);
        recycleview.setLayoutManager(manager);

        List<ListData> list = querydata(0, menuType);
        mAdapter = new FileAdapter(getApplicationContext(),list);
        recycleview.setAdapter(mAdapter);
        recycleview.setNestedScrollingEnabled(true);
        // 给RecycleView加入滑动监听
        recycleview.setOnClickListener(new RecyclerView.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.e(TAG, "setOnClickListener position " + v.getVerticalScrollbarPosition());
            }
        });
        recycleview.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (isScrollToEnd(recycleview)) {
                    Log.e("tag", "============scroll to end");
                    index += 1;
                }
            }
        });
    }

    //判断是否滑动到最后一个item
    private boolean isScrollToEnd(RecyclerView recycleview) {
        if (recycleview == null) return false;
        if (recycleview.computeVerticalScrollExtent() + recycleview.computeVerticalScrollOffset() >= recycleview.computeVerticalScrollRange())
            return true;
        return false;
    }
    public void folderback(View view) {
        Log.e(TAG, "folderback");
        mAdapter.backFolder();
    }


    public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {
        private List<ListData> mItems;
        private Context mContext;
        private MediaProvider mediaProvider = new MediaProvider();
        private String TAG = "Scanner";
        private List<Integer> folderidlist;
        public FileAdapter(Context context, List<ListData> items) {
            super();
            mItems = items;
            mContext = context;
            folderidlist = new ArrayList<>();
            folderidlist.add(0);
        }

        @Override
        public FileAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(mContext).inflate(R.layout.listview_item, parent, false);
            return new ViewHolder(v);
        }



        @Override
        public void onBindViewHolder(FileAdapter.ViewHolder holder, final int position) {
            final ListData data = mItems.get(position);
            holder.text.setText(data.getName());
            holder.text.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(data.fileTypte == ListData.AUDIO) {
                        Intent intent = new Intent();
                        intent.setClass(mContext, MainActivity.class);
                        intent.putExtra("path",data.getPath());
                        intent.putExtra("id",data.getId());
//                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_RECEIVER_FOREGROUND);

                       startActivity(intent);
                    } else if (data.fileTypte == ListData.FOLDER){
                        openfolder(data.getId());
                    }
                }
            });
        }

        public void  backFolder() {
            int id;
            if (folderidlist.isEmpty()) {
                Log.e(TAG, " backFolder folderidlist isEmpty error return");
                return;
            }

            folderidlist.remove(folderidlist.size()-1);
            if (folderidlist.isEmpty()){
                Log.e(TAG, " backFolder folderidlist back to root ");
                Intent intent = new Intent();
                    intent.setClass(mContext, Menu.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_RECEIVER_FOREGROUND);
                startActivity(intent);
            } else {

                id = folderidlist.get(folderidlist.size()-1);
                Log.e(TAG, " backFolder folderidlist id " + id);
                mItems = querydata(id);
                notifyDataSetChanged();
            }
        }

        private void openfolder(int id) {
            Log.e(TAG, " openfolder  id " + id);
            if (id > 0) {
                folderidlist.add(id);
                mItems = querydata(id);
                notifyDataSetChanged();
            } else {
                Log.e(TAG, " openfolder  error id: " + id);
            }

        }

        @Override
        public int getItemCount() {
            return mItems.size();

        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView text;
            public ViewHolder(View itemView) {
                super(itemView);
                text = (TextView) itemView.findViewById(R.id.desc);
            }
        }

        public List<ListData> querydata(int  parentId) {
            Log.e(TAG, " querydata sun");
            String URL = "content://media.scan/folder_dir";
            Uri folderuri = Uri.parse(URL);
            String AURL = "content://media.scan/audio";
            Uri audio = Uri.parse(AURL);
            List<ListData> list = new ArrayList();
            String parent_id = String.valueOf(parentId);
            Cursor c = mContext.getContentResolver().query(folderuri, new String[]{"_id" , "_name", "_path"}, "parent_id = ?",new String[] {parent_id},null);
            Log.e("query", "============folder================ ");
            if (c == null)
                Log.e(TAG, "c == null ");
            if (c.moveToFirst()) {
                do{
                    Log.e(TAG,  "id is :"+c.getInt(c.getColumnIndex( "_id"))+ ", name is :" + c.getString(c.getColumnIndex( MediaProvider.NAME)) + ", path is :" + c.getString(c.getColumnIndex( MediaProvider.PATH)));
                    ListData fol = new ListData(ListData.FOLDER);
                    fol.setId(c.getInt(c.getColumnIndex( "_id")));
                    fol.setName(c.getString(c.getColumnIndex( MediaProvider.NAME)));
                    fol.setPath(c.getString(c.getColumnIndex( MediaProvider.PATH)));
                    list.add(fol);
                } while (c.moveToNext());
                c.close();
            }
            Log.e("query", "============audio================ ");
            Cursor a = mContext.getContentResolver().query(audio, new String[]{"_id" , "_name", "_path"}, "parent_id = ?",new String[] {parent_id},null);
            if (a == null)
                Log.e(TAG, "a == null ");
            if (a.moveToFirst()) {
                do{
                    Log.e(TAG,  "id is :"+a.getInt(a.getColumnIndex( MediaProvider.ID))+ ", name is :" + a.getString(a.getColumnIndex( MediaProvider.NAME)) + ", path is :" + a.getString(a.getColumnIndex( MediaProvider.PATH)));
                    ListData fol = new ListData(ListData.AUDIO);
                    fol.setId(a.getInt(a.getColumnIndex( "_id")));
                    fol.setName(a.getString(a.getColumnIndex( MediaProvider.NAME)));
                    fol.setPath(a.getString(a.getColumnIndex( MediaProvider.PATH)));
                    list.add(fol);
                } while (a.moveToNext());
                a.close();
            }

            return list;
        }
    }
}
