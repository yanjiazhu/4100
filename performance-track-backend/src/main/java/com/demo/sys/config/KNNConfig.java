package com.demo.sys.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "knn")
public class KNNConfig {
    private int k = 5;  // default-k-value
    
    // weight
    private double attendanceWeight = 0.35;
    private double kpiWeight = 0.50;
    private double overtimeWeight = 0.15;
    
    // Standardization parameters
    private double maxOvertimeHours = 40.0;  // standard overtime hours

    public int getK() {
        return k;
    }

    public void setK(int k) {
        this.k = k;
    }

    public double getAttendanceWeight() {
        return attendanceWeight;
    }

    public void setAttendanceWeight(double attendanceWeight) {
        this.attendanceWeight = attendanceWeight;
    }

    public double getKpiWeight() {
        return kpiWeight;
    }

    public void setKpiWeight(double kpiWeight) {
        this.kpiWeight = kpiWeight;
    }

    public double getOvertimeWeight() {
        return overtimeWeight;
    }

    public void setOvertimeWeight(double overtimeWeight) {
        this.overtimeWeight = overtimeWeight;
    }

    public double getMaxOvertimeHours() {
        return maxOvertimeHours;
    }

    public void setMaxOvertimeHours(double maxOvertimeHours) {
        this.maxOvertimeHours = maxOvertimeHours;
    }
}