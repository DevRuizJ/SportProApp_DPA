# Entrega de semana 12 — 07/11/2026

## Diagnóstico del repositorio (01/11/2026)

No se puede acreditar todavía el 60% funcional integrado. Tener las pantallas de US-001 a US-010 no equivale a satisfacer sus criterios de aceptación.

Hallazgos comprobados:
- Corregido: RegisterViewModel crea usuarios reales en Firebase Authentication y perfiles en Firestore; el éxito espera al backend.
- Corregido: AuthRepository usa el perfil guardado por UID, sin usuarios mock ni inferencia del rol por correo. La autorización por academia todavía debe implementarse mediante membresías verificadas.
- Corregido: NavGraph usa la sesión real y UID de Firebase; el logout cierra Authentication.
- AcademyRepository y MatchEventCatalogRepository persisten JSON en SharedPreferences. No sincronizan entre dispositivos.
- Firebase está configurado en sportpro-dpa-2026-rh, con Authentication y la base Firestore Standard sportpro-live en Santiago. Reglas de perfiles privados desplegadas; otras colecciones siguen cerradas y pendientes de integración. Ver FIREBASE_SETUP.md.
- US-011 a US-019 siguen pendientes de integración funcional. Se han añadido el núcleo del partido y una cola Room independiente; esto no acredita historias completas.

Cambios de esta intervención:
- Núcleo Kotlin MatchLedger con registro, reintentos idempotentes, historial de correcciones/anulaciones, detección de versión obsoleta, marcador derivado y extracción de hechos para una futura integración IA.
- 16 pruebas unitarias del núcleo.
- Se eliminó la aceptación de correos arbitrarios con contraseñas de ocho caracteres y el fallback a usuarios simulados después de un error de Firebase.
- Las cuatro cuentas proporcionadas son cuentas de prueba reales de Authentication. Se retiró el fallback de cuentas simuladas. El sistema admite nuevas cuentas con cualquiera de los cuatro roles.
- Revisión de las 19 historias: ver AUDITORIA_HISTORIAS.md. Los repositorios locales se aislaron por UID sin borrar los archivos antiguos.
- El núcleo no está conectado a UI, almacenamiento o red. No constituye US-011, US-012 ni US-018 terminadas.

## Orden de implementación y puertas de aceptación

| Prioridad | Trabajo | Condición para darlo por terminado |
| --- | --- | --- |
| P0 | Firebase + US-001/002 | Registro real, sesión persistente, UID real, roles aprobados en servidor, cierre de sesión y reglas probadas |
| P0 | Datos compartidos de US-003/004/008/009/010 | Academia, categoría, jugadores y partido con IDs estables; catálogo sincronizado; vínculos PAD-JUG por UID |
| P0 | US-011/012 | Operador registra/corrige/anula; segundo teléfono recibe marcador y cronología sin recargar; cola offline visible |
| P0 | US-015/017 | Posts, comentarios, reacciones, reportes únicos por usuario, ocultamiento al tercer denunciante y moderación |
| P0 | US-018/019 | Proveedor IA real detrás del backend; borrador privado; edición y publicación solo por DT autorizado |
| P0 | Evidencia | Video continuo de práctica real y resultados reproducibles de pruebas |
| P1 | US-013 | Estadísticas derivadas de partidos finalizados, recalculables al corregir eventos |
| P1 | US-014 | Anuncios persistidos y FCM enviado por servidor; tratamiento de permisos y tokens |
| P1 | US-016 | Avisos de clubes verificados, filtros y reportes de convocatorias engañosas |

P1 no significa eliminar las historias del alcance. Primero cerrar las dependencias que exige la demostración de semana 12.

## Modelo lógico propuesto (independiente del motor)

La edición Standard de Firestore ya está identificada. Falta convertir este modelo lógico en colecciones, índices, reglas probadas y repositorios conectados.

| Entidad | Identidad y relaciones | Información principal |
| --- | --- | --- |
| Cuenta | UID de Authentication | nombre público, estado, rol solicitado; rol efectivo solo administrado por servidor |
| Academia / categoría | academyId / categoryId | nombre, sede, entrenadores asignados por UID |
| Membresía | academyId + UID | rol efectivo, categorías autorizadas, estado |
| Jugador | playerId; UID opcional | posición, categoría; separar ficha privada de perfil público |
| Ficha privada | playerId | nacimiento, datos físicos y revisiones, contactos y emergencias |
| Vínculo familiar | parentUid + playerId | autorización y estado verificados |
| Mensualidad | playerId + periodo | monto simulado y estado, sin pasarela |
| Entrenamiento / asistencia | trainingId / playerId | objetivo, ejercicios, duración, asistencia y observación privada |
| Partido | matchId; categoryId; convocationId | rival, fecha, estado, operadores autorizados y reloj |
| Convocatoria / alineación | matchId + playerId | disponibilidad, posición, titular/suplente, capitán |
| Catálogo | categoryId + typeId | campos requeridos, habilitado y versión |
| Evento | matchId + eventId | versión actual, tipo y versión de catálogo, segundo de juego, equipo, jugadores opcionales |
| Revisión de evento | eventId + revision | payload completo, autor UID, hora del servidor, motivo, versión esperada y estado |
| Resumen | matchId + summaryId | versión del conjunto de eventos, proveedor, texto, estado, revisor y fechas |
| Publicación / comentario | postId / commentId | autor UID, audiencia, texto, hasta 3 imágenes, estado de moderación |
| Reacción / reporte / bloqueo | contenido + UID | unicidad por usuario; motivo cerrado; destinatario bloqueado |
| Aviso de prueba | trialId + academyId | categoría, posiciones, fecha, zona y contacto institucional |
| Anuncio / dispositivo | announcementId / deviceId | categoría destino; token privado y fecha de actualización |

Los datos privados no deben coexistir en el documento público: ocultar campos en Compose no protege lecturas directas del SDK.

## Reglas de negocio para el partido

- IDs UUID creados antes de enviar. Reintentar la misma operación no crea otro gol.
- Tiempo del partido y fecha de captura son campos diferentes: registrar tarde no altera el minuto real.
- Jugador desconocido se guarda como null y se muestra como "Jugador no identificado".
- Catálogo validado por tipo; conservar su versión para interpretar eventos históricos aunque cambie.
- Gol requiere equipo. Sustitución representa salida y entrada separadas, permitiendo desconocido explícito según la política acordada.
- Corrección/anulación agrega revisión y exige motivo, UID y versión esperada.
- El backend verifica permisos y conflicto de versión de forma atómica. Las validaciones Kotlin no sustituyen seguridad del servidor.
- Una escritura rechazada debe mostrarse como rechazada; una escritura pendiente no se anuncia como sincronizada.
- El marcador se obtiene de las últimas revisiones activas. No incrementar contadores ciegamente en reintentos.
- Reloj compartido: duración acumulada y timestamp del servidor al reanudar; pausa/finalización con versión. El contador local solo representa ese estado.
- Finalizar y luego corregir invalida el borrador/aprobación anterior del resumen y obliga a recalcular estadísticas.
- No inferir minutos jugados fiables si faltan alineación o sustituciones: señalar datos incompletos.

## Persistencia local y migración

Decisión confirmada: Authentication + Firestore + FCM, con Room sobre SQLite para persistencia local. La infraestructura de cola de nuevos eventos ya tiene esquemas 1/2 y migración explícita 1→2. No está conectada a la UI ni al emisor remoto. Detalles y pruebas en OFFLINE_STORAGE.md.

Requisitos para completar el envío y las correcciones offline:
- tablas de comandos con operationId único, estado pendiente/enviado/rechazado y versión esperada;
- migraciones explícitas por versión, esquemas exportados y pruebas de migración;
- nunca fallbackToDestructiveMigration;
- transacción local para guardar comando y representación local;
- no borrar comandos hasta recibir confirmación del servidor;
- aislar datos y cola por UID; no enviar operaciones de otro usuario después de cambiar de cuenta.

Migración del prototipo: importar SharedPreferences una sola vez, conservar copia hasta verificar conteos y relaciones, registrar versión de importación y no subir datos demo como datos reales. Nombres de personas no son claves.

## Resumen IA confiable

La integración aún no existe. MatchLedger.narrativeFacts solo construye hechos; no genera narración.

Flujo propuesto:
1. Backend autentica al DT asignado y verifica partido finalizado e inicio/fin activos.
2. Construye snapshot de eventos efectivos ordenados con IDs/versiones; excluye contactos, salud y observaciones libres.
3. Llama al proveedor con secretos en servidor. Los datos del catálogo también se tratan como datos no confiables.
4. Solicita texto y referencias a eventId para cada afirmación factual; rechaza referencias desconocidas y marcador incoherente.
5. Guarda como PENDIENTE, con huella del snapshot. La validación automática reduce errores, pero no garantiza ausencia de invenciones.
6. DT revisa, edita o regenera; separar comentarios técnicos propios de hechos registrados.
7. Aprobar compara la huella con la versión actual y publica atómicamente. Un borrador nunca se entrega a JUG/PAD.
8. Una corrección posterior deja el texto obsoleto y retira su publicación hasta nueva aprobación.

Faltan selección de proveedor, configuración de credenciales y ejecución real. No incluir claves del proveedor en APK, Git ni prompts.

## Comunidad y permisos

- ADM administra su academia; DT opera categorías asignadas; JUG ve sus datos; PAD solo jugadores con vínculo verificado.
- Solicitar ADM/DT en registro no concede privilegios automáticamente.
- Comunidad expone únicamente perfil público autorizado. No publicar contactos, salud ni métricas privadas de menores.
- Publicaciones y comentarios deben admitir reporte. Tres denunciantes distintos ocultan preventivamente; contador y estado se calculan en servidor.
- Moderación conserva auditoría, permite desestimar o eliminar contenido y suspender cuentas. Un usuario suspendido no puede seguir escribiendo con un token previo.
- Bloqueo debe afectar feed e interacciones. Notificaciones respetan la audiencia y no incluyen datos sensibles.
- Verificar pertenencia al club para avisos oficiales. No confiar en un checkbox de "verificado" enviado por cliente.

## Guion de evidencia (pendiente de ejecutar)

Usar cuentas de prueba sin datos reales de menores. Grabar continuamente el partido real, al operador y al dispositivo espectador mediante un encuadre o captura sincronizada verificable.

| Caso | Acción | Resultado esperado / evidencia |
| --- | --- | --- |
| E01 | DT registra gol, PAD observa | Marcador y evento sin refrescar; medir desde envío hasta recepción, objetivo menor a 2 s |
| E02 | Gol sin jugador | Captura exitosa con jugador no identificado |
| E03 | Registrar tarde un evento al minuto anterior | Orden por tiempo del partido |
| E04 | Corregir gol de local a visitante | Marcador corregido y ambas revisiones conservadas |
| E05 | Anular gol con motivo | Se resta del marcador y se muestra anulado |
| E06 | Modo avión, registrar y cerrar app; abrir y reconectar | Pendientes sobreviven, se sincronizan una vez, sin duplicar |
| E07 | Dos operadores corrigen la misma versión | Uno confirma, otro recibe conflicto explícito |
| E08 | JUG intenta escribir por SDK sin usar UI | Backend rechaza |
| E09 | Tres cuentas reportan; una repite reporte | Se cuentan tres usuarios, no cuatro; contenido oculto |
| E10 | IA con partido finalizado | Borrador real contrastable contra eventos; PAD no puede leerlo |
| E11 | DT aprueba; luego se corrige evento | Publicación visible solo tras aprobación; después queda obsoleta |
| E12 | PAD intenta leer otro jugador | Backend rechaza datos privados |
| E13 | Catálogo cambia en otro dispositivo | Operador recibe cambio sin recargar, eventos previos siguen legibles |

Guardar fecha, commit, APK, dispositivos, red, cuentas/roles, pasos, resultado observado y enlaces a videos/logs. Las pruebas unitarias del núcleo no prueban sincronización, seguridad de Firebase ni el partido de práctica real.

## Dependencias pendientes

1. Integrar repositorios con Firestore sportpro-live, UID real y permisos por academia/categoría.
2. Completar UI y sincronización de la cola Room; probar conflictos y modo offline entre dispositivos.
3. Completar la configuración remota de Supabase Storage Free y seleccionar proveedor IA; validar con el docente la sustitución de Cloud Storage de Firebase.
4. Dos dispositivos y práctica real para el video.
5. Asignar responsables y puntos de US-019; llevar las historias al GitHub Project sin declarar terminadas las pendientes.
