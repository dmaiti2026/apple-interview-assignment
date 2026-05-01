package com.apple.taskmanager.repository;

import com.apple.taskmanager.entity.Task;
import com.apple.taskmanager.entity.TaskStatus;
import com.apple.taskmanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByStatus(TaskStatus status);

    List<Task> findByAssignedUser(User user);

    List<Task> findByCreatedBy(User user);

    long countByStatus(TaskStatus status);

    @Query("SELECT t FROM Task t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:assignedUserId IS NULL OR t.assignedUser.id = :assignedUserId)")
    List<Task> findWithFilters(
        @Param("status") TaskStatus status,
        @Param("assignedUserId") Long assignedUserId
    );
}
