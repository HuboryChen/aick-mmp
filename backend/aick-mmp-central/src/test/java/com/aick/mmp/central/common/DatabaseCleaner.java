package com.aick.mmp.central.common;

import org.springframework.stereotype.Component;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component
public class DatabaseCleaner {

    @PersistenceContext
    private EntityManager entityManager;

    public void clear() {
        entityManager.createNativeQuery("DELETE FROM alert_rule").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM edge_node").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM camera").executeUpdate();
    }
}