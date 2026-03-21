package com.school.system.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "`student`")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer studentId;

    private String studentNo; // 学号
    private String name;      // 姓名
    private Integer classId;  // 班级ID
    private Integer parentId; // 家长ID (暂时可以为空，等写家长管理时再绑定)
    private String password;
}