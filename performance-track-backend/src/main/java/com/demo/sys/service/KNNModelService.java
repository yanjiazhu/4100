package com.demo.sys.service;

import com.demo.sys.entity.DailyPerformance;
import com.demo.sys.entity.EmployeePerformance;
import com.demo.sys.model.KNNConfig;
import com.demo.sys.model.PerformanceTrainingData;
import org.springframework.stereotype.Service;
import weka.classifiers.Classifier;
import weka.classifiers.lazy.IBk;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.neighboursearch.LinearNNSearch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class KNNModelService {
    
    private Classifier knnClassifier;
    private Instances trainingInstances;
    private ArrayList<Attribute> attributes;
    private KNNConfig knnConfig;
    
    public KNNModelService() {
        // 默认KNN配置
        knnConfig = new KNNConfig();
        knnConfig.setK(3);
        knnConfig.setAttendanceWeight(0.4);
        knnConfig.setKpiWeight(0.5);
        knnConfig.setOvertimeWeight(0.1);
    }
    
    /**
     * 添加多个训练数据
     */
    public void addAllTrainingData(List<PerformanceTrainingData> trainingDataList) {
        try {
            System.out.println("开始添加训练数据...");
            
            // 初始化属性
            initializeAttributes();
            
            // 创建训练数据集
            if (trainingInstances == null) {
                trainingInstances = new Instances("PerformanceTraining", attributes, 0);
                trainingInstances.setClassIndex(attributes.size() - 1);
            }
            
            // 添加训练数据
            for (PerformanceTrainingData data : trainingDataList) {
                Instance instance = createInstance(
                    data.getAttendanceRate(),
                    data.getKpiCompletion(),
                    data.getOvertimeHours(),
                    data.getPerformanceRating()
                );
                trainingInstances.add(instance);
            }
            
            System.out.println("添加了 " + trainingDataList.size() + " 条训练数据，总计 " + trainingInstances.size() + " 条");
            
            // 训练模型
            buildAndTrainModel();
        } catch (Exception e) {
            System.err.println("添加训练数据失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("添加训练数据失败", e);
        }
    }
    
    /**
     * 预测绩效评分
     */
    public int predict(double attendanceRate, double kpiCompletion, double overtimeHours) {
        return predictPerformanceRating(attendanceRate, kpiCompletion, overtimeHours);
    }
    
    /**
     * 使用员工绩效数据训练KNN模型
     */
    public void trainModel(List<EmployeePerformance> trainingData) {
        try {
            System.out.println("开始训练KNN模型(使用月度数据)...");
            
            // 初始化属性
            initializeAttributes();
            
            // 创建训练数据集
            trainingInstances = new Instances("PerformanceTraining", attributes, 0);
            trainingInstances.setClassIndex(attributes.size() - 1);
            
            // 添加训练数据
            for (EmployeePerformance performance : trainingData) {
                if (performance.getPerformanceRating() != null) {
                    Instance instance = createInstance(
                        performance.getAttendanceRate(),
                        performance.getKpiCompletion(),
                        performance.getOvertimeHours(),
                        performance.getPerformanceRating()
                    );
                    trainingInstances.add(instance);
                }
            }
            
            buildAndTrainModel();
        } catch (Exception e) {
            System.err.println("KNN模型训练失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("KNN模型训练失败", e);
        }
    }
    
    /**
     * 使用每日绩效数据训练KNN模型
     */
    public void trainModelWithDailyData(List<DailyPerformance> dailyData) {
        try {
            System.out.println("开始训练KNN模型(使用每日数据)...");
            
            // 初始化属性
            initializeAttributes();
            
            // 创建训练数据集
            trainingInstances = new Instances("PerformanceTraining", attributes, 0);
            trainingInstances.setClassIndex(attributes.size() - 1);
            
            // 按员工和日期分组处理数据
            Map<String, Map<String, List<DailyPerformance>>> groupedData = dailyData.stream()
                .collect(Collectors.groupingBy(
                    DailyPerformance::getEmployeeId,
                    Collectors.groupingBy(dp -> dp.getRecordDate().toString().substring(0, 7)) // 按年月分组
                ));
            
            // 每个分组计算特征并添加到训练集
            for (Map.Entry<String, Map<String, List<DailyPerformance>>> employeeEntry : groupedData.entrySet()) {
                // 每个员工的每个月生成一个训练样本
                for (Map.Entry<String, List<DailyPerformance>> monthEntry : employeeEntry.getValue().entrySet()) {
                    List<DailyPerformance> records = monthEntry.getValue();
                    
                    if (records.isEmpty()) continue;
                    
                    // 计算该员工该月的各项指标
                    double attendanceRate = calculateAttendanceRate(records);
                    double kpiCompletion = calculateKpiCompletion(records);
                    double overtimeHours = calculateOvertimeHours(records);
                    
                    // 从历史数据中获取评分（如果有）
                    Integer performanceRating = determinePerformanceRating(records);
                    
                    // 只有有绩效评分的数据才加入训练集
                    if (performanceRating != null) {
                        Instance instance = createInstance(
                            attendanceRate,
                            kpiCompletion,
                            overtimeHours,
                            performanceRating
                        );
                        trainingInstances.add(instance);
                        
                        System.out.println("添加训练数据: 员工=" + employeeEntry.getKey() + 
                            ", 月份=" + monthEntry.getKey() + 
                            ", 出勤率=" + attendanceRate + 
                            ", KPI完成率=" + kpiCompletion + 
                            ", 加班时长=" + overtimeHours + 
                            ", 绩效评分=" + performanceRating);
                    }
                }
            }
            
            System.out.println("从每日数据生成了 " + trainingInstances.size() + " 条训练实例");
            
            buildAndTrainModel();
        } catch (Exception e) {
            System.err.println("使用每日数据训练KNN模型失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("KNN模型训练失败", e);
        }
    }
    
    /**
     * 构建并训练KNN模型
     */
    private void buildAndTrainModel() throws Exception {
        System.out.println("训练实例数: " + trainingInstances.size());
        
        if (trainingInstances.size() == 0) {
            throw new IllegalStateException("没有可用的训练数据");
        }
        
        // 配置KNN
        IBk knn = new IBk();
        
        // 配置KNN
        knn.setKNN(knnConfig.getK());
        
        // 配置距离度量
        EuclideanDistance distance = new EuclideanDistance();
        distance.setInstances(trainingInstances);
        
        // 设置属性索引和权重
        StringBuilder indices = new StringBuilder();
        for (int i = 0; i < trainingInstances.numAttributes() - 1; i++) {
            if (i > 0) indices.append(",");
            indices.append(i);
        }
        distance.setAttributeIndices(indices.toString());
        
        LinearNNSearch search = new LinearNNSearch();
        search.setDistanceFunction(distance);
        knn.setNearestNeighbourSearchAlgorithm(search);
        
        // 训练模型
        knn.buildClassifier(trainingInstances);
        knnClassifier = knn;
        
        System.out.println("KNN模型训练完成");
    }
    
    /**
     * 计算出勤率
     */
    private double calculateAttendanceRate(List<DailyPerformance> records) {
        double totalEffectiveHours = 0;
        double totalExpectedHours = 0;
        
        for (DailyPerformance record : records) {
            // 跳过周末
            int dayOfWeek = record.getRecordDate().getDayOfWeek().getValue();
            if (dayOfWeek > 5) continue; // 周末跳过
            
            // 每个工作日计入应该工作的时间
            totalExpectedHours += 8;
            
            if ("Y".equalsIgnoreCase(record.getAttendance())) {
                double lateEarlyMinutes = (record.getLateEarlyMinutes() != null ? record.getLateEarlyMinutes() : 0);
                double lateEarlyHours = lateEarlyMinutes / 60.0;
                double effectiveHours = Math.max(0, 8 - lateEarlyHours);
                totalEffectiveHours += effectiveHours;
            }
        }
        
        return totalExpectedHours > 0 ? (totalEffectiveHours / totalExpectedHours) * 100 : 0;
    }
    
    /**
     * 计算KPI完成率
     */
    private double calculateKpiCompletion(List<DailyPerformance> records) {
        int totalTasks = records.stream()
            .mapToInt(r -> r.getTotalTasks() != null ? r.getTotalTasks() : 0)
            .sum();
        int completedTasks = records.stream()
            .mapToInt(r -> r.getCompletedTasks() != null ? r.getCompletedTasks() : 0)
            .sum();
        
        return totalTasks > 0 ? ((double) completedTasks / totalTasks) * 100 : 0;
    }
    
    /**
     * 计算加班时长
     */
    private double calculateOvertimeHours(List<DailyPerformance> records) {
        return records.stream()
            .mapToDouble(r -> r.getOvertimeHours() != null ? r.getOvertimeHours() : 0)
            .sum();
    }
    
    /**
     * 确定绩效评分（从历史数据判断）
     * 这里需要实际业务逻辑来确定评分，暂时返回随机值用于示例
     */
    private Integer determinePerformanceRating(List<DailyPerformance> records) {
        // 简单逻辑：基于出勤率和KPI完成率的加权平均
        double attendanceRate = calculateAttendanceRate(records);
        double kpiCompletion = calculateKpiCompletion(records);
        
        // 这里假设我们已经有了一些绩效评分规则
        double score = (attendanceRate * 0.4) + (kpiCompletion * 0.6);
        
        // 将分数转换为1-3的评分
        if (score >= 85) {
            return 3; // 优
        } else if (score >= 70) {
            return 2; // 良
        } else if (score > 0) { // 至少有一些数据
            return 1; // 差
        } else {
            return null; // 没有足够信息
        }
    }
    
    /**
     * 预测员工绩效评分
     */
    public Integer predictPerformanceRating(double attendanceRate, double kpiCompletion, double overtimeHours) {
        if (knnClassifier == null || trainingInstances == null) {
            throw new IllegalStateException("KNN模型尚未训练，无法进行预测");
        }
        
        try {
            // 创建测试实例
            Instance testInstance = createInstance(attendanceRate, kpiCompletion, overtimeHours, null);
            testInstance.setDataset(trainingInstances);
            
            // 进行预测
            double prediction = knnClassifier.classifyInstance(testInstance);
            int predictedRating = (int) Math.round(prediction);
            
            // 确保评分在有效范围内 (1-3)
            predictedRating = Math.max(1, Math.min(3, predictedRating));
            
            System.out.println("预测结果: 出勤率=" + attendanceRate + 
                ", KPI完成率=" + kpiCompletion + 
                ", 加班时长=" + overtimeHours + 
                " => 绩效评分=" + predictedRating);
                
            return predictedRating;
        } catch (Exception e) {
            System.err.println("绩效评分预测失败: " + e.getMessage());
            e.printStackTrace();
            // 发生异常时返回默认评分
            return calculateDefaultRating(attendanceRate, kpiCompletion, overtimeHours);
        }
    }
    
    /**
     * 根据简单规则计算默认评分
     */
    private Integer calculateDefaultRating(double attendanceRate, double kpiCompletion, double overtimeHours) {
        // 简单规则：基于出勤率和KPI完成率的加权平均
        double score = (attendanceRate * 0.4) + (kpiCompletion * 0.6);
        
        // 将分数转换为1-3的评分
        if (score >= 85) {
            return 3; // 优
        } else if (score >= 70) {
            return 2; // 良
        } else {
            return 1; // 差
        }
    }
    
    /**
     * 初始化属性
     */
    private void initializeAttributes() {
        attributes = new ArrayList<>();
        
        // 添加特征属性
        attributes.add(new Attribute("attendanceRate"));
        attributes.add(new Attribute("kpiCompletion"));
        attributes.add(new Attribute("overtimeHours"));
        
        // 定义分类属性的可能值
        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("1"); // 差
        classValues.add("2"); // 良
        classValues.add("3"); // 优
        
        // 添加分类属性
        attributes.add(new Attribute("performanceRating", classValues));
    }
    
    /**
     * 创建实例
     */
    private Instance createInstance(Double attendanceRate, Double kpiCompletion, Double overtimeHours, Integer performanceRating) {
        Instance instance = new DenseInstance(attributes.size());
        
        // 设置特征值
        instance.setValue(0, attendanceRate != null ? attendanceRate : 0);
        instance.setValue(1, kpiCompletion != null ? kpiCompletion : 0);
        instance.setValue(2, overtimeHours != null ? overtimeHours : 0);
        
        // 如果提供了评分，设置为已知类别
        if (performanceRating != null) {
            instance.setValue(3, performanceRating.toString());
        }
        
        return instance;
    }
    
    /**
     * 更新KNN配置
     */
    public void updateConfig(KNNConfig config) {
        this.knnConfig = config;
        System.out.println("KNN配置已更新: K=" + config.getK() + 
            ", 出勤权重=" + config.getAttendanceWeight() + 
            ", KPI权重=" + config.getKpiWeight() + 
            ", 加班权重=" + config.getOvertimeWeight());
    }
}