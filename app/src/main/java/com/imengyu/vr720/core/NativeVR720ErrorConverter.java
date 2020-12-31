package com.imengyu.vr720.core;

import android.content.Context;

import com.imengyu.vr720.R;

public class NativeVR720ErrorConverter {

    public static final int VR_ERR_UN_KNOW = 0;
    public static final int VR_ERR_SUCCESS = 1;
    public static final int VR_ERR_FILE_NOT_EXISTS = 2;
    public static final int VR_ERR_FILE_NOT_SUPPORT = 3;
    public static final int VR_ERR_IMAGE_TOO_BIG = 4;
    public static final int VR_ERR_BIG_IMAGE_AND_NOT_JPG = 5;
    public static final int VR_ERR_BAD_IMAGE = 6;
    
    public static final int VR_ERR_VIDEO_PLAYER_AV_ERROR = 30;
    public static final int VR_ERR_VIDEO_PLAYER_NO_VIDEO_STREAM = 31;
    public static final int VR_ERR_VIDEO_PLAYER_VIDEO_NOT_SUPPORT = 32;
    
    public static String getStringError(Context context, int err) {
        switch (err) {
            default:
            case VR_ERR_UN_KNOW: return context.getString(R.string.text_error_un_know);
            case VR_ERR_SUCCESS: return context.getString(R.string.text_success);
            case VR_ERR_FILE_NOT_EXISTS: return context.getString(R.string.text_error_file_not_exists);
            case VR_ERR_FILE_NOT_SUPPORT: return context.getString(R.string.text_error_file_not_support);
            case VR_ERR_IMAGE_TOO_BIG: return context.getString(R.string.text_error_image_too_big);
            case VR_ERR_BIG_IMAGE_AND_NOT_JPG: return context.getString(R.string.text_error_big_image_and_not_jpg);
            case VR_ERR_BAD_IMAGE: return context.getString(R.string.text_error_bad_image);
            case VR_ERR_VIDEO_PLAYER_AV_ERROR: return context.getString(R.string.text_error_video_player_av_error);
            case VR_ERR_VIDEO_PLAYER_NO_VIDEO_STREAM: return context.getString(R.string.text_error_video_player_no_video_stream);
            case VR_ERR_VIDEO_PLAYER_VIDEO_NOT_SUPPORT: return context.getString(R.string.text_error_video_player_video_not_support);
        }

    }
}
