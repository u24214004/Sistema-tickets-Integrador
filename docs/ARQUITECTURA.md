# Arquitectura del primer incremento

El sistema es un monolito modular con API REST en Java y una SPA React. Se organiza por autenticación, empresas, tickets y configuración.

La petición atraviesa Spring Security, un controlador REST, el servicio de tickets, repositorios Spring Data JPA y la base relacional. Los controladores validan DTO y traducen HTTP; TicketService impone las reglas de creación y acceso. Las entidades no se serializan directamente. La separación de empresas se calcula con el usuario autenticado en el servidor.

## Decisiones

1. **Java 21 / Spring Boot 3**: preservan la tecnología comprometida por el equipo. Supabase no reemplaza la lógica Java.
2. **Dos roles**: INTERNO agrupa administrador y consultor; CLIENTE opera dentro de su empresa.
3. **JWT con Spring Security/Nimbus**: firma HS256, issuer y expiración validados. El secreto se obtiene del entorno; en demo puede generarse aleatoriamente y cambia al reiniciar.
4. **Token en memoria**: se elimina al salir o recargar. Evita persistir credenciales de sesión en almacenamiento del navegador.
5. **Migraciones**: Flyway crea companies, app_users, tickets y ticket_events. Hibernate valida el esquema y no lo modifica.
6. **Código de ticket**: TC-AÑO-ID derivado del ID y la fecha inalterable de creación, usando zona America/Lima. Los identificadores pueden tener saltos y no se reinician cada año.
7. **Persistencia**: PostgreSQL para el proyecto; H2 en modo PostgreSQL para pruebas rápidas y demo. También se valida la API contra un PostgreSQL real aislado.
8. **Frontend**: React/TypeScript/Vite con componentes separados y CSS adaptable; se evita añadir bibliotecas de estado o UI antes de necesitarlas.
9. **Logs**: Logback registra identificadores, no descripciones, correos ni credenciales. Rotación diaria/tamaño con límite de 50 MB.
10. **Commons Lang**: normalizeSpace y strip normalizan asunto y descripción. Bean Validation mantiene la validación en la entrada HTTP.

## Próximas decisiones

En la siguiente etapa se incorporarán contratos e imputaciones con BigDecimal, actualización transaccional de consumo, cambio de estado, semáforo SLA simplificado y exportación Excel con Apache POI. El PDF se resolverá mediante impresión del navegador. No se implementarán calendario laboral, autorización de sobregiros ni notificaciones de correo.
