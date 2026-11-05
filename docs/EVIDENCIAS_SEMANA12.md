# Evidencias semana 12
Estado al 04/11/2026. Entrega académica 07/11/2026.

## Evidencia disponible
Compilación y 51 tests unitarios; 8 instrumentados; 40 de reglas. Nuevo partido creado desde Android y confirmado en Firebase, inicio de reloj y gol sin jugador identificado con marcador actualizado. Registro de pruebas en PRUEBAS_FUNCIONALES.md.

## Guion de demostración pendiente
1. Usar dos teléfonos/emuladores con la misma versión. A: DT con claim aprobado; B: PAD/JUG activo.
2. B abre Partidos en vivo y comparte su UID. A crea partido con nombres reales de equipos, categoría, duración y UID de B. No grabar credenciales ni datos sensibles.
3. B abre el partido. Mostrar ambos dispositivos juntos y conexión disponible.
4. A inicia primer tiempo. Comprobar que reloj avanza en ambos y continúa al salir/reabrir.
5. A registra gol con minuto/segundo, equipo y jugador desconocido si corresponde; mostrar recepción automática y marcador B.
6. Registrar tarjeta y evento tardío. Corregir minuto/equipo del gol con motivo. Abrir historial.
7. Anular gol con motivo y comprobar descuento del marcador en B.
8. A pierde conexión, registra evento permitido y muestra pendiente. B no debe mostrarlo como confirmado.
9. Cerrar/reabrir A sin borrar datos; comprobar pendiente retenido. Recuperar red y comprobar recepción única en B.
10. Finalizar primer tiempo, descanso, segundo tiempo, pausa/reanudación y fin. Corregir evento finalizado conservando historia.
11. Registrar hora de confirmación y hora observable en B; calcular diferencia. Si excede 2 segundos, documentar red y resultado, no ocultarlo.

No existe todavía video continuo de un partido de práctica real. No presentar los fixtures QA como ese partido ni las capturas aisladas como video.

## Entregables
| Requisito | Estado |
|---|---|
|Integración fuente de datos|Auth/perfil y nuevo partido con Firestore; módulos anteriores mayormente locales|
|Partido operativo|Flujo mínimo implementado y prueba Android parcial; faltan aceptación offline y dos dispositivos|
|Comunidad|No operativa; pendiente US015/017|
|Video real|Pendiente|
|Datos incompletos/correcciones|Tests automatizados; gol desconocido probado en UI|
|Pérdida temporal de conexión|Cola y conservación probadas en SQLite; falta flujo E2E de red|
|Resumen IA revisable|No implementado US018/019|
|60% global|No certificado; requiere aceptación funcional por historia|

Los pendientes de comunidad/IA requieren implementación, no solo activar credenciales.
