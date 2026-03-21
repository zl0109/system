package com.school.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Entity
@Table(name = "`score`") // 对应数据库里名叫 score 的表
public class Score {

    @Id // 告诉 JPA 这是主键
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 主键自增
    private Integer scoreId;

    private Integer studentId;
    private String examName;
    private String subject;
    private BigDecimal score;
    private Date entryTime;
}