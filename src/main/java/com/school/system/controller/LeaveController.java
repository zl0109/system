package com.school.system.controller;

import com.school.system.entity.LeaveRecord;
import com.school.system.entity.Student;
import com.school.system.repository.LeaveRepository;
import com.school.system.repository.StudentRepository;
import com.school.system.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/leave")
@CrossOrigin
public class LeaveController {

    @Autowired
    private LeaveRepository leaveRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private TeacherRepository teacherRepository;

    // 🌟 核心升级：接收前端传来的角色和用户ID，进行数据隔离
    @GetMapping("/list")
    public List<LeaveRecord> getList(@RequestParam(defaultValue = "") String role,
                                     @RequestParam(defaultValue = "0") Integer userId) {
        // 先查出所有的请假单
        List<LeaveRecord> allLeaves = leaveRepository.findAll();

        // 🛡️ 安全拦截：如果是家长，执行数据隔离逻辑
        if ("parent".equals(role)) {
            // 1. 查出这个家长绑定的所有孩子
            List<Student> children = studentRepository.findByParentId(userId);
            List<Integer> childIds = new ArrayList<>();
            for (Student child : children) {
                childIds.add(child.getStudentId());
            }

            // 2. 只保留这些孩子的请假单
            List<LeaveRecord> filteredLeaves = new ArrayList<>();
            for (LeaveRecord leave : allLeaves) {
                if (childIds.contains(leave.getStudentId())) {
                    filteredLeaves.add(leave);
                }
            }
            allLeaves = filteredLeaves; // 替换结果集
        }

        // 🌟 翻译名字 (老师依然能看到所有的，家长只能看到自己的)
        for (LeaveRecord leave : allLeaves) {
            if (leave.getStudentId() != null) {
                studentRepository.findById(leave.getStudentId())
                        .ifPresent(student -> leave.setStudentName(student.getName()));
            }
            if (leave.getApproverId() != null) {
                teacherRepository.findById(leave.getApproverId())
                        .ifPresent(teacher -> leave.setApproverName(teacher.getName()));
            }
        }
        return allLeaves;
    }

    @PostMapping("/save")
    public String save(@RequestBody LeaveRecord leave) {
        leave.setStatus(0);
        leaveRepository.save(leave);
        return "提交成功";
    }

    @PostMapping("/approve")
    public String approve(@RequestBody Map<String, Object> params) {
        Integer leaveId = (Integer) params.get("leaveId");
        Integer status = (Integer) params.get("status");
        Integer approverId = (Integer) params.get("approverId");

        LeaveRecord leave = leaveRepository.findById(leaveId).orElse(null);
        if (leave != null) {
            leave.setStatus(status);
            leave.setApproverId(approverId);
            leaveRepository.save(leave);
            return "审批完成";
        }
        return "找不到请假单";
    }
}