package com.school.system.controller;

import com.school.system.dto.LoginDTO;
import com.school.system.entity.Admin;
import com.school.system.entity.Parent;
import com.school.system.entity.Teacher;
import com.school.system.entity.Student;
import com.school.system.entity.ClassInfo;
import com.school.system.repository.AdminRepository;
import com.school.system.repository.ParentRepository;
import com.school.system.repository.TeacherRepository;
import com.school.system.repository.StudentRepository;
import com.school.system.repository.ClassInfoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
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
    private AdminRepository adminRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private ClassInfoRepository classInfoRepository;

    //1. 多角色聚合登录接口
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginDTO loginDTO) {
        Map<String, Object> result = new HashMap<>();

        if ("parent".equals(loginDTO.getRole())) {
            Parent parent = parentRepository.findByPhoneAndPassword(loginDTO.getUsername(), loginDTO.getPassword());
            if (parent != null) {
                result.put("code", 200); result.put("msg", "家长登录成功");
                result.put("userId", parent.getParentId());
                result.put("userName", parent.getName());
                result.put("role", "parent");

                //查找该家长关联的孩子，获取孩子的班级ID给家长
                List<Student> children = studentRepository.findByParentId(parent.getParentId());
                if (children != null && !children.isEmpty()) {
                    result.put("classId", children.get(0).getClassId());
                }
                return result;
            }
        } else if ("teacher".equals(loginDTO.getRole())) {
            Teacher teacher = teacherRepository.findByPhoneAndPassword(loginDTO.getUsername(), loginDTO.getPassword());
            if (teacher != null) {
                result.put("code", 200); result.put("msg", "教师登录成功");
                result.put("userId", teacher.getTeacherId());
                result.put("userName", teacher.getName());

                String role = teacher.getRole() != null ? teacher.getRole() : "teacher";
                result.put("role", role);

                //如果他是班主任，查出他负责的班级ID
                if ("headmaster".equals(role)) {
                    ClassInfo classInfo = classInfoRepository.findByHeadmasterId(teacher.getTeacherId());
                    if (classInfo != null) {
                        result.put("classId", classInfo.getClassId());
                    }
                }
                return result;
            }
        } else if ("admin".equals(loginDTO.getRole())) {
            Admin admin = adminRepository.findByUsernameAndPassword(loginDTO.getUsername(), loginDTO.getPassword());
            if (admin != null) {
                result.put("code", 200);
                result.put("msg", "部门账号登录成功");
                result.put("userId", admin.getAdminId());
                result.put("userName", admin.getName() != null ? admin.getName() : "部门领导");
                result.put("role", "admin");
                result.put("deptType", admin.getDeptType());
                result.put("position", admin.getPosition());
                return result;
            }
        }else if ("student".equals(loginDTO.getRole())) {
            Student student = studentRepository.findByStudentNoAndPassword(loginDTO.getUsername(), loginDTO.getPassword());
            if (student != null) {
                result.put("code", 200); result.put("msg", "学生登录成功");
                result.put("userId", student.getStudentId());
                result.put("userName", student.getName());
                result.put("role", "student");

                //直接从学生表里获取班级ID
                result.put("classId", student.getClassId());
                return result;
            }
        }

        result.put("code", 400); result.put("msg", "账号、密码或身份选择错误");
        return result;
    }

    //2. 家长自助注册与动态绑定接口
    @PostMapping("/register")
    public Map<String, Object> registerParent(@RequestBody Map<String, String> payload) {
        Map<String, Object> result = new HashMap<>();

        String phone = payload.get("phone");
        String password = payload.get("password");
        String parentName = payload.get("parentName");
        String relation = payload.get("relation");
        String studentNo = payload.get("studentNo");
        String studentName = payload.get("studentName");

        // 1. 唯一性校验：手机号是否已被注册
        if (parentRepository.findByPhone(phone) != null) {
            result.put("code", 400);
            result.put("msg", "该手机号已被注册，请直接登录或找回密码");
            return result;
        }

        // 2. 安全身份校验：验证学号和姓名是否匹配
        Student student = studentRepository.findByStudentNoAndName(studentNo, studentName);
        if (student == null) {
            result.put("code", 404);
            result.put("msg", "验证失败：未找到该学生，请检查学号与姓名是否填写正确");
            return result;
        }

        // 3. 防拐卖机制：防止一个孩子被多个账号重复绑定
        if (student.getParentId() != null) {
            result.put("code", 403);
            result.put("msg", "该学生已绑定过家长账号，如需修改请联系班主任");
            return result;
        }

        try {
            // 4. 初始化并保存家长信息
            Parent parent = new Parent();
            parent.setPhone(phone);
            parent.setPassword(password);
            parent.setName(parentName);
            parent.setRelation(relation);

            // 先保存家长，获取自增的主键 ID
            Parent savedParent = parentRepository.save(parent);

            // 5. 将新生成的家长ID，反向写回学生表中，完成物理绑定
            student.setParentId(savedParent.getParentId());
            studentRepository.save(student);

            result.put("code", 200);
            result.put("msg", "注册并绑定成功！");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("msg", "系统异常，注册失败");
        }

        return result;
    }

    //3. 全角色通用的修改密码接口
    @PostMapping("/updatePassword")
    public Map<String, Object> updatePassword(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        Integer userId = (Integer) params.get("userId");
        String role = (String) params.get("role");
        String oldPassword = (String) params.get("oldPassword");
        String newPassword = (String) params.get("newPassword");

        boolean success = false;
        String msg = "原密码错误或系统异常";

        if ("parent".equals(role)) {
            Parent parent = parentRepository.findById(userId).orElse(null);
            if (parent != null && parent.getPassword().equals(oldPassword)) {
                parent.setPassword(newPassword);
                parentRepository.save(parent);
                success = true;
            }
        } else if ("teacher".equals(role) || "headmaster".equals(role)) {
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