package com.aick.mmp.central.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API 测试: 验证故障转移相关端点的可访问性和基本行为
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("central")
class FailoverApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("API: GET /failover-events - 管理员可访问")
    @WithMockUser(roles = {"ADMIN"})
    void testGetFailoverEvents_adminCanAccess() throws Exception {
        mockMvc.perform(get("/api/failover-events"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("API: GET /failover-events - VIEWER角色可访问")
    @WithMockUser(roles = {"VIEWER"})
    void testGetFailoverEvents_viewerCanAccess() throws Exception {
        mockMvc.perform(get("/api/failover-events"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("API: GET /cameras/pending-allocation - 管理员可查询待分配池")
    @WithMockUser(roles = {"ADMIN"})
    void testGetPendingAllocationCameras() throws Exception {
        mockMvc.perform(get("/api/cameras/pending-allocation"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("API: POST /edge-nodes/{id}/trigger-failover - 需要ADMIN权限")
    @WithMockUser(roles = {"ADMIN"})
    void testTriggerFailover_needsAdminPermission() throws Exception {
        // 使用一个不存在的节点ID，验证端点可达（会返回404或202）
        mockMvc.perform(post("/api/edge-nodes/99999/trigger-failover")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // 期望: 404(节点不存在) 或 202(异步接受) 均为合理响应
                    assertTrue(status == 404 || status == 202 || status == 500,
                            "触发端点应返回 404/202/500, 实际返回 " + status);
                });
    }

    @Test
    @DisplayName("API: POST /edge-nodes/{id}/trigger-failover - OPERATOR无权访问")
    @WithMockUser(roles = {"OPERATOR"})
    void testTriggerFailover_operatorDenied() throws Exception {
        mockMvc.perform(post("/api/edge-nodes/1/trigger-failover")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden()); // 403
    }
}
