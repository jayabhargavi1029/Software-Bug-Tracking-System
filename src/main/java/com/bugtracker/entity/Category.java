package com.bugtracker.entity;

/** The module of the software in which the bug was found. */
public enum Category implements Labelled {
    UI("User Interface"),
    BACKEND("Backend / Logic"),
    DATABASE("Database"),
    SECURITY("Security"),
    PERFORMANCE("Performance"),
    INTEGRATION("Integration"),
    DOCUMENTATION("Documentation");

    private final String label;

    Category(String label) { this.label = label; }

    @Override public String getLabel() { return label; }
    @Override public String getCssClass() { return name().toLowerCase(); }
}
