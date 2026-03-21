package com.school.system.websocket;

import com.school.system.entity.ChatMessage;
import com.school.system.repository.ChatMessageRepository;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/chat/{username}")
@Component
public class ChatEndpoint {

    private static final Map<String, Session> onlineUsers = new ConcurrentHashMap<>();
    private String username;

    // 🌟 高级魔法：用 static 关键字配合 setter 方法，强行注入数据库工具！
    private static ChatMessageRepository chatMessageRepository;

    @Autowired
    public void setChatMessageRepository(ChatMessageRepository repo) {
        ChatEndpoint.chatMessageRepository = repo;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("username") String username) {
        this.username = username;
        onlineUsers.put(username, session);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // 🌟 新增逻辑：收到消息后，第一时间偷偷存进 MySQL 数据库！
        ChatMessage chatMsg = new ChatMessage();
        chatMsg.setSender(username);
        chatMsg.setContent(message);
        chatMsg.setSendTime(new Date());
        chatMessageRepository.save(chatMsg);

        // 然后再像以前一样，广播给所有人
        broadcast(username, message);
    }

    @OnClose
    public void onClose(Session session) {
        onlineUsers.remove(username);
    }

    private void broadcast(String sender, String text) {
        String jsonMsg = String.format("{\"from\":\"%s\", \"text\":\"%s\"}", sender, text);
        for (Session s : onlineUsers.values()) {
            try {
                s.getBasicRemote().sendText(jsonMsg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}