package com.school.system.controller;

import com.school.system.repository.ClassInfoRepository;
import com.school.system.repository.ParentRepository;
import com.school.system.repository.StudentRepository;
import com.school.system.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/dashboard")
@CrossOrigin
public class DashboardController {

    @Autowired private ClassInfoRepository classInfoRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private ParentRepository parentRepository;

    //统计各表总人数的接口
    @GetMapping("/stats")
    public Map<String, Long> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("classCount", classInfoRepository.count());
        stats.put("teacherCount", teacherRepository.count());
        stats.put("studentCount", studentRepository.count());
        stats.put("parentCount", parentRepository.count());
        return stats;
    }
}