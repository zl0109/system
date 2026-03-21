package com.school.system.controller;

import com.school.system.entity.Student;
import com.school.system.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/student") // 属于管理员的权限路径
@CrossOrigin
public class StudentController {

    @Autowired
    private StudentRepository studentRepository;

    @GetMapping("/list")
    public List<Student> getList() {
        return studentRepository.findAll();
    }

    @PostMapping("/save")
    public String save(@RequestBody Student student) {
        studentRepository.save(student);
        return "保存成功";
    }

    @DeleteMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        studentRepository.deleteById(id);
        return "删除成功";
    }

    //快捷查找学生接口
    @GetMapping("/search")
    public List<Student> search(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return studentRepository.findAll();
        }
        return studentRepository.findByNameContainingOrStudentNoContaining(keyword, keyword);
    }

    //新增：供家长端调用的接口，用来查自己的孩子
    @GetMapping("/findByParent")
    public List<Student> findByParent(@RequestParam Integer parentId) {
        return studentRepository.findByParentId(parentId);
    }

}