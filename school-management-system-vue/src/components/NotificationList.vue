<template>
  <div class="notification-list">
    <!-- 列表头部 -->
    <div class="list-header">
      <div class="header-left">
        <el-icon class="header-icon"><List /></el-icon>
        <span class="header-title">{{ listTitle }}</span>
        <el-tag type="info" size="small" class="count-tag">
          {{ pagination.total }} 條記錄
        </el-tag>
        <!-- 搜索框和发布时间筛选 -->
        <el-input
          v-model="searchQuery"
          placeholder="搜索通知標題或發送人..."
          clearable
          prefix-icon="Search"
          class="search-input"
          @keyup.enter="handleSearch"
        />
        <el-date-picker
          v-model="publishDate"
          type="date"
          placeholder="發布時間"
          clearable
          value-format="YYYY-MM-DD"
          class="date-picker"
          @change="handleSearch"
        />
      </div>
      <el-button plain @click="handleRefresh">
        <el-icon><Refresh /></el-icon>
        刷新數據
      </el-button>
    </div>

    <!-- 表格 -->
    <div class="table-container">
      <el-table 
        :data="displayData" 
        style="width: 100%"
        v-loading="loading"
        stripe
        empty-text="暫無數據"
        :row-style="{ height: '56px' }"
        :cell-style="{ padding: '14px 0' }"
      >
        <el-table-column prop="title" label="通知標題" min-width="200" show-overflow-tooltip>
          <template #default="scope">
            <el-link 
              type="primary" 
              @click="viewNotification(scope.row)" 
              class="title-link"
              :underline="false"
            >
              {{ scope.row.title }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="senderName" label="發送人" width="120" align="center" />
        <el-table-column label="狀態" width="100" align="center">
          <template #default="scope">
            <el-tag 
              :type="getStatusTagType(scope.row.status)"
              size="small"
              effect="light"
            >
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="replyDeadline" label="回覆截止" width="180" align="center">
          <template #default="scope">
            <span v-if="scope.row.replyDeadline" class="deadline-text">
              {{ scope.row.replyDeadline }}
            </span>
            <span v-else class="no-deadline">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="發布時間" width="180" align="center" />
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="scope">
            <div class="action-buttons">
              <el-button 
                size="small" 
                type="primary"
                @click="viewNotification(scope.row)"
              >
                <el-icon><View /></el-icon>
                查看
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <div class="pagination-area">
      <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="pagination.currentPage"
        :page-sizes="[10, 20, 50]"
        :page-size="pagination.pageSize"
        layout="total, sizes, prev, pager, next"
        :total="pagination.total"
        background
      />
    </div>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="通知詳情"
      width="65%"
      :before-close="handleDetailClose"
      class="notification-detail-dialog"
      top="1vh"
      :modal="true"
      :lock-scroll="true"
      :close-on-click-modal="true"
      :close-on-press-escape="true"
      :show-close="true"
      :append-to-body="true"
    >
      <NotificationDetail 
        v-if="selectedNotification"
        :notification="selectedNotification"
      />
    </el-dialog>
  </div>
</template>

<script>
import { Search, Refresh, View, List } from '@element-plus/icons-vue'
import NotificationDetail from './NotificationDetail.vue'
import request from '@/utils/request'

export default {
  name: 'NotificationList',
  components: {
    NotificationDetail
  },
  props: {
    notifications: {
      type: Array,
      default: () => []
    },
    type: {
      type: String,
      default: 'ccToMe'
    },
    pagination: {
      type: Object,
      default: () => ({
        currentPage: 1,
        pageSize: 10,
        total: 0
      })
    }
  },
  emits: ['refresh', 'page-change'],
  data() {
    return {
      loading: false,
      searchQuery: '',
      publishDate: '',
      detailDialogVisible: false,
      selectedNotification: null
    }
  },
  computed: {
    listTitle() {
      const titleMap = {
        'ccToMe': '抄送我的',
        'mySend': '我發送的'
      }
      return titleMap[this.type] || '通知列表'
    },
    displayData() {
      // 直接使用后端返回的分页数据，不做前端切片
      let result = [...this.notifications]
      
      // 搜索過濾（僅在前端過濾當前頁數據）
      if (this.searchQuery) {
        const query = this.searchQuery.toLowerCase()
        result = result.filter(n => 
          n.title.toLowerCase().includes(query) ||
          (n.senderName && n.senderName.toLowerCase().includes(query))
        )
      }
      
      return result
    }
  },
  methods: {
    getStatusTagType(status) {
      const typeMap = {
        '0': 'info',
        '1': 'success',
        '2': 'warning'
      }
      return typeMap[status] || 'info'
    },

    getStatusText(status) {
      const statusMap = {
        '0': '草稿',
        '1': '已發布',
        '2': '已撤回'
      }
      return statusMap[status] || '未知'
    },

    handleSearch() {
      this.$emit('page-change', {
        pageNum: 1,
        pageSize: this.pagination.pageSize,
        publishDate: this.publishDate
      })
    },

    handleSizeChange(val) {
      this.$emit('page-change', {
        pageNum: 1,
        pageSize: val,
        publishDate: this.publishDate
      })
    },

    handleCurrentChange(val) {
      this.$emit('page-change', {
        pageNum: val,
        pageSize: this.pagination.pageSize,
        publishDate: this.publishDate
      })
    },

    handleRefresh() {
      this.searchQuery = ''
      this.publishDate = ''
      this.$emit('refresh')
    },

    async viewNotification(notification) {
      try {
        this.loading = true
        // 獲取完整的通知詳情（包含接收對象、抄送對象、問題等）
        const response = await request({
          url: `/system/notification/${notification.notificationId}`,
          method: 'get'
        })
        
        if (response.code === 200 || response.code === 0) {
          this.selectedNotification = response.data
          this.detailDialogVisible = true
        }
      } catch (error) {
        console.error('獲取詳情失敗:', error)
        this.$message.error('獲取詳情失敗')
      } finally {
        this.loading = false
      }
    },

    handleDetailClose() {
      this.detailDialogVisible = false
      this.selectedNotification = null
    }
  }
}
</script>

<style scoped>
.notification-list {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #ffffff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06), 0 4px 16px rgba(0, 0, 0, 0.04);
  border: 1px solid #e8ecf1;
  animation: cardAppear 0.4s ease both;
}

@keyframes cardAppear {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ===== 列表头部（含搜索） ===== */
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 24px;
  background: #ffffff;
  border-bottom: 1px solid #eef0f4;
  gap: 16px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.header-icon {
  color: #2563eb;
  font-size: 18px;
  flex-shrink: 0;
}

.header-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  letter-spacing: 0.02em;
  white-space: nowrap;
  flex-shrink: 0;
}

.count-tag {
  border-radius: 10px;
  padding: 2px 10px;
  font-size: 12px;
  font-weight: 600;
  background: #eff6ff !important;
  color: #2563eb !important;
  border-color: #bfdbfe !important;
  flex-shrink: 0;
}

/* 搜索框 */
.search-input {
  flex: 1;
  max-width: 320px;
  margin-left: 8px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 8px;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  box-shadow: none;
  transition: all 0.25s ease;
  height: 32px;
}

.search-input :deep(.el-input__wrapper:hover) {
  border-color: #c0c4cc;
}

.search-input :deep(.el-input__wrapper.is-focus) {
  background: #ffffff;
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1);
}

.search-input :deep(.el-input__inner) {
  color: #1f2937;
  font-size: 13px;
}

.search-input :deep(.el-input__inner::placeholder) {
  color: #9ca3af;
}

.search-input :deep(.el-input__prefix .el-icon) {
  color: #9ca3af;
}

/* 日期选择器 */
.date-picker {
  width: 160px;
  flex-shrink: 0;
}

.date-picker :deep(.el-input__wrapper) {
  border-radius: 8px;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  box-shadow: none;
  transition: all 0.25s ease;
  height: 32px;
}

.date-picker :deep(.el-input__wrapper:hover) {
  border-color: #c0c4cc;
}

.date-picker :deep(.el-input__wrapper.is-focus) {
  background: #ffffff;
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1);
}

.date-picker :deep(.el-input__inner) {
  color: #1f2937;
  font-size: 13px;
}

.date-picker :deep(.el-input__inner::placeholder) {
  color: #9ca3af;
}

.date-picker :deep(.el-input__prefix .el-icon) {
  color: #9ca3af;
}

/* 刷新按钮 */
.list-header :deep(.el-button) {
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  padding: 7px 16px;
  border-color: #e0e3eb;
  color: #6b7280;
  transition: all 0.2s ease;
  flex-shrink: 0;
  height: 34px;
}

.list-header :deep(.el-button:hover) {
  color: #2563eb;
  border-color: #93c5fd;
  background: #eff6ff;
}

/* ===== 表格区域 ===== */
.table-container {
  flex: 1;
  overflow: hidden;
  padding: 0;
  background: #ffffff;
  min-height: 0;
}

.table-container :deep(.el-table) {
  border: none;
  --el-table-border-color: #f0f0f4;
  font-size: 14px;
  height: 100% !important;
}

.table-container :deep(.el-table__inner-wrapper) {
  height: 100% !important;
}

.table-container :deep(.el-table__body-wrapper) {
  flex: 1;
  overflow-y: auto;
}

.table-container :deep(.el-table__row) {
  height: 56px;
  transition: background-color 0.2s ease;
}

.table-container :deep(.el-table__row:hover > td) {
  background-color: #eff6ff !important;
}

.table-container :deep(.el-table__row td) {
  padding: 14px 0;
  border-bottom: 1px solid #f5f5f8;
}

.table-container :deep(.el-table th) {
  background: #fafbfc !important;
  color: #6b7280;
  font-weight: 600;
  font-size: 14px;
  letter-spacing: 0.02em;
  text-align: center;
  height: 46px;
  padding: 12px 0;
  border-bottom: 1px solid #eef0f4;
}

.table-container :deep(.el-table th .cell) {
  font-weight: 600;
}

.table-container :deep(.el-table td) {
  text-align: center;
  color: #4b5563;
}

/* 标题列保持左对齐 */
.table-container :deep(.el-table__body tr td:first-child) {
  text-align: left;
}

/* stripe 行颜色优化 */
.table-container :deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background: #fafbfe;
}

.title-link {
  font-weight: 500;
  color: #2563eb;
  transition: all 0.2s ease;
  font-size: 14px;
}

.title-link:hover {
  color: #1d4ed8;
}

.deadline-text {
  color: #d97706;
  font-weight: 500;
  font-size: 14px;
}

.no-deadline {
  color: #d1d5db;
}

/* 状态 tag 优化 */
.table-container :deep(.el-tag) {
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  padding: 2px 10px;
}

/* ===== 操作按钮 ===== */
.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.action-buttons .el-button {
  border-radius: 8px;
  font-weight: 500;
  font-size: 13px;
  padding: 6px 14px;
  border: none;
  box-shadow: none !important;
  transition: all 0.2s ease;
}

.action-buttons .el-button:hover {
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25) !important;
  transform: translateY(-1px);
}

.action-buttons .el-button:focus {
  box-shadow: none !important;
}

.action-buttons .el-button .el-icon {
  margin-right: 4px;
}

.action-buttons .el-button--small {
  --el-button-size: 28px;
}

/* ===== 对话框样式 ===== */
.notification-detail-dialog :deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
}

.notification-detail-dialog :deep(.el-dialog__header) {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 10;
  padding: 16px;
  background: transparent;
  border: none;
  margin-right: 0;
}

.notification-detail-dialog :deep(.el-dialog__headerbtn) {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(8px);
  transition: all 0.2s ease;
  top: 0;
  right: 0;
}

.notification-detail-dialog :deep(.el-dialog__headerbtn:hover) {
  background: rgba(255, 255, 255, 0.45);
  transform: scale(1.05);
}

.notification-detail-dialog :deep(.el-dialog__headerbtn .el-dialog__close) {
  color: #ffffff;
  font-size: 16px;
  font-weight: 600;
}

.notification-detail-dialog :deep(.el-dialog__body) {
  padding: 24px;
  max-height: 78vh;
  overflow-y: auto;
}

.notification-detail-dialog :deep(.el-dialog__body::-webkit-scrollbar) {
  width: 6px;
}

.notification-detail-dialog :deep(.el-dialog__body::-webkit-scrollbar-thumb) {
  background: #d1d5db;
  border-radius: 3px;
}

.notification-detail-dialog :deep(.el-dialog__body::-webkit-scrollbar-track) {
  background: transparent;
}

/* ===== 分页区域 ===== */
.pagination-area {
  padding: 14px 24px;
  background: #fafbfc;
  border-top: 1px solid #eef0f4;
  display: flex;
  justify-content: flex-end;
  flex-shrink: 0;
}

.pagination-area :deep(.el-pagination) {
  --el-pagination-button-bg-color: #ffffff;
  --el-pagination-hover-color: #2563eb;
}

.pagination-area :deep(.el-pagination .btn-prev),
.pagination-area :deep(.el-pagination .btn-next),
.pagination-area :deep(.el-pager li) {
  border-radius: 8px;
  font-weight: 500;
}

.pagination-area :deep(.el-pager li.is-active) {
  background: linear-gradient(135deg, #2563eb 0%, #1e40af 100%) !important;
  color: #ffffff;
}

/* ===== 响应式设计 ===== */
@media (max-width: 1200px) {
  .list-header {
    padding: 14px 20px;
    flex-wrap: wrap;
  }

  .header-left {
    flex-wrap: wrap;
  }

  .search-input {
    max-width: 100%;
    flex: 1 1 200px;
  }
}

@media (max-width: 768px) {
  .list-header {
    padding: 12px 16px;
    flex-direction: column;
    gap: 10px;
    align-items: stretch;
  }

  .header-left {
    flex-wrap: wrap;
  }

  .search-input {
    flex: 1 1 100%;
    max-width: 100%;
    margin-left: 0;
  }

  .date-picker {
    width: 100%;
  }

  .pagination-area {
    padding: 12px 16px;
    justify-content: center;
  }

  .action-buttons {
    flex-direction: column;
    gap: 4px;
  }
}
</style>
