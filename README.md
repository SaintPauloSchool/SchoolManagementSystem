# 校園管理系統

一個完整的校園管理系統，支持通知發布、問題設置、靈活的接收對象配置等功能。

## 項目特點

✅ **全新的家校通知系統架構**  
✅ **現代化的前後端分離設計**  
✅ **豐富的交互功能和用戶體驗**  
✅ **現代簡約風格界面設計**  
✅ **完善的響應式佈局**
✅ **企業微信集成支持**

## 系統功能

### 📢 通知管理
- **抄送我的**：查看抄送給當前用戶的通知
- **我發送的**：管理自己發布的通知
- **發布通知**：創建新的通知（兩步發布流程）

### ✏️ 內容編輯
- 通知標題和正文編輯
- 發送人自動填充（當前登錄用戶，不可修改）
- 跳轉連結設置（選填）
- 附件/圖片上傳（支持多種格式）
- 問題設置（多種題型支持）

### ❓ 問題類型支持
1. **單選題** - 提供多個選項，用戶只能選擇一個
2. **多選題** - 提供多個選項，用戶可以選擇多個
3. **填空題** - 用戶輸入文本回答
4. **附件上傳** - 用戶上傳文件作為回答
5. **邏輯表單** - 支持複雜的問卷結構和條件分支邏輯

### 📤 發送設置
- **接收對象配置**：
    - 班級選擇
    - 學生/家長選擇
    - 教職員工選擇
- **抄送設置**：
    - 抄送教職工
    - 抄送教職工群組
- **發送配置**：
    - 回復截止時間設置

### 👥 組織架構管理
- **部門管理**：支持學校、學段、年級、班級等多級架構
- **家校通訊錄**：管理學生、家長關係
- **學校部門通訊錄**：管理教職員工信息
- **企業微信集成**：同步企業微信組織架構

## 技術架構

### 後端技術棧
- **Spring Boot 2.5.15** - 核心框架
- **MyBatis** - ORM 框架
- **MySQL 8.0+** - 數據庫
- **JWT** - 認證授權
- **Druid 1.2.27** - 數據庫連接池
- **Spring Security** - 安全框架
- **Swagger 3.0** - API 文檔
- **FastJSON 1.2.83** - JSON 處理
- **PageHelper 1.4.7** - 分頁插件

### 前端技術棧
- **Vue 3.2+** - 前端框架
- **Element Plus 2.2+** - UI 組件庫
- **Axios 0.24+** - HTTP 客戶端
- **Vite 2.5+** - 構建工具
- **Day.js** - 日期時間處理

### 數據庫設計
包含以下核心表：
- `notification` - 通知主表
- `notification_receiver` - 通知接收對象表
- `notification_cc` - 通知抄送對象表
- `notification_question` - 問題表
- `user_notification_read` - 用戶閱讀狀態表
- `notification_answer` - 回答表
- `sys_department` - 部門表
- `sys_parent_student_relation` - 家長學生關係表
- `sys_department_parent_binding` - 部門家長綁定表
- `wecom_school_department` - 企業微信學校部門表
- `wecom_school_department_member` - 企業微信部門成員表
- `sys_school_department` - 系統學校部門表
- `sys_school_department_member` - 系統學校部門成員表
- `class_section` - 課程班級表
- `sys_token` - Token 表

## 快速開始

### 1. 環境準備
```bash
# 確保安裝以下軟件
Java 8+
MySQL 8.0+
Node.js 16+
npm 8+
Maven 3.6+
```

### 2. 數據庫配置
```sql
# 創建數據庫
CREATE DATABASE school_student_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 執行初始化腳本
source sql/notification_system.sql
```

### 3. 後端啟動
```bash
# 修改數據庫連接配置
# sp-api/src/main/resources/application-dev.yml

# 開發環境運行 (使用內置 Tomcat)
cd sp-api
mvn spring-boot:run

# 或者在項目根目錄運行
mvn spring-boot:run -pl sp-api
```

### 4. 前端啟動
```bash
cd school-management-system-vue
npm install
npm run dev
```

### 5. 前端打包部署
```bash
# 進入前端項目目錄
cd school-management-system-vue

# 安裝依賴
npm install

# 打包構建
npm run build

# 打包完成後，dist 目錄即為靜態資源
# 可部署到 Nginx、Apache 或任何靜態資源服務器

# Nginx 配置示例
# server {
#     listen 80;
#     server_name your-domain.com;
#     root /path/to/dist;
#     index index.html;
#     
#     location / {
#         try_files $uri $uri/ /index.html;
#     }
#     
#     # API 代理配置
#     location /api/ {
#         proxy_pass http://backend-server:8080/;
#         proxy_set_header Host $host;
#         proxy_set_header X-Real-IP $remote_addr;
#     }
# }
```

### 6. 後端打包部署
```bash
# 進入項目根目錄
cd SchoolManagementSystem

# 打包成 WAR 文件
mvn clean package -DskipTests

# 生成的 WAR 包位置
# sp-api/target/sp-api.war

# 部署到 Tomcat
# 1. 複製 WAR 包到 Tomcat webapps 目錄
cp sp-api/target/sp-api.war /path/to/tomcat/webapps/

# 2. 啟動 Tomcat
# /path/to/tomcat/bin/startup.sh (Linux/Mac)
# /path/to/tomcat/bin/startup.bat (Windows)

# 3. 訪問應用
# http://localhost:8080/sp-api/
```

### 7. 完整部署流程
```bash
# 1. 準備環境
# - 安裝 JDK 8+
# - 安裝 MySQL 8.0+
# - 安裝 Node.js 16+
# - 安裝 Tomcat 9+ (生產環境)

# 2. 數據庫初始化
# - 創建數據庫
# - 執行 sql/notification_system.sql

# 3. 部署後端
# - 修改 application-dev.yml 配置數據庫連接
# - 打包：mvn clean package -DskipTests
# - 部署 WAR 到 Tomcat

# 4. 部署前端
# - 打包：npm run build
# - 配置 Nginx 或 Apache
# - 設置 API 代理

# 5. 啟動服務
# - 啟動 Tomcat
# - 啟動 Nginx
# - 訪問系統
```

### 8. 訪問系統
瀏覽器訪問：`http://localhost` (根據實際配置的域名和端口)

## 項目結構

```
SchoolManagementSystem/
├── sp-api/                 # 後端 API 服務入口
│   ├── src/main/java/com/sp/
│   │   ├── SpApplication.java              # Spring Boot 啟動類
│   │   └── web/controller/
│   │       ├── notification/               # 通知相關控制器
│   │       │   └── NotificationController.java
│   │       ├── DepartmentController.java   # 部門控制器
│   │       ├── SysSchoolDepartmentController.java
│   │       ├── WecomSchoolDepartmentController.java
│   │       └── common/                     # 通用控制器
│   └── src/main/resources/
│       ├── application.yml                 # 主配置文件
│       ├── application-dev.yml             # 開發環境配置
│       └── application-prod.yml            # 生產環境配置
├── sp-system/             # 系統業務模塊
│   └── src/main/java/com/sp/system/
│       ├── entity/                         # 實體類
│       │   ├── notification/              # 通知相關實體
│       │   ├── SysDepartment.java
│       │   ├── SysParentStudentRelation.java
│       │   ├── WecomSchoolDepartment.java
│       │   └── SysSchoolDepartment.java
│       ├── mapper/                        # Mapper 接口
│       └── service/                       # Service 層
├── sp-common/             # 公共模塊（工具類、註解、常量等）
├── sp-framework/          # 框架模塊（安全配置、攔截器等）
├── sql/                   # 數據庫腳本
│   └── notification_system.sql
├── school-management-system-vue/  # 前端項目
│   └── src/
│       ├── components/           # Vue 組件
│       │   ├── selectors/        # 選擇器組件
│       │   ├── SchoolNotificationSystem.vue    # 主系統組件
│       │   ├── NotificationList.vue            # 通知列表
│       │   ├── PublishNotification.vue         # 發布通知
│       │   ├── BasicInfoForm.vue               # 基本信息表單
│       │   ├── SendSettingsForm.vue            # 發送設置表單
│       │   ├── FormQuestionDialog.vue          # 問題對話框
│       │   ├── HomeSchoolContacts.vue          # 家校通訊錄
│       │   └── SchoolDepartment.vue            # 學校部門管理
│       ├── styles/
│       │   └── modern-theme.css    # 現代主題樣式
│       ├── App.vue
│       └── main.js
├── pom.xml                # Maven 父 POM
└── README.md              # 項目說明文檔
```

## API 接口文檔

### 通知管理接口
- `GET /system/notification/list` - 查詢通知列表
- `GET /system/notification/ccToMe` - 查詢抄送給我的通知
- `GET /system/notification/mySend` - 查詢我發送的通知
- `GET /system/notification/{id}` - 獲取通知詳情
- `POST /system/notification` - 發布通知
- `PUT /system/notification` - 修改通知
- `DELETE /system/notification/{ids}` - 刪除通知
- `PUT /system/notification/withdraw/{id}` - 撤回通知

## 測試

```bash
# 運行集成測試
cd sp-api
mvn test
```

## 部署

詳細部署說明請參考 [DEPLOYMENT.md](DEPLOYMENT.md)

### 生產環境部署建議

#### 前端部署
1. **靜態資源服務器**: 使用 Nginx 或 Apache
2. **CDN 加速**: 可將靜態資源上傳到 CDN
3. **Gzip 壓縮**: 啟用 Gzip 壓縮減少傳輸大小
4. **HTTPS**: 生產環境強烈建议使用 HTTPS

#### 後端部署
1. **應用服務器**: Tomcat 9+, Jetty, 或 WebLogic
2. **集群部署**: 多台服務器負載均衡
3. **數據庫連接池**: 合理配置 Druid 連接池大小
4. **JVM 調優**: 根據服務器配置調整堆內存大小
5. **日誌管理**: 配置日誌輪轉和保存策略

#### 性能優化
- 啟用 Redis 緩存
- 配置靜態資源緩存
- 數據庫查詢優化
- 接口限流和熔斷

## 開發規範

### 後端規範
- 使用統一的響應格式
- 控制器層添加權限控制
- Service 層處理業務邏輯
- Mapper 層負責數據訪問

### 前端規範
- 組件化開發
- 使用 Composition API
- 統一的狀態管理和錯誤處理
- 響應式設計適配不同屏幕
- 所有文本使用繁體中文

## 注意事項

1. 確保數據庫字符集為 utf8mb4 以支持 emoji
2. 文件上傳大小限制為 10MB
3. 通知狀態：0-草稿，1-已發布，2-已撤回
4. 前端組件採用模塊化設計，便於維護和擴展
5. 所有前端文本使用繁體中文
6. 推薦使用現代瀏覽器 (Chrome 90+, Firefox 88+, Safari 14+)
7. 最佳體驗寬度 > 1200px

## 瀏覽器支持

- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+