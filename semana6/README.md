# Proyecto final · Semana 6 · GitHub Copilot

**Alumno:** `Rodrigo Pacheco Pérez` · **Usuario de GitHub:** `rodrigopp-dev`

## 1. Qué construí

| | Feature | Especificación |
|---|---|---|
| [X] | `GET /reports/progress` — avance por proyecto | [`specs/progress.md`](../specs/progress.md) |

## 2. El pull request

- **URL del PR (mergeado):** `https://github.com/rodrigopp-dev/taskflow-copilot-rodrigo/pull/5`
- **Commit del merge en `main`:** `1d60724 (HEAD -> main, origin/main, origin/HEAD) Merge pull request #5 from rodrigopp-dev/feature/progress`
- **Comentarios de Copilot code review:** `1`

## 3. Cómo lo hice

| Paso | Qué hice | Evidencia |
|---|---|---|
| Rama y spec | `git switch -c feature/progress` y copié la spec a `specs/` | `git log --oneline main..feature/progress` (antes del merge) |
| Implementación | `copilot -p "/crear-endpoint-taskflow …"` con `gpt-5-mini` | `semana6/sesion-implementacion.md` (tiene la línea `Skill "crear-endpoint-taskflow" loaded successfully`) |
| Revisión | agente `revisor` sobre `semana6/proyecto-final.diff` | `semana6/revision.md` (termina con `Veredicto: APROBADO`) |
| Tests | `mvn test` en verde | `[INFO] Tests run: 79, Failures: 0, Errors: 0, Skipped: 0` |
| Comprobación REST | `verificar.ps1` con `casos-progress.ps1` | sección 5 de este documento |
| Code review | Copilot en el PR | la pestaña *Files changed* del PR |

## 4. Qué hizo el agente y qué corregí yo

| # | Qué hizo mal el agente (archivo) | Quién lo detectó | Cómo quedó corregido |
|---|---|---|---|
| 1 | `Copilot Code Review afirmó que .github/skills/verificar-taskflow/verificar.ps1 construía $auth con un token de reemplazo ******, por lo que los PATCH recibirían 401.` | `Copilot review` | `El hallazgo resultó falso. verificar.ps1 construye $auth con Authorization = "Bearer $($login.token)" después del login real.` |

## 5. Comprobaciones REST

```text
Repositorio: C:\Users\User\git\taskflow-copilot-rodrigo
URL de la app: http://127.0.0.1:8080
Empaquetando con Maven (mvn -q package -DskipTests), tarda unos segundos...
App arrancando (PID 8956). Esperando a que /info responda...
App lista en 8 s.
[OK]    GET /tasks/overdue devuelve solo la tarea 7
[OK]    GET /tasks/unassigned devuelve las tareas 4 y 6
[OK]    GET /projects/1/summary
[OK]    GET /projects/2/summary
[OK]    GET /projects/3/summary
[OK]    GET /projects/99/summary responde 404
[OK]    GET /projects/1/summary sin token responde 401
[OK]    GET /reports/progress devuelve los 3 proyectos con la semilla
[OK]    El proyecto 1 se llama «Plataforma TaskFlow»
[OK]    Tras pasar la tarea 1 a DONE, el proyecto 1 queda en 2/5 = 40
[OK]    GET /reports/progress sin token responde 401
App detenida (PID 8956).
[OK]    App apagada: el puerto 8080 ya no responde
RESULTADO: 12/12 OK
```

## 6. Créditos de la semana

| Qué | AI credits |
|---|---|
| Usados en septiembre según github.com (incluye semanas anteriores si usaste Copilot antes) | `179 / 1500 AI credits` |
| Implementación con la skill (`AI Credits` del PF-2) | `5.18` |
| Revisión del `revisor` (`AI Credits` del PF-3) | `2.06` |
| Correcciones del PF-4 y del PF-6, si hubo (`AI Credits`) | `0` |
