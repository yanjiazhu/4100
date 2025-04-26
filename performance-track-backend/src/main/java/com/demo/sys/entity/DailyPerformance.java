package com.demo.sys.entity;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "daily_performance")
public class DailyPerformance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "employee_id", nullable = false)
    private String employeeId;
    
    @Column(name = "employee_name", nullable = false)
    private String employeeName;
    
    @Column(nullable = false)
    private String department;
    
    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;
    
    @Column(name = "attendance", nullable = false)
    private String attendance;  // Y-attendance, N-absence
    
    @Column(name = "late_early_minutes")
    private Integer lateEarlyMinutes;  // late_early
    
    @Column(name = "completed_tasks", nullable = false)
    private Integer completedTasks;    // completed_task
    
    @Column(name = "total_tasks", nullable = false)
    private Integer totalTasks;        // total_task
    
    @Column(name = "overtime_hours")
    private Double overtimeHours;      // overtime hour
    
    // Calculate the effective working time ratio of the day (taking into account lateness and early departure)
    public double calculateEffectiveWorkRatio() {
        if ("N".equals(attendance)) {
            return 0.0;
        }
        
        double standardHours = 8.0;
        double lateEarlyHours = (lateEarlyMinutes != null ? lateEarlyMinutes : 0) / 60.0;
        double effectiveHours = standardHours - lateEarlyHours;
        
        return Math.max(0.0, Math.min(1.0, effectiveHours / standardHours));
    }
    
    // Calculate the task completion rate for the day
    public double calculateTaskCompletionRate() {
        if (totalTasks == 0) {
            return 0.0;
        }
        return (double) completedTasks / totalTasks * 100;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
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

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(LocalDate recordDate) {
        this.recordDate = recordDate;
    }

    public String getAttendance() {
        return attendance;
    }

    public void setAttendance(String attendance) {
        this.attendance = attendance;
    }

    public Integer getLateEarlyMinutes() {
        return lateEarlyMinutes;
    }

    public void setLateEarlyMinutes(Integer lateEarlyMinutes) {
        this.lateEarlyMinutes = lateEarlyMinutes;
    }

    public Integer getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(Integer completedTasks) {
        this.completedTasks = completedTasks;
    }

    public Integer getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(Integer totalTasks) {
        this.totalTasks = totalTasks;
    }

    public Double getOvertimeHours() {
        return overtimeHours;
    }

    public void setOvertimeHours(Double overtimeHours) {
        this.overtimeHours = overtimeHours;
    }
}