package com.school.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "`admin`")
public class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer adminId;

    private String username;
    private String password;
    private String name;
    private String deptType; // academic教务处, center管理中心

    // 职位身份
    private String position;

    private Date createTime;
}