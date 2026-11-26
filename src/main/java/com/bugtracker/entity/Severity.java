package com.bugtracker.entity;

public enum Severity implements Labelled {
    MINOR("Minor"), MODERATE("Moderate"), MAJOR("Major"), BLOCKER("Blocker");

    private final String label;

    Severity(String label) { this.label = label; }

    @Override public String getLabel() { return label; }
    @Override public String getCssClass() { return name().toLowerCase(); }
}
