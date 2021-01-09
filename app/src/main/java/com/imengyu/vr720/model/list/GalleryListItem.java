package com.imengyu.vr720.model.list;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.imengyu.vr720.R;
import com.imengyu.vr720.model.GalleryItem;
import com.imengyu.vr720.service.ListDataService;

public class GalleryListItem extends CheckableListItem {

    public GalleryListItem() {

    }
    public GalleryListItem(GalleryItem item) {
        setId(item.id);
        setCreateDate(Long.parseLong(item.createTime));
        setSortOrder(item.sortOrder);
        setName(item.name);
        setCheckable(id > 0);
    }

    public GalleryItem toGalleryItem() {
        GalleryItem item = new GalleryItem();
        item.id = id;
        item.name = name;
        item.createTime = String.valueOf(createDate);
        item.sortOrder = sortOrder;
        return item;
    }

    public int id;
    public String name;
    public int sortOrder;
    public long createDate;

    private int imageCount;
    private int videoCount;
    private String thumbnailPath;
    private Drawable thumbnail;
    private boolean thumbnailLoading;
    private boolean thumbnailLoadingStarted;
    private boolean thumbnailFail;
    private Object data;
    private boolean withSubTitleText = true;

    public boolean isWithSubTitleText() {
        return withSubTitleText;
    }
    public void setWithSubTitleText(boolean withSubTitleText) {
        this.withSubTitleText = withSubTitleText;
    }
    public String getSubTitle(Context context) {
        StringBuilder sb = new StringBuilder();
        if(imageCount > 0) {
            sb.append(imageCount);
            if(withSubTitleText) {
                sb.append(" ");
                sb.append(context.getString(R.string.text_count_image));
            }
        }
        if(videoCount > 0) {
            sb.append(videoCount);
            sb.append(" ");
            sb.append(context.getString(R.string.text_count_video));
        }
        if(imageCount == 0 && videoCount == 0)
            sb.append(context.getString(R.string.text_no_image));
        return sb.toString();
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getSortOrder() {
        return sortOrder;
    }
    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
    public long getCreateDate() {
        return createDate;
    }
    public void setCreateDate(long createDate) {
        this.createDate = createDate;
    }

    public Object getData() {
        return data;
    }
    public void setData(Object data) {
        this.data = data;
    }

    public Drawable getThumbnail() {
        return thumbnail;
    }
    public void setThumbnail(Drawable thumbnail) {
        this.thumbnail = thumbnail;
    }
    public boolean isThumbnailLoading() {
        return thumbnailLoading;
    }
    public void setThumbnailLoading(boolean thumbnailLoading) {
        this.thumbnailLoading = thumbnailLoading;
    }
    public boolean isThumbnailLoadingStarted() {
        return thumbnailLoadingStarted;
    }
    public void setThumbnailLoadingStarted(boolean thumbnailLoadingStarted) {
        this.thumbnailLoadingStarted = thumbnailLoadingStarted;
    }
    public boolean isThumbnailFail() {
        return thumbnailFail;
    }
    public void setThumbnailFail(boolean thumbnailFail) {
        this.thumbnailFail = thumbnailFail;
    }
    public String getThumbnailPath() {
        return thumbnailPath;
    }
    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }
    public int getImageCount() {
        return imageCount;
    }
    public void setImageCount(int imageCount) {
        this.imageCount = imageCount;
    }
    public int getVideoCount() {
        return videoCount;
    }
    public void setVideoCount(int videoCount) {
        this.videoCount = videoCount;
    }

    public void refresh(ListDataService listDataService) {
        setImageCount(listDataService.getGalleryImageCount(getId()));
        setVideoCount(listDataService.getGalleryVideoCount(getId()));
        setThumbnailFail(false);
        setThumbnailLoading(false);
        setThumbnailLoadingStarted(false);
        setThumbnailPath(listDataService.getGalleryFirstImagePath(getId()));
    }
}
