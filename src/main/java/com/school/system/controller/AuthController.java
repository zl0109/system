package com.school.system.controller;

import com.school.system.dto.LoginDTO;
import com.school.system.entity.Admin;
import com.school.system.entity.Parent;
import com.school.system.entity.Teacher;
import com.school.system.repository.AdminRepository;
import com.school.system.repository.ParentRepository;
import com.school.system.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.school.system.entity.Student;
import com.school.system.repository.StudentRepository;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private ParentRepository parentRepository;
    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private AdminRepository adminRepository; //引入管理员工具
    @Autowired
    private StudentRepository studentRepository;//把学生查询工具注进来

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginDTO loginDTO) {
        Map<String, Object> result = new HashMap<>();

        if ("parent".equals(loginDTO.getRole())) {
            Parent parent = parentRepository.findByPhoneAndPassword(loginDTO.getUsername(), loginDTO.getPassword());
            if (parent != null) {
                result.put("code", 200); result.put("msg", "家长登录成功");
                result.put("userId", parent.getParentId()); result.put("userName", parent.getName()); result.put("role", "parent");
                return result;
            }
        } else if ("teacher".equals(loginDTO.getRole())) {
            Teacher teacher = teacherRepository.findByPhoneAndPassword(loginDTO.getUsername(), loginDTO.getPassword());
            if (teacher != null) {
                result.put("code", 200); result.put("msg", "教师登录成功");
                result.put("userId", teacher.getTeacherId()); result.put("userName", teacher.getName());
                result.put("role", teacher.getRole() != null ? teacher.getRole() : "teacher");
                return result;
            }
        } else if ("admin".equals(loginDTO.getRole())) {
            // 1. 先查是不是真正的超级管理员 admin
            Admin admin = adminRepository.findByUsernameAndPassword(loginDTO.getUsername(), loginDTO.getPassword());
            if (admin != null) {
                result.put("code", 200); result.put("msg", "超级管理员登录成功");
                result.put("userId", admin.getAdminId()); result.put("userName", "超级管理员"); result.put("role", "admin");
                return result;
            }

            // 🌟 2. 如果不是 admin，再去查查是不是“班主任”走这扇门进来了！
            Teacher headmaster = teacherRepository.findByPhoneAndPassword(loginDTO.getUsername(), loginDTO.getPassword());
            if (headmaster != null && "headmaster".equals(headmaster.getRole())) {
                result.put("code", 200); result.put("msg", "班主任进入管理系统");
                result.put("userId", headmaster.getTeacherId());
                result.put("userName", headmaster.getName());
                result.put("role", "headmaster"); // 真实身份依然保留
                return result;
            }
        }else if ("student".equals(loginDTO.getRole())) {
            // 🌟 新增：学生登录逻辑（前端传过来的 username 就是学号）
            Student student = studentRepository.findByStudentNoAndPassword(loginDTO.getUsername(), loginDTO.getPassword());
            if (student != null) {
                result.put("code", 200); result.put("msg", "学生登录成功");
                result.put("userId", student.getStudentId());
                result.put("userName", student.getName());
                result.put("role", "student"); // 标记为学生
                return result;
            }
        }

        result.put("code", 400); result.put("msg", "账号、密码或身份选择错误");
        return result;
    }
}