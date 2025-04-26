package com.demo.sys.dto;

import java.time.YearMonth;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class EmployeePerformanceSearchDTO {
    private String employeeID;
    private String employeeName;
    private YearMonth recordDate;
    private String recordDateString;
    private int page = 0;
    private int size = 10;
    private String sortBy = "performanceRating";

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

    public YearMonth getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(YearMonth recordDate) {
        this.recordDate = recordDate;
    }

    public String getRecordDateString() {
        return recordDateString;
    }

    public void setRecordDateString(String recordDateString) {
        this.recordDateString = recordDateString;
        
        // try to YearMonth
        if (recordDateString != null && !recordDateString.isEmpty()) {
            try {
                if (recordDateString.length() == 7) { // YYYY-MM
                    this.recordDate = YearMonth.parse(recordDateString);
                } else if (recordDateString.length() == 10) { // YYYY-MM-DD
                    this.recordDate = YearMonth.parse(recordDateString.substring(0, 7));
                }
            } catch (Exception e) {
                // keep recordDate to null
            }
        }
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }
}