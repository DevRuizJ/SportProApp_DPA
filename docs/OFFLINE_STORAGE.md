# SQLite local con Room

## Alcance implementado

SportProDatabase conserva una cola local de nuevos eventos de partido en sportpro_offline.db. MatchEventOutbox serializa EventPayload con payloadVersion=1, admite jugador desconocido y devuelve un operationId estable. PendingMatchEventDao ofrece observación con Flow, registro de errores y confirmación sin eliminar la fila.

Cada consulta y actualización incluye ownerUid. Este filtro no sustituye la autorización de Firebase: el futuro emisor debe obtener el UID de la sesión autenticada. No enviar operaciones de otra cuenta y no borrar pendientes al cerrar sesión.

Esta es infraestructura local: todavía no está conectada a una pantalla ni a un emisor Firestore/WorkManager. No demuestra sincronización offline de extremo a extremo. Correcciones y anulaciones requieren comandos versionados propios y resolución explícita de conflictos antes de integrar el envío.

## Migraciones sin pérdida de datos

- Esquema 1: ID de operación, UID, partido, JSON y fecha de creación.
- Esquema 2: añade attemptCount, lastError y confirmedAtMillis mediante ALTER TABLE.
- MIGRATION_1_2 está registrada en el constructor; no se usa fallbackToDestructiveMigration.
- Ambos JSON de esquema se conservan en app/schemas y se incluyen en los assets de las pruebas.
- La versión 1 es una base de prueba creada durante esta implementación; no implica que haya usuarios con una versión anterior de Room instalada.

Al cambiar tablas: incrementar version, crear y registrar la migración, exportar el nuevo esquema y probar desde todas las versiones publicadas. No modificar los esquemas históricos. Si falta una migración, se produce un error en lugar de borrar los datos.

Las migraciones conservan datos durante actualizaciones de esquema. No recuperan datos después de desinstalar, borrar almacenamiento o perder el teléfono. Los repositorios previos de SharedPreferences siguen intactos; su importación a Room requiere otro proceso idempotente y probado.

## Integración pendiente

1. Guardar el evento local antes de indicar captura completada en la UI.
2. Mostrar pendientes y errores; observar los cambios mediante StateFlow en ViewModel.
3. Enviar usando operationId como identificador remoto, con autorización por partido.
4. Confirmar únicamente al recibir aceptación del servidor. Un error conserva el pendiente.
5. Hacer que los reintentos sean idempotentes también en el backend.
6. Definir un mecanismo de envío único para no duplicar colas entre Room y la persistencia de Firestore.
7. Probar dos dispositivos, modo avión, cierre/reapertura, cambio de cuenta y reconexión.

## Pruebas

OfflineDatabaseTest crea SQLite con el JSON histórico de esquema 1 y lo abre con el constructor de producción: Room ejecuta y valida la migración 1→2. Comprueba contenido preservado, reapertura del archivo, separación por UID, rechazo de duplicados y confirmación sin borrado. Se evita MigrationTestHelper por una incompatibilidad binaria observada entre room-testing y kotlinx.serialization del proyecto; no se simula SQLite ni se omite la validación real de Room. Ejecutar con un dispositivo/emulador:

./gradlew :app:connectedDebugAndroidTest

Validación realizada el 01/11/2026: assembleDebug correcto; 27 pruebas unitarias pasaron y connectedDebugAndroidTest ejecutó 4 pruebas correctas en emulator-5554 (3 de persistencia/migración y la prueba de contexto existente). No demuestra todavía el envío offline a Firestore ni sincronización entre dispositivos.
