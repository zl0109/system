package com.school.system.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "`class_info`")
public class ClassInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer classId;
    private String className;
    private Integer headmasterId; // 班主任ID

    //存放多名任课老师的ID
    private String teacherIds;
}