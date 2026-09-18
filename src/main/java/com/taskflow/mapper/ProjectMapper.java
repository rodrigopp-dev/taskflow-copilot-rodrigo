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
