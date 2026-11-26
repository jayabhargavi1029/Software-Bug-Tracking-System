package com.bugtracker.dto;

import com.bugtracker.entity.BugTicket;
import java.util.ArrayList;
import java.util.List;

/** All numbers and lists shown on a dashboard. Everything is calculated from the database. */
public class DashboardStats {
    private long total;
    private long open;
    private long inProgress;
    private long resolved;
    private long closed;
    private long critical;
    private List<DistributionItem> byStatus = new ArrayList<>();
    private List<DistributionItem> byPriority = new ArrayList<>();
    private List<BugTicket> recent = new ArrayList<>();
    private List<BugTicket> reported = new ArrayList<>();

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public long getOpen() { return open; }
    public void setOpen(long open) { this.open = open; }
    public long getInProgress() { return inProgress; }
    public void setInProgress(long inProgress) { this.inProgress = inProgress; }
    public long getResolved() { return resolved; }
    public void setResolved(long resolved) { this.resolved = resolved; }
    public long getClosed() { return closed; }
    public void setClosed(long closed) { this.closed = closed; }
    public long getCritical() { return critical; }
    public void setCritical(long critical) { this.critical = critical; }
    public List<DistributionItem> getByStatus() { return byStatus; }
    public void setByStatus(List<DistributionItem> byStatus) { this.byStatus = byStatus; }
    public List<DistributionItem> getByPriority() { return byPriority; }
    public void setByPriority(List<DistributionItem> byPriority) { this.byPriority = byPriority; }
    public List<BugTicket> getRecent() { return recent; }
    public void setRecent(List<BugTicket> recent) { this.recent = recent; }
    public List<BugTicket> getReported() { return reported; }
    public void setReported(List<BugTicket> reported) { this.reported = reported; }
}
