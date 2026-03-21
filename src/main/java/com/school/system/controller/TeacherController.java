package com.school.system.controller;

import com.school.system.entity.Teacher;
import com.school.system.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/teacher") // 注意路径加了 admin，代表管理员专属
@CrossOrigin
public class TeacherController {

    @Autowired
    private TeacherRepository teacherRepository;

    @GetMapping("/list")
    public List<Teacher> getList() {
        return teacherRepository.findAll();
    }

    @PostMapping("/save")
    public String save(@RequestBody Teacher teacher) {
        if (teacher.getRole() == null || teacher.getRole().isEmpty()) {
            teacher.setRole("teacher"); // 默认是普通老师
        }
        teacherRepository.save(teacher);
        return "保存成功";
    }

    @DeleteMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        teacherRepository.deleteById(id);
        return "删除成功";
    }
}