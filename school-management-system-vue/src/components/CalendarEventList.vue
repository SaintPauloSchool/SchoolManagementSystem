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
            <el-button type="success" :icon="Upload" @click="openUploadDialog" size="small">
              導入 Excel
            </el-button>
            <el-button type="primary" :icon="Plus" @click="handleAdd" size="small">
              新增行事
            </el-button>
          </div>
        </div>
      </template>

      <!-- 搜索栏 -->
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
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
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

      <!-- 分页 -->
      <div class="pagination-container" v-if="total > 0">
        <el-pagination v-model:current-page="pagination.currentPage" v-model:page-size="pagination.pageSize" :page-sizes="[10, 20, 50]" :total="total" layout="total, sizes, prev, pager, next" background @size-change="loadEvents" @current-change="loadEvents" />
      </div>
    </el-card>

    <!-- 新增/修改对话框 -->
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
          <el-button type="primary" @click="submitForm">確 定</el-button>
          <el-button @click="dialogVisible = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 导入对话框 -->
    <el-dialog title="導入 Excel" v-model="uploadVisible" width="400px" append-to-body>
      <el-upload
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
        <template #tip>
          <div class="el-upload__tip">
            請上傳 Excel 檔案。欄位順序：日期, 標題, 學部(全校/幼稚園/小學/中學), 備註
          </div>
        </template>
      </el-upload>
    </el-dialog>
  </div>
</template>

<script>
import { Calendar, Upload, Plus, Search, Refresh, UploadFilled, Edit, Delete } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { ElMessageBox, ElNotification } from 'element-plus'

export default {
  name: 'CalendarEventList',
  components: {
    Calendar, Upload, Plus, Search, Refresh, UploadFilled, Edit, Delete
  },
  data() {
    return {
      Upload, Plus, Search, Refresh, Edit, Delete,
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
      uploadVisible: false
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
          ElNotification({ title: '導入失敗', message: res.msg || '導入失敗', type: 'error', duration: 5000, dangerouslyUseHTMLString: true })
        }
      } catch (err) {
        ElNotification({ title: '導入錯誤', message: '導入發生錯誤，請稍後再試', type: 'error', duration: 4000 })
      }
    },
    getTargetTypeText(type) {
      const map = { 0: '全校', 1: '幼稚園', 2: '小學', 3: '中學' }
      return map[type] || '未知'
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
</style>
