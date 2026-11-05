# Pruebas funcionales y técnicas
Ejecuciones 03–04/11/2026. Los fixtures de pruebas no son datos de un partido real.

## Resultados ejecutados
| Comprobación | Resultado | Evidencia |
|---|---|---|
|Línea base assembleDebug + testDebugUnitTest|45 aprobadas|Salida Gradle inicial de la auditoría|
|assembleDebug + testDebugUnitTest tras implementación|51 aprobadas|app/build/reports/tests/testDebugUnitTest/index.html|
|connectedDebugAndroidTest|8 aprobadas en emulator-5554|app/build/reports/androidTests/connected/debug/index.html|
|Reglas users + live-match en Firebase Emulator|40 aprobadas, 0 fallidas|tools/firebase-tests/rules.test.mjs y live-match.test.mjs|
|Compilación/despliegue de reglas reales|Correcto|Firebase CLI deploy --only firestore:rules, sportpro-dpa-2026-rh|
|Ingreso UI DT existente|Correcto|Home mostró Entrenador y acceso a Partidos en vivo|
|Crear partido UI con espectadores asignados|Correcto|Confirmación explícita servidor, marcador inicial 0–0|
|Iniciar primer tiempo UI|Correcto|Reloj avanzó y apareció evento Inicio de tiempo|
|Registrar gol sin jugador identificado UI|Correcto|Marcador 1–0 y cronología Gol, Jugador no identificado|
|Reapertura y modo avión E2E|Pendiente|No declarar ejecutado|
|Dos dispositivos Android simultáneos|Pendiente|No sustituir con prueba SDK|

## Cobertura de reglas de partido
Creación por claim DT; selección ADM no autoriza. Lectura exclusiva de participantes; espectador sin escritura; otro DT sin acceso implícito. Autor/fecha obligatorios; historial atómico e inmutable. Gol inválido rechazado, sustitución conocida idéntica rechazada, jugador desconocido permitido. Reenvío idempotente incluso después de corrección. Dos operadores con versión antigua no sobrescriben. Reloj exige transiciones válidas e inicio/fin atómicos. Catálogo deshabilitado o versión antigua rechaza evento. Suspensión bloquea operaciones.

La primera ejecución conjunta corrió suites en paralelo y ambas limpiaban el mismo emulador: produjo interferencia de fixtures. Se corrigió ejecución secuencial con --test-concurrency=1 antes de los archivos. Ejecución final: 40/40.

## Latencia local medida
Listener de segundo cliente SDK, roles distintos, Firestore Emulator local:
| Revisión | Confirmación → listener (ms) | Solicitud → listener (ms) |
|---|---:|---:|
|Gol v1|0,2|29,9|
|Corrección v2|12,0|44,9|
|Anulación v3|0,1|32,5|

Son resultados locales; no certifican la red móvil ni dos teléfonos.

## Reproducción
Desde la raíz, JDK 21 de Android Studio disponible:
- Gradle: app:assembleDebug app:testDebugUnitTest
- Con dispositivo: app:connectedDebugAndroidTest
- Firebase: npx.cmd -y firebase-tools@latest emulators:exec --config firebase.tests.json --only firestore --project demo-sportpro "node --test --test-concurrency=1 tools/firebase-tests/rules.test.mjs tools/firebase-tests/live-match.test.mjs"

Usar únicamente proyecto demo para tests que limpian datos. No cambiarlo por el proyecto real. Los tests de migración crean y borran solo bases con nombre aleatorio de prueba.

## Matriz manual pendiente
Para cada formulario US003–009: vacío, requerido inválido, guardar una vez, pulsar dos veces, cancelar edición, salir/reabrir, editar y comprobar persistencia, cambiar de UID y comprobar aislamiento. No marcar aprobados por inspección del código.
Para partido: gol con equipo equivocado, minuto tardío, tarjeta sin autor, sustitución desconocida, corrección simultánea, anulación tras finalización, catálogo actualizado durante captura, desconexión y reenvío, cierre de proceso, reloj del teléfono alterado y reinicio del equipo.
