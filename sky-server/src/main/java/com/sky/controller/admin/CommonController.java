package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@Slf4j
@Api(tags = "通用接口")
@RequestMapping("/admin/common")
public class CommonController {
    @Autowired
    private AliOssUtil aliOssUtil;

    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file){
        //获取原始文件名
        String originalFilename = file.getOriginalFilename();
        //获取文件的后缀名
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        //构造新的文件名
        String filename = UUID.randomUUID().toString() + extension;
        //获取文件的请求路径
        try {
            String path=aliOssUtil.upload(file.getBytes(), filename);
            return Result.success(path);
        } catch (IOException e) {
            log.info("文件上传失败！");
        }
        return Result.error(MessageConstant.UPLOAD_FAILED);
    }
}
