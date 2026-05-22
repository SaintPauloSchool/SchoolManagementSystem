package com.sms.system.service.impl;

import com.sms.system.entity.CalendarEvent;
import com.sms.system.entity.vo.CalendarEventVO;
import com.sms.system.mapper.CalendarEventMapper;
import com.sms.system.service.ICalendarEventService;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    public CalendarEvent selectCalendarEventByEventId(Long eventId) {
        return calendarEventMapper.selectCalendarEventByEventId(eventId);
    }

    @Override
    public List<CalendarEvent> selectCalendarEventList(CalendarEventVO eventVO) {
        return calendarEventMapper.selectCalendarEventList(eventVO);
    }

    @Override
    public int insertCalendarEvent(CalendarEvent calendarEvent) {
        calendarEvent.setCreateTime(LocalDateTime.now());
        return calendarEventMapper.insertCalendarEvent(calendarEvent);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertCalendarEventBatch(List<CalendarEvent> calendarEvents) {
        LocalDateTime now = LocalDateTime.now();
        calendarEvents.forEach(e -> e.setCreateTime(now));
        return calendarEventMapper.insertCalendarEventBatch(calendarEvents);
    }

    @Override
    public int updateCalendarEvent(CalendarEvent calendarEvent) {
        calendarEvent.setUpdateTime(LocalDateTime.now());
        return calendarEventMapper.updateCalendarEvent(calendarEvent);
    }

    @Override
    public int deleteCalendarEventByEventIds(Long[] eventIds) {
        return calendarEventMapper.deleteCalendarEventByEventIds(eventIds);
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

            // 假設第一行是表頭，從第二行開始讀取 (index 1)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                try {
                    CalendarEvent event = new CalendarEvent();
                    
                    // 1. 事件日期
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

                    // 2. 事件標題
                    Cell titleCell = row.getCell(1);
                    if (titleCell != null) {
                        event.setTitle(getCellValueAsString(titleCell));
                    }

                    if (event.getTitle() == null || event.getTitle().trim().isEmpty() || event.getEventDate() == null) {
                        failureNum++;
                        failureMsg.append("<br/>第 ").append(i + 1).append(" 行導入失敗：日期和標題不能為空");
                        continue;
                    }

                    // 3. 對象類型 (0: 全校, 1: 幼稚園, 2: 小學, 3: 中學)
                    Cell typeCell = row.getCell(2);
                    String typeStr = getCellValueAsString(typeCell);
                    int targetType = 0; // 默認全校
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

                    // 4. 備註
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
                    failureMsg.append("<br/>第 ").append(i + 1).append(" 行導入失敗：").append(e.getMessage());
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
}
