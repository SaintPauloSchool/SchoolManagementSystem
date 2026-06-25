package com.sms.system.service.impl;

import com.sms.system.entity.CalendarEvent;
import com.sms.system.entity.dto.CalendarEventDeleteDTO;
import com.sms.system.entity.dto.CalendarEventQueryDTO;
import com.sms.system.entity.dto.CalendarEventSaveDTO;
import com.sms.system.entity.vo.CalendarEventVO;
import com.sms.system.mapper.CalendarEventMapper;
import com.sms.system.service.ICalendarEventService;
import com.sms.common.utils.bean.BeanCopyUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class CalendarEventServiceImpl implements ICalendarEventService {

    @Autowired
    private CalendarEventMapper calendarEventMapper;

    @Override
    public CalendarEventVO selectCalendarEventByEventId(Long eventId) {
        CalendarEvent entity = calendarEventMapper.selectCalendarEventByEventId(eventId);
        return BeanCopyUtils.copy(entity, CalendarEventVO.class);
    }

    @Override
    public List<CalendarEventVO> selectCalendarEventList(CalendarEventQueryDTO calendarEventQueryDTO) {
        CalendarEvent calendarEventQuery = BeanCopyUtils.copy(calendarEventQueryDTO, CalendarEvent.class);
        List<CalendarEvent> calendarEventList = calendarEventMapper.selectCalendarEventList(calendarEventQuery);
        return BeanCopyUtils.copyPageList(calendarEventList, CalendarEventVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertCalendarEvent(CalendarEventSaveDTO calendarEventSaveDTO, String createBy) {
        CalendarEvent calendarEvent = BeanCopyUtils.copy(calendarEventSaveDTO, CalendarEvent.class);
        calendarEvent.setCreateBy(createBy);
        calendarEvent.setCreateTime(LocalDateTime.now());
        return calendarEventMapper.insertCalendarEvent(calendarEvent);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertCalendarEventBatch(List<CalendarEventSaveDTO> calendarEventSaveDTOList, String createBy) {
        LocalDateTime now = LocalDateTime.now();
        List<CalendarEvent> calendarEventList = new ArrayList<>(calendarEventSaveDTOList.size());
        for (CalendarEventSaveDTO calendarEventSaveDTO : calendarEventSaveDTOList) {
            CalendarEvent calendarEvent = BeanCopyUtils.copy(calendarEventSaveDTO, CalendarEvent.class);
            calendarEvent.setCreateBy(createBy);
            calendarEvent.setCreateTime(now);
            calendarEventList.add(calendarEvent);
        }
        return calendarEventMapper.insertCalendarEventBatch(calendarEventList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateCalendarEvent(CalendarEventSaveDTO calendarEventSaveDTO, String updateBy) {
        CalendarEvent calendarEvent = BeanCopyUtils.copy(calendarEventSaveDTO, CalendarEvent.class);
        calendarEvent.setUpdateBy(updateBy);
        calendarEvent.setUpdateTime(LocalDateTime.now());
        return calendarEventMapper.updateCalendarEvent(calendarEvent);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCalendarEventByEventIds(CalendarEventDeleteDTO calendarEventDeleteDTO) {
        return calendarEventMapper.deleteCalendarEventByEventIds(calendarEventDeleteDTO.getEventIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String importCalendarEvent(MultipartFile file, String operName) throws Exception {
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            List<CalendarEvent> eventList = new ArrayList<>();

            for (int i = 2; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                try {
                    CalendarEvent event = new CalendarEvent();

                    Cell dateCell = row.getCell(0);
                    if (dateCell != null) {
                        if (dateCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dateCell)) {
                            event.setEventDate(dateCell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                        } else {
                            String dateStr = getCellValueAsString(dateCell);
                            if (dateStr != null && !dateStr.trim().isEmpty()) {
                                event.setEventDate(LocalDate.parse(dateStr.trim()));
                            }
                        }
                    }

                    Cell titleCell = row.getCell(1);
                    if (titleCell != null) {
                        event.setTitle(getCellValueAsString(titleCell));
                    }

                    if (event.getTitle() == null || event.getTitle().trim().isEmpty() || event.getEventDate() == null) {
                        failureNum++;
                        failureMsg.append("<br/>第 ").append(i + 1).append(" 行導入失敗：日期和標題不能為空（請確認格式：YYYY-MM-DD）");
                        continue;
                    }

                    Cell typeCell = row.getCell(2);
                    String typeStr = getCellValueAsString(typeCell);
                    int targetType = 0;
                    if (typeStr != null) {
                        typeStr = typeStr.trim();
                        if (typeStr.contains("幼稚園") || typeStr.equalsIgnoreCase("K")) {
                            targetType = 1;
                        } else if (typeStr.contains("小學") || typeStr.equalsIgnoreCase("P")) {
                            targetType = 2;
                        } else if (typeStr.contains("中學") || typeStr.equalsIgnoreCase("S") || typeStr.equalsIgnoreCase("F")) {
                            targetType = 3;
                        } else if (typeStr.matches("\\d+")) {
                            targetType = Integer.parseInt(typeStr);
                        }
                    }
                    event.setTargetType(targetType);

                    Cell remarkCell = row.getCell(3);
                    if (remarkCell != null) {
                        event.setRemark(getCellValueAsString(remarkCell));
                    }

                    event.setCreateBy(operName);
                    event.setCreateTime(LocalDateTime.now());

                    eventList.add(event);
                    successNum++;
                } catch (Exception e) {
                    failureNum++;
                    failureMsg.append("<br/>第 ").append(i + 1).append(" 行導入失敗：").append(e.getMessage() != null ? e.getMessage() : "格式錯誤");
                }
            }

            if (!eventList.isEmpty()) {
                calendarEventMapper.insertCalendarEventBatch(eventList);
            }
        } catch (Exception e) {
            throw new Exception("Excel 格式錯誤或無法讀取: " + e.getMessage());
        }

        if (failureNum > 0) {
            failureMsg.insert(0, "很抱歉，導入失敗！共 " + failureNum + " 筆數據格式不正確，錯誤如下：");
            throw new Exception(failureMsg.toString());
        } else {
            successMsg.insert(0, "恭喜您，數據已全部導入成功！共 " + successNum + " 筆");
        }
        return successMsg.toString();
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                double num = cell.getNumericCellValue();
                if (num == Math.floor(num)) {
                    return String.valueOf((long) num);
                }
                return String.valueOf(num);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("行事曆模版");

            XSSFCellStyle tipStyle = workbook.createCellStyle();
            tipStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            tipStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            tipStyle.setAlignment(HorizontalAlignment.LEFT);
            tipStyle.setVerticalAlignment(VerticalAlignment.TOP);
            tipStyle.setWrapText(true);
            XSSFFont tipFont = workbook.createFont();
            tipFont.setItalic(true);
            tipFont.setFontHeightInPoints((short) 10);
            tipFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            tipStyle.setFont(tipFont);

            XSSFCellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);

            String tipText =
                "第一行 (Row 1)：說明行（系統自動跳過）。\n" +
                "第二行 (Row 2)：表頭行（系統自動跳過），數據從第三行開始填寫。\n" +
                "第三行 (Row 3)：數據行。\n" +
                "C列 (學部)：若留空預設為「全校」。可填寫：全校 / 幼稚園 / 小學 / 中學。\n" +
                "日期格式：請使用 YYYY-MM-DD 格式（文字格式，例如 2026-09-01）。";

            Row row1 = sheet.createRow(0);
            row1.setHeightInPoints(78);
            Cell tipCell = row1.createCell(0);
            tipCell.setCellValue(tipText);
            tipCell.setCellStyle(tipStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

            Row headerRow = sheet.createRow(1);
            headerRow.setHeightInPoints(24);
            String[] headers = {"日期 (必填 格式YYYY-MM-DD)", "標題 (必填)", "學部 (選填)", "備註 (選填)"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DataValidationHelper dvHelper = sheet.getDataValidationHelper();
            DataValidationConstraint dvConstraint = dvHelper.createExplicitListConstraint(
                    new String[]{"全校", "幼稚園", "小學", "中學"});
            CellRangeAddressList addressList = new CellRangeAddressList(2, 500, 2, 2);
            DataValidation dataValidation = dvHelper.createValidation(dvConstraint, addressList);
            dataValidation.setShowErrorBox(true);
            dataValidation.setErrorStyle(DataValidation.ErrorStyle.STOP);
            dataValidation.createErrorBox("輸入錯誤", "學部請從下拉選單中選擇：全校 / 幼稚園 / 小學 / 中學");
            sheet.addValidationData(dataValidation);

            sheet.setColumnWidth(0, 7500);
            sheet.setColumnWidth(1, 7000);
            sheet.setColumnWidth(2, 4000);
            sheet.setColumnWidth(3, 7000);

            sheet.createFreezePane(0, 2);

            String filename = URLEncoder.encode("行事曆導入模版.xlsx", StandardCharsets.UTF_8.name())
                    .replaceAll("\\+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + filename);
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }
}
