package com.bugtracker.repository;

import com.bugtracker.entity.BugTicket;
import com.bugtracker.entity.TicketHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {
    List<TicketHistory> findByTicketOrderByIdDesc(BugTicket ticket);
    long countByTicket(BugTicket ticket);
    void deleteByTicket(BugTicket ticket);
}
