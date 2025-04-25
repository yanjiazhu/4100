package com.demo.sys.service;

import com.demo.sys.dto.PerformanceImportResultDTO;
import com.demo.sys.entity.DailyPerformance;
import com.demo.sys.entity.EmployeePerformance;
import com.demo.sys.repository.DailyPerformanceRepository;
import com.demo.sys.repository.EmployeePerformanceRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExcelImportService {

    @Autowired
    private DailyPerformanceRepository dailyPerformanceRepository;

    @Autowired
    private EmployeePerformanceRepository employeePerformanceRepository;
    
    @Autowired
    private KNNModelService knnModelService;

    /**
     * 导入Excel文件，处理每日考勤记录并计算月度绩效，并保存到数据库
     */
    @Transactional
    public List<EmployeePerformance> importExcel(MultipartFile file) throws IOException {
        // 1. 解析Excel文件，获取每日记录和月度绩效
        PerformanceImportResultDTO result = processExcelWithoutSaving(file);
        
        // 2. 保存数据到数据库
        return saveImportedData(result.getDailyPerformances(), result.getMonthlyPerformances());
    }
    
    /**
     * 处理Excel文件但不保存到数据库，用于预览
     */
    public PerformanceImportResultDTO processExcelWithoutSaving(MultipartFile file) throws IOException {
        // 1. 解析Excel文件，获取每日记录
        List<DailyPerformance> dailyRecords = parseDailyRecords(file);
        
        // 2. 按员工和月份分组计算月度绩效
        List<EmployeePerformance> monthlyPerformances = calculateMonthlyPerformance(dailyRecords);
        
        // 3. 使用KNN模型计算绩效评分
        calculatePerformanceRatings(monthlyPerformances);
        
        // 返回处理结果，但不保存到数据库
        return new PerformanceImportResultDTO(
            dailyRecords, 
            monthlyPerformances, 
            file.getOriginalFilename()
        );
    }
    
    /**
     * 保存导入的数据到数据库
     */
    @Transactional
    public List<EmployeePerformance> saveImportedData(
            List<DailyPerformance> dailyRecords, 
            List<EmployeePerformance> monthlyPerformances) {
        
        // 1. 保存每日记录
        dailyPerformanceRepository.saveAll(dailyRecords);
        
        // 2. 保存月度绩效
        employeePerformanceRepository.saveAll(monthlyPerformances);
        
        return monthlyPerformances;
    }

    /**
     * 解析Excel文件，获取每日记录
     */
    private List<DailyPerformance> parseDailyRecords(MultipartFile file) throws IOException {
        List<DailyPerformance> dailyRecords = new ArrayList<>();
        Workbook workbook = WorkbookFactory.create(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);

        // 验证表头
        Row headerRow = sheet.getRow(0);
        validateHeaders(headerRow);

        // 处理数据行
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            try {
                DailyPerformance record = createDailyRecord(row);
                dailyRecords.add(record);
            } catch (Exception e) {
                System.err.println("处理第 " + (i + 1) + " 行数据时出错: " + e.getMessage());
            }
        }

        return dailyRecords;
    }

    /**
     * 验证表头是否符合要求
     */
    private void validateHeaders(Row headerRow) {
        String[] expectedHeaders = {
            "Date", "EmployeeID", "EmployeeName", "Department", "Attendance", 
            "LateEarlyMinutes", "OvertimeHours", "TotalTasks", "CompletedTasks"
        };

        for (int i = 0; i < expectedHeaders.length; i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null || !expectedHeaders[i].equals(getCellValueAsString(cell))) {
                throw new IllegalArgumentException(
                    "Excel文件格式错误：表头不符合要求，应为: " + 
                    String.join(", ", expectedHeaders)
                );
            }
        }
    }

    /**
     * 从行数据创建每日记录
     */
    private DailyPerformance createDailyRecord(Row row) {
        DailyPerformance record = new DailyPerformance();
        
        // 解析日期
        String dateStr = getCellValueAsString(row.getCell(0));
        if (dateStr != null && !dateStr.isEmpty()) {
            record.setRecordDate(LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE));
        } else {
            throw new IllegalArgumentException("日期不能为空");
        }
        
        // 基本信息
        record.setEmployeeId(getCellValueAsString(row.getCell(1)));
        record.setEmployeeName(getCellValueAsString(row.getCell(2)));
        record.setDepartment(getCellValueAsString(row.getCell(3)));
        
        // 出勤信息
        record.setAttendance(getCellValueAsString(row.getCell(4)));
        record.setLateEarlyMinutes(getCellValueAsInteger(row.getCell(5)));
        record.setOvertimeHours(getCellValueAsDouble(row.getCell(6)));
        
        // 任务信息
        record.setTotalTasks(getCellValueAsInteger(row.getCell(7)));
        record.setCompletedTasks(getCellValueAsInteger(row.getCell(8)));
        
        return record;
    }

    /**
     * 计算月度绩效
     */
    private List<EmployeePerformance> calculateMonthlyPerformance(List<DailyPerformance> dailyRecords) {
        // 按员工ID和月份分组
        Map<String, Map<YearMonth, List<DailyPerformance>>> groupedRecords = 
            dailyRecords.stream()
                .collect(Collectors.groupingBy(
                    DailyPerformance::getEmployeeId,
                    Collectors.groupingBy(record -> 
                        YearMonth.from(record.getRecordDate())
                    )
                ));
        
        List<EmployeePerformance> monthlyPerformances = new ArrayList<>();
        
        // 遍历每个员工的每月记录
        for (Map.Entry<String, Map<YearMonth, List<DailyPerformance>>> employeeEntry : groupedRecords.entrySet()) {
            String employeeId = employeeEntry.getKey();
            
            for (Map.Entry<YearMonth, List<DailyPerformance>> monthEntry : employeeEntry.getValue().entrySet()) {
                YearMonth yearMonth = monthEntry.getKey();
                List<DailyPerformance> monthRecords = monthEntry.getValue();
                
                // 如果有记录，则创建月度绩效
                if (!monthRecords.isEmpty()) {
                    EmployeePerformance performance = new EmployeePerformance();
                    
                    // 设置基本信息
                    DailyPerformance firstRecord = monthRecords.get(0);
                    performance.setEmployeeID(employeeId);
                    performance.setEmployeeName(firstRecord.getEmployeeName());
                    performance.setDepartment(firstRecord.getDepartment());
                    performance.setRecordDate(yearMonth);
                    
                    // 计算出勤率
                    double totalEffectiveHours = 0; // 实际有效工作小时
                    double totalExpectedHours = 0; // 应该工作的总小时数

                    // 过滤掉周末，只计算工作日
                    List<DailyPerformance> workdayRecords = monthRecords.stream()
                        .filter(record -> {
                            int dayOfWeek = record.getRecordDate().getDayOfWeek().getValue();
                            // 1-5是周一到周五，6-7是周末
                            return dayOfWeek >= 1 && dayOfWeek <= 5;
                        })
                        .collect(Collectors.toList());
                    
                    System.out.println("员工: " + employeeId + ", 月份: " + yearMonth + ", 总记录: " + 
                        monthRecords.size() + ", 工作日记录: " + workdayRecords.size());
                    
                    for (DailyPerformance daily : workdayRecords) {
                        // 每个工作日计入应该工作的时间
                        totalExpectedHours += 8;

                        if ("Y".equalsIgnoreCase(daily.getAttendance())) {
                            // 计算有效工作时间
                            double lateEarlyMinutes = (daily.getLateEarlyMinutes() != null ? daily.getLateEarlyMinutes() : 0);
                            double lateEarlyHours = lateEarlyMinutes / 60.0;
                            double effectiveHours = Math.max(0, 8 - lateEarlyHours); // 不能为负数
                            totalEffectiveHours += effectiveHours;
                            
                            System.out.println("  员工: " + employeeId + 
                                ", 日期: " + daily.getRecordDate() + 
                                ", 出勤: Y" +
                                ", 迟到早退(分钟): " + lateEarlyMinutes + 
                                ", 有效工作时间(小时): " + effectiveHours);
                        } else {
                            // 没有出勤，有效工作时间为0
                            System.out.println("  员工: " + employeeId + 
                                ", 日期: " + daily.getRecordDate() + 
                                ", 出勤: N, 有效工作时间: 0");
                        }
                    }
                    
                    // 避免除以零
                    double attendanceRate = 0;
                    if (totalExpectedHours > 0) {
                        attendanceRate = (totalEffectiveHours / totalExpectedHours) * 100;
                    }
                    
                    System.out.println("员工: " + employeeId + 
                        ", 月份: " + yearMonth +
                        ", 总有效工作时间: " + totalEffectiveHours + 
                        ", 应工作时间: " + totalExpectedHours + 
                        ", 计算的出勤率: " + attendanceRate + "%");
                    
                    performance.setAttendanceRate(Math.round(attendanceRate * 10) / 10.0); // 保留1位小数
                    
                    // 计算KPI完成率
                    int totalTasks = workdayRecords.stream()
                        .mapToInt(r -> r.getTotalTasks() != null ? r.getTotalTasks() : 0)
                        .sum();
                    int completedTasks = workdayRecords.stream()
                        .mapToInt(r -> r.getCompletedTasks() != null ? r.getCompletedTasks() : 0)
                        .sum();
                    
                    double kpiCompletion = totalTasks > 0 ? ((double) completedTasks / totalTasks) * 100 : 0;
                    performance.setKpiCompletion(Math.round(kpiCompletion * 10) / 10.0); // 保留1位小数
                    
                    // 计算加班时长
                    double overtimeHours = monthRecords.stream()
                        .mapToDouble(r -> r.getOvertimeHours() != null ? r.getOvertimeHours() : 0)
                        .sum();
                    performance.setOvertimeHours(Math.round(overtimeHours * 10) / 10.0); // 保留1位小数
                    
                    // 绩效评分后期由KNN模型计算
                    
                    monthlyPerformances.add(performance);
                }
            }
        }
        
        return monthlyPerformances;
    }

    /**
     * 使用KNN模型计算绩效评分
     */
    private void calculatePerformanceRatings(List<EmployeePerformance> performances) {
        try {
            System.out.println("开始使用KNN模型计算绩效评分...");
            
            // 1. 准备训练数据 - 使用DailyPerformance数据
            // 获取历史每日绩效数据
            List<DailyPerformance> dailyTrainingData = dailyPerformanceRepository.findAll();
            
            System.out.println("获取到 " + dailyTrainingData.size() + " 条每日绩效记录用于训练");
            
            // 如果没有足够的历史数据，添加一些样本数据
            List<EmployeePerformance> sampleData = new ArrayList<>();
            if (dailyTrainingData.size() < 20) {
                System.out.println("每日绩效历史数据不足，添加模拟数据进行训练");
                sampleData = createSampleTrainingData();
            }
            
            // 2. 训练模型
            if (dailyTrainingData.size() >= 10) {
                System.out.println("使用每日绩效数据训练KNN模型");
                knnModelService.trainModelWithDailyData(dailyTrainingData);
                
                // 3. 为每个没有评分的绩效记录预测评分
                for (EmployeePerformance performance : performances) {
                    if (performance.getPerformanceRating() == null) {
                        Integer predictedRating = knnModelService.predictPerformanceRating(
                            performance.getAttendanceRate(),
                            performance.getKpiCompletion(),
                            performance.getOvertimeHours()
                        );
                        
                        performance.setPerformanceRating(predictedRating);
                        System.out.println("员工: " + performance.getEmployeeID() + 
                            ", 预测绩效评分: " + predictedRating + 
                            " (出勤率: " + performance.getAttendanceRate() + 
                            ", KPI完成率: " + performance.getKpiCompletion() + 
                            ", 加班时长: " + performance.getOvertimeHours() + ")");
                    }
                }
            } else if (!sampleData.isEmpty()) {
                // 使用样本数据训练
                System.out.println("使用样本数据训练KNN模型");
                knnModelService.trainModel(sampleData);
                
                // 预测评分
                for (EmployeePerformance performance : performances) {
                    if (performance.getPerformanceRating() == null) {
                        Integer predictedRating = knnModelService.predictPerformanceRating(
                            performance.getAttendanceRate(),
                            performance.getKpiCompletion(),
                            performance.getOvertimeHours()
                        );
                        
                        performance.setPerformanceRating(predictedRating);
                        System.out.println("员工: " + performance.getEmployeeID() + 
                            ", 预测绩效评分(样本): " + predictedRating + 
                            " (出勤率: " + performance.getAttendanceRate() + 
                            ", KPI完成率: " + performance.getKpiCompletion() + 
                            ", 加班时长: " + performance.getOvertimeHours() + ")");
                    }
                }
            } else {
                System.out.println("训练数据不足，无法训练KNN模型。需要至少10条记录。");
                // 使用简单规则设置默认评分
                setDefaultRatings(performances);
            }
        } catch (Exception e) {
            System.err.println("KNN模型计算异常: " + e.getMessage());
            e.printStackTrace();
            // 发生异常时使用简单规则设置默认评分
            setDefaultRatings(performances);
        }
    }
    
    /**
     * 当无法使用KNN模型时，使用简单规则设置默认评分
     */
    private void setDefaultRatings(List<EmployeePerformance> performances) {
        System.out.println("使用简单规则设置默认评分");
        
        for (EmployeePerformance performance : performances) {
            if (performance.getPerformanceRating() == null) {
                // 简单规则：基于出勤率和KPI完成率的加权平均
                double score = (performance.getAttendanceRate() * 0.4) + 
                               (performance.getKpiCompletion() * 0.6);
                
                // 将分数转换为1-3的评分
                int rating;
                if (score >= 85) {
                    rating = 3; // 优
                } else if (score >= 70) {
                    rating = 2; // 良
                } else {
                    rating = 1; // 差
                }
                
                performance.setPerformanceRating(rating);
                System.out.println("员工: " + performance.getEmployeeID() + 
                    ", 默认绩效评分: " + rating + 
                    " (加权分数: " + score + ", 出勤率: " + performance.getAttendanceRate() + 
                    ", KPI完成率: " + performance.getKpiCompletion() + ")");
            }
        }
    }
    
    /**
     * 创建一些样本训练数据（当系统中没有足够的历史数据时）
     */
    private List<EmployeePerformance> createSampleTrainingData() {
        List<EmployeePerformance> samples = new ArrayList<>();
        
        // 样本1：高出勤率、高KPI、适量加班 - 优秀(3)
        EmployeePerformance sample1 = new EmployeePerformance();
        sample1.setAttendanceRate(95.0);
        sample1.setKpiCompletion(92.0);
        sample1.setOvertimeHours(10.0);
        sample1.setPerformanceRating(3);
        samples.add(sample1);
        
        // 样本2：高出勤率、一般KPI、少量加班 - 良好(2)
        EmployeePerformance sample2 = new EmployeePerformance();
        sample2.setAttendanceRate(90.0);
        sample2.setKpiCompletion(80.0);
        sample2.setOvertimeHours(5.0);
        sample2.setPerformanceRating(2);
        samples.add(sample2);
        
        // 样本3：低出勤率、低KPI、无加班 - 差(1)
        EmployeePerformance sample3 = new EmployeePerformance();
        sample3.setAttendanceRate(70.0);
        sample3.setKpiCompletion(65.0);
        sample3.setOvertimeHours(0.0);
        sample3.setPerformanceRating(1);
        samples.add(sample3);
        
        // 样本4：一般出勤率、高KPI、大量加班 - 优秀(3)
        EmployeePerformance sample4 = new EmployeePerformance();
        sample4.setAttendanceRate(85.0);
        sample4.setKpiCompletion(95.0);
        sample4.setOvertimeHours(20.0);
        sample4.setPerformanceRating(3);
        samples.add(sample4);
        
        // 样本5：高出勤率、低KPI、适量加班 - 良好(2)
        EmployeePerformance sample5 = new EmployeePerformance();
        sample5.setAttendanceRate(92.0);
        sample5.setKpiCompletion(70.0);
        sample5.setOvertimeHours(8.0);
        sample5.setPerformanceRating(2);
        samples.add(sample5);
        
        // 样本6：一般出勤率、一般KPI、无加班 - 良好(2)
        EmployeePerformance sample6 = new EmployeePerformance();
        sample6.setAttendanceRate(80.0);
        sample6.setKpiCompletion(80.0);
        sample6.setOvertimeHours(0.0);
        sample6.setPerformanceRating(2);
        samples.add(sample6);
        
        // 样本7：低出勤率、高KPI、适量加班 - 良好(2)
        EmployeePerformance sample7 = new EmployeePerformance();
        sample7.setAttendanceRate(75.0);
        sample7.setKpiCompletion(90.0);
        sample7.setOvertimeHours(10.0);
        sample7.setPerformanceRating(2);
        samples.add(sample7);
        
        // 样本8：一般出勤率、低KPI、无加班 - 差(1)
        EmployeePerformance sample8 = new EmployeePerformance();
        sample8.setAttendanceRate(80.0);
        sample8.setKpiCompletion(60.0);
        sample8.setOvertimeHours(0.0);
        sample8.setPerformanceRating(1);
        samples.add(sample8);
        
        System.out.println("创建了 " + samples.size() + " 条样本训练数据");
        return samples;
    }

    // 工具方法：获取单元格字符串值
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                        .format(DateTimeFormatter.ISO_LOCAL_DATE);
                }
                return String.valueOf((long) cell.getNumericCellValue());
            default:
                return null;
        }
    }

    // 工具方法：获取单元格整数值
    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            case STRING:
                try {
                    return Integer.parseInt(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    // 工具方法：获取单元格浮点值
    private Double getCellValueAsDouble(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    return Double.parseDouble(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    /**
     * 从Excel文件名或内容中提取年月信息
     * 
     * @param file Excel文件
     * @return 提取的YearMonth对象
     * @throws IOException 如果文件读取失败
     */
    public YearMonth extractYearMonthFromExcel(MultipartFile file) throws IOException {
        // 尝试从文件名中提取年月
        String filename = file.getOriginalFilename();
        if (filename != null && filename.matches(".*_\\d{6}\\.xlsx?$")) {
            // 从像 "EmployeePerformance_202406.xlsx" 这样的文件名中提取
            String yearMonthStr = filename.replaceAll(".*_(\\d{6})\\..+", "$1");
            try {
                int year = Integer.parseInt(yearMonthStr.substring(0, 4));
                int month = Integer.parseInt(yearMonthStr.substring(4, 6));
                return YearMonth.of(year, month);
            } catch (Exception e) {
                // 如果从文件名解析失败，继续从内容提取
            }
        }
        
        // 从文件内容中提取年月
        Workbook workbook = WorkbookFactory.create(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        
        // 跳过表头，读取第一个数据行
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            Cell dateCell = row.getCell(0);
            if (dateCell != null) {
                String dateStr = getCellValueAsString(dateCell);
                if (dateStr != null && !dateStr.isEmpty()) {
                    try {
                        LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                        return YearMonth.from(date);
                    } catch (Exception e) {
                        // 继续尝试下一行
                    }
                }
            }
        }
        
        throw new IllegalArgumentException("无法从文件中提取年月信息");
    }
} 