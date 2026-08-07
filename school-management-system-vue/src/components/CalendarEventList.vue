<template>
  <div class="calendar-event-list">
    <el-card class="box-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">
            <el-icon><Calendar /></el-icon>
            行事曆管理
          </span>
          <div class="header-actions">
            <el-button type="success" :icon="Upload" @click="openUploadDialog">
              導入 Excel
            </el-button>
            <el-button type="primary" :icon="Plus" @click="handleAdd">
              新增行事
            </el-button>
          </div>
        </div>
      </template>

      <!-- 搜尋欄 -->
      <el-form :model="searchForm" ref="queryForm" :inline="true" class="search-form">
        <el-form-item label="行事標題">
          <el-input v-model="searchForm.title" placeholder="請輸入行事標題" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="學部">
          <el-select v-model="searchForm.targetType" placeholder="請選擇學部" style="width: 150px;" clearable>
            <el-option label="全校" :value="0" />
            <el-option label="幼稚園" :value="1" />
            <el-option label="小學" :value="2" />
            <el-option label="中學" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="行事日期">
          <el-date-picker v-model="searchForm.eventDate" type="date" placeholder="請選擇行事日期" value-format="YYYY-MM-DD" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜尋</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 列表 -->
      <el-table v-loading="loading" :data="calendarEvents" style="width: 100%" :row-style="{ height: '56px' }" empty-text="暫無數據">
        <el-table-column prop="eventDate" label="行事日期" width="120" align="center" />
        <el-table-column prop="targetType" label="學部" width="100" align="center">
          <template #default="scope">
            <span :class="['target-type-tag', `target-type-${scope.row.targetType}`]">
              {{ getTargetTypeText(scope.row.targetType) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="行事標題" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <el-button size="small" type="primary" :icon="Edit" @click="handleEdit(scope.row)">修改</el-button>
              <el-button size="small" type="danger" :icon="Delete" @click="handleDelete(scope.row)">刪除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分頁 -->
      <div class="pagination-container" v-if="total > 0">
        <el-pagination v-model:current-page="pagination.currentPage" v-model:page-size="pagination.pageSize" :page-sizes="[10, 20, 50]" :total="total" layout="total, sizes, prev, pager, next" background @size-change="loadEvents" @current-change="loadEvents" />
      </div>
    </el-card>

    <!-- 新增/修改對話框 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="行事標題" prop="title">
          <el-input v-model="form.title" placeholder="請輸入行事標題" />
        </el-form-item>
        <el-form-item label="行事日期" :prop="isEditMode ? 'eventDate' : 'eventDateRange'">
          <!-- 修改模式：單日選擇 -->
          <el-date-picker
            v-if="isEditMode"
            v-model="form.eventDate"
            type="date"
            placeholder="請選擇行事日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
          <!-- 新增模式：日期範圍選擇 -->
          <el-date-picker
            v-else
            v-model="form.eventDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="開始日期"
            end-placeholder="結束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="學部" prop="targetType">
          <el-select v-model="form.targetType" placeholder="請選擇學部" style="width: 100%">
            <el-option label="全校" :value="0" />
            <el-option label="幼稚園" :value="1" />
            <el-option label="小學" :value="2" />
            <el-option label="中學" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="備註" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="請輸入備註" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="submitForm">確 定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 導入對話框 -->
    <el-dialog title="導入 Excel" v-model="uploadVisible" width="650px" top="5vh" append-to-body>
      <div class="upload-dialog-content">
        <div class="upload-instruction">
          <div class="instruction-title">
            <span style="display:flex;align-items:center;gap:5px;">
              <el-icon><info-filled /></el-icon> Excel 製作範例 (請完全照此格式)：
            </span>
            <el-button
              type="primary"
              :icon="Download"
              :loading="templateDownloading"
              @click="downloadTemplate"
              style="margin-left:auto;"
            >
              下載模版
            </el-button>
          </div>
          
          <ul class="instruction-list">
            <li><strong>第一行 (Row 1)：</strong> 說明行（系統自動跳過）。</li>
            <li><strong>第二行 (Row 2)：</strong> 表頭行（系統自動跳過），<strong>數據從第三行開始填寫</strong>。</li>
            <li><strong>第三行 (Row 3)：</strong> 數據行。</li>
            <li><strong>C列 (學部)：</strong> 若留空預設為「全校」。可填寫：全校 / 幼稚園 / 小學 / 中學。</li>
            <li><strong>日期格式：</strong> 請使用 <code>YYYY-MM-DD</code> 格式（文字格式，例如 2026-09-01）。</li>
          </ul>

          <div class="demo-label">【示範】</div>
          <div class="excel-preview">
            <table class="excel-table">
              <thead>
                <tr>
                  <th></th>
                  <th>A列</th>
                  <th>B列</th>
                  <th>C列</th>
                  <th>D列</th>
                </tr>
              </thead>
              <tbody>
                <tr class="excel-tip-row">
                  <td class="row-num">1</td>
                  <td colspan="4" class="excel-cell-tip">說明行（系統自動跳過）。</td>
                </tr>
                <tr class="excel-header-row">
                  <td class="row-num">2</td>
                  <td>日期 (必填)</td>
                  <td>標題 (必填)</td>
                  <td>學部 (選填)</td>
                  <td>備註 (選填)</td>
                </tr>
                <tr>
                  <td class="row-num">3</td>
                  <td class="excel-cell-date">2026-05-25</td>
                  <td>學校運動會</td>
                  <td>全校</td>
                  <td>請準時出席</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <el-upload
          ref="uploadRef"
          class="upload-demo"
          drag
          action=""
          :http-request="customUpload"
          :limit="1"
          accept=".xlsx, .xls"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            將檔案拖曳至此處，或 <em>點擊上傳</em>
          </div>
        </el-upload>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { Calendar, Upload, Plus, Search, Refresh, UploadFilled, Edit, Delete, InfoFilled, Download } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { ElMessageBox, ElNotification } from 'element-plus'

export default {
  name: 'CalendarEventList',
  components: {
    Calendar, Upload, Plus, Search, Refresh, UploadFilled, Edit, Delete, Download
  },
  data() {
    return {
      Upload, Plus, Search, Refresh, Edit, Delete, Download,
      loading: false,
      calendarEvents: [],
      total: 0,
      pagination: {
        currentPage: 1,
        pageSize: 10
      },
      searchForm: {
        title: '',
        targetType: null,
        eventDate: ''
      },
      dialogVisible: false,
      dialogTitle: '',
      isEditMode: false,
      form: {},
      rules: {
        title: [{ required: true, message: '行事標題不能為空', trigger: 'blur' }],
        eventDate: [{ required: true, message: '行事日期不能為空', trigger: 'change' }],
        eventDateRange: [{ required: true, message: '請選擇行事日期範圍', trigger: 'change', type: 'array', min: 2 }],
        targetType: [{ required: true, message: '請選擇學部', trigger: 'change' }]
      },
      uploadVisible: false,
      templateDownloading: false
    }
  },
  mounted() {
    this.loadEvents()
  },
  methods: {
    async loadEvents() {
      this.loading = true
      try {
        const response = await request({
          url: '/system/calendarEvent/list',
          method: 'get',
          params: {
            pageNum: this.pagination.currentPage,
            pageSize: this.pagination.pageSize,
            title: this.searchForm.title,
            targetType: this.searchForm.targetType,
            eventDate: this.searchForm.eventDate
          }
        })
        
        if (response.code === 200 || response.code === 0) {
          this.calendarEvents = response.rows || []
          this.total = response.total || 0
        } else {
          ElNotification({ title: '加載失敗', message: response.msg || '加載失敗', type: 'error', duration: 4000 })
        }
      } catch (error) {
        console.error('加載失敗:', error)
        ElNotification({ title: '加載失敗', message: '資料加載失敗，請檢查是否具有管理員權限', type: 'error', duration: 4000 })
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.pagination.currentPage = 1
      this.loadEvents()
    },
    resetSearch() {
      this.searchForm = { title: '', targetType: null, eventDate: '' }
      this.handleSearch()
    },
    handleAdd() {
      this.isEditMode = false
      this.form = { title: '', eventDateRange: [], targetType: 0, remark: '' }
      this.dialogTitle = '新增行事'
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.isEditMode = true
      this.form = { ...row }
      this.dialogTitle = '修改行事'
      this.dialogVisible = true
    },
    async submitForm() {
      this.$refs.form.validate(async valid => {
        if (valid) {
          try {
            if (!this.isEditMode) {
              // 新增模式：展開日期範圍，一次批量送出
              const dates = this.generateDateRange(this.form.eventDateRange[0], this.form.eventDateRange[1])
              const payload = dates.map(date => ({
                title: this.form.title,
                eventDate: date,
                targetType: this.form.targetType,
                remark: this.form.remark
              }))
              const res = await request({
                url: '/system/calendarEvent/batch',
                method: 'post',
                data: payload
              })
              if (res.code === 200 || res.code === 0) {
                ElNotification({
                  title: '新增成功',
                  message: `成功新增 ${dates.length} 筆行事`,
                  type: 'success',
                  duration: 3000
                })
              } else {
                ElNotification({ title: '批量新增失敗', message: res.msg || '批量新增失敗', type: 'error', duration: 4000 })
                return
              }
            } else {
              // 修改模式：單筆更新
              const res = await request({
                url: '/system/calendarEvent',
                method: 'put',
                data: this.form
              })
              if (res.code === 200 || res.code === 0) {
                ElNotification({ title: '修改成功', message: '行事已成功更新', type: 'success', duration: 3000 })
              } else {
                ElNotification({ title: '修改失敗', message: res.msg || '操作失敗', type: 'error', duration: 4000 })
                return
              }
            }
            this.dialogVisible = false
            this.loadEvents()
          } catch (e) {
            ElNotification({ title: '操作錯誤', message: '操作發生錯誤，請稍後再試', type: 'error', duration: 4000 })
          }
        }
      })
    },
    generateDateRange(start, end) {
      const dates = []
      const current = new Date(start)
      const endDate = new Date(end)
      while (current <= endDate) {
        const y = current.getFullYear()
        const m = String(current.getMonth() + 1).padStart(2, '0')
        const d = String(current.getDate()).padStart(2, '0')
        dates.push(`${y}-${m}-${d}`)
        current.setDate(current.getDate() + 1)
      }
      return dates
    },
    handleDelete(row) {
      ElMessageBox.confirm(`確認要刪除事件 "${row.title}" 嗎？`, '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          const res = await request({
            url: '/system/calendarEvent/' + row.eventId,
            method: 'delete'
          })
          if (res.code === 200 || res.code === 0) {
            ElNotification({ title: '刪除成功', message: `「${row.title}」已成功刪除`, type: 'success', duration: 3000 })
            this.loadEvents()
          } else {
            ElNotification({ title: '刪除失敗', message: res.msg || '刪除失敗', type: 'error', duration: 4000 })
          }
        } catch (e) {
          ElNotification({ title: '刪除錯誤', message: '刪除發生錯誤，請稍後再試', type: 'error', duration: 4000 })
        }
      }).catch(() => {})
    },
    openUploadDialog() {
      this.uploadVisible = true
      this.$nextTick(() => {
        if (this.$refs.uploadRef) {
          this.$refs.uploadRef.clearFiles()
        }
      })
    },
    async customUpload(options) {
      const formData = new FormData()
      formData.append('file', options.file)
      try {
        const res = await request({
          url: '/system/calendarEvent/importData',
          method: 'post',
          data: formData,
          headers: { 'Content-Type': 'multipart/form-data' }
        })
        if (res.code === 200 || res.code === 0) {
          ElNotification({ title: '導入成功', message: res.msg || '導入成功', type: 'success', duration: 3000, dangerouslyUseHTMLString: true })
          this.uploadVisible = false
          this.loadEvents()
        } else {
          // 導入失敗，清除殘留檔案，讓使用者可以重新選取正確的 Excel
          this.$nextTick(() => { if (this.$refs.uploadRef) this.$refs.uploadRef.clearFiles() })
          ElNotification({ title: '導入失敗', message: res.msg || '導入失敗', type: 'error', duration: 5000, dangerouslyUseHTMLString: true })
        }
      } catch (err) {
        // 請求異常，同樣清除殘留檔案
        this.$nextTick(() => { if (this.$refs.uploadRef) this.$refs.uploadRef.clearFiles() })
        ElNotification({ title: '導入錯誤', message: '導入發生錯誤，請稍後再試', type: 'error', duration: 4000 })
      }
    },
    getTargetTypeText(type) {
      const map = { 0: '全校', 1: '幼稚園', 2: '小學', 3: '中學' }
      return map[type] || '未知'
    },
    async downloadTemplate() {
      this.templateDownloading = true
      try {
        const response = await request({
          url: '/system/calendarEvent/importTemplate',
          method: 'get',
          responseType: 'blob'
        })
        // response 攔截器對 blob 類型直接返回 response.data（即 Blob）
        const blob = new Blob([response], {
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = '行事曆導入模版.xlsx'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
      } catch (err) {
        console.error('下載模版失敗:', err)
        ElNotification({ title: '下載失敗', message: '模版下載失敗，請稍後再試', type: 'error', duration: 4000 })
      } finally {
        this.templateDownloading = false
      }
    }
  }
}
</script>

<style scoped>
.calendar-event-list {
  width: 100%;
}
.box-card {
  border-radius: 8px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.title .el-icon {
  font-size: 20px;
  color: #409eff;
}
.header-actions {
  display: flex;
  gap: 10px;
}
.search-form {
  margin-bottom: 15px;
  padding: 15px;
  background-color: #f9fbfd;
  border-radius: 8px;
}
.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding: 14px 24px;
}
.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
}
.action-buttons .el-button {
  border-radius: 6px;
  font-weight: 500;
  font-size: 13px;
  padding: 6px 12px;
  border: none;
  box-shadow: none !important;
  transition: all 0.2s ease;
}
.action-buttons .el-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15) !important;
}

/* 學部標籤 */
.target-type-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
}

/* 0: 全校 - 淡藍色 */
.target-type-0 {
  background-color: #dbeafe;
  color: #1e40af;
}

/* 1: 幼稚園 - 淡粉橘色 */
.target-type-1 {
  background-color: #ffedd5;
  color: #9a3412;
}

/* 2: 小學 - 淡綠色 */
.target-type-2 {
  background-color: #dcfce7;
  color: #166534;
}

/* 3: 中學 - 淡黃色 */
.target-type-3 {
  background-color: #fef9c3;
  color: #854d0e;
}

/* 導入對話框排版 */
.upload-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.upload-demo {
  text-align: center;
}

/* 導入說明樣式 */
.upload-instruction {
  background-color: #f8fafc;
  border-radius: 8px;
  padding: 12px;
  border: 1px solid #e2e8f0;
  text-align: left;
}
.instruction-title {
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 5px;
  justify-content: space-between;
}
/* Excel 預覽表格樣式 */
.excel-preview {
  margin: 10px 0;
  overflow-x: auto;
  border-radius: 4px;
  box-shadow: 0 0 0 1px #cbd5e1;
}
.excel-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  font-family: Arial, sans-serif;
  text-align: left;
  background-color: white;
}
.excel-table th, .excel-table td {
  border: 1px solid #cbd5e1;
  padding: 6px 8px;
  white-space: nowrap;
}
.excel-table thead th {
  background-color: #f1f5f9;
  color: #475569;
  text-align: center;
  font-weight: bold;
}
.excel-table .row-num {
  background-color: #f1f5f9;
  color: #475569;
  text-align: center;
  font-weight: bold;
  width: 30px;
}
.excel-header-row td {
  background-color: #e2e8f0;
  font-weight: 600;
  color: #1e293b;
}
.excel-cell-date {
  color: #059669;
  font-family: monospace;
  font-size: 14px;
}
.excel-tip-row td {
  background-color: #dbeafe;
  font-style: italic;
  color: #1e3a8a;
  font-size: 12px;
}
.excel-cell-tip {
  text-align: left;
  padding: 5px 10px !important;
}
.demo-label {
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  margin: 10px 0 4px 2px;
  letter-spacing: 0.5px;
}
.instruction-list {
  margin: 6px 0 0 0;
  padding-left: 20px;
  font-size: 13px;
  color: #475569;
  line-height: 1.7;
}
.instruction-list strong {
  color: #1e293b;
}
</style>
