package com.imengyu.vr720.service;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.imengyu.vr720.R;
import com.imengyu.vr720.model.GalleryItem;
import com.imengyu.vr720.model.ImageItem;
import com.imengyu.vr720.utils.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;

public class ListDataService {

    private final Context context;

    public ListDataService(Context context) {
        this.context = context;
    }

    private DatabaseHelper databaseHelper;
    private final ArrayList<ImageItem> imageList = new ArrayList<>();
    private final ArrayList<GalleryItem> galleryList = new ArrayList<>();
    private int galleryListMinId = 0;

    public int getGalleryListMinId() {
        return ++galleryListMinId;
    }

    public void loadList() {
        databaseHelper = new DatabaseHelper(this.context);

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        imageList.clear();
        galleryList.clear();

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
            li.name = cursor.getString(1);
            li.id = cursor.getInt(2);
            li.sortOrder = cursor.getInt(3);
            li.createTime = cursor.getString(4);

            galleryList.add(li);

            if(li.id >= galleryListMinId)
                galleryListMinId = li.id + 1;
        }
        cursor.close();

        db.close();

        //add default gallery list
        if(galleryList.size() == 0) {

            GalleryItem li = new GalleryItem();
            li.name = context.getString(R.string.text_i_like);
            li.id = GALLERY_LIST_ID_I_LIKE;
            li.sortOrder = 0;
            li.createTime = "0";

            galleryList.add(li);

            /*
            li = new GalleryItem();
            li.name = context.getString(R.string.text_videos);
            li.id = GALLERY_LIST_ID_VIDEOS;
            li.sortOrder = 0;
            li.createTime = "0";

            galleryList.add(li);
            */
        }
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
            String sql = "insert into gallery_list(name,gallery_id,sort_order,create_time) values('" +
                    i.name + "'," + i.id + "," + i.sortOrder + ",'" + i.createTime + "')";
            db.execSQL(sql);
        }

        db.close();
        databaseHelper.close();
    }

    public static final int GALLERY_LIST_ID_ADD = 0;
    public static final int GALLERY_LIST_ID_I_LIKE = -1;
    public static final int GALLERY_LIST_ID_VIDEOS = -2;

    public ImageItem addImageItem(String path) {
        return addImageItem(path, 0);
    }
    public ImageItem addImageItem(String path, int belongGallery) {
        ImageItem item = findImageItem(path);
        if(item != null) {
            if(belongGallery != 0 && !item.isInBelongGalleries(belongGallery))
                item.belongGalleries.add(belongGallery);
            return item;
        }
        item = new ImageItem(path, belongGallery != 0 ? String.valueOf(belongGallery) : "");
        imageList.add(item);
        return item;
    }
    public ImageItem findImageItem(String path) {
        for(int i = imageList.size() - 1; i>=0;i--)
            if(imageList.get(i).path.equals(path))
                return imageList.get(i);
            return null;
    }
    public void clearImageItems() {
        imageList.clear();
    }
    public void removeImageItem(String path) {
        for(int i = imageList.size() - 1; i>=0;i--)
            if(imageList.get(i).path.equals(path))
                imageList.remove(i);
    }
    public void removeImageItem(ImageItem imageItem) {
        imageList.remove(imageItem);
    }
    public ArrayList<ImageItem> getImageList() {
        return imageList;
    }
    public ArrayList<GalleryItem> getGalleryList() {
        return galleryList;
    }

    public GalleryItem getGalleryItem(int galleryId) {
        GalleryItem item = null;
        for(int i = galleryList.size() - 1; i>=0;i--) {
            item = galleryList.get(i);
            if (item.id == galleryId)
                return item;
        }
        return item;
    }
    public String getGalleryFirstImagePath(int galleryId) {
        for (ImageItem imageItem : imageList) {
            if(imageItem.isInBelongGalleries(galleryId))
                return imageItem.path;
        }
        return null;
    }
    public int getGalleryImageCount(int galleryId) {
        int count = 0;
        for (ImageItem imageItem : imageList) {
            if(!imageItem.isVideo && imageItem.isInBelongGalleries(galleryId))
                count++;
        }
        return count;
    }
    public int getGalleryVideoCount(int galleryId) {
        int count = 0;
        for (ImageItem imageItem : imageList) {
            if(imageItem.isVideo && imageItem.isInBelongGalleries(galleryId))
                count++;
        }
        return count;
    }

    public List<ImageItem> collectGalleryItems(int id) {
        List<ImageItem> list = new ArrayList<>();
        for(ImageItem imageItem : imageList) {
            if(imageItem.isInBelongGalleries(id))
                list.add(imageItem);
        }
        return list;
    }
    public void addGalleryItem(GalleryItem item) {
        galleryList.add(item);
    }
    public void renameGalleryItem(int id, String newName) {
        GalleryItem item = null;
        for(int i = galleryList.size() - 1; i>=0;i--) {
            item = galleryList.get(i);
            if (item.id == id) {
                item.name = newName;
                break;
            }
        }
    }
    public void removeGalleryItem(int id) {
        for(int i = galleryList.size() - 1; i>=0;i--)
            if(galleryList.get(i).id == id) {
                galleryList.remove(i);
                break;
            }
    }
}
