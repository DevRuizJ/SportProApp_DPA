# Ejecución adicional 04/11/2026
- Gradle app:assembleDebug app:testDebugUnitTest: correcto, 55 pruebas aprobadas, 0 omitidas, 0 fallidas.
- Firebase Emulator, suites secuenciales de perfiles y partidos: 41 aprobadas, 0 fallidas.
- Regresión nueva: un reloj olvidado cinco horas rechaza un periodo de 18.000 segundos y admite su cierre al máximo soportado de 14.400 segundos.
- Roles: selección ADM sin privilegios, prioridad del claim firmado y restauración offline limitada al UID probado.
- Latencia local revisión 1: confirmación→listener 0,2 ms; solicitud→listener 33,1 ms.
- Latencia local revisión 2: confirmación→listener 11,2 ms; solicitud→listener 44,6 ms.
- Latencia local revisión 3: confirmación→listener 12,0 ms; solicitud→listener 46,3 ms.
- Última instrumentación Room: 8 aprobadas antes del ajuste visual del aviso y máximo de reloj; no se cambió el esquema posteriormente.
- Segundo AVD small_phone creado e iniciado como emulator-5556 e instalación completada. Esto no constituye todavía una prueba simultánea de aceptación.
La ejecución anterior de 51 unitarias / 40 reglas se conserva en PRUEBAS_FUNCIONALES.md como evidencia histórica; esta ejecución amplía la cobertura.
