package com.apple.taskmanager.controller;

import com.apple.taskmanager.dto.ApprovalRequest;
import com.apple.taskmanager.dto.TaskRequest;
import com.apple.taskmanager.dto.TaskResponse;
import com.apple.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    /**
     * POST /api/tasks - Create a new task
     */
    @PostMapping
    public ResponseEntity<?> createTask(@Valid @RequestBody TaskRequest request, Principal principal) {
        try {
            TaskResponse response = taskService.createTask(request, principal.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/tasks - List tasks with optional filters and sorting
     * Query params: status, sortBy (date|priority), order (asc|desc), assignedUserId
     */
    @GetMapping
    public ResponseEntity<?> getTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "date") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String order,
            @RequestParam(required = false) Long assignedUserId,
            Principal principal) {
        try {
            List<TaskResponse> tasks = taskService.getTasks(status, sortBy, order, assignedUserId);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/tasks/{id}/approve - Approve or reject a task (MANAGER or ADMIN only)
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<?> approveTask(
            @PathVariable Long id,
            @Valid @RequestBody ApprovalRequest request,
            Principal principal) {
        try {
            TaskResponse response = taskService.approveOrReject(id, request, principal.getName());
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/tasks/stats - Dashboard statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(taskService.getStats());
    }

    /**
     * GET /api/tasks/export/csv - Export tasks as CSV (bonus feature)
     */
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) String status) {
        String csv = taskService.exportToCsv(status);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "tasks-export.csv");
        headers.setContentLength(bytes.length);

        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}
