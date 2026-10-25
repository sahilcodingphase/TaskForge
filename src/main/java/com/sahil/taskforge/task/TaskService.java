package com.sahil.taskforge.task;

import com.sahil.taskforge.task.dto.TaskRequest;
import com.sahil.taskforge.task.dto.TaskPageResponse;
import com.sahil.taskforge.task.dto.TaskResponse;
import com.sahil.taskforge.task.exception.InvalidTaskQueryException;
import com.sahil.taskforge.task.exception.TaskNotFoundException;
import com.sahil.taskforge.task.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

@Service
@Transactional
public class TaskService {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "dueDate", "title", "priority");
    private static final int MAX_PAGE_SIZE = 100;
    private final TaskRepository repository;

    public TaskService(TaskRepository repository) { this.repository = repository; }

    public TaskResponse create(TaskRequest request) {
        Task task = new Task();
        apply(task, request);
        return TaskResponse.from(repository.save(task));
    }

    @Transactional(readOnly = true)
    public TaskPageResponse findPage(int page, int size, TaskStatus status, String sort, String direction) {
        if (page < 0) throw new InvalidTaskQueryException("page", "Page must be zero or greater");
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidTaskQueryException("size", "Size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (!ALLOWED_SORT_FIELDS.contains(sort)) {
            throw new InvalidTaskQueryException("sort", "Sort must be one of: createdAt, dueDate, title, priority");
        }

        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException exception) {
            throw new InvalidTaskQueryException("direction", "Direction must be ASC or DESC");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Page<Task> result = status == null
                ? repository.findAll(pageable)
                : repository.findByStatus(status, pageable);
        return TaskPageResponse.from(result.map(TaskResponse::from));
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(Long id) { return TaskResponse.from(getTask(id)); }

    public TaskResponse update(Long id, TaskRequest request) {
        Task task = getTask(id);
        apply(task, request);
        return TaskResponse.from(repository.save(task));
    }

    public void delete(Long id) { repository.delete(getTask(id)); }

    private Task getTask(Long id) {
        return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    private void apply(Task task, TaskRequest request) {
        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setStatus(request.status() == null ? TaskStatus.TODO : request.status());
        task.setPriority(request.priority() == null ? TaskPriority.MEDIUM : request.priority());
        task.setDueDate(request.dueDate());
    }
}
