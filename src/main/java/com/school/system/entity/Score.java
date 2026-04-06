package com.school.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "`score`")
public class Score {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer scoreId;

    private String examName;    // 考试名称
    private String subject;     // 科目
    private String studentName; // 学生姓名
    private Double score;       // 分数
    private Date createTime;    // 录入时间
}