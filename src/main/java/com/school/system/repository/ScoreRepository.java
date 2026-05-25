package com.school.system.repository;

import com.school.system.entity.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScoreRepository extends JpaRepository<Score, Integer> {

    List<Score> findByExamNameAndStudentName(String examName, String studentName);
}