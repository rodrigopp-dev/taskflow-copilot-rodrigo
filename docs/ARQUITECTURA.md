# ARQUITECTURA — Guía rápida para un nuevo desarrollador

Este documento explica la estructura del proyecto TaskFlow, el recorrido de la petición POST /projects/{projectId}/tasks, dónde viven las reglas de negocio, cómo funciona la seguridad con JWT y cómo están organizados los tests.

1) Capas y paquetes

- Controladores (HTTP): `src/main/java/com/taskflow/controller` — ejemplos: `TaskController`, `ProjectController`, `AuthController`.
- Servicios (casos de uso / orquestación): `src/main/java/com/taskflow/service` — ejemplos: `TaskService`, `ProjectService`, `AuthService`, `JpaUserDetailsService`.
- Repositorios (acceso a datos, Spring Data JPA): `src/main/java/com/taskflow/repository` — ejemplos: `TaskRepository`, `ProjectRepository`, `UserRepository`.
- Modelo / Dominio: `src/main/java/com/taskflow/model` — ejemplos: `Task`, `Project`, `User`, `TaskStatus`, `Priority`.
- DTOs y mappers (contrato HTTP): `src/main/java/com/taskflow/dto`, `src/main/java/com/taskflow/mapper` — ejemplo: `TaskMapper`.
- Seguridad y configuración: `src/main/java/com/taskflow/security`, `src/main/java/com/taskflow/config` — ejemplos: `JwtService`, `JwtAuthenticationFilter`, `ProjectSecurity`, `SecurityConfig`.
- Manejo centralizado de errores: `src/main/java/com/taskflow/advice/GlobalExceptionHandler`.

Convenciones principales: DTOs como `record` con Bean Validation y `@Valid` en `@RequestBody`; inyección por constructor; controladores devuelven DTOs (nunca entidades); POST que crea → `201 Created` con `Location`; DELETE → `204 No Content`.

2) Recorrido de POST /projects/{projectId}/tasks (paso a paso)

Ruta invocada: `POST /projects/{projectId}/tasks` — implementada en:
- `src/main/java/com/taskflow/controller/TaskController.java` (`createTask`).

Flujo:
- 1) El `TaskController.createTask` recibe el `TaskRequest` (DTO) y el `projectId` del path. El `@Valid` realiza la validación de formato y campos.
- 2) El controlador primero valida que el proyecto exista llamando a `ProjectService.buscarPorId` (`src/main/java/com/taskflow/service/ProjectService.java`). Si no existe, lanza `ProjectNotFoundException` y el `GlobalExceptionHandler` traduce a `404`.
- 3) Si existe, el controlador delega la creación a `TaskService.crear(request, projectId)` (`src/main/java/com/taskflow/service/TaskService.java`).
- 4) `TaskService.crear` usa `TaskMapper.aEntidadNueva(request, projectId)` (`src/main/java/com/taskflow/mapper/TaskMapper.java`) para convertir el DTO en una entidad. `aEntidadNueva` llama a la factory de dominio `Task.crear(...)`.
- 5) `Task.crear(...)` (`src/main/java/com/taskflow/model/Task.java`) es la fábrica de negocio que fija `status=TODO`, `id=null` y aplica reglas de negocio críticas (por ejemplo: `dueDate` no puede estar en el pasado). Si viola reglas lanza `TaskValidationException`.
- 6) Si la entidad es válida, `TaskService` llama a `TaskRepository.save(nueva)` (`src/main/java/com/taskflow/repository/TaskRepository.java`) y JPA persiste la fila en la base de datos (id autogenerado).
- 7) `TaskController` construye la respuesta `201 Created` con cabecera `Location` apuntando a `/tasks/{id}` y el cuerpo `TaskResponse` mapeado por `TaskMapper.aResponse`.

Arquitectura de responsabilidades: el controller gestiona HTTP/validación frontera y errores 404; `ProjectService` se encarga de la lógica relacionada a proyectos; `TaskService` orquesta la creación y persistencia de tareas; las reglas de negocio están en la entidad `Task` y en las factorizaciones del mapper donde procede.

3) Dónde viven las reglas de negocio

- Reglas esenciales (invariantes y validaciones de negocio) están dentro de la entidad de dominio `src/main/java/com/taskflow/model/Task.java`:
  - `Task.crear(...)` valida la regla temporal (fecha límite no en el pasado).
  - El constructor de rehidratación valida invariantes como longitud del título y `projectId != null`.
  - `Task.setStatus(...)` implementa la regla "no pasar a DONE sin assignee".
- El servicio `TaskService` orquesta y TRADUCE excepciones: por ejemplo captura la `TaskValidationException` y relanza `TaskStateException` donde corresponde para distinguir 400 vs 422 mediante el `GlobalExceptionHandler`.
- Mapeos DTO <-> dominio se hacen en `src/main/java/com/taskflow/mapper/TaskMapper.java` (distinción entre crear vs rehidratar).

Principio: la lógica de negocio va al dominio (model), no a los controllers ni a los repositorios. Los servicios coordinan y aplican políticas operacionales (ej.: cascada en borrado de `ProjectService.eliminar`).

4) Seguridad con JWT (resumen)

Componentes principales:
- `src/main/java/com/taskflow/security/JwtService.java` — genera y valida tokens JWT (firma HMAC, exp, sub=username, claim role).
- `src/main/java/com/taskflow/security/JwtAuthenticationFilter.java` — filtro que se ejecuta una vez por request; extrae el header `Authorization: Bearer <token>`, verifica la firma/expiración y carga el `UserDetails` desde la BD (vía `JpaUserDetailsService`) para poblar el `SecurityContext`.
- `src/main/java/com/taskflow/config/SecurityConfig.java` — configura la `SecurityFilterChain`: rutas públicas (`/auth/**`, `/info`, Swagger, H2 console), CORS, CSRF disabled (porque se usa token), session stateless y añade el filtro JWT antes del filtro de username/password.
- `src/main/java/com/taskflow/security/ProjectSecurity.java` — bean usado por `@PreAuthorize` para decisiones data-driven (por ejemplo: `@PreAuthorize("hasRole('ADMIN') or @projectSecurity.esOwner(#id, authentication.name)")`).

Comportamiento clave:
- Si no hay token -> request llega como anónimo y los endpoints protegidos devuelven 401 (por el `AuthenticationEntryPoint`).
- Token inválido o expirado -> `JwtAuthenticationFilter` responde 401 JSON (el filtro corre antes del DispatcherServlet, por eso maneja la excepción y corta la cadena).
- Roles/authorities se cargan desde la BD (`UserDetails`), el token solo sirve para identidad/ventana de validez.

5) Organización de tests

Estructura de tests bajo `src/test/java/com/taskflow`:

- Unit tests (sin Spring): `src/test/java/com/taskflow/unit` — ejemplos: `src/test/java/com/taskflow/unit/TaskServiceTest.java`, `TaskValidationTest.java`. Usan JUnit 5 y Mockito.
- Slice tests (tests de capa, arrancando solo partes de Spring): `src/test/java/com/taskflow/slice` — ejemplos: `TaskControllerTest.java`, `ProjectControllerTest.java`, `TaskRepositoryTest.java`. Emplean `@WebMvcTest` o `@DataJpaTest` según corresponda.
- Integration tests (end-to-end con contexto completo, opcionalmente Testcontainers): `src/test/java/com/taskflow/integration` — ejemplo: `TaskflowApiApplicationTests.java`, `TaskRepositoryPostgresIT.java`. Nota: los IT que usan Testcontainers requieren `-Ddocker.tests=true` y no corren por defecto en la CI local.

Reglas prácticas para tests en este repo:
- No modificar tests existentes para hacer que pasen; arreglar el código si fallan.
- Para ejecutar la suite normal: `mvn -q test` (usa el perfil y comportamiento descritos en el README del proyecto).

6) Puntos de interés para empezar a contribuir

- Para entender validaciones: leer `src/main/java/com/taskflow/model/Task.java` y `src/main/java/com/taskflow/mapper/TaskMapper.java`.
- Para añadir endpoints relacionados a tareas bajo proyectos, mirar `src/main/java/com/taskflow/controller/TaskController.java` y sus tests slice en `src/test/java/com/taskflow/slice/TaskControllerTest.java`.
- Para cambios de seguridad, revisar `src/main/java/com/taskflow/config/SecurityConfig.java` y `src/main/java/com/taskflow/security/JwtAuthenticationFilter.java`.
- Para manejo de errores y códigos HTTP uniformes: `src/main/java/com/taskflow/advice/GlobalExceptionHandler.java`.

Si necesitas que convierta esto en una versión más corta (readme para onboarding rápido) o en una diapositiva con lo esencial, indícalo y lo genero.
