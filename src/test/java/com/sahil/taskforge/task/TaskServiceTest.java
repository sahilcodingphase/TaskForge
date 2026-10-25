package com.sahil.taskforge.task;

import com.sahil.taskforge.task.dto.TaskRequest;
import com.sahil.taskforge.task.dto.TaskPageResponse;
import com.sahil.taskforge.task.dto.TaskResponse;
import com.sahil.taskforge.task.exception.InvalidTaskQueryException;
import com.sahil.taskforge.task.exception.TaskNotFoundException;
import com.sahil.taskforge.task.repository.TaskRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    @Mock TaskRepository repository;
    @InjectMocks TaskService service;

    @Test
    void createAppliesDefaultStatusAndPriority() {
        when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        TaskRequest request = new TaskRequest("  Write tests  ", null, null, null, null);

        TaskResponse result = service.create(request);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(repository).save(captor.capture());
        assertEquals("Write tests", captor.getValue().getTitle());
        assertEquals(TaskStatus.TODO, captor.getValue().getStatus());
        assertEquals(TaskPriority.MEDIUM, captor.getValue().getPriority());
        assertEquals(TaskStatus.TODO, result.status());
        assertEquals(TaskPriority.MEDIUM, result.priority());
    }

    @Test
    void findByIdThrowsNotFoundForMissingTask() {
        when(repository.findById(42L)).thenReturn(Optional.empty());
        assertThrows(TaskNotFoundException.class, () -> service.findById(42L));
    }

    @Test
    void findPageUsesJpaPaginationAndSorting() {
        Task task = task("Existing task");
        Pageable requested = PageRequest.of(1, 3, Sort.by(Sort.Direction.ASC, "title"));
        when(repository.findAll(requested)).thenReturn(new PageImpl<>(List.of(task), requested, 7));

        TaskPageResponse result = service.findPage(1, 3, null, "title", "ASC");

        assertEquals("Existing task", result.content().get(0).title());
        assertEquals(1, result.page());
        assertEquals(3, result.size());
        assertEquals(7, result.totalElements());
        assertEquals(3, result.totalPages());
        verify(repository).findAll(requested);
    }

    @Test
    void findPageFiltersByStatusInRepository() {
        Task task = task("Done task");
        Pageable requested = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(repository.findByStatus(TaskStatus.DONE, requested))
                .thenReturn(new PageImpl<>(List.of(task), requested, 1));

        TaskPageResponse result = service.findPage(0, 10, TaskStatus.DONE, "createdAt", "DESC");

        assertEquals(1, result.totalElements());
        verify(repository).findByStatus(TaskStatus.DONE, requested);
        verify(repository, never()).findAll(any(Pageable.class));
    }

    @Test
    void findPageReturnsEmptyPageMetadata() {
        Pageable requested = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(repository.findAll(requested)).thenReturn(new PageImpl<>(List.of(), requested, 0));

        TaskPageResponse result = service.findPage(0, 10, null, "createdAt", "DESC");

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
        assertEquals(0, result.totalPages());
        assertTrue(result.first());
        assertTrue(result.last());
    }

    @Test
    void findPageRejectsOutOfRangePaginationAndUnsupportedSortOptions() {
        assertEquals("page", assertThrows(InvalidTaskQueryException.class,
                () -> service.findPage(-1, 10, null, "title", "ASC")).getField());
        assertEquals("size", assertThrows(InvalidTaskQueryException.class,
                () -> service.findPage(0, 101, null, "title", "ASC")).getField());
        assertEquals("size", assertThrows(InvalidTaskQueryException.class,
                () -> service.findPage(0, 0, null, "title", "ASC")).getField());
        assertEquals("sort", assertThrows(InvalidTaskQueryException.class,
                () -> service.findPage(0, 10, null, "id", "ASC")).getField());
        assertEquals("direction", assertThrows(InvalidTaskQueryException.class,
                () -> service.findPage(0, 10, null, "title", "sideways")).getField());
        verifyNoInteractions(repository);
    }

    @Test
    void findByIdReturnsExistingTask() {
        when(repository.findById(7L)).thenReturn(Optional.of(task("Existing task")));

        TaskResponse result = service.findById(7L);

        assertEquals("Existing task", result.title());
    }

    @Test
    void updateChangesExistingTaskFields() {
        Task task = task("Old title");
        when(repository.findById(7L)).thenReturn(Optional.of(task));
        when(repository.save(task)).thenReturn(task);

        TaskResponse result = service.update(7L,
                new TaskRequest("New title", "Updated description", TaskStatus.DONE, TaskPriority.HIGH, null));

        assertEquals("New title", result.title());
        assertEquals("Updated description", result.description());
        assertEquals(TaskStatus.DONE, result.status());
        assertEquals(TaskPriority.HIGH, result.priority());
        verify(repository).save(task);
    }

    @Test
    void deleteRemovesExistingTask() {
        Task task = task("Existing task");
        when(repository.findById(7L)).thenReturn(Optional.of(task));

        service.delete(7L);

        verify(repository).delete(task);
    }

    @Test
    void updateMissingTaskThrowsNotFound() {
        when(repository.findById(42L)).thenReturn(Optional.empty());
        assertThrows(TaskNotFoundException.class,
                () -> service.update(42L, new TaskRequest("Update", null, null, null, null)));
        verify(repository, never()).save(any());
    }

    @Test
    void deleteMissingTaskThrowsNotFound() {
        when(repository.findById(42L)).thenReturn(Optional.empty());
        assertThrows(TaskNotFoundException.class, () -> service.delete(42L));
        verify(repository, never()).delete(any(Task.class));
    }

    private Task task(String title) {
        Task task = new Task();
        task.setTitle(title);
        task.setStatus(TaskStatus.TODO);
        task.setPriority(TaskPriority.MEDIUM);
        return task;
    }
}
