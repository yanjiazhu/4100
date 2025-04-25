package com.demo.sys.service;

import com.demo.sys.dto.DailyPerformanceDTO;
import com.demo.sys.entity.EmployeePerformance;
import com.demo.sys.repository.EmployeePerformanceRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExcelService {

    @Autowired
    private EmployeePerformanceRepository employeePerformanceRepository;

    public List<EmployeePerformance> processExcelFile(MultipartFile file) throws IOException {
        List<DailyPerformanceDTO> dailyData = readExcelFile(file);
        return calculateMonthlyPerformance(dailyData);
    }

    private List<DailyPerformanceDTO> readExcelFile(MultipartFile file) throws IOException {
        List<DailyPerformanceDTO> dailyData = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();
            
            // Skip header row
            if (rows.hasNext()) {
                rows.next();
            }
            
            while (rows.hasNext()) {
                Row row = rows.next();
                DailyPerformanceDTO dto = new DailyPerformanceDTO();
                
                dto.setDate(LocalDate.parse(row.getCell(0).getStringCellValue()));
                dto.setEmployeeID(row.getCell(1).getStringCellValue());
                dto.setEmployeeName(row.getCell(2).getStringCellValue());
                dto.setDepartment(row.getCell(3).getStringCellValue());
                dto.setAttendance(row.getCell(4).getStringCellValue());
                dto.setLateEarlyMinutes((int) row.getCell(5).getNumericCellValue());
                dto.setOvertimeHours(row.getCell(6).getNumericCellValue());
                dto.setTotalTasks((int) row.getCell(7).getNumericCellValue());
                dto.setCompletedTasks((int) row.getCell(8).getNumericCellValue());
                
                dailyData.add(dto);
            }
        }
        
        return dailyData;
    }

    private List<EmployeePerformance> calculateMonthlyPerformance(List<DailyPerformanceDTO> dailyData) {
        // 按员工ID分组
        Map<String, List<DailyPerformanceDTO>> employeeDataMap = dailyData.stream()
                .collect(Collectors.groupingBy(DailyPerformanceDTO::getEmployeeID));
        
        List<EmployeePerformance> monthlyPerformances = new ArrayList<>();
        
        for (Map.Entry<String, List<DailyPerformanceDTO>> entry : employeeDataMap.entrySet()) {
            List<DailyPerformanceDTO> employeeData = entry.getValue();
            DailyPerformanceDTO firstRecord = employeeData.get(0);
            
            // 计算当月工作日数（不包括周末）
            YearMonth yearMonth = YearMonth.from(firstRecord.getDate());
            long totalWorkDays = employeeData.size(); // 排除了周末
            
            // 计算出勤率
            double attendanceRate = calculateAttendanceRate(employeeData, totalWorkDays);
            
            // 计算KPI完成率
            double kpiCompletion = calculateKPICompletion(employeeData);
            
            // 计算总加班时长
            double totalOvertimeHours = employeeData.stream()
                    .mapToDouble(DailyPerformanceDTO::getOvertimeHours)
                    .sum();
            
            EmployeePerformance performance = new EmployeePerformance();
            performance.setEmployeeID(firstRecord.getEmployeeID());
            performance.setEmployeeName(firstRecord.getEmployeeName());
            performance.setDepartment(firstRecord.getDepartment());
            performance.setKpiCompletion(kpiCompletion);
            performance.setAttendanceRate(attendanceRate);
            performance.setOvertimeHours(totalOvertimeHours);
            performance.setRecordDate(yearMonth);
            // performanceRating will be calculated by KNN model later
            
            monthlyPerformances.add(performance);
        }
        
        return monthlyPerformances;
    }
    
    private double calculateAttendanceRate(List<DailyPerformanceDTO> employeeData, long totalWorkDays) {
        double effectiveAttendanceDays = 0;
        
        for (DailyPerformanceDTO daily : employeeData) {
            if ("Y".equals(daily.getAttendance())) {
                // 如果有迟到早退，按比例计算出勤率
                if (daily.getLateEarlyMinutes() > 0) {
                    double effectiveHours = Math.max(8 - (daily.getLateEarlyMinutes() / 60.0), 0);
                    effectiveAttendanceDays += (effectiveHours / 8.0);
                } else {
                    effectiveAttendanceDays += 1.0;
                }
            }
        }
        
        return (effectiveAttendanceDays / totalWorkDays) * 100;
    }
    
    private double calculateKPICompletion(List<DailyPerformanceDTO> employeeData) {
        int totalTasksAssigned = employeeData.stream()
                .mapToInt(DailyPerformanceDTO::getTotalTasks)
                .sum();
        
        int totalTasksCompleted = employeeData.stream()
                .mapToInt(DailyPerformanceDTO::getCompletedTasks)
                .sum();
        
        return totalTasksAssigned > 0 ? ((double) totalTasksCompleted / totalTasksAssigned) * 100 : 0;
    }
} 