package com.demo.sys.repository;

import com.demo.sys.entity.DailyPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyPerformanceRepository extends JpaRepository<DailyPerformance, Long> {
    List<DailyPerformance> findByRecordDateBetween(LocalDate startDate, LocalDate endDate);
} 