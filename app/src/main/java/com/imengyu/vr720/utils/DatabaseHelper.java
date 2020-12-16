package com.imengyu.vr720.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    //类没有实例化,是不能用作父类构造器的参数,必须声明为静态
    private static final String name = "vr720"; //数据库名称
    private static final int version = 1; //数据库版本

    public DatabaseHelper(Context context) {
        //第三个参数CursorFactory指定在执行查询时获得一个游标实例的工厂类,设置为null,代表使用系统默认的工厂类
        super(context, name, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table if not exists image_list(_id integer primary key autoincrement," +
                "path text not null,belong_galleries text not null)");
        db.execSQL("create table if not exists gallery_list(_id integer primary key autoincrement," +
                "name text not null,gallery_id integer,sort_order integer,create_time text)");
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
