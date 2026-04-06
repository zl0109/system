package com.school.system.controller;

import com.school.system.entity.Notice;
import com.school.system.repository.NoticeRepository;
import com.school.system.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/notice")
@CrossOrigin
public class NoticeController {

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private TeacherRepository teacherRepository; // 引入老师数据库用来查科目

    @GetMapping("/list")
    public List<Notice> getList() {
        List<Notice> notices = noticeRepository.findAll();

        // 核心：遍历每一个通知/作业，翻译发布人的详细信息
        for (Notice n : notices) {
            if (n.getTeacherId() != null) {
                teacherRepository.findById(n.getTeacherId()).ifPresent(t -> {
                    // 填入老师姓名
                    n.setPublisherName(t.getName());
                    // 翻译角色
                    n.setPublisherRole("headmaster".equals(t.getRole()) ? "班主任" : "教师");
                    // 填入科目
                    n.setPublisherSubject(t.getSubject());
                });
            }
        }
        return notices;
    }

    @PostMapping("/save")
    public String save(@RequestBody Notice notice) {
        if (notice.getPublishTime() == null) {
            notice.setPublishTime(new Date());
        }

        //如果是发布“作业”，系统自动提取该老师的“教学科目”
        if ("作业".equals(notice.getType()) && notice.getTeacherId() != null) {
            teacherRepository.findById(notice.getTeacherId()).ifPresent(teacher -> {
                notice.setSubject(teacher.getSubject());
            });
        }

        noticeRepository.save(notice);
        return "保存成功";
    }

    @DeleteMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        noticeRepository.deleteById(id);
        return "删除成功";
    }
}