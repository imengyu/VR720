package com.imengyu.vr720.list;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.util.Size;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.imengyu.vr720.R;
import com.imengyu.vr720.adapter.MainListAdapter;
import com.imengyu.vr720.config.MainMessages;
import com.imengyu.vr720.fragment.HomeFragment;
import com.imengyu.vr720.model.ImageItem;
import com.imengyu.vr720.model.list.MainListItem;
import com.imengyu.vr720.service.ListImageCacheService;
import com.imengyu.vr720.utils.DateUtils;
import com.imengyu.vr720.utils.ScreenUtils;
import com.imengyu.vr720.utils.StringUtils;
import com.imengyu.vr720.utils.layout.WrapHeightGridLayoutManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 主列表控制
 */
public class MainList extends SelectableListSolver<MainListItem> {

    public MainList(Context context, ListImageCacheService listImageCacheService) {
        this.context = context;
        this.listImageCacheService = listImageCacheService;
        resources = context.getResources();
    }

    public void init(Handler handler, RecyclerView recycler_main) {
        this.handler = handler;
        this.recycler_main = recycler_main;

        mainListAdapter = new MainListAdapter(this, context, R.layout.item_main, mainListGropedItems);
        mainListAdapter.setMainListCheckGroupListener(this::checkGroupItems);

        Size screenSize = ScreenUtils.getScreenSize(context);
        itemHorizontalCount = screenSize.getHeight() / (screenSize.getWidth() / 3 - 3);

        listLinearLayoutManager = new LinearLayoutManager(context);
        listTwoGridLayoutManager = new GridLayoutManager(context, 2);
        gridSmallLayoutManager = new WrapHeightGridLayoutManager(context, 3);
        gridSmallHorizontalLayoutManager = new WrapHeightGridLayoutManager(context, itemHorizontalCount);

        GridLayoutManager.SpanSizeLookup spanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) { return getGridSpanSize(position); }
        };

        gridSmallLayoutManager.setSpanSizeLookup(spanSizeLookup);
        gridSmallHorizontalLayoutManager.setSpanSizeLookup(spanSizeLookup);

        super.init(mainListAdapter, mainListGropedItems);
        super.setListOnNotifyChangeListener(this::notifyChange);

        recycler_main.setHasFixedSize(false);
        recycler_main.setAdapter(mainListAdapter);
        recycler_main.setItemAnimator(new DefaultItemAnimator());
        updateLayout();
    }

    private int itemHorizontalCount;
    private LinearLayoutManager listLinearLayoutManager;
    private GridLayoutManager listTwoGridLayoutManager;
    private GridLayoutManager gridSmallLayoutManager;
    private GridLayoutManager gridSmallHorizontalLayoutManager;
    private RecyclerView recycler_main;

    private final Resources resources;
    private final Context context;
    private final ListImageCacheService listImageCacheService;
    private Handler handler;

    /**
     * 获取资源
     */
    public Resources getResources() { return resources; }

    private boolean isHorizontal = false;
    private boolean isGrid = false;
    private boolean groupByDate = true;

    /**
     * 设置列表是否以宫格模式显示
     * @param isGrid 是否以宫格模式显示
     */
    public void setListIsGrid(boolean isGrid) {
        this.isGrid = isGrid;
        if(listAdapter != null)
            ((MainListAdapter)listAdapter).setGrid(isGrid);
        if(recycler_main != null)
            updateLayout();
        resetAllThumbnail();
        notifyChange();
    }
    public void setListIsHorizontal(boolean isHorizontal) {
        this.isHorizontal = isHorizontal;
        if(recycler_main != null)
            updateLayout();
    }
    public boolean isGrid() {
        return isGrid;
    }
    public boolean isGroupByDate() {
        return groupByDate;
    }
    public void setGroupByDate(boolean groupByDate) {
        this.groupByDate = groupByDate;
    }

    private int gridLineItemCount = 0;
    private void updateLayout() {
        if(isGrid) {
            recycler_main.setLayoutManager(isHorizontal ? gridSmallHorizontalLayoutManager : gridSmallLayoutManager);
            gridLineItemCount = isHorizontal ? itemHorizontalCount : 3;
            ((MainListAdapter)listAdapter).setLineItemCount(gridLineItemCount);
        } else {
            recycler_main.setLayoutManager(isHorizontal ? listTwoGridLayoutManager : listLinearLayoutManager);
        }
    }

    private final List<MainListItem> searchedItems = new ArrayList<>();
    private final List<MainListItem> preSearchItems = new ArrayList<>();

    /**
     * 进行搜索
     * @param s 关键字
     */
    public int doSearch(String s, int searchType) {
        searchedItems.clear();
        preSearchItems.clear();
        preSearchItems.addAll(mainListItems);

        String[] keys = s.toLowerCase().split(" ");
        //生成搜索正则
        StringBuilder regex = new StringBuilder();
        for(int j = 0; j < keys.length; j++) {
            if(keys[j].isEmpty())
                continue;
            regex.append(StringUtils.replaceRegexSpecialChar(keys[j]));
            if(j < keys.length - 1)
                regex.append('|');
        }

        Pattern p = Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE);

        MainListItem item ;
        for(int i = preSearchItems.size() - 1; i>=0; i--) {

            item = preSearchItems.get(i);
            if(searchType != HomeFragment.SEARCH_TYPE_ALL) {
                if (!item.isVideo() && searchType != HomeFragment.SEARCH_TYPE_IMAGE) continue;
                if (item.isVideo() && searchType != HomeFragment.SEARCH_TYPE_VIDEO) continue;
            }

            if(p.matcher(item.getFileName()).find()) {
                searchedItems.add(item);
                preSearchItems.remove(i);
                continue;
            }
            if(p.matcher(item.getFileModifyDate()).find()) {
                searchedItems.add(item);
                preSearchItems.remove(i);
                continue;
            }
            if(p.matcher(item.getFileSize()).find()) {
                searchedItems.add(item);
                preSearchItems.remove(i);
            }
        }

        List<MainListItem> selectedItems = getSelectedItems();
        for(int i = preSearchItems.size() - 1; i >= 0; i--)
            preSearchItems.get(i).setSearchHidden(true);
        for(int i = searchedItems.size() - 1; i >= 0; i--) {
            item = searchedItems.get(i);
            item.setSearchHidden(false);
            if(selectedItems.contains(item)) {
                item.setChecked(false);
                selectedItems.remove(item);
            }
        }

        notifyChange();
        notifyCheckItemCountChanged();

        return searchedItems.size();
    }

    /**
     * 清空搜索
     */
    public void resetSearch() {
        for(int i = mainListItems.size() - 1; i>=0; i--)
            mainListItems.get(i).setSearchHidden(false);
        mainListAdapter.notifyDataSetChanged();
    }

    //====================================================
    //主列表控制
    //====================================================

    private final List<String> mainListDateItems = new ArrayList<>();
    private final List<MainListItem> mainListGropedItems = new ArrayList<>();
    private final List<MainListItem> mainListItems = new ArrayList<>();
    private MainListAdapter mainListAdapter = null;

    private int getGridSpanSize(int pos) {
        MainListItem item = mainListGropedItems.get(pos);
        if(item.getForceItemType() == MainListItem.ITEM_TYPE_TEXT
                || item.getForceItemType() == MainListItem.ITEM_TYPE_GROUP_HEADER)
            return gridLineItemCount;
        else
            return 1;
    }
    private void groupItems() {
        if(isGrid && groupByDate) {

            mainListDateItems.clear();

            for (MainListItem i : mainListItems) {
                if (!mainListDateItems.contains(i.getFileModifyDate()))
                    mainListDateItems.add(i.getFileModifyDate());
            }

            mainListGropedItems.clear();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mainListDateItems.sort((o1, o2) -> -o1.compareTo(o2));
            }

            for (String i : mainListDateItems) {
                MainListItem itemHeader = new MainListItem(i, true);
                mainListGropedItems.add(itemHeader);

                boolean checked = true;
                for (MainListItem j : mainListItems)
                    if (j.getFileModifyDate().equals(i)) {
                        if(checked && !j.isChecked()) checked = false;
                        mainListGropedItems.add(j);
                    }
                itemHeader.setChecked(checked);
            }
        }else {
            mainListGropedItems.clear();
            mainListGropedItems.addAll(mainListItems);
        }
    }
    private void checkGroupItems(String name, boolean check) {
        for (MainListItem i : mainListItems) {
            if (name.equals(i.getFileModifyDate())) {
                i.setChecked(check);
                if(check) {
                    if(!selectedItems.contains(i))
                        selectedItems.add(i);
                } else {
                    selectedItems.remove(i);
                }
            }
        }
        notifyCheckItemCountChanged();
        mainListAdapter.notifyDataSetChanged();
    }

    public ArrayList<CharSequence> getMainListPathItems() {
        ArrayList<CharSequence> list = new ArrayList<>();
        for (MainListItem li : mainListItems) {
            String filePath = li.getFilePath();
            if (filePath != null)
                list.add(filePath);
        }
        return list;
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
            if (drawable != null) {
                item.setThumbnail(drawable);
                item.setThumbnailLoading(false);
                item.setThumbnailFail(false);
            } else {
                item.setThumbnailLoading(false);
                item.setThumbnailFail(true);
            }
            synchronized (changeLockSyl) {
                if (!changeLock) {
                    changeLock = true;
                    handler.sendEmptyMessageDelayed(MainMessages.MSG_REFRESH_LIST_CHECK, 900);
                }
            }
        }).start();
    }

    private final byte[] changeLockSyl = new byte[0];
    private boolean changeLock = false;
    public void changeCheck() {
        synchronized(changeLockSyl) { changeLock = false; }
        refresh();
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
    public void resetAllThumbnail() {
        for(MainListItem m : mainListItems)
            m.setThumbnailLoaded(false);
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
     */
    public void addImageToItem(ImageItem imageItem) {
        File f = new File(imageItem.path);
        if (f.exists()) {

            if(existsImageItem(imageItem.path))
                return;

            final MainListItem newItem = new MainListItem(imageItem);

            newItem.setThumbnailLoading(true);
            newItem.setThumbnailFail(false);
            newItem.setFileModifyDateValue(f.lastModified());
            newItem.setFileModifyDate(DateUtils.format(new Date(f.lastModified()), DateUtils.FORMAT_SHORT));
            newItem.setFileSizeValue(f.length());

            mainListItems.add(newItem);
        } else {
            final MainListItem newItem = new MainListItem(imageItem);

            newItem.setThumbnailLoading(false);
            newItem.setThumbnailFail(true);

            mainListItems.add(newItem);
        }
    }
    /**
     * 清空
     */
    public void clear() {
        searchedItems.clear();
        mainListItems.clear();
        mainListGropedItems.clear();
        mainListAdapter.notifyDataSetChanged();
    }

    /**
     * 删除一个项目
     * @param item 项目
     */
    public void deleteItem(MainListItem item) {
        selectedItems.remove(item);
        mainListItems.remove(item);
        int index = mainListGropedItems.indexOf(item);
        if(index >= 0) {
            mainListGropedItems.remove(index);
            mainListAdapter.notifyItemRemoved(index);
        }
        else mainListAdapter.notifyDataSetChanged();
    }
    /**
     * 删除某些项目
     */
    public void deleteItems(List<MainListItem> items) {
        mainListItems.removeAll(items);

        if(items.size() < 16)
            for(MainListItem item : items) {
                int index = mainListGropedItems.indexOf(item);
                if (index >= 0) {
                    mainListGropedItems.remove(index);
                    mainListAdapter.notifyItemRemoved(index);
                }
            }
        else
            mainListGropedItems.removeAll(items);
        selectedItems.removeAll(items);
        notifyChange(1000);
    }
    /**
     * 通知刷新
     */
    public void notifyChange() {
        groupItems();
        handler.sendEmptyMessage(MainMessages.MSG_REFRESH_LIST);
    }
    public void notifyChange(int delay) {
        handler.sendEmptyMessageDelayed(MainMessages.MSG_REFRESH_LIST, delay);
    }
    @Override
    public void refresh() {
        super.refresh();
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
            if(mainSortType == MAIN_SORT_DATE){
                long old1 = m1.getFileModifyDateValue();
                long old2 = m2.getFileModifyDateValue();
                if (old1 > old2) result = 1;
                if (old1 < old2) result = -1;
            } else if(mainSortType == MAIN_SORT_NAME){
                result = m1.getFileName().compareTo(m2.getFileName());
            } else if(mainSortType == MAIN_SORT_SIZE){
                long old1 = m1.getFileSizeValue();
                long old2 = m2.getFileSizeValue();
                if (old1> old2) result = 1;
                if (old1 < old2) result = -1;
            }

            return mainSortReverse ? result : -result;
        }
    }

    public void sort() {
        Collections.sort(mainListItems, new MainComparatorValues());
        groupItems();
        notifyChange();
    }
    public void sort(int sortType) {
        if (mainSortType != sortType)
            mainSortType = sortType;
        else
            mainSortReverse = !mainSortReverse;
        sort();
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
