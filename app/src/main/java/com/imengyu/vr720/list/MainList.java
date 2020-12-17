package com.imengyu.vr720.list;

import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;

import com.imengyu.vr720.R;
import com.imengyu.vr720.adapter.MainListAdapter;
import com.imengyu.vr720.config.MainMessages;
import com.imengyu.vr720.model.ImageItem;
import com.imengyu.vr720.model.OnListCheckableChangedListener;
import com.imengyu.vr720.model.list.MainListItem;
import com.imengyu.vr720.service.ListImageCacheService;
import com.imengyu.vr720.utils.FileSizeUtil;
import com.imengyu.vr720.utils.FileUtils;
import com.imengyu.vr720.utils.ImageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 主列表控制
 */
public class MainList extends SelectableListSolver<MainListItem> {

    public MainList(Context context, ListImageCacheService listImageCacheService) {
        this.context = context;
        this.listImageCacheService = listImageCacheService;
        resources = context.getResources();
    }

    public void init(Handler handler, GridView gridView) {
        this.handler = handler;

        mainListAdapter = new MainListAdapter(this, context, R.layout.item_main, mainListItems);
        mainListAdapter.registerDataSetObserver(new DataSetObserver() {

            private boolean nextChangedDoNotNotify = false;
            @Override
            public void onChanged() {
                super.onChanged();
                if(nextChangedDoNotNotify){
                    nextChangedDoNotNotify = false;
                    return;
                }
                if(mainListItems.size() >= 4){
                    for(int i = mainListItems.size() - 1;i>=0;i--){
                        MainListItem item = mainListItems.get(i);
                        if(item.getForceItemType() == MainListItem.ITEM_TYPE_TEXT)
                            mainListItems.remove(item);
                    }
                    mainListItems.add(new MainListItem(resources.getString(R.string.text_end)));
                    nextChangedDoNotNotify=true;
                    mainListAdapter.notifyDataSetChanged();
                }else if(mainListItems.size() > 0){
                    for(int i = mainListItems.size() - 1;i>=0;i--){
                        MainListItem item = mainListItems.get(i);
                        if(item.getForceItemType() == MainListItem.ITEM_TYPE_TEXT)
                            mainListItems.remove(item);
                    }
                    nextChangedDoNotNotify=true;
                    mainListAdapter.notifyDataSetChanged();

                }

            }
        });

        super.init(mainListAdapter, mainListItems);
        super.setListOnNotifyChangeListener(this::notifyChange);

        gridView.setAdapter(mainListAdapter);
        gridView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
    }

    private final Resources resources;
    private final Context context;
    private final ListImageCacheService listImageCacheService;
    private Handler handler;

    public Resources getResources() {
        return resources;
    }

    //====================================================
    //主列表控制
    //====================================================

    private final List<MainListItem> mainListItems = new ArrayList<>();
    private MainListAdapter mainListAdapter = null;

    public ArrayList<CharSequence> getMainListPathItems() {
        ArrayList<CharSequence> list = new ArrayList<>();
        for (MainListItem li : mainListItems) {
            String filePath = li.getFilePath();
            if (filePath != null)
                list.add(filePath);
        }
        return list;
    }
    public List<MainListItem> getMainListItems() {
        return mainListItems;
    }
    public MainListAdapter getMainListAdapter() {
        return mainListAdapter;
    }
    public int getMainListItemCount() { return mainListItems.size(); }

    /**
     * 缩略图加载
     */
    public void loadThumbnail(MainListItem item) {
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

    //====================================================
    //条目操作
    //====================================================

    /**
     * 条目是否已存在
     * @param filePath 文件路径
     * @return 返回是否已存在
     */
    public boolean existsImageItem(String filePath) {
        for(MainListItem m : mainListItems){
            if(filePath.equals(m.getFilePath()))
                return true;
        }
        return false;
    }

    /**
     * 查找指定文件路径的条目
     * @param filePath 文件路径
     * @return 返回条目，如果不存在，则返回null
     */
    public MainListItem findImageItem(String filePath) {
        for(MainListItem m : mainListItems){
            if(filePath.equals(m.getFilePath()))
                return m;
        }
        return null;
    }
    /**
     * 条目添加
     * @param imageItem 条目
     * @param notify 是否通知更新列表
     */
    public void addImageToItem(ImageItem imageItem, boolean notify) {
        File f = new File(imageItem.path);
        if (f.exists()) {

            if(existsImageItem(imageItem.path))
                return;

            final MainListItem newItem = new MainListItem(imageItem);

            newItem.setThumbnailLoading(true);
            newItem.setThumbnailFail(false);
            newItem.setFileModifyDate(f.lastModified());
            newItem.setFileSizeValue(f.length());

            mainListItems.add(newItem);
            if (notify) mainListAdapter.notifyDataSetChanged();
        }
    }
    /**
     * 清空
     */
    public void clear() {
        mainListItems.clear();
        mainListAdapter.notifyDataSetChanged();
    }

    /**
     * 删除一个项目
     * @param item 项目
     */
    public void deleteItem(MainListItem item) {
        mainListItems.remove(item);
        mainListAdapter.notifyDataSetChanged();
    }
    /**
     * 删除某些项目
     */
    public void deleteItems(List<MainListItem> items) {
        mainListItems.removeAll(items);
        mainListAdapter.notifyDataSetChanged();
    }
    /**
     * 通知刷新
     */
    public void notifyChange() {
        handler.sendEmptyMessage(MainMessages.MSG_REFRESH_LIST);
    }
    public void notifyChange(int delay) {
        handler.sendEmptyMessageDelayed(MainMessages.MSG_REFRESH_LIST, delay);
    }

    //====================================================
    //条目排序
    //====================================================

    public static final int MAIN_SORT_DATE = 681;
    public static final int MAIN_SORT_NAME = 682;
    public static final int MAIN_SORT_SIZE = 683;

    private int mainSortType = MAIN_SORT_DATE;
    private boolean mainSortReverse = false;

    private class MainComparatorValues implements Comparator<MainListItem> {

        @Override
        public int compare(MainListItem m1, MainListItem m2) {
            int result = 0;
            if(mainSortType==MAIN_SORT_DATE){
                long old1=m1.getFileModifyDate();
                long old2=m2.getFileModifyDate();
                if (old1> old2) result = 1;
                if (old1 < old2) result = -1;
            } else if(mainSortType==MAIN_SORT_NAME){
                result = m1.getFileName().compareTo(m2.getFileName());
            } else if(mainSortType==MAIN_SORT_SIZE){
                long old1=m1.getFileSizeValue();
                long old2=m2.getFileSizeValue();
                if (old1> old2) result = 1;
                if (old1 < old2) result = -1;
            }

            return mainSortReverse ? result : -result;
        }
    }

    public void sort() {
        Collections.sort(mainListItems, new MainComparatorValues());
        mainListAdapter.notifyDataSetChanged();
    }
    public void sort(int sortType) {
        if (mainSortType != sortType)
            mainSortType = sortType;
        else
            mainSortReverse = !mainSortReverse;
        Collections.sort(mainListItems, new MainComparatorValues());
        mainListAdapter.notifyDataSetChanged();
    }

    public int getMainSortType() {
        return mainSortType;
    }
    public boolean isMainSortReverse() {
        return mainSortReverse;
    }
    public void setMainSortType(int mainSortType) {
        this.mainSortType = mainSortType;
    }
    public void setMainSortReverse(boolean mainSortReverse) {
        this.mainSortReverse = mainSortReverse;
    }
}
