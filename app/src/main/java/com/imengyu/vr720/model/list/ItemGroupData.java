package com.imengyu.vr720.model.list;

public class ItemGroupData {
    public int useCount;
    public String name;
    public Object tag;

    public ItemGroupData(String name) {
        this.name = name;
    }
    public ItemGroupData(String name, int count) {
        this.name = name;
        this.useCount = count;
    }
}
