package com.bugtracker.entity;

/** Interface implemented by every enum that is shown in the UI (label + CSS class for badges). */
public interface Labelled {
    String getLabel();
    String getCssClass();
}
