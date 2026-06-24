package com.sms.web.controller.common;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sms.common.annotation.Log;
import com.sms.common.core.domain.AjaxResult;
import com.sms.common.enums.BusinessType;
import com.sms.common.exception.file.FileSizeLimitExceededException;
import com.sms.common.utils.file.FileUploadUtils;

/**
 * 通用請求處理
 */
@RestController
@RequestMapping("/common")
public class CommonController
{
    /**
     * 通用上傳請求
     */
    @Log(title = "文件上傳", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult uploadFile(@RequestParam("file") MultipartFile file) throws Exception
    {
        try
        {
            // 上傳並返回新文件名稱
            String fileName = FileUploadUtils.upload(file);
            Map<String, Object> data = new HashMap<>();
            data.put("url", fileName);
            return AjaxResult.success(data);
        }
        catch (FileSizeLimitExceededException | IOException e)
        {
            return AjaxResult.error(e.getMessage());
        } catch (Exception e)
        {
            return AjaxResult.error("上傳失敗：" + e.getMessage());
        }
    }
}
