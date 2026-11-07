# Configuración y trabajo pendiente
03/11/2026. Rama codex/us001-us012-live-match. Sin cambios publicados en main.

## Firebase
Reglas de firestore.rules compiladas y desplegadas en sportpro-dpa-2026-rh, base sportpro-live, tras 40 pruebas de reglas aprobadas. No se habilitaron servicios de pago.

Los usuarios nuevos solicitan un rol. Un administrador confiable debe asignar claims mediante Admin SDK fuera del APK: role=authenticated y app_role=ADM/DT/JUG/PAD. Mantener compatibilidad con claims heredados hasta realizar transición verificada. No asignar privilegios basándose en correo ni guardar credenciales administrativas en Android. Cerrar/abrir sesión después de asignación para actualizar token.

Para demostrar partidos: DT aprobado crea el partido e incluye UID del espectador, que aparece en Partidos en vivo. Un operador adicional necesita también claim DT/ADM. La selección por UID es provisional; falta selector de padrón/categorías con vínculos verificados.

FCM no tiene flujo de envío operativo. Falta infraestructura confiable de notificaciones y comprobación de recepción; no simular envío con Toast. No se activó Cloud Functions ni facturación para resolverlo.

## Supabase
Faltan verificar proyecto, URL/clave pública, bucket y políticas RLS. La integración de escudos está preparada pero no se declara subida exitosa. No usar service_role en APK. La política de escudos públicos no sirve para publicar fotos o datos de menores. No se migró a Cloud Storage de pago.

## Funcionalidad pendiente (no es solo configuración)
US003–009: repositorios remotos, padrón de jugadores/DT reales, categorías, relaciones padre-hijo por UID, formularios de edición y permisos por academia. Las preferencias existentes no se borraron.
US010: unificar editor de catálogo local anterior con catálogo remoto; campos configurables todavía no cubiertos por la nueva ruta.
US011: selección de participantes desde alineación/plantel; revisión asistida de conflictos rechazados; envío con proceso terminado; validar reloj ante cambios de red reales.
US015: comunidad, imágenes, comentarios, reacciones y moderación no operativos.
US018/019: proveedor de IA seguro, generación solo desde eventos, revisión, aprobación/publicación no operativos.
No se puede declarar 60% funcional solo por número de pantallas.

## Evidencia externa
Pendiente partido de práctica real, grabación continua y medición en dos teléfonos. Las pruebas con fixtures/emulador no reemplazan el video.
GitHub Project no se actualizó: acceso web no devolvió contenido verificable. CAMBIOS_HISTORIAS_USUARIO.md queda listo para trasladar criterios; no se alteraron responsables ni puntos.
