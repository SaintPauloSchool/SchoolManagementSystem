package com.sms.web.controller.common;

import com.sms.common.annotation.Log;
import com.sms.common.config.OverallSituationConfig;
import com.sms.common.constant.Constants;
import com.sms.common.enums.BusinessType;
import com.sms.common.utils.StringUtils;
import com.sms.common.utils.file.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

/**
 * 通用文件下載處理
 */
@RestController
@RequestMapping("/common/download")
public class FileDownloadController {

    private static final Logger log = LoggerFactory.getLogger(FileDownloadController.class);

    /**
     * 本地資源通用下載
     */
    @Log(title = "文件下載", businessType = BusinessType.EXPORT)
    @GetMapping("/resource")
    public void resourceDownload(String resource, HttpServletResponse response) {
        try {
            if (!FileUtils.checkAllowDownload(resource)) {
                throw new Exception(StringUtils.format("資源文件({})非法，不允許下載。 ", resource));
            }
            // 本地資源路徑
            String localPath = OverallSituationConfig.getProfile();
            // 數據庫資源地址
            String downloadPath = localPath + StringUtils.substringAfter(resource, Constants.RESOURCE_PREFIX);
            // 下載名稱
            String downloadName = StringUtils.substringAfterLast(downloadPath, "/");
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, downloadName);
            FileUtils.writeBytes(downloadPath, response.getOutputStream());
        } catch (Exception e) {
            log.error("下載文件失敗", e);
        }
    }
}
