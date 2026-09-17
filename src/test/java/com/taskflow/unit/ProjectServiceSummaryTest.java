package com.taskflow.unit;

import com.taskflow.model.Priority;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceSummaryTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void resumenDe_proyectoConTareas_calculaConteos() throws Exception {
        Project proyecto = new Project(2L, "App Móvil", "d", 2L, null);
        Task t1 = new Task(10L, "T-1", "d", TaskStatus.TODO, Priority.MED, 2L, 1L, null);
        Task t2 = new Task(11L, "T-2", "d", TaskStatus.IN_PROGRESS, Priority.MED, 2L, 1L, null);
        Task t3 = new Task(12L, "T-3", "d", TaskStatus.IN_PROGRESS, Priority.MED, 2L, 1L, LocalDate.now().minusDays(1));
        Task t4 = new Task(13L, "T-4", "d", TaskStatus.DONE, Priority.MED, 2L, 1L, null);

        when(taskRepository.findByProjectId(2L)).thenReturn(List.of(t1, t2, t3, t4));

        var resp = projectService.resumenDe(proyecto);

        assertEquals(4, resp.totalTasks());
        assertEquals(1, resp.byStatus().get("TODO"));
        assertEquals(2, resp.byStatus().get("IN_PROGRESS"));
        assertEquals(1, resp.byStatus().get("DONE"));
        assertEquals(1, resp.overdue());
    }

    @Test
    void resumenDe_proyectoSinTareas_devuelveCeros() {
        Project proyecto = new Project(3L, "Empty", "d", 1L, null);
        when(taskRepository.findByProjectId(3L)).thenReturn(List.of());

        var resp = projectService.resumenDe(proyecto);

        assertEquals(0, resp.totalTasks());
        assertEquals(0, resp.byStatus().get("TODO"));
        assertEquals(0, resp.byStatus().get("IN_PROGRESS"));
        assertEquals(0, resp.byStatus().get("DONE"));
        assertEquals(0, resp.overdue());
    }
}
