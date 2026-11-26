package com.bugtracker.repository;

import com.bugtracker.entity.BugTicket;
import com.bugtracker.entity.Priority;
import com.bugtracker.entity.Status;
import com.bugtracker.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BugTicketRepository extends JpaRepository<BugTicket, Long>, JpaSpecificationExecutor<BugTicket> {
    long countByStatus(Status status);
    long countByPriority(Priority priority);
    long countByAssignedTo(User user);
    long countByAssignedToAndStatus(User user, Status status);
    long countByAssignedToAndPriority(User user, Priority priority);
    List<BugTicket> findTop5ByOrderByIdDesc();
    List<BugTicket> findByAssignedToOrderByIdDesc(User user);
    List<BugTicket> findByReportedByOrderByIdDesc(User user);
}
