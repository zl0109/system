package com.school.system.controller;

import com.school.system.entity.Notice;
import com.school.system.repository.NoticeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notice")
@CrossOrigin
public class NoticeController {

    @Autowired
    private NoticeRepository noticeRepository;

    @GetMapping("/list")
    public List<Notice> getList() {
        return noticeRepository.findAll();
    }

    @PostMapping("/save")
    public String save(@RequestBody Notice notice) {
        noticeRepository.save(notice);
        return "发布成功";
    }
}