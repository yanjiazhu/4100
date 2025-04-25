package com.demo.sys.controller;

import com.demo.sys.entity.EmployeePerformance;
import com.demo.sys.entity.DailyPerformance;
import com.demo.sys.service.EmployeePerformanceService;
import com.demo.sys.service.ExcelImportService;
import com.demo.sys.dto.EmployeePerformanceSearchDTO;
import com.demo.sys.dto.PerformanceImportResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/employee-performance")
public class EmployeePerformanceController {
    
    private static final Logger logger = LoggerFactory.getLogger(EmployeePerformanceController.class);

    @Autowired
    private EmployeePerformanceService employeePerformanceService;

    @Autowired
    private ExcelImportService excelImportService;

    /**
     * upload the file to database
     */
    @PostMapping("/preview")
    public ResponseEntity<?> previewExcel(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("文件为空，请选择文件");
            }
            
            //
            YearMonth yearMonth = excelImportService.extractYearMonthFromExcel(file);
            
            //
            if (employeePerformanceService.hasDataForYearMonth(yearMonth)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Performance data for this month("+yearMonth.toString()+") already exists, please don't upload again");
                response.put("yearMonth", yearMonth.toString());
                response.put("alreadyExists", true);
                
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
            
            //
            PerformanceImportResultDTO result = excelImportService.processExcelWithoutSaving(file);
            
            //
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("文件处理失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body("文件处理失败: " + e.getMessage());
        }
    }
    
    /**
     * confirm to save the file
     */
    @PostMapping("/confirm")
    public ResponseEntity<?> confirmImport(@RequestBody PerformanceImportResultDTO data) {
        try {
            if (data == null || data.getMonthlyPerformances() == null || data.getMonthlyPerformances().isEmpty()) {
                return ResponseEntity.badRequest().body("没有待保存的数据");
            }
            
            //
            if (!data.getMonthlyPerformances().isEmpty()) {
                EmployeePerformance firstRecord = data.getMonthlyPerformances().get(0);
                YearMonth yearMonth = firstRecord.getRecordDate();
                
                //
                if (employeePerformanceService.hasDataForYearMonth(yearMonth)) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "该月份(" + yearMonth.toString() + ")的绩效数据已存在，请勿重复上传");
                    response.put("yearMonth", yearMonth.toString());
                    response.put("alreadyExists", true);
                    
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                }
            }
            
            //
            List<EmployeePerformance> savedPerformances = excelImportService.saveImportedData(
                data.getDailyPerformances(), 
                data.getMonthlyPerformances()
            );
            
            return ResponseEntity.ok(savedPerformances);
        } catch (Exception e) {
            logger.error("保存数据失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body("保存数据失败: " + e.getMessage());
        }
    }

    /**
     * same way to upload
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadExcel(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("文件为空，请选择文件");
            }
            
            // 从Excel文件中提取年月信息
            YearMonth yearMonth = excelImportService.extractYearMonthFromExcel(file);
            
            // 检查该月份的数据是否已存在
            if (employeePerformanceService.hasDataForYearMonth(yearMonth)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "该月份(" + yearMonth.toString() + ")的绩效数据已存在，请勿重复上传");
                response.put("yearMonth", yearMonth.toString());
                response.put("alreadyExists", true);
                
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
            
            List<EmployeePerformance> performances = excelImportService.importExcel(file);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "文件上传成功，共导入" + performances.size() + "条绩效记录");
            response.put("data", performances);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("文件处理失败: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "文件处理失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/search")
    public ResponseEntity<Page<EmployeePerformance>> search(@RequestBody EmployeePerformanceSearchDTO searchDTO) {
        try {
            PageRequest pageable = PageRequest.of(
                searchDTO.getPage(), 
                searchDTO.getSize(), 
                Sort.by(searchDTO.getSortBy())
            );
            
            Page<EmployeePerformance> result;
            
            //
            if (searchDTO.getRecordDate() != null) {
                result = employeePerformanceService.search(
                    searchDTO.getEmployeeID(),
                    searchDTO.getEmployeeName(),
                    searchDTO.getRecordDate(),
                    pageable
                );
            } else if (searchDTO.getRecordDateString() != null && !searchDTO.getRecordDateString().isEmpty()) {
                String dateStr = searchDTO.getRecordDateString();
                
                //
                try {
                    if (dateStr.length() >= 7) {
                        String yearMonthStr = dateStr.substring(0, 7); // YYYY-MM
                        String[] parts = yearMonthStr.split("-");
                        if (parts.length == 2) {
                            int year = Integer.parseInt(parts[0]);
                            int month = Integer.parseInt(parts[1]);
                            
                            //
                            List<EmployeePerformance> records = employeePerformanceService.findByYearAndMonth(year, month);
                            
                            if (!records.isEmpty()) {
                                //
                                int start = (int) pageable.getOffset();
                                int end = Math.min((start + pageable.getPageSize()), records.size());
                                
                                if (start < records.size()) {
                                    List<EmployeePerformance> pageContent = records.subList(start, end);
                                    return ResponseEntity.ok(new PageImpl<>(pageContent, pageable, records.size()));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    //
                }
                
                //
                String searchPrefix = dateStr;
                if (dateStr.length() >= 7) {
                    searchPrefix = dateStr.substring(0, 7); // 只用YYYY-MM部分
                }
                
                result = employeePerformanceService.searchWithDatePrefix(
                    searchDTO.getEmployeeID(),
                    searchDTO.getEmployeeName(),
                    searchPrefix,
                    pageable
                );
            } else {
                //
                result = employeePerformanceService.search(
                    searchDTO.getEmployeeID(),
                    searchDTO.getEmployeeName(),
                    null,
                    pageable
                );
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("搜索过程中发生错误: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get the outstanding employees in the past six months
     * * Return the employee with the best overall performance (by department, employee number, name)
     */
    @GetMapping("/top-performer")
    public ResponseEntity<?> getTopPerformer() {
        try {
            // 获取所有记录的日期
            List<String> allDates = employeePerformanceService.findAllDistinctDates();
            
            if (allDates.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "message", "No performance data available",
                    "messageZh", "没有可用的绩效数据",
                    "data", null
                ));
            }
            
            // find the newest month
            YearMonth latestMonth = null;
            for (String dateStr : allDates) {
                try {
                    YearMonth month = YearMonth.parse(dateStr);
                    if (latestMonth == null || month.isAfter(latestMonth)) {
                        latestMonth = month;
                    }
                } catch (Exception e) {
                    // jump the month
                    logger.warn("无法解析日期: {}", dateStr);
                }
            }
            
            if (latestMonth == null) {
                return ResponseEntity.ok(Map.of(
                    "message", "Could not determine the latest month from available data",
                    "messageZh", "无法从现有数据确定最新月份",
                    "data", null
                ));
            }
            
            logger.info("找到最新月份: {}", latestMonth);
            
            //
            Map<String, Double> employeeScores = new HashMap<>();
            Map<String, String> employeeDepartments = new HashMap<>();
            Map<String, String> employeeNames = new HashMap<>();
            
            //
            for (int i = 0; i < 6; i++) {
                YearMonth targetMonth = latestMonth.minusMonths(i);
                logger.info("处理月份: {}", targetMonth);
                
                //
                List<EmployeePerformance> monthlyData = employeePerformanceService.findByRecordDate(targetMonth);
                if (!monthlyData.isEmpty()) {
                    logger.info("月份 {} 找到 {} 条记录", targetMonth, monthlyData.size());
                    for (EmployeePerformance performance : monthlyData) {
                        String employeeId = performance.getEmployeeID();
                        
                        //
                        //
                        double attendanceScore = performance.getAttendanceRate() * 40;
                        double kpiScore = performance.getKpiCompletion() * 40;
                        
                        //
                        double overtimeScore = Math.min(performance.getOvertimeHours(), 10) * 2;
                        
                        double totalScore = attendanceScore + kpiScore + overtimeScore;
                        
                        //
                        employeeScores.put(
                            employeeId,
                            employeeScores.getOrDefault(employeeId, 0.0) + totalScore
                        );
                        
                        //
                        employeeDepartments.put(employeeId, performance.getDepartment());
                        employeeNames.put(employeeId, performance.getEmployeeName());
                    }
                } else {
                    logger.info("月份 {} 没有找到记录", targetMonth);
                }
            }
            
            //
            if (employeeScores.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "message", "No performance data available for the past six months",
                    "messageZh", "过去六个月没有可用的绩效数据",
                    "data", null
                ));
            }
            
            //
            String topEmployeeId = null;
            double highestScore = 0;
            
            for (Map.Entry<String, Double> entry : employeeScores.entrySet()) {
                if (entry.getValue() > highestScore) {
                    highestScore = entry.getValue();
                    topEmployeeId = entry.getKey();
                }
            }
            
            //
            Map<String, Object> result = new HashMap<>();
            result.put("employeeId", topEmployeeId);
            result.put("employeeName", employeeNames.get(topEmployeeId));
            result.put("department", employeeDepartments.get(topEmployeeId));
            result.put("score", highestScore);
            result.put("latestMonth", latestMonth.toString());
            
            return ResponseEntity.ok(Map.of(
                "message", "Successfully retrieved top performer for the past six months including " + latestMonth,
                "messageZh", "成功获取截至 " + latestMonth + " 的最近六个月最佳员工",
                "data", result
            ));
            
        } catch (Exception e) {
            logger.error("获取优秀员工失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "message", "Failed to get top performer: " + e.getMessage(),
                "messageZh", "获取最佳员工失败: " + e.getMessage(),
                "success", false
            ));
        }
    }
} 