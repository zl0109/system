package com.school.system.controller;

import com.school.system.entity.Parent;
import com.school.system.repository.ParentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/parent") // 属于管理员的权限路径
@CrossOrigin
public class ParentController {

    @Autowired
    private ParentRepository parentRepository;

    @GetMapping("/list")
    public List<Parent> getList() {
        return parentRepository.findAll();
    }

    @PostMapping("/save")
    public String save(@RequestBody Parent parent) {
        parentRepository.save(parent);
        return "保存成功";
    }

    @DeleteMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        parentRepository.deleteById(id);
        return "删除成功";
    }
}