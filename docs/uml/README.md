# UML Diagrams - ReliaQuest Employee API

This directory contains PlantUML diagrams documenting the architecture and flows of the ReliaQuest Employee API system.
test
## Diagrams

### 1. Class Diagram (`class-diagram.puml`)
Shows all classes, interfaces, models, and their relationships across both the `api` and `server` modules.

**Includes:**
- `api` module: Controllers, Services, Models, Config, Exceptions, Interceptors
- `server` module: Mock Controllers, Services, Models, Config, Interceptors
- Inheritance (`--|>`), dependency (`..>`), and association (`-->`) relationships

### 2. Sequence Diagrams (`sequence-diagrams.puml`)
Multi-page file showing request/response flows for each major operation:

| Page | Flow |
|------|------|
| 1 | **Get All Employees** - Full chain including rate-limit retry |
| 2 | **Get Employee By ID** - Success and 404 paths |
| 3 | **Create Employee** - With validation and mapping |
| 4 | **Delete Employee By ID** - Two-step: resolve name then delete |
| 5 | **Rate Limit Retry Flow** - Exponential backoff detail |
| 6 | **Search Employees By Name** - Filter logic |
| 7 | **Get Highest Salary & Top 10 Earners** - Sorting flows |

### 3. Component Diagram (`component-diagram.puml`)
High-level architecture showing how the two Spring Boot services are structured and interact.

**Includes:**
- `api` module layers: Web → Controller → Service → Configuration
- `server` module layers: Web → Controller → Service → In-Memory Data
- HTTP communication between services via `RestTemplate`
- `ModelMapper` transformation pipeline
- OpenAPI/Swagger UI integration

## Rendering

To render these diagrams you can use any of the following:

1. **VS Code** - Install the [PlantUML extension](https://marketplace.visualstudio.com/items?itemName=jebbs.plantuml)
2. **IntelliJ IDEA** - Install the [PlantUML Integration plugin](https://plugins.jetbrains.com/plugin/7017-plantuml-integration)
3. **Online** - Paste contents at [plantuml.com/plantuml](https://www.plantuml.com/plantuml/uml/)
4. **CLI** - Run `plantuml *.puml` with PlantUML JAR installed

## System Overview

```
+---------------------------+         +---------------------------+
|   Employee API (:8111)    |         |  Mock Employee API (:8112)|
|---------------------------|         |---------------------------|
|  LoggingInterceptor       |         |  RandomRateLimit          |
|  EmployeeControllerImpl   | ------> |  MockEmployeeController   |
|  EmployeeService          |  HTTP   |  MockEmployeeService      |
|  ModelMapper              |         |  In-Memory Data (50 emps) |
|  RestTemplate             |         |                           |
+---------------------------+         +---------------------------+
         ^
         |
    API Clients
```
