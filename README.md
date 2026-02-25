# TraceGuard

Multi-tenant Audit & Activity Tracking Platform.

## Stack
- Java 21
- Spring Boot 3
- PostgreSQL
- API Key Authentication
- Scope-based Authorization

## Features
- Tenant isolation
- API key hashing (SHA-256)
- Constant-time comparison
- Scope-based authorization (READ/WRITE)
- Thread-safe TenantContext
- Unit tests

## Architecture
Controller → Service → Repository
Stateless API
Multi-tenant via API key

## How to run
...