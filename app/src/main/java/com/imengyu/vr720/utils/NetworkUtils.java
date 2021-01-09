package com.imengyu.vr720.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

public class NetworkUtils {

    private static boolean networkWifi = false;

    public static boolean isNetworkWifi() { return networkWifi; }
    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (context != null) {
            Network network =cm.getActiveNetwork();
            if(network!=null){
                NetworkCapabilities nc=cm.getNetworkCapabilities(network);
                if(nc!=null){
                    if(nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){//WIFI
                        networkWifi = true;
                        return true;
                    } else if(nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)){//移动数据
                        networkWifi = false;
                        return true;
                    }
                }
            }

        }
        return false;
    }

}
