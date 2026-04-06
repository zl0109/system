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
            // 部门专属通道：只查 admin 表
            Admin admin = adminRepository.findByUsernameAndPassword(loginDTO.getUsername(), loginDTO.getPassword());
            if (admin != null) {
                result.put("code", 200);
                result.put("msg", "部门账号登录成功");
                result.put("userId", admin.getAdminId());
                // 返回真实姓名和具体部门角色
                result.put("userName", admin.getName() != null ? admin.getName() : "部门领导");
                result.put("role", "admin");
                result.put("deptType", admin.getDeptType()); // 把具体是哪个部门也告诉前端
                result.put("position", admin.getPosition());
                return result;
            }
        }else if ("student".equals(loginDTO.getRole())) {
            //学生登录逻辑（前端传过来的 username 就是学号）
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

    //全角色通用的修改密码接口
    @PostMapping("/updatePassword")
    public Map<String, Object> updatePassword(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        Integer userId = (Integer) params.get("userId");
        String role = (String) params.get("role"); // 接收前端传来的角色标识
        String oldPassword = (String) params.get("oldPassword");
        String newPassword = (String) params.get("newPassword");

        boolean success = false;
        String msg = "原密码错误或系统异常";

        // 🛡️ 根据不同的角色，去不同的表里核对和修改密码
        if ("parent".equals(role)) {
            Parent parent = parentRepository.findById(userId).orElse(null);
            if (parent != null && parent.getPassword().equals(oldPassword)) {
                parent.setPassword(newPassword);
                parentRepository.save(parent);
                success = true;
            }
        } else if ("teacher".equals(role) || "headmaster".equals(role)) {
            // 普通老师和班主任都在 teacher 表里
            Teacher teacher = teacherRepository.findById(userId).orElse(null);
            if (teacher != null && teacher.getPassword().equals(oldPassword)) {
                teacher.setPassword(newPassword);
                teacherRepository.save(teacher);
                success = true;
            }
        } else if ("admin".equals(role)) {
            Admin admin = adminRepository.findById(userId).orElse(null);
            if (admin != null && admin.getPassword().equals(oldPassword)) {
                admin.setPassword(newPassword);
                adminRepository.save(admin);
                success = true;
            }
        } else if ("student".equals(role)) {
            Student student = studentRepository.findById(userId).orElse(null);
            if (student != null && student.getPassword().equals(oldPassword)) {
                student.setPassword(newPassword);
                studentRepository.save(student);
                success = true;
            }
        }

        // 返回结果
        if (success) {
            result.put("code", 200);
            result.put("msg", "密码修改成功，请重新登录！");
        } else {
            result.put("code", 400);
            result.put("msg", msg);
        }
        return result;
    }

}