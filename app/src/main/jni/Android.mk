LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := vr720

MY_CPP_LIST := $(wildcard $(LOCAL_PATH)/*.cpp)
MY_CPP_LIST += $(wildcard $(LOCAL_PATH)/libpng/*.c)
MY_CPP_LIST += $(wildcard $(LOCAL_PATH)/libjpeg/*.c)
MY_CPP_LIST += $(wildcard $(LOCAL_PATH)/imageloaders/*.cpp)
MY_CPP_LIST := $(wildcard $(LOCAL_PATH)/core/*.cpp)

LOCAL_SRC_FILES := $(MY_CPP_LIST:$(LOCAL_PATH)/%=%)
include $(BUILD_SHARED_LIBRARY)