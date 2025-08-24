package com.aick.mmp.service;

import com.aick.mmp.model.EdgeNode;
import com.aick.mmp.model.CdnNode;

import java.util.Map;

public interface NetworkMonitorService {
    void evaluateAndAdjust(EdgeNode edgeNode, Map<String, Double> metrics);

    void evaluateAndAdjustCdn(CdnNode cdnNode, Map<String, Double> metrics);
}
