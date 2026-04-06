package com.school.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "`chat_message`")
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String sender;   // 发送人姓名
    private String content;  // 内容
    private Date sendTime;   // 发送时间

    private String chatType; // GROUP 或 PRIVATE
    private String targetId; // 群组(班级)ID，或者私聊对象的联合ID
    private String senderId; // 发送人的联合ID (如 student_1)
    //新增：已读状态
    private Integer isRead = 0;

    private String msgType = "TEXT";
}