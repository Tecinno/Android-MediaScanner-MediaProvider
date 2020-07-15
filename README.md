![简易音乐播放.png](https://i.loli.net/2020/07/14/C9aK1USMryjPh6Q.png)

针对Android的MediaProvider 和 MediaScanner对车机USB播放的几点问题：
1、扫描的时候是按照深度扫描，并且顺序扫描，如果视频太多，音乐就一直扫描不到，而且一般歌曲视频不会放到那么深的目录；
2、如果USB删除了一部分媒体文件，Android虽然在预扫描的时候会删除不存在的数据，但是预扫描的时候是可以播放歌曲视频的，这个时候就会有脏数据；如果先预扫描再播放就很慢。
3、插入新USB，扫描新数据的时候会读取音频title等信息，耗时特别严重。
4、相对于车机的需求，Android创建的表比较多，多出很多冗余信息。



针对Android有几个优化的点：
1、update 广播可以在prescan后再发，并且update广播传入参数区别audio和video，prescan之后取获取下数据库音视频数据，如果有数据就发送对应的update广播，除此之外update广播只在数据insert的时候发，音频和视频第一次扫描30个数据都发送update广播，
之后250个数据发送一次，prescan如果没有数据删除就不要做genre和album清除，MediaProvider改为插入时可以读取，不要把文件夹信息写到数据库，因为MediaProvider会根据媒体信息自动添加需要的文件夹，