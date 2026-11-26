package com.bugtracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bugtracker.entity.Status;
import org.junit.jupiter.api.Test;

/** Plain unit test (no Spring, no database) for the status workflow rules. */
class StatusTransitionTest {

    @Test
    void openTicketCanBeStartedButNotReopened() {
        assertTrue(Status.OPEN.canMoveTo(Status.IN_PROGRESS));
        assertFalse(Status.OPEN.canMoveTo(Status.REOPENED));
    }

    @Test
    void closedTicketCanOnlyBeReopened() {
        assertEquals(1, Status.CLOSED.allowedNext().size());
        assertTrue(Status.CLOSED.canMoveTo(Status.REOPENED));
    }

    @Test
    void resolvedTicketCanBeClosedOrReopened() {
        assertTrue(Status.RESOLVED.canMoveTo(Status.CLOSED));
        assertTrue(Status.RESOLVED.canMoveTo(Status.REOPENED));
        assertFalse(Status.RESOLVED.canMoveTo(Status.IN_PROGRESS));
    }

    @Test
    void statusHasReadableLabelAndCssClass() {
        assertEquals("In Progress", Status.IN_PROGRESS.getLabel());
        assertEquals("in-progress", Status.IN_PROGRESS.getCssClass());
    }
}
