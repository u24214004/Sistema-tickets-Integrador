# Alcance de desarrollo y backlog

Referencia de alcance: CONTEXTO_Y_ALCANCE_PROYECTO.md, versión 2.0. El informe APF2 se conserva como diseño de la visión completa. No se presupone que los porcentajes declarados en ese documento constituyan una rúbrica oficial verificada.

## Incremento APF3 implementado

- Estructura modular con controladores, servicios, repositorios y DTO.
- Repositorio Git local; publicación en GitHub pendiente de destino.
- Conexión y migraciones PostgreSQL; demo local H2.
- Login JWT con dos perfiles y contraseñas BCrypt.
- Creación/listado/detalle, filtros y paginación de tickets.
- Control de acceso entre empresas.
- Indicadores reales y evento de creación.
- Logback y Commons Lang integrados.
- Pruebas API y frontend, documentación y scripts de arranque.

## Siguiente incremento: entrega final

1. Contrato mensual: inicio, fin y horas contratadas; selección del período vigente.
2. Imputaciones con fecha, consultor, horas y descripción; BigDecimal; consumo consistente en una transacción.
3. Barra de consumo y avisos al 75%, 90% y al superar el 100%; sobregiro permitido sin aprobación.
4. Cambio de estado para el personal interno y eventos de auditoría.
5. SLA por horas naturales; objetivos sencillos por severidad a acordar. En espera muestra una condición de espera, sin afirmar que compensa el vencimiento.
6. Liquidación por empresa y período; Excel con Apache POI; vista de impresión PDF.
7. Pruebas de los flujos finales y despliegue del backend y frontend.

## Fuera del MVP académico

Calendario hábil/feriados, tareas programadas de SLA, pausas con compensación, aprobación formal de sobregiros, multiplicadores de horas, correos transaccionales, cinco roles granulares, plataforma comercial de autoservicio y facturación electrónica.

Las tablas de bolsas e imputaciones se añadirán por migraciones posteriores. Los prototipos de horas/reportes no se presentan como funciones disponibles en este incremento.
