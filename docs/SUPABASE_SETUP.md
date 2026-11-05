# Supabase Storage para SportPro

## Alcance

Se conserva Firebase Authentication para las cuentas, Firestore para datos compartidos, FCM para avisos y Room para la cola local. Supabase se usa para archivos; no se traslada el modelo de negocio a otra base.

El ejemplo del profesor se integra primero en Registrar academia: Photo Picker, vista previa, ViewModel + StateFlow, preparación fuera del hilo principal, subida y representación del escudo desde su URL. La academia se guarda solo cuando la subida devuelve éxito. Su ficha todavía se persiste en SharedPreferences: no se comparte automáticamente entre dispositivos.

El bucket publico imagenes se reserva a escudos. Las fotos de jugadores siguen locales: su subida privada necesita un bucket separado y permisos de academia/categoría/vínculo familiar. No basta con usar una URL difícil de adivinar.

## Configuración externa pendiente

1. Crear un proyecto de Supabase en una organización Free. No activar Pro ni complementos de pago.
2. En Authentication > Third-Party Auth, añadir Firebase con Project ID sportpro-dpa-2026-rh.
3. Asignar desde un entorno administrativo confiable los claims de Firebase:
   - role = authenticated (rol técnico de Supabase).
   - app_role = ADM, DT, JUG o PAD (rol de SportPro verificado).
   Preservar los demás claims. Los usuarios actuales pueden tener role=ADM/DT/JUG/PAD: mover ese valor a app_role antes de reemplazarlo. Nunca permitir que el cliente se asigne ADM. No hace falta desplegar Cloud Functions de pago para configurar manualmente las cuentas de demostración con Admin SDK.
4. Renovar el token mediante nuevo inicio de sesión tras el cambio de claims. AuthRepository obtiene el rol de navegación del perfil privado en Firestore, sin inferirlo del correo. Elegir ADM en el registro NO otorga permiso de carga: Storage verifica app_role en el token firmado, asignado por un administrador confiable.
5. Ejecutar supabase/migrations/202611010001_public_crests.sql en el SQL Editor del proyecto nuevo. Falla si imagenes ya existe, evitando convertir accidentalmente un bucket privado en público. No eliminar esa protección sin revisar su contenido.
6. Copiar Project URL y Publishable key (sb_publishable_...). El cliente admite ese formato y rechaza claves secretas; no usar service_role. Configurar las siguientes propiedades de Gradle en el archivo del usuario ~/.gradle/gradle.properties o mediante variables de entorno del mismo nombre:

SPORTPRO_SUPABASE_URL=https://TU_PROYECTO.supabase.co
SPORTPRO_SUPABASE_PUBLISHABLE_KEY=sb_publishable_TU_CLAVE

7. Sincronizar Gradle y recompilar. Estas constantes están en BuildConfig y son recuperables del APK; la autorización depende de RLS, no de ocultarlas.

No se han creado recursos remotos, modificado claims ni ejecutado el SQL. Sin URL/clave y configuración externa, el formulario muestra que la subida todavía no está configurada.

## Límites y comportamiento

- Archivo seleccionado: máximo 10 MiB antes de decodificar.
- Imagen de salida: PNG, lado mayor de hasta 1024 px, máximo 2 MiB, sin metadatos EXIF del archivo original. Conserva transparencia.
- Bucket: límite 2 MiB y MIME image/png.
- Ruta: UID de Firebase / UUID.png. Solo ADM puede insertar en su carpeta.
- No se habilitan sobrescrituras, listado general, borrados ni edición de objetos desde la app.
- Descargar un escudo mediante su URL pública no exige sesión, deliberadamente.
- El token se obtiene desde Firebase; la subida falla si cambia la cuenta.
- Esta carga requiere conexión. Room aún no encola imágenes.
- Si el servidor guarda la imagen pero se pierde su respuesta, puede quedar un archivo huérfano; un reintento utiliza otra ruta. No hay limpieza automática todavía. Revisar esos archivos en la consola durante las pruebas.
- Las cuotas Free incluyen 1 GB de archivos; monitorear también transferencia. Los proyectos pueden pausarse tras una semana de inactividad. No hay garantía de disponibilidad continua del plan gratuito.
- Cambiar de proveedor no cancela cargos o recursos ya existentes de Firebase/Google Cloud.

## Prueba de aceptación remota pendiente

Con datos ficticios y un escudo sin datos personales:
- ADM sube desde Registrar academia; cerrar/reabrir conserva URL y muestra la imagen.
- Inspeccionar en Storage formato, tamaño y ruta del objeto.
- JUG/PAD/DT no pueden subir ni sobrescribir objetos, tampoco llamando directamente al SDK.
- Usuario de otro proyecto Firebase y sesión anónima: inserción rechazada.
- ADM no puede escribir en la carpeta de otro UID.
- Una imagen mayor al límite se rechaza; un error de red conserva el formulario.
- La URL de un escudo público abre sin iniciar sesión; ninguna foto privada debe estar en este bucket.
- Cambiar de cuenta durante la carga impide guardar la academia como si perteneciera a la nueva sesión.

El SQL preparado debe verificarse contra el servicio antes de afirmar que estos permisos están desplegados o probados.

## Fuentes

- https://supabase.com/docs/guides/auth/third-party/firebase-auth
- https://supabase.com/docs/guides/storage/security/access-control
- https://supabase.com/docs/guides/storage/buckets/creating-buckets
- https://supabase.com/pricing
