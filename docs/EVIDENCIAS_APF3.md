# Evidencias y seguridad

## Recorrido de demostración

1. Iniciar sesión como cliente@demo.local. Ver únicamente los tickets de Andina.
2. Crear un ticket con asunto, descripción y severidad.
3. Consultar el correlativo y abrir el detalle.
4. Filtrar por severidad y actualizar la bandeja.
5. Salir e iniciar sesión como otrocliente@demo.local: el ticket anterior no aparece.
6. Iniciar sesión como equipo@demo.local: ver todas las empresas y seleccionar una al crear.

## Evidencias del laboratorio aplicadas

- Árbol Maven: `mvnw.cmd dependency:tree` muestra Logback (starter logging) y Commons Lang.
- Archivo backend/logs/tickets.log: creación con id, empresa_id y actor_id.
- Pruebas de seguridad: acceso anónimo, intento de elegir otra empresa y token alterado.
- Capturas de login, bandeja, creación y detalle, incluyendo vista cliente e interna.

La lectura/exportación Excel del laboratorio se integra con Apache POI en la siguiente etapa de liquidación. Este incremento no afirma completar todas las actividades E1-E6 del ejercicio de pedidos.

## Checklist

- [x] Dependencias declaradas y versiones reproducibles; Maven BOM y package-lock.
- [x] Sin contraseñas reales ni secretos JWT en código versionado; .env ignorado.
- [x] Credenciales ficticias solo en demo/seed explícito.
- [x] Sin tokens, contraseñas o descripción del incidente en logs.
- [x] Validación de entrada y consultas JPA parametrizadas.
- [x] Errores genéricos sin detalles SQL ni stack trace al usuario.
- [x] Restricción de acceso aplicada en el servidor.
- [x] Logs con rotación y límite de retención.
- [ ] Subida de archivos: fuera del alcance actual; no hay endpoint de carga.
- [ ] Revisión de vulnerabilidades: ejecutar análisis periódico antes de publicar.

## Validación ejecutada

Los resultados finales de las pruebas y la comprobación con PostgreSQL se registran en VERIFICACION.md al cerrar el incremento.
