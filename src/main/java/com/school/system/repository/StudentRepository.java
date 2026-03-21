package com.school.system.repository;

import com.school.system.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Integer> {
    //魔法方法1：登录用（按学号和密码找学生）
    Student findByStudentNoAndPassword(String studentNo, String password);

    //魔法方法2：搜索用（按姓名或学号模糊查找）
    List<Student> findByNameContainingOrStudentNoContaining(String name, String studentNo);

    //根据家长 ID 找出对应的学生
    List<Student> findByParentId(Integer parentId);
}