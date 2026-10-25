package com.sahil.taskforge.task.dto;

import com.sahil.taskforge.task.TaskPriority;
import com.sahil.taskforge.task.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TaskRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,
        @Size(max = 5000, message = "Description must be at most 5000 characters")
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate
) { }
