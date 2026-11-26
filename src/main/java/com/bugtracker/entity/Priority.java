package com.bugtracker.entity;

public enum Priority implements Labelled {
    LOW("Low"), MEDIUM("Medium"), HIGH("High"), CRITICAL("Critical");

    private final String label;

    Priority(String label) { this.label = label; }

    @Override public String getLabel() { return label; }
    @Override public String getCssClass() { return name().toLowerCase(); }
}
