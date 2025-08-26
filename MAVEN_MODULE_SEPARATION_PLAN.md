# Maven Module Separation Plan

## Current Architecture Issues
1. **Single Module Problems**:
   - Both edge and central services share the same JAR file
   - Edge nodes carry unnecessary central service dependencies
   - Difficult to optimize dependencies for specific deployment types
   - Harder to maintain separate versions and releases

## Proposed Module Structure

```
aick-mmp/
├── pom.xml                           # Parent POM
├── aick-mmp-shared/                  # Shared components module
│   ├── pom.xml
│   └── src/main/java/com/aick/mmp/shared/
│       ├── model/                    # Shared entities
│       ├── dto/                      # Shared DTOs
│       ├── adapter/protocol/         # Protocol adapters
│       ├── exception/                # Common exceptions
│       ├── util/                     # Utilities (JwtUtil, etc.)
│       └── config/                   # Common configurations
├── aick-mmp-central/                 # Central services module
│   ├── pom.xml
│   └── src/main/java/com/aick/mmp/central/
│       ├── CentralApplication.java
│       ├── controller/               # Central controllers
│       ├── service/                  # Central services
│       ├── repository/               # Central repositories
│       ├── dto/                      # Central-specific DTOs
│       └── config/                   # Central configurations
├── aick-mmp-edge/                    # Edge services module
│   ├── pom.xml
│   └── src/main/java/com/aick/mmp/edge/
│       ├── EdgeApplication.java
│       ├── controller/               # Edge controllers
│       ├── service/                  # Edge services
│       ├── dto/                      # Edge-specific DTOs
│       └── config/                   # Edge configurations
└── frontend/                         # Frontend (unchanged)
    ├── package.json
    └── src/
```

## Module Dependencies

### aick-mmp-shared (Base Module)
- **Purpose**: Common components used by both central and edge
- **Dependencies**: Spring Boot Starter, JPA, Security basics
- **Contents**: 
  - Entities (Camera, EdgeNode, StreamSession, User, CdnNode)
  - Protocol adapters
  - Common DTOs
  - Utilities and exceptions

### aick-mmp-central (Central Services)
- **Purpose**: Central server functionality
- **Dependencies**: 
  - aick-mmp-shared
  - Spring Boot Web, Security, Data JPA
  - Database drivers (MySQL, Redis)
  - Message queue (Kafka)
  - WebRTC libraries
- **Contents**:
  - Central controllers and services
  - User authentication and management
  - Global camera and edge node management
  - Streaming orchestration

### aick-mmp-edge (Edge Services)
- **Purpose**: Edge node functionality
- **Dependencies**:
  - aick-mmp-shared (minimal subset)
  - Spring Boot Web, minimal Security
  - Lightweight database drivers
  - Streaming libraries
- **Contents**:
  - Edge controllers and services
  - Local camera management
  - Stream processing
  - Heartbeat and monitoring

## Benefits of This Approach

### 1. **Optimized Deployments**
- Edge nodes: ~50MB JAR (vs current ~80MB)
- Central servers: ~70MB JAR with full features
- Reduced memory footprint on resource-constrained edge devices

### 2. **Independent Development**
- Teams can work on edge and central services independently
- Different release cycles for edge and central components
- Easier testing and debugging

### 3. **Dependency Management**
- Edge nodes don't need authentication libraries for external users
- Central servers don't need edge-specific streaming optimizations
- Better security through minimal dependencies

### 4. **CI/CD Optimization**
- Separate build pipelines for edge and central
- Independent deployment strategies
- Smaller Docker images

## Migration Steps

### Phase 1: Create Module Structure
1. Create parent POM with module definitions
2. Create aick-mmp-shared module
3. Move shared components to shared module

### Phase 2: Split Services
1. Create aick-mmp-central module
2. Move central services and controllers
3. Create aick-mmp-edge module
4. Move edge services and controllers

### Phase 3: Optimize Dependencies
1. Define minimal dependencies for each module
2. Update Docker configurations
3. Create separate build profiles

### Phase 4: Update CI/CD
1. Modify build scripts for multi-module builds
2. Update Docker Compose for new artifacts
3. Test deployment strategies

## Docker Configuration Changes

### Current Single JAR
```dockerfile
FROM openjdk:8-jre-slim
COPY target/aick-mmp-backend.jar app.jar
ENTRYPOINT ["java", "-Dspring.profiles.active=${PROFILE}", "-jar", "/app.jar"]
```

### Proposed Multiple JARs
```dockerfile
# Central Dockerfile
FROM openjdk:8-jre-slim
COPY aick-mmp-central/target/aick-mmp-central.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]

# Edge Dockerfile  
FROM openjdk:8-jre-slim
COPY aick-mmp-edge/target/aick-mmp-edge.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## Estimated Timeline
- **Phase 1-2**: 2-3 days (Structure setup and code migration)
- **Phase 3**: 1-2 days (Dependency optimization)  
- **Phase 4**: 1-2 days (CI/CD updates and testing)
- **Total**: 4-7 days

## Risks and Mitigation
1. **Build Complexity**: Mitigated by clear module structure and documentation
2. **Dependency Conflicts**: Resolved through careful shared module design
3. **Deployment Changes**: Gradual migration with backward compatibility

This modular approach aligns with microservices best practices and will significantly improve the maintainability, deployability, and scalability of the AICK-MMP system.