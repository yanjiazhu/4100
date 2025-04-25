package com.demo.sys.repository;

import com.demo.sys.entity.EmployeePerformance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.YearMonth;
import java.util.List;

@Repository
public interface EmployeePerformanceRepository extends JpaRepository<EmployeePerformance, Long> {
    
    @Query("SELECT e FROM EmployeePerformance e WHERE " +
           "(:employeeID IS NULL OR :employeeID = '' OR e.employeeID = :employeeID) AND " +
           "(:employeeName IS NULL OR :employeeName = '' OR e.employeeName LIKE %:employeeName%) AND " +
           "(:recordDate IS NULL OR e.recordDate = :recordDate)")
    Page<EmployeePerformance> search(
            @Param("employeeID") String employeeID,
            @Param("employeeName") String employeeName,
            @Param("recordDate") YearMonth recordDate,
            Pageable pageable);
    
    /**
     * Query using a date string prefix
     * * Perform fuzzy matching by directly querying the string representation of the record_date field
     */
    @Query(value = "SELECT e.* FROM employee_performance e WHERE " +
           "(:employeeID IS NULL OR :employeeID = '' OR e.employee_id = :employeeID) AND " +
           "(:employeeName IS NULL OR :employeeName = '' OR e.employee_name LIKE CONCAT('%', :employeeName, '%')) AND " +
           "(:datePrefix IS NULL OR :datePrefix = '' OR CAST(e.record_date AS VARCHAR) LIKE CONCAT(:datePrefix, '%'))", 
           nativeQuery = true)
    Page<EmployeePerformance> searchWithDatePrefix(
            @Param("employeeID") String employeeID,
            @Param("employeeName") String employeeName,
            @Param("datePrefix") String datePrefix,
            Pageable pageable);
    
    /**
     * Search records by year and month
     */
    @Query(value = "SELECT * FROM employee_performance WHERE " +
           "EXTRACT(YEAR FROM record_date) = :year AND " +
           "EXTRACT(MONTH FROM record_date) = :month", 
           nativeQuery = true)
    List<EmployeePerformance> findByYearAndMonth(
            @Param("year") int year, 
            @Param("month") int month);

    List<EmployeePerformance> findByRecordDate(YearMonth recordDate);
    
    /**
     * Find all records with existing performance ratings
     */
    List<EmployeePerformance> findByPerformanceRatingIsNotNull();
} 