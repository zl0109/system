package com.school.system.repository;

import com.school.system.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Integer> {
    //登录用（按学号和密码找学生）
    Student findByStudentNoAndPassword(String studentNo, String password);

    //搜索用（按姓名或学号模糊查找）
    List<Student> findByNameContainingOrStudentNoContaining(String name, String studentNo);

    //根据家长 ID 找出对应的学生
    List<Student> findByParentId(Integer parentId);
    //根据班级ID查出该班所有的学生 (用来统计群人数)
    List<Student> findByClassId(Integer classId);

    // 根据学号和姓名同时查找学生（用于安全校验孩子身份）
    Student findByStudentNoAndName(String studentNo, String name);
}