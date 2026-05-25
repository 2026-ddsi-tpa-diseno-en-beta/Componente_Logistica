# Arquitectura - FachadaLogística
```mermaid
flowchart LR

Cliente --> API[API Gateway]

subgraph Sistema["Sistema Solución"]

    direction TB

    Donadores[Servicio de Donadores y Entidades]
    Donaciones[Servicio de Donaciones]
    Incentivos[Servicio de Incentivos]
    Logistica[Servicio de Logística]

end

API --> Donadores
API --> Donaciones
API --> Incentivos
API --> Logistica

%% Conexiones internas (lado derecho del diagrama)
Donadores --> Logistica
Logistica --> Donadores

Donaciones --> Logistica
Logistica --> Donaciones