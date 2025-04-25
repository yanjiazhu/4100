package com.demo.sys.model;

public class PerformanceTrainingData {
    private double attendanceRate;    // attendance
    private double kpiCompletion;     // kpi
    private double overtimeHours;     // overtime
    private int performanceRating;    // performance

    //
    public PerformanceTrainingData() {
    }

    //
    public PerformanceTrainingData(double attendanceRate, double kpiCompletion, 
                                 double overtimeHours, int performanceRating) {
        this.attendanceRate = attendanceRate;
        this.kpiCompletion = kpiCompletion;
        this.overtimeHours = overtimeHours;
        this.performanceRating = performanceRating;
    }

    // Getters
    public double getAttendanceRate() {
        return attendanceRate;
    }

    public double getKpiCompletion() {
        return kpiCompletion;
    }

    public double getOvertimeHours() {
        return overtimeHours;
    }

    public int getPerformanceRating() {
        return performanceRating;
    }

    // Setters
    public void setAttendanceRate(double attendanceRate) {
        this.attendanceRate = attendanceRate;
    }

    public void setKpiCompletion(double kpiCompletion) {
        this.kpiCompletion = kpiCompletion;
    }

    public void setOvertimeHours(double overtimeHours) {
        this.overtimeHours = overtimeHours;
    }

    public void setPerformanceRating(int performanceRating) {
        this.performanceRating = performanceRating;
    }

    // Get feature vector
    public double[] getFeatures() {
        return new double[]{attendanceRate, kpiCompletion, overtimeHours};
    }
} 