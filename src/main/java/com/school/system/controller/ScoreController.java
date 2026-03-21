package com.school.system.controller;

import com.school.system.entity.Score;
import com.school.system.dto.ScoreDTO; // 引入刚才建的 DTO
import com.school.system.repository.ScoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/score")
@CrossOrigin
public class ScoreController {

    @Autowired
    private ScoreRepository scoreRepository;

    // --- 修改这里 ---
    @GetMapping("/list")
    public List<ScoreDTO> getScoreList() {
        // 不再用单表的 findAll() 了，用我们自己写的连表查询方法！
        return scoreRepository.findScoreListWithStudentName();
    }
    // --------------

    @PostMapping("/save")
    public String saveScore(@RequestBody Score score) {
        score.setEntryTime(new Date());
        scoreRepository.save(score);
        return "保存成功";
    }
}