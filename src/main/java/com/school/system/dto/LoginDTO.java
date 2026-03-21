package com.school.system.dto;

import lombok.Data;

@Data
public class LoginDTO {
    private String username; // 账号/手机号
    private String password; // 密码
    private String role;     // 角色 (admin/teacher/parent/student)
}