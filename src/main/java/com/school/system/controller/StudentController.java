package com.school.system.controller;

import com.school.system.entity.Student;
import com.school.system.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.school.system.repository.ClassInfoRepository;
import com.school.system.repository.ParentRepository;

import java.util.List;

@RestController
@RequestMapping("/admin/student") // 属于管理员的权限路径
@CrossOrigin
public class StudentController {

    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private ClassInfoRepository classInfoRepository; // 查班级名
    @Autowired
    private ParentRepository parentRepository;       // 查家长名

    @GetMapping("/list")
    public List<Student> getList() {
        List<Student> students = studentRepository.findAll();

        // 遍历每一个学生，把ID翻译成中文名
        for (Student s : students) {
            if (s.getClassId() != null) {
                classInfoRepository.findById(s.getClassId())
                        .ifPresent(c -> s.setClassName(c.getClassName()));
            }
            if (s.getParentId() != null) {
                parentRepository.findById(s.getParentId())
                        .ifPresent(p -> {
                            // 拼出李四(爸爸)
                            String rel = p.getRelation() != null ? p.getRelation() : "家长";
                            s.setParentName(p.getName() + " (" + rel + ")");
                        });
            }
        }
        return students;
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