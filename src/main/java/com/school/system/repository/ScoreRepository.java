package com.school.system.repository;

import com.school.system.entity.Score;
import com.school.system.dto.ScoreDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ScoreRepository extends JpaRepository<Score, Integer> {

    // 重点来了！这就是多表关联查询 (LEFT JOIN)
    // 把 score 表 (取别名s) 和 student 表 (取别名st) 通过 student_id 连起来
    @Query(value = "SELECT s.score_id AS scoreId, s.exam_name AS examName, " +
            "s.subject AS subject, s.score AS score, s.entry_time AS entryTime, " +
            "st.name AS studentName " +
            "FROM score s LEFT JOIN student st ON s.student_id = st.student_id",
            nativeQuery = true)
    List<ScoreDTO> findScoreListWithStudentName();
}