LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := vr720
LOCAL_LDLIBS := -llog -landroid -lGLESv3 -lz
LOCAL_CPP_FEATURES += exceptions

LOCAL_C_INCLUDES := $(LOCAL_PATH)/libjpeg/ \
                    $(LOCAL_PATH)/libpng/ \
                    $(LOCAL_PATH)/core/ \
                    $(LOCAL_PATH)/imageloaders/ \
                    $(LOCAL_PATH)/zlib/ \
                    $(LOCAL_PATH)/utils/ \
                    $(LOCAL_PATH)/glm/

LIB_JPEG_FILES := $(wildcard $(LOCAL_PATH)/libjpeg/*.c)
LIB_PNG_FILES := $(wildcard $(LOCAL_PATH)/libpng/*.c)
LIB_PNG_FILES += $(wildcard $(LOCAL_PATH)/libpng/arm/*.c)

EXCLUDE_JPEG_FILES := $(LOCAL_PATH)/libjpeg/jstdhuff.c \
                    $(LOCAL_PATH)/libjpeg/jdmrg565.c \
                    $(LOCAL_PATH)/libjpeg/jdcol565.c \
                    $(LOCAL_PATH)/libjpeg/jdmrgext.c \
                    $(LOCAL_PATH)/libjpeg/jdcolext.c \
                    $(LOCAL_PATH)/libjpeg/jccolext.c

MY_CPP_LIST := $(LOCAL_PATH)/vr720.cpp
MY_CPP_LIST += $(wildcard $(LOCAL_PATH)/utils/*.cpp)
MY_CPP_LIST += $(wildcard $(LOCAL_PATH)/imageloaders/*.cpp)
MY_CPP_LIST += $(wildcard $(LOCAL_PATH)/core/*.cpp)
MY_CPP_LIST += $(LOCAL_PATH)/vr720Renderer.cpp

MY_CPP_LIST += $(LIB_PNG_FILES)
MY_CPP_LIST += $(filter-out $(EXCLUDE_JPEG_FILES), $(LIB_JPEG_FILES))

LOCAL_SRC_FILES := $(MY_CPP_LIST:$(LOCAL_PATH)/%=%)
include $(BUILD_SHARED_LIBRARY)