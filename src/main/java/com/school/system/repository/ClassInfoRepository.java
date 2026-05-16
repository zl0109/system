package com.school.system.repository;

import com.school.system.entity.ClassInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassInfoRepository extends JpaRepository<ClassInfo, Integer> {
    ClassInfo findByHeadmasterId(Integer headmasterId);
}