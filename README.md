重点在于数据库做成，UI太难看，不想花时间做。刚学计算机一年，代码写的有点乱，但是主要还是策略。
程序里面扫描的是sdcard,如果有USB就可以改下MediaProvider中修改scanPath。
对了，代码中加入了sqlite.c，如果在mk文件包含了libsqlite就不用加了，为了方便我直接加了。
移植的话很方便，只要调用native函数ProcessDirectory，然后修改一些配置就可以在指定数据库额外创建几个快表。

针对车机USB歌曲视频播放机能，MediaProvider对于新数据插入速度很慢，我的车机MIPS是10000，新的USB，一万首歌和一万个视频全部扫描需要20分钟，而我的需求是10秒内完成，对此我增加了一个快速扫描的机制，在Android扫描前或者同时，我做一次快速扫描，不添加专辑等信息（这样专辑列表可能慢几秒更新好），用最快的速度把所有歌曲视频文件夹路径名称id，parentid等信息插入数据库（同时也包括预扫描，扫描时判断文件是否存在是否更新过等机能），这样用户就可以在所有歌曲列表、所有视频列表或者文件夹里面找到自己想要播放的歌曲视频，为了方便找，也可以根据拼音排序，英文排序，或者文件修改时间排序。

![主界面.png](https://github.com/Tecinno/MediaScanner/blob/tamago/%E4%B8%BB%E7%95%8C%E9%9D%A2.png)

针对Android的MediaProvider 和 MediaScanner对车机USB播放的几点问题：
1、扫描的时候是按照深度扫描，并且顺序扫描，如果视频太多，音乐就一直扫描不到，而且一般歌曲视频不会放到那么深的目录；
2、如果USB删除了一部分媒体文件，Android虽然在预扫描的时候会删除不存在的数据，但是预扫描的时候是可以播放歌曲视频的，这个时候就会有脏数据；如果先预扫描再播放就很慢。
3、插入新USB，扫描新数据的时候会读取音频title等信息，耗时特别严重。
4、相对于车机的需求，Android创建的表比较多，多出很多冗余信息。

