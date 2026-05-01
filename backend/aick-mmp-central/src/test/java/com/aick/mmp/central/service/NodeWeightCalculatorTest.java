package com.aick.mmp.central.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NodeWeightCalculator 单元测试
 * 测试节点健康判断和权重计算逻辑
 */
@DisplayName("NodeWeightCalculator Tests")
class NodeWeightCalculatorTest {

    private NodeWeightCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new NodeWeightCalculator();
    }

    @Nested
    @DisplayName("isNodeHealthy")
    class IsNodeHealthyTests {

        @Test
        @DisplayName("两者都为 NULL 时返回 true（新节点可用）")
        void bothNull_returnsTrue() {
            assertTrue(calculator.isNodeHealthy(null, null));
        }

        @Test
        @DisplayName("CPU 为 NULL，内存正常时返回 true")
        void cpuNull_memoryNormal_returnsTrue() {
            assertTrue(calculator.isNodeHealthy(null, 50.0));
        }

        @Test
        @DisplayName("内存为 NULL，CPU 正常时返回 true")
        void memoryNull_cpuNormal_returnsTrue() {
            assertTrue(calculator.isNodeHealthy(50.0, null));
        }

        @Test
        @DisplayName("两者都正常时返回 true")
        void bothNormal_returnsTrue() {
            assertTrue(calculator.isNodeHealthy(60.0, 70.0));
        }

        @Test
        @DisplayName("CPU 超过阈值时返回 false（即使内存正常）")
        void cpuOverThreshold_returnsFalse() {
            assertFalse(calculator.isNodeHealthy(85.0, 50.0));
        }

        @Test
        @DisplayName("内存超过阈值时返回 false（即使 CPU 正常）")
        void memoryOverThreshold_returnsFalse() {
            assertFalse(calculator.isNodeHealthy(50.0, 90.0));
        }

        @Test
        @DisplayName("两者都超过阈值时返回 false")
        void bothOverThreshold_returnsFalse() {
            assertFalse(calculator.isNodeHealthy(85.0, 90.0));
        }

        @Test
        @DisplayName("CPU 达到阈值时返回 false")
        void cpuAtThreshold_returnsFalse() {
            assertFalse(calculator.isNodeHealthy(80.0, 50.0));
        }

        @Test
        @DisplayName("内存达到阈值时返回 false")
        void memoryAtThreshold_returnsFalse() {
            assertFalse(calculator.isNodeHealthy(50.0, 85.0));
        }

        @Test
        @DisplayName("CPU 为 NULL，内存超过阈值时返回 false")
        void cpuNull_memoryOverThreshold_returnsFalse() {
            assertFalse(calculator.isNodeHealthy(null, 90.0));
        }

        @Test
        @DisplayName("内存为 NULL，CPU 超过阈值时返回 false")
        void memoryNull_cpuOverThreshold_returnsFalse() {
            assertFalse(calculator.isNodeHealthy(85.0, null));
        }

        @Test
        @DisplayName("边界值：CPU 79.9% 返回 true")
        void cpuJustBelowThreshold_returnsTrue() {
            assertTrue(calculator.isNodeHealthy(79.9, 50.0));
        }

        @Test
        @DisplayName("边界值：内存 84.9% 返回 true")
        void memoryJustBelowThreshold_returnsTrue() {
            assertTrue(calculator.isNodeHealthy(50.0, 84.9));
        }

        @Test
        @DisplayName("CPU NULL 且内存超过阈值时返回 false")
        void cpuNull_memoryOver85_returnsFalse() {
            assertFalse(calculator.isNodeHealthy(null, 90.0));
        }

        @Test
        @DisplayName("内存 NULL 且 CPU 超过阈值时返回 false")
        void memoryNull_cpuOver80_returnsFalse() {
            assertFalse(calculator.isNodeHealthy(85.0, null));
        }
    }

    @Nested
    @DisplayName("calculateWeight")
    class CalculateWeightTests {

        @Test
        @DisplayName("不健康节点返回 0")
        void unhealthyNode_returnsZero() {
            // CPU 85% 超阈值，节点不健康，权重为 0
            assertEquals(0.0, calculator.calculateWeight(null, 85.0, 50.0));
        }

        @Test
        @DisplayName("健康节点返回正权重")
        void healthyNode_returnsPositiveWeight() {
            // 创建一个模拟节点
            com.aick.mmp.shared.model.EdgeNode node = com.aick.mmp.shared.model.EdgeNode.builder()
                    .currentCameraCount(5)
                    .maxCameraSupport(20)
                    .build();
            double weight = calculator.calculateWeight(node, 50.0, 60.0);
            assertTrue(weight > 0 && weight <= 100);
        }

        @Test
        @DisplayName("低负载节点权重高于高负载节点")
        void lowLoadNode_hasHigherWeight() {
            com.aick.mmp.shared.model.EdgeNode node = com.aick.mmp.shared.model.EdgeNode.builder()
                    .currentCameraCount(5)
                    .maxCameraSupport(20)
                    .build();
            double lowLoadWeight = calculator.calculateWeight(node, 30.0, 40.0);
            double highLoadWeight = calculator.calculateWeight(node, 70.0, 75.0);
            assertTrue(lowLoadWeight > highLoadWeight);
        }
    }
}
