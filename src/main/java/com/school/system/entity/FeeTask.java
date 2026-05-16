package com.school.system.entity;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "fee_task")
public class FeeTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String title;
    private Double amount;
    private Integer classId;
    private Integer initiatorId;
    private Date createTime;
    private Integer status; // 0:收取中 1:已结束

    // Getters and Setters (请自行使用 IDEA 生成或者使用 @Data 注解)
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public Integer getClassId() { return classId; }
    public void setClassId(Integer classId) { this.classId = classId; }
    public Integer getInitiatorId() { return initiatorId; }
    public void setInitiatorId(Integer initiatorId) { this.initiatorId = initiatorId; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}