package com.demo.sys.dto;

import com.demo.sys.entity.DailyPerformance;
import com.demo.sys.entity.EmployeePerformance;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.List;

/**
 * Performance data import result DTO
 * * Used to store temporary data after Excel import, including daily performance and monthly performance
 * * Add JsonIgnoreProperties to ensure compatibility of front-end and back-end data transmission
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PerformanceImportResultDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private List<DailyPerformance> dailyPerformances;
    private List<EmployeePerformance> monthlyPerformances;
    private String fileName;
    private int recordCount;
    
    public PerformanceImportResultDTO() {
    }
    
    public PerformanceImportResultDTO(
            List<DailyPerformance> dailyPerformances, 
            List<EmployeePerformance> monthlyPerformances, 
            String fileName) {
        this.dailyPerformances = dailyPerformances;
        this.monthlyPerformances = monthlyPerformances;
        this.fileName = fileName;
        this.recordCount = monthlyPerformances != null ? monthlyPerformances.size() : 0;
    }

    public List<DailyPerformance> getDailyPerformances() {
        return dailyPerformances;
    }

    public void setDailyPerformances(List<DailyPerformance> dailyPerformances) {
        this.dailyPerformances = dailyPerformances;
    }

    public List<EmployeePerformance> getMonthlyPerformances() {
        return monthlyPerformances;
    }

    public void setMonthlyPerformances(List<EmployeePerformance> monthlyPerformances) {
        this.monthlyPerformances = monthlyPerformances;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(int recordCount) {
        this.recordCount = recordCount;
    }
} 