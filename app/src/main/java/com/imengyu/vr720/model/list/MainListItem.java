package com.imengyu.vr720.model.list;

import android.graphics.drawable.Drawable;

import com.imengyu.vr720.model.ImageItem;
import com.imengyu.vr720.utils.FileSizeUtil;
import com.imengyu.vr720.utils.FileUtils;

/**
 * 主列表数据
 */
public class MainListItem extends CheckableListItem {

    public static final int ITEM_TYPE_NORMAL = 0;
    public static final int ITEM_TYPE_TEXT = 1;

    private ImageItem imageItem;

    private String filePath;
    private String fileName;
    private String fileSize;
    private String fileModifyDate;
    private Drawable thumbnail;
    private boolean thumbnailLoading;
    private boolean thumbnailLoadingStarted;
    private boolean thumbnailFail;
    private boolean isVideo;
    private long fileSizeValue;
    private long fileModifyDateValue;
    private int forceItemType;
    private boolean isSearchHidden = false;

    public MainListItem(String itemText){
        this.forceItemType = ITEM_TYPE_TEXT;
        this.fileName = itemText;
    }
    public MainListItem(ImageItem imageItem) {
        this.filePath = imageItem.path;
        this.fileName = FileUtils.getFileName(imageItem.path);
        this.fileSize = FileSizeUtil.getAutoFileOrFilesSize(imageItem.path);
        this.imageItem = imageItem;
        this.forceItemType = ITEM_TYPE_NORMAL;
        this.isVideo = imageItem.isVideo;
    }

    public Drawable getThumbnail() {
        return thumbnail;
    }
    public void setThumbnail(Drawable thumbnail) {
        this.thumbnail = thumbnail;
    }
    public String getFilePath() {
        return filePath;
    }
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    public String getFileName() {
        return fileName;
    }
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    public String getFileSize() {
        return fileSize;
    }
    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }
    public boolean isThumbnailLoading() {
        return thumbnailLoading;
    }
    public void setThumbnailLoading(boolean thumbnailLoading) {
        this.thumbnailLoading = thumbnailLoading;
    }
    public boolean isThumbnailFail() {
        return thumbnailFail;
    }
    public void setThumbnailFail(boolean thumbnailFail) {
        this.thumbnailFail = thumbnailFail;
    }
    public long getFileSizeValue() {
        return fileSizeValue;
    }
    public void setFileSizeValue(long fileSizeValue) {
        this.fileSizeValue = fileSizeValue;
    }
    public String getFileModifyDate() {
        return fileModifyDate;
    }
    public void setFileModifyDate(String fileModifyDate) {
        this.fileModifyDate = fileModifyDate;
    }
    public long getFileModifyDateValue() {
        return fileModifyDateValue;
    }
    public void setFileModifyDateValue(long fileModifyDateValue) {
        this.fileModifyDateValue = fileModifyDateValue;
    }
    public boolean isThumbnailLoadingStarted() {
        return thumbnailLoadingStarted;
    }
    public void setThumbnailLoadingStarted(boolean thumbnailLoadingStarted) {
        this.thumbnailLoadingStarted = thumbnailLoadingStarted;
    }
    public int getForceItemType() {
        return forceItemType;
    }
    public void setForceItemType(int forceItemType) {
        this.forceItemType = forceItemType;
    }
    public ImageItem getImageItem() {
        return imageItem;
    }
    public boolean isVideo() {
        return isVideo;
    }
    public void setVideo(boolean video) {
        isVideo = video;
    }
    public boolean isSearchHidden() {
        return isSearchHidden;
    }
    public void setSearchHidden(boolean searchHidden) {
        isSearchHidden = searchHidden;
    }
}
