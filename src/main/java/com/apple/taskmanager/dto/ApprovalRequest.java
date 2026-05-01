package com.apple.taskmanager.dto;

import com.apple.taskmanager.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

public class ApprovalRequest {

    @NotNull(message = "Status is required")
    private TaskStatus status; // APPROVED or REJECTED

    private String comments;

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
}
