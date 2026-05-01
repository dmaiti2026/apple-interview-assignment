package com.apple.taskmanager.service;

import com.apple.taskmanager.dto.ApprovalRequest;
import com.apple.taskmanager.dto.TaskRequest;
import com.apple.taskmanager.dto.TaskResponse;
import com.apple.taskmanager.entity.*;
import com.apple.taskmanager.repository.TaskRepository;
import com.apple.taskmanager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    public TaskResponse createTask(TaskRequest request, String creatorUsername) {
        User creator = userRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new RuntimeException("Creator not found"));

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDateTime(LocalDateTime.parse(request.getDateTime()));
        task.setPriority(request.getPriority());
        task.setStatus(TaskStatus.PENDING);
        task.setCreatedBy(creator);

        if (request.getAssignedUserId() != null) {
            User assignedUser = userRepository.findById(request.getAssignedUserId())
                    .orElseThrow(() -> new RuntimeException("Assigned user not found"));
            task.setAssignedUser(assignedUser);
        }

        Task saved = taskRepository.save(task);

        // Notification simulation
        System.out.println("[NOTIFICATION] New task created: '" + saved.getTitle() +
                "' (ID: " + saved.getId() + ") by " + creator.getName() +
                " | Status: PENDING | Priority: " + saved.getPriority());

        return TaskResponse.from(saved);
    }

    public List<TaskResponse> getTasks(String status, String sortBy, String order, Long assignedUserId) {
        TaskStatus taskStatus = null;
        if (status != null && !status.isBlank()) {
            taskStatus = TaskStatus.valueOf(status.toUpperCase());
        }

        List<Task> tasks = taskRepository.findWithFilters(taskStatus, assignedUserId);

        // Sorting
        Comparator<Task> comparator;
        if ("priority".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparingInt(t -> priorityOrder(t.getPriority()));
        } else {
            comparator = Comparator.comparing(Task::getDateTime, Comparator.nullsLast(Comparator.naturalOrder()));
        }

        if ("desc".equalsIgnoreCase(order)) {
            comparator = comparator.reversed();
        }

        return tasks.stream()
                .sorted(comparator)
                .map(TaskResponse::from)
                .collect(Collectors.toList());
    }

    public TaskResponse approveOrReject(Long taskId, ApprovalRequest request, String reviewerUsername) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        if (task.getStatus() != TaskStatus.PENDING) {
            throw new IllegalStateException("Task is not in PENDING state. Current status: " + task.getStatus());
        }

        if (request.getStatus() != TaskStatus.APPROVED && request.getStatus() != TaskStatus.REJECTED) {
            throw new IllegalArgumentException("Status must be APPROVED or REJECTED");
        }

        User reviewer = userRepository.findByUsername(reviewerUsername)
                .orElseThrow(() -> new RuntimeException("Reviewer not found"));

        TaskStatus oldStatus = task.getStatus();
        task.setStatus(request.getStatus());
        task.setComments(request.getComments());
        task.setReviewedBy(reviewer);
        task.setReviewedDate(LocalDateTime.now());

        Task saved = taskRepository.save(task);

        // Notification simulation
        System.out.println("[NOTIFICATION] Task '" + saved.getTitle() + "' (ID: " + saved.getId() + ") " +
                oldStatus + " -> " + saved.getStatus() +
                " | Reviewed by: " + reviewer.getName() +
                (request.getComments() != null && !request.getComments().isBlank()
                        ? " | Comments: " + request.getComments() : ""));

        // Notify assigned user
        if (saved.getAssignedUser() != null) {
            System.out.println("[NOTIFICATION -> " + saved.getAssignedUser().getName() + "] " +
                    "Your task '" + saved.getTitle() + "' has been " + saved.getStatus());
        }

        return TaskResponse.from(saved);
    }

    public String exportToCsv(String status) {
        List<Task> tasks;
        if (status != null && !status.isBlank()) {
            tasks = taskRepository.findByStatus(TaskStatus.valueOf(status.toUpperCase()));
        } else {
            tasks = taskRepository.findAll();
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Title,Description,DateTime,Priority,Status,AssignedTo,CreatedBy,CreatedDate,Comments\n");

        for (Task t : tasks) {
            sb.append(escapeCsv(String.valueOf(t.getId()))).append(",");
            sb.append(escapeCsv(t.getTitle())).append(",");
            sb.append(escapeCsv(t.getDescription())).append(",");
            sb.append(escapeCsv(t.getDateTime() != null ? t.getDateTime().format(fmt) : "")).append(",");
            sb.append(escapeCsv(t.getPriority() != null ? t.getPriority().name() : "")).append(",");
            sb.append(escapeCsv(t.getStatus() != null ? t.getStatus().name() : "")).append(",");
            sb.append(escapeCsv(t.getAssignedUser() != null ? t.getAssignedUser().getName() : "")).append(",");
            sb.append(escapeCsv(t.getCreatedBy() != null ? t.getCreatedBy().getName() : "")).append(",");
            sb.append(escapeCsv(t.getCreatedDate() != null ? t.getCreatedDate().format(fmt) : "")).append(",");
            sb.append(escapeCsv(t.getComments())).append("\n");
        }

        return sb.toString();
    }

    public java.util.Map<String, Long> getStats() {
        java.util.Map<String, Long> stats = new java.util.LinkedHashMap<>();
        stats.put("total", taskRepository.count());
        stats.put("pending", taskRepository.countByStatus(TaskStatus.PENDING));
        stats.put("approved", taskRepository.countByStatus(TaskStatus.APPROVED));
        stats.put("rejected", taskRepository.countByStatus(TaskStatus.REJECTED));
        return stats;
    }

    private int priorityOrder(Priority p) {
        return switch (p) {
            case CRITICAL -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
        };
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
