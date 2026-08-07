package com.sms.system.service.impl.notification;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.system.entity.SysDepartment;
import com.sms.system.entity.SysSchoolDepartmentMember;
import com.sms.system.entity.SysSchoolFamilyContact;
import com.sms.system.entity.SysSchoolDepartment;
import com.sms.system.entity.notification.*;
import com.sms.system.entity.vo.QuestionItemVO;
import com.sms.system.mapper.SysDepartmentMapper;
import com.sms.system.mapper.SysSchoolDepartmentMemberMapper;
import com.sms.system.mapper.SysSchoolFamilyContactMapper;
import com.sms.system.mapper.SysSchoolDepartmentMapper;
import com.sms.system.mapper.notification.*;
import com.sms.system.service.notification.INotificationExportService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 通知導出 Service 業務層處理
 */
@Service
public class NotificationExportServiceImpl implements INotificationExportService {

    private static final Logger log = LoggerFactory.getLogger(NotificationExportServiceImpl.class);

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private NotificationQuestionMapper notificationQuestionMapper;

    @Autowired
    private NotificationAnswerMapper notificationAnswerMapper;

    @Autowired
    private NotificationSendRecordMapper notificationSendRecordMapper;

    @Autowired
    private NotificationUserReadRecordMapper notificationUserReadRecordMapper;

    @Autowired
    private SysSchoolDepartmentMemberMapper schoolDepartmentMemberMapper;

    @Autowired
    private SysSchoolFamilyContactMapper schoolFamilyContactMapper;

    @Autowired
    private SysDepartmentMapper departmentMapper;

    @Autowired
    private SysSchoolDepartmentMapper schoolDepartmentMapper;

    @Value("${sp.profile:}")
    private String spProfile;

    @Override
    public void exportNotificationAnswers(Long notificationId, HttpServletResponse response) {
        try {
            // 1. 查詢通知基本資訊
            Notification notification = notificationMapper.selectById(notificationId);
            if (notification == null) {
                log.error("通知不存在，notificationId: {}", notificationId);
                return;
            }

            // 2. 查詢發送記錄
            NotificationSendRecord sendRecord = notificationSendRecordMapper.selectByNotificationId(notificationId);
            Integer totalCount = sendRecord != null ? sendRecord.getTotalCount() : 0;

            // 3. 查詢閱讀記錄
            List<NotificationUserReadRecord> readRecords = notificationUserReadRecordMapper.selectBySendRecordId(
                    sendRecord != null ? sendRecord.getSendRecordId() : null);

            // 4. 查詢問題列表
            List<NotificationQuestion> questions = notificationQuestionMapper.selectByNotificationId(notificationId);

            // 5. 查詢所有回答
            List<NotificationAnswer> allAnswers = notificationAnswerMapper.selectByNotificationId(notificationId);

            // 6. 統計已處理人數（去重後的userId）
            long processedCount = allAnswers.stream()
                    .map(NotificationAnswer::getUserId)
                    .distinct()
                    .count();

            // 7. 創建Excel工作簿
            Workbook workbook = new XSSFWorkbook();

            // 8. 創建統計Sheet
            createStatisticsSheet(workbook, notification, questions, allAnswers, totalCount, (int) processedCount);

            // 9. 創建詳情Sheet
            createDetailSheet(workbook, notification, sendRecord, questions, allAnswers, readRecords);

            // 10. 導出Excel和附件爲Zip
            response.setContentType("application/zip");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode(notification.getTitle() + "_回復統計", "UTF-8");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".zip");

            try (OutputStream out = response.getOutputStream();
                 ZipOutputStream zos = new ZipOutputStream(out)) {

                // 寫入Excel文件
                ZipEntry excelEntry = new ZipEntry(notification.getTitle() + "_回復統計.xlsx");
                zos.putNextEntry(excelEntry);
                workbook.write(zos);
                zos.closeEntry();

                // 收集附件
                Set<String> addedFileNames = new HashSet<>(); // 防止同名文件覆蓋
                for (NotificationAnswer answer : allAnswers) {
                    if (answer.getAnswerData() != null) {
                        try {
                            JSONArray answerArray = JSON.parseArray(answer.getAnswerData());
                            for (int i = 0; i < answerArray.size(); i++) {
                                JSONObject answerObj = answerArray.getJSONObject(i);
                                // 檢查是否有 attachmentUrls
                                if (answerObj.containsKey("attachmentUrls") && answerObj.getString("attachmentUrls") != null) {
                                    String attachmentUrlsStr = answerObj.getString("attachmentUrls");
                                    if (!attachmentUrlsStr.isEmpty()) {
                                        // 有些可能是字符串，嘗試解析爲JSONArray
                                        JSONArray attachments = null;
                                        try {
                                            attachments = JSON.parseArray(attachmentUrlsStr);
                                        } catch (Exception e) {
                                            log.warn("解析 attachmentUrls 失敗，可能不是 JSON 數組格式: {}", attachmentUrlsStr);
                                        }

                                        if (attachments != null) {
                                            // 遍歷附件
                                            for (int j = 0; j < attachments.size(); j++) {
                                                // 獲取附件資訊
                                                JSONObject attachment = attachments.getJSONObject(j);
                                                String url = attachment.getString("url");
                                                String name = attachment.getString("name");

                                                // 處理附件
                                                if (url != null && url.startsWith("/profile")) {
                                                    String basePath = (spProfile != null && !spProfile.trim().isEmpty())
                                                            ? spProfile
                                                            : com.sms.common.config.OverallSituationConfig.getProfile();
                                                    String localPath = url.replace("/profile", basePath);
                                                    File file = new File(localPath);
                                                    if (file.exists() && file.isFile()) {
                                                        // 處理同名文件
                                                        String safeName = getString(name, file, url);

                                                        String finalName = getString(safeName, addedFileNames);
                                                        addedFileNames.add(finalName);

                                                        ZipEntry fileEntry = new ZipEntry("附件/" + finalName);
                                                        zos.putNextEntry(fileEntry);
                                                        try (FileInputStream fis = new FileInputStream(file)) {
                                                            byte[] buffer = new byte[1024];
                                                            int length;
                                                            while ((length = fis.read(buffer)) >= 0) {
                                                                zos.write(buffer, 0, length);
                                                            }
                                                        }
                                                        zos.closeEntry();
                                                    } else {
                                                        log.warn("找不到附件文件: {}", localPath);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.error("解析或打包附件時出錯", e);
                        }
                    }
                }
                zos.flush();
            }

            workbook.close();
            log.info("導出通知回復答案成功（含附件），notificationId: {}", notificationId);

        } catch (Exception e) {
            log.error("導出通知回復答案失敗，notificationId: {}", notificationId, e);
        }
    }

    /**
     * 處理文件名，確保不重複
     * @param name 文件名
     * @param file 文件對象
     * @param url 文件的URL
     * @return 處理後的文件名
     */
    private static String getString(String name, File file, String url) {
        String safeName = name;
        if (safeName == null || safeName.isEmpty()) {
            safeName = file.getName();
        } else {
            // 確保帶擴展名
            String ext = "";
            int dotIndex = url.lastIndexOf(".");
            if (dotIndex > 0) {
                ext = url.substring(dotIndex);
            }
            if (!ext.isEmpty() && !safeName.toLowerCase().endsWith(ext.toLowerCase())) {
                safeName += ext;
            }
        }
        return safeName;
    }

    /**
     * 處理文件名，確保不重複
     * @param safeName
     * @param addedFileNames
     * @return
     */
    private static String getString(String safeName, Set<String> addedFileNames) {
        int suffix = 1;
        String finalName = safeName;
        while (addedFileNames.contains(finalName)) {
            int dotIndex = safeName.lastIndexOf(".");
            if (dotIndex > 0) {
                finalName = safeName.substring(0, dotIndex) + "(" + suffix + ")" + safeName.substring(dotIndex);
            } else {
                finalName = safeName + "(" + suffix + ")";
            }
            suffix++;
        }
        return finalName;
    }

    /**
     * 創建統計Sheet
     */
    private void createStatisticsSheet(Workbook workbook, Notification notification,
                                       List<NotificationQuestion> questions,
                                       List<NotificationAnswer> allAnswers,
                                       Integer totalCount, Integer processedCount) {
        Sheet sheet = workbook.createSheet("統計");

        // 創建樣式
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle titleStyle = createTitleStyle(workbook);

        int rowNum = 0;

        // 第1-4行：合併單元格顯示問卷資訊
        Row headerInfoRow = sheet.createRow(rowNum);
        headerInfoRow.setHeightInPoints(100); // 設置較高的行高以容納4行內容
        Cell headerInfoCell = headerInfoRow.createCell(0);

        // 構建多行文本
        String infoBuilder = notification.getTitle() + "\n" +
                "發送時間：" + formatDate(notification.getCreateTime()) + "\n" +
                "接收人數：" + totalCount + "\n" +
                "已處理人數：" + processedCount;

        headerInfoCell.setCellValue(infoBuilder);
        headerInfoCell.setCellStyle(titleStyle);

        // 合併 A1:C4 (行0-3, 列0-2)
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 3, 0, 2));
        // 爲合併區域添加邊框
        setRegionBorder(sheet, 0, 3, 0, 2, titleStyle);

        // 跳過已合併的行
        rowNum = 4;
        // 表頭行
        Row headerRow = sheet.createRow(rowNum++);
        headerRow.setHeightInPoints(25);
        String[] headers = {"題目", "選項", "已選人數"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 第5行起：統計數據
        for (NotificationQuestion question : questions) {
            // 解析題目數據
            List<QuestionItemVO> questionItems = parseQuestionContent(question);

            for (QuestionItemVO item : questionItems) {
                // 統計Sheet不顯示填空題（type="3"）
                if ("3".equals(item.getType())) {
                    continue;
                }

                // 統計每個選項的選擇人數
                Map<String, Long> optionCountMap = countOptionSelections(allAnswers, item.getId());
                int optionCount = item.getOptions().size();
                int firstRowOfQuestion = rowNum;
                int lastRowOfQuestion = rowNum + optionCount - 1;

                // 寫入數據行
                for (int i = 0; i < optionCount; i++) {
                    String option = item.getOptions().get(i);
                    Row dataRow = sheet.createRow(rowNum++);
                    dataRow.setHeightInPoints(20);

                    // 題目列（合併單元格）- 只在第一行創建
                    if (i == 0) {
                        Cell questionCell = dataRow.createCell(0);
                        questionCell.setCellValue(item.getTitle());
                        questionCell.setCellStyle(dataStyle);

                        // 爲合併區域的第一列所有行創建單元格並設置樣式
                        for (int r = firstRowOfQuestion; r < lastRowOfQuestion; r++) {
                            Row mergeRow = sheet.getRow(r);
                            if (mergeRow == null) {
                                mergeRow = sheet.createRow(r);
                            }
                            Cell mergeCell = mergeRow.getCell(0);
                            if (mergeCell == null) {
                                mergeCell = mergeRow.createCell(0);
                            }
                            mergeCell.setCellStyle(dataStyle);
                        }

                        // 合併單元格
                        if (lastRowOfQuestion > firstRowOfQuestion) {
                            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                                    firstRowOfQuestion, lastRowOfQuestion, 0, 0));
                        }
                    }

                    // 選項列
                    Cell optionCell = dataRow.createCell(1);
                    optionCell.setCellValue(option);
                    optionCell.setCellStyle(dataStyle);

                    // 已選人數列
                    Cell countCell = dataRow.createCell(2);
                    countCell.setCellValue(optionCountMap.getOrDefault(option, 0L));
                    countCell.setCellStyle(dataStyle);
                }
            }
        }

        // 設置列寬
        sheet.setColumnWidth(0, 8000);
        sheet.setColumnWidth(1, 8000);
        sheet.setColumnWidth(2, 3000);

        // 爲所有數據行設置邊框（包括合併單元格）
        int startDataRow = 5; // 從第6行開始（索引5）
        int endDataRow = rowNum - 1;
        for (int r = startDataRow; r <= endDataRow; r++) {
            Row row = sheet.getRow(r);
            if (row != null) {
                for (int c = 0; c < 3; c++) {
                    Cell cell = row.getCell(c);
                    if (cell == null) {
                        cell = row.createCell(c);
                    }
                    cell.setCellStyle(dataStyle);
                }
            }
        }
    }

    /**
     * 創建詳情Sheet
     */
    private void createDetailSheet(Workbook workbook, Notification notification,
                                   NotificationSendRecord sendRecord,
                                   List<NotificationQuestion> questions,
                                   List<NotificationAnswer> allAnswers,
                                   List<NotificationUserReadRecord> readRecords) {
        Sheet sheet = workbook.createSheet("詳情");

        // 創建樣式
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle titleStyle = createTitleStyle(workbook);

        int rowNum = 0;
        int colNum = 0;

        // 解析所有問題
        List<QuestionItemVO> allQuestionItems = new ArrayList<>();
        for (NotificationQuestion question : questions) {
            allQuestionItems.addAll(parseQuestionContent(question));
        }

        // 第1-2行：合併單元格顯示問卷資訊（與統計Sheet一致）
        Row headerInfoRow = sheet.createRow(rowNum);
        headerInfoRow.setHeightInPoints(80);
        Cell headerInfoCell = headerInfoRow.createCell(0);

        // 創建合併單元格
        int totalCount = sendRecord != null ? sendRecord.getTotalCount() : 0;
        // 已處理數
        long processedCount = allAnswers.stream()
                .map(NotificationAnswer::getUserId)
                .distinct()
                .count();

        // 構建多行文本
        String infoBuilder = notification.getTitle() + "\n" +
                "發送時間：" + formatDate(notification.getCreateTime()) + "\n" +
                "接收人數：" + totalCount + "\n" +
                "已處理人數：" + processedCount;

        headerInfoCell.setCellValue(infoBuilder);
        headerInfoCell.setCellStyle(titleStyle);

        // 計算需要合併的列數（固定6列 + 所有問題的選項數）
        int totalOptionCols = 6; // 班級、姓名、關係、發送狀態、閱讀時間、確認時間
        for (QuestionItemVO item : allQuestionItems) {
            totalOptionCols += item.getOptions().size();
        }

        // 合併 A1:第N列1-2行 (行0-1, 列0到totalOptionCols-1)
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 1, 0, totalOptionCols - 1));
        setRegionBorder(sheet, 0, 1, 0, totalOptionCols - 1, titleStyle);

        // 跳過已合併的行
        rowNum = 2;

        // 第3行：表頭（固定列合併2行 + 問題標題合併單元格 + 選項）
        Row headerRow = sheet.createRow(rowNum);
        headerRow.setHeightInPoints(35); // 增加行高以容納多行文本
        List<String> fixedHeaders = Arrays.asList("班級", "姓名", "關係", "發送狀態", "閱讀時間", "確認時間");

        // 創建固定列表頭（合併2行：表頭行和選項行）
        for (String header : fixedHeaders) {
            Cell cell = headerRow.createCell(colNum);
            cell.setCellValue(header);
            cell.setCellStyle(headerStyle);
            colNum++;
        }

        // 添加問題標題和問題選項作爲表頭
        for (QuestionItemVO item : allQuestionItems) {
            int optionCount = item.getOptions().size();
            int firstCol = colNum;
            int lastCol = colNum + optionCount - 1;

            // 問題標題（合併單元格）
            Cell titleCell = headerRow.createCell(firstCol);
            titleCell.setCellValue(item.getTitle());
            titleCell.setCellStyle(headerStyle);

            // 合併問題標題單元格
            if (lastCol > firstCol) {
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum, rowNum, firstCol, lastCol));
            }

            // 爲合併區域的標題列所有列設置樣式
            for (int c = firstCol; c <= lastCol; c++) {
                Cell cell = headerRow.getCell(c);
                if (cell == null) {
                    cell = headerRow.createCell(c);
                }
                cell.setCellStyle(headerStyle);
            }

            colNum = lastCol + 1;
        }

        // 第4行：問題選項作爲第二行表頭（固定列已合併，不再創建）
        Row optionHeaderRow = sheet.createRow(rowNum + 1);
        optionHeaderRow.setHeightInPoints(35);

        colNum = 6;
        for (QuestionItemVO item : allQuestionItems) {
            for (String option : item.getOptions()) {
                Cell cell = optionHeaderRow.createCell(colNum++);
                cell.setCellValue(option);
                cell.setCellStyle(headerStyle);
            }
        }

        // 合併固定列的2行（rowNum 到 rowNum+1）並設置邊框
        for (int i = 0; i < 6; i++) {
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum, rowNum + 1, i, i));
            setRegionBorder(sheet, rowNum, rowNum + 1, i, i, headerStyle);
        }

        // 爲問題標題合併區域設置邊框
        int borderColNum = 6;
        for (QuestionItemVO item : allQuestionItems) {
            int optionCount = item.getOptions().size();
            int firstCol = borderColNum;
            int lastCol = borderColNum + optionCount - 1;

            if (lastCol > firstCol) {
                setRegionBorder(sheet, rowNum, rowNum, firstCol, lastCol, headerStyle);
            }

            borderColNum = lastCol + 1;
        }

        // 移動到下一行
        rowNum += 2;

        // 查詢所有家長學生關係（使用家長ID和學生ID組合查詢）
        List<String> allParentUserIds = readRecords.stream()
                .map(NotificationUserReadRecord::getUserId)
                .distinct()
                .collect(Collectors.toList());

        // 使用組合鍵 parentUserId_departmentId 存儲 WeCom 關係
        Map<String, SysSchoolFamilyContact> relationMap = new HashMap<>();
        Map<String, SysSchoolDepartmentMember> customMemberMap = new HashMap<>();
        if (!allParentUserIds.isEmpty()) {
            List<SysSchoolFamilyContact> relations = schoolFamilyContactMapper.selectByParentUserIds(
                    allParentUserIds);
            for (SysSchoolFamilyContact relation : relations) {
                String key = parentDeptKey(relation.getParentUserId(), relation.getDepartmentId());
                relationMap.put(key, relation);
            }
            List<SysSchoolDepartmentMember> customMembers =
                    schoolDepartmentMemberMapper.selectMembersByUserids(allParentUserIds);
            if (customMembers != null) {
                for (SysSchoolDepartmentMember member : customMembers) {
                    if (member.getUserid() == null || member.getDepartmentId() == null) {
                        continue;
                    }
                    customMemberMap.put(parentDeptKey(member.getUserid(), member.getDepartmentId()), member);
                }
            }
        }

        Map<Long, String> deptIdNameMap = new HashMap<>();
        List<SysDepartment> allDepartments = departmentMapper.selectAll();
        if (allDepartments != null) {
            for (SysDepartment department : allDepartments) {
                if (department.getId() != null) {
                    deptIdNameMap.put(department.getId(), department.getName());
                }
            }
        }
        List<SysSchoolDepartment> schoolDepartmentsType1 = schoolDepartmentMapper.selectAll(1);
        if (schoolDepartmentsType1 != null) {
            for (SysSchoolDepartment department : schoolDepartmentsType1) {
                if (department.getId() != null) {
                    deptIdNameMap.putIfAbsent(department.getId(), department.getName());
                }
            }
        }
        List<SysSchoolDepartment> schoolDepartmentsType2 = schoolDepartmentMapper.selectAll(2);
        if (schoolDepartmentsType2 != null) {
            for (SysSchoolDepartment department : schoolDepartmentsType2) {
                if (department.getId() != null) {
                    deptIdNameMap.putIfAbsent(department.getId(), department.getName());
                }
            }
        }

        // 按 parentUserId + student_id 分組答案
        Map<String, List<NotificationAnswer>> answersByUser = new HashMap<>();
        for (NotificationAnswer answer : allAnswers) {
            String key = answer.getUserId() + "_"
                    + (answer.getStudentId() != null ? answer.getStudentId() : "");
            answersByUser.computeIfAbsent(key, k -> new ArrayList<>()).add(answer);
        }

        // 記錄數據開始行
        int dataStartRow = rowNum;

        // 寫入數據行
        for (NotificationUserReadRecord record : readRecords) {
            Row dataRow = sheet.createRow(rowNum++);
            colNum = 0;

            // 按 parentUserId + departmentId 查詢 WeCom 關係；自定義家校 fallback 到成員表
            String relationKey = parentDeptKey(record.getUserId(), record.getDepartmentId());
            SysSchoolFamilyContact relation = relationMap.get(relationKey);
            String studentName = "";
            String relationDesc = "";
            if (relation != null) {
                studentName = relation.getStudentName() != null ? relation.getStudentName() : "";
                relationDesc = relation.getRelationDesc() != null ? relation.getRelationDesc() : "";
            } else {
                SysSchoolDepartmentMember customMember = customMemberMap.get(relationKey);
                if (customMember != null && customMember.getName() != null) {
                    studentName = customMember.getName();
                }
            }

            // 班級：直接讀取閱讀記錄中的 department_id
            String className = resolveClassName(record, deptIdNameMap);
            dataRow.createCell(colNum++).setCellValue(className);

            // 姓名
            dataRow.createCell(colNum++).setCellValue(studentName);

            // 關係
            dataRow.createCell(colNum++).setCellValue(relationDesc);

            // 發送狀態（1=發送成功，0=發送失敗）
            String sendStatusText = getString(record);
            dataRow.createCell(colNum++).setCellValue(sendStatusText);

            // 閱讀時間
            dataRow.createCell(colNum++).setCellValue(formatDate(record.getReadTime()));

            // 確認時間
            dataRow.createCell(colNum++).setCellValue(formatDate(record.getReplyTime()));

            // 按 parentUserId + student_id 獲取該家長-學生對的答案
            String answerKey = record.getUserId() + "_"
                    + (record.getStudentId() != null ? record.getStudentId() : "");
            List<NotificationAnswer> userAnswers = answersByUser.getOrDefault(answerKey, new ArrayList<>());

            // 對於每個問題項，從用戶答案中查找匹配的選項
            for (QuestionItemVO item : allQuestionItems) {
                List<String> selectedOptions = new ArrayList<>();

                // 遍歷用戶的所有答案，查找匹配的nodeId
                for (NotificationAnswer answer : userAnswers) {
                    if (answer.getAnswerData() != null) {
                        try {
                            JSONArray answerArray = JSON.parseArray(answer.getAnswerData());
                            for (int i = 0; i < answerArray.size(); i++) {
                                JSONObject answerObj = answerArray.getJSONObject(i);
                                String nodeId = answerObj.getString("nodeId");

                                // 匹配 nodeId
                                if (String.valueOf(item.getId()).equals(nodeId)) {
                                    String answerContent = answerObj.getString("answerContent");
                                    if (answerContent != null) {
                                        selectedOptions = JSON.parseArray(answerContent, String.class);
                                    }
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            log.error("解析答案數據失敗", e);
                        }
                    }
                }

                // 標記選中的選項或填寫的答案
                for (String option : item.getOptions()) {
                    Cell cell = dataRow.createCell(colNum++);

                    // 判斷是否爲填空題（type="3"）
                    if ("3".equals(item.getType())) {
                        // 填空題：解析新的答案格式並顯示對應的值
                        String answerText = parseFillBlankAnswer(userAnswers, item.getId());
                        cell.setCellValue(answerText);
                    } else {
                        // 選擇題：標記選中的選項
                        if (selectedOptions.contains(option)) {
                            cell.setCellValue("√");
                        } else {
                            cell.setCellValue("");
                        }
                    }
                    cell.setCellStyle(dataStyle);
                }
            }
        }

        // 設置列寬（增加寬度）
        sheet.setColumnWidth(0, 5000);  // 班級
        sheet.setColumnWidth(1, 5000);  // 姓名
        sheet.setColumnWidth(2, 3500);  // 關係
        sheet.setColumnWidth(3, 3500);  // 發送狀態
        sheet.setColumnWidth(4, 5000);  // 閱讀時間
        sheet.setColumnWidth(5, 5000);  // 確認時間

        // 問題列的寬度
        int colIndex = 6;
        for (QuestionItemVO item : allQuestionItems) {
            for (int i = 0; i < item.getOptions().size(); i++) {
                sheet.setColumnWidth(colIndex++, 5000);
            }
        }

        // 爲所有數據行設置邊框
        for (int r = dataStartRow; r < rowNum; r++) {
            Row dataRow = sheet.getRow(r);
            if (dataRow != null) {
                int totalCols = 6; // 固定6列
                for (QuestionItemVO item : allQuestionItems) {
                    totalCols += item.getOptions().size();
                }

                for (int c = 0; c < totalCols; c++) {
                    Cell cell = dataRow.getCell(c);
                    if (cell != null) {
                        cell.setCellStyle(dataStyle);
                    } else {
                        // 如果單元格不存在，創建它並設置樣式
                        cell = dataRow.createCell(c);
                        cell.setCellStyle(dataStyle);
                    }
                }
            }
        }
    }

    private String parentDeptKey(String parentUserId, Long departmentId) {
        return parentUserId + "_" + (departmentId != null ? departmentId : "null");
    }

    /**
     * 解析班級名稱
     */
    private String resolveClassName(NotificationUserReadRecord record, Map<Long, String> deptIdNameMap) {
        if (record.getDepartmentId() == null) {
            return "";
        }
        return deptIdNameMap.getOrDefault(record.getDepartmentId(), "");
    }

    /**
     * 獲取發送狀態
     */
    private static String getString(NotificationUserReadRecord record) {
        String sendStatusText = "";
        if (record.getSendStatus() != null) {
            if ("1".equals(record.getSendStatus()) || "1".equals(String.valueOf(record.getSendStatus()))) {
                sendStatusText = "發送成功";
            } else if ("0".equals(record.getSendStatus()) || "0".equals(String.valueOf(record.getSendStatus()))) {
                sendStatusText = "發送失敗";
            } else {
                sendStatusText = record.getSendStatus();
            }
        }
        return sendStatusText;
    }

    /**
     * 解析問題內容，提取問題項
     */
    private List<QuestionItemVO> parseQuestionContent(NotificationQuestion question) {
        List<QuestionItemVO> items = new ArrayList<>();

        try {
            // 只處理邏輯表單類型（type="5"）
            if ("5".equals(question.getQuestionType()) && question.getContent() != null) {
                // 邏輯表單類型，解析content字段
                JSONObject contentJson = JSON.parseObject(question.getContent());
                JSONArray questionsArray = contentJson.getJSONArray("questions");

                if (questionsArray != null) {
                    for (int i = 0; i < questionsArray.size(); i++) {
                        JSONObject q = questionsArray.getJSONObject(i);
                        String type = q.getString("type");

                        QuestionItemVO item = new QuestionItemVO();
                        item.setId(q.getLong("id"));
                        item.setTitle(q.getString("title"));
                        item.setType(type);

                        // 處理填空題（type="3"）
                        if ("3".equals(type)) {
                            handleFillBlankQuestion(item, q.getString("content"), items);
                        } else {
                            // 其他題型：處理選項
                            JSONArray optionsArray = q.getJSONArray("options");
                            if (optionsArray != null && !optionsArray.isEmpty()) {
                                List<String> options = new ArrayList<>();
                                for (int j = 0; j < optionsArray.size(); j++) {
                                    options.add(optionsArray.getString(j));
                                }
                                item.setOptions(options);
                                // 只有有選項的問題才添加（過濾掉附件上傳、文本輸入等無選項題型）
                                items.add(item);
                            } else {
                                item.setOptions(new ArrayList<>());
                                // 沒有選項的問題不添加（如附件上傳、文本輸入等）
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析問題內容失敗，questionId: {}", question.getQuestionId(), e);
        }

        return items;
    }

    /**
     * 處理填空題：從content中提取文本，將{{fillblank-n}}佔位符替換爲回答n（n是序號）
     */
    private void handleFillBlankQuestion(QuestionItemVO item, String content, List<QuestionItemVO> items) {
        if (content != null && !content.isEmpty()) {
            // 提取所有fillblank的ID，按出現順序編號
            Pattern pattern = Pattern.compile("\\{\\{fillblank-(\\d+)}}");
            List<String> options = getStrings(content, pattern);
            item.setOptions(options);
            // 填空題有內容就添加
            if (!options.isEmpty()) {
                items.add(item);
            }
        } else {
            item.setOptions(new ArrayList<>());
        }
    }

    /**
     * 獲取內容中的字符串
     * @param content
     * @param pattern
     * @return
     */
    private static List<String> getStrings(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);

        int index = 1;
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "(答" + index++ + ")");
        }
        matcher.appendTail(sb);

        String optionText = sb.toString().trim();
        List<String> options = new ArrayList<>();
        if (!optionText.isEmpty()) {
            options.add(optionText);
        }
        return options;
    }

    /**
     * 統計選項選擇人數
     */
    private Map<String, Long> countOptionSelections(List<NotificationAnswer> answers, Long questionId) {
        Map<String, Long> countMap = new HashMap<>();
        String targetNodeId = String.valueOf(questionId);

        for (NotificationAnswer answer : answers) {
            try {
                if (answer.getAnswerData() != null) {
                    JSONArray answerArray = JSON.parseArray(answer.getAnswerData());
                    for (int i = 0; i < answerArray.size(); i++) {
                        JSONObject answerObj = answerArray.getJSONObject(i);
                        String nodeId = answerObj.getString("nodeId");

                        // 匹配 nodeId
                        if (targetNodeId.equals(nodeId)) {
                            String answerContent = answerObj.getString("answerContent");
                            if (answerContent != null) {
                                List<String> selectedOptions = JSON.parseArray(answerContent, String.class);
                                for (String option : selectedOptions) {
                                    countMap.put(option, countMap.getOrDefault(option, 0L) + 1);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("統計選項失敗", e);
            }
        }

        return countMap;
    }

    /**
     * 解析填空題答案，根據blankId匹配顯示對應的值
     * @param userAnswers 用戶的所有答案
     * @param questionId 問題ID（即nodeId）
     * @return 格式化的答案字符串，如："回答1：12，回答2：123，回答3：1234"
     */
    private String parseFillBlankAnswer(List<NotificationAnswer> userAnswers, Long questionId) {
        String targetNodeId = String.valueOf(questionId);

        for (NotificationAnswer answer : userAnswers) {
            if (answer.getAnswerData() != null) {
                try {
                    JSONArray answerArray = JSON.parseArray(answer.getAnswerData());
                    for (int i = 0; i < answerArray.size(); i++) {
                        JSONObject answerObj = answerArray.getJSONObject(i);
                        String nodeId = answerObj.getString("nodeId");

                        // 匹配 nodeId
                        if (targetNodeId.equals(nodeId)) {
                            String answerContent = answerObj.getString("answerContent");
                            if (answerContent != null && !answerContent.isEmpty()) {
                                // 解析新的答案格式：[{"blankId":"fillblank-xxx","value":"12"},...]
                                JSONArray fillBlanksArray = JSON.parseArray(answerContent);

                                StringBuilder result = new StringBuilder();
                                int index = 1;
                                for (int j = 0; j < fillBlanksArray.size(); j++) {
                                    JSONObject fillBlank = fillBlanksArray.getJSONObject(j);
                                    String value = fillBlank.getString("value");

                                    if (j > 0) {
                                        result.append("，");
                                    }
                                    result.append("答").append(index++).append("：").append(value != null ? value : "");
                                }

                                return result.toString();
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("解析填空題答案失敗", e);
                }
            }
        }

        return "";
    }

    /**
     * 創建標題樣式
     */
    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT); // 靠左對齊
        style.setVerticalAlignment(VerticalAlignment.CENTER); // 垂直置中
        style.setWrapText(true); // 自動換行
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 設置合併區域的邊框
     */
    private void setRegionBorder(Sheet sheet, int firstRow, int lastRow, int firstCol, int lastCol, CellStyle style) {
        org.apache.poi.ss.util.CellRangeAddress region = new org.apache.poi.ss.util.CellRangeAddress(
                firstRow, lastRow, firstCol, lastCol);

        // 創建帶邊框的樣式
        CellStyle borderStyle = sheet.getWorkbook().createCellStyle();
        borderStyle.cloneStyleFrom(style);
        borderStyle.setBorderTop(BorderStyle.THIN);
        borderStyle.setBorderBottom(BorderStyle.THIN);
        borderStyle.setBorderLeft(BorderStyle.THIN);
        borderStyle.setBorderRight(BorderStyle.THIN);

        // 爲合併區域內的所有單元格設置樣式
        for (int rowNum = firstRow; rowNum <= lastRow; rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) {
                row = sheet.createRow(rowNum);
            }
            for (int colNum = firstCol; colNum <= lastCol; colNum++) {
                Cell cell = row.getCell(colNum);
                if (cell == null) {
                    cell = row.createCell(colNum);
                }
                cell.setCellStyle(borderStyle);
            }
        }

        // 使用 RegionUtil 設置合併區域的邊框（確保外邊框完整）
        // 注意：RegionUtil 必須在設置完單元格樣式後調用，否則可能會被覆蓋
        org.apache.poi.ss.util.RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
        org.apache.poi.ss.util.RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
        org.apache.poi.ss.util.RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
        org.apache.poi.ss.util.RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
    }

    /**
     * 創建表頭樣式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true); // 自動換行
        return style;
    }

    /**
     * 創建數據樣式
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }

    /**
     * 格式化日期
     */
    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dateTime.format(formatter);
    }

}
