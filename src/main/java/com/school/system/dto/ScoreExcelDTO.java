package com.school.system.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class ScoreExcelDTO {

    @ExcelProperty("学号")
    private String studentNo;

    @ExcelProperty("学生姓名")
    private String studentName;

    @ExcelProperty("语文")
    private Double chinese;

    @ExcelProperty("数学")
    private Double math;

    @ExcelProperty("英语")
    private Double english;

    @ExcelProperty("物理")
    private Double physics;

    @ExcelProperty("化学")
    private Double chemistry;

    @ExcelProperty("生物")
    private Double biology;

    @ExcelProperty("政治")
    private Double politics;

    @ExcelProperty("历史")
    private Double history;

    @ExcelProperty("地理")
    private Double geography;

    @ExcelProperty("总分")
    private Double totalScore;
}