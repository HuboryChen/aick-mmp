package com.aick.mmp.central.service;

import com.aick.mmp.shared.model.EdgeNode;
import com.aick.mmp.shared.model.CdnNode;

import java.util.Map;

public interface NetworkMonitorService {
    void evaluateAndAdjust(EdgeNode edgeNode, Map<String, Double> metrics);

    void evaluateAndAdjustCdn(CdnNode cdnNode, Map<String, Double> metrics);
}
