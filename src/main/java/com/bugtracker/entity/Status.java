package com.bugtracker.entity;

import java.util.EnumSet;
import java.util.Set;

/** Life-cycle of a ticket. Each status knows which statuses it may move to. */
public enum Status implements Labelled {
    OPEN("Open"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved"),
    CLOSED("Closed"),
    REOPENED("Reopened");

    private final String label;

    Status(String label) { this.label = label; }

    @Override public String getLabel() { return label; }
    @Override public String getCssClass() { return name().toLowerCase().replace('_', '-'); }

    /** The statuses this status is allowed to change to. */
    public Set<Status> allowedNext() {
        return switch (this) {
            case OPEN -> EnumSet.of(IN_PROGRESS, RESOLVED, CLOSED);
            case IN_PROGRESS -> EnumSet.of(OPEN, RESOLVED, CLOSED);
            case RESOLVED -> EnumSet.of(CLOSED, REOPENED);
            case CLOSED -> EnumSet.of(REOPENED);
            case REOPENED -> EnumSet.of(IN_PROGRESS, RESOLVED, CLOSED);
        };
    }

    public boolean canMoveTo(Status next) { return allowedNext().contains(next); }
}
