package com.taskflow.dto;

/**
 * DTO de salida para el reporte de progreso por proyecto.
 *
 * projectId: id del proyecto
 * projectName: nombre del proyecto
 * totalTasks: total de tareas del proyecto
 * doneTasks: tareas en estado DONE
 * percentDone: porcentaje completado (0.0..100.0) redondeado a 1 decimal
 */
public record ProjectProgressResponse(Long projectId, String projectName, long totalTasks, long doneTasks, double percentDone) {
}
