package com.school.system.controller;

import com.school.system.entity.ClassInfo;
import com.school.system.repository.ClassInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/class")
@CrossOrigin
public class ClassInfoController {

    @Autowired
    private ClassInfoRepository classInfoRepository;

    @GetMapping("/list")
    public List<ClassInfo> getList() {
        return classInfoRepository.findAll();
    }

    @PostMapping("/save")
    public String save(@RequestBody ClassInfo classInfo) {
        classInfoRepository.save(classInfo);
        return "保存成功";
    }

    @DeleteMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        classInfoRepository.deleteById(id);
        return "删除成功";
    }
}