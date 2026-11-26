package com.bugtracker.dto;

/** One bar of the "distribution" charts on the dashboard. */
public class DistributionItem {
    private final String label;
    private final String cssClass;
    private final long count;
    private final int percent;

    public DistributionItem(String label, String cssClass, long count, int percent) {
        this.label = label;
        this.cssClass = cssClass;
        this.count = count;
        this.percent = percent;
    }

    public String getLabel() { return label; }
    public String getCssClass() { return cssClass; }
    public long getCount() { return count; }
    public int getPercent() { return percent; }
}
