package com.school.system.dto;

import java.math.BigDecimal;
import java.util.Date;

// 这是一个接口，Spring Boot 会自动把连表查出来的数据塞到这里面
public interface ScoreDTO {
    Integer getScoreId();
    String getExamName();
    String getSubject();
    BigDecimal getScore();
    Date getEntryTime();

    // 关键点：这个就是我们千辛万苦要连表查出来的学生名字！
    String getStudentName();
}