package com.school.system.controller;

import com.school.system.entity.*;
import com.school.system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/fee")
@CrossOrigin
public class FeeController {

    @Autowired private FeeTaskRepository feeTaskRepository;
    @Autowired private FeeRecordRepository feeRecordRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private ChatMessageRepository chatMessageRepository; //注入消息接口
    @Autowired private ParentRepository parentRepository; //注入家长表的操作接口

    // 1. 班主任发起收款
    @PostMapping("/create")
    public Map<String, Object> createFeeTask(@RequestBody FeeTask feeTask) {
        Map<String, Object> result = new HashMap<>();

        //保存缴费任务主表
        feeTask.setCreateTime(new Date());
        feeTask.setStatus(0);
        FeeTask savedTask = feeTaskRepository.save(feeTask);

        //找出该班级所有的学生，并为他们生成一条“未缴费”的明细记录
        List<Student> students = studentRepository.findByClassId(feeTask.getClassId());
        int recordCount = 0;

        for (Student s : students) {
            // 只给绑定了家长的学生生成账单，没绑定的后续补
            if (s.getParentId() != null) {
                FeeRecord record = new FeeRecord();
                record.setTaskId(savedTask.getId());
                record.setParentId(s.getParentId());
                record.setStudentId(s.getStudentId());
                record.setPayStatus(0); // 初始化为未交
                feeRecordRepository.save(record);
                recordCount++;
            }
        }

        result.put("code", 200);
        result.put("taskId", savedTask.getId());
        result.put("msg", "成功发起了收款，共生成了 " + recordCount + " 份待缴费账单！");
        return result;
    }

    // 2. 家长模拟秒级支付
    @PostMapping("/pay")
    public Map<String, Object> payFee(@RequestBody Map<String, Integer> params) {
        Map<String, Object> result = new HashMap<>();
        Integer taskId = params.get("taskId");
        Integer parentId = params.get("parentId");

        // 找到该家长对应的这笔账单
        FeeRecord record = feeRecordRepository.findByTaskIdAndParentId(taskId, parentId);

        if (record != null && record.getPayStatus() == 0) {
            record.setPayStatus(1); // 修改状态为已交
            record.setPayTime(new Date()); // 记录交钱时间
            feeRecordRepository.save(record);

            result.put("code", 200);
            result.put("msg", "支付核销成功！");
        } else {
            result.put("code", 400);
            result.put("msg", "未找到该账单或已支付过！");
        }
        return result;
    }

    // 1. 获取所有缴费任务 (教职工台账用)
    @GetMapping("/tasks")
    public List<FeeTask> getAllTasks() {
        return feeTaskRepository.findAll();
    }

    // 2. 根据任务ID获取所有的缴费明细 (教职工点开抽屉看进度用)
    @GetMapping("/records/{taskId}")
    public List<FeeRecord> getRecordsByTask(@PathVariable Integer taskId) {
        return feeRecordRepository.findByTaskId(taskId);
    }

    // 3. 根据家长ID获取他自己的所有账单 (家长用)
    @GetMapping("/myRecords/{parentId}")
    public List<FeeRecord> getMyRecords(@PathVariable Integer parentId) {
        // 由于之前没建根据家长查的内置方法，这里用一种简单的流过滤实现
        List<FeeRecord> all = feeRecordRepository.findAll();
        List<FeeRecord> mine = new ArrayList<>();
        for (FeeRecord r : all) {
            if (r.getParentId().equals(parentId)) {
                mine.add(r);
            }
        }
        return mine;
    }

    // 4. 获取所有学生名单 (专门提供给前端做姓名翻译)
    @GetMapping("/students")
    public List<Student> getAllStudentsForFee() {
        return studentRepository.findAll();
    }

    // 批量发送缴费提醒消息
    @PostMapping("/remind")
    public Map<String, Object> remindUnpaidParents(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();

        String taskTitle = (String) params.get("taskTitle");
        List<Integer> parentIds = (List<Integer>) params.get("parentIds");
        Integer teacherId = (Integer) params.get("teacherId"); // 拿到前端传来的班主任ID

        if (parentIds != null && !parentIds.isEmpty() && teacherId != null) {
            for (Integer parentId : parentIds) {
                String content = "【系统催缴】您好，班级发布的缴费任务 [" + taskTitle + "] 您暂未完成，请及时前往“缴费管理”页面处理。";

                // 实例化你的聊天实体类 (类名可能叫 ChatMessage)
                ChatMessage msg = new ChatMessage();
                msg.setSender("系统助手");
                msg.setContent(content);
                msg.setSendTime(new Date());
                msg.setChatType("PRIVATE"); // 必须是大写的 PRIVATE

                //手动拼接出前端能识别的字符串 ID 格式
                msg.setTargetId("parent_" + parentId);
                msg.setSenderId("teacher_" + teacherId);

                msg.setIsRead(0);
                msg.setMsgType("TEXT");

                // 保存到数据库
                chatMessageRepository.save(msg);
                System.out.println("成功向 parent_" + parentId + " 写入催缴消息！");
            }
        }

        result.put("code", 200);
        result.put("msg", "提醒发送成功");
        return result;
    }

    // 5. 获取所有家长名单 (专门提供给前端做姓名翻译)
    @GetMapping("/parents")
    public List<Parent> getAllParentsForFee() {
        return parentRepository.findAll();
    }

    // 高级群收款接口：支持区分部分人员和独立金额
    @PostMapping("/createAdvanced")
    public Map<String, Object> createAdvancedFeeTask(@RequestBody Map<String, Object> payload) {
        Map<String, Object> result = new HashMap<>();

        try {
            String title = (String) payload.get("title");

            // 核心修复：彻底解决 String 强转 Integer 的崩溃问题，兼容各种前端传参格式
            Integer classId = Integer.valueOf(payload.get("classId").toString());
            Integer initiatorId = Integer.valueOf(payload.get("initiatorId").toString());
            String feeType = (String) payload.get("feeType");
            List<Map<String, Object>> details = (List<Map<String, Object>>) payload.get("details");

            if (details == null || details.isEmpty()) {
                result.put("code", 400);
                result.put("msg", "接收对象为空");
                return result;
            }

            FeeTask task = new FeeTask();
            task.setTitle(title);
            task.setClassId(classId);
            task.setInitiatorId(initiatorId);

            if ("UNIFORM".equals(feeType)) {
                task.setAmount(Double.valueOf(details.get(0).get("amount").toString()));
            } else {
                task.setAmount(0.0);
            }
            task.setCreateTime(new Date());
            task.setStatus(0);

            feeTaskRepository.save(task);

            for (Map<String, Object> detail : details) {
                // 核心修复：安全解析 studentId
                Integer studentId = Integer.valueOf(detail.get("studentId").toString());
                Double amount = Double.valueOf(detail.get("amount").toString());

                Student student = studentRepository.findById(studentId).orElse(null);
                if (student != null && student.getParentId() != null) {
                    FeeRecord record = new FeeRecord();
                    record.setTaskId(task.getId());
                    record.setStudentId(studentId);
                    record.setParentId(student.getParentId());
                    record.setPayStatus(0);
                    record.setActualAmount(amount);

                    feeRecordRepository.save(record);
                }
            }

            result.put("code", 200);
            result.put("taskId", task.getId());
            result.put("msg", "高级收款任务发布成功");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("msg", "服务器数据解析错误: " + e.getMessage());
        }
        return result;
    }

    // 根据缴费明细ID更新支付状态，支付宝支付成功后调用
    // 支付宝支付成功后，同步更新缴费状态，并跳转回前端账单页面
    @GetMapping(value = "/paySuccess", produces = "text/html;charset=UTF-8")
    public String paySuccess(@RequestParam Integer recordId) {
        Optional<FeeRecord> optional = feeRecordRepository.findById(recordId);

        if (optional.isPresent()) {
            FeeRecord record = optional.get();

            if (record.getPayStatus() == null || record.getPayStatus() == 0) {
                record.setPayStatus(1);
                record.setPayTime(new Date());
                feeRecordRepository.save(record);
            }

            return "<html><head><meta charset='UTF-8'></head>"
                    + "<body style='font-family: Arial; text-align:center; padding-top:80px;'>"
                    + "<h2>支付成功</h2>"
                    + "<p>账单状态已更新，正在返回系统账单页面...</p>"
                    + "<script>"
                    + "setTimeout(function(){"
                    + "window.location.href='http://localhost:5173/layout/fee';"
                    + "}, 1200);"
                    + "</script>"
                    + "</body></html>";
        }

        return "<html><head><meta charset='UTF-8'></head>"
                + "<body style='font-family: Arial; text-align:center; padding-top:80px;'>"
                + "<h2>支付失败</h2>"
                + "<p>未找到对应的缴费账单。</p>"
                + "<script>"
                + "setTimeout(function(){"
                + "window.location.href='http://localhost:5173/layout/fee';"
                + "}, 2000);"
                + "</script>"
                + "</body></html>";
    }

}