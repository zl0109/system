package com.school.system.repository;

import com.school.system.entity.FeeTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeeTaskRepository extends JpaRepository<FeeTask, Integer> {
}