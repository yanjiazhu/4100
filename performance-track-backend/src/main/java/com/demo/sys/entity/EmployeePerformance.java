package com.demo.sys.entity;

import javax.persistence.*;
import java.time.YearMonth;

@Entity
@Table(name = "employee_performance")
public class EmployeePerformance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "employee_id", nullable = false)
    private String employeeID;
    
    @Column(name = "employee_name", nullable = false)
    private String employeeName;
    
    @Column(nullable = false)
    private String department;
    
    @Column(name = "attendance_rate", nullable = false)
    private double attendanceRate;
    
    @Column(name = "kpi_completion", nullable = false)
    private double kpiCompletion;
    
    @Column(name = "overtime_hours", nullable = false)
    private double overtimeHours;
    
    @Column(name = "performance_rating")
    private Integer performanceRating;  // 1-差, 2-良, 3-优
    
    @Column(name = "record_date", nullable = false)
    private YearMonth recordDate;
    
    // Custom converter for YearMonth
    @Convert(converter = YearMonthConverter.class)
    public YearMonth getRecordDate() {
        return recordDate;
    }
    
    public void setRecordDate(YearMonth recordDate) {
        this.recordDate = recordDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmployeeID() {
        return employeeID;
    }

    public void setEmployeeID(String employeeID) {
        this.employeeID = employeeID;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public double getAttendanceRate() {
        return attendanceRate;
    }

    public void setAttendanceRate(double attendanceRate) {
        this.attendanceRate = attendanceRate;
    }

    public double getKpiCompletion() {
        return kpiCompletion;
    }

    public void setKpiCompletion(double kpiCompletion) {
        this.kpiCompletion = kpiCompletion;
    }

    public double getOvertimeHours() {
        return overtimeHours;
    }

    public void setOvertimeHours(double overtimeHours) {
        this.overtimeHours = overtimeHours;
    }

    public Integer getPerformanceRating() {
        return performanceRating;
    }

    public void setPerformanceRating(Integer performanceRating) {
        this.performanceRating = performanceRating;
    }
}