# Copilot CLI Session

> [!NOTE]
> - **Session ID:** `32af526f-e54b-4ff0-ac22-c17fb06b6b91`  
> - **Started:** 18/9/2026, 12:38:15 p.m.  
> - **Duration:** 1m 13s  
> - **Exported:** 18/9/2026, 12:39:29 p.m.  

---

<sub>3s</sub>

### User

Revisa semana6/proyecto-final.diff contra la especificación specs/progress.md.

---

<sub>14s</sub>

### Copilot

Reading the diff and the spec to compare changes and files. Running two parallel file reads: the diff and the specification. Then will open each file modified in the diff to inspect context. Proceeding to read both files now.

---

<sub>14s</sub>

### `view`

**view the file at C:\Users\User\git\taskflow-copilot-rodrigo\semana6\proyecto-final.diff.**

C:\Users\User\git\taskflow-copilot-rodrigo\semana6\proyecto-final.diff

<details>
<summary>232 lines</summary>

```diff
diff --git a/src/main/java/com/taskflow/controller/ReportController.java b/src/main/java/com/taskflow/controller/ReportController.java
new file mode 100644
index 0000000..5c14cf7
--- /dev/null
+++ b/src/main/java/com/taskflow/controller/ReportController.java
@@ -0,0 +1,28 @@
+package com.taskflow.controller;
+
+import com.taskflow.dto.ProjectProgressResponse;
+import com.taskflow.service.ProjectService;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.tags.Tag;
+import org.springframework.web.bind.annotation.GetMapping;
+import org.springframework.web.bind.annotation.RestController;
+
+import java.util.List;
+
+@RestController
+@Tag(name = "Reports", description = "Reportes de avance de proyectos")
+public class ReportController {
+
+    private final ProjectService projectService;
+
+    public ReportController(ProjectService projectService) {
+        this.projectService = projectService;
+    }
+
+    @Operation(summary = "Reporte de progreso por proyecto",
+            description = "Devuelve el avance de cada proyecto: total de tareas, terminadas y porcentaje redondeado a un decimal.")
+    @GetMapping("/reports/progress")
+    public List<ProjectProgressResponse> getProgress() {
+        return projectService.progresoPorProyecto();
+    }
+}
diff --git a/src/main/java/com/taskflow/dto/ProjectProgressResponse.java b/src/main/java/com/taskflow/dto/ProjectProgressResponse.java
new file mode 100644
index 0000000..b3b9751
--- /dev/null
+++ b/src/main/java/com/taskflow/dto/ProjectProgressResponse.java
@@ -0,0 +1,13 @@
+package com.taskflow.dto;
+
+/**
+ * DTO de salida para el reporte de progreso por proyecto.
+ *
+ * projectId: id del proyecto
+ * projectName: nombre del proyecto
+ * totalTasks: total de tareas del proyecto
+ * doneTasks: tareas en estado DONE
+ * percentDone: porcentaje completado (0.0..100.0) redondeado a 1 decimal
+ */
+public record ProjectProgressResponse(Long projectId, String projectName, long totalTasks, long doneTasks, double percentDone) {
+}
diff --git a/src/main/java/com/taskflow/mapper/ProjectMapper.java b/src/main/java/com/taskflow/mapper/ProjectMapper.java
index c136a97..3283477 100644
--- a/src/main/java/com/taskflow/mapper/ProjectMapper.java
+++ b/src/main/java/com/taskflow/mapper/ProjectMapper.java
@@ -41,4 +41,9 @@ public final class ProjectMapper {
         }
         return new com.taskflow.dto.ProjectSummaryResponse(p.getId(), p.getName(), tasks.size(), byStatus, overdue);
     }
+
+    /** Construye el DTO de progreso por proyecto. */
+    public static com.taskflow.dto.ProjectProgressResponse aProgreso(Project proyecto, long totalTasks, long doneTasks, double percentDone) {
+        return new com.taskflow.dto.ProjectProgressResponse(proyecto.getId(), proyecto.getName(), totalTasks, doneTasks, percentDone);
+    }
 }
diff --git a/src/main/java/com/taskflow/service/ProjectService.java b/src/main/java/com/taskflow/service/ProjectService.java
index aed5fee..bc65df8 100644
--- a/src/main/java/com/taskflow/service/ProjectService.java
+++ b/src/main/java/com/taskflow/service/ProjectService.java
@@ -110,4 +110,27 @@ public class ProjectService {
         java.util.List<Task> tareas = tareasDe(project.getId());
         return com.taskflow.mapper.ProjectMapper.aSummary(project, tareas);
     }
+
+    /**
+     * Reporte de progreso por proyecto: para cada proyecto cuenta tareas y DONE y calcula el
+     * porcentaje redondeado a un decimal.
+     */
+    public java.util.List<com.taskflow.dto.ProjectProgressResponse> progresoPorProyecto() {
+        java.util.List<Project> proyectos = projectRepository.findAll();
+        return proyectos.stream()
+                .sorted(java.util.Comparator.comparing(Project::getId))
+                .map(p -> {
+                    java.util.List<Task> tareas = taskRepository.findByProjectId(p.getId());
+                    long total = tareas.size();
+                    long done = tareas.stream().filter(t -> t.getStatus() == com.taskflow.model.TaskStatus.DONE).count();
+                    double percentDone;
+                    if (total == 0) {
+                        percentDone = 0.0;
+                    } else {
+                        percentDone = Math.round((done * 100.0 / total) * 10.0) / 10.0;
+                    }
+                    return com.taskflow.mapper.ProjectMapper.aProgreso(p, total, done, percentDone);
+                })
+                .toList();
+    }
 }
diff --git a/src/test/java/com/taskflow/slice/ProgresoProyectosControllerTest.java b/src/test/java/com/taskflow/slice/ProgresoProyectosControllerTest.java
new file mode 100644
index 0000000..dba09a2
--- /dev/null
+++ b/src/test/java/com/taskflow/slice/ProgresoProyectosControllerTest.java
@@ -0,0 +1,47 @@
+package com.taskflow.slice;
+
+import com.taskflow.controller.ReportController;
+import com.taskflow.dto.ProjectProgressResponse;
+import com.taskflow.security.JwtAuthenticationFilter;
+import com.taskflow.service.ProjectService;
+import org.junit.jupiter.api.Test;
+import org.springframework.beans.factory.annotation.Autowired;
+import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
+import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
+import org.springframework.http.MediaType;
+import org.springframework.test.context.bean.override.mockito.MockitoBean;
+import org.springframework.test.web.servlet.MockMvc;
+
+import java.util.List;
+
+import static org.mockito.Mockito.when;
+import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
+import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
+import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
+
+@WebMvcTest(ReportController.class)
+@AutoConfigureMockMvc(addFilters = false)
+class ProgresoProyectosControllerTest {
+
+    @Autowired
+    private MockMvc mockMvc;
+
+    @MockitoBean
+    private ProjectService projectService;
+
+    @MockitoBean
+    private JwtAuthenticationFilter jwtAuthenticationFilter;
+
+    @Test
+    void getProgress_devuelve200YJson() throws Exception {
+        ProjectProgressResponse r1 = new ProjectProgressResponse(1L, "Plataforma TaskFlow", 5, 1, 20.0);
+        ProjectProgressResponse r2 = new ProjectProgressResponse(2L, "App Móvil", 4, 1, 25.0);
+        when(projectService.progresoPorProyecto()).thenReturn(List.of(r1, r2));
+
+        mockMvc.perform(get("/reports/progress").accept(MediaType.APPLICATION_JSON))
+                .andExpect(status().isOk())
+                .andExpect(jsonPath("$[0].projectId").value(1))
+                .andExpect(jsonPath("$[0].percentDone").value(20.0))
+                .andExpect(jsonPath("$[1].totalTasks").value(4));
+    }
+}
diff --git a/src/test/java/com/taskflow/unit/ProgresoProyectosServiceTest.java b/src/test/java/com/taskflow/unit/ProgresoProyectosServiceTest.java
new file mode 100644
index 0000000..2487f5b
--- /dev/null
+++ b/src/test/java/com/taskflow/unit/ProgresoProyectosServiceTest.java
@@ -0,0 +1,74 @@
+package com.taskflow.unit;
+
+import com.taskflow.model.Priority;
+import com.taskflow.model.Project;
+import com.taskflow.model.Task;
+import com.taskflow.model.TaskStatus;
+import com.taskflow.repository.ProjectRepository;
+import com.taskflow.repository.TaskRepository;
+import com.taskflow.repository.UserRepository;
+import com.taskflow.service.ProjectService;
+import org.junit.jupiter.api.Test;
+import org.junit.jupiter.api.extension.ExtendWith;
+import org.mockito.InjectMocks;
+import org.mockito.Mock;
+import org.mockito.junit.jupiter.MockitoExtension;
+
+import java.util.List;
+
+import static org.junit.jupiter.api.Assertions.assertEquals;
+import static org.mockito.Mockito.when;
+
+@ExtendWith(MockitoExtension.class)
+class ProgresoProyectosServiceTest {
+
+    @Mock
+    private ProjectRepository projectRepository;
+
+    @Mock
+    private TaskRepository taskRepository;
+
+    @Mock
+    private UserRepository userRepository;
+
+    @InjectMocks
+    private ProjectService projectService;
+
+    @Test
+    void progresoPorProyecto_calculaConteosYOrden() throws Exception {
+        // Repositorio devuelve en orden 3,1,2 (simula orden arbitrario)
+        Project p3 = new Project(3L, "Migración Legacy", "d", 1L, null);
+        Project p1 = new Project(1L, "Plataforma TaskFlow", "d", 1L, null);
+        Project p2 = new Project(2L, "App Móvil", "d", 1L, null);
+        when(projectRepository.findAll()).thenReturn(List.of(p3, p1, p2));
+
+        // p1: 5 tareas, 1 DONE -> 20.0
+        Task p1t1 = new Task(101L, "T-1", "d", TaskStatus.DONE, Priority.MED, 1L, 1L, null);
+        Task p1t2 = new Task(102L, "T-2", "d", TaskStatus.TODO, Priority.MED, 1L, 1L, null);
+        Task p1t3 = new Task(103L, "T-3", "d", TaskStatus.TODO, Priority.MED, 1L, 1L, null);
+        Task p1t4 = new Task(104L, "T-4", "d", TaskStatus.IN_PROGRESS, Priority.MED, 1L, 1L, null);
+        Task p1t5 = new Task(105L, "T-5", "d", TaskStatus.TODO, Priority.MED, 1L, 1L, null);
+        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(p1t1, p1t2, p1t3, p1t4, p1t5));
+
+        // p2: 3 tareas, 1 DONE -> 33.3
+        Task p2t1 = new Task(201L, "T-1", "d", TaskStatus.DONE, Priority.MED, 2L, 1L, null);
+        Task p2t2 = new Task(202L, "T-2", "d", TaskStatus.TODO, Priority.MED, 2L, 1L, null);
+        Task p2t3 = new Task(203L, "T-3", "d", TaskStatus.TODO, Priority.MED, 2L, 1L, null);
+        when(taskRepository.findByProjectId(2L)).thenReturn(List.of(p2t1, p2t2, p2t3));
+
+        // p3: sin tareas -> 0.0
+        when(taskRepository.findByProjectId(3L)).thenReturn(List.of());
+
+        var resp = projectService.progresoPorProyecto();
+
+        assertEquals(3, resp.size());
+        // Orden por projectId asc: 1,2,3
+        assertEquals(1L, resp.get(0).projectId());
+        assertEquals(2L, resp.get(1).projectId());
+        assertEquals(3L, resp.get(2).projectId());
+
+        assertEquals(20.0, resp.get(0).percentDone(), 0.0001);
+        assertEquals(33.3, resp.get(1).percentDone(), 0.0001);
+        assertEquals(0.0, resp.get(2).percentDone(), 0.0001);
+    }
+}
```

</details>


---

<sub>14s</sub>

### `view`

**view the file at C:\Users\User\git\taskflow-copilot-rodrigo\specs\progress.md.**

C:\Users\User\git\taskflow-copilot-rodrigo\specs\progress.md

<details>
<summary>77 lines</summary>

````
# Especificación — `GET /reports/progress`

## Qué

Un reporte con el avance de **cada** proyecto: cuántas tareas tiene, cuántas están terminadas y qué
porcentaje representan. Sirve para una pantalla de «cómo vamos» sin pedir las tareas proyecto por
proyecto.

```
GET /reports/progress
Authorization: ******
```

## Respuesta 200

```json
[
  { "projectId": 1, "projectName": "Plataforma TaskFlow", "totalTasks": 5, "doneTasks": 1, "percentDone": 20.0 },
  { "projectId": 2, "projectName": "App Móvil",           "totalTasks": 4, "doneTasks": 1, "percentDone": 25.0 },
  { "projectId": 3, "projectName": "Migración Legacy",    "totalTasks": 0, "doneTasks": 0, "percentDone": 0.0 }
]
```

| Campo | Tipo | Qué es |
|---|---|---|
| `projectId` | número | el `id` del proyecto |
| `projectName` | texto | el `name` del proyecto |
| `totalTasks` | número | cuántas tareas tiene el proyecto |
| `doneTasks` | número | cuántas de esas están en `DONE` |
| `percentDone` | número decimal | `doneTasks` entre `totalTasks`, por 100, redondeado a **un decimal** |

## Reglas

1. Aparecen **todos** los proyectos, también los que no tienen tareas.
2. Proyecto sin tareas: `totalTasks` 0, `doneTasks` 0 y `percentDone` **`0.0`**. Nunca `NaN` ni un
   error por dividir entre cero.
3. El porcentaje se calcula en decimal **antes** de dividir (`doneTasks * 100.0 / totalTasks`) y se
   redondea a un decimal: 1 de 3 → `33.3`; 2 de 3 → `66.7`; 1 de 8 → `12.5`. Es la trampa `long/long`
   que ya documenta `ReportService.porcentajeCompletadas()`: con división entera, 1 de 4 da `0`.
4. Orden: por `projectId` ascendente, aunque el repositorio los devuelva en otro orden.
5. Seguridad: cualquier usuario autenticado puede pedirlo (sin token → `401`). No cambies
   `SecurityConfig`.

## Dónde

- DTO de salida nuevo en `com.taskflow.dto`:
  `public record ProjectProgressResponse(Long projectId, String projectName, long totalTasks, long doneTasks, double percentDone)`,
  con su Javadoc.
- Mapper: método nuevo `public static ProjectProgressResponse aProgreso(Project proyecto, long totalTasks, long doneTasks, double percentDone)`
  en `ProjectMapper`.
- Método nuevo en `ProjectService`: `public List<ProjectProgressResponse> progresoPorProyecto()`.
  `ProjectService` ya tiene los dos repositorios que hacen falta (`projectRepository` y
  `taskRepository`): usa `projectRepository.findAll()` y `taskRepository.findByProjectId(Long)`.
  **No agregues métodos a los repositorios.** La cuenta y el redondeo van aquí, no en el controller.
- Controller **nuevo** `ReportController` en `com.taskflow.controller`, con
  `@Tag(name = "Reports", description = …)`, `@GetMapping("/reports/progress")` y `@Operation` en
  español. Recibe `ProjectService` por constructor. Es un controller nuevo, así que ningún slice test
  existente lo conoce y no rompe nada.

## Tests que deben existir al terminar

Los dos en **clases nuevas**:

- **Unit** `src/test/java/com/taskflow/unit/ProgresoProyectosServiceTest.java`, con `@Mock` de
  `ProjectRepository`, `TaskRepository` y `UserRepository` (los tres del constructor de
  `ProjectService`) e `@InjectMocks ProjectService`. Un caso con tres proyectos que el mock devuelve
  en orden `3, 1, 2`:
  - el 1 con 5 tareas y 1 `DONE` → `20.0`;
  - el 2 con 3 tareas y 1 `DONE` → `33.3` (prueba el redondeo);
  - el 3 sin tareas → `0.0`;
  - y la lista sale en orden `1, 2, 3`.
- **Slice** `src/test/java/com/taskflow/slice/ProgresoProyectosControllerTest.java`, con
  `@WebMvcTest(ReportController.class)`, `@AutoConfigureMockMvc(addFilters = false)` y `@MockitoBean`
  de `ProjectService` y `JwtAuthenticationFilter`. Caso: `200` y cada campo del JSON de los dos
  primeros elementos que devuelve el mock (`$[0].projectId`, `$[0].percentDone`, `$[1].totalTasks`…).

## Resultado esperado con la semilla

Con la app recién arrancada con el perfil `h2` y el token de `ana`:

| Petición | Resultado |
|---|---|
| `GET /reports/progress` | `200` con los tres objetos del ejemplo de arriba, en ese orden |
| `PATCH /tasks/1/status` con `{"status": "DONE"}` y luego `GET /reports/progress` | el proyecto 1 pasa a `doneTasks` 2 y `percentDone` `40.0` (la tarea 1 tiene responsable, así que el PATCH responde 200) |
| `GET /reports/progress` sin token | `401` |

## Restricciones para el agente

- No modifiques, borres ni desactives ningún test existente.
- No toques archivos fuera de `ProjectProgressResponse.java` (nuevo), `ProjectMapper.java`,
  `ProjectService.java`, `ReportController.java` (nuevo) y las dos clases de test nuevas.
- Al terminar, `mvn -q test` tiene que pasar completo.

El pull request del proyecto final lleva además `semana6/`, `.github/skills/verificar-taskflow/casos-progress.ps1`
y una línea nueva en `verificar.ps1`. Esos archivos los agregas tú en el proceso (PF-2 a PF-7 de la guía),
no el agente: no cuentan contra esta restricción.
````

</details>


---

<sub>36s</sub>

### Copilot

Abrir los archivos actuales ProjectMapper.java y ProjectService.java para verificar contexto y confirmarlos contra el diff (lecturas paralelas). Ejecutando las lecturas.

---

<sub>36s</sub>

### `view`

**view the file at C:\Users\User\git\taskflow-copilot-rodrigo\src\main\java\com\taskflow\mapper\ProjectMapper.java.**

C:\Users\User\git\taskflow-copilot-rodrigo\src\main\java\com\taskflow\mapper\ProjectMapper.java

<details>
<summary>43 lines</summary>

```
package com.taskflow.mapper;

import com.taskflow.dto.ProjectResponse;
import com.taskflow.model.Project;

/**
 * ProjectMapper — puente DTO &lt;-&gt; dominio del lado Project. Estático, a mano, sin MapStruct.
 *
 * HOY se SIMPLIFICÓ (lo prometía D3): la entidad ya guarda 'ownerId' directo (se aplanó el 'User
 * owner' en MP-4), así que aResponse ya no deriva el id desde un objeto (p.getOwner().id()) — lee
 * p.getOwnerId() tal cual. El contrato de salida (ProjectResponse con ownerId Long) no cambió; el
 * mapeo se volvió trivial porque el dominio por fin coincide con la forma canónica.
 */
public final class ProjectMapper {

    private ProjectMapper() {
        // no instanciable
    }

    /** Entidad -> DTO de salida. Ahora ownerId sale directo del campo (sin puente por objeto). */
    public static ProjectResponse aResponse(Project p) {
        return new ProjectResponse(p.getId(), p.getName(), p.getDescription(),
                p.getOwnerId(), p.getCreatedAt());
    }

    /**
     * Construye el DTO ProjectSummaryResponse a partir del proyecto y sus tareas.
     */
    public static com.taskflow.dto.ProjectSummaryResponse aSummary(Project p, java.util.List<com.taskflow.model.Task> tasks) {
        java.util.Map<String, Integer> byStatus = new java.util.LinkedHashMap<>();
        for (com.taskflow.model.TaskStatus s : com.taskflow.model.TaskStatus.values()) {
            byStatus.put(s.name(), 0);
        }
        int overdue = 0;
        for (com.taskflow.model.Task t : tasks) {
            String key = t.getStatus().name();
            byStatus.put(key, byStatus.getOrDefault(key, 0) + 1);
            if (t.estaVencida()) {
                overdue++;
            }
        }
        return new com.taskflow.dto.ProjectSummaryResponse(p.getId(), p.getName(), tasks.size(), byStatus, overdue);
    }

    /** Construye el DTO de progreso por proyecto. */
    public static com.taskflow.dto.ProjectProgressResponse aProgreso(Project proyecto, long totalTasks, long doneTasks, double percentDone) {
        return new com.taskflow.dto.ProjectProgressResponse(proyecto.getId(), proyecto.getName(), totalTasks, doneTasks, percentDone);
    }
}
```

</details>


---

<sub>36s</sub>

### `view`

**view the file at C:\Users\User\git\taskflow-copilot-rodrigo\src\main\java\com\taskflow\service\ProjectService.java.**

C:\Users\User\git\taskflow-copilot-rodrigo\src\main\java\com\taskflow\service\ProjectService.java

<details>
<summary>123 lines</summary>

```
package com.taskflow.service;

import com.taskflow.dto.ProjectRequest;
import com.taskflow.exception.ProjectNotFoundException;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.User;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ProjectService — la capa de negocio del lado Project. Cero HTTP aquí (el "no existe" se traduce con
 * orElseThrow -> ProjectNotFoundException; el advice le pone el 404).
 *
 * Cambios de HOY (S2D4) — y por qué la promesa del día NO lo cubre:
 *   - MP-4: el modelo Project se aplanó (User owner -> Long ownerId), así que 'crear' y 'reemplazar'
 *     manejan un ownerId (Long), no un objeto User. Antes 'crear' construía con 0L (convención del
 *     InMemory de "aún sin id"); ahora construye con null y la BD asigna el id (IDENTITY).
 *   - MP-6: 'tareasDe' cambió su stream-filter de findAll() por taskRepository.findByProjectId(id)
 *     — la nota de D3 ("en D4 esto se vuelve un query method") se paga AQUÍ. La promesa "no tocar el
 *     servicio" protege a TaskService (que no se tocó), no a ProjectService (que ya se tocó en MP-4).
 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;   // D5: para resolver el owner desde el username del JWT

    public ProjectService(ProjectRepository projectRepository, TaskRepository taskRepository,
                          UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    /** Todos los proyectos. */
    public List<Project> listar() {
        return projectRepository.findAll();
    }

    /** Un proyecto por id (el Optional sube tal cual: el controller decide 404 vs 200). */
    public Optional<Project> buscarPorId(Long id) {
        return projectRepository.findById(id);
    }

    /**
     * Las tareas de un proyecto: MP-6 sustituye el stream-filter de D2 por la derived query
     * taskRepository.findByProjectId(projectId) — el LIKE... perdón, el WHERE project_id = ? lo hace
     * la BD (viajan solo las filas del proyecto). "no existe el proyecto" (404) lo decide el
     * controller con buscarPorId; ESTE método, si el proyecto existe pero no tiene tareas, devuelve
     * [] (-> 200 con []). No son lo mismo.
     */
    public List<Task> tareasDe(Long projectId) {
        return taskRepository.findByProjectId(projectId);
    }

    /**
     * Crea un proyecto (POST): el request trae name y description; el ownerId ya NO es una constante
     * (murió el 1L fijo de D3) — se resuelve del USERNAME AUTENTICADO (el que puso el JWT en el
     * Authentication). El dueño no lo decide el cliente ni una semilla: es QUIEN llama. createdAt = hoy;
     * id == null -> la BD lo asigna (IDENTITY).
     */
    public Project crear(ProjectRequest request, String ownerUsername) {
        User owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new UsernameNotFoundException(ownerUsername));
        Project nuevo = new Project(null, request.name(), request.description(),
                owner.getId(), LocalDate.now());
        return projectRepository.save(nuevo);
    }

    /**
     * Reemplazo COMPLETO (PUT): existe -> reconstruye con el MISMO id, conservando ownerId y createdAt
     * (el cliente no los toca); name y description vienen del request. save con id presente = update.
     * No existe -> 404.
     */
    public Project reemplazar(Long id, ProjectRequest request) {
        Project actual = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
        Project reemplazo = new Project(actual.getId(), request.name(), request.description(),
                actual.getOwnerId(), actual.getCreatedAt());
        return projectRepository.save(reemplazo);
    }

    /**
     * Elimina un proyecto (DELETE) con CASCADA MANUAL: borra primero sus tareas (regla "no Task sin
     * Project" — y además la FK task.project_id lo EXIGE: borrar el proyecto con tareas colgando daría
     * una violación de integridad) y luego el proyecto. No existe -> 404. La regla owner/ADMIN llega
     * en D5 (hoy queda abierto, documentado en el @Operation del controller).
     */
    public void eliminar(Long id) {
        projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
        tareasDe(id).forEach(t -> taskRepository.deleteById(t.getId()));   // cascada manual (la FK obliga el orden)
        projectRepository.deleteById(id);
    }

    /**
     * Resumen de un proyecto: delega en taskRepository y en el mapper para construir el DTO.
     * Recibe la entidad Project ya encontrada por el controller.
     */
    public com.taskflow.dto.ProjectSummaryResponse resumenDe(Project project) {
        java.util.List<Task> tareas = tareasDe(project.getId());
        return com.taskflow.mapper.ProjectMapper.aSummary(project, tareas);
    }

    /**
     * Reporte de progreso por proyecto: para cada proyecto cuenta tareas y DONE y calcula el
     * porcentaje redondeado a un decimal.
     */
    public java.util.List<com.taskflow.dto.ProjectProgressResponse> progresoPorProyecto() {
        java.util.List<Project> proyectos = projectRepository.findAll();
        return proyectos.stream()
                .sorted(java.util.Comparator.comparing(Project::getId))
                .map(p -> {
                    java.util.List<Task> tareas = taskRepository.findByProjectId(p.getId());
                    long total = tareas.size();
                    long done = tareas.stream().filter(t -> t.getStatus() == com.taskflow.model.TaskStatus.DONE).count();
                    double percentDone;
                    if (total == 0) {
                        percentDone = 0.0;
                    } else {
                        percentDone = Math.round((done * 100.0 / total) * 10.0) / 10.0;
                    }
                    return com.taskflow.mapper.ProjectMapper.aProgreso(p, total, done, percentDone);
                })
                .toList();
    }
}
```

</details>


---

<sub>48s</sub>

### Copilot

Opening test directories to compare test annotations and patterns (buscar uso de @MockitoBean vs @MockBean). Ejecutando lecturas paralelas de dos test directories.

---

<sub>48s</sub>

### `view`

**view the file at C:\Users\User\git\taskflow-copilot-rodrigo\src\test\java\com\taskflow\slice.**

C:\Users\User\git\taskflow-copilot-rodrigo\src\test\java\com\taskflow\slice

```
ProgresoProyectosControllerTest.java
ProjectControllerSummaryTest.java
ProjectControllerTest.java
TaskControllerTest.java
TaskRepositoryTest.java
```


---

<sub>48s</sub>

### `view`

**view the file at C:\Users\User\git\taskflow-copilot-rodrigo\src\test\java\com\taskflow\unit.**

C:\Users\User\git\taskflow-copilot-rodrigo\src\test\java\com\taskflow\unit

```
ProgresoProyectosServiceTest.java
ProjectServiceSummaryTest.java
ReportServiceTest.java
TaskServiceTest.java
TaskValidationTest.java
```


---

<sub>1m 13s</sub>

### Copilot

Resumen: se revisó el diff semana6/proyecto-final.diff contra specs/progress.md — todo lo pedido está implementado y los dos tests exigidos existen. Tabla de hallazgos a continuación.

| # | Gravedad | Archivo:línea | Punto de la checklist | Qué pasa | Qué harías |
|---|---:|---|---|---|---|
| 1 | Sugerencia | ReportController.java:1 (controller nuevo) | Alcance | Controller nuevo /reports/progress presente con @Tag y @Operation en español. | Ninguna acción: cumple la spec. |
| 2 | Sugerencia | ProjectProgressResponse.java:1 (DTO nuevo) | Contrato / DTO | record con (Long projectId, String projectName, long totalTasks, long doneTasks, double percentDone) y Javadoc presente. | Ninguna acción: cumple la spec. |
| 3 | Sugerencia | ProjectMapper.java (método aProgreso) | Mapper | Método aProgreso añadido y devuelve el record correcto. | Ninguna acción: cumple la spec. |
| 4 | Sugerencia | ProjectService.java:progresoPorProyecto() | Lógica de negocio / Reglas | Implementa findAll(), ordena por id, cuenta tareas, cuenta DONE por status, evita división por cero y redondea a 1 decimal con Math.round(...*10)/10. | Ninguna acción: cumple la spec. |
| 5 | Sugerencia | src/test/java/... (dos tests nuevos) | Tests requeridos | Unit test (ProjectService) y slice test (ReportController) añadidos; mocks/annotations coinciden con lo pedido (incluye UserRepository mock en unit y JwtAuthenticationFilter en slice). | Ninguna acción: cumple la spec. |
| 6 | Sugerencia | ProjectService.java (tipos/operaciones) | Regla de dominio y precisión | Usa getStatus() para contar DONE (no reescribe reglas del dominio). Redondeo correcto. | Ninguna acción: cumple la spec. |

Casos sin test:
- ninguno (los dos tests solicitados por la especificación están presentes y cubren los casos: orden, redondeo y proyecto sin tareas).

Veredicto: APROBADO.

---

<sub>Generated by [GitHub Copilot CLI](https://github.com/features/copilot/cli)</sub>
