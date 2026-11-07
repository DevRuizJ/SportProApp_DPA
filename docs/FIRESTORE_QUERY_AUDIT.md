# Consulta y acceso revisados
Base Standard sportpro-live verificada por CLI. Authentication email/password ya habilitado.
Colección users/{uid}: get(Source.SERVER), creación transaccional si no existe. Sin list. Se admite únicamente sincronizar email y updatedAt del titular activo cuando el nuevo correo coincide con el token verificado de Authentication; uid, rol y demás campos no se pueden modificar desde cliente. El token se renueva antes de comparar el perfil con Authentication.
Campos: uid/email/names/lastNames/selectedRole/birthDateMillis/isActive/createdAt/updatedAt.
selectedRole identifica la elección de onboarding y navegación. No concede acceso a academias ajenas: futuras reglas de negocio deben usar membresías verificadas por academia y operador por partido. isActive solo puede cambiarlo un entorno administrativo.
Los perfiles con correo/fecha son privados al titular. Las cuentas importadas pueden crear perfil compatible solo con rol firmado por Firebase.
Resto de consultas del repositorio: todavía SharedPreferences, no son consultas Firestore ni prueban sincronización.
Actualización de reglas: validar creación y mutación maliciosa, autenticación, tipos, tamaño, fechas, UID, colección privada y ausencia de permisos sobre otros módulos.
