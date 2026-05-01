package com.apple.taskmanager.service;

import com.apple.taskmanager.dto.ApprovalRequest;
import com.apple.taskmanager.dto.TaskRequest;
import com.apple.taskmanager.dto.TaskResponse;
import com.apple.taskmanager.entity.*;
import com.apple.taskmanager.repository.TaskRepository;
import com.apple.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskService taskService;

    private User creator;
    private User manager;
    private User assignedUser;

    @BeforeEach
    void setUp() {
        creator = new User("user1", "pass", "Alice Smith", "alice@example.com", Role.USER);
        creator.setId(1L);

        manager = new User("manager1", "pass", "Sarah Johnson", "sarah@example.com", Role.MANAGER);
        manager.setId(2L);

        assignedUser = new User("user2", "pass", "Bob Jones", "bob@example.com", Role.USER);
        assignedUser.setId(3L);
    }

    // -------------------------------------------------------------------------
    // createTask
    // -------------------------------------------------------------------------

    @Test
    void createTask_savesTaskWithPendingStatus() {
        TaskRequest req = buildTaskRequest("Q2 Review", Priority.HIGH, null);
        Task saved = buildTask(1L, "Q2 Review", Priority.HIGH, TaskStatus.PENDING, creator, null);

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(creator));
        when(taskRepository.save(any(Task.class))).thenReturn(saved);

        TaskResponse response = taskService.createTask(req, "user1");

        assertThat(response.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(response.getTitle()).isEqualTo("Q2 Review");
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void createTask_withAssignedUser_setsAssignedUser() {
        TaskRequest req = buildTaskRequest("Design Sprint", Priority.MEDIUM, assignedUser.getId());
        Task saved = buildTask(2L, "Design Sprint", Priority.MEDIUM, TaskStatus.PENDING, creator, assignedUser);

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(creator));
        when(userRepository.findById(assignedUser.getId())).thenReturn(Optional.of(assignedUser));
        when(taskRepository.save(any(Task.class))).thenReturn(saved);

        TaskResponse response = taskService.createTask(req, "user1");

        assertThat(response.getAssignedUserName()).isEqualTo("Bob Jones");
    }

    @Test
    void createTask_unknownCreator_throwsRuntimeException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        TaskRequest req = buildTaskRequest("Task X", Priority.LOW, null);

        assertThatThrownBy(() -> taskService.createTask(req, "ghost"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Creator not found");
    }

    @Test
    void createTask_unknownAssignedUser_throwsRuntimeException() {
        TaskRequest req = buildTaskRequest("Task Y", Priority.LOW, 99L);

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(creator));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.createTask(req, "user1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Assigned user not found");
    }

    // -------------------------------------------------------------------------
    // getTasks
    // -------------------------------------------------------------------------

    @Test
    void getTasks_noFilters_returnsAllTasksSortedByDateDesc() {
        Task t1 = buildTask(1L, "Older", Priority.LOW, TaskStatus.PENDING, creator, null);
        t1.setDateTime(LocalDateTime.of(2025, 1, 1, 9, 0));

        Task t2 = buildTask(2L, "Newer", Priority.HIGH, TaskStatus.APPROVED, creator, null);
        t2.setDateTime(LocalDateTime.of(2025, 6, 1, 9, 0));

        when(taskRepository.findWithFilters(null, null)).thenReturn(List.of(t1, t2));

        List<TaskResponse> result = taskService.getTasks(null, "date", "desc", null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("Newer");
        assertThat(result.get(1).getTitle()).isEqualTo("Older");
    }

    @Test
    void getTasks_filterByStatus_passesStatusToRepository() {
        when(taskRepository.findWithFilters(TaskStatus.PENDING, null)).thenReturn(List.of());

        taskService.getTasks("PENDING", "date", "asc", null);

        verify(taskRepository).findWithFilters(TaskStatus.PENDING, null);
    }

    @Test
    void getTasks_filterByAssignedUserId_passesIdToRepository() {
        when(taskRepository.findWithFilters(null, 3L)).thenReturn(List.of());

        taskService.getTasks(null, "date", "asc", 3L);

        verify(taskRepository).findWithFilters(null, 3L);
    }

    @Test
    void getTasks_sortByPriorityDesc_returnsLowestPriorityFirst() {
        // priorityOrder: CRITICAL=0, HIGH=1, MEDIUM=2, LOW=3
        // asc  → CRITICAL first (lowest order value)
        // desc → reversed → LOW first (highest order value)
        Task low = buildTask(1L, "Low Task", Priority.LOW, TaskStatus.PENDING, creator, null);
        Task critical = buildTask(2L, "Critical Task", Priority.CRITICAL, TaskStatus.PENDING, creator, null);
        Task medium = buildTask(3L, "Medium Task", Priority.MEDIUM, TaskStatus.PENDING, creator, null);

        when(taskRepository.findWithFilters(null, null)).thenReturn(List.of(low, critical, medium));

        List<TaskResponse> result = taskService.getTasks(null, "priority", "desc", null);

        assertThat(result.get(0).getTitle()).isEqualTo("Low Task");
        assertThat(result.get(1).getTitle()).isEqualTo("Medium Task");
        assertThat(result.get(2).getTitle()).isEqualTo("Critical Task");
    }

    @Test
    void getTasks_sortByPriorityAsc_returnsCriticalFirst() {
        Task low = buildTask(1L, "Low Task", Priority.LOW, TaskStatus.PENDING, creator, null);
        Task critical = buildTask(2L, "Critical Task", Priority.CRITICAL, TaskStatus.PENDING, creator, null);

        when(taskRepository.findWithFilters(null, null)).thenReturn(List.of(low, critical));

        List<TaskResponse> result = taskService.getTasks(null, "priority", "asc", null);

        assertThat(result.get(0).getTitle()).isEqualTo("Critical Task");
        assertThat(result.get(1).getTitle()).isEqualTo("Low Task");
    }

    @Test
    void getTasks_invalidStatus_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> taskService.getTasks("INVALID", "date", "asc", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // -------------------------------------------------------------------------
    // approveOrReject
    // -------------------------------------------------------------------------

    @Test
    void approveOrReject_approvePendingTask_returnsApprovedResponse() {
        Task task = buildTask(1L, "Q2 Review", Priority.HIGH, TaskStatus.PENDING, creator, assignedUser);

        ApprovalRequest req = new ApprovalRequest();
        req.setStatus(TaskStatus.APPROVED);
        req.setComments("Looks great!");

        Task approved = buildTask(1L, "Q2 Review", Priority.HIGH, TaskStatus.APPROVED, creator, assignedUser);
        approved.setReviewedBy(manager);
        approved.setComments("Looks great!");

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(manager));
        when(taskRepository.save(any(Task.class))).thenReturn(approved);

        TaskResponse response = taskService.approveOrReject(1L, req, "manager1");

        assertThat(response.getStatus()).isEqualTo(TaskStatus.APPROVED);
        assertThat(response.getComments()).isEqualTo("Looks great!");
    }

    @Test
    void approveOrReject_rejectPendingTask_returnsRejectedResponse() {
        Task task = buildTask(1L, "Bug Fix", Priority.MEDIUM, TaskStatus.PENDING, creator, null);

        ApprovalRequest req = new ApprovalRequest();
        req.setStatus(TaskStatus.REJECTED);
        req.setComments("Needs more info");

        Task rejected = buildTask(1L, "Bug Fix", Priority.MEDIUM, TaskStatus.REJECTED, creator, null);
        rejected.setReviewedBy(manager);
        rejected.setComments("Needs more info");

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(manager));
        when(taskRepository.save(any(Task.class))).thenReturn(rejected);

        TaskResponse response = taskService.approveOrReject(1L, req, "manager1");

        assertThat(response.getStatus()).isEqualTo(TaskStatus.REJECTED);
    }

    @Test
    void approveOrReject_taskNotPending_throwsIllegalStateException() {
        Task task = buildTask(1L, "Already Done", Priority.LOW, TaskStatus.APPROVED, creator, null);

        ApprovalRequest req = new ApprovalRequest();
        req.setStatus(TaskStatus.APPROVED);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.approveOrReject(1L, req, "manager1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not in PENDING state");
    }

    @Test
    void approveOrReject_taskNotFound_throwsRuntimeException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        ApprovalRequest req = new ApprovalRequest();
        req.setStatus(TaskStatus.APPROVED);

        assertThatThrownBy(() -> taskService.approveOrReject(99L, req, "manager1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Task not found: 99");
    }

    @Test
    void approveOrReject_reviewerNotFound_throwsRuntimeException() {
        Task task = buildTask(1L, "Task", Priority.HIGH, TaskStatus.PENDING, creator, null);

        ApprovalRequest req = new ApprovalRequest();
        req.setStatus(TaskStatus.APPROVED);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.approveOrReject(1L, req, "ghost"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Reviewer not found");
    }

    @Test
    void approveOrReject_statusIsPending_throwsIllegalArgumentException() {
        Task task = buildTask(1L, "Task", Priority.HIGH, TaskStatus.PENDING, creator, null);

        ApprovalRequest req = new ApprovalRequest();
        req.setStatus(TaskStatus.PENDING); // invalid for approval — check fires before reviewer lookup

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.approveOrReject(1L, req, "manager1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("APPROVED or REJECTED");
    }

    @Test
    void approveOrReject_stampsReviewedByAndReviewedDate() {
        Task task = buildTask(1L, "Task", Priority.HIGH, TaskStatus.PENDING, creator, null);

        ApprovalRequest req = new ApprovalRequest();
        req.setStatus(TaskStatus.APPROVED);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(manager));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        taskService.approveOrReject(1L, req, "manager1");

        verify(taskRepository).save(argThat(t ->
                t.getReviewedBy() == manager && t.getReviewedDate() != null
        ));
    }

    // -------------------------------------------------------------------------
    // getStats
    // -------------------------------------------------------------------------

    @Test
    void getStats_returnsCorrectCounts() {
        when(taskRepository.count()).thenReturn(10L);
        when(taskRepository.countByStatus(TaskStatus.PENDING)).thenReturn(4L);
        when(taskRepository.countByStatus(TaskStatus.APPROVED)).thenReturn(5L);
        when(taskRepository.countByStatus(TaskStatus.REJECTED)).thenReturn(1L);

        Map<String, Long> stats = taskService.getStats();

        assertThat(stats.get("total")).isEqualTo(10L);
        assertThat(stats.get("pending")).isEqualTo(4L);
        assertThat(stats.get("approved")).isEqualTo(5L);
        assertThat(stats.get("rejected")).isEqualTo(1L);
    }

    // -------------------------------------------------------------------------
    // exportToCsv
    // -------------------------------------------------------------------------

    @Test
    void exportToCsv_noFilter_includesAllTasksWithHeader() {
        Task task = buildTask(1L, "My Task", Priority.HIGH, TaskStatus.PENDING, creator, null);
        task.setDateTime(LocalDateTime.of(2025, 3, 15, 10, 30));
        task.setCreatedDate(LocalDateTime.of(2025, 3, 14, 9, 0));

        when(taskRepository.findAll()).thenReturn(List.of(task));

        String csv = taskService.exportToCsv(null);

        assertThat(csv).startsWith("ID,Title,Description,DateTime,Priority,Status,AssignedTo,CreatedBy,CreatedDate,Comments");
        assertThat(csv).contains("My Task");
        assertThat(csv).contains("HIGH");
        assertThat(csv).contains("PENDING");
        assertThat(csv).contains("Alice Smith");
    }

    @Test
    void exportToCsv_withStatusFilter_onlyCallsFindByStatus() {
        when(taskRepository.findByStatus(TaskStatus.APPROVED)).thenReturn(List.of());

        taskService.exportToCsv("APPROVED");

        verify(taskRepository).findByStatus(TaskStatus.APPROVED);
        verify(taskRepository, never()).findAll();
    }

    @Test
    void exportToCsv_titleWithComma_isQuotedCorrectly() {
        Task task = buildTask(1L, "Fix, the bug", Priority.LOW, TaskStatus.PENDING, creator, null);
        task.setDateTime(LocalDateTime.now());
        task.setCreatedDate(LocalDateTime.now());

        when(taskRepository.findAll()).thenReturn(List.of(task));

        String csv = taskService.exportToCsv(null);

        assertThat(csv).contains("\"Fix, the bug\"");
    }

    @Test
    void exportToCsv_titleWithQuote_isEscapedCorrectly() {
        Task task = buildTask(1L, "It's a \"test\"", Priority.LOW, TaskStatus.PENDING, creator, null);
        task.setDateTime(LocalDateTime.now());
        task.setCreatedDate(LocalDateTime.now());

        when(taskRepository.findAll()).thenReturn(List.of(task));

        String csv = taskService.exportToCsv(null);

        assertThat(csv).contains("\"It's a \"\"test\"\"\"");
    }

    @Test
    void exportToCsv_blankStatus_returnsAllTasks() {
        when(taskRepository.findAll()).thenReturn(List.of());

        taskService.exportToCsv("  ");

        verify(taskRepository).findAll();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private TaskRequest buildTaskRequest(String title, Priority priority, Long assignedUserId) {
        TaskRequest req = new TaskRequest();
        req.setTitle(title);
        req.setDateTime("2025-06-01T10:00:00");
        req.setPriority(priority);
        req.setAssignedUserId(assignedUserId);
        return req;
    }

    private Task buildTask(Long id, String title, Priority priority, TaskStatus status, User createdBy, User assignedUser) {
        Task task = new Task();
        task.setId(id);
        task.setTitle(title);
        task.setPriority(priority);
        task.setStatus(status);
        task.setCreatedBy(createdBy);
        task.setAssignedUser(assignedUser);
        task.setDateTime(LocalDateTime.of(2025, 6, 1, 10, 0));
        task.setCreatedDate(LocalDateTime.of(2025, 5, 30, 8, 0));
        return task;
    }
}