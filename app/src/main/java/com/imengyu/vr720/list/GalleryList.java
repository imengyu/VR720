package com.imengyu.vr720.list;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.widget.AbsListView;
import android.widget.ListView;

import com.imengyu.vr720.R;
import com.imengyu.vr720.adapter.GalleryListAdapter;
import com.imengyu.vr720.config.MainMessages;
import com.imengyu.vr720.model.list.GalleryListItem;
import com.imengyu.vr720.service.ListImageCacheService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GalleryList extends SelectableListSolver<GalleryListItem> {

    private final Resources resources;
    private final Context context;
    private final ListImageCacheService listImageCacheService;
    private Handler handler;

    public Resources getResources() {
        return resources;
    }

    public GalleryList(Context context, ListImageCacheService listImageCacheService) {
        this.context = context;
        this.listImageCacheService = listImageCacheService;
        resources = context.getResources();
    }

    private final ArrayList<GalleryListItem> listItems = new ArrayList<>();
    private GalleryListAdapter listAdapter = null;

    public void init(Handler handler, ListView listView) {
        this.handler = handler;

        listAdapter = new GalleryListAdapter(this, context, R.layout.item_gallery, listItems);

        super.init(listAdapter, listItems);
        super.setListOnNotifyChangeListener(this::notifyChange);

        listView.setAdapter(listAdapter);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
    }

    //====================================================
    //条目操作
    //====================================================

    public List<GalleryListItem> getListItems() {
        return listItems;
    }
    public GalleryListAdapter getListAdapter() {
        return listAdapter;
    }
    public int getListItemCount() { return listItems.size(); }
    public GalleryListItem findItem(int id) {
        for (GalleryListItem item : listItems)
            if (item.getId() == id)
                return item;
        return null;
    }
    public void addItem(GalleryListItem item, boolean notify) {
        listItems.add(item);
        if (notify) refresh();
    }
    public void addItem(String name, int id, boolean notify) {
        
        final GalleryListItem newItem = new GalleryListItem();
        newItem.setName(name);
        newItem.setId(id);
        
        listItems.add(newItem);
        if (notify) refresh();
    } 
    public void clear() {
        listItems.clear();
        refresh();
    }
    public void deleteItem(GalleryListItem item) {
        listItems.remove(item);
        refresh();
    }
    public void deleteItems(List<GalleryListItem> items) {
        listItems.removeAll(items);
        refresh();
    }
    public void notifyChange() {
        handler.sendEmptyMessage(MainMessages.MSG_REFRESH_GALLERY_LIST);
    }
    public void loadItemThumbnail(GalleryListItem item) {
        //在背景线程进行缩略图加载
        new Thread(() -> {
            Drawable drawable = listImageCacheService.loadImageThumbnailCache(item.getThumbnailPath());
            if(drawable != null) {
                item.setThumbnail(drawable);
                item.setThumbnailLoading(false);
                notifyChange();
            } else {
                item.setThumbnailLoading(false);
                item.setThumbnailFail(true);
                notifyChange();
            }
        }).start();
    }

    //====================================================
    //条目排序
    //====================================================
    
    public static final int GALLERY_SORT_NAME = 671;
    public static final int GALLERY_SORT_DATE = 672;
    public static final int GALLERY_SORT_CUSTOM = 673;

    private int sortType = GALLERY_SORT_NAME;
    private boolean sortReverse = false;

    private class ComparatorValues implements Comparator<GalleryListItem> {

        @Override
        public int compare(GalleryListItem m1, GalleryListItem m2) {
            int result = 0;
            if(sortType == GALLERY_SORT_DATE){
                long old1 = m1.getCreateDate();
                long old2 = m2.getCreateDate();
                if (old1 > old2) result = 1;
                if (old1 < old2) result = -1;
            } else if(sortType == GALLERY_SORT_NAME){
                result = m1.getName().compareTo(m2.getName());
            } else if(sortType == GALLERY_SORT_CUSTOM){
                long old1 = m1.getSortOrder();
                long old2 = m2.getSortOrder();
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
