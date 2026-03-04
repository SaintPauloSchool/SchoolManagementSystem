# 學生後台管理系統

一個完整的學生後台管理系統，支持通知發布、問題設置、靈活的接收對象配置等功能。

## 項目特點

✅ **完全刪除舊的學生手冊功能**  
✅ **全新的家校通知系統架構**  
✅ **現代化的前後端分離設計**  
✅ **豐富的交互功能和用戶體驗**  
✅ **現代簡約風格界面設計**  
✅ **完善的響應式佈局**

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
5. **分支問題** - 根據用戶回答跳轉到不同子問題

### 📤 發送設置
- **接收對象配置**：
  - 班級選擇
  - 學生/家長選擇
  - 教職員工選擇
- **抄送設置**：
  - 抄送教職員工
  - 抄送學校通訊錄
- **發送配置**：
  - 回復截止時間設置

## 技術架構

### 後端技術棧
- **Spring Boot** - 核心框架
- **MyBatis Plus** - ORM 框架
- **MySQL** - 數據庫
- **JWT** - 認證授權
- **Druid** - 數據庫連接池

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

## 快速開始

### 1. 環境準備
```bash
# 確保安裝以下軟件
Java 8+
MySQL 5.7+
Node.js 16+
npm 8+
Maven 3.6+
```

### 2. 數據庫配置
```sql
# 創建數據庫
CREATE DATABASE student_notification CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 執行初始化腳本
source sql/notification_system.sql
```

### 3. 後端啟動
```bash
# 修改數據庫連接配置
# sp-api/src/main/resources/application.yml

# 開發環境運行 (使用內置 Tomcat)
mvn spring-boot:run

# 或者打包成 WAR 後部署到外部 Tomcat 運行
# 詳見「後端打包部署」章節
```

### 4. 前端啟動
```bash
cd student-backend-system-vue
npm install
npm run dev
```

### 5. 前端打包部署
```bash
# 進入前端項目目錄
cd student-backend-system-vue

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
# 進入後端項目根目錄
cd StudentBackendSystem

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

# 或直接使用外部 Tomcat 運行 (不推薦用於開發環境)
# 將 WAR 包部署到 Tomcat webapps 目錄後啟動
```

### 7. 完整部署流程
```bash
# 1. 準備環境
# - 安裝 JDK 8+
# - 安裝 MySQL 5.7+
# - 安裝 Node.js 16+
# - 安裝 Tomcat 9+ (生產環境)

# 2. 數據庫初始化
# - 創建數據庫
# - 執行 sql/notification_system.sql

# 3. 部署後端
# - 打包：mvn clean package -DskipTests
# - 部署 WAR 到 Tomcat
# - 修改 application.yml 配置

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
StudentBackendSystem/
├── sp-api/                 # 後端 API 服務
│   ├── src/main/java/
│   │   └── com/sp/
│   │       ├── web/controller/system/notification/    # 通知控制器
│   │       └── ...
│   └── src/main/resources/
├── sp-system/             # 系統業務模塊
│   └── src/main/java/com/sp/system/
│       ├── entity/notification/      # 通知相關實體類
│       ├── mapper/notification/      # Mapper接口
│       ├── service/notification/     # Service接口
│       └── service/impl/notification/ # Service 實現
├── sp-common/             # 公共模塊
├── sp-framework/          # 框架模塊
├── sql/                   # 數據庫腳本
│   └── notification_system.sql
├── student-backend-system-vue/  # 前端項目
│   └── src/
│       ├── components/           # Vue 組件
│       │   ├── selectors/        # 選擇器組件
│       │   ├── SchoolNotificationSystem.vue    # 主系統組件
│       │   ├── NotificationList.vue            # 通知列表
│       │   ├── PublishNotification.vue         # 發布通知
│       │   ├── BasicInfoForm.vue               # 基本信息表單
│       │   ├── SendSettingsForm.vue            # 發送設置表單
│       │   ├── NotificationDetail.vue          # 通知詳情
│       │   └── QuestionDialog.vue              # 問題對話框
│       ├── styles/
│       │   └── modern-theme.css    # 現代主題樣式
│       ├── App.vue
│       └── main.js
├── DEPLOYMENT.md          # 部署文檔
└── REFACTOR_GUIDE.md      # 前端重構指南
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