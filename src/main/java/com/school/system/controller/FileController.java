package com.school.system.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/file")
@CrossOrigin
public class FileController {

    //接收前端传来的图片，存入本地 uploads 文件夹，并返回访问 URL
    @PostMapping("/upload")
    public Map<String, Object> upload(MultipartFile file) {
        Map<String, Object> res = new HashMap<>();
        try {
            // 获取文件后缀名 (比如 .jpg, .png)
            String originalFilename = file.getOriginalFilename();
            String ext = originalFilename.substring(originalFilename.lastIndexOf("."));
            // 随机生成一个全球唯一的文件名，防止重名覆盖
            String newName = UUID.randomUUID().toString() + ext;

            // 设定保存路径为当前项目目录下的 uploads 文件夹
            String path = System.getProperty("user.dir") + "/uploads/";
            File dir = new File(path);
            if (!dir.exists()) dir.mkdirs(); // 如果文件夹不存在就创建

            // 保存文件
            file.transferTo(new File(path + newName));

            res.put("code", 200);
            res.put("url", "http://localhost:8080/uploads/" + newName); // 拼接出访问地址
        } catch (Exception e) {
            res.put("code", 500);
            res.put("msg", "文件上传失败");
        }
        return res;
    }
}