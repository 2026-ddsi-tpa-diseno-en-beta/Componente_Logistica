# Logística: ejecución y cambios de entrega 5

Requiere Java 21 y Maven. Validación: `mvn verify`. En Logística `verify` también exige 80% de cobertura; no se cambió ese umbral.

Las asignaciones se persisten en la API. Los callbacks repetidos conservan la asignación incluso después de la entrega. POST /entregas/lote reporta una entrega física con varios paquetes de una misma necesidad. Los workers no necesitan base de datos.

Configurar integrations.donadores-url e integrations.donaciones-url; RabbitMQ para API y workers. En producción usar variables de entorno para conexión, usuario y contraseña de PostgreSQL y para las integraciones. No reemplazar application.properties de producción con las configuraciones H2 de prueba.

Swagger: `/swagger-ui/index.html`; contrato: `/v3/api-docs`; salud: `/actuator/health`; métricas: `/actuator/prometheus`. Los cuatro componentes propagan `X-Trace-Id`. Para logs centralizados configurar `BETTERSTACK_SOURCE_TOKEN` y `BETTERSTACK_INGEST_URL`; verificar la recepción en la cuenta del equipo.

La integración de los seis flujos está en el repo testing, `local/probar_integracion.py`, y requiere los cuatro repos como carpetas hermanas. La suite usa H2 aislado y simula callbacks del worker; no valida un broker externo. MCP está como módulo independiente en `mcp-server` y el bot en telegramBot.

El despliegue todavía requiere probar la base existente, las credenciales reales y las URLs públicas. Una llamada HTTP entre servicios no participa de la transacción de la base local: si falla otro componente durante una operación, revisar el resultado con el traceId antes de reintentar.

Para el worker usar `java -jar target/my-app-name-1.0-SNAPSHOT-worker.jar`. El JAR activa el perfil worker; configurar `LOGISTICA_API_URL`, la conexión RabbitMQ y un `WORKER_ID` distinto para cada instancia. Las métricas de stock se cargan sólo en la API. La suite incluye arranque del worker sin datasource y pruebas de API con persistencia JPA real en H2.

Configuración completa del ensayo externo: `testing/SETUP_PRESENTACION.md`. El exportador OTLP se habilita con `GRAFANA_OTLP_METRICS_ENABLED`; Prometheus permite validar métricas sin credenciales de un proveedor.
