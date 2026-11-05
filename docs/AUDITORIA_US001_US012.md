# Auditoría US-001 a US-012

Fecha de inicio: 03/11/2026. Rama: codex/us001-us012-live-match.
Base: Branch_Hancco; solo siete documentos Markdown estaban sin seguimiento. No se descartaron cambios.

## Línea base
assembleDebug y 45 pruebas unitarias: aprobadas. Primer intento instrumentado: bloqueado por ausencia de dispositivos; se prepara el AVD instalado.
GitHub y Project no devolvieron contenido mediante acceso web. Los criterios originales entregados por el usuario son la referencia disponible.

## Estado inicial
| Historia | Implementación | Navegación | Persistencia | Seguridad | Pruebas | Pendientes |
|---|---|---|---|---|---|---|
|001|Parcial|Registro operativo|Auth + users en Firestore|Rol solicitado separado de autorización; falta reforzar rutas locales|Unitarias y reglas previas|Claims verificados y aprobación|
|002|Parcial|Login/logout/restauración|Firebase real|Perfil privado; rutas basadas en rol solicitado|45 unitarias del conjunto|Permisos de operaciones|
|003|Parcial / imágenes bloqueadas|Pantalla y formulario|Preferencias por UID|Solo protección local; DT fijo|Sin aceptación integral|DT reales, remoto, edición y Supabase|
|004|Parcial|Ficha editable|Preferencias por UID|Sin acceso remoto a datos sensibles|Sin aceptación integral|Plantel, vínculos y permisos compartidos|
|005|Parcial|Tabla y edición|Preferencias, jugadores vacíos|Vinculación por nombre inadecuada|Sin aceptación integral|Jugadores y vínculos UID|
|006|Parcial|Formulario/biblioteca|Preferencias por UID|Sin categorías remotas|Sin aceptación integral|Edición y asignación real|
|007|Parcial|Formulario con sesión demo fallback|Preferencias; plantel vacío|Sin vínculos remotos|Sin aceptación integral|Eliminar fallback y conectar plantel|
|008|Parcial|Formularios/respuestas|Preferencias por UID|Vinculación por nombre|Sin aceptación integral|Sincronización, permisos y push|
|009|Parcial|Pizarra y publicación local|Preferencias por UID|Validaciones locales; 11 fijo|Sin aceptación integral|Reglas por categoría, integración partido|
|010|Parcial|Catálogo editable|Preferencias por UID|Sin backend|Sin aceptación integral|Catálogo compartido versionado|
|011|Parcial|Sin pantalla|Ledger puro + Room v2|Sin backend|16 tests dominio + Room previos|Operador, reloj, cola y trazabilidad remota|
|012|No implementada|Sin pantalla|Sin listeners|Sin ACL partido|No ejecutadas|Seguimiento real, correcciones y latencia|

## Decisión
Priorizar una ruta completa de partido y seguimiento con ACL verificadas y datos ingresados por el usuario. Firestore será la fuente compartida; Room preservará comandos antes del envío. Las limitaciones de módulos anteriores se registrarán por criterio y no se presentarán como completas.

## Estado de la revisión al 04/11/2026
Ninguna historia se declara completa por tener pantalla. El alcance corregido prioriza US011/012; no termina todos los criterios de US003–009.

|Historia|Implementación final revisada|Navegación|Persistencia|Seguridad|Pruebas|Pendientes|
|---|---|---|---|---|---|---|
|001|Parcial, registro real existente|Conservada|Auth + perfil privado|Solicitud ADM no autoriza; claim separado|Tests auth y reglas|Panel de aprobación y aceptación UI de todos los roles|
|002|Parcial con mejoras|Rutas privilegiadas protegidas por claim|Sesión/cache UID, recuperación de contraseña existente|No autorización por correo; caché offline no concede rol remoto|Login DT real y tests|Restauración offline E2E, navegación de cada rol|
|003|Parcial / imagen bloqueada externamente|Conservada, ADM verificado|Preferencias UID|Backend no expone academias sin reglas|Inspección|DT fijo, remoto, edición, Supabase|
|004|Parcial|Conservada|Ficha local UID|Contactos no copiados al partido|Inspección|Padrón/vínculos UID, autorización compartida, historial completo|
|005|Parcial|Conservada|Preferencias UID|Vínculos por nombre aún insuficientes|Inspección|Remoto, vínculos verificados, aceptación de periodos/montos|
|006|Parcial|Ruta DT/ADM verificado|Preferencias UID|Sin academia/categoría remota|Inspección|Edición y sincronización|
|007|Parcial mejorada|Vacío real sin entrenamiento ficticio|Preferencias UID|Ruta protegida; plantel vacío|Compilación|Jugadores reales y vínculos, aceptación UI|
|008|Parcial mejorada|Conservada|Se preservan convocatorias; no se agregan jugadores ficticios|Vínculos por nombre pendientes|Compilación|Sincronización y FCM|
|009|Parcial|Ruta protegida|Pizarra local|No publica permisos remotos|Inspección|Cantidad por categoría e integración alineación-partido|
|010|Parcial con ruta remota nueva|Catálogo dentro del partido|Firestore versionado por propietario/categoría|Solo propietario aprobado modifica|Reglas, conflictos de versión|Unificar editor anterior y campos configurables|
|011|Parcial funcional comprobada|Nueva captura, reloj, historial y acciones|Room v3 + transacciones Firestore|Claim + operador ACL + historial inmutable|Dominio, reglas, migración; crear/iniciar/gol Android real|Roster/alineación, conflictos asistidos, offline E2E completo|
|012|Parcial funcional comprobada|Seguimiento reactivo|Listeners con cierre, marcador derivado|ACL de espectadores|Listener SDK gol/corrección/anulación y marcador Android|Dos dispositivos Android, medición real, reconexión E2E|

## Correcciones concretas
- Ruta Partidos en vivo para todos los usuarios autenticados; creación/operación por rol aprobado y asignación explícita.
- Cronómetro reconstruible, descansos/pausas y duración configurable; límite soportado de 240 minutos con cierre posible de un reloj olvidado, sin ampliar el rango de validación.
- Eventos desconocidos/tardíos, corrección con motivo, anulación e historial inmutable; marcador recalculado.
- UUID persistido antes de envío, revisión optimista y reintentos idempotentes. Rechazos conservados localmente.
- Migración Room 2→3 sin pérdida ni limpieza y cobertura histórica 1→3.
- Rutas locales privilegiadas ya no aceptan solo rol solicitado; se retiran dos fuentes de datos de demostración.

## Límites de la auditoría
Se inspeccionaron repositorios, pantallas, rutas y servicios del inventario. No se ejecutó cada botón de US003–009: faltan datos compartidos reales y relaciones autorizadas. No se declara auditoría funcional completa de todas las interacciones. No se realizó refactorización masiva ni migración insegura de preferencias locales.

US015, US018 y US019 no están operativas; no se certifica el entregable completo de semana 12. Consultar CONFIGURACION_PENDIENTE.md y EVIDENCIAS_SEMANA12.md.
