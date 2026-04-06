package com.school.system.repository;

import com.school.system.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {
    //专门用来找出两个人之间的“未读消息”
    List<ChatMessage> findBySenderIdAndTargetIdAndIsRead(String senderId, String targetId, Integer isRead);
}