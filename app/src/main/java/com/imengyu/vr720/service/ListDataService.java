package com.imengyu.vr720.service;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.imengyu.vr720.model.GalleryItem;
import com.imengyu.vr720.model.ImageItem;
import com.imengyu.vr720.utils.DatabaseHelper;

import java.util.ArrayList;

public class ListDataService {

    private Context context;

    public ListDataService(Context context) {
        this.context = context;
    }

    private DatabaseHelper databaseHelper;
    private final ArrayList<ImageItem> imageList = new ArrayList<>();
    private final ArrayList<GalleryItem> galleryList = new ArrayList<>();

    public void loadList() {
        databaseHelper = new DatabaseHelper(this.context);

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        //Load image list
        Cursor cursor = db.rawQuery("select * from image_list", null);
        while (cursor.moveToNext()) {
            imageList.add(new ImageItem(
                    cursor.getString(1),
                    cursor.getString(2)
            ));
        }
        cursor.close();

        //Load gallery list
        cursor = db.rawQuery("select * from gallery_list", null);
        while (cursor.moveToNext()) {
            GalleryItem li = new GalleryItem();
            li.id = cursor.getInt(0);
            li.name = cursor.getString(1);
            galleryList.add(li);
        }
        cursor.close();

        db.close();
    }
    public void saveList() {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        db.execSQL("delete from 'image_list'; ");
        db.execSQL("delete from 'gallery_list'; ");

        //Save image list
        for(ImageItem i : imageList) {
            String sql = "insert into image_list(path,belong_galleries) values('" +
                    i.path + "','" + i.getBelongGalleries() + "')";
            db.execSQL(sql);
        }
        //Save gallery list
        for(GalleryItem i : galleryList) {
            String sql = "insert into gallery_list(name) values('" + i.name + "')";
            db.execSQL(sql);
        }

        db.close();
        databaseHelper.close();
    }

    public void addImageItem(String path) {
        imageList.add(new ImageItem(path, ""));
    }
    public void clearImageItems() {
        imageList.clear();
    }
    public void removeImageItem(String path) {
        for(int i = imageList.size() - 1; i>=0;i--)
            if(imageList.get(i).path.equals(path))
                imageList.remove(i);
    }
    public ArrayList<ImageItem> getImageList() {
        return imageList;
    }
    public ArrayList<GalleryItem> getGalleryList() {
        return galleryList;
    }
}
