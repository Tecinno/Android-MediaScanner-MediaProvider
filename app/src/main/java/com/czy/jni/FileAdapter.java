//
//package com.czy.jni;
//
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.List;
//
//import android.app.Activity;
//import android.content.Context;
//import android.content.Intent;
//import android.database.Cursor;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.net.Uri;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.BaseAdapter;
//import android.widget.ImageView;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {
//    private List<ListData> mItems;
//    private Context mContext;
//    private MediaProvider mediaProvider = new MediaProvider();
//    private String TAG = "Scanner";
////    private Context mcontext;
//    public FileAdapter(Context context, List<ListData> items) {
//        super();
//        mItems = items;
//        mContext = context;
//    }
//
//    @Override
//    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//        View v = LayoutInflater.from(mContext).inflate(R.layout.listview_item, parent, false);
//        return new ViewHolder(v);
//    }
//
//
//
//    @Override
//    public void onBindViewHolder(ViewHolder holder, final int position) {
//        final ListData data = mItems.get(position);
////        final String url =data.getName();
//        holder.text.setText(data.getName());
//        holder.text.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if(!data.isfolder) {
//                    Intent intent = new Intent();
//                    intent.setClass(mContext, MainActivity.class);
//                    intent.putExtra("path",data.getPath());
//                    intent.putExtra("id",data.getId());
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
//                    mContext.startActivity(intent);
//                } else {
//                    openfolder(data.getId());
//                }
//
//            }
//        });
//    }
//    private void openfolder(int id) {
//        List<ListData> list =  querydata(id);
//    }
//    private void  setView()
//    {
////        LinearLayoutManager manager = new LinearLayoutManager(mContext);
////        manager.setOrientation(LinearLayoutManager.VERTICAL);
////        RecyclerView recycleview = mContext.findViewById(R.id.recycleview);
////        TextView t = (TextView) (RecyclerView)context.findViewById(R.id.user);
//////        recycleview.setLayoutManager(new StaggeredGridLayoutManager(20 , StaggeredGridLayoutManager.HORIZONTAL));//LinearLayoutManager
////        recycleview.setLayoutManager(manager);
////
////        List<ListData> list = querydata();
////        FileAdapter mAdapter = new FileAdapter(mContext,list);
////        recycleview.setAdapter(mAdapter);
////        recycleview.setNestedScrollingEnabled(true);
////        // 给RecycleView加入滑动监听
////        recycleview.addOnScrollListener(new RecyclerView.OnScrollListener() {
////            @Override
////            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
////                super.onScrolled(recyclerView, dx, dy);
////            }
////
////
////        });
//    }
//    @Override
//    public int getItemCount() {
//        return mItems.size();
//
//    }
//
//    public class ViewHolder extends RecyclerView.ViewHolder {
////        ImageView image;
//        TextView text;
//        public ViewHolder(View itemView) {
//            super(itemView);
//            text = (TextView) itemView.findViewById(R.id.desc);
//        }
//    }
//
//    private List<ListData> querydata(int  parentId) {
//        Log.e(TAG, " querydata");
//        String URL = "content://media.scan/folder_dir";
//        Uri folderuri = Uri.parse(URL);
//        String AURL = "content://media.scan/audio";
//        Uri audio = Uri.parse(AURL);
//        List<ListData> list = new ArrayList();
//        String parent_id = String.valueOf(parentId);
//        Cursor c = mContext.getContentResolver().query(folderuri, new String[]{"_id" , "_name", "_path"}, "parent_id = ?",new String[] {parent_id},null);
//        Log.e("query", "============folder================ ");
//        if (c == null)
//            Log.e(TAG, "c == null ");
//        if (c.moveToFirst()) {
//            do{
//                Log.e(TAG,  "id is :"+c.getInt(c.getColumnIndex( "_id"))+ ", name is :" + c.getString(c.getColumnIndex( MediaProvider.NAME)) + ", path is :" + c.getString(c.getColumnIndex( MediaProvider.PATH)));
//                ListData fol = new ListData(true);
//                fol.setId(c.getInt(c.getColumnIndex( "_id")));
//                fol.setName(c.getString(c.getColumnIndex( MediaProvider.NAME)));
//                fol.setPath(c.getString(c.getColumnIndex( MediaProvider.PATH)));
//            } while (c.moveToNext());
//        }
//        Log.e("query", "============audio================ ");
//        Cursor a = mContext.getContentResolver().query(audio, new String[]{"_id" , "_name", "_path"}, "parent_id = ?",new String[] {parent_id},null);
//        if (a == null)
//            Log.e(TAG, "a == null ");
//        if (a.moveToFirst()) {
//            do{
//                Log.e(TAG,  "id is :"+a.getInt(a.getColumnIndex( MediaProvider.ID))+ ", name is :" + a.getString(a.getColumnIndex( MediaProvider.NAME)) + ", path is :" + a.getString(a.getColumnIndex( MediaProvider.PATH)));
//                ListData fol = new ListData(false);
//                fol.setId(a.getInt(a.getColumnIndex( "_id")));
//                fol.setName(a.getString(a.getColumnIndex( MediaProvider.NAME)));
//                fol.setPath(a.getString(a.getColumnIndex( MediaProvider.PATH)));
//                list.add(fol);
//            } while (a.moveToNext());
//        }
//
//        return list;
//    }
//}
//
//
//
