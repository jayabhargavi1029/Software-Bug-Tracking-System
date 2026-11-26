package com.bugtracker.repository;

import com.bugtracker.entity.BugTicket;
import com.bugtracker.entity.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByTicketOrderByCreatedAtAscIdAsc(BugTicket ticket);
    long countByTicket(BugTicket ticket);
    void deleteByTicket(BugTicket ticket);
}
