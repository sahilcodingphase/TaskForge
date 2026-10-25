package com.sahil.taskforge.task.dto;

import com.sahil.taskforge.task.Task;
import com.sahil.taskforge.task.TaskPriority;
import com.sahil.taskforge.task.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse(Long id, String title, String description, TaskStatus status,
                           TaskPriority priority, LocalDate dueDate,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(task.getId(), task.getTitle(), task.getDescription(), task.getStatus(),
                task.getPriority(), task.getDueDate(), task.getCreatedAt(), task.getUpdatedAt());
    }
}
