<template>
  <div class="notification-list">
    <!-- 列表頭部 -->
    <div class="list-header">
      <div class="header-meta">
        <el-icon class="header-icon"><List /></el-icon>
        <span class="header-title">{{ listTitle }}</span>
        <el-tag type="info" size="small" class="count-tag">
          {{ pagination.total }} 條記錄
        </el-tag>
      </div>
      <div class="header-filters">
        <el-input
            v-model="searchQuery"
            placeholder="搜尋通知標題或發送人..."
            clearable
            prefix-icon="Search"
            class="filter-control search-input"
            @keyup.enter="handleSearch"
        />
        <el-date-picker
            v-model="publishDate"
            type="date"
            placeholder="發佈時間"
            clearable
            value-format="YYYY-MM-DD"
            class="filter-control date-picker"
            @change="handleSearch"
        />
        <el-button plain class="refresh-btn" @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          刷新數據
        </el-button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="table-container">
      <el-table
          :data="displayData"
          :style="tableStyle"
          v-loading="loading"
          stripe
          empty-text="暫無數據"
          :row-style="{ height: '48px' }"
          :cell-style="{ padding: '10px 0' }"
      >
        <el-table-column prop="title" label="通知標題" min-width="180" show-overflow-tooltip>
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
        <el-table-column prop="senderName" label="發送人" width="110" align="center" />
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
        <el-table-column prop="replyDeadline" label="回覆截止" width="140" align="center">
          <template #default="scope">
            <div v-if="scope.row.replyDeadline" class="datetime-block is-deadline">
              <span class="date-part">{{ scope.row.replyDeadline.split(' ')[0] }}</span>
              <span class="time-part" v-if="scope.row.replyDeadline.split(' ')[1]">{{ scope.row.replyDeadline.split(' ')[1] }}</span>
            </div>
            <span v-else class="no-deadline">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="發佈時間" width="140" align="center">
          <template #default="scope">
            <div v-if="scope.row.createTime" class="datetime-block">
              <span class="date-part">{{ scope.row.createTime.split(' ')[0] }}</span>
              <span class="time-part" v-if="scope.row.createTime.split(' ')[1]">{{ scope.row.createTime.split(' ')[1] }}</span>
            </div>
            <span v-else class="no-deadline">-</span>
          </template>
        </el-table-column>
        <el-table-column
            label="操作"
            :width="actionColumnWidth"
            fixed="right"
            align="center"
        >
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
              <el-button
                  v-if="type === 'mySend' && scope.row.status === '1'"
                  size="small"
                  type="warning"
                  @click="handleRecall(scope.row)"
              >
                <el-icon><RefreshLeft /></el-icon>
                撤回
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分頁 -->
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

    <!-- 詳情對話框 -->
    <el-dialog
        v-model="detailDialogVisible"
        title="通知詳情"
        :width="detailDialogWidth"
        :fullscreen="detailDialogFullscreen"
        :before-close="handleDetailClose"
        class="notification-detail-dialog"
        :top="detailDialogFullscreen ? '0' : undefined"
        :align-center="!detailDialogFullscreen"
        :modal="true"
        :lock-scroll="true"
        :close-on-click-modal="true"
        :close-on-press-escape="true"
        :show-close="false"
        :append-to-body="true"
    >
      <NotificationDetail
          v-if="selectedNotification"
          :notification="selectedNotification"
          :detail-type="type"
          @close="handleDetailClose"
      />
    </el-dialog>
  </div>
</template>

<script>
import { ElNotification } from 'element-plus'
import { Search, Refresh, View, List, RefreshLeft } from '@element-plus/icons-vue'
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
      selectedNotification: null,
      viewportWidth: typeof window !== 'undefined' ? window.innerWidth : 1200
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
    detailDialogFullscreen() {
      return this.viewportWidth <= 768
    },
    detailDialogWidth() {
      return this.detailDialogFullscreen ? '100%' : '65%'
    },
    isMobileTable() {
      return this.viewportWidth <= 768
    },
    actionColumnWidth() {
      if (this.isMobileTable) {
        return this.type === 'mySend' ? 140 : 100
      }
      return this.type === 'mySend' ? 180 : 120
    },
    /** 手機端加寬表格，操作列 fixed 右側，其餘欄位可橫向滑動查看 */
    tableStyle() {
      return { width: '100%' }
    },
    displayData() {
      // 直接使用後端返回的分頁數據，不做前端切片
      let result = [...this.notifications]

      // 搜尋過濾（僅在前端過濾當前頁數據）
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
  mounted() {
    window.addEventListener('resize', this.updateViewportWidth)
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.updateViewportWidth)
  },
  methods: {
    updateViewportWidth() {
      this.viewportWidth = window.innerWidth
    },

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
        '1': '已發佈',
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
          const vo = response.data
          // 將後端 NotificationDetailVO 重新組合為 NotificationDetail.vue 期望的扁平格式：
          // notification.title / notification.receivers / notification.statistics 等
          this.selectedNotification = {
            ...vo.notification,
            receivers: vo.receivers || [],
            ccs: vo.ccs || [],
            questions: vo.questions || [],
            statistics: {
              ...(vo.sendStatistics || {}),
              ...(vo.readStatistics || {})
            }
          }
          this.detailDialogVisible = true
        }
      } catch (error) {
        console.error('獲取詳情失敗:', error)
        ElNotification({ title: "操作失敗", message: '獲取詳情失敗', type: "error", duration: 4000 })
      } finally {
        this.loading = false
      }
    },

    handleRecall(row) {
      this.$confirm('確定要撤回該通告嗎？撤回後家長及學生將無法查看。', '提示', {
        confirmButtonText: '確定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          this.loading = true
          const response = await request({
            url: `/system/notification/recall/${row.notificationId}`,
            method: 'post'
          })
          if (response.code === 200 || response.code === 0) {
            ElNotification({
              title: '操作成功',
              message: '通告已成功撤回',
              type: 'success',
              duration: 3000
            })
            this.handleRefresh()
          } else {
            ElNotification({
              title: '操作失敗',
              message: response.msg || '撤回失敗',
              type: 'error',
              duration: 4000
            })
          }
        } catch (error) {
          console.error('撤回失敗:', error)
          ElNotification({
            title: '操作失敗',
            message: '撤回請求失敗: ' + (error.message || '未知錯誤'),
            type: 'error',
            duration: 4000
          })
        } finally {
          this.loading = false
        }
      }).catch(() => {
        // 取消撤回
      })
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

/* ===== 列表頭部（含搜尋） ===== */
.list-header {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  padding: 14px 24px;
  background: #ffffff;
  border-bottom: 1px solid #eef0f4;
  gap: 16px;
  flex-shrink: 0;
  flex-wrap: wrap;
}

.header-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.header-filters {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
  justify-content: flex-start;
}

.header-icon {
  color: #2563eb;
  font-size: 16px;
  flex-shrink: 0;
}

.header-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  letter-spacing: 0.02em;
  white-space: nowrap;
  flex-shrink: 0;
}

.count-tag {
  border-radius: 10px;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 500;
  background: #eff6ff !important;
  color: #2563eb !important;
  border-color: #bfdbfe !important;
  flex-shrink: 0;
}

/* 搜尋框 / 日期選擇器：統一樣式 */
.filter-control.search-input {
  flex: 1;
  max-width: 320px;
  min-width: 180px;
}

.filter-control.date-picker {
  width: 160px;
  flex-shrink: 0;
}

.filter-control :deep(.el-input__wrapper),
.filter-control.el-date-editor :deep(.el-input__wrapper),
.filter-control.el-input :deep(.el-input__wrapper) {
  border-radius: 8px !important;
  background: #ffffff !important;
  border: 1px solid #d1d5db !important;
  box-shadow: none !important;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
  height: 32px !important;
  padding: 0 11px !important;
}

.filter-control :deep(.el-input__wrapper:hover),
.filter-control.el-date-editor :deep(.el-input__wrapper:hover) {
  border-color: #9ca3af !important;
  background: #ffffff !important;
  box-shadow: none !important;
}

.filter-control :deep(.el-input__wrapper.is-focus),
.filter-control.el-date-editor :deep(.el-input__wrapper.is-focus) {
  background: #ffffff !important;
  border-color: #2563eb !important;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.12) !important;
}

.filter-control :deep(.el-input__inner) {
  color: #1f2937 !important;
  font-size: 13px !important;
  height: 30px !important;
  line-height: 30px !important;
}

.filter-control :deep(.el-input__inner::placeholder) {
  color: #9ca3af !important;
}

.filter-control :deep(.el-input__prefix .el-icon),
.filter-control :deep(.el-input__prefix-inner .el-icon) {
  color: #9ca3af !important;
}

.filter-control :deep(.el-input__suffix .el-icon) {
  color: #9ca3af !important;
}

/* 刷新按鈕 */
.refresh-btn {
  border-radius: 8px !important;
  font-size: 13px !important;
  font-weight: 500 !important;
  padding: 7px 16px !important;
  border-color: #d1d5db !important;
  color: #6b7280 !important;
  transition: all 0.2s ease;
  flex-shrink: 0;
  height: 32px !important;
}

.refresh-btn:hover {
  color: #2563eb !important;
  border-color: #93c5fd !important;
  background: #eff6ff !important;
}

/* ===== 表格區域 ===== */
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
  font-size: 13px;
  height: 100% !important;
}

.table-container :deep(.el-table__inner-wrapper) {
  height: 100% !important;
}

.table-container :deep(.el-table__body-wrapper) {
  flex: 1;
  overflow: auto;
}

.table-container :deep(.el-table__row) {
  height: 48px;
  transition: background-color 0.2s ease;
}

.table-container :deep(.el-table__row:hover > td) {
  background-color: #eff6ff !important;
}

.table-container :deep(.el-table__row td) {
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f8;
}

.table-container :deep(.el-table th) {
  background: #fafbfc !important;
  color: #6b7280;
  font-weight: 600;
  font-size: 13px;
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

/* 標題列保持左對齊 */
.table-container :deep(.el-table__body tr td:first-child) {
  text-align: left;
}

/* stripe 行顏色優化 */
.table-container :deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background: #fafbfe;
}

.title-link {
  font-weight: 500;
  color: #2563eb;
  transition: all 0.2s ease;
  font-size: 13px;
}

.title-link:hover {
  color: #1d4ed8;
}

.datetime-block {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  line-height: 1.35;
}

.datetime-block .date-part {
  color: #374151;
  font-weight: 500;
  font-size: 13px;
}

.datetime-block .time-part {
  color: #8c98a9;
  font-size: 11.5px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  margin-top: 2px;
}

.datetime-block.is-deadline .date-part {
  color: #d97706;
}

.datetime-block.is-deadline .time-part {
  color: #f59e0b;
}

.no-deadline {
  color: #d1d5db;
}

/* 狀態 tag 優化 */
.table-container :deep(.el-tag) {
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  padding: 2px 10px;
}

/* ===== 操作按鈕 ===== */
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

/* ===== 對話框樣式 ===== */
.notification-detail-dialog :deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
}

.notification-detail-dialog :deep(.el-dialog__header) {
  display: none;
}

.notification-detail-dialog :deep(.el-dialog__body) {
  padding: 8px;
  max-height: calc(100vh - 48px);
  overflow-y: auto;
  height: auto;
  box-sizing: border-box;
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

/* ===== 分頁區域 ===== */
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

/* ===== 響應式設計 ===== */
@media (max-width: 1200px) {
  .list-header {
    padding: 14px 20px;
  }

  .header-filters {
    flex-wrap: wrap;
    justify-content: flex-start;
  }

  .search-input {
    max-width: 100%;
    flex: 1 1 200px;
  }
}

@media (max-width: 768px) {
  .list-header {
    padding: 12px 14px;
    flex-direction: column;
    gap: 10px;
    align-items: stretch;
  }

  .header-meta {
    width: 100%;
  }

  .header-filters {
    width: 100%;
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
    justify-content: stretch;
  }

  .filter-control.search-input,
  .filter-control.date-picker,
  .refresh-btn {
    width: 100% !important;
    max-width: 100% !important;
    min-width: 0 !important;
    margin: 0 !important;
  }

  .filter-control.date-picker.el-date-editor,
  .filter-control.date-picker :deep(.el-input) {
    width: 100% !important;
  }

  .filter-control :deep(.el-input__wrapper),
  .filter-control.el-date-editor :deep(.el-input__wrapper) {
    height: 36px !important;
  }

  .refresh-btn {
    height: 36px !important;
    justify-content: center;
  }

  .pagination-area {
    padding: 12px 16px;
    justify-content: center;
  }

  .action-buttons {
    flex-direction: row;
    flex-wrap: nowrap;
    gap: 4px;
    justify-content: center;
  }

  /* 操作列 fixed 右側；中間欄位可左右滑動 */
  .table-container {
    overflow: hidden;
  }

  .table-container :deep(.el-table__body-wrapper),
  .table-container :deep(.el-scrollbar__wrap) {
    overflow-x: auto !important;
    -webkit-overflow-scrolling: touch;
  }

  .table-container :deep(.el-table .cell) {
    padding-left: 8px;
    padding-right: 8px;
  }

  .table-container :deep(.el-table__fixed-right),
  .table-container :deep(.el-table-fixed-column--right) {
    box-shadow: -6px 0 10px rgba(15, 23, 42, 0.08);
  }

  /* 將詳情 Dialog 在手機改為全屏閱讀 */
  .notification-detail-dialog :deep(.el-dialog) {
    width: 100% !important;
    height: 100% !important;
    max-height: 100% !important;
    margin: 0 !important;
    border-radius: 0;
    overflow: hidden;
  }

  .notification-detail-dialog :deep(.el-dialog__body) {
    padding: 8px !important;
    max-height: none;
    overflow-x: hidden;
  }
}

</style>

<style>
/* append-to-body：隱藏頂部標題欄 */
.el-dialog.notification-detail-dialog .el-dialog__header,
.notification-detail-dialog.el-dialog .el-dialog__header {
  display: none !important;
}

.el-dialog.notification-detail-dialog,
.notification-detail-dialog.el-dialog {
  height: auto !important;
  max-height: calc(100vh - 24px) !important;
}

.el-dialog.notification-detail-dialog .el-dialog__body,
.notification-detail-dialog.el-dialog .el-dialog__body {
  padding: 8px !important;
  max-height: calc(100vh - 48px) !important;
  height: auto !important;
  box-sizing: border-box !important;
}

@media (max-width: 768px) {
  .el-dialog.notification-detail-dialog,
  .notification-detail-dialog.el-dialog {
    width: 100% !important;
    max-width: 100vw !important;
    height: 100% !important;
    max-height: 100% !important;
    margin: 0 !important;
    border-radius: 0 !important;
    overflow: hidden !important;
  }

  .el-dialog.notification-detail-dialog.is-fullscreen,
  .notification-detail-dialog.el-dialog.is-fullscreen {
    display: flex;
    flex-direction: column;
    height: 100% !important;
    max-height: 100vh !important;
  }

  .el-dialog.notification-detail-dialog .el-dialog__body,
  .notification-detail-dialog.el-dialog .el-dialog__body {
    padding: 8px !important;
    flex: 1 1 auto;
    min-height: 0;
    overflow-x: hidden !important;
    overflow-y: auto !important;
    max-height: none !important;
    height: auto !important;
    /* 手機隱藏滾動條，避免底部出現未鋪滿的灰條 */
    scrollbar-width: none;
    -ms-overflow-style: none;
  }

  .el-dialog.notification-detail-dialog .el-dialog__body::-webkit-scrollbar,
  .notification-detail-dialog.el-dialog .el-dialog__body::-webkit-scrollbar {
    width: 0 !important;
    height: 0 !important;
    display: none !important;
  }
}
</style>