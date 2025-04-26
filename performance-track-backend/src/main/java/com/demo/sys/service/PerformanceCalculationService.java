package com.demo.sys.service;

import com.demo.sys.entity.DailyPerformance;
import com.demo.sys.entity.EmployeePerformance;
import com.demo.sys.model.PerformanceTrainingData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PerformanceCalculationService {

    @Autowired
    private KNNModelService knnModel;

    /**
     * 处理新上传的Excel数据
     * 1. 保存DailyPerformance数据
     * 2. 训练KNN模型
     * 3. 生成EmployeePerformance记录
     */
    public List<EmployeePerformance> processExcelData(List<DailyPerformance> newDailyRecords) {
        // 1. 按员工和月份分组
        Map<String, Map<YearMonth, List<DailyPerformance>>> groupedRecords = 
            newDailyRecords.stream()
                .collect(Collectors.groupingBy(
                    DailyPerformance::getEmployeeId,
                    Collectors.groupingBy(record -> 
                        YearMonth.from(record.getRecordDate())
                    )
                ));

        // 2. 准备训练数据
        List<PerformanceTrainingData> trainingData = groupedRecords.values().stream()
            .flatMap(employeeMonths -> employeeMonths.values().stream()
                .map(monthRecords -> {
                    double attendanceRate = calculateAttendanceRate(monthRecords);
                    double kpiCompletion = calculateKPICompletion(monthRecords);
                    double overtimeHours = calculateOvertimeHours(monthRecords);
                    
                    // 基于月度指标计算初始评分用于训练
                    int initialRating = calculateInitialRating(
                        attendanceRate, kpiCompletion, overtimeHours);

                    return new PerformanceTrainingData(
                        attendanceRate,
                        kpiCompletion,
                        overtimeHours,
                        initialRating
                    );
                }))
            .collect(Collectors.toList());

        // 3. 训练KNN模型
        knnModel.addAllTrainingData(trainingData);

        // 4. 生成月度绩效记录
        return groupedRecords.values().stream()
            .flatMap(employeeMonths -> employeeMonths.values().stream()
                .map(monthRecords -> {
                    EmployeePerformance performance = createMonthlyPerformance(monthRecords);
                    
                    // 使用KNN预测评分
                    int predictedRating = knnModel.predict(
                        performance.getAttendanceRate(),
                        performance.getKpiCompletion(),
                        performance.getOvertimeHours()
                    );
                    performance.setPerformanceRating(predictedRating);
                    
                    return performance;
                }))
            .collect(Collectors.toList());
    }

    // 计算月度出勤率
    private double calculateAttendanceRate(List<DailyPerformance> dailyRecords) {
        double totalEffectiveWork = dailyRecords.stream()
                .mapToDouble(DailyPerformance::calculateEffectiveWorkRatio)
                .sum();
        return (totalEffectiveWork / dailyRecords.size()) * 100;
    }

    // 计算月度KPI完成率
    private double calculateKPICompletion(List<DailyPerformance> dailyRecords) {
        int totalCompleted = dailyRecords.stream()
                .mapToInt(DailyPerformance::getCompletedTasks)
                .sum();
        int totalTasks = dailyRecords.stream()
                .mapToInt(DailyPerformance::getTotalTasks)
                .sum();
        return totalTasks > 0 ? ((double) totalCompleted / totalTasks) * 100 : 0;
    }

    // 计算月度加班时长
    private double calculateOvertimeHours(List<DailyPerformance> dailyRecords) {
        return dailyRecords.stream()
                .mapToDouble(record -> record.getOvertimeHours() != null ? 
                        record.getOvertimeHours() : 0.0)
                .sum();
    }

    // 创建月度绩效记录
    private EmployeePerformance createMonthlyPerformance(List<DailyPerformance> dailyRecords) {
        DailyPerformance firstRecord = dailyRecords.get(0);
        
        EmployeePerformance performance = new EmployeePerformance();
        performance.setEmployeeID(firstRecord.getEmployeeId());
        performance.setEmployeeName(firstRecord.getEmployeeName());
        performance.setDepartment(firstRecord.getDepartment());
        performance.setAttendanceRate(calculateAttendanceRate(dailyRecords));
        performance.setKpiCompletion(calculateKPICompletion(dailyRecords));
        performance.setOvertimeHours(calculateOvertimeHours(dailyRecords));
        performance.setRecordDate(YearMonth.from(firstRecord.getRecordDate()));
        
        return performance;
    }

    // 计算初始评分（用于模型训练）
    private int calculateInitialRating(double attendanceRate, double kpiCompletion, double overtimeHours) {
        double score = 0;
        
        // 考勤权重35%
        score += (attendanceRate / 100.0) * 0.35;
        
        // KPI权重50%
        score += (kpiCompletion / 100.0) * 0.50;
        
        // 加班权重15%（假设20小时是满分）
        score += Math.min(overtimeHours / 20.0, 1.0) * 0.15;
        
        // 转换为1-3分制
        if (score >= 0.9) return 3;  // 优
        if (score >= 0.75) return 2; // 良
        return 1;                    // 差
    }
} 