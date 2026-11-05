# Migraciones y conservación
03/11/2026.

## Room
Se conserva MIGRATION_1_2 y se agrega MIGRATION_2_3:
`ALTER TABLE pending_match_events ADD COLUMN rejectedAtMillis INTEGER`.

La columna nullable diferencia un rechazo permanente de una espera de conectividad. No hay eliminación de tablas, limpieza de registros ni fallbackToDestructiveMigration. Los esquemas exportados se conservan en app/schemas.

Pruebas instrumentadas ejecutadas:
- Abrir esquema histórico v1 con builder de producción v3: conserva payload, IDs y fecha.
- Abrir v2 con pendientes y confirmados: conserva intentos, error, confirmación y payload.
- Rechazar y reabrir: conserva el registro para revisión y lo excluye del envío automático.
- Operaciones repetidas no sobrescriben una fila; aislamiento por UID y confirmación comprobados.
8 pruebas instrumentadas totales aprobadas en emulator-5554. El primer intento del nuevo fixture v2 falló porque asumía un array indices presente; se corrigió el fixture para esquemas sin índices. No fue un fallo de migración.

## Compatibilidad de payload
El nuevo emisor utiliza payloadVersion=2. Filas antiguas v1 se conservan y quedan para revisión; no se transforman adivinando partido, catálogo ni identidad. No es una migración destructiva ni una sincronización automática de datos anteriores.

## Firestore
Se añadieron colecciones de partidos independientes sin borrar ni reescribir perfiles existentes. Las reglas de users permanecen compatibles. No se ejecutaron borrados remotos. Las nuevas colecciones requieren campos completos; documentos arbitrarios antiguos fuera de este contrato no se importan silenciosamente.

## Recuperación
Antes de distribuir un cambio de esquema, conservar APK anterior y exportaciones de schema y ejecutar pruebas con copia de datos, nunca contra la base única de un usuario. Para inspección de un dispositivo debug, detener escrituras y extraer conjuntamente SQLite y sus archivos WAL/SHM mediante Device Explorer; proteger esa copia por contener datos privados. No desinstalar ni borrar almacenamiento para solucionar una migración.

Si Room rechaza un esquema, preservar archivos y reproducir en una copia; corregir con migración adicional. La base confirmada en Firestore no sustituye un respaldo de comandos que aún no se enviaron. No se implementó una pantalla de exportación/recuperación automática.
