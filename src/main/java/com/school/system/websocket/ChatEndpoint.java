package com.school.system.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.system.entity.ChatMessage;
import com.school.system.repository.ChatMessageRepository;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//URL 升级：现在使用 身份_ID 作为唯一标识连入 (例如: student_1)
@ServerEndpoint("/chat/{userId}")
@Component
public class ChatEndpoint {

    private static final Map<String, Session> onlineUsers = new ConcurrentHashMap<>();
    private static ChatMessageRepository chatMessageRepository;
    private static final ObjectMapper objectMapper = new ObjectMapper(); // 用来解析前端发来的 JSON

    private String userId;

    @Autowired
    public void setChatMessageRepository(ChatMessageRepository repo) {
        ChatEndpoint.chatMessageRepository = repo;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        this.userId = userId;
        onlineUsers.put(userId, session);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            Map<String, String> msgData = objectMapper.readValue(message, Map.class);

            //  1. 判断这是一条什么类型的消息？(聊天 还是 已读回执)
            String type = msgData.getOrDefault("type", "CHAT");

            // 处理隐形的“已读回执”指令
            if ("READ_ACK".equals(type)) {
                String targetId = msgData.get("targetId"); // 谁发出的消息被读了 (也就是对面的发送者)
                String readerId = msgData.get("senderId"); // 谁点开的屏幕 (当前读信人)

                // 去数据库里把这些未读消息翻出来，强行盖上“已读(1)”的章
                List<ChatMessage> unreadMsgs = chatMessageRepository.findBySenderIdAndTargetIdAndIsRead(targetId, readerId, 0);
                if (!unreadMsgs.isEmpty()) {
                    for (ChatMessage m : unreadMsgs) {
                        m.setIsRead(1);
                    }
                    chatMessageRepository.saveAll(unreadMsgs);
                }

                // 核心：把这个“回执”静悄悄地通过 WebSocket 传给原发信人，让他的屏幕变成“已读”！
                Session targetSession = onlineUsers.get(targetId);
                if (targetSession != null) {
                    targetSession.getBasicRemote().sendText(message);
                }
                return; // 回执处理完毕，直接结束！
            }

            //  处理正常的聊天消息
            String chatType = msgData.get("chatType");
            String targetId = msgData.get("targetId");

            ChatMessage chatMsg = new ChatMessage();
            chatMsg.setChatType(chatType);
            chatMsg.setTargetId(targetId);
            chatMsg.setSenderId(this.userId);
            chatMsg.setSender(msgData.get("senderName"));
            chatMsg.setContent(msgData.get("content"));
            //记录这是一段文字还是图片URL
            chatMsg.setMsgType(msgData.getOrDefault("msgType", "TEXT"));
            chatMsg.setSendTime(new Date());
            chatMsg.setIsRead(0); // 刚发出的消息绝对是未读(0)
            chatMessageRepository.save(chatMsg);

            // 智能路由分发
            if ("PRIVATE".equals(chatType)) {
                Session targetSession = onlineUsers.get(targetId);
                if (targetSession != null) {
                    targetSession.getBasicRemote().sendText(message);
                }
                if (!this.userId.equals(targetId)) {
                    session.getBasicRemote().sendText(message);
                }
            } else {
                for (Session s : onlineUsers.values()) {
                    s.getBasicRemote().sendText(message);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session) {
        onlineUsers.remove(userId);
    }
}