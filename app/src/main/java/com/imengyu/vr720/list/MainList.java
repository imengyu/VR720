package com.imengyu.vr720.list;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
import com.imengyu.vr720.model.list.ItemGroupData;
import com.imengyu.vr720.model.list.MainListItem;
import com.imengyu.vr720.service.ListImageCacheService;
import com.imengyu.vr720.utils.DateUtils;
import com.imengyu.vr720.utils.ScreenUtils;
import com.imengyu.vr720.utils.StringUtils;
import com.imengyu.vr720.utils.layout.WrapHeightGridLayoutManager;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
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

    public void init(RecyclerView recycler_main) {
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

        bottomItems.add(new MainListItem("", false));
        bottomItems.add(new MainListItem(context.getString(R.string.text_reached_the_bottom), false));
        bottomItems.add(new MainListItem("", false));

        updateLayout();
    }

    private int itemHorizontalCount;
    private LinearLayoutManager listLinearLayoutManager;
    private GridLayoutManager listTwoGridLayoutManager;
    private GridLayoutManager gridSmallLayoutManager;
    private GridLayoutManager gridSmallHorizontalLayoutManager;
    private RecyclerView recycler_main;

    public RecyclerView getRecyclerView() {
        return recycler_main;
    }

    private final Resources resources;
    private final Context context;
    private final ListImageCacheService listImageCacheService;

    /**
     * 获取资源
     */
    public Resources getResources() { return resources; }

    private final List<MainListItem> bottomItems = new ArrayList<>();

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
        groupItems();
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
        this.sort();
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

    private final HashMap<String, ItemGroupData> mainListDateItems = new HashMap<>();
    private final List<MainListItem> mainListGropedItems = new ArrayList<>();
    private final List<MainListItem> mainListItems = new ArrayList<>();
    private MainListAdapter mainListAdapter = null;

    //====================================================
    //分组管理
    //====================================================

    private int getGridSpanSize(int pos) {
        MainListItem item = mainListGropedItems.get(pos);
        if(item.getForceItemType() == MainListItem.ITEM_TYPE_TEXT
                || item.getForceItemType() == MainListItem.ITEM_TYPE_GROUP_HEADER)
            return gridLineItemCount;
        else
            return 1;
    }
    private void groupItems() {

        mainListGropedItems.removeAll(bottomItems);

        if(isGrid && groupByDate) {

            //清空索引数
            for(String keys : mainListDateItems.keySet()) {
                ItemGroupData itemGroupData = mainListDateItems.get(keys);
                if (itemGroupData != null)
                    itemGroupData.useCount = 0;
            }

            //分组
            ItemGroupData itemGroupData;
            for (MainListItem i : mainListItems) {
                if (!mainListDateItems.containsKey(i.getGroup())) {
                    //添加组
                    MainListItem itemHeader = new MainListItem(i.getGroup(), true);
                    mainListGropedItems.add(itemHeader);
                    //添加
                    itemGroupData = new ItemGroupData(i.getGroup(), 1);
                    itemGroupData.tag = itemHeader;
                    mainListDateItems.put(i.getGroup(), itemGroupData);
                }
                else {
                    itemGroupData = mainListDateItems.get(i.getGroup());
                    if(itemGroupData != null) {
                        itemGroupData.useCount++;
                        MainListItem itemHeader = (MainListItem) itemGroupData.tag;
                        if (!mainListGropedItems.contains(itemHeader))
                            mainListGropedItems.add(itemHeader);
                    }
                }
            }

            //Log.d("MainList", String.format("mainListDateItems: %d", mainListDateItems.size()));

            for(int i = mainListGropedItems.size() - 1; i>= 0; i--) {
                MainListItem item = mainListGropedItems.get(i);
                //清空不存在子条目的组
                if(item.getForceItemType() == MainListItem.ITEM_TYPE_GROUP_HEADER) {
                    itemGroupData = mainListDateItems.get(item.getGroup());
                    if(itemGroupData == null || itemGroupData.useCount <= 0) {
                        mainListGropedItems.remove(item);
                        mainListDateItems.remove(item.getGroup());
                    }
                }
                //清除条目
                else if(item.getForceItemType() == MainListItem.ITEM_TYPE_NORMAL)
                    mainListGropedItems.remove(i);
            }
            //根据日期排序
            Collections.sort(mainListGropedItems, (o1, o2) -> {
                int result = -o1.getGroup().compareTo(o2.getGroup());
                return (sortType == SORT_DATE && mainSortReverse) ?
                        -result : result;
            });
            //插入条目
            int index ;
            for (String key : mainListDateItems.keySet()) {
                ItemGroupData groupData = mainListDateItems.get(key);
                MainListItem itemHeader = (MainListItem) Objects.requireNonNull(groupData).tag;
                index = mainListGropedItems.indexOf(itemHeader) + 1;

                boolean checked = true;
                MainListItem item;
                for (int j = mainListItems.size() - 1; j >= 0; j--) {
                    item = mainListItems.get(j);
                    if (item.getGroup().equals(key)) {
                        if (checked && !item.isChecked()) checked = false;
                        mainListGropedItems.add(index, item);
                    }
                }
                itemHeader.setChecked(checked);
            }
        }
        else {
            mainListGropedItems.clear();
            mainListGropedItems.addAll(mainListItems);
        }

        if(mainListItems.size() > 0)
            mainListGropedItems.addAll(bottomItems);
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
    public void cutDateHeaderRef(String header) {
        ItemGroupData itemGroupData = mainListDateItems.get(header);
        if(itemGroupData != null) {
            itemGroupData.useCount--;
            if(itemGroupData.useCount <= 0) {
                MainListItem item = (MainListItem)itemGroupData.tag;
                int index = mainListGropedItems.indexOf(item);
                mainListGropedItems.remove(index);
                mainListDateItems.remove(header);
                mainListAdapter.notifyItemRemoved(index);
            }
        }
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
                    subHandler.sendEmptyMessageDelayed(MainMessages.MSG_REFRESH_LIST_CHECK, 900);
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
            newItem.setGroup(newItem.getFileModifyDate());

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
        mainListDateItems.clear();
        mainListAdapter.notifyDataSetChanged();
    }

    /**
     * 删除一个项目
     * @param item 项目
     */
    public void deleteItem(MainListItem item) {
        selectedItems.remove(item);
        mainListItems.remove(item);

        cutDateHeaderRef(item.getGroup());

        int index = mainListGropedItems.indexOf(item);
        if(index >= 0) {
            mainListGropedItems.remove(index);
            mainListAdapter.notifyItemRemoved(index);
        }
    }
    /**
     * 删除某些项目
     */
    public void deleteItems(List<MainListItem> items) {
        mainListItems.removeAll(items);

        if(items.size() == 1) {
            deleteItem(items.get(0));
        } else {
            if(items.size() >= 10) {
                mainListGropedItems.removeAll(items);
                subHandler.sendEmptyMessageDelayed(MainMessages.MSG_REFRESH_LIST, 360);
            }else for(MainListItem item : items) {

                cutDateHeaderRef(item.getGroup());

                int index = mainListGropedItems.indexOf(item);
                if(index >= 0) {
                    mainListGropedItems.remove(index);
                    mainListAdapter.notifyItemRemoved(index);
                }

                subHandler.sendEmptyMessageDelayed(MainMessages.MSG_REFRESH_LIST, 460);
            }
        }

        selectedItems.removeAll(items);

        subHandler.sendEmptyMessageDelayed(MainMessages.MSG_REFRESH_LIST_CHECK_COUNT, 360);
    }

    /**
     * 通知刷新
     */
    public void notifyChange() {
        subHandler.sendEmptyMessage(MainMessages.MSG_REFRESH_LIST);
    }
    @Override
    public void refresh() {
        super.refresh();
    }

    //====================================================
    //条目排序
    //====================================================

    public static final int SORT_DATE = 681;
    public static final int SORT_NAME = 682;
    public static final int SORT_SIZE = 683;

    private int sortType = SORT_DATE;
    private boolean mainSortReverse = false;

    private class MainComparatorValues implements Comparator<MainListItem> {

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
            } else if(sortType == SORT_SIZE){
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
        if (this.sortType != sortType)
            this.sortType = sortType;
        else
            mainSortReverse = !mainSortReverse;
        sort();
    }

    public int getSortType() {
        return sortType;
    }
    public boolean isSortReverse() {
        return mainSortReverse;
    }
    public void setSortType(int sortType) {
        this.sortType = sortType;
    }
    public void setSortReverse(boolean sortReverse) {
        this.mainSortReverse = sortReverse;
    }

    //====================================================
    //Handler
    //====================================================

    public static class SubHandler extends Handler {
        private final WeakReference<MainList> mTarget;

        SubHandler(MainList target) {
            super(Looper.myLooper());
            mTarget = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MainMessages.MSG_REFRESH_LIST_WITH_NOTIFY:
                    mTarget.get().mainListAdapter.notifyItemRangeChanged(0, mTarget.get().mainListGropedItems.size());
                    break;
                case MainMessages.MSG_REFRESH_LIST_CHECK:
                    mTarget.get().changeCheck();
                    break;
                case MainMessages.MSG_REFRESH_LIST_CHECK_COUNT:
                    mTarget.get().notifyCheckItemCountChanged();
                    break;
                case MainMessages.MSG_NOTIFY_DATA_CHANGED:
                    mTarget.get().mainListAdapter.notifyDataSetChanged();
                    break;
                case MainMessages.MSG_REFRESH_LIST:
                    mTarget.get().refresh();
                    mTarget.get().mainListAdapter.notifyDataSetChanged();
                    break;
                case MainMessages.MSG_LATE_CHECK:
                    mTarget.get().mainListAdapter.handlerLateCheck(msg.obj);
                    break;
                case MainMessages.MSG_LATE_DEL_ITEMS:
                    break;
            }
        }
    }
    private final SubHandler subHandler = new SubHandler(this);

    public SubHandler getSubHandler() {
        return subHandler;
    }
}
