package com.apple.taskmanager.dto;

import com.apple.taskmanager.entity.Priority;
import com.apple.taskmanager.entity.Task;
import com.apple.taskmanager.entity.TaskStatus;

import java.time.format.DateTimeFormatter;

public class TaskResponse {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private Long id;
    private String title;
    private String description;
    private String dateTime;
    private Priority priority;
    private TaskStatus status;
    private Long assignedUserId;
    private String assignedUserName;
    private Long createdById;
    private String createdByName;
    private String createdDate;
    private String comments;
    private String reviewedByName;
    private String reviewedDate;

    public static TaskResponse from(Task task) {
        TaskResponse r = new TaskResponse();
        r.id = task.getId();
        r.title = task.getTitle();
        r.description = task.getDescription();
        r.dateTime = task.getDateTime() != null ? task.getDateTime().format(FORMATTER) : null;
        r.priority = task.getPriority();
        r.status = task.getStatus();
        r.createdDate = task.getCreatedDate() != null ? task.getCreatedDate().format(FORMATTER) : null;
        r.comments = task.getComments();

        if (task.getAssignedUser() != null) {
            r.assignedUserId = task.getAssignedUser().getId();
            r.assignedUserName = task.getAssignedUser().getName();
        }
        if (task.getCreatedBy() != null) {
            r.createdById = task.getCreatedBy().getId();
            r.createdByName = task.getCreatedBy().getName();
        }
        if (task.getReviewedBy() != null) {
            r.reviewedByName = task.getReviewedBy().getName();
        }
        if (task.getReviewedDate() != null) {
            r.reviewedDate = task.getReviewedDate().format(FORMATTER);
        }
        return r;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getDateTime() { return dateTime; }
    public Priority getPriority() { return priority; }
    public TaskStatus getStatus() { return status; }
    public Long getAssignedUserId() { return assignedUserId; }
    public String getAssignedUserName() { return assignedUserName; }
    public Long getCreatedById() { return createdById; }
    public String getCreatedByName() { return createdByName; }
    public String getCreatedDate() { return createdDate; }
    public String getComments() { return comments; }
    public String getReviewedByName() { return reviewedByName; }
    public String getReviewedDate() { return reviewedDate; }
}
