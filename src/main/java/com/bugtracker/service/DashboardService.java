package com.bugtracker.service;

import com.bugtracker.dto.DashboardStats;
import com.bugtracker.dto.DistributionItem;
import com.bugtracker.entity.Labelled;
import com.bugtracker.entity.Priority;
import com.bugtracker.entity.Status;
import com.bugtracker.entity.User;
import com.bugtracker.repository.BugTicketRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Calculates dashboard statistics directly from the database. */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final BugTicketRepository tickets;

    public DashboardService(BugTicketRepository tickets) {
        this.tickets = tickets;
    }

    /** Statistics for all tickets (admin dashboard). */
    public DashboardStats adminStats() {
        DashboardStats stats = new DashboardStats();
        long total = tickets.count();
        stats.setTotal(total);
        stats.setOpen(tickets.countByStatus(Status.OPEN) + tickets.countByStatus(Status.REOPENED));
        stats.setInProgress(tickets.countByStatus(Status.IN_PROGRESS));
        stats.setResolved(tickets.countByStatus(Status.RESOLVED));
        stats.setClosed(tickets.countByStatus(Status.CLOSED));
        stats.setCritical(tickets.countByPriority(Priority.CRITICAL));

        List<DistributionItem> byStatus = new ArrayList<>();
        for (Status s : Status.values()) {
            byStatus.add(item(s, tickets.countByStatus(s), total));
        }
        List<DistributionItem> byPriority = new ArrayList<>();
        for (Priority p : Priority.values()) {
            byPriority.add(item(p, tickets.countByPriority(p), total));
        }
        stats.setByStatus(byStatus);
        stats.setByPriority(byPriority);
        stats.setRecent(tickets.findTop5ByOrderByIdDesc());
        return stats;
    }

    /** Statistics for the tickets assigned to one developer. */
    public DashboardStats developerStats(User developer) {
        DashboardStats stats = new DashboardStats();
        long total = tickets.countByAssignedTo(developer);
        stats.setTotal(total);
        stats.setOpen(tickets.countByAssignedToAndStatus(developer, Status.OPEN)
                + tickets.countByAssignedToAndStatus(developer, Status.REOPENED));
        stats.setInProgress(tickets.countByAssignedToAndStatus(developer, Status.IN_PROGRESS));
        stats.setResolved(tickets.countByAssignedToAndStatus(developer, Status.RESOLVED));
        stats.setClosed(tickets.countByAssignedToAndStatus(developer, Status.CLOSED));
        stats.setCritical(tickets.countByAssignedToAndPriority(developer, Priority.CRITICAL));
        stats.setRecent(tickets.findByAssignedToOrderByIdDesc(developer));
        stats.setReported(tickets.findByReportedByOrderByIdDesc(developer));
        return stats;
    }

    private DistributionItem item(Labelled value, long count, long total) {
        int percent = total == 0 ? 0 : (int) Math.round(count * 100.0 / total);
        return new DistributionItem(value.getLabel(), value.getCssClass(), count, percent);
    }
}
