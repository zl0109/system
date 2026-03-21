package com.school.system.repository;

import com.school.system.entity.Parent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParentRepository extends JpaRepository<Parent, Integer> {
    // 魔法方法：按照手机号和密码去数据库里找人
    Parent findByPhoneAndPassword(String phone, String password);
}