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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgresoProyectosServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void progresoPorProyecto_calculaConteosYOrden() throws Exception {
        // Repositorio devuelve en orden 3,1,2 (simula orden arbitrario)
        Project p3 = new Project(3L, "Migración Legacy", "d", 1L, null);
        Project p1 = new Project(1L, "Plataforma TaskFlow", "d", 1L, null);
        Project p2 = new Project(2L, "App Móvil", "d", 1L, null);
        when(projectRepository.findAll()).thenReturn(List.of(p3, p1, p2));

        // p1: 5 tareas, 1 DONE -> 20.0
        Task p1t1 = new Task(101L, "T-1", "d", TaskStatus.DONE, Priority.MED, 1L, 1L, null);
        Task p1t2 = new Task(102L, "T-2", "d", TaskStatus.TODO, Priority.MED, 1L, 1L, null);
        Task p1t3 = new Task(103L, "T-3", "d", TaskStatus.TODO, Priority.MED, 1L, 1L, null);
        Task p1t4 = new Task(104L, "T-4", "d", TaskStatus.IN_PROGRESS, Priority.MED, 1L, 1L, null);
        Task p1t5 = new Task(105L, "T-5", "d", TaskStatus.TODO, Priority.MED, 1L, 1L, null);
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(p1t1, p1t2, p1t3, p1t4, p1t5));

        // p2: 3 tareas, 1 DONE -> 33.3
        Task p2t1 = new Task(201L, "T-1", "d", TaskStatus.DONE, Priority.MED, 2L, 1L, null);
        Task p2t2 = new Task(202L, "T-2", "d", TaskStatus.TODO, Priority.MED, 2L, 1L, null);
        Task p2t3 = new Task(203L, "T-3", "d", TaskStatus.TODO, Priority.MED, 2L, 1L, null);
        when(taskRepository.findByProjectId(2L)).thenReturn(List.of(p2t1, p2t2, p2t3));

        // p3: sin tareas -> 0.0
        when(taskRepository.findByProjectId(3L)).thenReturn(List.of());

        var resp = projectService.progresoPorProyecto();

        assertEquals(3, resp.size());
        // Orden por projectId asc: 1,2,3
        assertEquals(1L, resp.get(0).projectId());
        assertEquals(2L, resp.get(1).projectId());
        assertEquals(3L, resp.get(2).projectId());

        assertEquals(20.0, resp.get(0).percentDone(), 0.0001);
        assertEquals(33.3, resp.get(1).percentDone(), 0.0001);
        assertEquals(0.0, resp.get(2).percentDone(), 0.0001);
    }
}
