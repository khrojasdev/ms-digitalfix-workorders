# ms-digitalfix-workorders

**Proyecto:** DigitalFix - Gestion de ordenes de trabajo para una red de 20 empresas de mantenimiento electrico  
**Componente:** Microservicio  
**Asignatura:** DSY1107 Desarrollo Cloud Native I - Duoc UC  
**Primera entrega (EP1):** 14 de septiembre de 2026

## Descripcion

Gestion de ordenes de trabajo: creacion, maquina de estados, asignacion y trazabilidad.

## Tecnologias

Spring Boot 3, Spring Data JPA, Oracle, Flyway, RabbitMQ, Kafka

## API

Todas las rutas llegan por el BFF, que agrega la empresa, el usuario y los roles.

| Metodo | Ruta | Quien | Que hace |
|---|---|---|---|
| POST | `/api/workorders` | ADMIN, SUPERVISOR, CLIENTE | Crea la orden (queda CREADA) |
| GET | `/api/workorders?status=&from=&to=` | todos menos TECNICO | Lista con filtros opcionales; el CLIENTE ve solo las suyas |
| GET | `/api/workorders/{id}` | segun rol | Detalle con su historial (CLIENTE: las suyas; TECNICO: las asignadas) |
| GET | `/api/workorders/mias` | TECNICO | Su ficha y sus ordenes asignadas |
| PUT | `/api/workorders/{id}/status` | ADMIN, SUPERVISOR, TECNICO | `{"status": "...", "comentario": "opcional"}` |
| PUT | `/api/workorders/{id}/asignacion` | ADMIN, SUPERVISOR | `{"tecnicoId": 3, "comentario": "opcional"}` |
| GET/POST/PUT/DELETE | `/api/workorders/tecnicos` | ADMIN, SUPERVISOR | Tecnicos de terreno |

Ciclo de la orden: `CREADA -> ASIGNADA -> EN_DESPLAZAMIENTO -> EN_EJECUCION -> CERRADA`,
con `CANCELADA` desde CREADA, ASIGNADA o EN_DESPLAZAMIENTO. Un cambio no permitido
responde 409 (por ejemplo "No se puede pasar a EN_EJECUCION sin ASIGNAR un tecnico").

Cada paso queda en `historial_orden` con quien lo hizo y el comentario, si lo hubo.

El tecnico no se da de alta dos veces: cuando entra por primera vez con el rol
TECNICO del tenant, se vincula con la ficha que tenga su mismo correo o, si no
existe, se le crea una con el nombre y el correo de su cuenta.

## Mensajeria (RabbitMQ)

Productor. Cada cambio se publica en `ordenes.negocio.exchange` (topic) con la
routing key `ordenes.eventos.<tipo>`, por ejemplo `ordenes.eventos.asignada`.
Nombres en `application.yml` (`amqp.*`) y exchange declarado en `config/RabbitMQConfig`.

| Consumidor | Cola | Para que |
|---|---|---|
| ms-digitalfix-audit | `ordenes.auditoria.queue` | Bitacora |
| ms-digitalfix-report | `ordenes.reportes.queue` | Indicadores |
| ms-digitalfix-notify | `ordenes.avisos.queue` | Avisos al cliente y ticket de despacho |
| ms-digitalfix-catalog | `catalogo.stock.ordenes.queue` | Descuento de stock al asignar |

El evento lleva `eventKey` (idempotencia), `correlacionId` (si la peticion no trae
uno se usa la propia clave), estado anterior y nuevo, servicio, tecnico y la nota
del cambio.

## Integrantes

| Integrante | GitHub |
|---|---|
| Kevin Rojas | @khrojasdev |
| Christopher Perez | @ChrisPerezV |
| Diego Lopez | @DiegoLopez-f |

## Repositorios del proyecto

- [`digitalfix-frontend`](https://github.com/khrojasdev/digitalfix-frontend) - Frontend
- [`ms-digitalfix-bff`](https://github.com/khrojasdev/ms-digitalfix-bff) - BFF / API interna
- [`ms-digitalfix-usuarios`](https://github.com/khrojasdev/ms-digitalfix-usuarios) - Microservicio
- [`ms-digitalfix-catalog`](https://github.com/khrojasdev/ms-digitalfix-catalog) - Microservicio
- [`ms-digitalfix-workorders`](https://github.com/khrojasdev/ms-digitalfix-workorders) - Microservicio **(este)**
- [`ms-digitalfix-notify`](https://github.com/khrojasdev/ms-digitalfix-notify) - Microservicio
- [`ms-digitalfix-report`](https://github.com/khrojasdev/ms-digitalfix-report) - Microservicio
- [`ms-digitalfix-audit`](https://github.com/khrojasdev/ms-digitalfix-audit) - Microservicio
- [`digitalfix-infra`](https://github.com/khrojasdev/digitalfix-infra) - Infraestructura


## Tablero

El backlog completo vive en un unico GitHub Project que enlaza los ocho repositorios.
Columnas: Backlog, To Do, In Progress, In Review, Done. Limite de trabajo en curso: 2 tarjetas por persona.

## Como se trabaja aqui

1. Cada tarea del tablero tiene su propia rama, indicada en el cuerpo del issue.
2. `git switch <rama>` - nunca se trabaja directamente sobre `main`.
3. Commits con Conventional Commits: `feat(catalogo): agrega endpoint de servicios`.
4. Pull request hacia `main` con `Closes #<numero del issue>` en la descripcion.
5. Revisa un companero distinto del autor. Recien ahi se hace merge.

Referencias entre repositorios: `khrojasdev/otro-repo#12`.

## Configuracion

Ninguna credencial vive en este repositorio. Todo llega por variables de entorno;
revisa `.env.example` para saber cuales.
