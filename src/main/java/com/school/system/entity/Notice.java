package com.school.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "`notice`")
public class Notice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer noticeId;

    private String type;         // 对应前端的 "通知" 或 "作业"
    private String title;        // 标题
    private String content;      // 内容
    private Date publishTime;    // 发布时间

    private Integer teacherId;   // 发布人的ID (老师或班主任)
    private Integer classId;     // 面向的班级ID (如果是校园通知可以为空)
    private String subject;

    // 仅用于前端显示的“隐身字段” (不会存入数据库)
    @Transient
    private String publisherName;    // 发布人姓名
    @Transient
    private String publisherRole;    // 发布人角色(班主任/普通教师)
    @Transient
    private String publisherSubject; // 发布人教授科目
}