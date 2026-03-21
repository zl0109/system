package com.school.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "`notice`")
public class Notice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer noticeId;

    private String title;       // 标题
    private String content;     // 内容 (你的数据库里有这个)
    private Date publishTime;   // 发布时间
    private Integer teacherId;  // 发布教师的 ID
    private Integer classId;    // 接收班级的 ID
    private Integer noticeType; // 通知类型 (tinyint 用 Integer 接收即可)
}