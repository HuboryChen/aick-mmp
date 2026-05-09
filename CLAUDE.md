# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

### Backend (Maven)
```bash
cd backend
mvn clean package -DskipTests           # Build all modules
mvn spring-boot:run -pl aick-mmp-central # Run central service
mvn spring-boot:run -pl aick-mmp-edge    # Run edge service
```

### Frontend (npm)
```bash
cd frontend
npm install --legacy-peer-deps           # Install dependencies
npm start                                # Dev server (hot reload)
npm run build                            # Production build
npm run lint                             # Run ESLint
```

### Docker
```bash
docker-compose up -d                      # Start all services
docker-compose ps                         # Check service status
docker-compose logs -f [service]          # View logs
docker-compose down                       # Stop all services
```

### Development Environment
```bash
# Start infrastructure only
docker-compose up -d mysql redis kafka zookeeper janus

# Backend with hot reload
cd backend
mvn spring-boot:run

# Frontend with hot reload
cd frontend
npm start
```

## High-Level Architecture

### Maven Multi-Module Structure
```
backend/
├── aick-mmp-parent/   # Dependency management, defines all versions
├── aick-mmp-shared/   # Shared: entities, DTOs, utilities, protocol adapters
├── aick-mmp-central/  # Central service: API gateway, auth, management
└── aick-mmp-edge/     # Edge nodes: device接入, local processing
```

**Dependencies**: central/edge depend on shared. No cross-dependencies between central and edge.

### DDD Layering (Backend)
Each service follows Domain-Driven Design:
- **Domain Layer**: Entities, value objects, domain services, repository interfaces
- **Application Layer**: Services, DTOs, command/query objects
- **Infrastructure Layer**: Repository implementations, configs, external clients

### Frontend Three-Layer Architecture
1. **Layer 1** (`theme.css`): CSS Variables (design tokens) - single source of truth
2. **Layer 2** (`index.css` @layer components): Reusable Tailwind component classes
3. **Layer 3** (JSX className): Atomic Tailwind utility classes

**Industrial Command Center Design**: Dark theme by default, neon blue accents (#00d4ff), glassmorphism cards, pulse animations.

## Critical Technical Constraints

From `spec/Me2AI/技术约束.md` - these are hard requirements:

### Backend
- Java 21 (no lower versions)
- Spring Boot 3.2.5
- Spring Security 6 for auth
- BCrypt for passwords (no MD5/SHA1)
- MySQL 8.0, Redis 6.2, Kafka 7.4.0
- WebRTC for streaming (no alternatives)
- Janus Gateway as media server

### Frontend
- React 18.x
- Ant Design 5.x (no other UI frameworks)
- TypeScript required
- WebRTC for video (no HLS.js alternatives for streaming)
- All API calls via Axios

### Architecture
- DDD required (domain/application/infrastructure layers)
- RESTful API design
- Microservices architecture
- Docker containerization
- Kubernetes for production

## Authentication System

Two authentication methods handled by `UnifiedAuthFilter`:

1. **JWT** (Priority 1): For frontend users. Bearer token in `Authorization` header.
2. **AK/SK** (Priority 2): For Edge nodes/system apps. Headers:
   - `X-Access-Key`: Access Key identifier
   - `X-Signature`: HMAC-SHA256 signature
   - `X-Timestamp`: Request timestamp (±5min tolerance)

**Strategy Pattern**: `AuthenticationStrategyFactory` manages strategies by priority.

## Spec System (Human-AI Collaboration)

The project uses a dual-spec system:

### Me2AI (Human → AI)
Location: `spec/Me2AI/`
- **需求描述.md**: User requirements
- **技术约束.md**: Technical constraints (read before planning)
- **任务规划.md**: Human task planning (AI doesn't touch)

### AI2AI (AI → AI)
Location: `spec/AI2AI/`
- **后端架构信息.md**: Backend structure and module characteristics
- **前端架构信息.md**: Frontend structure and external characteristics
- **协议和数据.md**: API contracts and database design

**Rule**: Always read relevant Me2AI specs before planning. Update AI2AI specs after task completion to reflect current state.

## Key Backend Services

### aick-mmp-central
- `AuthService`: JWT auth, login/logout
- `ApiKeyService`: AK/SK management for system apps
- `SystemAppService`: System app CRUD (for Edge node registration)
- `CameraService`: Camera management with load balancing
- `EdgeNodeService`: Edge node management
- `EdgeNodeHealthService`: Heartbeat monitoring (3min timeout)
- `EdgeNodeFailoverService`: Auto-migration on node failure
- `StreamingService`: WebRTC/Janus integration

### aick-mmp-shared
- `model/`: JPA entities (User, Camera, EdgeNode, SystemApp, ApiKey)
- `adapter/protocol/`: Protocol adapters (RTSP, ONVIF, GB28181)
- `util/`: JwtUtil, AESEncryptionUtil, SignatureUtil

## Key Frontend Concepts

### Industrial UI Components
Located in `frontend/src/components/ui/`:
- `IndustrialCard`: Glassmorphism card with optional glow border
- `StatusIndicator`: Pulse animation for online/offline/warning
- `PageHeader`: Decorated page header with icon
- `GlowButton`: Glowing button variants

### Theme System
- CSS Variables in `theme.css` - do not duplicate elsewhere
- Auto-detects system preference on first visit
- Follows system changes unless manually overridden

### Video Wall
- Supports 1/4/9/16 screen layouts
- WebRTC streaming via Janus Gateway
- Scan-line loading animation

## Database

When schema changes occur: reinitialize the database (no migration during development). Docker volumes: `mysql-data`.

## Important Notes

- **No new specs** in `spec/` folder unless explicitly requested
- **Maintain AI2AI specs** after code changes
- **Docs folder**: Use `docs/` for new markdown files
- **Remove debug code** after fixing issues
- **Use framework solutions** for rendering, don't reinvent
- **Stop and ask** if unsure about an approach

## Service Ports

| Service | Port | Notes |
|---------|------|-------|
| Frontend | 80 | Nginx + React |
| Central LB | 8090 | Load balancer |
| Edge LB | 8083 | Load balancer |
| Central Service | 8080 | Spring Boot |
| Edge Service | 8081 | Spring Boot |
| MySQL | 3306 | Database |
| Redis | 6379 | Cache |
| Kafka | 9092 | Message queue |
| Janus | 8088/8188/8089 | WebRTC gateway |
| RTSP Server | 8554 | MediaMTX |
