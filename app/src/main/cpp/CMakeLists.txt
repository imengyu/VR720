# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

#扫描所有c文件

AUX_SOURCE_DIRECTORY(. DIR_SRCS)

SET(PROJECT_LIB_DIR ${CMAKE_SOURCE_DIR}/../jniLibs)
SET(DIR_SRCS VR720.cpp VR720Renderer.cpp)

FILE(GLOB_RECURSE TEMP_DIR_SRCS LIST_DIRECTORIES false RELATIVE ${CMAKE_SOURCE_DIR}
        "*.h"
        "*.cpp"
        "*.c")

# 包含目录
include_directories(
        .
        libjpeg
        libpng
        glm
        ffmpeg
)

message(DEBUG ${DIR_SRCS})

add_library( # Sets the name of the library.
             vr720
             # Sets the library as a shared library.
             SHARED
             # Provides a relative path to your source file(s).
             ${DIR_SRCS} ${TEMP_DIR_SRCS})

add_library(avfilter SHARED IMPORTED)
add_library(avcodec SHARED IMPORTED)
add_library(avformat SHARED IMPORTED)
add_library(avutil SHARED IMPORTED)
add_library(swscale SHARED IMPORTED)
add_library(avdevice SHARED IMPORTED)
add_library(postproc SHARED IMPORTED)
add_library(swresample SHARED IMPORTED)

#设置引进目标属性
set_target_properties(avcodec PROPERTIES IMPORTED_LOCATION  ${PROJECT_LIB_DIR}/${ANDROID_ABI}/libavcodec.so)
set_target_properties(avfilter PROPERTIES IMPORTED_LOCATION  ${PROJECT_LIB_DIR}/${ANDROID_ABI}/libavfilter.so)
set_target_properties(avformat PROPERTIES IMPORTED_LOCATION  ${PROJECT_LIB_DIR}/${ANDROID_ABI}/libavformat.so)
set_target_properties(avutil PROPERTIES IMPORTED_LOCATION  ${PROJECT_LIB_DIR}/${ANDROID_ABI}/libavutil.so)
set_target_properties(swscale PROPERTIES IMPORTED_LOCATION  ${PROJECT_LIB_DIR}/${ANDROID_ABI}/libswscale.so)
set_target_properties(avdevice PROPERTIES IMPORTED_LOCATION  ${PROJECT_LIB_DIR}/${ANDROID_ABI}/libavdevice.so)
set_target_properties(postproc PROPERTIES IMPORTED_LOCATION  ${PROJECT_LIB_DIR}/${ANDROID_ABI}/libpostproc.so)
set_target_properties(swresample PROPERTIES IMPORTED_LOCATION  ${PROJECT_LIB_DIR}/${ANDROID_ABI}/libswresample.so)


find_library(android-lib android)
find_library(GLESv3-lib GLESv3)
find_library(log-lib log)
find_library(z-lib z)

target_link_libraries(vr720
        ${log-lib}
        ${android-lib}
        ${GLESv3-lib}
        ${z-lib}
        avcodec
        avfilter
        avformat
        avutil
        swscale
        avdevice
        postproc
        swresample)
