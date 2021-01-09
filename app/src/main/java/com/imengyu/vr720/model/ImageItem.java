package com.imengyu.vr720.model;

import com.imengyu.vr720.utils.FileUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ImageItem implements Serializable {
    public String path;
    public List<Integer> belongGalleries = new ArrayList<>();
    public boolean isVideo = false;

    public ImageItem() {

    }
    public ImageItem(String path, String belongGalleriesStr) {
        this.path = path;
        this.isVideo = FileUtils.getFileIsVideo(path);
        setBelongGalleries(belongGalleriesStr);
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
    public void setBelongGalleries(String galleries) {
        String[] str = galleries.split(";");
        for(String s : str)
            if(!s.isEmpty())
                belongGalleries.add(Integer.parseInt(s));
    }
}
