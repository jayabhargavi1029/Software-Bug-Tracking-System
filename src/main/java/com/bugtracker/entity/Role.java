package com.bugtracker.entity;

public enum Role implements Labelled {
    ADMIN("Administrator"),
    DEVELOPER("Developer");

    private final String label;

    Role(String label) { this.label = label; }

    @Override public String getLabel() { return label; }
    @Override public String getCssClass() { return name().toLowerCase(); }
}
