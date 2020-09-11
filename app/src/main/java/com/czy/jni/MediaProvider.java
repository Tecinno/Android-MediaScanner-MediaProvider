package com.czy.jni;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.storage.StorageManager;
import android.system.Os;
import android.util.Log;
import android.os.storage.StorageVolume;
import java.io.File;
import java.util.Locale;
import android.os.Trace;
public class MediaProvider extends ContentProvider {
    final static String TAG = "Scanner";
    static {
        System.loadLibrary("native-lib");
    }
//    static final String scanPath = "/udisk";//sdcard/android_ubuntu  /udisk
    static final String scanPath = "/sdcard";//sdcard/android_ubuntu  /udisk
    static final String ACTION_MEDIA_MOUNTED = "android.intent.action.MEDIA_MOUNTED";
    static final String PROVIDER_NAME = "media.scan";
    static final String AUDIO_STRING_URL = "content://" + PROVIDER_NAME + "/audio";
    static final String VIDEO_STRING_URL = "content://" + PROVIDER_NAME + "/video";
    static final Uri AUDIO_URI = Uri.parse(AUDIO_STRING_URL);
    static final Uri VIDEO_URI = Uri.parse(VIDEO_STRING_URL);

    static final String ID = "_id";
    static final String NAME = "_name";
    static final String PATH = "_path";

    static final int AUDIO = 1;
    static final int VIDEO = 2;
    static final int FOLDER = 3;
    static final String DATABASE_NAME = "external_udisk.db";
    String oldVolume = "";
    static final UriMatcher uriMatcher;
    static{
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "audio", AUDIO);
        uriMatcher.addURI(PROVIDER_NAME, "video", VIDEO);
        uriMatcher.addURI(PROVIDER_NAME, "folder_dir", FOLDER);
//        uriMatcher.addURI(PROVIDER_NAME, "music/*", MUSIC_NAME);
    }

    private SQLiteDatabase db;
    static final String AUDIO_TABLE_NAME = "audio";
    static final String VIDEO_TABLE_NAME = "video";
    static final String FOLDER_TABLE_NAME = "folder_dir";
    /**
     * 创建和管理提供者内部数据源的帮助类.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {
        Context mcontext;
        // StorageManager mStorageManager = context.getSystemService(StorageManager.class);
        // final VolumeInfo volumeInfo = mStorageManager.getVolumeInfo(path);
        // final int volumeID = (volumeInfo == null) ? -1 : volumeInfo.getVolumeID();
        // String dbName = "external-" + Integer.toHexString(volumeID) + ".db";



        static final int DATABASE_VERSION = 1;

        //===============creat audio table==============
        static final String CREATE_DB_AUDIO_TABLE = "CREATE TABLE IF NOT EXISTS audio(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "parent_id INTEGER," +
                "size INTEGER," +
                "mtime INTEGER," +
                "_name TEXT," +
                "_path TEXT NOT NULL," +
                "album TEXT," +
                "genre TEXT," +
                "artist TEXT," +
                "genre_id INTEGER," +
                "album_id INTEGER," +
                "artist_id INTEGER" +
                ");";


        //===============creat audiolist table==============
        static final String CREATE_DB_AUDIO_LIST_TABLE = "CREATE TABLE IF NOT EXISTS audiolist(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "_name TEXT NOT NULL," +
                "_path TEXT NOT NULL" +
                ");";
        //===============creat video table==============
        static final String CREATE_DB_VIDEO_TABLE = "CREATE TABLE IF NOT EXISTS video(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "parent_id INTEGER," +
                "size INTEGER," +
                "mtime INTEGER," +
                "_name TEXT," +
                "_path TEXT NOT NULL" +
                ");";


        //===============creat videolist table==============
        static final String CREATE_DB_VIDEO_LIST_TABLE = "CREATE TABLE IF NOT EXISTS videolist(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "_name TEXT NOT NULL," +
                "_path TEXT NOT NULL" +
                ");";
        //===============creat folder_dir table==============
        static final String CREATE_DB_FLODER_DIR_TABLE = "CREATE TABLE IF NOT EXISTS folder_dir(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "parent_id INTEGER," +
                "_name TEXT," +
                "_path TEXT NOT NULL," +
                "dir_layer TEXT," +
                "has_audio INTEGER," +
                "has_video INTEGER" +
                ");";



        //===============creat album table==============
        static final String CREATE_DB_ALBUM_TABLE = "CREATE TABLE IF NOT EXISTS album(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "parent_id INTEGER," +
                "_name TEXT NOT NULL," +
                "_path TEXT NOT NULL," +
                "dir_layer TEXT," +
                "has_audio INTEGER," +
                "has_video INTEGER" +
                ");";


        DatabaseHelper(Context context){
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            mcontext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {
            db.execSQL(CREATE_DB_AUDIO_TABLE);
            db.execSQL(CREATE_DB_VIDEO_TABLE);
            db.execSQL(CREATE_DB_AUDIO_LIST_TABLE);
            db.execSQL(CREATE_DB_VIDEO_LIST_TABLE);
            db.execSQL(CREATE_DB_FLODER_DIR_TABLE);
            db.execSQL(CREATE_DB_ALBUM_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//            db.execSQL("DROP TABLE IF EXISTS " + AUDIO_TABLE_NAME);
//            db.execSQL("DROP TABLE IF EXISTS " + VIDEO_TABLE_NAME);
            onCreate(db);
        }
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        DatabaseHelper dbHelper = new DatabaseHelper(context);

        db = dbHelper.getWritableDatabase();//创建数据库
//        db.enableWriteAheadLogging();
//        db.setLocale(Locale.CHINESE);
//        db.enableWriteAheadLogging();
        return (db == null)? false:true;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Trace.beginSection("insert");
        String mediaTable;
        switch (uriMatcher.match(uri)) {
            case AUDIO:mediaTable = AUDIO_TABLE_NAME;break;
            case VIDEO:mediaTable = VIDEO_TABLE_NAME;break;
            case FOLDER:mediaTable = FOLDER_TABLE_NAME;break;
            default:throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (mediaTable == null) {
            Log.e(TAG,"mediaUri == null");
            return null;
        }

        if (values.get(NAME).equals("open database")) {
            Log.e(TAG,"open database start:");
            StorageManager storageManager = (StorageManager) getContext().getSystemService(Context.STORAGE_SERVICE);
            File path = new File(scanPath);
            int isNewVolume = 0;
            if (path != null) {
                final StorageVolume volumeInfo = storageManager.getStorageVolume(path);
                String volumeId = volumeInfo.getUuid() == null ? "001" : volumeInfo.getUuid();
                //判断是不是上次插入的设备
                if(oldVolume.equals(volumeId)) {
                    Log.e(TAG,"old volume : "+ volumeId);
                } else{
                    oldVolume = volumeId;
                    getContext().deleteDatabase(DATABASE_NAME);
                    DatabaseHelper dbHelper = new DatabaseHelper(getContext());
                    db = dbHelper.getWritableDatabase();//创建数据库
                    if(db != null) {
                        isNewVolume = 1;
                        Log.e(TAG,"delete old db and creat new db sucess ");
                    }
                    Log.e(TAG,"new volume : "+ volumeId);
                }
                //开始扫描
                mediascanner(isNewVolume);
            } else
                Log.e(TAG,"path is null : "+ path);

            Log.e(TAG,"open database finish:");
            Trace.endSection();
            return uri;
        }

        long rowID = db.insert(mediaTable, "", values);
        Log.e(TAG,"insert success rowID :" + rowID);

        if (rowID > 0)
        {
            Uri _uri = ContentUris.withAppendedId(uri, rowID);
            getContext().getContentResolver().notifyChange(_uri, null);
            return _uri;
        }
        throw new SQLException("Failed to add a record into " + uri);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        String mediaTable;
        switch (uriMatcher.match(uri)) {
            case AUDIO:mediaTable = AUDIO_TABLE_NAME;break;
            case VIDEO:mediaTable = VIDEO_TABLE_NAME;break;
            case FOLDER:mediaTable = FOLDER_TABLE_NAME;break;
            default:throw new IllegalArgumentException("Unknown URI " + uri);
        }
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(mediaTable);

        Cursor c = qb.query(db, projection, selection, selectionArgs,null, null, sortOrder);
        if (c == null)
            return null;
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;
        String mediaTable;
        switch (uriMatcher.match(uri)) {
            case AUDIO:mediaTable = AUDIO_TABLE_NAME;break;
            case VIDEO:mediaTable = VIDEO_TABLE_NAME;break;
            case FOLDER:mediaTable = FOLDER_TABLE_NAME;break;
            default:throw new IllegalArgumentException("Unknown URI " + uri);
        }
        count = db.delete(mediaTable, selection, selectionArgs);//参数1：表名   参数2：约束删除列的名字   参数3：具体行的值
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count = 0;
        String mediaTable;
        switch (uriMatcher.match(uri)) {
            case AUDIO:mediaTable = AUDIO_TABLE_NAME;break;
            case VIDEO:mediaTable = VIDEO_TABLE_NAME;break;
            case FOLDER:mediaTable = FOLDER_TABLE_NAME;break;
            default:throw new IllegalArgumentException("Unknown URI " + uri);
        }
        count = db.update(mediaTable, values, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        String mediaTable;
        switch (uriMatcher.match(uri)) {
            case AUDIO:mediaTable = AUDIO_TABLE_NAME;break;
            case VIDEO:mediaTable = VIDEO_TABLE_NAME;break;
            case FOLDER:mediaTable = FOLDER_TABLE_NAME;break;
            default:throw new IllegalArgumentException("Unknown URI " + uri);
        }
        return mediaTable;
    }
    public void mediascanner(int isNewVolume){
        Trace.beginSection("mediascanner");
        Log.e(TAG,"mediascanner : "+ isNewVolume);
        long startTime = System.nanoTime();
//        String scanPath = "/sdcard/android_ubuntu";
        scan(scanPath, isNewVolume);
        long endTime = System.nanoTime();
        Log.e(TAG,"all scan resume time : " + (endTime-startTime)/1000000.0 + " ms");
        Trace.endSection();
    }

    public native int scan(String scanPath, int isNewVolume);
}