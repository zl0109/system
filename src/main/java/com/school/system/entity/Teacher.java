package com.school.system.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "`teacher`")
public class Teacher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer teacherId;
    private String name;
    private String phone;
    private String password;
    private String role;
    private String subject;
}