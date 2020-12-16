package com.imengyu.vr720.model.list;

public class CheckableListItem {

    private boolean checkable = true;
    private boolean checked;
    private int checkIndex;

    public boolean isChecked() {
        return checked;
    }
    public void setChecked(boolean checked) {
        this.checked = checked;
    }
    public int getCheckIndex() {
        return checkIndex;
    }
    public void setCheckIndex(int checkIndex) {
        this.checkIndex = checkIndex;
    }
    public boolean isCheckable() {
        return checkable;
    }
    public void setCheckable(boolean checkable) {
        this.checkable = checkable;
    }
}
