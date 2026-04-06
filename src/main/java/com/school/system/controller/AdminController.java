package com.school.system.controller;

import com.school.system.entity.Admin;
import com.school.system.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/dept")
@CrossOrigin
public class AdminController {

    @Autowired
    private AdminRepository adminRepository;

    @GetMapping("/list")
    public List<Admin> getList() {
        return adminRepository.findAll();
    }

    @PostMapping("/save")
    public String save(@RequestBody Admin admin) {
        adminRepository.save(admin);
        return "保存成功";
    }

    @DeleteMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        adminRepository.deleteById(id);
        return "删除成功";
    }
}