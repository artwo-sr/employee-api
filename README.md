# Employee Microservice

API REST empresarial para la gestión de empleados, construida con **Spring Boot 3.4.1** y **Java 17**. Diseñada siguiendo los principios de **Arquitectura Limpia**, **SOLID** y **API First**, lista para despliegue en entornos de producción con Oracle Database.

---

## Tabla de Contenidos

1. [Descripción del Proyecto](#descripción-del-proyecto)
2. [Stack Tecnológico](#stack-tecnológico)
3. [Estructura del Proyecto](#estructura-del-proyecto)
4. [Prerrequisitos](#prerrequisitos)
5. [Variables de Entorno](#variables-de-entorno)
6. [Ejecución Local con Maven](#ejecución-local-con-maven)
7. [Ejecución con Docker Compose](#ejecución-con-docker-compose)
8. [Ejecución de Pruebas](#ejecución-de-pruebas)
9. [Endpoints de la API](#endpoints-de-la-api)
10. [Documentación Swagger](#documentación-swagger)
11. [Monitoreo — Spring Actuator](#monitoreo--spring-actuator)
12. [Seguridad](#seguridad)
13. [Colección Postman](#colección-postman)
14. [Pipeline CI/CD](#pipeline-cicd)

---

## Descripción del Proyecto

Este microservicio expone una API REST completa para la gestión del catálogo de empleados de una organización. Permite:

- **Consultar** empleados individuales o en lista completa.
- **Registrar** uno o múltiples empleados en una sola solicitud atómica (**Batch Insert**).
- **Actualizar** todos o sólo los campos requeridos de un empleado (**Full/Partial Update**).
- **Eliminar** empleados por ID.
- **Buscar** empleados por nombre parcial, insensible a mayúsculas/minúsculas.

Todos los errores son manejados globalmente mediante `@RestControllerAdvice` y devueltos en un formato JSON estandarizado con códigos HTTP semánticos.

---

## Stack Tecnológico

| Componente            | Tecnología                          |
|-----------------------|-------------------------------------|
| Lenguaje              | Java 17                             |
| Framework             | Spring Boot 3.4.1                   |
| Persistencia          | Spring Data JPA + Hibernate         |
| Base de datos (prod)  | Oracle Database 21c XE              |
| Base de datos (dev)   | H2 In-Memory                        |
| Seguridad             | Spring Security (HTTP Basic Auth)   |
| Documentación API     | SpringDoc OpenAPI 3 / Swagger UI    |
| Monitoreo             | Spring Boot Actuator                |
| Logging               | Logback (vía `application.yml`)     |
| Pruebas               | JUnit 5 + Mockito + AssertJ         |
| Build                 | Apache Maven 3.9                    |
| Contenedores          | Docker + Docker Compose             |
| CI/CD                 | GitHub Actions                      |

---

## Estructura del Proyecto

```
employee-microservice/
├── src/
│   ├── main/
│   │   ├── java/com/enterprise/employee/
│   │   │   ├── config/          # SecurityConfig, OpenApiConfig
│   │   │   ├── controller/      # EmployeeController
│   │   │   ├── dto/             # EmployeeRequestDTO, EmployeeResponseDTO
│   │   │   ├── entity/          # Employee, Sex
│   │   │   ├── exception/       # Excepciones personalizadas + GlobalExceptionHandler
│   │   │   ├── filter/          # RequestHeaderLoggingFilter
│   │   │   ├── mapper/          # EmployeeMapper
│   │   │   ├── repository/      # EmployeeRepository
│   │   │   └── service/         # EmployeeService + EmployeeServiceImpl
│   │   └── resources/
│   │       └── application.yml  # Configuración multi-perfil (dev/test/prod)
│   └── test/
│       └── java/com/enterprise/employee/
│           ├── controller/      # EmployeeControllerTest (WebMvcTest)
│           └── service/         # EmployeeServiceImplTest (Mockito)
├── .github/workflows/ci.yml     # Pipeline GitHub Actions
├── docker-compose.yml           # Oracle XE + API
├── Dockerfile                   # Multi-stage build
├── postman_collection.json      # Colección Postman v2.1
└── pom.xml
```

---

## Prerrequisitos

Para ejecutar el proyecto **localmente con Maven** (perfil `dev`, H2 in-memory):

| Herramienta | Versión mínima |
|-------------|----------------|
| Java JDK    | 17             |
| Maven       | 3.9            |

Para ejecutar con **Docker Compose** (perfil `prod`, Oracle XE):

| Herramienta    | Versión mínima |
|----------------|----------------|
| Docker Engine  | 24.x           |
| Docker Compose | v2.x           |

> **Nota:** La imagen `gvenzl/oracle-xe:21-slim-faststart` requiere al menos **2 GB de RAM** disponibles para el contenedor de Oracle.

---

## Variables de Entorno

Crea un archivo `.env` en la raíz del proyecto (no lo incluyas en control de versiones):

```env
# Oracle Database
ORACLE_SYSTEM_PASSWORD=Oracle123!
DB_USERNAME=employee_user
DB_PASSWORD=Emp@ssw0rd!
ORACLE_DATABASE=EMPLOYEEDB

# Aplicación
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=prod

# Credenciales de la API (sólo para el perfil prod con InMemoryUserDetailsManager)
ADMIN_PASSWORD=admin123
VIEWER_PASSWORD=viewer123
```

> **Importante:** En producción real, las credenciales deben gestionarse con un gestor de secretos (HashiCorp Vault, AWS Secrets Manager, etc.) y **nunca** almacenarse en texto plano.

---

## Ejecución Local con Maven

El perfil `dev` utiliza una base de datos **H2 en memoria**, por lo que no requiere ninguna instalación adicional.

```bash
# Clonar el repositorio
git clone https://github.com/artwo-sr/employee-api.git
cd employee-api

# Compilar y ejecutar con el perfil 'dev' (por defecto)
mvn spring-boot:run

# O bien, especificando el perfil explícitamente
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Y en caso de tener el puerto 8080 ocupado por docker, especificar el puerto 8081
mvn spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments="--server.port=8081"
```


La aplicación estará disponible en: `http://localhost:8080/api/v1`

La consola H2 estará disponible en: `http://localhost:8080/api/v1/h2-console`
- **JDBC URL:** `jdbc:h2:mem:employeedb`
- **Usuario:** `sa`
- **Contraseña:** *(vacía)*

---

## Ejecución con Docker Compose
> 💡 **Nota:** Esta es la forma recomendada para evaluar el proyecto. Al utilizar Docker, **no necesitas tener instalado Java, Maven ni Oracle en tu máquina local**. El contenedor se encarga de descargar las dependencias, compilar el proyecto a través de un build multi-etapa y levantar la base de datos de forma totalmente aislada.

```bash
# 1. Crear el archivo .env con las variables (ver sección anterior)
cp .env.example .env   # Ajusta los valores

# 2. Construir la imagen y levantar los servicios
docker compose up --build -d

# 3. Verificar que ambos servicios estén corriendo
docker compose ps

# 4. Ver logs en tiempo real de la API
docker compose logs -f employee-api

# 5. Detener los servicios (conserva los datos)
docker compose down

# 6. Detener los servicios y borrar el volumen de Oracle
docker compose down -v
```

> **Primera ejecución:** Oracle XE puede tardar entre 60 y 120 segundos en inicializarse completamente. La API esperará automáticamente a que la base de datos reporte estado `healthy` antes de arrancar.

---

## Ejecución de Pruebas

Las pruebas unitarias utilizan el perfil `test` con **H2 en modo Oracle**, por lo que no necesitan Oracle instalado.

```bash
# Ejecutar todas las pruebas unitarias
mvn clean test

# Ejecutar pruebas y generar reporte de cobertura (Surefire)
mvn clean verify

# Ejecutar únicamente las pruebas del servicio
mvn test -Dtest=EmployeeServiceImplTest

# Ejecutar únicamente las pruebas del controlador
mvn test -Dtest=EmployeeControllerTest
```

Los reportes de pruebas se generan en: `target/surefire-reports/`

### Cobertura de pruebas

| Clase bajo prueba        | Tipo de prueba       | Escenarios cubiertos |
|--------------------------|----------------------|----------------------|
| `EmployeeServiceImpl`    | Unitaria (Mockito)   | getAllEmployees, getById (éxito/fallo), createEmployees (single/batch/validación de negocio), updateEmployee (completo/parcial/no encontrado), deleteEmployee (éxito/fallo), searchByName |
| `EmployeeController`     | Web Slice (WebMvcTest) | GET all, GET by ID (200/404/400 tipo), POST (201/400 validación/400 fecha), PUT (200/404), DELETE (204/404), GET search |

---

## Endpoints de la API

Base URL: `http://localhost:8080/api/v1`

| Método   | Endpoint                         | Descripción                                    | Status exitoso |
|----------|----------------------------------|------------------------------------------------|----------------|
| `GET`    | `/employees`                     | Obtener todos los empleados                    | 200 OK         |
| `GET`    | `/employees/{id}`                | Obtener empleado por ID                        | 200 OK         |
| `POST`   | `/employees`                     | Crear uno o múltiples empleados (batch)        | 201 Created    |
| `PUT`    | `/employees/{id}`                | Actualizar empleado (total o parcial)          | 200 OK         |
| `DELETE` | `/employees/{id}`                | Eliminar empleado por ID                       | 204 No Content |
| `GET`    | `/employees/search?name={name}`  | Búsqueda parcial por nombre (case-insensitive) | 200 OK         |

### Ejemplo: Crear empleado (`POST /employees`)

**Request:**
```json
[
  {
    "firstName":      "Carlos",
    "middleName":     "Alberto",
    "fatherLastName": "García",
    "motherLastName": "López",
    "age":            32,
    "sex":            "MALE",
    "birthDate":      "10-03-1993",
    "position":       "Software Engineer",
    "active":         true
  }
]
```

**Response `201 Created`:**
```json
[
  {
    "id":                     1,
    "firstName":              "Carlos",
    "middleName":             "Alberto",
    "fatherLastName":         "García",
    "motherLastName":         "López",
    "age":                    32,
    "sex":                    "MALE",
    "birthDate":              "10-03-1993",
    "position":               "Software Engineer",
    "systemRegistrationDate": "20-04-2026 10:30:00",
    "active":                 true
  }
]
```

### Ejemplo: Error de validación (`400 Bad Request`)

```json
{
  "timestamp":   "20-04-2026 10:31:05",
  "status":      400,
  "error":       "Bad Request",
  "message":     "Validation failed for 1 field(s)",
  "path":        "/api/v1/employees",
  "fieldErrors": [
    {
      "field":         "firstName",
      "rejectedValue": "",
      "message":       "First name is required"
    }
  ]
}
```

---

## Documentación Swagger

La documentación interactiva de la API está disponible en el perfil `dev`:

| Recurso              | URL                                              |
|----------------------|--------------------------------------------------|
| Swagger UI           | http://localhost:8080/api/v1/swagger-ui.html     |
| OpenAPI JSON         | http://localhost:8080/api/v1/v3/api-docs         |

> En un despliegue en producción real, considera deshabilitar el Swagger UI configurando `springdoc.swagger-ui.enabled=false` y `springdoc.api-docs.enabled=false` para proteger la especificación del API.

Para autenticarse en el Swagger UI, haz clic en el botón **Authorize** e ingresa:
- **Username:** `admin`
- **Password:** `admin123`

---

## Monitoreo — Spring Actuator

| Endpoint               | URL                                              | Descripción                        |
|------------------------|--------------------------------------------------|------------------------------------|
| Health Check           | http://localhost:8080/api/v1/actuator/health     | Estado de la aplicación y BD       |
| Información            | http://localhost:8080/api/v1/actuator/info       | Versión y metadata del microservicio |
| Métricas               | http://localhost:8080/api/v1/actuator/metrics    | Métricas de JVM y HTTP (dev)       |
| Loggers                | http://localhost:8080/api/v1/actuator/loggers    | Niveles de logging en tiempo real (dev) |

En el perfil `prod`, sólo se exponen los endpoints `health` e `info`, y los detalles de salud requieren autenticación.

---

## Seguridad

- **Mecanismo:** HTTP Basic Authentication (stateless — sin sesión del lado del servidor).
- **Contraseñas:** Almacenadas con hash **BCrypt** (factor de costo 10).
- **Headers sensitivos:** El `RequestHeaderLoggingFilter` enmascara automáticamente los valores de `Authorization`, `Cookie`, `X-Api-Key` y otros headers sensibles en los logs.
- **Endpoints públicos:** Swagger UI, OpenAPI docs, Actuator y H2 Console (sólo dev) son accesibles sin autenticación.
- **CSRF:** Deshabilitado (apropiado para APIs REST stateless que no usan cookies de sesión).

| Usuario  | Contraseña   | Rol      |
|----------|--------------|----------|
| `admin`  | `admin123`   | `ADMIN`  |
| `viewer` | `viewer123`  | `VIEWER` |

> **Nota de producción:** `InMemoryUserDetailsManager` es adecuado para pruebas. Para producción, reemplazarlo con `JdbcUserDetailsManager` o un `UserDetailsService` personalizado conectado a la base de datos de usuarios.

---

## Colección Postman

Importa el archivo `postman_collection.json` incluido en la raíz del proyecto:

1. Abre Postman → `File` → `Import`
2. Selecciona `postman_collection.json`
3. Verifica que la variable de entorno `baseUrl` esté en `http://localhost:8080/api/v1`
4. Usa la autenticación heredada configurada en la colección (`admin / admin123`)

La colección incluye los 6 endpoints del negocio más los endpoints de Actuator y Swagger.

---

## Pipeline CI/CD

El proyecto incluye un workflow de **GitHub Actions** ubicado en `.github/workflows/ci.yml`.

**Se activa en:**
- `push` a la rama `main`
- Apertura / actualización de un `pull_request` hacia `main`

**Pasos del pipeline:**

1. Checkout del código fuente
2. Instalación de JDK 17 (Eclipse Temurin)
3. Cache del repositorio Maven local (clave: hash de `pom.xml`)
4. `mvn clean verify` — compila y ejecuta todas las pruebas con perfil `test` (H2, sin Oracle)
5. Publicación del reporte JUnit en la pestaña "Tests" del workflow
6. Build de la imagen Docker (smoke check — sin push al registry)

---

*Desarrollado como entregable de evaluación técnica para arquitectura de microservicios empresariales.*
