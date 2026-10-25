package com.sahil.taskforge.task;

import com.sahil.taskforge.task.dto.TaskRequest;
import com.sahil.taskforge.task.dto.TaskPageResponse;
import com.sahil.taskforge.task.dto.TaskResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService service;

    public TaskController(TaskService service) { this.service = service; }

    @PostMapping
    @Operation(summary = "Create a task")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Task created"),
            @ApiResponse(responseCode = "400", description = "Invalid task data")
    })
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@Valid @RequestBody TaskRequest request) { return service.create(request); }

    @GetMapping
    @Operation(summary = "List tasks", description = "Returns a paginated task list with optional status filtering and sorting.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of tasks returned"),
            @ApiResponse(responseCode = "400", description = "Invalid query parameter")
    })
    public TaskPageResponse findAll(
            @Parameter(name = "page", in = ParameterIn.QUERY, description = "Zero-based page index", schema = @Schema(defaultValue = "0", minimum = "0"))
            @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(name = "size", in = ParameterIn.QUERY, description = "Page size from 1 to 100", schema = @Schema(defaultValue = "10", minimum = "1", maximum = "100"))
            @RequestParam(name = "size", defaultValue = "10") int size,
            @Parameter(name = "status", in = ParameterIn.QUERY, description = "Filter tasks by status")
            @RequestParam(name = "status", required = false) TaskStatus status,
            @Parameter(name = "sort", in = ParameterIn.QUERY, description = "Sort field", schema = @Schema(defaultValue = "createdAt", allowableValues = {"createdAt", "dueDate", "title", "priority"}))
            @RequestParam(name = "sort", defaultValue = "createdAt") String sort,
            @Parameter(name = "direction", in = ParameterIn.QUERY, description = "Sort direction", schema = @Schema(defaultValue = "DESC", allowableValues = {"ASC", "DESC"}))
            @RequestParam(name = "direction", defaultValue = "DESC") String direction) {
        return service.findPage(page, size, status, sort, direction);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a task by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task returned"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public TaskResponse findById(@PathVariable Long id) { return service.findById(id); }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a task")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task updated"),
            @ApiResponse(responseCode = "400", description = "Invalid task data"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Task deleted"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}
