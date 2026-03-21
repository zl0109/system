package com.school.system.repository;

import com.school.system.entity.LeaveRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveRepository extends JpaRepository<LeaveRecord, Integer> {
}