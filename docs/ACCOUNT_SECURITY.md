# Recuperación y cambios de credenciales

Implementación: 01–02/11/2026. Validación final: 02/11/2026.

## Uso

- Login → ¿Olvidaste tu contraseña? → ingresar correo → Enviar enlace.
- Firebase envía el enlace; al abrirlo, su página alojada permite elegir la contraseña. Regresar a SportPro e iniciar sesión.
- Home → Mi cuenta → Contraseña: contraseña actual, nueva contraseña y confirmación.
- Home → Mi cuenta → Correo: contraseña actual y nuevo correo. Abrir el enlace recibido en la nueva dirección; después volver a iniciar sesión.
- La solicitud de cambio de correo NO cambia inmediatamente la dirección. Firebase verifica la nueva dirección primero.
- UID y rol permanecen iguales, por lo que se conservan los datos locales separados por UID.
- Al iniciar sesión/restaurarla, se renueva el token y se sincroniza el correo verificado de Authentication en users/{UID}. El perfil no se modifica antes de verificar.
- Una contraseña actual incorrecta no cierra la sesión ni anuncia un cambio exitoso.
- Las contraseñas solo permanecen en memoria durante el formulario y se limpian al finalizar el intento; no se guardan en Firestore, Room, preferencias ni SavedStateHandle.
- El botón se bloquea durante el envío y después del éxito para evitar solicitudes duplicadas.
- Ante un timeout, la solicitud podría haber llegado al servidor: revisar el correo o intentar iniciar sesión antes de repetir el cambio.

## Envíos y costo

Se usa el envío incorporado de Firebase Authentication, sin SMTP propio, extensiones ni Cloud Functions. No se activa Blaze ni se añaden servicios de pago.

La documentación consultada indica para Spark:
- Restablecimiento de contraseña: 150 correos/día.
- Cambio de dirección: 1.000 correos/día.
- Verificación de dirección: 1.000 correos/día.

No confundir correos enviados con enlaces generados ni con inicio de sesión sin contraseña. Firebase aplica controles adicionales contra abuso y puede limitar solicitudes. No hay garantía de entrega instantánea: revisar Spam y posibles demoras.

La respuesta de recuperación no confirma si el correo existe. Esto reduce la exposición de cuentas, aunque la protección del endpoint también depende de la configuración de Firebase.

Las cuentas de ejemplo @sportpro.com no garantizan buzones reales. Para probar entrega hay que usar una dirección propia accesible. Un éxito del SDK significa que Firebase aceptó la solicitud; no demuestra que el usuario recibió el correo.

## Seguridad y pruebas

La actualización del perfil solo admite email y updatedAt del propietario activo. El email debe coincidir con el token firmado y verificado de Firebase. Cambiar UID, rol, estado o información ajena sigue rechazado.

Resultado: 45 pruebas unitarias aprobadas, 30 pruebas de reglas aprobadas y 3 flujos de integración de Authentication aprobados. Build Debug e instalación en el emulador Android correctos. Se comprobó visualmente el enlace de recuperación y su validación de correo vacío.

Los tests locales usan demo-sportpro y emuladores; no envían correos reales. Cubren recuperación, contraseña anterior incorrecta, reemplazo de contraseña, cambio de correo pendiente/verificado, conservación de UID y rechazo de reutilización del enlace.

La entrega en un buzón real y la apertura del enlace requieren una prueba manual con el titular. No se han enviado correos a los cuatro usuarios de ejemplo.

Comandos:
- Gradle: :app:assembleDebug :app:testDebugUnitTest
- Firebase Emulator Suite (Java 21+):
  npx.cmd -y firebase-tools@latest emulators:exec --config firebase.tests.json --only auth,firestore --project demo-sportpro "npm.cmd --prefix tools/firebase-tests test && npm.cmd --prefix tools/firebase-tests run test:auth"

## Fuentes

- https://firebase.google.com/docs/auth/limits
- https://firebase.google.com/docs/auth/android/manage-users
- https://firebase.google.com/docs/reference/android/com/google/firebase/auth/FirebaseUser#verifyBeforeUpdateEmail(java.lang.String)
- https://firebase.google.com/docs/emulator-suite/connect_auth
