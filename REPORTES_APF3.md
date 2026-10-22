# Librerías del avance 3: reporte de tickets

## Aplicación de cada librería

| Librería | Dónde se utiliza | Evidencia |
| --- | --- | --- |
| Apache POI 5.5.1 | TicketExcelWriter | Libro `.xlsx` con dos hojas, estilos, filtros, fechas y totales |
| Guava 33.7.2-jre | TicketReportService y TicketExcelWriter | ImmutableList, Multimaps.index, ImmutableListMultimap y Stopwatch |
| Logback | TicketReportService y logback-spring.xml | Mensaje de generación con actor, cantidad y duración; rotación de archivos |

## Flujo y alcance

React solicita el archivo usando el JWT en Authorization. Spring Boot identifica al usuario y consulta TicketService.reportRows, que reutiliza el mismo filtro por empresa de la bandeja. Guava prepara los datos y POI construye el libro; Logback registra la generación. La respuesta HTTP entrega el archivo como adjunto con Cache-Control: no-store.

Un cliente obtiene únicamente registros de su empresa. El personal interno obtiene los registros de todas las empresas. Los filtros status y severity son los mismos de la bandeja. No se exporta solamente la página visible.

La primera hoja, Tickets, contiene código, asunto, empresa, solicitante, severidad, estado y fecha en America/Lima. La segunda, Resumen, cuenta por estado y severidad y conserva los filtros y la fecha de generación. Se incluyen las categorías sin registros con valor cero. Los totales son fórmulas SUM evaluadas por POI.

El límite predeterminado es 5000 filas; se puede configurar con app.reports.max-rows (entre 1 y 10000). Si el conjunto excede el límite se responde HTTP 413 con un mensaje para aplicar filtros. No se truncan resultados silenciosamente.

Los textos aportados por usuarios se escriben con setCellValue(String), nunca con setCellFormula. Solamente las fórmulas de totales, definidas por el programa, se crean como fórmulas. Los archivos y streams se cierran con try-with-resources. No hay carga de archivos Office en este incremento.

## Demostración

1. Iniciar sesión como cliente y crear o consultar tickets.
2. Seleccionar una severidad y pulsar Exportar Excel.
3. Abrir tickets.xlsx y comprobar que solo aparecen la empresa y severidad esperadas.
4. Revisar Resumen y la fórmula del total.
5. Iniciar sesión como personal interno y exportar todas las empresas.
6. Mostrar backend/logs/tickets.log: Reporte Excel generado actor_id=... cantidad=... duracion_ms=...

## Comandos de evidencia

Desde backend, con Java 21 activo:

```powershell
.\mvnw.cmd test
.\mvnw.cmd dependency:tree
```

El árbol debe mostrar org.apache.poi:poi-ooxml, com.google.guava:guava y ch.qos.logback:logback-classic. Logback ya llega con los starters de Spring Boot, por lo que no se agrega otro proveedor de logging.

Desde frontend:

```powershell
npm.cmd test
npm.cmd run build
```

## Pruebas añadidas

Ocho pruebas de API verifican autenticación, separación entre empresas, exportación de más de una página, resumen con fórmula, filtros, reporte vacío, texto que parece una fórmula, parámetros inválidos y límite de exportación. Las pruebas abren el archivo con Apache POI para comprobar la lectura real.

Dos pruebas React verifican que el botón envía el token y los filtros, descarga el archivo y presenta errores sin descargar respuestas inválidas. La suite de API existente se conserva.

La clave del perfil test es ficticia y exclusiva de pruebas, independiente del .env local. Los fixtures del reporte se preparan dentro de transacciones que se revierten después de cada prueba.

La función entrega un reporte operativo de tickets; contratos, imputaciones, liquidación monetaria y PDF de liquidación siguen en el backlog. No se añadieron tablas ni migraciones para esta exportación.

## Validación ejecutada

- Backend con PostgreSQL real: 17 pruebas aprobadas (9 existentes y 8 de reportes).
- Frontend: 5 pruebas aprobadas (3 existentes y 2 de descarga).
- Compilación TypeScript/Vite y formato Prettier: correctos.
- Descarga autenticada desde el servidor: HTTP 200, dos hojas y fórmulas SUM(B2:B5) y SUM(B9:B12).
- Evidencias locales excluidas de Git: artifacts/tickets-ejemplo.xlsx, artifacts/dependencias-reportes.txt y logs de ejecución.

La comprobación HTTP se realizó con una clave JWT temporal del proceso. El JWT_SECRET actual del .env local tiene 24 bytes; se requieren al menos 32 para el arranque normal. No se modificó ese secreto privado durante la integración de reportes.
