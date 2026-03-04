package com.sp.web.controller.common;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sp.common.constant.Constants;
import com.sp.common.core.domain.AjaxResult;
import com.sp.common.exception.file.FileSizeLimitExceededException;
import com.sp.common.exception.file.InvalidExtensionException;
import com.sp.common.utils.file.FileUploadUtils;

/**
 * 通用请求处理
 */
@RestController
@RequestMapping("/common")
public class CommonController
{
    /**
     * 通用上传请求
     */
    @PostMapping("/upload")
    public AjaxResult uploadFile(@RequestParam("file") MultipartFile file) throws Exception
    {
        try
        {
            // 上传并返回新文件名称
            String fileName = FileUploadUtils.upload(file);
            Map<String, Object> data = new HashMap<>();
            data.put("url", fileName);
            return AjaxResult.success(data);
        }
        catch (FileSizeLimitExceededException e)
        {
            return AjaxResult.error(e.getMessage());
        } catch (IOException e)
        {
            return AjaxResult.error(e.getMessage());
        }
        catch (Exception e)
        {
            return AjaxResult.error("上传失败：" + e.getMessage());
        }
    }
}
