package com.imengyu.vr720.model;

import java.util.ArrayList;
import java.util.List;

public class ImageItem {
    public String path;
    public List<Integer> belongGalleries = new ArrayList<>();
    public boolean isVideo = false;

    public ImageItem(String path, String belongGalleriesStr) {
        this.path = path;

        String[] str = belongGalleriesStr.split(";");
        for(String s : str)
            if(!s.isEmpty())
                belongGalleries.add(Integer.parseInt(s));
    }

    public boolean isInBelongGalleries(int id) {
        return belongGalleries.contains(id);
    }
    public String getBelongGalleries() {
        StringBuilder sb = new StringBuilder();
        for(Integer i : belongGalleries) {
            if(sb.length() != 0)
                sb.append(';');
            sb.append(i);
        }
        return sb.toString();
    }
}
