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

    //接收角色、用户ID、班级ID，进行三维数据物理隔离
    @GetMapping("/list")
    public List<LeaveRecord> getList(@RequestParam(defaultValue = "") String role,
                                     @RequestParam(defaultValue = "0") Integer userId,
                                     @RequestParam(required = false) Integer classId) {
        // 先查出所有的请假单
        List<LeaveRecord> allLeaves = leaveRepository.findAll();
        List<LeaveRecord> filteredLeaves = new ArrayList<>();

        //安全拦截1：家长只能看自己绑定的孩子
        if ("parent".equals(role)) {
            List<Student> children = studentRepository.findByParentId(userId);
            List<Integer> childIds = new ArrayList<>();
            for (Student child : children) {
                childIds.add(child.getStudentId());
            }
            for (LeaveRecord leave : allLeaves) {
                if (childIds.contains(leave.getStudentId())) {
                    filteredLeaves.add(leave);
                }
            }
            allLeaves = filteredLeaves;
        }
        //安全拦截2：学生本人只能看自己的
        else if ("student".equals(role)) {
            for (LeaveRecord leave : allLeaves) {
                if (userId.equals(leave.getStudentId())) {
                    filteredLeaves.add(leave);
                }
            }
            allLeaves = filteredLeaves;
        }
        //安全拦截3：班主任只能看自己所管辖班级的学生
        else if ("headmaster".equals(role)) {
            if (classId != null) {
                List<Student> classStudents = studentRepository.findByClassId(classId);
                List<Integer> classStudentIds = new ArrayList<>();
                for (Student s : classStudents) {
                    classStudentIds.add(s.getStudentId());
                }
                for (LeaveRecord leave : allLeaves) {
                    if (classStudentIds.contains(leave.getStudentId())) {
                        filteredLeaves.add(leave);
                    }
                }
                allLeaves = filteredLeaves;
            } else {
                allLeaves = new ArrayList<>(); // 没传班级ID直接返回空，防止越权
            }
        }
        // 如果是 admin 管理员等最高权限，默认不进拦截器，可查看全校数据

        //翻译名字
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