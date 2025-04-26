package com.demo.sys.model;

/**
 * KNN
 */
public class KNNConfig {
    
    // K-value
    private int k = 3;
    
    // weight
    private double attendanceWeight = 0.4;  // attendance_weight
    private double kpiWeight = 0.5;         // KPI_weight
    private double overtimeWeight = 0.1;    // overtime_weight
    
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
} 