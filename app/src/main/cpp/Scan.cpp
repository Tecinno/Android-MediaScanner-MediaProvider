// #include <sys/stat.h>
// #include <dirent.h>


#include <sstream>
#include <jni.h>
#include "Scan.h"
#include "android/log.h"

namespace android{
#define ANDORID_LOG_ERROR 6
#define printf(...) __android_log_print(ANDORID_LOG_ERROR, "Scanner", __VA_ARGS__)
#define MEDIA_SCAN_RESULT_OK 1
#define MEDIA_SCAN_RESULT_SKIPPED 2
#define MEDIA_SCAN_RESULT_ERROR 0
#define DBPATH  "/thirdparty/0/udiskMediaData/com.czy.jni/database/external_udisk.db"
#define DBPATH1 "/data/data/com.czy.jni/databases/external_udisk.db"
    static const char *videoType[] = {".mp4", ".3Gp", ".m4v", ".avi"};
    static const char *audioType[] = {".mp3", ".ape", ".flac", ".wav", ".m4a"};
    static const int audioSize = sizeof(audioType) / sizeof(audioType[0]);
    static const int videoSize = sizeof(videoType) / sizeof(videoType[0]);

    Scan::Scan(){
    };
    Scan::~Scan(){};
    int Scan::callback(void *data, int args_num, char **columnValue, char **columnName) {
        printf("callback-------------\n");
        for(int i = 0; i < args_num; i++){
            printf("%s = %s\n", columnName[i], columnValue[i]);
        }

        return 0;
    }



    int Scan::ProcessDirectory(const char *path) {

        printf("ProcessDirectory");
//        return 0;
        struct stat statbuf;
        std::queue<sDirEntry> q1;
        sDirEntry root(0, path);
        char fileNameStore[4096];
        int dirCount = 0;
        int audioCount = 0;
        int videoCount = 0;
        int dirLayer = 0;
        bool noMedia = false;
        int parent_id = 0;
        //================open database===============
        if (creat_database(mdb) == -1) {
            printf("creat_database failed\n");
            return -1;
        }
//        if (mdb != NULL) {
//            sqlite3_close(mdb);
//            printf("sqlite3_close");
//        } else {
//            printf("mdb is NULL when close db");
//        }

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
            printf("begin opendir !!!\n");
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
                    printf("doProcessDirectory SKIPPED: %s \n",dir_entity_p->d_name);
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
                printf("before mdb == NULL\n");
//                if (creat_database(mdb) == -1) {
//                    printf("creat_database failed\n");
//                    return -1;
//                }

                if (mdb == NULL){
                    printf("mdb == NULL\n");
                }
                parent_id = getId(dir_entry_parent.abs_file_name_p);
                if (parent_id == -1) {
                    printf("getParentId fail !!!\n");
                    parent_id = 0;
//                    continue;
                }
                //=======================end query parent_id=====================

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
                                    printf("mdb == NULL when audio scanFile");
                                    return -1;
                                }else if (scanFile(mdb, fileNameStore, audio, parent_id, dirLayer) == true) {
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
                                    printf("mdb == NULL when video scanFile");
                                    return -1;
                                }else if (scanFile(mdb, fileNameStore, video, parent_id, dirLayer) == true) {
                                    ++videoCount;
                                    // printf("insert sqlite3 %s ,success\n", fileNameStore);
                                } else
                                    printf("insert sqlite3 %s ,fail !!!\n", fileNameStore);
                                break;
                            }
                        }
                    }



                } else if (type == DT_DIR) {
                    // all dir count could not more than 9999
                    if (dirCount > 9999) {
                        printf("MediaScanner::doProcessDirectoryEntry  dirCount is %d >= 9999 \n",dirCount);
                        if (mdb == NULL) {
                            printf("mdb == NULL when sqlite3_close");
                            return -1;
                        }else
                            sqlite3_close(mdb);
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
                    if (mdb == NULL) {
                        printf("mdb == NULL when scanfile");
                        return -1;
                    } else if(scanFile(mdb, fileNameStore, folder, parent_id, dirLayer) == true) {
                        // printf("insert sqlite3 %s ,success\n", fileNameStore);
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
        printf("MediaScanner::doProcessDirectory finish, audioCount = %d, videoCount = %d, dirLayer = %d, dirCount = %d \n",
               audioCount, videoCount, dirLayer, dirCount);
        printf("\n");
        printf("\n");


        //=====================query=====================
        // printf("select \n");
        // sql = "select * from audio";
        // rs = sqlite3_exec(db, sql.c_str(), callback, callbackData, &errMsg);
        // if (rs != SQLITE_OK) {
        //     printf("select sqlite3 fail %s\n", errMsg);

        // } else {
        //     printf("%s\n", sql.c_str());
        //     printf("select sqlite3 success callbackData:%s\n", callbackData);
        // }
        //=====================query=====================


        if (mdb != NULL)
           sqlite3_close(mdb);
        else
            printf("mdb is NULL when close db");
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
        sqlite3_stmt* stmt = queryData("audio", projection, 2, NULL, NULL);
        if (stmt == NULL) {
            printf("prescan stmt == NULL\\n");
            sqlite3_finalize(stmt);
            return ;
        }
        std::string sql = "delete from audio where _id in ( ";
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
            return ;
        } else {
            printf("prescan %s ,success\n", sql.c_str());
        }
        sqlite3_finalize(stmt);
    }
/*
    get folder Id in folder_dir
*/
    int Scan::getId(const char* path) {
        if (mdb == NULL){
            printf("mdb == NULL\n");
            int rc = sqlite3_open_v2(DBPATH1, &mdb, SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE | SQLITE_OPEN_NOMUTEX | SQLITE_OPEN_SHAREDCACHE, NULL);
            if (rc != SQLITE_OK) {
                return -1;
            }
        }
        sqlite3_stmt* stmt = NULL;
        const char* zTail;
        const char *dirSuffix = strrchr(path, '/') + 1;
        std::string sql = "select _id from folder_dir where _name = '";
        sql.append(dirSuffix);
        sql.append("'");
        if (sqlite3_prepare_v2(mdb, sql.c_str(), -1, &stmt, &zTail) == SQLITE_OK) {
            if (sqlite3_step(stmt) == SQLITE_ROW) {
                int parent_id = sqlite3_column_int(stmt, 0);
                // std::stringstream ss;
                // ss<<id;
                // ss>>parent_id;
                // sprintf(parent_id, "%d", id);
//                printf("query parent_id success dirSuffix : %s ,parent_id: %d \n", dirSuffix, parent_id);
                sqlite3_finalize(stmt);
                return parent_id;
            } else {
                printf("getId not id \n");
                sqlite3_finalize(stmt);
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

// bool Scan::checkFileChange(String path, long lastFileChange) {
//     sqlite3 *db = open_database();
//     if (db == NULL) {
//         printf("open_database  fail !!!\n");
//         return NULL;
//     }
// }

// void Scan::testQuery() {
//     std::string projection[1] = {"*"};
//     sqlite3_stmt* stmt = queryData("audio", projection, "_id", "2");
//     if (sqlite3_step(stmt) == SQLITE_ROW) {
//         const unsigned char* data = sqlite3_column_text(stmt, 4);
//         printf("testQuery data: %s \n", data);
//     } else {
//         printf("testQuery not id \n");
//         return;
//     }
// }

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
        return MEDIA_NEED_INSERT;
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
    bool Scan::scanFile(sqlite3 *db, const char* path, mediaType type, int parentId, const int dirLayer) {
        // static std::string new_audio_list = "delete from audio where _id not in ( ";
        // static std::string new_video_list = "delete from video where _id not in ( ";;
        // static std::string new_folder_list = "delete from folder_dir where _id not in ( ";;


        const char* name = strrchr(path, '/') + 1;


        // printf("scanFile parentId %s !!!\n", parentId);
        std::string sql;
        std::string value;
        std::string parent_id;
        // printf("scanFile 0  parentId %s !!!\n", parentId);
        char* errMsg;
        // printf("scanFile -2  parentId %s !!!\n", parentId);
        //get data size and changeTime
        struct stat statbuf;
        stat(path, &statbuf);
        // printf("scanFile -1  parentId %s !!!\n", parentId);
        std::string layer;
        std::string size;
        std::string mtime;
        // printf("scanFile 1  parentId %s !!!\n", parentId);
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
        // printf("scanFile 2  parentId %s !!!\n", parentId);
        int checkFileResult = checkFileNeedUpdate(path, statbuf.st_mtime, type);
        // checkFileResult = MEDIA_NO_UPDATE;
        if (checkFileResult == MEDIA_NO_UPDATE) {
            printf("checkFileResult exist  : %s !!!\n", path);
            return true;
        }
        // printf("scanFile 3 parentId %s !!!\n", parentId);
        if (type == audio) {
            if (checkFileResult == MEDIA_NEED_INSERT) {
                updateFolderHaveMedia(db, parentId, audio);
//                printf("audio parentId %d !!!\n", parentId);
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
            } else if (checkFileResult == MEDIA_NEED_UPDATE) {
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
            }

        } else if (type == video) {
            if (checkFileResult == MEDIA_NEED_INSERT) {
                updateFolderHaveMedia(db, parentId, video);
//                printf("video parentId %d !!!\n", parentId);
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
            } else if (checkFileResult == MEDIA_NEED_UPDATE) {
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
            }
        } else if (type == folder) {
//            printf("folder parentId %d !!!\n", parentId);
            // getId(path);
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
            }
        }
        // printf("sql %s \n", sql.c_str());

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
        }
//        printf("open sqlite3 success\n");
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

//        sqlite3 *db = open_database();
        char *errMsg;
        //===============creat audio table==============
        std::string sql = "CREATE TABLE IF NOT EXISTS audio("
        "_id INTEGER PRIMARY KEY AUTOINCREMENT,"\
        "parent_id INTEGER,"\
        "size INTEGER,"\
        "mtime INTEGER,"\
        "_name TEXT NOT NULL,"\
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
        "_name TEXT NOT NULL,"\
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

        //===============set locale language==============
//        sql = "insert into android_metadata (locale) values ('zh')";//"insert into audio(_path, _data) values ('fileNameStore', 'fileNameStore')"
//        rs = sqlite3_exec(db, sql.c_str(), 0, 0, &errMsg);
//        if (rs != SQLITE_OK) {
//            printf("insert sqlite3 fail \n");
//        }
        //================sqlite3===============

        //=============insert test=========
//        printf("insert test=================   \n");
//        sql = "insert into audio (_name, _path) values ('sqlite3test', 'sqlite3test')";
//        rs = sqlite3_exec(db, sql.c_str(), 0, 0, &errMsg);
//        if (rs != SQLITE_OK) {
//            printf("insert sqlite3 fail %s\n", errMsg);
//            return -1;
//        }
//        printf("insert sqlite3 success\n");
        return 0;
    }




};

