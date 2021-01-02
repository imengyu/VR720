package com.imengyu.vr720.list;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.ListView;

import com.imengyu.vr720.R;
import com.imengyu.vr720.adapter.GalleryGridAdapter;
import com.imengyu.vr720.adapter.GalleryListAdapter;
import com.imengyu.vr720.config.MainMessages;
import com.imengyu.vr720.model.list.MainListItem;
import com.imengyu.vr720.model.list.MainListItem;
import com.imengyu.vr720.service.ListImageCacheService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GalleryGridList extends SelectableListSolver<MainListItem> {

    private final Resources resources;
    private final Context context;
    private final ListImageCacheService listImageCacheService;
    private Handler handler;

    public Resources getResources() {
        return resources;
    }

    public GalleryGridList(Context context, ListImageCacheService listImageCacheService) {
        this.context = context;
        this.listImageCacheService = listImageCacheService;
        resources = context.getResources();
    }

    private final ArrayList<MainListItem> listItems = new ArrayList<>();
    private GalleryGridAdapter listAdapter = null;

    public void init(Handler handler, GridView gridView) {
        this.handler = handler;

        listAdapter = new GalleryGridAdapter(this, context, R.layout.item_gallery_grid, listItems);

        super.init(listAdapter, listItems);
        super.setListOnNotifyChangeListener(this::notifyChange);

        gridView.setAdapter(listAdapter);
        gridView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
    }

    //====================================================
    //条目操作
    //====================================================

    public ArrayList<CharSequence> getListPathItems() {
        ArrayList<CharSequence> list = new ArrayList<>();
        for (MainListItem li : listItems) {
            String filePath = li.getFilePath();
            if (filePath != null)
                list.add(filePath);
        }
        return list;
    }
    public List<MainListItem> getListItems() {
        return listItems;
    }
    public GalleryGridAdapter getListAdapter() {
        return listAdapter;
    }
    public int getListItemCount() { return listItems.size(); }
    public void addItem(MainListItem item, boolean notify) {
        listItems.add(item);
        if (notify) refresh();
    }
    public void clear() {
        listItems.clear();
        refresh();
    }
    public void deleteItem(MainListItem item) {
        listItems.remove(item);
        refresh();
    }
    public void deleteItems(List<MainListItem> items) {
        listItems.removeAll(items);
        refresh();
    }
    public void notifyChange() {
        handler.sendEmptyMessage(MainMessages.MSG_REFRESH_LIST);
    }
    public void notifyChange(int delay) {
        handler.sendEmptyMessageDelayed(MainMessages.MSG_REFRESH_LIST, delay);
    }
    public void loadItemThumbnail(MainListItem item) {
        //在背景线程进行缩略图加载
        new Thread(() -> {
            Drawable drawable = listImageCacheService.loadImageThumbnailCache(item.getFilePath());
            if(drawable != null) {
                item.setThumbnail(drawable);
                item.setThumbnailLoading(false);
            } else {
                item.setThumbnailLoading(false);
                item.setThumbnailFail(true);
            }
            notifyChange(500);
        }).start();
    }
    /**
     * 查找指定文件路径的条目
     * @param filePath 文件路径
     * @return 返回条目，如果不存在，则返回null
     */
    public MainListItem findImageItem(String filePath) {
        for(MainListItem m : listItems){
            if(filePath.equals(m.getFilePath()))
                return m;
        }
        return null;
    }

    //====================================================
    //条目排序
    //====================================================
    
    public static final int SORT_NAME = 671;
    public static final int SORT_DATE = 672;
    public static final int SORT_SIZE = 683;

    private int sortType = SORT_NAME;
    private boolean sortReverse = false;

    private class ComparatorValues implements Comparator<MainListItem> {

        @Override
        public int compare(MainListItem m1, MainListItem m2) {
            int result = 0;
            if(sortType == SORT_DATE){
                long old1 = m1.getFileModifyDateValue();
                long old2 = m2.getFileModifyDateValue();
                if (old1 > old2) result = 1;
                if (old1 < old2) result = -1;
            } else if(sortType == SORT_NAME){
                result = m1.getFileName().compareTo(m2.getFileName());
            } else if(sortType == SORT_SIZE) {
                long old1 = Long.parseLong(m1.getFileSize());
                long old2 = Long.parseLong(m2.getFileSize());
                if (old1> old2) result = 1;
                if (old1 < old2) result = -1;
            }

            return sortReverse ? result : -result;
        }
    }

    public void sort() {
        Collections.sort(listItems, new ComparatorValues());
        refresh();
    }
    public void sort(int sortType) {
        if (this.sortType != sortType)
            this.sortType = sortType;
        else
            sortReverse = !sortReverse;
        Collections.sort(listItems, new ComparatorValues());
        refresh();
    }

    public int getSortType() {
        return sortType;
    }
    public boolean isSortReverse() {
        return sortReverse;
    }
    public void setSortType(int sortType) {
        this.sortType = sortType;
    }
    public void setSortReverse(boolean sortReverse) {
        this.sortReverse = sortReverse;
    }
}
