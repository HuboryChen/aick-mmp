# 后端单元测试覆盖率提升任务清单

## 1. P0优先级 - 核心Service测试

### 1.1 AlertRuleService 测试

- [ ] 1.1.1 补充 AlertRuleServiceImpl 构造函数测试
- [ ] 1.1.2 补充 createAlertRule 方法测试
- [ ] 1.1.3 补充 updateAlertRule 方法测试
- [ ] 1.1.4 补充 deleteAlertRule 方法测试
- [ ] 1.1.5 补充 getAlertRuleById 方法测试
- [ ] 1.1.6 补充 listAlertRules 方法测试
- [ ] 1.1.7 补充 toggleAlertRuleStatus 方法测试
- [ ] 1.1.8 补充 copyAlertRule 方法测试

### 1.2 EdgeNodeService 测试

- [ ] 1.2.1 补充 EdgeNodeServiceImpl 构造函数测试
- [ ] 1.2.2 补充 registerEdgeNode 方法测试
- [ ] 1.2.3 补充 updateEdgeNode 方法测试
- [ ] 1.2.4 补充 deleteEdgeNode 方法测试
- [ ] 1.2.5 补充 getEdgeNodeById 方法测试
- [ ] 1.2.6 补充 listEdgeNodes 方法测试
- [ ] 1.2.7 补充 processHeartbeat 方法测试
- [ ] 1.2.8 补充 checkNodeHealth 方法测试
- [ ] 1.2.9 补充 getNodeWeight 方法测试

### 1.3 StreamingService 测试

- [ ] 1.3.1 补充 StreamingServiceImpl 构造函数测试
- [ ] 1.3.2 补充 createSession 方法测试
- [ ] 1.3.3 补充 stopSession 方法测试
- [ ] 1.3.4 补充 getSession 方法测试
- [ ] 1.3.5 补充 listActiveSessions 方法测试
- [ ] 1.3.6 补充 JanusClient 集成测试

## 2. P1优先级 - 业务Service测试

### 2.1 RegionService 测试

- [ ] 2.1.1 补充 createRegion 方法测试
- [ ] 2.1.2 补充 updateRegion 方法测试
- [ ] 2.1.3 补充 deleteRegion 方法测试
- [ ] 2.1.4 补充 getRegionTree 方法测试
- [ ] 2.1.5 补充 moveRegion 方法测试

### 2.2 CdnNodeService 测试

- [ ] 2.2.1 补充 createCdnNode 方法测试
- [ ] 2.2.2 补充 updateCdnNode 方法测试
- [ ] 2.2.3 补充 deleteCdnNode 方法测试
- [ ] 2.2.4 补充 listCdnNodes 方法测试
- [ ] 2.2.5 补充 healthCheck 方法测试

### 2.3 RecordingService 测试

- [ ] 2.3.1 补充 RecordingServiceImpl 构造函数测试
- [ ] 2.3.2 补充 startRecording 方法测试
- [ ] 2.3.3 补充 stopRecording 方法测试
- [ ] 2.3.4 补充 queryRecordings 方法测试
- [ ] 2.3.5 补充 deleteRecording 方法测试
- [ ] 2.3.6 补充 getRecordingPlaybackUrl 方法测试

### 2.4 CameraService 测试

- [ ] 2.4.1 补充 createCamera 方法测试
- [ ] 2.4.2 补充 updateCamera 方法测试
- [ ] 2.4.3 补充 deleteCamera 方法测试
- [ ] 2.4.4 补充 listCameras 方法测试
- [ ] 2.4.5 补充 batchOperation 方法测试

## 3. P2优先级 - Controller测试

### 3.1 CameraController 测试

- [ ] 3.1.1 补充 getCameras 端点测试
- [ ] 3.1.2 补充 createCamera 端点测试
- [ ] 3.1.3 补充 updateCamera 端点测试
- [ ] 3.1.4 补充 deleteCamera 端点测试
- [ ] 3.1.5 补充 batchOperation 端点测试

### 3.2 EdgeNodeController 测试

- [ ] 3.2.1 补充 getEdgeNodes 端点测试
- [ ] 3.2.2 补充 createEdgeNode 端点测试
- [ ] 3.2.3 补充 updateEdgeNode 端点测试
- [ ] 3.2.4 补充 deleteEdgeNode 端点测试

### 3.3 AlertRuleController 测试

- [ ] 3.3.1 补充 getAlertRules 端点测试
- [ ] 3.3.2 补充 createAlertRule 端点测试
- [ ] 3.3.3 补充 updateAlertRule 端点测试
- [ ] 3.3.4 补充 deleteAlertRule 端点测试
- [ ] 3.3.5 补充 toggleAlertRuleStatus 端点测试

## 4. Repository层测试

### 4.1 核心Repository测试

- [ ] 4.1.1 补充 CameraRepository JPA测试
- [ ] 4.1.2 补充 EdgeNodeRepository JPA测试
- [ ] 4.1.3 补充 AlertRuleRepository JPA测试
- [ ] 4.1.4 补充 RecordingRepository JPA测试
- [ ] 4.1.5 补充 RegionRepository JPA测试
- [ ] 4.1.6 补充 CdnNodeRepository JPA测试

## 5. 工具类测试

### 5.1 加密和签名工具测试

- [ ] 5.1.1 补充 AESEncryptionUtil 测试
- [ ] 5.1.2 补充 SignatureUtil 测试
- [ ] 5.1.3 补充 JwtUtil 测试

### 5.2 业务工具测试

- [ ] 5.2.1 补充 NodeWeightCalculator 测试
- [ ] 5.2.2 补充 AlertConditionEvaluator 测试

## 6. 验证与报告

- [ ] 6.1 运行 JaCoCo 覆盖率报告
- [ ] 6.2 验证 Service 层覆盖率 >= 50%
- [ ] 6.3 验证 Repository 层覆盖率 >= 60%
- [ ] 6.4 验证 Controller 层覆盖率 >= 40%
- [ ] 6.5 更新 spec/Me2AI/任务规划.md 标记完成状态
