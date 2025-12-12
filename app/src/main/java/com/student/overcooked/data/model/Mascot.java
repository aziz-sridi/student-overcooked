package com.student.overcooked.data.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a mascot character with multiple stages
 */
public class Mascot {
    private String id;
    private String name;
    private int price;
    private List<Integer> stageDrawables;
    private String[] stageNames;
    private int thumbnailResId;

    public Mascot(String id, String name, int price, int thumbnailResId, List<Integer> stageDrawables, String[] stageNames) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.thumbnailResId = thumbnailResId;
        this.stageDrawables = stageDrawables != null ? stageDrawables : new ArrayList<>();
        this.stageNames = stageNames != null ? stageNames : new String[]{};
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getPrice() { return price; }
    public int getThumbnailResId() { return thumbnailResId; }
    public List<Integer> getStageDrawables() { return stageDrawables; }
    public String[] getStageNames() { return stageNames; }
}
