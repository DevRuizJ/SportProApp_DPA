# Arquitectura y estado de SportPro

## Decisión vigente del usuario

Usar Firebase Authentication + Cloud Firestore + FCM y SQLite local mediante Room con migraciones que conserven datos. No añadir Realtime Database ni SQL Connect.

Se descarta Cloud Storage para Firebase por preocupación de cobros. Se retira su configuración de firebase.json para evitar despliegues accidentales. storage.rules permanece como archivo inactivo de bloqueo total. No activar Blaze ni aprovisionar nuevos recursos de pago bajo esta decisión.

Retirar la configuración local NO desactiva recursos remotos ni cancela cargos. El usuario reporta un cobro, pero el agente no ha visto la factura ni verificado su concepto o el estado actual de Cloud Billing. Revisar ese estado por separado antes de asumir que cesaron los cargos. No borrar recursos remotos o desactivar facturación de otros proyectos.

## Firebase configurado

Proyecto: sportpro-dpa-2026-rh
Consola: https://console.firebase.google.com/project/sportpro-dpa-2026-rh/overview
Paquete: com.dpa.sportpro
App ID: 1:86674345315:android:61dc8fbbd16029deacd756

Authentication:
- Proveedor email/password habilitado.
- Importadas cuentas de prueba admin@sportpro.com, dt@sportpro.com, jugador@sportpro.com y padre@sportpro.com.
- UIDs: admin_1, dt_1, jug_1, pad_1.
- Registro conectado a createUserWithEmailAndPassword y perfil privado en users/{UID}. Login recupera el rol seleccionado desde Firestore; no hay fallback mock ni inferencia por correo.
- Sesión restaurada y cierre real. Falta prueba visual completa en dispositivo de cada rol; la validación contra el servicio se registra debajo.
- Registro parcial recuperable al repetir las mismas credenciales. Las contraseñas no se guardan en la base.

Firestore, verificado el 01/11/2026:
- Base sportpro-live; región southamerica-west1 (Santiago).
- Edición STANDARD, tipo FIRESTORE_NATIVE, tiempo real habilitado y freeTier=true.
- Reglas de users/{UID} desplegadas: creación validada y lectura privada por UID. Listado, cambios de rol/estado y demás colecciones permanecen denegados. Índices sin cambios.
- La base sportpro anterior fue eliminada con autorización.
- SportProFirestore elige explícitamente sportpro-live desde Android.
- Los perfiles de cuenta ya usan esta base. Los repositorios deportivos basados en SharedPreferences aún no sincronizan; están separados por UID para no heredar datos al cambiar de cuenta. Ver AUDITORIA_HISTORIAS.md.
- selectedRole es preferencia de onboarding/navegación, no permiso global. Antes de abrir datos deportivos implementar ACL por academia y vínculos familiares.
- BoM 34.4.0 conserva compatibilidad con Kotlin 2.2 del proyecto; BoM 35.1.0 falló por metadatos Kotlin 2.4.

FCM sigue pendiente de integrar; no se ha desplegado un emisor seguro de notificaciones.

## Validación ejecutada el 01/11/2026

- 35 pruebas unitarias y 7 instrumentadas en emulator-5554 aprobaron; compilación Debug correcta.
- 26 pruebas de reglas Firestore aprobaron en Emulator Suite: validación de perfiles, privacidad entre cuentas, bloqueo de escalamiento de rol y colecciones no habilitadas.
- Despliegue de reglas: dry-run y deploy completados correctamente en sportpro-dpa-2026-rh.
- tools/firebase-tests/live-auth-smoke.mjs --run-live comprobó contra Firebase real registro → perfil en sportpro-live → cierre → login para ADM, DT, JUG y PAD, con correos nuevos independientes de los cuatro usuarios iniciales. También verificó rechazo de lectura de otro perfil.
- Las cuatro cuentas sintéticas y sus documentos fueron eliminados al terminar. Las cuentas iniciales de prueba permanecen intactas.
- La prueba remota utiliza el SDK JavaScript y verifica servicios/reglas; no sustituye la prueba visual del flujo Android por cada rol.

Las reglas actuales son una base de prototipo: protegen el perfil privado por UID, validan campos y niegan el resto de colecciones. Se deben revisar y ampliar con pruebas de membresías antes de compartir ampliamente la aplicación.

## Recuperación y cambios de credenciales — 02/11/2026

Implementados recuperación por correo, cambio de contraseña con reautenticación y cambio de email con verificación previa. Ver ACCOUNT_SECURITY.md para instrucciones y cuotas gratuitas.

Las reglas actualizadas se probaron y desplegaron en sportpro-live: solo permiten sincronizar email y updatedAt del titular activo si el correo coincide con el token verificado. El resto de restricciones permanece vigente.

Validación: 45 pruebas unitarias, 30 pruebas de reglas y 3 flujos de integración en Authentication Emulator aprobados. Compilación e instalación Android correctas; enlace de recuperación y validación de correo comprobados en el dispositivo. La recepción en un buzón real requiere prueba manual del titular; no se enviaron correos a cuentas de ejemplo.

## Persistencia offline

Room administra SQLite dentro del teléfono. Caso elegido: cola durable de eventos de partido pendientes, separada por UID y partido. El estado remoto confirmado se conserva en Firestore cuando se implemente el envío.

Reglas:
- Guardar localmente antes de intentar enviar.
- Reutilizar operationId en los reintentos remotos.
- Confirmar envío únicamente después de aceptación del servidor.
- No transmitir eventos de un UID distinto al usuario autenticado.
- No eliminar pendientes al cerrar sesión.
- Versionar el esquema y registrar cada migración.
- No usar fallbackToDestructiveMigration ni borrar/recrear la base.
- Probar conservación de datos además de validez del esquema.
- Las migraciones no protegen ante desinstalar, borrar datos o pérdida física del dispositivo.
- No se está migrando todavía el JSON de SharedPreferences; conservarlo hasta diseñar y probar su importación.

## Proveedor elegido para imágenes: Supabase Storage Free

El usuario eligió Supabase y compartió un ejemplo de su profesor. Se preparó la integración de escudos públicos en Registrar academia mediante SDK Storage, Firebase Auth, ViewModel y StateFlow. Las fotos privadas de jugadores no se suben a ese bucket.

La configuración y el SQL están en SUPABASE_SETUP.md y supabase/migrations/202611010001_public_crests.sql. Faltan Project URL, clave publicable, integración Third-Party Auth, claims role=authenticated y app_role con el rol de SportPro, y ejecución/verificación de las políticas remotas.

No se creó cuenta, no se subieron imágenes ni se modificaron claims en esta intervención. La integración cliente no implica que el servicio remoto esté listo. Supabase se usa para archivos; los datos de negocio permanecen destinados a Firestore. Las academias siguen guardadas localmente hasta integrar su repositorio remoto.

El enunciado original menciona Cloud Storage de Firebase; documentar la sustitución acordada sin presentarla como uso literal de ese servicio.

Fuentes:
- https://supabase.com/docs/guides/platform/billing-on-supabase
- https://supabase.com/docs/guides/auth/third-party/overview
- https://supabase.com/docs/guides/platform/free-project-pausing
- https://cloudinary.com/pricing
- https://firebase.google.com/docs/firestore/pricing
