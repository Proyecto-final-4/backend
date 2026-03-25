# Finanzas IA — Proyecto Backend (Java/Spring)

## Propósito

Agente de inteligencia artificial que ayuda a personas a llevar el control de sus finanzas personales de forma conversacional, eliminando la fricción de las aplicaciones contables tradicionales (formularios, menús, inicios de sesión complejos).

El usuario interactúa en lenguaje natural: anota gastos, consulta su situación financiera y recibe recomendaciones accionables — todo a través de una interfaz agéntica.

---

## Alcance de este repositorio

Este repositorio es el **backend de datos (Java/Spring Boot)**. Es la **única** capa con acceso directo a la base de datos. No orquesta agentes ni workflows de IA.

### Responsabilidades

| Responsabilidad | Descripción |
|---|---|
| Persistencia | CRUD de todas las entidades del dominio vía JPA/PostgreSQL |
| Autenticación | Emisión y validación de JWT (Bearer token) |
| RAG (Retrieval) | Generación de embeddings y búsqueda vectorial sobre historial del usuario |
| API REST | Endpoints consumidos por el BFF de Next.js |
| Almacenamiento S3 | Subida de documentos para RAG — **fuera del MVP inicial** |

### Fuera de alcance (este repositorio)

- Orquestación de agentes IA (responsabilidad del BFF Next.js con LangChain/LangGraph)
- Lógica conversacional
- Streaming de respuestas
- Notificaciones (SQS/SNS — evaluación futura)
- Ingesta de documentos S3 para RAG — fase posterior al MVP

---

## Arquitectura del sistema

```
┌──────────────────────────────────────────────────────────┐
│                       Usuario                            │
└───────────────────────────┬──────────────────────────────┘
                            │ Interfaz conversacional
┌───────────────────────────▼──────────────────────────────┐
│           BFF / Orquestador IA (Next.js)                 │
│           LangChain · LangGraph · AI SDK                 │
│                                                          │
│  Herramientas (tools) registradas en el agente:          │
│  ├─ /transactions     (registrar y consultar movimientos)│
│  ├─ /summary          (balance y resumen financiero)     │
│  ├─ /budgets          (estado de presupuestos)           │
│  ├─ /goals            (metas de ahorro)                  │
│  └─ /rag/search       (búsqueda semántica en historial)  │
└───────────────────────────┬──────────────────────────────┘
                            │ HTTP + JWT
┌───────────────────────────▼──────────────────────────────┐
│          Este repositorio — Backend Java/Spring          │
│                                                          │
│  ┌──────────────┐  ┌─────────────────┐  ┌────────────┐  │
│  │  Controllers │  │    Services     │  │   Repos    │  │
│  │  (REST API)  │  │ (dominio + RAG) │  │   (JPA)    │  │
│  └──────────────┘  └─────────────────┘  └─────┬──────┘  │
│                                               │          │
│  ┌────────────────────────────────────────────▼───────┐  │
│  │              PostgreSQL 16 + pgvector              │  │
│  │     Datos relacionales + embeddings vectoriales    │  │
│  └────────────────────────────────────────────────────┘  │
│                                                          │
│  ┌───────────────────────────────────────────────────┐   │
│  │   S3 (LocalStack) — subida de docs RAG [futuro]   │   │
│  └───────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────┘
```

---

## Stack tecnológico

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.x |
| ORM | Spring Data JPA / Hibernate |
| Base de datos | PostgreSQL 16 + pgvector |
| Migraciones | Flyway |
| Autenticación | JWT (Bearer token) — Spring Security |
| IA / Embeddings | Spring AI + OpenAI (text-embedding-3-small) |
| Vector store | pgvector vía Spring AI |
| Almacenamiento | AWS S3 / LocalStack — fase futura |
| Build | Maven Wrapper |
| Calidad | SpotBugs · Spotless · Checkstyle |

---

## Entidades del dominio

### Diagrama de relaciones

```
┌──────────────────┐
│      User        │
│────────────────  │
│ id (UUID) PK     │
│ email (unique)   │
│ password_hash    │
│ name             │
│ created_at       │
│ updated_at       │
└──────┬───────────┘
       │ 1
       │ tiene muchos
       ├──────────────────────────────────┐
       │                                  │
       │ N                                │ N
┌──────▼───────────┐            ┌─────────▼────────┐
│   Transaction    │            │     Budget        │
│──────────────────│            │──────────────────-│
│ id (UUID) PK     │            │ id (UUID) PK      │
│ user_id FK       │            │ user_id FK        │
│ category_id FK   │            │ category_id FK    │
│ amount           │            │ amount_limit      │
│ type             │            │ period            │
│ transaction_date │            │ start_date        │
│ description      │            │ end_date          │
│ notes            │            │ is_active         │
│ embedding vector │            │ created_at        │
│ created_at       │            └──────────┬────────┘
│ updated_at       │                       │
└──────────────────┘                       │ N
                                           │
┌──────────────────┐            ┌──────────▼────────┐
│   SavingsGoal    │            │     Category      │
│──────────────────│            │───────────────────│
│ id (UUID) PK     │            │ id (UUID) PK      │
│ user_id FK       │            │ name              │
│ name             │            │ type              │
│ description      │            │ color             │
│ target_amount    │            │ icon              │
│ current_amount   │            │ is_system         │
│ target_date      │            │ user_id FK (null) │
│ is_completed     │            │ parent_id FK      │◄─┐
│ created_at       │            └───────────────────┘  │
│ updated_at       │                     (auto-ref)     │
└──────────────────┘                                   │
                                                       │
                                            categoría padre (opcional)
```

---

### Detalle de cada entidad

#### `User`
Cuenta de usuario. Raíz de todos los datos financieros.

| Campo | Tipo | Notas |
|---|---|---|
| id | UUID | PK generado |
| email | String | Único, usado para login |
| password_hash | String | BCrypt |
| name | String | Nombre para mostrar |
| created_at | Instant | |
| updated_at | Instant | |

---

#### `Category`
Clasificación de transacciones. Incluye categorías del sistema (predefinidas) y categorías personalizadas del usuario.

| Campo | Tipo | Notas |
|---|---|---|
| id | UUID | PK |
| name | String | Ej: "Alimentación", "Transporte" |
| type | Enum | `INCOME` / `EXPENSE` / `BOTH` |
| color | String | Hex, para la UI |
| icon | String | Nombre de icono, para la UI |
| is_system | Boolean | `true` = predefinida, no editable por usuario |
| user_id | UUID FK | `null` si es categoría del sistema |
| parent_id | UUID FK | Auto-referencia para jerarquía (opcional) |

Categorías del sistema iniciales (via Flyway seed):
- **Gastos:** Alimentación, Transporte, Salud, Vivienda, Entretenimiento, Educación, Ropa, Otros gastos
- **Ingresos:** Salario, Freelance, Inversiones, Otros ingresos

---

#### `Transaction`
Registro central de cada movimiento financiero del usuario. Almacena el embedding vectorial de la descripción para habilitar el RAG.

| Campo | Tipo | Notas |
|---|---|---|
| id | UUID | PK |
| user_id | UUID FK | Propietario |
| category_id | UUID FK | Categoría asignada |
| amount | BigDecimal | Siempre positivo; `type` define la dirección |
| type | Enum | `INCOME` / `EXPENSE` |
| transaction_date | LocalDate | Fecha del movimiento (no necesariamente hoy) |
| description | String | Texto libre — lo que escribió el usuario |
| notes | String | Notas adicionales opcionales |
| embedding | vector(1536) | Generado por Spring AI al guardar; habilita RAG |
| created_at | Instant | |
| updated_at | Instant | |

**Nota sobre el embedding:** Al crear o actualizar una transacción, el servicio genera un embedding del campo `description` usando OpenAI (`text-embedding-3-small`) y lo persiste en la columna `vector`. El endpoint `/rag/search` ejecuta una búsqueda de similitud coseno sobre esta columna filtrada por `user_id`.

---

#### `Budget`
Límite de gasto definido por el usuario para una categoría en un período determinado.

| Campo | Tipo | Notas |
|---|---|---|
| id | UUID | PK |
| user_id | UUID FK | Propietario |
| category_id | UUID FK | Categoría limitada |
| amount_limit | BigDecimal | Límite máximo de gasto |
| period | Enum | `DAILY` / `WEEKLY` / `MONTHLY` |
| start_date | LocalDate | Inicio de vigencia |
| end_date | LocalDate | Fin de vigencia (`null` = indefinido) |
| is_active | Boolean | Permite pausar sin eliminar |
| created_at | Instant | |

---

#### `SavingsGoal`
Meta de ahorro con un objetivo monetario y fecha límite opcional.

| Campo | Tipo | Notas |
|---|---|---|
| id | UUID | PK |
| user_id | UUID FK | Propietario |
| name | String | Ej: "Vacaciones", "Fondo de emergencia" |
| description | String | Descripción opcional |
| target_amount | BigDecimal | Cuánto se quiere ahorrar |
| current_amount | BigDecimal | Progreso acumulado (actualizado manualmente o por regla) |
| target_date | LocalDate | Fecha objetivo (`null` = sin límite) |
| is_completed | Boolean | Marcada como alcanzada |
| created_at | Instant | |
| updated_at | Instant | |

---

## API REST — Endpoints

### Autenticación (`/auth`)
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/auth/register` | Crear cuenta nueva |
| POST | `/auth/login` | Login, devuelve JWT |

### Perfil (`/users`)
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/users/me` | Datos del usuario autenticado |
| PUT | `/users/me` | Actualizar nombre o contraseña |

### Transacciones (`/transactions`)
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/transactions` | Crear transacción (genera embedding) |
| GET | `/transactions` | Listar con filtros (fecha, categoría, tipo, paginación) |
| GET | `/transactions/{id}` | Detalle de una transacción |
| PUT | `/transactions/{id}` | Actualizar (regenera embedding si cambia descripción) |
| DELETE | `/transactions/{id}` | Eliminar |

### Categorías (`/categories`)
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/categories` | Listar todas (sistema + personalizadas del usuario) |
| POST | `/categories` | Crear categoría personalizada |
| PUT | `/categories/{id}` | Editar (solo categorías propias) |
| DELETE | `/categories/{id}` | Eliminar (solo categorías propias, sin transacciones) |

### Resumen financiero (`/summary`)
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/summary` | Balance actual, totales por tipo y por categoría del período |
| GET | `/summary/trends` | Comparativa entre períodos (mes actual vs anterior, etc.) |

### Presupuestos (`/budgets`)
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/budgets` | Listar presupuestos activos |
| POST | `/budgets` | Crear presupuesto |
| PUT | `/budgets/{id}` | Actualizar |
| DELETE | `/budgets/{id}` | Eliminar |
| GET | `/budgets/{id}/status` | Gasto acumulado vs límite en el período actual |

### Metas de ahorro (`/goals`)
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/goals` | Listar metas |
| POST | `/goals` | Crear meta |
| PUT | `/goals/{id}` | Actualizar (incluye progreso) |
| DELETE | `/goals/{id}` | Eliminar |

### RAG (`/rag`)
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/rag/search` | Búsqueda semántica en el historial del usuario. Body: `{ query, limit }` |

---

## Fases de desarrollo

### Fase 1 — MVP (este sprint)
- [ ] Entidades JPA + migraciones Flyway
- [ ] Autenticación JWT (register / login)
- [ ] CRUD Transacciones con generación de embeddings
- [ ] CRUD Categorías (con seed de categorías del sistema)
- [ ] Endpoint `/summary` (balance básico)
- [ ] Endpoint `/rag/search`

### Fase 2 — Completar funcionalidades core
- [ ] CRUD Presupuestos + endpoint `/budgets/{id}/status`
- [ ] CRUD Metas de ahorro
- [ ] Endpoint `/summary/trends`
- [ ] Filtros y paginación completos en `/transactions`

### Fase 3 — Funcionalidades avanzadas
- [ ] Subida de documentos a S3 para RAG ampliado
- [ ] Notificaciones asíncronas (SQS/SNS)
- [ ] Exportación de reportes

---

## Decisiones de arquitectura confirmadas

| Decisión | Elección |
|---|---|
| Acceso a base de datos | Solo este backend (Java) |
| Autenticación | JWT Bearer token — Spring Security |
| Orquestación de agentes | Next.js BFF (LangChain/LangGraph) |
| RAG knowledge base | Solo historial de transacciones del usuario |
| Embeddings | Generados en Java (Spring AI + OpenAI), almacenados en pgvector |
| RAG expuesto como | Endpoint REST `/rag/search` consumido como tool por el agente |
| Streaming | No requerido en esta fase |
| S3 | Implementación futura (post-MVP) |
| SQS/SNS | Evaluación futura |

---

## Convenciones del proyecto

- Idioma del código: inglés (clases, métodos, variables, commits)
- Idioma de documentación: español
- Commits: Conventional Commits (`feat:`, `fix:`, `chore:`, etc.)
- Formato de código: Google Java Format (Spotless)
- Estilo: Google Checks (Checkstyle)
- Todo cambio pasa por CI (quality → integration tests)
