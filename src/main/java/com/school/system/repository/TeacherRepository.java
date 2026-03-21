package com.school.system.repository;

import com.school.system.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherRepository extends JpaRepository<Teacher, Integer> {
    Teacher findByPhoneAndPassword(String phone, String password);
}