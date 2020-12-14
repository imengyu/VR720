package com.imengyu.vr720.list;

import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.imengyu.vr720.R;
import com.imengyu.vr720.config.MainMessages;
import com.imengyu.vr720.utils.DateUtils;
import com.imengyu.vr720.utils.FileSizeUtil;
import com.imengyu.vr720.utils.FileUtils;
import com.imengyu.vr720.utils.ImageUtils;
import com.imengyu.vr720.widget.MainThumbnailImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * 主列表控制
 */
public class MainList {

    public MainList(Context context) {
        this.context = context;
        resources = context.getResources();
    }

    public void init(Handler handler, ListView listView) {
        this.handler = handler;

        mainListAdapter = new MainListAdapter(context, R.layout.item_main, mainListItems);
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
                    mainListItems.add(new MainListItem("\n\n"));
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

        listView.setAdapter(mainListAdapter);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
    }

    private Resources resources;
    private Context context;
    private Handler handler;

    private final List<MainListItem> selectedItems = new ArrayList<>();

    public List<MainListItem> getSelectedItems() { return selectedItems; }
    public int getSelectedItemCount() { return selectedItems.size(); }
    public void clearSelectedItems() {
        selectedItems.clear();
        for (MainListItem item : mainListItems)
            item.setChecked(false);
        notifyChange();
        notifyCheckItemCountChanged();
    }
    public void selectAllItems() {
        for (MainListItem item : mainListItems) {
            if(!item.isChecked()) {
                item.setChecked(true);
                selectedItems.add(item);
            }
        }
        notifyChange();
        notifyCheckItemCountChanged();
    }
    public boolean isMainListCheckMode() {
        return mainListAdapter.isCheckable();
    }
    public void setMainListCheckMode  (boolean checkMod) {
        if(checkMod != mainListAdapter.isCheckable()) {
            mainListAdapter.setCheckable(checkMod);
            mainListAdapter.notifyDataSetChanged();
        }
    }

    //====================================================
    //主列表事件
    //====================================================

    //图片点击事件
    private final View.OnClickListener mainListBaseOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int i = (int)view.getTag();
            if(mainListAdapter.isCheckable()) {
                MainListItem item = mainListAdapter.getItem(i);
                if(item != null) {
                    if(item.isChecked()) {
                        item.setChecked(false);
                        selectedItems.remove(item);
                    }else {
                        item.setChecked(true);
                        if(!selectedItems.contains(item))
                            selectedItems.add(item);
                    }
                    notifyChange();
                    notifyCheckItemCountChanged();
                }
            } else
                mainListOnItemClickListener.onItemClick(null, view, i, 0);
        }
    };
    //图片长按事件
    private final View.OnLongClickListener mainListBaseOnLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
            if(!mainListAdapter.isCheckable()) {
                setMainListCheckMode(true);
                int i = (int)view.getTag();
                MainListItem item = mainListAdapter.getItem(i);
                if(item != null) {
                    item.setChecked(true);
                    if (!selectedItems.contains(item))
                        selectedItems.add(item);
                    notifyCheckItemCountChanged();
                }
            }
            return true;
        }
    };

    private AdapterView.OnItemClickListener mainListOnItemClickListener;
    private OnMainListCheckItemCountChangedListener mainListCheckItemCountChangedListener;

    public void setMainListCheckableChangedListener(OnMainListCheckableChangedListener mainListCheckableChangedListener) {
        mainListAdapter.setMainListCheckableChangedListener(mainListCheckableChangedListener);
    }
    public void setMainListOnItemClickListener(AdapterView.OnItemClickListener onItemClickListener) {
        mainListOnItemClickListener = onItemClickListener;
    }
    public void setMainListCheckItemCountChangedListener(OnMainListCheckItemCountChangedListener mainListCheckItemCountChangedListener) {
        this.mainListCheckItemCountChangedListener = mainListCheckItemCountChangedListener;
    }

    public interface OnMainListCheckableChangedListener {
        void onMainListCheckableChangedListener(boolean checkable);
    }
    public interface OnMainListCheckItemCountChangedListener {
        void onMainListCheckItemCountChangedListener(int checkedCount);
    }

    private void notifyCheckItemCountChanged() {
        if(mainListCheckItemCountChangedListener!=null)
            mainListCheckItemCountChangedListener.onMainListCheckItemCountChangedListener(selectedItems.size());
    }

    //====================================================
    //主列表控制
    //====================================================

    private final List<MainListItem> mainListItems = new ArrayList<>();
    private MainListAdapter mainListAdapter = null;

    public ArrayList<CharSequence> getMainListPathItems() {
        ArrayList<CharSequence> list = new ArrayList<>();
        for (MainListItem li : mainListItems)
            if(li.filePath != null)
                list.add(li.filePath);
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
     * 列表条目绑定
     */
    private static class ViewHolder {
        MainThumbnailImageView imageView;
        TextView textView;
        CheckBox checkMark;
    }
    /**
     * 主列表数据
     */
    public static class MainListItem {

        public static final int ITEM_TYPE_NORMAL = 827;
        public static final int ITEM_TYPE_TEXT = 828;

        private String filePath;
        private String fileName;
        private String fileSize;
        private Drawable thumbnail;
        private boolean thumbnailLoading;
        private boolean thumbnailLoadingStarted;
        private boolean thumbnailFail;
        private boolean checked;
        private int checkeIndex;
        private long fileSizeValue;
        private long fileModifyDate;
        private int forceItemType;

        public MainListItem(String itemText){
            this.forceItemType = ITEM_TYPE_TEXT;
            this.fileName = itemText;
        }
        public MainListItem(String filePath, String fileName, String fileSize) {
            this.filePath = filePath;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.forceItemType = ITEM_TYPE_NORMAL;
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
        public boolean isChecked() {
            return checked;
        }
        public void setChecked(boolean checked) {
            this.checked = checked;
        }
        public int getCheckeIndex() {
            return checkeIndex;
        }
        public void setCheckeIndex(int checkeIndex) {
            this.checkeIndex = checkeIndex;
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
        public long getFileModifyDate() {
            return fileModifyDate;
        }
        public void setFileModifyDate(long fileModifyDate) {
            this.fileModifyDate = fileModifyDate;
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
    }
    /**
     * 主列表适配器
     */
    public class MainListAdapter extends ArrayAdapter<MainListItem> {

        private boolean mCheckable;
        private OnMainListCheckableChangedListener mainListCheckableChangedListener;

        MainListAdapter(Context context, int layoutId, List<MainListItem> list) {
            super(context, layoutId, list);
        }

        void setCheckable(boolean mCheckable) {
            this.mCheckable = mCheckable;
            if (this.mainListCheckableChangedListener != null)
                this.mainListCheckableChangedListener.onMainListCheckableChangedListener(mCheckable);
        }

        boolean isCheckable() { return mCheckable; }

        void setMainListCheckableChangedListener(OnMainListCheckableChangedListener mainListCheckableChangedListener) {
            this.mainListCheckableChangedListener = mainListCheckableChangedListener;
        }

        /**
         * 缩略图加载
         */
        void loadThumbnail(MainListItem item) {
            //在背景线程进行缩略图加载
            new Thread(() -> {
                try {
                    item.thumbnail = new BitmapDrawable(resources, ImageUtils.revitionImageSize(item.filePath, 800, 400));
                    item.setThumbnailLoading(false);
                    notifyChange();
                } catch (Exception e) {
                    e.printStackTrace();
                    item.setThumbnailLoading(false);
                    item.setThumbnailFail(true);
                    notifyChange();
                }
            }).start();
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            final MainListItem item = getItem(position);

            ViewHolder viewHolder;

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_main, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.textView = convertView.findViewById(R.id.text_item);
                viewHolder.imageView = convertView.findViewById(R.id.img_item);
                viewHolder.checkMark = convertView.findViewById(R.id.check_item);

                viewHolder.imageView.setOnLongClickListener(mainListBaseOnLongClickListener);
                viewHolder.imageView.setOnClickListener(mainListBaseOnClickListener);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            if(item != null) {
                if (item.getForceItemType() == MainListItem.ITEM_TYPE_NORMAL) {
                    viewHolder.imageView.setChecked(item.isChecked());
                    if (item.isThumbnailFail())
                        viewHolder.imageView.setImageResource(R.drawable.ic_noprob);
                    else if (item.isThumbnailLoading()) {
                        viewHolder.imageView.setImageResource(R.drawable.ic_tumb);

                        if (!item.isThumbnailLoadingStarted()) {
                            item.setThumbnailLoadingStarted(true);
                            loadThumbnail(item);
                        }
                    } else viewHolder.imageView.setImageDrawable(item.getThumbnail());

                    viewHolder.imageView.setTag(position);
                    viewHolder.imageView.setVisibility(View.VISIBLE);
                    viewHolder.imageView.setImageText(item.getFileName());

                    if(mainSortType == MAIN_SORT_DATE)
                        viewHolder.imageView.setImageSize(DateUtils.format(new Date(item.getFileModifyDate()), DateUtils.FORMAT_SHORT));
                    else viewHolder.imageView.setImageSize(item.getFileSize());

                    viewHolder.textView.setVisibility(View.GONE);

                    viewHolder.checkMark.setChecked(item.isChecked());
                    viewHolder.checkMark.setVisibility(mCheckable ? View.VISIBLE : View.GONE);
                }
                else if (item.getForceItemType() == MainListItem.ITEM_TYPE_TEXT) {
                    viewHolder.imageView.setVisibility(View.GONE);
                    viewHolder.textView.setVisibility(View.VISIBLE);
                    viewHolder.checkMark.setVisibility(View.GONE);
                    viewHolder.textView.setText(item.getFileName());
                }
            }
            return convertView;
        }
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
     * @param filePath 文件路径
     * @param notify 是否通知更新列表
     */
    public void addImageToItem(String filePath, boolean notify) {
        File f = new File(filePath);
        if (f.exists()) {

            if(existsImageItem(filePath))
                return;

            final MainListItem newItem = new MainListItem(filePath, FileUtils.getFileName(filePath), FileSizeUtil.getAutoFileOrFilesSize(filePath));
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
    public void deleteItem(MainList.MainListItem item) {
        mainListItems.remove(item);
        mainListAdapter.notifyDataSetChanged();
    }
    /**
     * 删除某些项目
     */
    public void deleteItems(List<MainList.MainListItem> items) {
        mainListItems.removeAll(items);
        mainListAdapter.notifyDataSetChanged();
    }
    /**
     * 通知刷新
     */
    public void notifyChange() {
        handler.sendEmptyMessage(MainMessages.MSG_REFRESH_LIST);
    }
    /**
     * 强制刷新列表
     */
    public void refesh() {
        mainListAdapter.notifyDataSetChanged();
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
