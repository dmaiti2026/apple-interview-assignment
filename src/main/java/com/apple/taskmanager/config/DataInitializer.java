package com.apple.taskmanager.config;

import com.apple.taskmanager.entity.*;
import com.apple.taskmanager.repository.TaskRepository;
import com.apple.taskmanager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Create users
        User admin = createUser("admin", "admin123", "Dipankar Maiti", "admin@apple.com", Role.ADMIN);
        User manager1 = createUser("manager1", "manager123", "Sarah Johnson", "sarah@apple.com", Role.MANAGER);
        User manager2 = createUser("manager2", "manager123", "Tom Williams", "tom@apple.com", Role.MANAGER);
        User user1 = createUser("user1", "user123", "Alice Smith", "alice@apple.com", Role.USER);
        User user2 = createUser("user2", "user123", "Bob Chen", "bob@apple.com", Role.USER);
        User user3 = createUser("user3", "user123", "Carol Davis", "carol@apple.com", Role.USER);

        // Create sample tasks
        createTask("Q2 Product Review", "Complete the quarterly product performance review and prepare slides for exec presentation.",
                LocalDateTime.now().plusDays(3), Priority.HIGH, TaskStatus.PENDING, user1, manager1, null);

        createTask("Onboard New Engineers", "Prepare onboarding documentation and schedule sessions for new team members joining next month.",
                LocalDateTime.now().plusDays(7), Priority.MEDIUM, TaskStatus.PENDING, user2, manager1, null);

        createTask("Security Audit", "Conduct comprehensive security audit of all production APIs and services.",
                LocalDateTime.now().plusDays(14), Priority.CRITICAL, TaskStatus.PENDING, user3, manager2, null);

        createTask("Design System Update", "Update the component library to reflect latest design guidelines.",
                LocalDateTime.now().minusDays(5), Priority.MEDIUM, TaskStatus.APPROVED, user1, manager1, manager1);

        createTask("Annual Budget Planning", "Compile team budget requirements for fiscal year planning.",
                LocalDateTime.now().minusDays(10), Priority.HIGH, TaskStatus.APPROVED, user2, manager2, manager2);

        createTask("Legacy API Migration", "Migrate deprecated v1 endpoints to v3 REST API.",
                LocalDateTime.now().minusDays(3), Priority.HIGH, TaskStatus.REJECTED, user3, manager1, manager1);

        createTask("Customer Feedback Analysis", "Analyze Q1 NPS results and identify top improvement areas.",
                LocalDateTime.now().plusDays(2), Priority.MEDIUM, TaskStatus.PENDING, user1, manager2, null);

        createTask("Infrastructure Cost Review", "Review cloud infrastructure spend and identify optimization opportunities.",
                LocalDateTime.now().plusDays(5), Priority.LOW, TaskStatus.PENDING, user2, manager1, null);

        createTask("Accessibility Compliance", "Ensure all web properties meet WCAG 2.1 AA standards.",
                LocalDateTime.now().minusDays(1), Priority.HIGH, TaskStatus.APPROVED, user3, manager2, manager2);

        createTask("Team Performance Reviews", "Complete mid-year performance review cycle for all direct reports.",
                LocalDateTime.now().plusDays(30), Priority.MEDIUM, TaskStatus.PENDING, user2, manager1, null);

        System.out.println("==================================================");
        System.out.println(" Task Manager - Sample Data Initialized");
        System.out.println("==================================================");
        System.out.println(" Users:");
        System.out.println("   admin     / admin123   (ADMIN)");
        System.out.println("   manager1  / manager123 (MANAGER)");
        System.out.println("   manager2  / manager123 (MANAGER)");
        System.out.println("   user1     / user123    (USER)");
        System.out.println("   user2     / user123    (USER)");
        System.out.println("   user3     / user123    (USER)");
        System.out.println("--------------------------------------------------");
        System.out.println(" App running at: http://localhost:8080");
        System.out.println(" H2 Console at:  http://localhost:8080/h2-console");
        System.out.println("==================================================");
    }

    private User createUser(String username, String rawPassword, String name, String email, Role role) {
        User user = new User(username, passwordEncoder.encode(rawPassword), name, email, role);
        return userRepository.save(user);
    }

    private Task createTask(String title, String description, LocalDateTime dateTime,
                             Priority priority, TaskStatus status,
                             User createdBy, User assignedUser, User reviewedBy) {
        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setDateTime(dateTime);
        task.setPriority(priority);
        task.setStatus(status);
        task.setCreatedBy(createdBy);
        task.setAssignedUser(assignedUser);

        if (reviewedBy != null) {
            task.setReviewedBy(reviewedBy);
            task.setReviewedDate(LocalDateTime.now().minusDays(1));
            if (status == TaskStatus.REJECTED) {
                task.setComments("Does not meet current technical requirements. Please revise approach.");
            } else {
                task.setComments("Looks good, proceed.");
            }
        }

        return taskRepository.save(task);
    }
}
