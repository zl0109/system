package com.school.system.dto;

import com.alibaba.excel.annotation.ExcelProperty;

public class StudentExcelDTO {

    @ExcelProperty("学号")
    private String studentNo;

    @ExcelProperty("学生姓名")
    private String name;

    @ExcelProperty("班级ID")
    private Integer classId;

    @ExcelProperty("家长ID(选填)")
    private Integer parentId;

    // ================= 手动补全的 Getter 和 Setter =================

    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getClassId() { return classId; }
    public void setClassId(Integer classId) { this.classId = classId; }

    public Integer getParentId() { return parentId; }
    public void setParentId(Integer parentId) { this.parentId = parentId; }
}