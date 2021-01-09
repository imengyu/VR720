package com.imengyu.vr720.model;

import com.imengyu.vr720.service.ListDataService;

import java.io.Serializable;
import java.util.Date;

public class GalleryItem implements Serializable {
    public String name;
    public int id;
    public String createTime;
    public int sortOrder = 0;

    public static GalleryItem newInstance(ListDataService listDataService, String name) {
        GalleryItem galleryItem = new GalleryItem();
        galleryItem.createTime = String.valueOf(new Date().getTime());
        galleryItem.id = listDataService.getGalleryListMinId();
        galleryItem.name = name;
        return galleryItem;
    }
}
