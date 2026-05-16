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
    public List<Notice> getList(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Integer userId
    ) {
        List<Notice> allNotices = noticeRepository.findAll();
        java.util.List<Notice> filteredNotices = new java.util.ArrayList<>();

        for (Notice n : allNotices) {
            //1：作业绝对不允许跨班广播！只有“通知”且 classId 为空时，才是全校公共通知。
            boolean isPublic = n.getClassId() == null && "通知".equals(n.getType());

            // 规则2：本班放行（当前登录学生的班级ID 必须与 作业绑定的班级ID 一致）
            boolean isSameClass = classId != null && classId.equals(n.getClassId());

            // 规则3：发布者放行（自己发的作业自己永远可见，便于后续删除编辑）
            boolean isPublisher = userId != null && userId.equals(n.getTeacherId());

            // 规则4：上帝视角放行（最高行政权限可查看全校所有班级数据）
            boolean isAdmin = "admin".equals(role) || "department".equals(role) || "leadership".equals(role);

            // 满足其一即可放行
            if (isPublic || isSameClass || isPublisher || isAdmin) {
                if (n.getTeacherId() != null) {
                    teacherRepository.findById(n.getTeacherId()).ifPresent(t -> {
                        n.setPublisherName(t.getName());
                        n.setPublisherRole("headmaster".equals(t.getRole()) ? "班主任" : "教师");
                        n.setPublisherSubject(t.getSubject());
                    });
                }
                filteredNotices.add(n);
            }
        }

        filteredNotices.sort((n1, n2) -> {
            if (n1.getPublishTime() == null || n2.getPublishTime() == null) return 0;
            return n2.getPublishTime().compareTo(n1.getPublishTime());
        });

        return filteredNotices;
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