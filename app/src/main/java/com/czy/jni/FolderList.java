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
import android.os.Trace;
public class FolderList extends AppCompatActivity {

    private RecyclerView recycleview;
    private FileAdapter mAdapter;
    private int index;
    private String TAG = "Scanner";
    final static private int Folder = 0;
    final static private int AudioList = 1;
    final static private int VideoList = 2;
    final static private int VideoFavoriteList = 3;
    final static private int audioFavoriteList = 4;
    private TextView listCountText ;
    private ContentResolver contentResolver;//getContentResolver
    private int menuType;
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
        menuType = intent.getIntExtra("menuType", Folder);
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
        Trace.beginSection("querydata");
        Log.i(TAG, " querydata first");
        String URL = "content://media.scan/folder_dir";
        Uri folderuri = Uri.parse(URL);
        String AURL = "content://media.scan/audio";
        Uri audio = Uri.parse(AURL);
        String VURL = "content://media.scan/video";
        Uri video = Uri.parse(VURL);
        List<ListData> list = new ArrayList();
        String parent_id = String.valueOf(parentId);
        if (menuType == Folder) {
            Cursor c = getContentResolver().query(folderuri, new String[]{"_id" , "_name", "_path"}, "parent_id = ?",new String[] {parent_id},null);
            Log.i(TAG, "============folder================ ");
            if (c == null)
            {
                Log.e(TAG, "c == null ");
                return null;
            }
            if (c.moveToFirst()) {
                do{
                    Log.i(TAG,  "id is :"+c.getInt(c.getColumnIndex( "_id"))+ ", name is :" + c.getString(c.getColumnIndex( MediaProvider.NAME)) + ", path is :" + c.getString(c.getColumnIndex( MediaProvider.PATH)));
                    ListData fol = new ListData(ListData.FOLDER);
                    fol.setId(c.getInt(c.getColumnIndex( "_id")));
                    fol.setName(c.getString(c.getColumnIndex( MediaProvider.NAME)));
                    fol.setPath(c.getString(c.getColumnIndex( MediaProvider.PATH)));
                    fol.fileTypte = ListData.FOLDER;
                    list.add(fol);
//                    Log.i("query", " folder list count "+list.size());
                } while (c.moveToNext());
                c.close();
            }
        }
        Log.i(TAG, "============audio & video================ ");
        Cursor a = null;
        Cursor v = null;
        int type;
        if (menuType == AudioList) //判断类型，防止数据中parent_id为null
        {
            a = getContentResolver().query(audio, new String[]{"_id" , "_name", "_path"}, null,null,"_name COLLATE LOCALIZED ASC");
            type = ListData.AUDIO;
        }
        else if (menuType == audioFavoriteList)
        {
            a = getContentResolver().query(audio, new String[]{"_id" , "_name", "_path"}, "is_favorite = ?",new String[] {"1"},"_name COLLATE LOCALIZED ASC");
            type = ListData.AUDIO;
        }
        else if (menuType == VideoList)
        {
            a = getContentResolver().query(video, new String[]{"_id" , "_name", "_path"}, null,null,"_name COLLATE LOCALIZED ASC");
            type = ListData.VIDEO;
        }
        else if (menuType == VideoFavoriteList)
        {
            a = getContentResolver().query(video, new String[]{"_id" , "_name", "_path"}, "is_favorite = ?",new String[] {"1"},"_name COLLATE LOCALIZED ASC");
            type = ListData.VIDEO;
        }
        else
        {
            a = getContentResolver().query(audio, new String[]{"_id" , "_name", "_path"}, "parent_id = ?",new String[] {parent_id},"_name COLLATE LOCALIZED ASC");
            v = getContentResolver().query(video, new String[]{"_id" , "_name", "_path"}, "parent_id = ?",new String[] {parent_id},"_name COLLATE LOCALIZED ASC");
            type = ListData.MediaFix;
        }
        if (a == null)
        {
            Log.e(TAG, "a == null ");
            return null;
        }
        if (a.moveToFirst()) {
            type = type == ListData.MediaFix ? ListData.AUDIO : type;
            do{
                Log.i(TAG,  "audio id is :"+a.getInt(a.getColumnIndex( MediaProvider.ID))+ ", name is :" + a.getString(a.getColumnIndex( MediaProvider.NAME)) + ", path is :" + a.getString(a.getColumnIndex( MediaProvider.PATH)));
                ListData fol = new ListData(type);
                fol.setId(a.getInt(a.getColumnIndex( "_id")));
                fol.setName(a.getString(a.getColumnIndex( MediaProvider.NAME)));
                fol.setPath(a.getString(a.getColumnIndex( MediaProvider.PATH)));
                if(fol.getName() == null && fol.getPath() != null) {
                    fol.setName(fol.getPath().substring(fol.getPath().lastIndexOf("/")+1));
                }
                list.add(fol);
//                Log.i(TAG, "  list count "+list.size());
            } while (a.moveToNext());
            a.close();
        }

        if (type == ListData.MediaFix && v != null && v.moveToFirst()) {
            type = ListData.VIDEO;
            do{
                Log.i(TAG,  "video id is :"+v.getInt(v.getColumnIndex( MediaProvider.ID))+ ", name is :" + v.getString(v.getColumnIndex( MediaProvider.NAME)) + ", path is :" + v.getString(v.getColumnIndex( MediaProvider.PATH)));
                ListData fol = new ListData(type);
                fol.setId(v.getInt(v.getColumnIndex( "_id")));
                fol.setName(v.getString(v.getColumnIndex( MediaProvider.NAME)));
                fol.setPath(v.getString(v.getColumnIndex( MediaProvider.PATH)));
                if(fol.getName() == null && fol.getPath() != null) {
                    fol.setName(fol.getPath().substring(fol.getPath().lastIndexOf("/")+1));
                }
                list.add(fol);
            } while (v.moveToNext());
            v.close();
        }
        listCountText.setText("list count : " + list.size());
        Trace.endSection();
        return list;
    }

    private void  setView(int menuType)
    {
        listCountText = this.findViewById(R.id.listcount);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        recycleview = (RecyclerView)findViewById(R.id.recycleview);
        recycleview.setLayoutManager(manager);

        List<ListData> list = querydata(0, menuType);
        mAdapter = new FileAdapter(getApplicationContext(),list, menuType);
        recycleview.setAdapter(mAdapter);
        recycleview.setNestedScrollingEnabled(true);
        // 给RecycleView加入滑动监听
        recycleview.setOnClickListener(new RecyclerView.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.i(TAG, "setOnClickListener position " + v.getVerticalScrollbarPosition());
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
                    Log.i("tag", "============scroll to end");
//                    mAdapter.refreshList();
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
        Log.i(TAG, "folderback");
        mAdapter.backFolder();
    }


    public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {
        private List<ListData> mItems;
        private Context mContext;
        private MediaProvider mediaProvider = new MediaProvider();
        private String TAG = "Scanner";
        private List<Integer> folderidlist;
        private int menuType;
//        private TextView listCountText;
        public FileAdapter(Context context, List<ListData> items, int menuType) {
            super();
            mItems = items;
            mContext = context;
            folderidlist = new ArrayList<>();
            folderidlist.add(0);
            listCountText = (TextView)findViewById(R.id.listcount);
            this.menuType = menuType;//判断列表是文件夹还是歌曲列表
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
                        startActivity(intent);
                        overridePendingTransition(R.anim.fate_enter,R.anim.fate_out);
                    } else if (data.fileTypte == ListData.VIDEO) {
                        Intent intent = new Intent();
                        intent.setClass(mContext, Video.class);
                        intent.putExtra("path",data.getPath());
                        intent.putExtra("_id",data.getId());
                        startActivity(intent);

                        overridePendingTransition(R.anim.fate_enter,R.anim.fate_out);
                    } else if (data.fileTypte == ListData.FOLDER){
                        openfolder(data.getId());
                    }
                }
            });
        }
        //返回到上级文件夹
        public void  backFolder() {
            int id;
            if (folderidlist.isEmpty()) {
                Log.e(TAG, " backFolder folderidlist isEmpty error return");
                return;
            }

            folderidlist.remove(folderidlist.size()-1);
            if (folderidlist.isEmpty()){
                Log.e(TAG, " backFolder folderidlist back to menu ");
                Intent intent = new Intent();
                    intent.setClass(mContext, Menu.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_RECEIVER_FOREGROUND);
                startActivity(intent);
                overridePendingTransition(R.anim.fate_enter,R.anim.fate_out);
            } else {

                id = folderidlist.get(folderidlist.size()-1);
                Log.i(TAG, " backFolder folderidlist id " + id);
                mItems = querydata(id, Folder);
                notifyDataSetChanged();
            }
        }
        //打开一个文件夹
        private void openfolder(int id) {
            Log.e(TAG, " openfolder  id " + id);
            if (id > 0) {
                folderidlist.add(id);
                mItems = querydata(id, menuType);
                notifyDataSetChanged();
            } else {
                Log.e(TAG, " openfolder  error id: " + id);
            }

        }
        public void refreshList() {
            Log.i(TAG, " refreshList");
            mItems = querydata(folderidlist.get(folderidlist.size()-1), menuType);
            notifyDataSetChanged();
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

        public List<ListData> querydata(int  parentId, int menuType) {
            Trace.beginSection("querydata sun");
            Log.i(TAG, " querydata sun");
            String URL = "content://media.scan/folder_dir";
            Uri folderuri = Uri.parse(URL);
            String AURL = "content://media.scan/audio";
            Uri audio = Uri.parse(AURL);
            String VURL = "content://media.scan/video";
            Uri video = Uri.parse(VURL);
            List<ListData> list = new ArrayList();
            String parent_id = String.valueOf(parentId);
            Cursor c;
            if (menuType == Folder) {
                c = mContext.getContentResolver().query(folderuri, new String[]{"_id" , "_name", "_path"}, "parent_id = ?",new String[] {parent_id},null);
                Log.i(TAG, "============folder================ ");
                if (c == null)
                {
                    Log.e(TAG, "c == null ");
                    return null;
                }
                if (c.moveToFirst()) {
                    do{
                        Log.i(TAG,  "id is :"+c.getInt(c.getColumnIndex( "_id"))+ ", name is :" + c.getString(c.getColumnIndex( MediaProvider.NAME)) + ", path is :" + c.getString(c.getColumnIndex( MediaProvider.PATH)));
                        ListData fol = new ListData(ListData.FOLDER);
                        fol.setId(c.getInt(c.getColumnIndex( "_id")));
                        fol.setName(c.getString(c.getColumnIndex( MediaProvider.NAME)));
                        fol.setPath(c.getString(c.getColumnIndex( MediaProvider.PATH)));
                        list.add(fol);
                    } while (c.moveToNext());
                    c.close();
                }
            }

            Log.i(TAG, "============audio================ ");
            Cursor a;
            int type;
            if (menuType == AudioList) //判断类型，防止数据中parent_id为null
            {
                a = mContext.getContentResolver().query(audio, new String[]{"_id" , "_name", "_path"}, null,null,"_name COLLATE LOCALIZED ASC");
//                a = null;
                type = ListData.AUDIO;
            } else if (menuType == VideoList) {
                a = null;
                type = ListData.AUDIO;
            }
            else
            {
                a = mContext.getContentResolver().query(audio, new String[]{"_id" , "_name", "_path"}, "parent_id = ?",new String[] {parent_id},"_name COLLATE LOCALIZED ASC");
                type = ListData.AUDIO;
            }

            if (a == null)
            {
                Log.e(TAG, "a == null ");
                return null;
            }
            if (a.moveToFirst()) {
                do{
//                    Log.i(TAG,  "id is :"+a.getInt(a.getColumnIndex( MediaProvider.ID))+ ", name is :" + a.getString(a.getColumnIndex( MediaProvider.NAME)) + ", path is :" + a.getString(a.getColumnIndex( MediaProvider.PATH)));
                    ListData fol = new ListData(type);
                    fol.setId(a.getInt(a.getColumnIndex( "_id")));
                    fol.setName(a.getString(a.getColumnIndex( MediaProvider.NAME)));
                    fol.setPath(a.getString(a.getColumnIndex( MediaProvider.PATH)));
                    if(fol.getName() == null && fol.getPath() != null) {
                        fol.setName(fol.getPath().substring(fol.getPath().lastIndexOf("/")+1));
                    }
                    list.add(fol);
                } while (a.moveToNext());
                a.close();
            }

            Log.i(TAG, "============video================ ");
            Cursor v;
            if (menuType == VideoList)
            {
                v = mContext.getContentResolver().query(video, new String[]{"_id" , "_name", "_path"}, null,null,"_name COLLATE LOCALIZED ASC");
                type = ListData.VIDEO;
//                v = null;
            }
            else if (menuType == AudioList) {
                v = null;
                type = ListData.AUDIO;
            }
            else
            {
                v = mContext.getContentResolver().query(video, new String[]{"_id" , "_name", "_path"}, "parent_id = ?",new String[] {parent_id},"_name COLLATE LOCALIZED ASC");
                type = ListData.VIDEO;
            }
            if (v == null)
            {
                Log.e(TAG, "v == null ");
                return null;
            }
            if (v.moveToFirst()) {
                do{
//                    Log.i(TAG,  "id is :"+a.getInt(a.getColumnIndex( MediaProvider.ID))+ ", name is :" + a.getString(a.getColumnIndex( MediaProvider.NAME)) + ", path is :" + a.getString(a.getColumnIndex( MediaProvider.PATH)));
                    ListData fol = new ListData(type);
                    fol.setId(v.getInt(v.getColumnIndex( "_id")));
                    fol.setName(v.getString(v.getColumnIndex( MediaProvider.NAME)));
                    fol.setPath(v.getString(v.getColumnIndex( MediaProvider.PATH)));
                    if(fol.getName() == null && fol.getPath() != null) {
                        fol.setName(fol.getPath().substring(fol.getPath().lastIndexOf("/")+1));
                    }
                    list.add(fol);
                } while (v.moveToNext());
                v.close();
            }

            listCountText.setText("list count : " + list.size());
            Trace.endSection();
            return list;
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fate_enter,R.anim.fate_out);
    }
}
