# Revisión funcional de US-001 a US-019

Fecha: 01/11/2026 (Lima). Revisión del código, repositorios, navegación, dependencias y reglas. No equivale a una certificación del 60% ni a pruebas de todas las historias.

## Corrección prioritaria

El registro anterior solo validaba campos y activaba registrationSuccess. Nunca llamaba a Firebase Authentication. El inicio de sesión deducía el rol del correo, tenía cuatro usuarios simulados de respaldo y la navegación inventaba UID active_user. Se corrigieron estos tres puntos.

El formulario ahora crea la cuenta real y su perfil en users/{UID} de sportpro-live. Guarda nombres, apellidos, correo, rol seleccionado y fecha de nacimiento cuando corresponde. No guarda contraseñas en Firestore, Room ni preferencias. Solo presenta éxito tras confirmar el perfil. Si Authentication ya creó la cuenta y falla Firestore, repetir registro con las mismas credenciales completa el perfil que falta; no sobrescribe uno existente.

El login recupera el perfil por UID, comprueba isActive, y usa el rol almacenado. Las cuentas importadas sin perfil pueden inicializarlo únicamente a partir de un rol firmado en sus claims. Una cuenta sin perfil ni claims recibe instrucciones para completar registro. No hay fallback de usuarios ficticios ni selección del rol a partir del correo.

La sesión de navegación se recupera desde Firebase; cerrar sesión llama a signOut. Los argumentos de rutas ya no son fuente de autoridad. Los campos y el diseño existentes se conservan; solo se conectaron callbacks, errores existentes y estado de envío. Registro e inicio/recuperación del perfil requieren conexión.

## Rol y permisos

selectedRole expresa el rol elegido en onboarding y el Home correspondiente. No convierte a alguien en administrador global, no equivale a app_role firmado de Supabase y no permite leer otras cuentas o academias. Las reglas actuales solo abren el documento privado del titular y niegan cambios de rol/isActive desde el cliente.

El acceso a datos deportivos debe implementarse con membresías de academia/categoría y vínculos PAD-JUG por UID. No se deben abrir todas las colecciones a cualquiera que haya elegido ADM. Faltan los flujos de invitación/asignación y moderación administrativa.

## Estado por historia

| US | Comprobado / corregido | Falta para cumplir todos los criterios |
| --- | --- | --- |
| 001 | Registro real Auth + perfil Firestore, validación de contraseña/fecha, cuatro roles sin lista cerrada de correos, recuperación de registro parcial | Vinculación posterior de menores y tratamiento de cuentas importadas con información incompleta |
| 002 | Login real, perfil/rol por UID, sesión restaurada, cierre real y rechazo de cuentas suspendidas | ACL por academia en todos los módulos y prueba de navegación completa por rol |
| 003 | Formulario, categorías y subida de escudo Supabase preparada | Supabase remoto pendiente; academias locales, DT fijo Carlos Gómez, asociación por nombre en vez de UID |
| 004 | Formulario, cámara/galería, evolución física local; caché y foto de cámara aisladas por cuenta; retirada ficha ficticia por defecto | Colección de jugadores, selección de jugador real, relaciones por UID, permisos backend y fotos privadas compartidas |
| 005 | Estados simulados y validaciones locales; retirada deuda ficticia por defecto | Jugadores reales, periodos remotos, PAD-JUG por UID, autorizaciones servidor |
| 006 | Sesiones y biblioteca local | Guardado compartido, categorías reales asignadas al DT, quitar fallback de categorías del prototipo |
| 007 | Marcado de asistencia local | Plantel real activo, historial remoto, permisos por sesión; el fallback de sesión es aún un prototipo |
| 008 | Formulario y respuestas locales; retirada convocatoria ficticia inicial | Convocatorias remotas, confirmación por UID, sincronización entre cuentas/dispositivos y push |
| 009 | Formación, titulares, suplentes y capitán con validaciones locales | Guardado remoto y enlace con partido real/publicación autorizada |
| 010 | Catálogo configurable y eventos iniciales locales | Sincronización por categoría y campos/tipos versionados en servidor |
| 011 | MatchLedger conserva revisiones y anulación; Room preserva comandos nuevos | Pantalla operativa, cronómetro compartido, emisor de cola, revisiones remotas y resolución de conflictos |
| 012 | Marcador derivable en el núcleo | Listener remoto y pantalla de espectador; medir latencia real con dos dispositivos |
| 013 | Acceso visual de estadísticas no implementa cálculo real | Acumulados de partidos finalizados y recálculo al corregir |
| 014 | Sin servicio FCM/emisor seguro integrado | Anuncios persistidos, tokens, permisos, envío y lectura por audiencia |
| 015 | Sin módulo funcional de comunidad | Publicaciones, imágenes, comentarios, reacciones y permisos |
| 016 | Sin módulo funcional de avisos | Modelo, filtros, contacto institucional y verificación |
| 017 | Sin moderación real | Reportes únicos, ocultamiento al tercer usuario, bloqueo, panel y suspensión |
| 018 | narrativeFacts extrae hechos; no llama a IA | Proveedor real, backend gratuito viable, snapshot de eventos, validación y borrador privado |
| 019 | Sin revisión/publicación real | Edición DT, rechazo/regeneración, aprobación, publicación e invalidación ante cambios |

## Datos locales anteriores

Los archivos globales de SharedPreferences permanecen sin borrar. Cada cuenta usa ahora un espacio separado por hash del UID. Los singletons de convocatorias/alineaciones se renuevan al cambiar de cuenta. La foto de cámara también tiene archivo por UID.

Los archivos globales antiguos no tienen propietario verificable y NO se asignan automáticamente al primer usuario que inicia sesión. Por ello, los datos del prototipo pueden dejar de aparecer hasta una importación controlada. No se han subido a Firestore ni eliminado. La separación local es una protección temporal, no sincronización entre entrenador, padre y jugador.

Se dejaron de cargar automáticamente los jugadores de ejemplo para asistencia/mensualidades, las cuotas simuladas prepagadas y la convocatoria ficticia. Las pantallas siguen existiendo; requieren sus fuentes reales para completar esas historias.

## Evidencia y prioridad restante

Pruebas de esta intervención: 35 pruebas unitarias, 7 instrumentadas y 26 pruebas de reglas en el emulador Firestore. Resultado del despliegue y prueba contra Firebase real se registra en FIREBASE_SETUP.md.

Prioridad: usuarios y membresías → academia/categoría/jugador real → convocatorias/alineación/catálogo remoto → partido y sincronización → comunidad/moderación → IA/revisión.

Faltan pantallas para US-011 a US-019. La petición de no modificar el diseño limita esta intervención a lógica y conexiones existentes mientras se aclara la creación de esas pantallas. No se han añadido pantallas nuevas ni rediseñado las existentes.

La práctica real, el video continuo y la medición con dos dispositivos siguen pendientes; no se pueden sustituir por pruebas unitarias ni capturas simuladas.
