package com.school.system.controller;

import com.school.system.entity.Score;
import com.school.system.entity.Student;
import com.school.system.entity.ClassInfo;
import com.school.system.repository.ScoreRepository;
import com.school.system.repository.StudentRepository;
import com.school.system.repository.ClassInfoRepository;
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
    @Autowired private ClassInfoRepository classInfoRepository; // 引入班级表操作

    // 核心补充 1：提供给前端用于匹配学生班级
    @GetMapping("/students")
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    // 核心补充 2：提供给前端用于下拉框展示班级列表
    @GetMapping("/classes")
    public List<ClassInfo> getAllClasses() {
        return classInfoRepository.findAll();
    }

    @GetMapping("/list")
    public List<Score> getList(@RequestParam String role, @RequestParam String userName, @RequestParam Integer userId) {
        List<Score> allScores = scoreRepository.findAll();

        if ("student".equals(role)) {
            return allScores.stream().filter(s -> userName.equals(s.getStudentName())).collect(Collectors.toList());
        }
        else if ("parent".equals(role)) {
            List<String> childNames = studentRepository.findByParentId(userId).stream()
                    .map(Student::getName).collect(Collectors.toList());
            return allScores.stream().filter(s -> childNames.contains(s.getStudentName())).collect(Collectors.toList());
        }

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
                score.setCreateTime(now);
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