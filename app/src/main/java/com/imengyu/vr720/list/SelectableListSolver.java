package com.imengyu.vr720.list;

import android.view.View;
import android.widget.AdapterView;

import com.imengyu.vr720.adapter.CheckableListAdapter;
import com.imengyu.vr720.model.OnListCheckableChangedListener;
import com.imengyu.vr720.model.OnNotifyChangeListener;
import com.imengyu.vr720.model.list.CheckableListItem;

import java.util.ArrayList;
import java.util.List;

public class SelectableListSolver<T extends CheckableListItem> {

    public void init(CheckableListAdapter<T> adapter, List<T> items) {
        listAdapter = adapter;
        listItems = items;
    }

    protected CheckableListAdapter<T> listAdapter;
    protected List<T> listItems;
    private final List<T> selectedItems = new ArrayList<>();
    private int checkableItemsCount = 0;

    public List<T> getSelectedItems() { return selectedItems; }
    public int getSelectedItemCount() { return selectedItems.size(); }
    public void clearSelectedItems() {
        selectedItems.clear();
        for (T item : listItems)
            item.setChecked(false);
        notifyChange();
        notifyCheckItemCountChanged();
    }
    public void selectAllItems() {
        for (T item : listItems) {
            if(!item.isChecked() && item.isCheckable()) {
                item.setChecked(true);
                selectedItems.add(item);
            }
        }
        notifyChange();
        notifyCheckItemCountChanged();
    }
    public boolean isListCheckMode() {
        return listAdapter.isCheckable();
    }
    public void setListCheckMode(boolean checkMod) {
        if(checkMod != listAdapter.isCheckable()) {
            listAdapter.setCheckable(checkMod);
            listAdapter.notifyDataSetChanged();
        }
    }
    public int getCheckableItemsCount() {
        return checkableItemsCount;
    }
    public void updateCheckableItemsCount() {
        checkableItemsCount = 0;
        for(T t : listItems) {
            if(t.isCheckable())
                checkableItemsCount++;
        }
    }

    private final View.OnClickListener mainListBaseOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int i = (int)view.getTag();
            if(listAdapter.isCheckable()) {
                T item = listAdapter.getItem(i);
                if(item != null) {
                    if(item.isChecked()) {
                        item.setChecked(false);
                        selectedItems.remove(item);
                    }else if(item.isCheckable()) {
                        item.setChecked(true);
                        if(!selectedItems.contains(item))
                            selectedItems.add(item);
                    }
                    notifyChange();
                    notifyCheckItemCountChanged();
                }
            } else
                listOnItemClickListener.onItemClick(null, view, i, 0);
        }
    };
    private final View.OnLongClickListener mainListBaseOnLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
            if(!listAdapter.isCheckable()) {
                setListCheckMode(true);
                int i = (int)view.getTag();
                T item = listAdapter.getItem(i);
                if(item != null && item.isCheckable()) {
                    item.setChecked(true);
                    if (!selectedItems.contains(item))
                        selectedItems.add(item);
                    notifyCheckItemCountChanged();
                }
            }
            return true;
        }
    };

    public View.OnClickListener getMainListBaseOnClickListener() {
        return mainListBaseOnClickListener;
    }
    public View.OnLongClickListener getMainListBaseOnLongClickListener() {
        return mainListBaseOnLongClickListener;
    }

    private AdapterView.OnItemClickListener listOnItemClickListener;
    private OnListCheckItemCountChangedListener listCheckItemCountChangedListener;
    private OnNotifyChangeListener onNotifyChangeListener;

    public void setListCheckableChangedListener(OnListCheckableChangedListener listCheckableChangedListener) {
        listAdapter.setMainListCheckableChangedListener(listCheckableChangedListener);
    }
    public void setListOnItemClickListener(AdapterView.OnItemClickListener listOnItemClickListener) {
        this.listOnItemClickListener = listOnItemClickListener;
    }
    public void setListCheckItemCountChangedListener(OnListCheckItemCountChangedListener listCheckItemCountChangedListener) {
        this.listCheckItemCountChangedListener = listCheckItemCountChangedListener;
    }
    public void setListOnNotifyChangeListener(OnNotifyChangeListener onNotifyChangeListener) {
        this.onNotifyChangeListener = onNotifyChangeListener;
    }

    private void notifyCheckItemCountChanged() {
        if(listCheckItemCountChangedListener!=null)
            listCheckItemCountChangedListener.onListCheckItemCountChangedListener(selectedItems.size());
    }
    private void notifyChange() {
        if(onNotifyChangeListener != null)
            onNotifyChangeListener.onNotifyChange();
    }

    public void refresh() {
        updateCheckableItemsCount();
        listAdapter.notifyDataSetChanged();
    }
}
