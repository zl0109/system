package com.school.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "`leave`")
public class LeaveRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer leaveId;

    private Integer studentId;
    private String reason;
    private Date startTime;
    private Date endTime;
    private Integer status;
    private Integer approverId;

    //魔法隐身字段：数据库里没有，但返回给前端时临时带上
    @Transient
    private String studentName;
    @Transient
    private String approverName;
}