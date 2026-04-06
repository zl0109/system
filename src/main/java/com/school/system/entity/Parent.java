package com.school.system.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "`parent`")
public class Parent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer parentId;
    private String name;
    private String phone;
    private String password;
    private String relation;
}