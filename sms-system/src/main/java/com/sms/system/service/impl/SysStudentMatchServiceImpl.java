package com.sms.system.service.impl;

import com.github.houbb.opencc4j.util.ZhConverterUtil;
import com.sms.system.entity.SysStudentMatch;
import com.sms.system.entity.SysDepartmentParentBinding;
import com.sms.system.entity.vo.SysWecomStudentVO;
import com.sms.system.mapper.SysStudentMatchMapper;
import com.sms.system.mapper.SysDepartmentParentBindingMapper;
import com.sms.system.service.ISysStudentMatchService;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;


@Service
public class SysStudentMatchServiceImpl implements ISysStudentMatchService {

    private static final Logger log = LoggerFactory.getLogger(SysStudentMatchServiceImpl.class);

    @Autowired
    private SysStudentMatchMapper sysStudentMatchMapper;

    @Autowired
    private SysDepartmentParentBindingMapper sysDepartmentParentBindingMapper;

    @Override
    public List<SysStudentMatch> selectSysStudentMatchList(SysStudentMatch sysStudentMatch) {
        return sysStudentMatchMapper.selectSysStudentMatchList(sysStudentMatch);
    }

    @Override
    public List<SysStudentMatch> selectUnmatchedList(SysStudentMatch sysStudentMatch) {
        return sysStudentMatchMapper.selectUnmatchedList(sysStudentMatch);
    }

    @Override
    public List<SysWecomStudentVO> selectWecomCandidates(String queryName, String queryMobile, String queryClass) {
        String queryNameTraditional = "";
        String queryNameSimplified = "";
        if (queryName != null && !queryName.trim().isEmpty()) {
            queryName = queryName.trim();
            queryNameTraditional = ZhConverterUtil.toTraditional(queryName);
            queryNameSimplified = ZhConverterUtil.toSimple(queryName);
        }
        return sysStudentMatchMapper.selectWecomCandidates(queryNameTraditional, queryNameSimplified, queryMobile, queryClass);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindStudent(Long matchId, String studentUserIdWecom) {
        SysStudentMatch match = sysStudentMatchMapper.selectSysStudentMatchById(matchId);
        if (match == null) {
            return false;
        }

        List<SysWecomStudentVO> wecomStudents = sysStudentMatchMapper.selectWecomStudentInfoList();
        String studentNameWecom = "";
        for (SysWecomStudentVO stu : wecomStudents) {
            if (studentUserIdWecom.equals(stu.getStudentUserId())) {
                studentNameWecom = stu.getStudentName();
                break;
            }
        }
        if (studentNameWecom == null || studentNameWecom.isEmpty()) {
            studentNameWecom = studentUserIdWecom;
        }

        match.setStudentUserIdWecom(studentUserIdWecom);
        match.setStudentNameWecom(studentNameWecom);
        match.setMatchStatus("2"); // 手動匹配成功

        return sysStudentMatchMapper.updateSysStudentMatch(match) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String importExcel(MultipartFile file, String operName) throws Exception {
        List<SysStudentMatch> excelList = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);

            int headerRowIndex = -1;
            Row headerRow = null;
            for (int r = 0; r < 10; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                boolean isHeader = false;
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    String val = getCellValueAsString(row.getCell(c));
                    if (val != null && (val.equalsIgnoreCase("StudentProfileNumber")
                            || val.equalsIgnoreCase("IDName")
                            || val.equalsIgnoreCase("ADID"))) {
                        isHeader = true;
                        break;
                    }
                }
                if (isHeader) {
                    headerRowIndex = r;
                    headerRow = row;
                    break;
                }
            }

            if (headerRow == null) {
                headerRow = sheet.getRow(1);
            }

            Map<String, Integer> headerMap = new HashMap<>();
            if (headerRow != null) {
                for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                    String val = getCellValueAsString(headerRow.getCell(c));
                    if (val != null) {
                        headerMap.put(val.trim(), c);
                    }
                }
            }

            if (!headerMap.containsKey("IDName") || !headerMap.containsKey("ClassSection")) {
                throw new Exception("Excel 表頭缺少必填欄位 (IDName 或 ClassSection)！");
            }

            int startRow = (headerRowIndex == -1) ? 2 : headerRowIndex + 1;
            int totalRows = sheet.getLastRowNum();

            for (int i = startRow; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String name = getMapCellValue(row, headerMap, "IDName");
                String className = getMapCellValue(row, headerMap, "ClassSection");

                if (name == null || name.isEmpty() || className == null || className.isEmpty()) {
                    continue;
                }

                SysStudentMatch item = new SysStudentMatch();
                item.setStudentProfileNum(getMapCellValue(row, headerMap, "StudentProfileNumber"));
                item.setAdid(getMapCellValue(row, headerMap, "ADID"));
                item.setStudentNameLocal(name);
                item.setClassNameLocal(className);
                item.setIdEnglishName(getMapCellValue(row, headerMap, "IDEnglishName"));
                item.setEnglishFirstName(getMapCellValue(row, headerMap, "EnglishFirstName"));
                item.setEnglishLastName(getMapCellValue(row, headerMap, "EnglishLastName"));
                item.setMatchStatus("0");
                item.setSyncStatus("0");
                item.setCreateBy(operName);

                excelList.add(item);
            }
        }

        if (excelList.isEmpty()) {
            return "Excel 中未讀取到有效的學生數據！";
        }

        sysStudentMatchMapper.batchInsertOrUpdate(excelList);

        return String.format("Excel 數據導入完成！共成功導入 %d 筆數據，請選取行並點擊「同步至企業微信」進行自動匹配與更名。", excelList.size());
    }

    // ──────────────────────────────────────────────────────────────
    //  以下三個方法供 sms-api 的 StudentMatchHandler 使用
    //  企微 API 調用已完全轉移至 Handler 層，Service 只負責 DB 操作
    // ──────────────────────────────────────────────────────────────

    @Override
    public List<SysStudentMatch> getPendingListForSync(List<Long> matchIds) {
        List<SysStudentMatch> result = new ArrayList<>();
        for (Long id : matchIds) {
            SysStudentMatch m = sysStudentMatchMapper.selectSysStudentMatchById(id);
            if (m != null && !"1".equals(m.getSyncStatus())) {
                result.add(m);
            }
        }
        return result;
    }

    @Override
    public Map<String, List<Long>> getStudentDeptMap(List<String> studentUserIds) {
        Map<String, List<Long>> deptMap = new HashMap<>();
        if (studentUserIds == null || studentUserIds.isEmpty()) {
            return deptMap;
        }
        List<SysDepartmentParentBinding> bindings =
                sysDepartmentParentBindingMapper.selectByStudentUserIds(studentUserIds);
        if (bindings != null) {
            for (SysDepartmentParentBinding b : bindings) {
                if (b.getStudentUserId() != null && b.getDepartmentId() != null) {
                    deptMap.computeIfAbsent(b.getStudentUserId(), k -> new ArrayList<>())
                           .add(b.getDepartmentId());
                }
            }
        }
        return deptMap;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOneSyncResult(SysStudentMatch match, String syncStatus, String errorMsg, String operName) {
        match.setSyncStatus(syncStatus);
        match.setErrorMsg(errorMsg);
        match.setUpdateBy(operName);
        sysStudentMatchMapper.updateSysStudentMatch(match);

        // 同步成功時同步更新本地關係表姓名
        if ("1".equals(syncStatus) && match.getStudentUserIdWecom() != null) {
            sysStudentMatchMapper.updateWecomStudentName(
                    match.getStudentUserIdWecom(), match.getStudentNameLocal());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String syncData(String operName) {
        List<SysStudentMatch> unmatchedList = sysStudentMatchMapper.selectUnmatchedList(null);
        if (unmatchedList == null || unmatchedList.isEmpty()) {
            return "暫無未匹配的學生數據，無需執行數據同步匹配！";
        }

        List<SysWecomStudentVO> wecomStudents = sysStudentMatchMapper.selectWecomStudentInfoList();
        if (wecomStudents == null || wecomStudents.isEmpty()) {
            return "本地關係表中未找到企微學生數據，無法執行自動比對！";
        }

        int matchedCount = 0;
        for (SysStudentMatch item : unmatchedList) {
            String excelClass = item.getClassNameLocal() != null ? item.getClassNameLocal().trim() : "";
            String excelName = item.getStudentNameLocal() != null ? item.getStudentNameLocal().trim() : "";
            if (excelClass.isEmpty() || excelName.isEmpty()) {
                continue;
            }
            String excelNameTraditional = ZhConverterUtil.toTraditional(excelName);

            Optional<SysWecomStudentVO> matchedOpt = wecomStudents.stream().filter(wecomStu -> {
                String wecomClass = wecomStu.getClassCodeWecom();
                String wecomName = wecomStu.getStudentName();
                if (wecomClass == null || wecomName == null) {
                    return false;
                }
                if (!wecomClass.trim().equalsIgnoreCase(excelClass)) {
                    return false;
                }
                String wecomNameTraditional = ZhConverterUtil.toTraditional(wecomName.trim());
                return wecomNameTraditional.equals(excelNameTraditional);
            }).findFirst();

            if (matchedOpt.isPresent()) {
                SysWecomStudentVO wecomStu = matchedOpt.get();
                String studentUserId = wecomStu.getStudentUserId();
                String wecomName = wecomStu.getStudentName();

                item.setStudentUserIdWecom(studentUserId);
                item.setStudentNameWecom(wecomName);
                item.setMatchStatus("1"); // 自動匹配成功
                item.setUpdateBy(operName);

                sysStudentMatchMapper.updateSysStudentMatch(item);
                matchedCount++;
            }
        }

        return String.format("同步數據對照完成！共成功自動匹配 %d 筆數據，請點擊已匹配數據勾選並點擊「同步至企業微信」進行同步更名。", matchedCount);
    }

    // ──────────── 私有輔助方法 ────────────

    private String getMapCellValue(Row row, Map<String, Integer> headerMap, String colName) {
        Integer colIndex = headerMap.get(colName);
        if (colIndex == null) return null;
        Cell cell = row.getCell(colIndex);
        String val = getCellValueAsString(cell);
        return val != null ? val.trim() : null;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
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
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        // 使用簡單的 POI 生成模版
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.xssf.usermodel.XSSFSheet sheet = workbook.createSheet("學籍對照導入模版");
            
            // 說明樣式
            org.apache.poi.xssf.usermodel.XSSFCellStyle tipStyle = workbook.createCellStyle();
            tipStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            tipStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            tipStyle.setWrapText(true);
            org.apache.poi.xssf.usermodel.XSSFFont tipFont = workbook.createFont();
            tipFont.setItalic(true);
            tipFont.setFontHeightInPoints((short) 10);
            tipFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.DARK_BLUE.getIndex());
            tipStyle.setFont(tipFont);
            
            // 表頭樣式
            org.apache.poi.xssf.usermodel.XSSFCellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
            org.apache.poi.xssf.usermodel.XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // 說明文字
            String tipText = "第一行 (Row 1)：說明行（系統自動跳過）。\n" +
                             "第二行 (Row 2)：表頭行（系統自動跳過），數據從第三行開始填寫。\n" +
                             "IDName 與 ClassSection 為必填列。";
            
            org.apache.poi.ss.usermodel.Row row1 = sheet.createRow(0);
            row1.setHeightInPoints(50);
            org.apache.poi.ss.usermodel.Cell tipCell = row1.createCell(0);
            tipCell.setCellValue(tipText);
            tipCell.setCellStyle(tipStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 6));
            
            // 表頭
            org.apache.poi.ss.usermodel.Row row2 = sheet.createRow(1);
            String[] headers = { "StudentProfileNumber", "ADID", "ClassSection", "IDName", "IDEnglishName", "EnglishFirstName", "EnglishLastName" };
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = row2.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 示例數據列
            org.apache.poi.ss.usermodel.Row row3 = sheet.createRow(2);
            row3.createCell(0).setCellValue("95339");
            row3.createCell(1).setCellValue("s95339");
            row3.createCell(2).setCellValue("K1E");
            row3.createCell(3).setCellValue("張三");
            row3.createCell(4).setCellValue("Cheong Sam");
            row3.createCell(5).setCellValue("Sam");
            row3.createCell(6).setCellValue("Cheong");
            
            // 欄寬
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 6000);
            }
            
            String filename = URLEncoder.encode("學籍對照導入模版.xlsx", StandardCharsets.UTF_8.name())
                    .replaceAll("\\+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + filename);
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
            
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }
}

