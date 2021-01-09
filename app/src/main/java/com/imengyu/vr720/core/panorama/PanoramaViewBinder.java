package com.imengyu.vr720.core.panorama;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import com.imengyu.vr720.activity.PanoActivity;
import com.imengyu.vr720.R;
import com.imengyu.vr720.core.natives.NativeVR720GLSurfaceView;
import com.imengyu.vr720.core.natives.NativeVR720Renderer;
import com.imengyu.vr720.widget.MyTitleBar;
import com.imengyu.vr720.widget.ToolbarButton;

public class PanoramaViewBinder {
    
    private final PanoActivity activity;
    
    public PanoramaViewBinder(PanoActivity activity) {
        this.activity = activity;
        initResources();
        initAnimations();
        initView();
    }
    
    private void initView() {
        titlebar = activity.findViewById(R.id.titlebar);
        activity_pano = activity.findViewById(R.id.activity_pano);
        layout_video_control = activity.findViewById(R.id.layout_video_control);
        layout_debug = activity.findViewById(R.id.layout_debug);
        pano_error = activity.findViewById(R.id.pano_error_view);
        pano_loading = activity.findViewById(R.id.pano_loading_view);
        pano_menu = activity.findViewById(R.id.pano_menu);
        pano_info = activity.findViewById(R.id.pano_info);
        pano_tools = activity.findViewById(R.id.pano_tools);
        text_debug = activity.findViewById(R.id.text_debug);
        text_sensor_cant_use_in_mode = activity.findViewById(R.id.text_sensor_cant_use_in_mode);
        text_pano_error = activity.findViewById(R.id.text_pano_error);
        text_pano_info_size = activity.findViewById(R.id.text_pano_info_size);
        text_pano_info_file_size = activity.findViewById(R.id.text_pano_info_file_size);
        text_pano_info_file_path = activity.findViewById(R.id.text_pano_info_file_path);
        text_pano_info_image_time = activity.findViewById(R.id.text_pano_info_image_time);
        text_pano_info_shutter_time = activity.findViewById(R.id.text_pano_info_shutter_time);
        text_pano_info_exposure_bias_value = activity.findViewById(R.id.text_pano_info_exposure_bias_value);
        text_pano_info_iso_sensitivit = activity.findViewById(R.id.text_pano_info_iso_sensitivit);
        text_pano_error = activity.findViewById(R.id.text_pano_error);
        text_video_length = activity.findViewById(R.id.text_video_length);
        text_video_current_pos = activity.findViewById(R.id.text_video_current_pos);
        seek_x = activity.findViewById(R.id.seek_x);
        seek_y = activity.findViewById(R.id.seek_y);
        seek_z = activity.findViewById(R.id.seek_z);
        seek_video = activity.findViewById(R.id.seek_video);
        button_video_play_pause = activity.findViewById(R.id.button_video_play_pause);
        button_prev = activity.findViewById(R.id.button_prev);
        button_next = activity.findViewById(R.id.button_next);
        button_short = activity.findViewById(R.id.button_short);
        button_more = activity.findViewById(R.id.button_more);
        button_like =  activity.findViewById(R.id.button_like);
        glSurfaceView = activity.findViewById(R.id.pano_view);
        button_mode = activity.findViewById(R.id.button_mode);
        text_not_support_sensor = activity.findViewById(R.id.text_not_support_sensor);
        switch_enable_vr = activity.findViewById(R.id.switch_enable_vr);
        switch_enable_gyro = activity.findViewById(R.id.switch_enable_gyro);
        view_top_bar_placeholder = activity.findViewById(R.id.view_top_bar_placeholder);
        view_bottom_bar_placeholder = activity.findViewById(R.id.view_bottom_bar_placeholder);
    }
    private void initAnimations() {
        bottom_up = AnimationUtils.loadAnimation(activity, R.anim.bottom_up);
        bottom_down = AnimationUtils.loadAnimation(activity, R.anim.bottom_down);
        fade_hide = AnimationUtils.loadAnimation(activity, R.anim.fade_hide);
        fade_show = AnimationUtils.loadAnimation(activity, R.anim.fade_show);
        top_down = AnimationUtils.loadAnimation(activity, R.anim.top_down);
        top_up = AnimationUtils.loadAnimation(activity, R.anim.top_up);
        right_to_left_in = AnimationUtils.loadAnimation(activity, R.anim.right_to_left_in);
        left_to_right_out = AnimationUtils.loadAnimation(activity, R.anim.left_to_right_out);
    }
    private void initResources() {
        //加载图标
        Resources resources = activity.getResources();
        iconModeBall = ResourcesCompat.getDrawable(resources, R.drawable.ic_bar_projection_ball, null);
        iconModeLittlePlanet = ResourcesCompat.getDrawable(resources, R.drawable.ic_bar_projection_little_planet, null);
        iconModeRectiliner = ResourcesCompat.getDrawable(resources, R.drawable.ic_bar_projection_rectilinear, null);
        iconModeSource = ResourcesCompat.getDrawable(resources, R.drawable.ic_bar_projection_source, null);
        iconModeVideoBall = ResourcesCompat.getDrawable(resources, R.drawable.ic_bar_projection_video_ball, null);
        iconModeMercator = ResourcesCompat.getDrawable(resources, R.drawable.ic_bar_projection_mercator, null);

        iconModeBall.setBounds(0,0, iconModeBall.getMinimumWidth(), iconModeBall.getMinimumHeight());
        iconModeLittlePlanet.setBounds(0,0, iconModeLittlePlanet.getMinimumWidth(), iconModeLittlePlanet.getMinimumHeight());
        iconModeRectiliner.setBounds(0,0, iconModeRectiliner.getMinimumWidth(), iconModeRectiliner.getMinimumHeight());
        iconModeSource.setBounds(0,0, iconModeSource.getMinimumWidth(), iconModeSource.getMinimumHeight());
        iconModeVideoBall.setBounds(0,0,iconModeVideoBall.getMinimumWidth(), iconModeVideoBall.getMinimumHeight());
        iconModeMercator.setBounds(0,0, iconModeMercator.getMinimumWidth(), iconModeMercator.getMinimumHeight());

        ic_play = ResourcesCompat.getDrawable(resources, R.drawable.ic_play, null);
        ic_pause = ResourcesCompat.getDrawable(resources, R.drawable.ic_pause, null);
        ic_play.setBounds(0,0,ic_play.getMinimumWidth(), ic_play.getMinimumHeight());
        ic_pause.setBounds(0,0, ic_pause.getMinimumWidth(), ic_pause.getMinimumHeight());

        likeColorStateList = ColorStateList.valueOf(resources.getColor(R.color.colorImageLike, null));
    }
    
    public View pano_error;
    public View pano_loading;
    public View pano_menu;
    public View pano_info;
    public View pano_tools;
    public TextView text_pano_error;
    public TextView text_pano_info_size;
    public TextView text_pano_info_file_size;
    public TextView text_pano_info_file_path;
    public TextView text_pano_info_image_time;
    public TextView text_pano_info_shutter_time;
    public TextView text_pano_info_exposure_bias_value;
    public TextView text_pano_info_iso_sensitivit;
    public TextView text_sensor_cant_use_in_mode;
    public TextView text_debug;
    public TextView text_video_current_pos;
    public TextView text_video_length;
    public View layout_debug;
    public View layout_video_control;
    public ImageButton button_prev;
    public ImageButton button_next;
    public Button button_video_play_pause;
    public SeekBar seek_video;
    public SeekBar seek_x;
    public SeekBar seek_y;
    public SeekBar seek_z;
    public MyTitleBar titlebar;
    public NativeVR720GLSurfaceView glSurfaceView;
    public ToolbarButton button_like;
    public ToolbarButton button_short;
    public ToolbarButton button_mode;
    public ToolbarButton button_more;
    public ConstraintLayout activity_pano;
    public View view_top_bar_placeholder;
    public View view_bottom_bar_placeholder;

    public Drawable iconModeBall;
    public Drawable iconModeLittlePlanet;
    public Drawable iconModeRectiliner;
    public Drawable iconModeSource;
    public Drawable iconModeMercator;
    public Drawable iconModeVideoBall;
    
    public TextView text_not_support_sensor ;
    public SwitchCompat switch_enable_vr;
    public SwitchCompat switch_enable_gyro;

    public Drawable ic_play;
    public Drawable ic_pause;

    public Animation bottom_up, bottom_down;
    public Animation fade_hide, fade_show;
    public Animation top_down, top_up;
    public Animation left_to_right_out, right_to_left_in;
    public ColorStateList likeColorStateList;
    
    public void updateDebugValue(StringBuilder debugString) {
        text_debug.setText(debugString.toString());
        debugString.delete(0, debugString.length());
    }
    public void updateModeButton(boolean gyroEnabled, int currentPanoMode) {
        switch (currentPanoMode) {
            case NativeVR720Renderer.PanoramaMode_PanoramaSphere:
                button_mode.setText(R.string.text_mode_ball);
                button_mode.setCompoundDrawables(null, iconModeBall, null, null);
                break;
            case NativeVR720Renderer.PanoramaMode_PanoramaCylinder:
                button_mode.setText(R.string.text_mode_rectilinear);
                button_mode.setCompoundDrawables(null, iconModeRectiliner, null, null);
                break;
            case NativeVR720Renderer.PanoramaMode_PanoramaAsteroid:
                button_mode.setText(R.string.text_mode_little_planet);
                button_mode.setCompoundDrawables(null, iconModeLittlePlanet, null, null);
                break;
            case NativeVR720Renderer.PanoramaMode_PanoramaOuterBall:
                button_mode.setText(R.string.text_mode_video_ball);
                button_mode.setCompoundDrawables(null, iconModeVideoBall, null, null);
                break;
            case NativeVR720Renderer.PanoramaMode_PanoramaFull360:
                button_mode.setText(R.string.text_mode_360_pano);
                button_mode.setCompoundDrawables(null, iconModeRectiliner, null, null);
                break;
            case NativeVR720Renderer.PanoramaMode_PanoramaFullOrginal:
                button_mode.setText(R.string.text_mode_source);
                button_mode.setCompoundDrawables(null, iconModeSource, null, null);
                break;
            case NativeVR720Renderer.PanoramaMode_PanoramaMercator:
                button_mode.setText(R.string.text_mode_mercator);
                button_mode.setCompoundDrawables(null, iconModeMercator, null, null);
                break;
        }
        updateModeText(gyroEnabled, currentPanoMode);
    }
    public void updateModeText(boolean gyroEnabled, int currentPanoMode) {
        if(gyroEnabled) {
            switch (currentPanoMode) {
                case NativeVR720Renderer.PanoramaMode_PanoramaSphere:
                case NativeVR720Renderer.PanoramaMode_PanoramaOuterBall:
                case NativeVR720Renderer.PanoramaMode_PanoramaCylinder:
                case NativeVR720Renderer.PanoramaMode_PanoramaAsteroid:
                case NativeVR720Renderer.PanoramaMode_PanoramaMercator:
                    text_sensor_cant_use_in_mode.setVisibility(View.GONE);
                    break;
                case NativeVR720Renderer.PanoramaMode_PanoramaFull360:
                case NativeVR720Renderer.PanoramaMode_PanoramaFullOrginal:
                    text_sensor_cant_use_in_mode.setVisibility(View.VISIBLE);
                    break;
            }
        }else {
            text_sensor_cant_use_in_mode.setVisibility(View.GONE);
        }
    }
}
