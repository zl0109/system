package com.school.system.controller;

import com.school.system.entity.Score;
import com.school.system.entity.Student;
import com.school.system.repository.ScoreRepository;
import com.school.system.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/score")
@CrossOrigin
public class ScoreController {

    @Autowired private ScoreRepository scoreRepository;
    @Autowired private StudentRepository studentRepository;

    // 不同身份拿到不同数据
    @GetMapping("/list")
    public List<Score> getList(@RequestParam String role, @RequestParam String userName, @RequestParam Integer userId) {
        List<Score> allScores = scoreRepository.findAll();

        // 1. 如果是学生，只返回名字匹配的成绩
        if ("student".equals(role)) {
            return allScores.stream().filter(s -> userName.equals(s.getStudentName())).collect(Collectors.toList());
        }
        // 2. 如果是家长，找出他绑定的所有孩子，只返回这些孩子的成绩
        else if ("parent".equals(role)) {
            List<String> childNames = studentRepository.findByParentId(userId).stream()
                    .map(Student::getName).collect(Collectors.toList());
            return allScores.stream().filter(s -> childNames.contains(s.getStudentName())).collect(Collectors.toList());
        }

        // 3. 如果是老师、班主任、教务处，返回全校成绩供管理
        return allScores;
    }

    @PostMapping("/save")
    public Map<String, Object> save(@RequestBody Score score) {
        if (score.getCreateTime() == null) {
            score.setCreateTime(new Date());
        }
        scoreRepository.save(score);

        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("msg", "保存成功");
        return res;
    }

    @PostMapping("/saveBatch")
    public Map<String, Object> saveBatch(@RequestBody List<Score> scores) {
        Date now = new Date();
        for (Score score : scores) {
            if (score.getCreateTime() == null) {
                score.setCreateTime(now); // 统一打上当前时间戳
            }
        }
        scoreRepository.saveAll(scores);

        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("msg", "批量录入成功");
        return res;
    }

    @DeleteMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        scoreRepository.deleteById(id);
        return "删除成功";
    }
}