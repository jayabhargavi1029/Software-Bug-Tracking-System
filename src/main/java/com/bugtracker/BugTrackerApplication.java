package com.bugtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point of the Software Bug and Ticket Tracking System. */
@SpringBootApplication
public class BugTrackerApplication {
    public static void main(String[] args) {
        SpringApplication.run(BugTrackerApplication.class, args);
    }
}
