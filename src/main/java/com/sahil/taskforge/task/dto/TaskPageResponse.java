package com.sahil.taskforge.task.dto;

import org.springframework.data.domain.Page;
import java.util.List;

public record TaskPageResponse(List<TaskResponse> content, int page, int size,
                               long totalElements, int totalPages,
                               boolean first, boolean last) {
    public static TaskPageResponse from(Page<TaskResponse> result) {
        return new TaskPageResponse(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isFirst(), result.isLast());
    }
}
