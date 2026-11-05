# Cambios verificables de criterios
Revisión 04/11/2026. Fuente: backlog completo entregado por el usuario. No se modificaron responsables, puntos, numeración ni objetivos. GitHub Project sin actualización comprobada; este documento está listo para trasladarse.

| US | Criterio original / ambigüedad | Criterio corregido o añadido | Implementación y verificación |
|---|---|---|---|
|001|Seleccionar ADM en registro|El rol seleccionado es una solicitud. Solo un administrador confiable asigna claims firmados; modificar selectedRole o un correo no concede autorización.|Claims separados, rutas y reglas protegidas; tests de escalamiento remoto aprobados. Panel de aprobación pendiente.|
|002|Redirigir por rol almacenado|Para operaciones privilegiadas utilizar rol firmado y relación con recurso. Restaurar sesión offline solo del mismo UID con perfil en caché; confirmar permisos al enviar.|Implementado; login DT real comprobado. Falta prueba E2E de restauración sin red.|
|003|Asociar DT registrado|Usar UID de DT aprobado y categoría/academia persistidas; no nombre fijo. Error de imagen no puede simular éxito.|Pendiente padrón remoto; Supabase no configurado.|
|004|Datos sensibles visibles a autorizados|Autorizar por UID y pertenencia de categoría, nunca por nombre. Mantener datos de contacto fuera de documentos compartidos del partido.|Partido no copia contactos; ficha compartida y vínculos pendientes.|
|005|Padre/jugador consulta información propia|Relación padre-hijo verificada por backend; clave única jugador-periodo y monto finito no negativo; cero cobros reales.|Repositorio local parcial; autorización compartida y validación integral pendientes.|
|006|Crear/reutilizar sesiones|Guardar categoría real y DT autorizado; al reabrir recuperar ejercicios y fechas; editar sin duplicar sesión.|Persistencia local existente; sincronización/edición integral pendientes.|
|007|Lista completa de jugadores activos|Sin entrenamiento real mostrar vacío, no inventar sesión. Clave única sesión-jugador y pertenencia verificada.|Fallback ficticio retirado; padrón remoto pendiente.|
|008|Confirmación en tiempo real|Respuesta por UID del jugador o apoderado vinculado; confirmación persistida y observable por DT, envío push verificable separado del guardado.|Retirada inyección automática de jugadores. Sincronización y FCM pendientes.|
|009|Once titular y suplentes|Cantidad según categoría; sin duplicación entre titulares/suplentes; capitán pertenece al plantel; publicar versión usable en partido.|Pizarra local parcial; integración y reglas por categoría pendientes.|
|010|Catálogo configurable|Versión por responsable/categoría; cambios mediante transacción, suscripciones activas, campos validados por tipo; conflicto de catálogo offline debe quedar visible.|Nueva ruta sincroniza habilitación de 12 tipos. Editor local anterior y campos configurables pendientes de unificación.|
|011|Gol exige autor / permite desconocido|Jugador puede ser no identificado explícitamente. Mantener equipo, minuto/segundo y detalle obligatorio; luego corregir con autor, motivo y fecha.|Validación dominio/reglas y gol desconocido real en Android comprobados.|
|011|Cronómetro sincronizado|Estado y timestamps servidor + duración acumulada; pausas no suman, reabrir reconstruye, aproximación visible sin calibración. Transiciones online; eventos offline en cola durable.|Implementado; tests dominio y transacciones aprobados. Validación de deriva en dispositivos pendiente.|
|011|Corregir/anular sin pérdida|UUID de operación, versión esperada, escritura atómica de evento e historial inmutable. Conflictos se retienen para revisión; nunca sobrescribir silenciosamente.|Pruebas de dos operadores, reenvío, corrección y anulación aprobadas en emulador.|
|012|Actualización menor a 2 segundos|Medir confirmación servidor a recepción en segundo dispositivo, registrar red/dispositivos, no inferir cumplimiento por compilación. Marcador derivado de goles activos por ID/versión.|Dos clientes SDK emulados: 0,1–12 ms en última ejecución. Falta dos dispositivos Android y red real.|

La creación independiente del partido es un flujo mínimo real para US011/012; no equivale a completar la integración de convocatorias/alineación. Acceso de espectadores mediante UID explícito hasta contar con padrón verificado. El control de reloj requiere red para evitar dos autoridades incompatibles durante desconexión.

US015/018/019 conservan su alcance original: comunidad segura y resumen únicamente desde eventos, pendiente de revisión del DT antes de publicación. No se implementó ni simuló IA.
