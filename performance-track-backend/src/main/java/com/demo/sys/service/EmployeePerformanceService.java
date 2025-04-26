package com.demo.sys.service;

import com.demo.sys.entity.EmployeePerformance;
import com.demo.sys.repository.EmployeePerformanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeePerformanceService {

    @Autowired
    private EmployeePerformanceRepository employeePerformanceRepository;

    /**
     * 根据YearMonth对象搜索
     */
    public Page<EmployeePerformance> search(String employeeID, String employeeName, YearMonth recordDate, Pageable pageable) {
        return employeePerformanceRepository.search(employeeID, employeeName, recordDate, pageable);
    }
    
    /**
     * 使用日期前缀字符串进行搜索
     */
    public Page<EmployeePerformance> searchWithDatePrefix(String employeeID, String employeeName, String datePrefix, Pageable pageable) {
        return employeePerformanceRepository.searchWithDatePrefix(employeeID, employeeName, datePrefix, pageable);
    }
    
    /**
     * 计算所有记录数量
     */
    public long countAll() {
        return employeePerformanceRepository.count();
    }
    
    /**
     * 获取所有不同的日期
     */
    public List<String> findAllDistinctDates() {
        // 获取所有记录
        List<EmployeePerformance> allRecords = employeePerformanceRepository.findAll();
        
        // 提取所有不同的日期
        return allRecords.stream()
            .map(record -> {
                if (record.getRecordDate() != null) {
                    return record.getRecordDate().toString();
                }
                return "null";
            })
            .distinct()
            .collect(Collectors.toList());
    }
    
    /**
     * 根据年月查询记录
     */
    public List<EmployeePerformance> findByYearAndMonth(int year, int month) {
        return employeePerformanceRepository.findByYearAndMonth(year, month);
    }
    
    /**
     * 根据YearMonth查询记录
     */
    public List<EmployeePerformance> findByRecordDate(YearMonth yearMonth) {
        return employeePerformanceRepository.findByRecordDate(yearMonth);
    }
    
    /**
     * 检查特定年月的数据是否已存在
     * @param yearMonth 年月
     * @return 如果存在记录则返回true，否则返回false
     */
    public boolean hasDataForYearMonth(YearMonth yearMonth) {
        List<EmployeePerformance> existingRecords = employeePerformanceRepository.findByRecordDate(yearMonth);
        return !existingRecords.isEmpty();
    }
    
    /**
     * 检查特定年月的数据是否已存在
     * @param year 年
     * @param month 月
     * @return 如果存在记录则返回true，否则返回false
     */
    public boolean hasDataForYearMonth(int year, int month) {
        List<EmployeePerformance> existingRecords = employeePerformanceRepository.findByYearAndMonth(year, month);
        return !existingRecords.isEmpty();
    }
} 