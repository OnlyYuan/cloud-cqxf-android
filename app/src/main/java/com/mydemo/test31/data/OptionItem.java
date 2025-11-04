package com.mydemo.test31.data;

public class OptionItem {
    private final int id;
    private final String name;

    public OptionItem(int id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getter
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}