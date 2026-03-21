package com.school.system.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController // 这个注解告诉 Spring Boot：这个类是用来接收前端请求的
@CrossOrigin    // 这个注解非常关键！它允许前端（5173端口）跨域访问后端（8080端口）
public class TestController {

    // 当有人访问 http://localhost:8080/hello 时，就会执行这个方法
    @GetMapping("/hello")
    public String sayHello() {
        return "恭喜你！家校通系统后端接口测试成功！";
    }
}