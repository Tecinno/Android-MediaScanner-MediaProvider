# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)
#21.0.6113669   21.3.6528147  C:\Users\liujieqin\AppData\Local\Android\Sdk\ndk\21.3.6528147\platforms\android-24\arch-x86
#link_directories(C:\\Users\\liujieqin\\AppData\\Local\\Android\\Sdk\\ndk\\21.0.6113669\\platforms\\android-24\\arch-arm\\usr\\lib)
#include_directories(C:\\Users\\liujieqin\\AppData\\Local\\Android\\Sdk\\ndk\\21.3.6528147\\platforms\\android-24\\arch-x86\\usr\\include)
#SET(libsqlite3 /system/lib/libsqlite.so)
#SET(libsqlite3 C:\\Users\\liujieqin\\AppData\\Local\\Android\\Sdk\\ndk\\21.3.6528147\\platforms\\android-24\\arch-x86\\usr\\lib\\libsqlite3.so)
# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        native-lib

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        native-lib.cpp
        Scan.cpp
        sqlite3.c
        )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.


#C:\Users\liujieqin\AndroidStudioProjects\JNI\app\src\main\libs

#add_library(libmedia
#        SHARED
#        ${CMAKE_CURRENT_SOURCE_DIR}/libmedia.so)
#set_target_properties(libmedia
#        PROPERTIES IMPORTED_LOCATION
#        libmedia.so)

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)
# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.


#include_directories(libs/include)

target_link_libraries( # Specifies the target library.
        native-lib
#        libmedia
        # Links the target library to the log library
        # included in the NDK.
        ${log-lib}
        )
