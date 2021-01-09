package com.imengyu.vr720.config;

import com.imengyu.vr720.BuildConfig;

public class Constants {
    public static final String FILE_PROVIDER_NAME = "com.imengyu.vr720.fileProvider";

    public static final String FEED_BACK_URL = "https://imengyu.top/services/commonFeedback/app/" + BuildConfig.APPLICATION_ID;
    public static final String ERROR_FEED_BACK_URL = "https://imengyu.top/services/commonFeedback/app-error-report/" + BuildConfig.APPLICATION_ID;

    public static final String HELP_TRANSLATE_URL = "https://imengyu.top/linkJump?to=vr720_help_us_translate";
    public static final String ARGEEMENT_URL = "https://imengyu.top/linkJump?to=vr720_argeement";
    public static final String PRIVACY_POLICY_URL = "https://imengyu.top/linkJump?to=vr720_privacy_policy";

    public static final String CHECK_UPDATE_URL = "https://imengyu.top/services/update/?appid=%s&version=%d";

    public static final String LICENSE_PAGE_URL = "file:///android_asset/help/help_license.html";
    public static final String HELP_PAGE_URL = "file:///android_asset/help/help.html";
    public static final String ERROR_PAGE_URL = "file:///android_asset/error.html";
}
