package com.demo.sys.dto;

import java.time.LocalDate;

public class DailyPerformanceDTO {
    private LocalDate date;
    private String employeeID;
    private String employeeName;
    private String department;
    private String attendance;  // Y/N
    private Integer lateEarlyMinutes;
    private Double overtimeHours;
    private Integer totalTasks;
    private Integer completedTasks;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
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

    public Double getOvertimeHours() {
        return overtimeHours;
    }

    public void setOvertimeHours(Double overtimeHours) {
        this.overtimeHours = overtimeHours;
    }

    public Integer getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(Integer totalTasks) {
        this.totalTasks = totalTasks;
    }

    public Integer getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(Integer completedTasks) {
        this.completedTasks = completedTasks;
    }
}