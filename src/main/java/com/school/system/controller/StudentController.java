package com.school.system.controller;

import com.alibaba.excel.EasyExcel;
import com.school.system.dto.StudentExcelDTO;
import com.school.system.entity.Student;
import com.school.system.repository.ClassInfoRepository;
import com.school.system.repository.ParentRepository;
import com.school.system.repository.StudentRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/student")
@CrossOrigin
public class StudentController {

    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private ClassInfoRepository classInfoRepository;
    @Autowired
    private ParentRepository parentRepository;

    @GetMapping("/list")
    public List<Student> getList() {
        List<Student> students = studentRepository.findAll();
        for (Student s : students) {
            if (s.getClassId() != null) {
                classInfoRepository.findById(s.getClassId())
                        .ifPresent(c -> s.setClassName(c.getClassName()));
            }
            if (s.getParentId() != null) {
                parentRepository.findById(s.getParentId())
                        .ifPresent(p -> {
                            String rel = p.getRelation() != null ? p.getRelation() : "家长";
                            s.setParentName(p.getName() + " (" + rel + ")");
                        });
            }
        }
        return students;
    }

    @PostMapping("/save")
    public String save(@RequestBody Student student) {
        studentRepository.save(student);
        return "保存成功";
    }

    @DeleteMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        studentRepository.deleteById(id);
        return "删除成功";
    }

    @GetMapping("/search")
    public List<Student> search(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return studentRepository.findAll();
        }
        return studentRepository.findByNameContainingOrStudentNoContaining(keyword, keyword);
    }

    @GetMapping("/findByParent")
    public List<Student> findByParent(@RequestParam Integer parentId) {
        return studentRepository.findByParentId(parentId);
    }

    // ================= Excel 导入导出 =================

    @GetMapping("/export")
    public void exportStudents(HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("学生信息档案", "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            List<Student> students = studentRepository.findAll();
            List<StudentExcelDTO> dtoList = new ArrayList<>();

            for (Student s : students) {
                StudentExcelDTO dto = new StudentExcelDTO();
                dto.setStudentNo(s.getStudentNo());
                dto.setName(s.getName());
                dto.setClassId(s.getClassId());
                dto.setParentId(s.getParentId());
                dtoList.add(dto);
            }

            EasyExcel.write(response.getOutputStream(), StudentExcelDTO.class)
                    .sheet("学生数据")
                    .doWrite(dtoList);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/import")
    public Map<String, Object> importStudents(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<StudentExcelDTO> list = EasyExcel.read(file.getInputStream())
                    .head(StudentExcelDTO.class)
                    .sheet()
                    .doReadSync();

            List<Student> studentsToSave = new ArrayList<>();

            for (StudentExcelDTO dto : list) {
                Student student = new Student();
                student.setStudentNo(dto.getStudentNo());
                student.setName(dto.getName());
                student.setClassId(dto.getClassId());
                student.setParentId(dto.getParentId());

                // 默认初始密码
                student.setPassword("123456");

                studentsToSave.add(student);
            }

            studentRepository.saveAll(studentsToSave);

            result.put("code", 200);
            result.put("msg", "成功导入 " + list.size() + " 条学生数据！");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("msg", "文件解析失败，请检查模板格式是否正确");
        }
        return result;
    }
}