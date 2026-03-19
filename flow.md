# ReliaQuest Employee API - Flow Documentation

This document explains the end-to-end flow of the ReliaQuest Employee API using UML diagrams.

---

## Architecture Overview

The system consists of two Spring Boot applications:

- **API Module** (port `8111`) — Public-facing REST API consumed by clients
- **Server Module** (port `8112`) — Mock Employee data store with random rate limiting

---

## 1. System Component Diagram

```plantuml
@startuml
skinparam componentStyle rectangle

actor Client

package "API Module (port 8111)" {
  [EmployeeControllerImpl] as Controller
  [EmployeeService] as Service
  [LoggingInterceptor] as Logger
  [EmployeeControllerAdvice] as Advice
}

package "Server Module (port 8112)" {
  [MockEmployeeController] as MockController
  [MockEmployeeService] as MockService
  [RandomRequestLimitInterceptor] as RateLimiter
  database "In-Memory\nEmployee Store" as DB
}

Client --> Controller : HTTP REST
Controller --> Service : delegates
Service --> MockController : RestTemplate HTTP
RateLimiter --> MockController : intercepts
MockController --> MockService
MockService --> DB
Logger --> Controller : intercepts
Advice --> Controller : handles exceptions

@enduml
```

---

## 2. API Endpoints Flow

### 2a. GET All Employees

```plantuml
@startuml
title GET /api/v1/employee

actor Client
participant "EmployeeControllerImpl" as Controller
participant "EmployeeService" as Service
participant "RestTemplate" as RT
participant "MockEmployeeController\n(Server)" as Mock

Client -> Controller : GET /api/v1/employee
Controller -> Service : getAllEmployees()
Service -> RT : exchange(GET, serverUrl)
RT -> Mock : GET /api/v1/employee

alt Rate limit not exceeded
  Mock --> RT : Response<List<MockEmployee>>
  RT --> Service : ResponseEntity (200 OK)
  Service -> Service : map MockEmployee → Employee
  Service --> Controller : List<Employee>
  Controller --> Client : 200 OK, List<Employee>
else Rate limit exceeded (429)
  Mock --> RT : 429 Too Many Requests
  RT --> Service : HttpStatusCodeException(429)
  Service -> Service : executeWithRetry() - backoff & retry
  note right : Exponential backoff\ninitial=10s, max=120s
  Service --> Controller : List<Employee> (after retry)
  Controller --> Client : 200 OK, List<Employee>
end

@enduml
```

### 2b. GET Employee by ID

```plantuml
@startuml
title GET /api/v1/employee/{id}

actor Client
participant "EmployeeControllerImpl" as Controller
participant "EmployeeService" as Service
participant "RestTemplate" as RT
participant "MockEmployeeController\n(Server)" as Mock

Client -> Controller : GET /api/v1/employee/{id}
Controller -> Service : getEmployeeById(id)
Service -> RT : exchange(GET, serverUrl/{id})
RT -> Mock : GET /api/v1/employee/{id}

alt Employee found
  Mock --> RT : Response<MockEmployee> (200 OK)
  RT --> Service : ResponseEntity
  Service -> Service : map MockEmployee → Employee
  Service --> Controller : Employee
  Controller --> Client : 200 OK, Employee

else Employee not found (404)
  Mock --> RT : 404 Not Found
  RT --> Service : HttpStatusCodeException(404)
  Service -> Service : throw EmployeeNotFoundException
  Service --> Controller : EmployeeNotFoundException
  Controller --> Client : 404 Not Found

else Rate limited (429)
  Mock --> RT : 429 Too Many Requests
  RT --> Service : HttpStatusCodeException(429)
  Service -> Service : executeWithRetry() - backoff & retry
end

@enduml
```

### 2c. GET Employees by Name Search

```plantuml
@startuml
title GET /api/v1/employee/search/{searchString}

actor Client
participant "EmployeeControllerImpl" as Controller
participant "EmployeeService" as Service

Client -> Controller : GET /api/v1/employee/search/{searchString}
Controller -> Service : getEmployeesByNameSearch(searchString)
Service -> Service : getAllEmployees()
note right : Fetches ALL employees\nfrom Mock Server
Service -> Service : filter(name.contains(searchString))\n(case-insensitive)
Service --> Controller : List<Employee>
Controller --> Client : 200 OK, List<Employee>

@enduml
```

### 2d. GET Highest Salary

```plantuml
@startuml
title GET /api/v1/employee/highestSalary

actor Client
participant "EmployeeControllerImpl" as Controller
participant "EmployeeService" as Service

Client -> Controller : GET /api/v1/employee/highestSalary
Controller -> Service : getHighestSalaryOfEmployees()
Service -> Service : getAllEmployees()
Service -> Service : sort by salary descending
Service -> Service : findFirst() → Optional<Integer>
Service --> Controller : Optional<Integer>
Controller --> Client : 200 OK, Integer (salary)\nor 404 if no employees

@enduml
```

### 2e. GET Top 10 Highest Earning Employee Names

```plantuml
@startuml
title GET /api/v1/employee/topTenHighestEarningEmployeeNames

actor Client
participant "EmployeeControllerImpl" as Controller
participant "EmployeeService" as Service

Client -> Controller : GET /api/v1/employee/topTenHighestEarningEmployeeNames
Controller -> Service : getTopTenHighestEarningEmployeeNames()
Service -> Service : getAllEmployees()
Service -> Service : sort by salary descending
Service -> Service : limit(10)
Service -> Service : map to names
Service --> Controller : List<String> (names)
Controller --> Client : 200 OK, List<String>

@enduml
```

### 2f. POST Create Employee

```plantuml
@startuml
title POST /api/v1/employee

actor Client
participant "EmployeeControllerImpl" as Controller
participant "EmployeeService" as Service
participant "RestTemplate" as RT
participant "MockEmployeeController\n(Server)" as Mock

Client -> Controller : POST /api/v1/employee\n{name, salary, age, title}
Controller -> Service : createEmployee(CreateEmployee)
Service -> RT : exchange(POST, serverUrl, body)
RT -> Mock : POST /api/v1/employee

alt Creation successful
  Mock --> RT : Response<MockEmployee> (200 OK)
  RT --> Service : ResponseEntity
  Service -> Service : map MockEmployee → Employee
  Service --> Controller : Employee
  Controller --> Client : 201 Created, Employee

else Validation error or server error
  Mock --> RT : 4xx/5xx
  RT --> Service : HttpStatusCodeException
  Service -> Service : throw RuntimeException
  Service --> Controller : RuntimeException
  Controller --> Client : 500 Internal Server Error

else Rate limited (429)
  Mock --> RT : 429 Too Many Requests
  Service -> Service : executeWithRetry() - backoff & retry
end

@enduml
```

### 2g. DELETE Employee by ID

```plantuml
@startuml
title DELETE /api/v1/employee/{id}

actor Client
participant "EmployeeControllerImpl" as Controller
participant "EmployeeService" as Service
participant "RestTemplate" as RT
participant "MockEmployeeController\n(Server)" as Mock

Client -> Controller : DELETE /api/v1/employee/{id}
Controller -> Service : deleteEmployeeById(id)

Service -> Service : getEmployeeById(id)
note right : First fetches employee\nto get their name

alt Employee not found
  Service --> Controller : EmployeeNotFoundException
  Controller --> Client : 404 Not Found
end

Service -> Service : map Employee → DeleteEmployee (name)
Service -> RT : exchange(DELETE, serverUrl, body{name})
RT -> Mock : DELETE /api/v1/employee/{name}

alt Deletion successful
  Mock --> RT : Response<Boolean>(true)
  RT --> Service : ResponseEntity
  Service --> Controller : employeeName (String)
  Controller --> Client : 200 OK, employeeName

else Deletion failed
  Mock --> RT : Response<Boolean>(false) or error
  Service -> Service : throw RuntimeException
  Controller --> Client : 500 Internal Server Error

else Rate limited (429)
  Mock --> RT : 429 Too Many Requests
  Service -> Service : executeWithRetry() - backoff & retry
end

@enduml
```

---

## 3. Retry / Rate-Limit Handling (executeWithRetry)

```plantuml
@startuml
title executeWithRetry - Exponential Backoff Flow

start

:attempt = 1
backOffTime = INITIAL_BACKOFF_TIME (10s);

while (backOffTime < MAX_BACKOFF_TIME (120s)) is (yes)
  :Execute HTTP Request;

  if (Response is 2xx?) then (yes)
    :isSuccess = true
    capture responseBody;
    break
  else (no)
    if (Status == 429 Too Many Requests?) then (yes)
      :attempt++
      backOffTime *= 2
      Thread.sleep(backOffTime);
    else (no)
      :log error
      throw HttpStatusCodeException;
      stop
    endif
  endif
endwhile (no)

if (isSuccess?) then (yes)
  :return responseBody;
  stop
else (no)
  :throw RateLimitExceededException
  "N attempts exhausted";
  stop
endif

@enduml
```

---

## 4. Server-Side Rate Limiting (RandomRequestLimitInterceptor)

```plantuml
@startuml
title RandomRequestLimitInterceptor - Prehandle Flow

start

note right
  REQUEST_LIMIT = random(5..10)
  BACKOFF_DURATION = random(30..90s)
end note

:Incoming HTTP Request;

if (requestCount >= REQUEST_LIMIT?) then (yes)
  if (now < lastRequested + BACKOFF_DURATION?) then (yes - still in backoff window)
    :Return 429 Too Many Requests;
    :Block request (return false);
    stop
  else (no - backoff expired)
    :Reset counter to 0
    Allow request;
  endif
else (no)
  :Increment request counter
  Update lastRequested timestamp;
endif

:Proceed to Controller (return true);
stop

@enduml
```

---

## 5. Exception Handling Flow

```plantuml
@startuml
title Exception Handling via EmployeeControllerAdvice

actor Client
participant "EmployeeControllerImpl" as Controller
participant "EmployeeService" as Service
participant "EmployeeControllerAdvice" as Advice

Client -> Controller : HTTP Request
Controller -> Service : delegate

alt EmployeeNotFoundException thrown
  Service --> Advice : EmployeeNotFoundException
  Advice --> Client : 404 Not Found
else RateLimitExceededException thrown
  Service --> Advice : RateLimitExceededException
  Advice --> Client : 429 Too Many Requests
else RuntimeException thrown
  Service --> Advice : RuntimeException
  Advice --> Client : 500 Internal Server Error
end

@enduml
```

---

## 6. Data Model Mapping

```plantuml
@startuml
title Model Mapping Flow (ModelMapper)

class MockEmployee {
  + UUID id
  + String employee_name
  + Integer employee_salary
  + Integer employee_age
  + String employee_title
  + String employee_email
}

class Employee {
  + String id
  + String name
  + Integer salary
  + Integer age
  + String title
  + String email
}

class CreateEmployee {
  + String name
  + Integer salary
  + Integer age
  + String title
}

class DeleteEmployee {
  + String name
}

MockEmployee ..> Employee : ModelMapper maps to
Employee ..> DeleteEmployee : ModelMapper maps to
CreateEmployee --> MockEmployee : used to create

@enduml
```

---

## Summary

| Endpoint | Method | Handler | Notes |
|---|---|---|---|
| `/api/v1/employee` | GET | `getAllEmployees` | Fetches all, maps via ModelMapper |
| `/api/v1/employee/search/{name}` | GET | `getEmployeesByNameSearch` | Fetches all, filters in-memory |
| `/api/v1/employee/{id}` | GET | `getEmployeeById` | Direct server lookup by UUID |
| `/api/v1/employee/highestSalary` | GET | `getHighestSalaryOfEmployees` | Fetches all, sorts, returns top salary |
| `/api/v1/employee/topTenHighestEarningEmployeeNames` | GET | `getTopTenHighestEarningEmployeeNames` | Fetches all, sorts, returns top 10 names |
| `/api/v1/employee` | POST | `createEmployee` | Delegates POST to mock server |
| `/api/v1/employee/{id}` | DELETE | `deleteEmployeeById` | Lookup by ID then delete by name |

All server calls go through `executeWithRetry()` with exponential backoff to handle the mock server's random rate limiting (429 responses).
