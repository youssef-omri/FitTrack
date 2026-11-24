package com.example.fittrack;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class HistoryDateGroup implements Serializable {
    private String date;
    private List<WorkoutHistoryItem> items;

    public HistoryDateGroup(String date) {
        this.date = date;
        this.items = new ArrayList<>();
    }

    public HistoryDateGroup(String date, List<WorkoutHistoryItem> items) {
        this.date = date;
        this.items = items != null ? items : new ArrayList<>();
    }

    public String getDate() {
        return date != null ? date : "";
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<WorkoutHistoryItem> getItems() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    public void setItems(List<WorkoutHistoryItem> items) {
        this.items = items;
    }

    public void addItem(WorkoutHistoryItem item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
    }

    public int getItemCount() {
        return items != null ? items.size() : 0;
    }
}
