// #include <sys/stat.h>
// #include <dirent.h>


#include <sstream>
#include <jni.h>
#include "Scan.h"
#include "android/log.h"

namespace android{

#define ANDORID_LOG_ERROR 6
#define TAG     "Scanner"
#define printf(...) __android_log_print(ANDORID_LOG_ERROR, TAG, __VA_ARGS__)
//#define printf(fmt) __android_log_print(ANDORID_LOG_ERROR, TAG, "[TC:%ld][TT:%ld]" fmt, __get_tick_count(), __get_thread_time(), __VA_ARGS__)
#define MEDIA_SCAN_RESULT_OK 1
#define MEDIA_SCAN_RESULT_SKIPPED 2
#define MEDIA_SCAN_RESULT_ERROR 0
#define DBPATH  "/thirdparty/0/udiskMediaData/com.czy.jni/database/external_udisk.db"
#define DBPATH1 "/data/data/com.czy.jni/databases/external_udisk.db"
#define ONCE_INSERT_COUNT 200

static const char *videoType[] = {".mp4", ".3Gp", ".m4v", ".avi"};
static const char *audioType[] = {".mp3", ".ape", ".flac", ".wav", ".m4a"};
static const int audioSize = sizeof(audioType) / sizeof(audioType[0]);
static const int videoSize = sizeof(videoType) / sizeof(videoType[0]);

    Scan::Scan(){
    };
    Scan::~Scan(){
    };
    int Scan::callback(void *data, int args_num, char **columnValue, char **columnName) {
        printf("callback-------------\n");
        for(int i = 0; i < args_num; i++){
            printf("%s = %s\n", columnName[i], columnValue[i]);
        }

        return 0;
    }



    int Scan::ProcessDirectory(const char *path, bool firstScan) {

        printf("ProcessDirectory");
//        return 0;
        clock_t  start = clock();
        struct stat statbuf;
        std::queue<sDirEntry> q1;
        sDirEntry root(0, path);
//        memcpy(pathRoot, path, strlen(path));
        rootPathLen = strlen(path);
        char fileNameStore[4096];
        int dirCount = 0;
        int audioCount = 0;
        int videoCount = 0;
        int dirLayer = 0;
        bool noMedia = false;
        int parent_id = 0;
        insertcount = 0;
        char *errMsg;
        //================open database===============
        if (creat_database(mdb) == -1) {
            printf("creat_database failed\n");
            return -1;
        }


        //================open database===============

        q1.push(root);
        prescan();
        while(!q1.empty()) {
            sDirEntry dir_entry_parent = q1.front();
            //dirLayer control
            dirLayer = dir_entry_parent.depth;
            if (dirLayer >= 100) {
                printf("MediaScanner::doProcessDirectoryEntry  dirLayer is %d >= 100 \n",dir_entry_parent.depth);
                sqlite3_close(mdb);
                return MEDIA_SCAN_RESULT_SKIPPED;
            }
            DIR *dir_p = opendir(dir_entry_parent.abs_file_name_p);
            if (!dir_p) {
                printf("Error opening directory '%s', skipping: %s.\n", dir_entry_parent.abs_file_name_p, strerror(errno));
                q1.pop();
                closedir(dir_p);
                continue;
            }

            struct dirent *dir_entity_p;
            int filecount = 0;
            while((dir_entity_p = readdir(dir_p)) != NULL)
            {

                if (dir_entity_p->d_name[0] == '.' &&
                    (dir_entity_p->d_name[1] == 0 || (dir_entity_p->d_name[1] == '.' && dir_entity_p->d_name[2] == 0)
                     || strncmp(dir_entity_p->d_name,".nomedia",8))) {
//                    printf("doProcessDirectory SKIPPED: %s \n",dir_entity_p->d_name);
                    continue;
                }

                //each dir files count more than 9999 should SKIPPED
                if (filecount >= 9999) {
                    printf(" doProcessDirectory filecount per dir is %d >= 9999 \n", filecount);
                    break;
                } else {
                    ++filecount;
                }

                if ((sizeof(dir_entry_parent.abs_file_name_p) + sizeof(dir_entity_p->d_name) + 1 ) >= 4096) {
                    printf("path is too long : %s%s !!! \n",dir_entry_parent.abs_file_name_p,dir_entity_p->d_name);
                    continue;
                }
                // memset(fileNameStore, 0x00, sizeof(fileNameStore)/sizeof(fileNameStore[0]));
                memset(fileNameStore, 0, sizeof(fileNameStore));
                sprintf(fileNameStore, "%s/%s", dir_entry_parent.abs_file_name_p, dir_entity_p->d_name);
                // memcpy
                int type = dir_entity_p->d_type;
                if (type == DT_UNKNOWN)
                {
                    // If the type is unknown, stat() the file instead.
                    // This is sometimes necessary when accessing NFS mounted filesystems, but
                    // could be needed in other cases well.
                    if (stat(fileNameStore, &statbuf) == 0) {
                        if (S_ISREG(statbuf.st_mode)) {
                            type = DT_REG;
                        } else if (S_ISDIR(statbuf.st_mode)) {
                            type = DT_DIR;
                        }
                    } else {
                        printf("stat() failed for %s: %s \n", dir_entry_parent.abs_file_name_p, strerror(errno) );
                    }
                }

                //=======================start query parent_id=====================
                //find folder or file parent folder id,save in parent_id
                //audio ,video or folder will use it
                if (!firstScan) {
                    parent_id = getId(dir_entry_parent.abs_file_name_p);
                    if (parent_id == -1) {
                        printf("getParentId fail !!!\n");
                        parent_id = 0;
                    }
                } else
                    parent_id = 0;

                //=======================end query parent_id=====================
//                printf("DT_DIR fileNameStore %s",fileNameStore);
                if (type == DT_REG)
                {
                    bool findMediaFile = false;
                    const char *nameSuffix = strrchr(dir_entity_p->d_name, '.');
                    if (!nameSuffix)
                        continue;
                    // printf("scan audio name is : %s   audioCount is : %d \n", dir_entity_p->d_name, audioCount + 1);
                    if (audioCount < 9999) {
                        for (int i = 0; i < audioSize; i++) {
                            if (!strcasecmp(nameSuffix, audioType[i])) {
                                // printf("scan audio name is : %s   audioCount is : %d \n", dir_entity_p->d_name, audioCount + 1);
                                findMediaFile = true;
                                //extract file data and insert to database
                                if (mdb == NULL) {
                                    open_database(mdb);
                                    if (mdb == NULL) {
                                        printf("mdb == NULL when audio scanFile");
                                        return -1;
                                    }
                                }else if (scanFile(mdb, fileNameStore, audio, parent_id, dirLayer, firstScan) == true) {
                                    ++audioCount;
                                    // printf("insert sqlite3 %s ,success\n", fileNameStore);
                                } else
                                    printf("insert sqlite3 %s ,fail !!!\n", fileNameStore);
                                break;
                            }
                        }
                    }
                    if (videoCount < 9999 && !findMediaFile) {
                        for (int i = 0; i < videoSize; i++) {
                            if (!strcasecmp(nameSuffix, videoType[i])) {
                                // printf("scan video name is : %s   videoCount is : %d \n", dir_entity_p->d_name, videoCount + 1);
                                //extract file data and insert to database
                                if (mdb == NULL) {
                                    open_database(mdb);
                                    if (mdb == NULL) {
                                        printf("mdb == NULL");
                                        return -1;
                                    }
                                }else if (scanFile(mdb, fileNameStore, video, parent_id, dirLayer, firstScan) == true) {
                                    ++videoCount;
                                    // printf("insert sqlite3 %s ,success\n", fileNameStore);
                                } else
                                    printf("insert sqlite3 %s ,fail !!!\n", fileNameStore);
                                break;
                            }
                        }
                    }



                } else if (type == DT_DIR) {
//                    printf("DT_DIR fileNameStore %s",fileNameStore);
                    // all dir count could not more than 9999
                    if (dirCount > 9999) {
                        printf("MediaScanner::doProcessDirectoryEntry  dirCount is %d >= 9999 \n",dirCount);
                        return MEDIA_SCAN_RESULT_SKIPPED;
                    } else {
                        ++dirCount;
                    }

                    bool childNoMedia = noMedia;
                    // set noMedia flag on directories with a name that starts with '.'
                    // for example, the Mac ".Trashes" directory
                    if (dir_entity_p->d_name[0] == '.')
                        childNoMedia = true;
                    // nomedia flag is used to make a ".nomedia" file to stop android to stopping scanning medie files in this dir
                    // if (stat(fileNameStore, &statbuf) == 0)
                    // {
                    if (!firstScan) {
//                        if (mdb == NULL)
//                            open_database(mdb);
//                            if (mdb == NULL) {
//                                printf("mdb == NULL when DT_DIR scanFile");
//                                return -1;
//                            }
//                        printf("DT_DIR fileNameStore %s",fileNameStore);
//                        scanFile(mdb, fileNameStore, folder, parent_id, dirLayer, firstScan);
                    }

                    //push folder dir to queue
                    sDirEntry s_dir(dir_entry_parent.depth + 1, fileNameStore);
                    q1.push(s_dir);
                }
                else {
                    // do nothing
                    ;
                }
            }

            q1.pop();
            closedir(dir_p);
        }
        // sqlite3_close(db);
        if (mdb != NULL) {
            flush(mdb, firstScan);
            sqlite3_close(mdb);
        }else
            printf("mdb is NULL when close db");
        clock_t  end = clock();
        double resumetime = (double)(end - start)/1000.0;

        if (firstScan)
            printf("first scan resume time : %lf ms", resumetime);
        else
            printf("second scan resume time : %lf ms", resumetime);

        printf("MediaScanner::doProcessDirectory finish, audioCount = %d, videoCount = %d, dirLayer = %d, dirCount = %d \n",
               audioCount, videoCount, dirLayer, dirCount);
        printf("\n");
        printf("\n");

        return MEDIA_SCAN_RESULT_OK;
    }

    /*
    delete the older data
    */
    void Scan::prescan(){
        printf("prescan \\n");
        if(mdb == NULL)
            open_database(mdb);
        const char* projection[2] = {"_id", "_path"};
        char* errMsg;
        std::string table;
        sqlite3_stmt* stmt;
        for(int i = 0; i < 3; ++i) {
            switch (i) {
                case 0:stmt = queryData("audio", projection, 2, NULL, NULL);table = "audio";
                    break;
                case 1:stmt = queryData("video", projection, 2, NULL, NULL);table = "video";
                    break;
                case 2:stmt = queryData("folder_dir", projection, 2, NULL, NULL);table = "folder_dir";
                    break;
            }
            if (stmt == NULL) {
                printf("prescan stmt == NULL\\n");
                sqlite3_finalize(stmt);
                continue ;
            }
            std::string sql = "delete from ";
            sql.append(table);
            sql.append(" where _id in ( ");
            while (sqlite3_step(stmt) == SQLITE_ROW) {
                int id = sqlite3_column_int(stmt, 0);
                if (id >= 0) {
                    std::string deleteid;
                    std::stringstream ss;
                    ss<<id;
                    ss>>deleteid;
                    const char* abspath = (char*)sqlite3_column_text(stmt, 1);
                    if( access( abspath, 0) == -1 ) {
                        sql.append(deleteid);
                        while (sqlite3_step(stmt) == SQLITE_ROW) {
                            id = sqlite3_column_int(stmt, 0);
                            if (id >= 0) {
                                std::stringstream ss;
                                ss<<id;
                                ss>>deleteid;
                                abspath = (char*)sqlite3_column_text(stmt, 1);
                                if( access( abspath, 0) == -1 ) {
                                    printf("prescan id : %d path : %s no exist !!! \\n", id, abspath);
                                    sql.append(", ");
                                    sql.append(deleteid);
                                }
                            }
                        }
                        break;
                    }
                }
            }
            sql.append(")");
            int rs = sqlite3_exec(mdb, sql.c_str(), 0, 0, &errMsg);
            if (rs != SQLITE_OK) {
                printf("prescan %s fail %s !!!\n", sql.c_str(), errMsg);
                sqlite3_finalize(stmt);
                continue ;
            } else {
                printf("prescan %s ,success\n", sql.c_str());
            }
            sqlite3_finalize(stmt);
        }


    }
/*
    get folder Id in folder_dir
    *可以保存上次的路径，不用再查询
*/
    int Scan::getId(const char* path) {
        if (mdb == NULL){
            open_database(mdb);
        }
        sqlite3_stmt* stmt = NULL;
        const char* zTail;
        const char *dirSuffix = strrchr(path, '/') + 1;//获取文件夹的名称

//        if (!strncmp(path, pathRoot, strlen(path))){ // 判断是不是根文件夹
        if (strlen(path) == rootPathLen){ // 判断是不是根文件夹
//            printf("getId is root : %s \n",path );
            return 0;
        }
//        else
//            printf("getId is not root : %s  \n",path);
        std::string sql = "select _id from folder_dir where _name = '";//查询文件夹的id
        sql.append(dirSuffix);
        sql.append("'");
        if (sqlite3_prepare_v2(mdb, sql.c_str(), -1, &stmt, &zTail) == SQLITE_OK) {
            if (sqlite3_step(stmt) == SQLITE_ROW) {
                int parent_id = sqlite3_column_int(stmt, 0);
                sqlite3_finalize(stmt);
                return parent_id;
            } else {
                printf("getId %s no find \n",dirSuffix);
                int len = strlen(path) - strlen(dirSuffix) - 1;
                char parentPath[len + 1];
                memset(parentPath, 0, sizeof(parentPath));
                memcpy(parentPath, path, len);//获取父文件夹路径
//                parentPath[len]='\0';
//                printf("getId  len %d,  parentPath %s \n",len, parentPath);
                int parentid = getId(parentPath);
                if (parentid != -1)
                    insertFolder(mdb, path, parentid); //如果没有找到对应folder，就插入
                sqlite3_finalize(stmt);
                sql = "select _id from folder_dir order by _id desc LIMIT 1";//获取刚刚插入的id
                if (sqlite3_prepare_v2(mdb, sql.c_str(), -1, &stmt, &zTail) == SQLITE_OK) {
                    if (sqlite3_step(stmt) == SQLITE_ROW) {
                        int id = sqlite3_column_int(stmt, 0);
                        sqlite3_finalize(stmt);
                        return id;
                    }
                }
                return 0;
            }
        } else {
            printf("sqlite3_prepare_v2 fail\n");
            sqlite3_finalize(stmt);
            return -1;

        }
    }


    sqlite3_stmt* Scan::queryData(const char* table, const char* projection[], int projectionSize, const char* selection, const char* selectArg) {
//        sqlite3 *db = open_database();
        if (mdb == NULL){
            printf("mdb == NULL\n");
            int rc = sqlite3_open_v2(DBPATH1, &mdb, SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE | SQLITE_OPEN_NOMUTEX | SQLITE_OPEN_SHAREDCACHE, NULL);
            if (rc != SQLITE_OK) {
                return NULL;
            }
        }
        sqlite3_stmt* stmt = NULL;
        const char* zTail;
        // sql = "select projection[] from table where selection = selectArg";
        std::string sql = "select ";
        sql.append(projection[0]);
//        int projectionSize = sizeof(projection)/sizeof(projection[0]);
        for (int i = 1; i < projectionSize; ++i) {
            sql.append(",");
            sql.append(projection[i]);
        }
        sql.append(" from ");
        sql.append(table);
        if (selection != NULL) {
            sql.append(" where ");
            sql.append(selection);
            sql.append(" = '");
            sql.append(selectArg);
            sql.append("'");
        }

//        printf(" projectionSize: %d , %s \n", projectionSize, sql.c_str());
        if (sqlite3_prepare_v2(mdb, sql.c_str(), -1, &stmt, &zTail) == SQLITE_OK) {
            return stmt;
        } else {
            printf("sqlite3_prepare_v2 fail\n");
            return NULL;
        }
    }

    int Scan::checkFileNeedUpdate(const char* path, int mtime, mediaType type) {
        switch (type){
            case audio:{
                const char* projection[2] = {"_id", "mtime"};
                sqlite3_stmt* stmt = queryData("audio", projection, 2, "_path", path);
                if (stmt == NULL) {
                    printf("checkFileNeedUpdate stmt == NULL\n");
                    sqlite3_finalize(stmt);
                    return MEDIA_NEED_UPDATE;
                }
                if (sqlite3_step(stmt) == SQLITE_ROW) {

                    int id = sqlite3_column_int(stmt, 0);
//                    printf("checkFileNeedUpdate SQLITE_ROW :%s  id %d \n", path, id);
                    if (id > 0) {
                        int oldMtime = sqlite3_column_int(stmt, 1);
                        if (oldMtime == mtime) {
                            // printf("checkFileNeedUpdate file no change :%s\n", path);
                            sqlite3_finalize(stmt);
                            return MEDIA_NO_UPDATE;
                        } else {
                            sqlite3_finalize(stmt);
                            return MEDIA_NEED_UPDATE;
                        }
                    }
                }
                sqlite3_finalize(stmt);
            }
                break;
            case video:{
                const char* projection[2] = {"_id", "mtime"};
                sqlite3_stmt* stmt = queryData("video", projection,2, "_path", path);
                if (stmt == NULL) {
                    printf("checkFileNeedUpdate stmt == NULL\n");
                    sqlite3_finalize(stmt);
                    return MEDIA_NEED_UPDATE;
                }
                if (sqlite3_step(stmt) == SQLITE_ROW) {
                    int id = sqlite3_column_int(stmt, 0);
                    if (id > 0) {
                        int oldMtime = sqlite3_column_int(stmt, 1);
                        if (oldMtime == mtime) {
                            // printf("checkFileNeedUpdate file no change :%s\n", path);
                            sqlite3_finalize(stmt);
                            return MEDIA_NO_UPDATE;
                        } else {
                            sqlite3_finalize(stmt);
                            return MEDIA_NEED_UPDATE;
                        }
                    }
                }
                sqlite3_finalize(stmt);
            }
                break;
            case folder:{
                const char* projection[1] = {"_id"};
                sqlite3_stmt* stmt = queryData("folder_dir", projection, 1, "_path", path);
                if (stmt == NULL) {
                    printf("checkFileNeedUpdate stmt == NULL\n");
                    sqlite3_finalize(stmt);
                    return MEDIA_NEED_UPDATE;
                }
                if (sqlite3_step(stmt) == SQLITE_ROW) {
                    int id = sqlite3_column_int(stmt, 0);
                    if (id > 0) {
                        sqlite3_finalize(stmt);
                        return MEDIA_NO_UPDATE;
                    } else {
                        return MEDIA_NEED_UPDATE;
                    }
                }
                sqlite3_finalize(stmt);
            }
                break;
            default:printf("checkFileNeedUpdate table no find\n");return false;
        }
//        printf("MEDIA_NEED_INSERT :%s\n", path);
        return MEDIA_NEED_INSERT;
    }

    bool Scan::flush(sqlite3 *db, bool firstScan) {
        printf("flush  %d !!!\n", mediaList.size());
        char* errMsg;
        if (firstScan){
            if (sqlite3_exec(db,"begin transaction;",0,0,&errMsg) != SQLITE_OK) {
                printf("begin fail %s !!!\n", errMsg);
                return false;
            } else
                printf("begin   !!!\n");
        }

        std::list<std::string>::iterator ctr;
        for (ctr = mediaList.begin(); ctr != mediaList.end(); ++ctr) {
            std::string sql = *ctr;
            int rs = sqlite3_exec(db, sql.c_str(), 0, 0, &errMsg);
            if (rs != SQLITE_OK) {
                printf("%s fail %s !!!\n", sql.c_str(), errMsg);
//            sqlite3_exec(db,"rollback transaction;",0,0, &errMsg);
//            printf("rollback transaction;  %s !!!\n",  errMsg);
//                return false;
            } else
                printf("%s", sql.c_str());
        }
        mediaList.clear();
        if (firstScan) {
            if (sqlite3_exec(db,"commit transaction;",0,0,&errMsg) != SQLITE_OK) {
                printf("commit fail %s !!!\n", errMsg);
                return false;
            } else
                printf("commit   !!!\n");
        }

        return true;
    }
    /*
    * The method insert folder data to database
    *
    * @param db : database
    * @param path : data path
    * @param parentId : previous directory's id
    */
    bool Scan::insertFolder(sqlite3 *db, const char* path, int parentId) {
        const char* name = strrchr(path, '/') + 1;
        printf("insertFolder path %s  !!!\n",path);
        std::string sql;
        std::string value;
        std::string parent_id;
        char* errMsg;
        {
            std::stringstream ss;
            ss<<parentId;
            ss>>parent_id;
        }
        value = "values ('";
        value.append(path);
        value.append("','");
        value.append(name);
        value.append("',");
        value.append(parent_id);
        value.append(")");
        sql = "insert into folder_dir(_path, _name, parent_id) ";
        sql.append(value);
        int rs = sqlite3_exec(db, sql.c_str(), 0, 0, &errMsg);
        if (rs != SQLITE_OK) {
            printf("%s fail %s !!!\n", sql.c_str(), errMsg);
            return false;
        } else {
            printf("%s ,success\n", sql.c_str());
            return true;
        }
    }

    /*
     * The method insert or update audio/video/folder data to database
     *
     * @param db : database
     * @param path : data path
     * @param type : media type (audio/video/folder)
     * @param parentId : previous directory's id
     * @param parentId : directory's layer
     */
    bool Scan::scanFile(sqlite3 *db, const char* path, mediaType type, int parentId, const int dirLayer, bool firstScan) {
        const char* name = strrchr(path, '/') + 1;
        // printf("scanFile parentId %s !!!\n", parentId);
        std::string sql;
        std::string value;
        std::string parent_id;
        char* errMsg;
        struct stat statbuf;
        std::string layer;
        std::string size;
        std::string mtime;
            stat(path, &statbuf);
            {
                std::stringstream ss;
                ss<<dirLayer;
                ss>>layer;
            }
            {
                std::stringstream ss;
                ss<<statbuf.st_size;
                ss>>size;
            }
            {
                std::stringstream ss;
                ss<<statbuf.st_mtime;
                ss>>mtime;
            }
            {
                std::stringstream ss;
                ss<<parentId;
                ss>>parent_id;
            }
        int checkFileResult = checkFileNeedUpdate(path, statbuf.st_mtime, type);
        if (checkFileResult == MEDIA_NO_UPDATE) {
            return true;
        }


        if (type == audio) {
            if (checkFileResult == MEDIA_NEED_INSERT) {
                if(firstScan) {
                    sql = "insert into audio(_path, _name) ";
                    value = "values ('";
                    value.append(path);
                    value.append("','");
                    value.append(name);
                    value.append("')");
                    sql.append(value);
                }else {
                    updateFolderHaveMedia(db, parentId, audio);
                    sql = "insert into audio(_path, _name, parent_id, size, mtime) ";
                    value = "values ('";
                    value.append(path);
                    value.append("','");
                    value.append(name);
                    value.append("',");
                    value.append(parent_id);
                    value.append(",");
                    value.append(size);
                    value.append(",");
                    value.append(mtime);
                    value.append(")");
                    sql.append(value);//"insert into audio(_path, _data) values ('fileNameStore', 'fileNameStore')"
                }
            } else if (checkFileResult == MEDIA_NEED_UPDATE && !firstScan) {
                sql = "update audio set ";
                value = "_path = '";
                value.append(path);
                value.append("',_name = '");
                value.append(name);
                value.append("',parent_id = '");
                value.append(parent_id);
                value.append("',size = '");
                value.append(size);
                value.append("',mtime = '");
                value.append(mtime);
                value.append("' where _path = '");
                value.append(path);
                value.append("'");
                sql.append(value);
            } else {
                return true;
            }
        } else if (type == video) {
            if (checkFileResult == MEDIA_NEED_INSERT) {
                if(firstScan) {
                    sql = "insert into video(_path) ";
                    value = "values ('";
                    value.append(path);
                    value.append("')");
                    sql.append(value);
                }else {
                    updateFolderHaveMedia(db, parentId, video);
                    sql = "insert into video(_path, _name, parent_id, size, mtime) ";
                    value = "values ('";
                    value.append(path);
                    value.append("','");
                    value.append(name);
                    value.append("',");
                    value.append(parent_id);
                    value.append(",");
                    value.append(size);
                    value.append(",");
                    value.append(mtime);
                    value.append(")");
                    sql.append(value);//"insert into audio(_path, _data) values ('fileNameStore', 'fileNameStore')"
                }
            } else if (checkFileResult == MEDIA_NEED_UPDATE && !firstScan) {
                sql = "update video set ";
                value = "_path = '";
                value.append(path);
                value.append("',_name = '");
                value.append(name);
                value.append("',parent_id = '");
                value.append(parent_id);
                value.append("',size = '");
                value.append(size);
                value.append("',mtime = '");
                value.append(mtime);
                value.append("' where _path = '");
                value.append(path);
                value.append("'");
                sql.append(value);
            } else {
                return true;
            }
        } else if (type == folder) {
            if (checkFileResult == MEDIA_NEED_INSERT) {
                value = "values ('";
                value.append(path);
                value.append("','");
                value.append(name);
                value.append("',");
                value.append(layer);
                value.append(",");
                value.append(parent_id);
                value.append(")");
                sql = "insert into folder_dir(_path, _name, dir_layer, parent_id) ";
                sql.append(value);
            } else {
                return true;
            }
        }

        mediaList.push_back(sql);
//        printf("push_back : %s ,size %d\n", sql.c_str(), mediaList.size());
        if (mediaList.size() > 200) {

            return  flush(mdb, firstScan);
        }
        return true;

//        if(insertcount == 0) {
//            if (sqlite3_exec(db,"begin transaction;",0,0,&errMsg) != SQLITE_OK) {
//                printf("begin fail %s !!!\n", errMsg);
//                return false;
//            }
//            printf("begin transaction!!!\n");
//        }
//        ++insertcount;
//        int rs = sqlite3_exec(db, sql.c_str(), 0, 0, &errMsg);
//        if (rs != SQLITE_OK) {
//            printf("%s fail %s !!!\n", sql.c_str(), errMsg);
////            sqlite3_exec(db,"rollback transaction;",0,0, &errMsg);
////            printf("rollback transaction;  %s !!!\n",  errMsg);
//            return false;
//        } else {
//            if(insertcount > ONCE_INSERT_COUNT) {
//                insertcount = 0;
//                if (sqlite3_exec(db,"commit transaction;",0,0,&errMsg) != SQLITE_OK) {
//                    printf("commit fail %s !!!\n", errMsg);
//                    return false;
//                }
//                printf("insert : %d \n", ONCE_INSERT_COUNT);
//            }
//            printf("%s ,success\n", sql.c_str());
//
//            return true;
//        }
    }

    /*
     * The method update folder_dir table has_audio or has_video to 1
     *
     * @param db : database
     * @param id : folder id
     * @param type : media type (audio/video)
     */
    bool Scan::updateFolderHaveMedia(sqlite3 *db, int id, mediaType type) {
        std::string sql;
        std::string value;
        std::string parent_id;
        {
            std::stringstream ss;
            ss<<id;
            ss>>parent_id;
        }
        char* errMsg;
        if (type == audio) {
            sql = "update folder_dir set ";
            value = "has_audio = '1'";
            value.append(" where _id = '");
            value.append(parent_id);
            value.append("'");
            sql.append(value);
        } else if (type == video) {
            sql = "update folder_dir set ";
            value = "has_video = '1'";
            value.append(" where _id = '");
            value.append(parent_id);
            value.append("'");
            sql.append(value);
        }
        int rs = sqlite3_exec(db, sql.c_str(), 0, 0, &errMsg);
        printf("updateFolderHaveMedia %s\n", sql.c_str());
        if (rs != SQLITE_OK) {
            printf("%s fail %s !!!\n", sql.c_str(), errMsg);
            return false;
        } else {
            printf("%s ,success\n", sql.c_str());
            return true;
        }
    }

    bool Scan::open_database(sqlite3* &mdb) {
        if (mdb == NULL) {
//            int rc = sqlite3_open("/data/data/com.czy.jni/cache/external_udisk1.db", &mdb);SQLITE_OPEN_NOMUTEX， SQLITE_OPEN_SHAREDCACHE///data/data/com.czy.jni/databases/external_udisk.db
            int rc = sqlite3_open_v2(DBPATH1, &mdb, SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE | SQLITE_OPEN_NOMUTEX | SQLITE_OPEN_SHAREDCACHE, NULL);
            if (rc != SQLITE_OK) {
                printf("open sqlite3 fail\n");
                return false;
            }
//            sqlite3_exec(mdb,"PRAGMA synchronous = OFF; ",0,0,0);
        }
        return true;
    }

    int Scan::creat_database(sqlite3* &db) {
        //==========open database====================
//        sqlite3 *db;
        int rc = sqlite3_open_v2(DBPATH1, &db, SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE | SQLITE_OPEN_NOMUTEX | SQLITE_OPEN_SHAREDCACHE, NULL);
        if (rc != SQLITE_OK) {
            printf("open db error  !!!\n");
            return -1;
        }
        sqlite3_exec(db,"PRAGMA synchronous = OFF; ",0,0,0);

//        sqlite3 *db = open_database();
        char *errMsg;
        //===============creat audio table==============
        std::string sql = "CREATE TABLE IF NOT EXISTS audio("
        "_id INTEGER PRIMARY KEY AUTOINCREMENT,"\
        "parent_id INTEGER,"\
        "size INTEGER,"\
        "mtime INTEGER,"\
        "_name TEXT ,"\
        "_path TEXT NOT NULL,"\
        "album TEXT,"\
        "genre TEXT,"\
        "artist TEXT"\
        ");";
        printf(" begin sqlite3_exec \n");
        int rs = sqlite3_exec(db, sql.c_str(), callback, 0, &errMsg);;
        if (rs != SQLITE_OK) {
            printf("CREATE audio table fail %s\n", errMsg);
            return -1;
        }
        printf("CREATE audio table success\n");

        //===============creat audiolist table==============
        sql = "CREATE TABLE IF NOT EXISTS audiolist("\
        "_id INTEGER PRIMARY KEY AUTOINCREMENT,"\
        "_name TEXT NOT NULL,"\
        "_path TEXT NOT NULL"\
        ");";

        rs = sqlite3_exec(db, sql.c_str(), 0, 0, &errMsg);
        if (rs != SQLITE_OK) {
            printf("CREATE audiolist table fail %s\n", errMsg);
            return -1;
        }
        printf("CREATE audiolist table success\n");

        //===============creat video table==============
        sql = "CREATE TABLE IF NOT EXISTS video("\
        "_id INTEGER PRIMARY KEY AUTOINCREMENT,"\
        "parent_id INTEGER,"\
        "size INTEGER,"\
        "mtime INTEGER,"\
        "_name TEXT,"\
        "_path TEXT NOT NULL"\
        ");";
        rs = sqlite3_exec(db, sql.c_str(), callback, 0, &errMsg);
        if (rs != SQLITE_OK) {
            printf("CREATE video table fail %s\n", errMsg);
            return -1;
        }
        printf("CREATE video table success\n");

        //===============creat videolist table==============
        sql = "CREATE TABLE IF NOT EXISTS videolist("\
        "_id INTEGER PRIMARY KEY AUTOINCREMENT,"\
        "_name TEXT NOT NULL,"\
        "_path TEXT NOT NULL"\
        ");";
        rs = sqlite3_exec(db, sql.c_str(), callback, 0, &errMsg);
        if (rs != SQLITE_OK) {
            printf("CREATE videolist table fail %s\n", errMsg);
            return -1;
        }
        printf("CREATE videolist table success\n");

        //===============creat folder_dir table==============
        sql = "CREATE TABLE IF NOT EXISTS folder_dir("\
        "_id INTEGER PRIMARY KEY AUTOINCREMENT,"\
        "parent_id INTEGER,"\
        "_name TEXT ,"\
        "_path TEXT NOT NULL,"\
        "dir_layer TEXT,"\
        "has_audio INTEGER,"\
        "has_video INTEGER"\
        ");";
        rs = sqlite3_exec(db, sql.c_str(), callback, 0, &errMsg);
        if (rs != SQLITE_OK) {
            printf("CREATE dir table fail %s\n", errMsg);
            return -1;
        }
        printf("CREATE dir table success\n");

        //===============creat album table==============
        sql = "CREATE TABLE IF NOT EXISTS album("\
        "_id INTEGER PRIMARY KEY AUTOINCREMENT,"\
        "parent_id INTEGER,"\
        "_name TEXT NOT NULL,"\
        "_path TEXT NOT NULL,"\
        "dir_layer TEXT NOT NULL,"\
        "has_audio INTEGER,"\
        "has_video INTEGER"\
        ");";
        rs = sqlite3_exec(db, sql.c_str(), callback, 0, &errMsg);
        if (rs != SQLITE_OK) {
            printf("CREATE dir table fail %s\n", errMsg);
            return -1;
        }
        printf("CREATE dir table success\n");

        //===============creat metadata table==============
        sql = "CREATE TABLE IF NOT EXISTS android_metadata("\
        "locale TEXT NOT NULL"\
        ");";
        rs = sqlite3_exec(db, sql.c_str(), callback, 0, &errMsg);
        if (rs != SQLITE_OK) {
            printf("CREATE video android_metadata fail %s\n", errMsg);
            return -1;
        }
        printf("CREATE video android_metadata success\n");

        return 0;
    }




};

