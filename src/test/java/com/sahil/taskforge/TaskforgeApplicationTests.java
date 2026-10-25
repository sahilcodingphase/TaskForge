package com.sahil.taskforge;

import com.sahil.taskforge.task.TaskController;
import com.sahil.taskforge.task.TaskPriority;
import com.sahil.taskforge.task.TaskService;
import com.sahil.taskforge.task.TaskStatus;
import com.sahil.taskforge.task.dto.TaskPageResponse;
import com.sahil.taskforge.task.dto.TaskResponse;
import com.sahil.taskforge.task.exception.InvalidTaskQueryException;
import com.sahil.taskforge.task.exception.TaskNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
        + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration")
@AutoConfigureMockMvc
class TaskforgeApplicationTests {
    @Autowired MockMvc mockMvc;
    @MockitoBean TaskService service;

    private TaskResponse sample() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 25, 10, 0);
        return new TaskResponse(7L, "Prepare demo", "Review API", TaskStatus.IN_PROGRESS,
                TaskPriority.HIGH, LocalDate.of(2026, 11, 5), now, now);
    }

    private TaskPageResponse samplePage() {
        return new TaskPageResponse(List.of(sample()), 0, 10, 1, 1, true, true);
    }

    @Test
    void createReturnsCreatedTask() throws Exception {
        when(service.create(any())).thenReturn(sample());
        mockMvc.perform(post("/api/tasks").contentType(APPLICATION_JSON)
                        .content("""
                                {"title":"Prepare demo","description":"Review API","status":"IN_PROGRESS","priority":"HIGH","dueDate":"2026-11-05"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    void listReturnsPageMetadataAndTasks() throws Exception {
        when(service.findPage(0, 10, null, "createdAt", "DESC")).thenReturn(samplePage());
        mockMvc.perform(get("/api/tasks")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Prepare demo"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void listPassesPaginationFilteringAndSortOptions() throws Exception {
        when(service.findPage(2, 5, TaskStatus.DONE, "title", "ASC")).thenReturn(samplePage());
        mockMvc.perform(get("/api/tasks").param("page", "2").param("size", "5")
                        .param("status", "DONE").param("sort", "title").param("direction", "ASC"))
                .andExpect(status().isOk());
        verify(service).findPage(2, 5, TaskStatus.DONE, "title", "ASC");
    }

    @Test
    void emptyPageReturnsEmptyContentAndZeroTotals() throws Exception {
        when(service.findPage(0, 10, null, "createdAt", "DESC"))
                .thenReturn(new TaskPageResponse(List.of(), 0, 10, 0, 0, true, true));
        mockMvc.perform(get("/api/tasks")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void unsupportedStatusReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/tasks").param("status", "WAITING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.status").exists());
        verifyNoInteractions(service);
    }

    @Test
    void malformedPageReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/tasks").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.page").exists());
        verifyNoInteractions(service);
    }

    @Test
    void invalidSortReturnsBadRequest() throws Exception {
        when(service.findPage(0, 10, null, "unknown", "DESC"))
                .thenThrow(new InvalidTaskQueryException("sort", "Unsupported sort field"));
        mockMvc.perform(get("/api/tasks").param("sort", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.sort").value("Unsupported sort field"));
    }

    @Test
    void invalidDirectionReturnsBadRequest() throws Exception {
        when(service.findPage(0, 10, null, "title", "sideways"))
                .thenThrow(new InvalidTaskQueryException("direction", "Direction must be ASC or DESC"));
        mockMvc.perform(get("/api/tasks").param("sort", "title").param("direction", "sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.direction").exists());
    }

    @Test
    void getByIdReturnsTask() throws Exception {
        when(service.findById(7L)).thenReturn(sample());
        mockMvc.perform(get("/api/tasks/7")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void updateReturnsUpdatedTask() throws Exception {
        when(service.update(eq(7L), any())).thenReturn(sample());
        mockMvc.perform(put("/api/tasks/7").contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Prepare demo\",\"status\":\"IN_PROGRESS\",\"priority\":\"HIGH\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Prepare demo"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/tasks/7")).andExpect(status().isNoContent());
        verify(service).delete(7L);
    }

    @Test
    void blankTitleIsRejectedBeforeServiceCall() throws Exception {
        mockMvc.perform(post("/api/tasks").contentType(APPLICATION_JSON)
                        .content("{\"title\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").value("Title is required"));
        verifyNoInteractions(service);
    }

    @Test
    void oversizedTitleAndDescriptionAreRejected() throws Exception {
        String title = "x".repeat(201);
        String description = "x".repeat(5001);
        mockMvc.perform(post("/api/tasks").contentType(APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"description\":\"" + description + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").value("Title must be at most 200 characters"))
                .andExpect(jsonPath("$.fieldErrors.description").value("Description must be at most 5000 characters"));
        verifyNoInteractions(service);
    }

    @Test
    void malformedDueDateReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/tasks").contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Prepare demo\",\"dueDate\":\"not-a-date\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body contains an invalid value or date"));
        verifyNoInteractions(service);
    }

    @Test
    void nonexistentTaskReturnsNotFound() throws Exception {
        when(service.findById(999L)).thenThrow(new TaskNotFoundException(999L));
        mockMvc.perform(get("/api/tasks/999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Task with id 999 was not found"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void nonNumericTaskIdReturnsConsistentBadRequest() throws Exception {
        mockMvc.perform(get("/api/tasks/not-a-number")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void invalidEnumValueReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/tasks").contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Prepare demo\",\"status\":\"WAITING\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body contains an invalid value or date"));
        verifyNoInteractions(service);
    }

    @Test
    void swaggerUiEntryPointRedirectsToTheUi() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void openApiDocumentIncludesMetadataCrudAndListParameters() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("TaskForge API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"))
                .andExpect(jsonPath("$.paths['/api/tasks'].post").exists())
                .andExpect(jsonPath("$.paths['/api/tasks'].get").exists())
                .andExpect(jsonPath("$.paths['/api/tasks/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/tasks/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/tasks/{id}'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/tasks'].get.parameters[*].name")
                        .value(containsInAnyOrder("page", "size", "status", "sort", "direction")));
    }
}

