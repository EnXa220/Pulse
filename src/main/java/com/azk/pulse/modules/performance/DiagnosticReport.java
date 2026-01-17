package com.azk.pulse.modules.performance;

import java.util.ArrayList;
import java.util.List;

public class DiagnosticReport {
    private final List<String> issues = new ArrayList<>();
    private final List<String> recommendations = new ArrayList<>();
    private final List<String> patterns = new ArrayList<>();
    private String risk = "LOW";
    private HealthEvaluator.HealthResult health;

    public List<String> getIssues() {
        return issues;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public List<String> getPatterns() {
        return patterns;
    }

    public String getRisk() {
        return risk;
    }

    public void setRisk(String risk) {
        this.risk = risk;
    }

    public HealthEvaluator.HealthResult getHealth() {
        return health;
    }

    public void setHealth(HealthEvaluator.HealthResult health) {
        this.health = health;
    }
}
