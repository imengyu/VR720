package com.imengyu.vr720.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;


/**
 * 联网工具类，用于连接网络，取得数据
 */

public class HttpUtils {

  /**
   * 简单请求 http 获取JSON
   * @param url URL
   * @return 返回 JSONObject
   */
  public static JSONObject httpGetJson(String url) throws IOException {
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder().url(url).build();

    Response response = client.newCall(request).execute();//发送请求
    ResponseBody responseBody = response.body();
    if(responseBody == null)
      return null;
    String result = responseBody.string();
    return JSON.parseObject(result);
  }
}
