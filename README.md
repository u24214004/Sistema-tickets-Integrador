# TechConsulting Perú · Sistema de tickets

Primer incremento del APF3 del curso Integrador I, UTP. Implementa acceso y gestión inicial de tickets del alcance recortado. Las funciones de horas, bolsas, SLA y liquidación se desarrollarán en la siguiente etapa.

## Funciones disponibles

- Login real con BCrypt y JWT de una hora.
- Dos roles: INTERNO (administrador/consultor) y CLIENTE.
- Creación, listado paginado, filtros y detalle de tickets.
- Correlativo estable TC-AÑO-ID, calculado a partir del identificador y la fecha de creación.
- Indicadores consultados en la API: total, abiertos, en proceso y resueltos.
- Aislamiento de empresas en el backend; el cliente no elige otra empresa.
- Registro del evento de creación dentro de la misma transacción.
- Migraciones Flyway, Logback con rotación y Commons Lang para normalización.

## Tecnologías

Java 21, Spring Boot 3.5.16, Spring Security, Spring Data JPA, PostgreSQL, Maven Wrapper; React 19, TypeScript, Vite y CSS adaptable. H2 se utiliza exclusivamente en el perfil de demostración y las pruebas rápidas. El frontend usa componentes pequeños sin una dependencia adicional de UI.

## Arranque rápido sin configurar PostgreSQL

Requiere JDK 21 y Node compatible con Vite (el desarrollo fue verificado con Node 24.11.1). Ejecuta desde la raíz en dos terminales PowerShell:

```powershell
# Terminal 1
.\scripts\Start-Backend.ps1 -Demo

# Terminal 2
.\scripts\Start-Frontend.ps1
```

Abre http://localhost:5173. El perfil demo usa una base de datos en memoria: los registros se pierden al reiniciar el backend. No modifica el PostgreSQL instalado.

## Ejecución con PostgreSQL persistente

Con Docker Desktop en ejecución:

```powershell
.\scripts\Initialize-Local.ps1
docker compose -p tickets-apf3 up -d --wait
.\scripts\Start-Backend.ps1 -SeedDemo
```

Ejecuta el frontend en otra terminal. Docker publica PostgreSQL en 55432, sin interferir con un servidor instalado en 5432. Los datos se conservan en el volumen tickets-apf3_tickets_data. `-SeedDemo` crea cuentas ficticias solo si no hay usuarios; no usarlo para datos reales. Para posteriores arranques puedes omitirlo.

Para usar tu propio PostgreSQL, configura DB_URL, DB_USER y DB_PASSWORD en .env. El usuario necesita permisos de creación de tablas en una base dedicada. Configura también JWT_SECRET con al menos 32 bytes. El arranque normal requiere este secreto y no crea cuentas de prueba automáticamente.

## Arranque directo desde terminal (sin scripts)

Spring Boot carga automáticamente el archivo `.env` de la raíz al ejecutar desde `backend` o desde la raíz del proyecto. El archivo utiliza líneas `CLAVE=valor` sin comillas ni instrucciones `export`. Las variables del entorno conservan prioridad; si quedaron valores antiguos en la terminal, abre una terminal nueva.

Con JDK 21 activo y PostgreSQL iniciado:

```powershell
cd C:\PROY_SA\Integrador\sistema_tickets_app\backend
.\mvnw.cmd spring-boot:run
```

Si el contenedor está detenido, desde la raíz ejecuta antes `docker compose -p tickets-apf3 up -d --wait`. En otra terminal, entra a `frontend` y ejecuta `npm.cmd run dev`.

Si Maven utiliza otro JDK, configura Java 21 en esa terminal:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

## Cuentas ficticias de demostración

Todas utilizan la contraseña **Demo2026!**, únicamente en demo o con SeedDemo:

| Cuenta | Perfil | Empresa |
| --- | --- | --- |
| equipo@demo.local | Interno | Todas |
| cliente@demo.local | Cliente | Andina Industrial |
| otrocliente@demo.local | Cliente | Comercial del Pacífico |

## Comprobaciones

```powershell
# En backend, con JAVA_HOME apuntando al JDK 21
.\mvnw.cmd test
.\mvnw.cmd package
.\mvnw.cmd dependency:tree

# En frontend
npm.cmd ci
npm.cmd test
npm.cmd run build
npm.cmd run format:check
```

Para repetir la suite con PostgreSQL real, ejecuta `scripts/Test-Postgres.ps1` desde la raíz con el contenedor iniciado.

La suite de API verifica autenticación, manipulación de JWT, separación de clientes, creación y validación. Las pruebas React verifican acceso, rechazo de credenciales y envío del formulario.

## Seguridad y límites de este incremento

El token se conserva solo en memoria del navegador; recargar exige iniciar sesión. Salir descarta el token local, pero no revoca JWT ya emitidos (expiran a la hora). Se usa Authorization Bearer, sin cookies de autenticación, por lo que CSRF está deshabilitado en esta API stateless. Las contraseñas y los tokens no se registran en logs. .env está excluido de Git. Los servicios se enlazan a loopback para uso local.

Los estados adicionales están definidos para la próxima etapa; en este incremento los tickets se crean abiertos y no existe todavía una operación de cambio de estado. Los indicadores no simulan actividad. El histórico de creación no se expone como una bitácora completa ni se garantiza inmutabilidad frente a administradores de la base de datos.

Antes de una publicación real faltan HTTPS, limitación de intentos de login, gestión de cuentas y revisión de seguridad del despliegue. Este repositorio no se ha publicado en GitHub ni desplegado en la nube.

## Documentación

- [Arquitectura y decisiones](docs/ARQUITECTURA.md)
- [Contrato de API](docs/API.md)
- [Alcance y backlog](docs/ALCANCE_APF3.md)
- [Checklist y evidencias](docs/EVIDENCIAS_APF3.md)
- [Resultados de verificación](docs/VERIFICACION.md)
