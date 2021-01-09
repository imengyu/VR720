package com.imengyu.vr720.utils;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.imengyu.vr720.BuildConfig;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Date;

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "CrashHandler";

    private static String PATH = "";
    private static final String FILE_NAME = "crash";

    //log文件的后缀名
    private static final String FILE_NAME_SUFFIX = ".trace.txt";

    private static CrashHandler sInstance = null;

    //系统默认的异常处理（默认情况下，系统会终止当前的异常程序）
    private Thread.UncaughtExceptionHandler mDefaultCrashHandler;

    //构造方法私有，防止外部构造多个实例，即采用单例模式
    private CrashHandler() {
    }

    public static CrashHandler getInstance() {
        if(sInstance == null)
            sInstance = new CrashHandler();
        return sInstance;
    }

    //这里主要完成初始化工作
    public void init(Context context, boolean enable) {
        //获取系统默认的异常处理器
        mDefaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler();
        if(enable) {
            //将当前实例设为系统默认的异常处理器
            Thread.setDefaultUncaughtExceptionHandler(this);
        }
        //获取路径
        PATH = context.getCacheDir().getAbsolutePath() + "/errorLogs";
    }

    /**
     * 这个是最关键的函数，当程序中有未被捕获的异常，系统将会自动调用#uncaughtException方法
     * thread为出现未捕获异常的线程，ex为未捕获的异常，有了这个ex，我们就可以得到异常信息。
     */
    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable ex) {
        //导出异常信息到SD卡中
        dumpExceptionToSDCard(ex);

        //打印出当前调用栈信息
        ex.printStackTrace();

        //如果系统提供了默认的异常处理器，则交给系统去结束我们的程序，否则就由我们自己结束自己
        if (mDefaultCrashHandler != null) {
            mDefaultCrashHandler.uncaughtException(thread, ex);
        } else {
            System.exit(-1);
        }

    }

    private void dumpExceptionToSDCard(Throwable ex) {

        File dir = new File(PATH);
        if (!dir.exists() && !dir.mkdirs())
            Log.w(TAG, "Make dirs for " + PATH + " failed! ");

        long current = System.currentTimeMillis();
        String time = DateUtils.format(new Date(current), DateUtils.FORMAT_FULL);

        //以当前时间创建log文件
        File file = new File(PATH + FILE_NAME + time + FILE_NAME_SUFFIX);
        try {
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            //导出发生异常的时间
            pw.println(time);
            //导出手机信息
            dumpPhoneInfo(pw);

            pw.println();
            //导出异常的调用栈信息
            ex.printStackTrace(pw);
            pw.close();
        } catch (Exception e) {
            Log.e(TAG, "Dump crash info failed");
        }
    }
    private void dumpPhoneInfo(PrintWriter pw) {

        //应用的版本名称和版本号
        pw.print("App Version: ");
        pw.print(BuildConfig.APPLICATION_ID);
        pw.print('_');
        pw.print(BuildConfig.VERSION_CODE);
        pw.print('_');
        pw.println(BuildConfig.VERSION_NAME);

        //android版本号
        pw.print("OS Version: ");
        pw.print(Build.VERSION.RELEASE);
        pw.print("_");
        pw.println(Build.VERSION.SDK_INT);

        //手机制造商
        pw.print("Vendor: ");
        pw.println(Build.MANUFACTURER);

        //手机型号
        pw.print("Model: ");
        pw.println(Build.MODEL);

        //cpu架构
        pw.print("CPU ABI: ");
        for(String abi : Build.SUPPORTED_ABIS)
            pw.println(abi);
    }
}
