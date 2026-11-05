# Arquitectura de persistencia
Revisión: 03/11/2026. No se habilitó facturación ni se añadió una base remota adicional.

## Fuente de verdad
Firebase Authentication identifica al usuario. Firestore `sportpro-live` almacena su perfil privado y el estado confirmado de los partidos. Sus listeners sustituyen consultas periódicas. La caché persistente predeterminada del SDK Android permite consultar datos previamente recibidos; no garantiza disponer de un partido nunca abierto.

Room v3 almacena comandos pendientes de eventos, no otra copia autoritativa del marcador. Cada operación tiene UUID estable, propietario UID, partido, payload, intentos, error, confirmación y rechazo. Se escribe antes de intentar enviarla. La transacción remota verifica versión, crea la revisión inmutable y actualiza el evento de forma atómica. Un reintento consulta el historial por operationId y no vuelve a aplicar el evento. La confirmación local solo se marca después de respuesta del servidor.

Las colecciones son:
- users/{uid}: perfil privado; rol solicitado no concede privilegios.
- matches/{id}: equipos, categoría, duración, ACL de espectadores/operadores, estado y reloj.
- matches/{id}/events/{eventId}: versión vigente, autor, fecha servidor, instante de captura y datos deportivos.
- matches/{id}/events/{eventId}/history/{operationId}: revisiones inmutables.
- matchCatalogs/{ownerUid_hashCategoria}: tipos habilitados y versión.
- clockProbes/{uid}: muestra de hora servidor, privada al usuario.

## Reloj y marcador
El reloj usa fase, duración acumulada, inicio con timestamp servidor y versión. La UI solo representa el tiempo; no acumula ticks como fuente de verdad. Una muestra servidor se ancla a elapsedRealtime y se conserva con boot count. Tras reiniciar el teléfono se requiere recalibración; sin ella se indica tiempo aproximado. El error de estimación depende de latencia/asimetría de red; no se promete precisión absoluta.

Inicio, descanso, segundo tiempo, pausas y finalización son transacciones online. Incluyen eventos de frontera cuando corresponde. El tiempo adicional se muestra frente a duración nominal; no se termina automáticamente. No se admiten cambios contradictorios sobre una versión antigua. El rango deportivo soportado es 0–240 minutos: al llegar al máximo el contador se detiene y muestra una advertencia, permitiendo cerrar los periodos pendientes. Se preservan los timestamps originales; el tiempo de abandono fuera de ese rango no se registra como tiempo deportivo. Esto evita bloquear el cierre de un reloj olvidado sin relajar el límite del backend.

El marcador se recalcula a partir de goles vigentes, agrupados por ID y versión. Anular o cambiar equipo no duplica goles. La cronología conserva anulaciones.

## Desconexión
Con partido y sesión previamente recibidos, el operador asignado puede guardar eventos en Room sin internet. La interfaz distingue pendientes y rechazados. Los controles de reloj y catálogo requieren conexión. Un cambio de permisos, catálogo o versión puede rechazar una operación: se retiene el payload para revisión, nunca se presenta como sincronizado ni se borra silenciosamente.

SessionViewModel reintenta mientras vive el proceso/sesión, con espera progresiva hasta 60 segundos. Al reabrir se recuperan pendientes. No hay garantía de envío con proceso terminado, ni WorkManager en esta versión. Un mutex evita envíos paralelos dentro del proceso. El backend comprueba claims y ACL en cada transacción.

## Datos anteriores: limitación explícita
Academias, ficha técnica, mensualidades, entrenamientos, asistencia, convocatorias, pizarra y catálogo anterior siguen usando SharedPreferences separadas por UID. Se preservan; no se migraron automáticamente a Firestore. No se deben presentar como sincronizados entre usuarios. El catálogo del partido remoto es el utilizado por la nueva captura; el editor local anterior aún necesita unificación.

La migración de estos módulos exige relaciones por UID/categoría y reglas propias; copiar nombres como autorización sería inseguro. No existe todavía un padrón compartido autorizado de jugadores/apoderados. Se mantiene cerrado el acceso remoto a colecciones no implementadas.

## Privacidad
Solo UID incluidos en viewerUids pueden leer partidos; escribir exige además UID operador y claim firmado DT/ADM. Esto no publica contactos, datos físicos ni fichas de menores. El cliente no escribe claims. Compatibilidad temporal: se aceptan claims firmados heredados role=DT/ADM además de app_role. La selección ADM durante registro permanece como solicitud pendiente.

Supabase no forma parte de la persistencia del partido. No se presume operativo su almacenamiento.
