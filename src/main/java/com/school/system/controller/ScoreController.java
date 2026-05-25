package com.school.system.controller;

import com.alibaba.excel.EasyExcel;
import com.school.system.dto.ScoreExcelDTO;
import com.school.system.entity.ClassInfo;
import com.school.system.entity.Score;
import com.school.system.entity.Student;
import com.school.system.repository.ClassInfoRepository;
import com.school.system.repository.ScoreRepository;
import com.school.system.repository.StudentRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/score")
@CrossOrigin
public class ScoreController {

    @Autowired
    private ScoreRepository scoreRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ClassInfoRepository classInfoRepository;

    private final List<String> subjectList = Arrays.asList(
            "语文", "数学", "英语", "物理", "化学", "生物", "政治", "历史", "地理"
    );

    @GetMapping("/students")
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @GetMapping("/classes")
    public List<ClassInfo> getAllClasses() {
        return classInfoRepository.findAll();
    }

    @GetMapping("/list")
    public List<Score> getList(@RequestParam String role,
                               @RequestParam String userName,
                               @RequestParam Integer userId) {
        List<Score> allScores = scoreRepository.findAll();

        if ("student".equals(role)) {
            return allScores.stream()
                    .filter(s -> userName.equals(s.getStudentName()))
                    .collect(Collectors.toList());
        }

        if ("parent".equals(role)) {
            List<String> childNames = studentRepository.findByParentId(userId)
                    .stream()
                    .map(Student::getName)
                    .collect(Collectors.toList());

            return allScores.stream()
                    .filter(s -> childNames.contains(s.getStudentName()))
                    .collect(Collectors.toList());
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

    /**
     * 成绩 Excel 导出
     * 表格从“学号”开始，不导出考试名称；
     * 自动计算总分，并按照总分从高到低排序。
     */
    @GetMapping("/export")
    public void exportScores(@RequestParam String examName,
                             @RequestParam Integer classId,
                             HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");

            String fileName = URLEncoder.encode(examName + "-成绩表", "UTF-8")
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            List<Student> allStudents = studentRepository.findAll();
            List<Student> studentsInClass = allStudents.stream()
                    .filter(student -> Objects.equals(student.getClassId(), classId))
                    .collect(Collectors.toList());

            List<Score> allScores = scoreRepository.findAll();

            List<ScoreExcelDTO> excelList = new ArrayList<>();

            for (Student student : studentsInClass) {
                ScoreExcelDTO dto = new ScoreExcelDTO();

                dto.setStudentNo(student.getStudentNo());
                dto.setStudentName(student.getName());

                dto.setChinese(findScoreValue(allScores, examName, student.getName(), "语文"));
                dto.setMath(findScoreValue(allScores, examName, student.getName(), "数学"));
                dto.setEnglish(findScoreValue(allScores, examName, student.getName(), "英语"));
                dto.setPhysics(findScoreValue(allScores, examName, student.getName(), "物理"));
                dto.setChemistry(findScoreValue(allScores, examName, student.getName(), "化学"));
                dto.setBiology(findScoreValue(allScores, examName, student.getName(), "生物"));
                dto.setPolitics(findScoreValue(allScores, examName, student.getName(), "政治"));
                dto.setHistory(findScoreValue(allScores, examName, student.getName(), "历史"));
                dto.setGeography(findScoreValue(allScores, examName, student.getName(), "地理"));

                dto.setTotalScore(calcTotalScore(dto));

                excelList.add(dto);
            }

            excelList.sort((a, b) -> Double.compare(
                    b.getTotalScore() == null ? 0 : b.getTotalScore(),
                    a.getTotalScore() == null ? 0 : a.getTotalScore()
            ));

            EasyExcel.write(response.getOutputStream(), ScoreExcelDTO.class)
                    .sheet("成绩表")
                    .doWrite(excelList);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 成绩 Excel 导入
     * Excel 中不需要考试名称；
     * 考试名称由前端通过 examName 参数传入。
     */
    @PostMapping("/import")
    public Map<String, Object> importScores(@RequestParam("file") MultipartFile file,
                                            @RequestParam String examName) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<ScoreExcelDTO> excelList = EasyExcel.read(file.getInputStream())
                    .head(ScoreExcelDTO.class)
                    .sheet()
                    .doReadSync();

            List<Student> allStudents = studentRepository.findAll();
            List<Score> allScores = scoreRepository.findAll();

            List<Score> needDeleteScores = new ArrayList<>();
            List<Score> needSaveScores = new ArrayList<>();

            Date now = new Date();

            for (ScoreExcelDTO dto : excelList) {
                if (dto.getStudentNo() == null || dto.getStudentNo().trim().isEmpty()) {
                    continue;
                }

                if (dto.getStudentName() == null || dto.getStudentName().trim().isEmpty()) {
                    continue;
                }

                Student matchedStudent = findStudent(allStudents, dto.getStudentNo(), dto.getStudentName());

                if (matchedStudent == null) {
                    continue;
                }

                String studentName = matchedStudent.getName();

                List<Score> oldScores = allScores.stream()
                        .filter(score -> examName.equals(score.getExamName()))
                        .filter(score -> studentName.equals(score.getStudentName()))
                        .collect(Collectors.toList());

                needDeleteScores.addAll(oldScores);

                addScore(needSaveScores, examName, studentName, "语文", dto.getChinese(), now);
                addScore(needSaveScores, examName, studentName, "数学", dto.getMath(), now);
                addScore(needSaveScores, examName, studentName, "英语", dto.getEnglish(), now);
                addScore(needSaveScores, examName, studentName, "物理", dto.getPhysics(), now);
                addScore(needSaveScores, examName, studentName, "化学", dto.getChemistry(), now);
                addScore(needSaveScores, examName, studentName, "生物", dto.getBiology(), now);
                addScore(needSaveScores, examName, studentName, "政治", dto.getPolitics(), now);
                addScore(needSaveScores, examName, studentName, "历史", dto.getHistory(), now);
                addScore(needSaveScores, examName, studentName, "地理", dto.getGeography(), now);
            }

            if (!needDeleteScores.isEmpty()) {
                scoreRepository.deleteAll(needDeleteScores);
            }

            if (!needSaveScores.isEmpty()) {
                scoreRepository.saveAll(needSaveScores);
            }

            result.put("code", 200);
            result.put("msg", "成功导入 " + needSaveScores.size() + " 条成绩数据");
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("msg", "成绩导入失败，请检查 Excel 表格格式是否正确");
            return result;
        }
    }

    private Student findStudent(List<Student> students, String studentNo, String studentName) {
        for (Student student : students) {
            if (studentNo.equals(student.getStudentNo()) && studentName.equals(student.getName())) {
                return student;
            }
        }
        return null;
    }

    private Double findScoreValue(List<Score> allScores,
                                  String examName,
                                  String studentName,
                                  String subject) {
        for (Score score : allScores) {
            if (examName.equals(score.getExamName())
                    && studentName.equals(score.getStudentName())
                    && subject.equals(score.getSubject())) {
                return score.getScore();
            }
        }
        return null;
    }

    private Double calcTotalScore(ScoreExcelDTO dto) {
        double total = 0;

        total += dto.getChinese() == null ? 0 : dto.getChinese();
        total += dto.getMath() == null ? 0 : dto.getMath();
        total += dto.getEnglish() == null ? 0 : dto.getEnglish();
        total += dto.getPhysics() == null ? 0 : dto.getPhysics();
        total += dto.getChemistry() == null ? 0 : dto.getChemistry();
        total += dto.getBiology() == null ? 0 : dto.getBiology();
        total += dto.getPolitics() == null ? 0 : dto.getPolitics();
        total += dto.getHistory() == null ? 0 : dto.getHistory();
        total += dto.getGeography() == null ? 0 : dto.getGeography();

        return total;
    }

    private void addScore(List<Score> list,
                          String examName,
                          String studentName,
                          String subject,
                          Double value,
                          Date createTime) {
        if (value == null) {
            return;
        }
        //前端传来的成绩数据先被封装成 Score 对象
        //后端再通过saveAll()批量写入成绩表。
        Score score = new Score();
        score.setExamName(examName);
        score.setStudentName(studentName);
        score.setSubject(subject);
        score.setScore(value);
        score.setCreateTime(createTime);

        list.add(score);
    }
}