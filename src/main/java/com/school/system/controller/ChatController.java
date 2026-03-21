package com.school.system.controller;

import com.school.system.entity.ChatMessage;
import com.school.system.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
@CrossOrigin
public class ChatController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    // 前端一进页面，就调这个接口拿以前所有的聊天记录
    @GetMapping("/history")
    public List<ChatMessage> getHistory() {
        return chatMessageRepository.findAll();
    }
}