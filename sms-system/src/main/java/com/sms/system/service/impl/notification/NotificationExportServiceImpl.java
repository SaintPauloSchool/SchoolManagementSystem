package com.sms.system.service.impl.notification;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sms.system.entity.SysDepartment;
import com.sms.system.entity.SysDepartmentParentBinding;
import com.sms.system.entity.SysParentStudentRelation;
import com.sms.system.entity.notification.*;
import com.sms.system.entity.vo.QuestionItemVO;
import com.sms.system.mapper.SysDepartmentMapper;
import com.sms.system.mapper.SysDepartmentParentBindingMapper;
import com.sms.system.mapper.SysParentStudentRelationMapper;
import com.sms.system.mapper.notification.*;
import com.sms.system.service.notification.INotificationExportService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 通知导出 Service 业务层处理
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
    private SysParentStudentRelationMapper parentStudentRelationMapper;

    @Autowired
    private SysDepartmentParentBindingMapper departmentParentBindingMapper;

    @Autowired
    private SysDepartmentMapper departmentMapper;

    @Override
    public void exportNotificationAnswers(Long notificationId, HttpServletResponse response) {
        try {
            // 1. 查询通知基本信息
            Notification notification = notificationMapper.selectById(notificationId);
            if (notification == null) {
                log.error("通知不存在，notificationId: {}", notificationId);
                return;
            }

            // 2. 查询发送记录
            NotificationSendRecord sendRecord = notificationSendRecordMapper.selectByNotificationId(notificationId);
            Integer totalCount = sendRecord != null ? sendRecord.getTotalCount() : 0;

            // 3. 查询阅读记录
            List<NotificationUserReadRecord> readRecords = notificationUserReadRecordMapper.selectBySendRecordId(
                    sendRecord != null ? sendRecord.getSendRecordId() : null);

            // 4. 查询问题列表
            List<NotificationQuestion> questions = notificationQuestionMapper.selectByNotificationId(notificationId);

            // 5. 查询所有回答
            List<NotificationAnswer> allAnswers = notificationAnswerMapper.selectByNotificationId(notificationId);

            // 6. 统计已处理人数（去重后的userId）
            long processedCount = allAnswers.stream()
                    .map(NotificationAnswer::getUserId)
                    .distinct()
                    .count();

            // 7. 创建Excel工作簿
            Workbook workbook = new XSSFWorkbook();

            // 8. 创建统计Sheet
            createStatisticsSheet(workbook, notification, questions, allAnswers, totalCount, (int) processedCount);

            // 9. 创建详情Sheet
            createDetailSheet(workbook, notification, sendRecord, questions, allAnswers, readRecords);

            // 10. 导出Excel
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode(notification.getTitle() + "_回复统计", "UTF-8");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");

            try (OutputStream out = response.getOutputStream()) {
                workbook.write(out);
                out.flush();
            }

            workbook.close();
            log.info("导出通知回复答案成功，notificationId: {}", notificationId);

        } catch (Exception e) {
            log.error("导出通知回复答案失败，notificationId: {}", notificationId, e);
        }
    }

    /**
     * 创建统计Sheet
     */
    private void createStatisticsSheet(Workbook workbook, Notification notification,
                                       List<NotificationQuestion> questions,
                                       List<NotificationAnswer> allAnswers,
                                       Integer totalCount, Integer processedCount) {
        Sheet sheet = workbook.createSheet("统计");

        // 创建样式
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle titleStyle = createTitleStyle(workbook);

        int rowNum = 0;

        // 第1-4行：合并单元格显示问卷信息
        Row headerInfoRow = sheet.createRow(rowNum);
        headerInfoRow.setHeightInPoints(100); // 设置较高的行高以容纳4行内容
        Cell headerInfoCell = headerInfoRow.createCell(0);
        
        // 构建多行文本
        String infoBuilder = notification.getTitle() + "\n" +
                "发送时间：" + formatDate(notification.getCreateTime()) + "\n" +
                "接收人数：" + totalCount + "\n" +
                "已处理人数：" + processedCount;
        
        headerInfoCell.setCellValue(infoBuilder);
        headerInfoCell.setCellStyle(titleStyle);
        
        // 合并 A1:C4 (行0-3, 列0-2)
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 3, 0, 2));
        // 为合并区域添加边框
        setRegionBorder(sheet, 0, 3, 0, 2, titleStyle);
        
        // 跳过已合并的行
        rowNum = 4;
        // 表头行
        Row headerRow = sheet.createRow(rowNum++);
        headerRow.setHeightInPoints(25);
        String[] headers = {"题目", "选项", "已选人数"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 第5行起：统计数据
        for (NotificationQuestion question : questions) {
            // 解析题目数据
            List<QuestionItemVO> questionItems = parseQuestionContent(question);

            for (QuestionItemVO item : questionItems) {
                // 统计每个选项的选择人数
                Map<String, Long> optionCountMap = countOptionSelections(allAnswers, item.getId());
                int optionCount = item.getOptions().size();
                int firstRowOfQuestion = rowNum;
                int lastRowOfQuestion = rowNum + optionCount - 1;

                // 写入数据行
                for (int i = 0; i < optionCount; i++) {
                    String option = item.getOptions().get(i);
                    Row dataRow = sheet.createRow(rowNum++);
                    dataRow.setHeightInPoints(20);

                    // 题目列（合并单元格）- 只在第一行创建
                    if (i == 0) {
                        Cell questionCell = dataRow.createCell(0);
                        questionCell.setCellValue(item.getTitle());
                        questionCell.setCellStyle(dataStyle);

                        // 为合并区域的第一列所有行创建单元格并设置样式
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

                        // 合并单元格
                        if (lastRowOfQuestion > firstRowOfQuestion) {
                            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                                    firstRowOfQuestion, lastRowOfQuestion, 0, 0));
                        }
                    }

                    // 选项列
                    Cell optionCell = dataRow.createCell(1);
                    optionCell.setCellValue(option);
                    optionCell.setCellStyle(dataStyle);

                    // 已选人数列
                    Cell countCell = dataRow.createCell(2);
                    countCell.setCellValue(optionCountMap.getOrDefault(option, 0L));
                    countCell.setCellStyle(dataStyle);
                }
            }
        }

        // 设置列宽
        sheet.setColumnWidth(0, 8000);
        sheet.setColumnWidth(1, 8000);
        sheet.setColumnWidth(2, 3000);
        
        // 为所有数据行设置边框（包括合并单元格）
        int startDataRow = 5; // 从第6行开始（索引5）
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
     * 创建详情Sheet
     */
    private void createDetailSheet(Workbook workbook, Notification notification,
                                   NotificationSendRecord sendRecord,
                                   List<NotificationQuestion> questions,
                                   List<NotificationAnswer> allAnswers,
                                   List<NotificationUserReadRecord> readRecords) {
        Sheet sheet = workbook.createSheet("详情");

        // 创建样式
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle titleStyle = createTitleStyle(workbook);

        int rowNum = 0;
        int colNum = 0;

        // 解析所有问题
        List<QuestionItemVO> allQuestionItems = new ArrayList<>();
        for (NotificationQuestion question : questions) {
            allQuestionItems.addAll(parseQuestionContent(question));
        }

        // 第1-2行：合并单元格显示问卷信息（与统计Sheet一致）
        Row headerInfoRow = sheet.createRow(rowNum);
        headerInfoRow.setHeightInPoints(80);
        Cell headerInfoCell = headerInfoRow.createCell(0);

        // 创建合并单元格
        int totalCount = sendRecord != null ? sendRecord.getTotalCount() : 0;
        // 已处理数
        long processedCount = allAnswers.stream()
                .map(NotificationAnswer::getUserId)
                .distinct()
                .count();
        
        // 构建多行文本
        String infoBuilder = notification.getTitle() + "\n" +
                "发送时间：" + formatDate(notification.getCreateTime()) + "\n" +
                "接收人数：" + totalCount + "\n" +
                "已处理人数：" + processedCount;
        
        headerInfoCell.setCellValue(infoBuilder);
        headerInfoCell.setCellStyle(titleStyle);
        
        // 计算需要合并的列数（固定5列 + 所有问题的选项数）
        int totalOptionCols = 5; // 姓名、班级、发送状态、阅读时间、确认时间
        for (QuestionItemVO item : allQuestionItems) {
            totalOptionCols += item.getOptions().size();
        }
        
        // 合并 A1:第N列1-2行 (行0-1, 列0到totalOptionCols-1)
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 1, 0, totalOptionCols - 1));
        setRegionBorder(sheet, 0, 1, 0, totalOptionCols - 1, titleStyle);
        
        // 跳过已合并的行
        rowNum = 2;

        // 第3行：表头（固定列合并2行 + 问题标题合并单元格 + 选项）
        Row headerRow = sheet.createRow(rowNum);
        headerRow.setHeightInPoints(35); // 增加行高以容纳多行文本
        List<String> fixedHeaders = Arrays.asList("姓名", "班级", "发送状态", "阅读时间", "确认时间");
        
        // 创建固定列表头（合并2行：表头行和选项行）
        for (String header : fixedHeaders) {
            Cell cell = headerRow.createCell(colNum);
            cell.setCellValue(header);
            cell.setCellStyle(headerStyle);
            colNum++;
        }

        // 添加问题标题和问题选项作为表头
        for (QuestionItemVO item : allQuestionItems) {
            int optionCount = item.getOptions().size();
            int firstCol = colNum;
            int lastCol = colNum + optionCount - 1;
            
            // 问题标题（合并单元格）
            Cell titleCell = headerRow.createCell(firstCol);
            titleCell.setCellValue(item.getTitle());
            titleCell.setCellStyle(headerStyle);
            
            // 合并问题标题单元格
            if (lastCol > firstCol) {
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum, rowNum, firstCol, lastCol));
            }
            
            // 为合并区域的标题列所有列设置样式
            for (int c = firstCol; c <= lastCol; c++) {
                Cell cell = headerRow.getCell(c);
                if (cell == null) {
                    cell = headerRow.createCell(c);
                }
                cell.setCellStyle(headerStyle);
            }
            
            colNum = lastCol + 1;
        }

        // 第4行：问题选项作为第二行表头（固定列已合并，不再创建）
        Row optionHeaderRow = sheet.createRow(rowNum + 1);
        optionHeaderRow.setHeightInPoints(35);
        
        colNum = 5;
        for (QuestionItemVO item : allQuestionItems) {
            for (String option : item.getOptions()) {
                Cell cell = optionHeaderRow.createCell(colNum++);
                cell.setCellValue(option);
                cell.setCellStyle(headerStyle);
            }
        }
        
        // 合并固定列的2行（rowNum 到 rowNum+1）并设置边框
        for (int i = 0; i < 5; i++) {
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum, rowNum + 1, i, i));
            setRegionBorder(sheet, rowNum, rowNum + 1, i, i, headerStyle);
        }
        
        // 为问题标题合并区域设置边框
        int borderColNum = 5;
        for (QuestionItemVO item : allQuestionItems) {
            int optionCount = item.getOptions().size();
            int firstCol = borderColNum;
            int lastCol = borderColNum + optionCount - 1;
            
            if (lastCol > firstCol) {
                setRegionBorder(sheet, rowNum, rowNum, firstCol, lastCol, headerStyle);
            }
            
            borderColNum = lastCol + 1;
        }
        
        // 移动到下一行
        rowNum += 2;

        // 查询所有家长学生关系
        List<String> allUserIds = readRecords.stream()
                .map(NotificationUserReadRecord::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<String, SysParentStudentRelation> relationMap = new HashMap<>();
        if (!allUserIds.isEmpty()) {
            List<SysParentStudentRelation> relations = parentStudentRelationMapper.selectByParentUserIds(allUserIds);
            relationMap = relations.stream()
                    .collect(Collectors.toMap(SysParentStudentRelation::getParentUserId, r -> r, (r1, r2) -> r1));
        }

        // 查询部门绑定关系
        Map<String, String> departmentMap = new HashMap<>();
        if (!allUserIds.isEmpty()) {
            // 查询所有班级部门
            List<SysDepartment> allDepartments = departmentMapper.selectAll();
            Map<Long, String> deptIdNameMap = allDepartments.stream()
                    .collect(Collectors.toMap(SysDepartment::getId, SysDepartment::getName, (d1, d2) -> d1));

            // 根据班级ID查询家长绑定关系
            List<Long> deptIds = allDepartments.stream()
                    .filter(d -> d.getType() != null && d.getType() == 1) // type=1是班级
                    .map(SysDepartment::getId)
                    .collect(Collectors.toList());

            if (!deptIds.isEmpty()) {
                List<SysDepartmentParentBinding> bindings = departmentParentBindingMapper.selectByDepartmentIds(deptIds);
                for (SysDepartmentParentBinding binding : bindings) {
                    String deptName = deptIdNameMap.get(binding.getDepartmentId());
                    if (deptName != null) {
                        departmentMap.put(binding.getParentUserId(), deptName);
                    }
                }
            }
        }

        // 按用户分组答案
        Map<String, List<NotificationAnswer>> answersByUser = allAnswers.stream()
                .collect(Collectors.groupingBy(NotificationAnswer::getUserId));

        // 记录数据开始行
        int dataStartRow = rowNum;

        // 写入数据行
        for (NotificationUserReadRecord record : readRecords) {
            Row dataRow = sheet.createRow(rowNum++);
            colNum = 0;

            // 姓名：student_name - relation_desc
            SysParentStudentRelation relation = relationMap.get(record.getUserId());
            String name = "";
            if (relation != null) {
                name = (relation.getStudentName() != null ? relation.getStudentName() : "") + "-" +
                        (relation.getRelationDesc() != null ? relation.getRelationDesc() : "");
            }
            dataRow.createCell(colNum++).setCellValue(name);

            // 班级
            String className = departmentMap.getOrDefault(record.getUserId(), "");
            dataRow.createCell(colNum++).setCellValue(className);

            // 发送状态（1=发送成功，0=发送失败）
            String sendStatusText = getString(record);
            dataRow.createCell(colNum++).setCellValue(sendStatusText);

            // 阅读时间
            dataRow.createCell(colNum++).setCellValue(formatDate(record.getReadTime()));

            // 确认时间
            dataRow.createCell(colNum++).setCellValue(formatDate(record.getReplyTime()));

            // 问题答案
            List<NotificationAnswer> userAnswers = answersByUser.getOrDefault(record.getUserId(), new ArrayList<>());
            
            // 对于每个问题项，从用户答案中查找匹配的选项
            for (QuestionItemVO item : allQuestionItems) {
                List<String> selectedOptions = new ArrayList<>();
                
                // 遍历用户的所有答案，查找匹配的nodeId
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
                            log.error("解析答案数据失败", e);
                        }
                    }
                }

                // 标记选中的选项
                for (String option : item.getOptions()) {
                    Cell cell = dataRow.createCell(colNum++);
                    if (selectedOptions.contains(option)) {
                        cell.setCellValue("√");
                    } else {
                        cell.setCellValue("");
                    }
                    cell.setCellStyle(dataStyle);
                }
            }
        }

        // 设置列宽（增加宽度）
        sheet.setColumnWidth(0, 5000);  // 姓名
        sheet.setColumnWidth(1, 5000);  // 班级
        sheet.setColumnWidth(2, 3500);  // 发送状态
        sheet.setColumnWidth(3, 5000);  // 阅读时间
        sheet.setColumnWidth(4, 5000);  // 确认时间
        
        // 问题列的宽度
        int colIndex = 5;
        for (QuestionItemVO item : allQuestionItems) {
            for (int i = 0; i < item.getOptions().size(); i++) {
                sheet.setColumnWidth(colIndex++, 5000);
            }
        }
        
        // 为所有数据行设置边框
        for (int r = dataStartRow; r < rowNum; r++) {
            Row dataRow = sheet.getRow(r);
            if (dataRow != null) {
                int totalCols = 5; // 固定5列
                for (QuestionItemVO item : allQuestionItems) {
                    totalCols += item.getOptions().size();
                }
                
                for (int c = 0; c < totalCols; c++) {
                    Cell cell = dataRow.getCell(c);
                    if (cell != null) {
                        cell.setCellStyle(dataStyle);
                    } else {
                        // 如果单元格不存在，创建它并设置样式
                        cell = dataRow.createCell(c);
                        cell.setCellStyle(dataStyle);
                    }
                }
            }
        }
    }

    /**
     * 獲取發送狀態
     */
    private static String getString(NotificationUserReadRecord record) {
        String sendStatusText = "";
        if (record.getSendStatus() != null) {
            if ("1".equals(record.getSendStatus()) || "1".equals(String.valueOf(record.getSendStatus()))) {
                sendStatusText = "发送成功";
            } else if ("0".equals(record.getSendStatus()) || "0".equals(String.valueOf(record.getSendStatus()))) {
                sendStatusText = "发送失败";
            } else {
                sendStatusText = record.getSendStatus();
            }
        }
        return sendStatusText;
    }

    /**
     * 解析问题内容，提取问题项
     */
    private List<QuestionItemVO> parseQuestionContent(NotificationQuestion question) {
        List<QuestionItemVO> items = new ArrayList<>();

        try {
            if ("5".equals(question.getQuestionType()) && question.getContent() != null) {
                // 逻辑表单类型，解析content字段
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

                        JSONArray optionsArray = q.getJSONArray("options");
                        if (optionsArray != null && !optionsArray.isEmpty()) {
                            List<String> options = new ArrayList<>();
                            for (int j = 0; j < optionsArray.size(); j++) {
                                options.add(optionsArray.getString(j));
                            }
                            item.setOptions(options);
                            // 只有有选项的问题才添加（过滤掉附件上传、文本输入等无选项题型）
                            items.add(item);
                        } else {
                            item.setOptions(new ArrayList<>());
                            // 没有选项的问题不添加（如附件上传、文本输入等）
                        }
                    }
                }
            } else {
                // 普通题型
                QuestionItemVO item = new QuestionItemVO();
                item.setId(question.getQuestionId());
                item.setTitle(question.getQuestionTitle());
                item.setType(question.getQuestionType());

                if (question.getOptions() != null) {
                    List<String> options = JSON.parseArray(question.getOptions(), String.class);
                    item.setOptions(options);
                    // 只有有选项的问题才添加（过滤掉附件上传、文本输入等无选项题型）
                    if (!options.isEmpty()) {
                        items.add(item);
                    }
                } else {
                    item.setOptions(new ArrayList<>());
                    // 没有选项的问题不添加
                }
            }
        } catch (Exception e) {
            log.error("解析问题内容失败，questionId: {}", question.getQuestionId(), e);
        }

        return items;
    }

    /**
     * 统计选项选择人数
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
                log.error("统计选项失败", e);
            }
        }

        return countMap;
    }

    /**
     * 创建标题样式
     */
    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT); // 靠左对齐
        style.setVerticalAlignment(VerticalAlignment.CENTER); // 垂直置中
        style.setWrapText(true); // 自动换行
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 设置合并区域的边框
     */
    private void setRegionBorder(Sheet sheet, int firstRow, int lastRow, int firstCol, int lastCol, CellStyle style) {
        org.apache.poi.ss.util.CellRangeAddress region = new org.apache.poi.ss.util.CellRangeAddress(
                firstRow, lastRow, firstCol, lastCol);
        
        // 创建带边框的样式
        CellStyle borderStyle = sheet.getWorkbook().createCellStyle();
        borderStyle.cloneStyleFrom(style);
        borderStyle.setBorderTop(BorderStyle.THIN);
        borderStyle.setBorderBottom(BorderStyle.THIN);
        borderStyle.setBorderLeft(BorderStyle.THIN);
        borderStyle.setBorderRight(BorderStyle.THIN);
        
        // 为合并区域内的所有单元格设置样式
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
        
        // 使用 RegionUtil 设置合并区域的边框（确保外边框完整）
        // 注意：RegionUtil 必须在设置完单元格样式后调用，否则可能会被覆盖
        org.apache.poi.ss.util.RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
        org.apache.poi.ss.util.RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
        org.apache.poi.ss.util.RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
        org.apache.poi.ss.util.RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
    }

    /**
     * 创建表头样式
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
        style.setWrapText(true); // 自动换行
        return style;
    }

    /**
     * 创建数据样式
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
    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

}
