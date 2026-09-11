<template>
  <div class="notification-list">
    <!-- 列表頭部 -->
    <div class="list-header">
      <div class="header-meta">
        <el-icon class="header-icon"><List /></el-icon>
        <span class="header-title">{{ listTitle }}</span>
        <span class="count-chip">{{ pagination.total }} 條</span>
      </div>
      <div class="header-filters">
        <div class="search-block">
          <div class="search-shell">
            <el-icon class="search-leading"><Search /></el-icon>
            <input
                v-model="searchQuery"
                class="search-input"
                type="text"
                placeholder="搜尋標題或發送人"
                @keyup.enter="handleSearch"
            />
            <button
                v-if="searchQuery"
                type="button"
                class="search-clear"
                aria-label="清除搜尋"
                @click="clearSearch"
            >×</button>
            <span class="search-divider"></span>
            <button
                type="button"
                class="date-icon-btn"
                :class="{ active: !!publishDate || datePanelVisible }"
                aria-label="篩選發佈時間"
                @click="toggleDatePanel"
            >
              <el-icon :size="16"><Calendar /></el-icon>
            </button>
          </div>

          <div v-if="datePanelVisible" class="date-panel">
            <div class="date-panel-head">
              <button type="button" class="nav-btn" @click="shiftMonth(-1)">‹</button>
              <span class="month-label">{{ panelMonthLabel }}</span>
              <button type="button" class="nav-btn" @click="shiftMonth(1)">›</button>
            </div>
            <div class="week-row">
              <span v-for="w in weekLabels" :key="w">{{ w }}</span>
            </div>
            <div class="day-grid">
              <button
                  v-for="(day, idx) in panelDays"
                  :key="idx"
                  type="button"
                  class="day-cell"
                  :class="{
                    muted: !day.currentMonth,
                    selected: day.value === publishDate,
                    today: day.value === todayValue
                  }"
                  @click="selectPanelDate(day.value)"
              >
                {{ day.label }}
              </button>
            </div>
            <div class="date-panel-foot">
              <button type="button" class="foot-btn" @click="clearPublishDate">清除</button>
              <button type="button" class="foot-btn primary" @click="selectPanelDate(todayValue)">今天</button>
            </div>
          </div>

          <div v-if="publishDate && !datePanelVisible" class="date-chip-row">
            <button type="button" class="date-chip" @click="clearPublishDate">
              {{ formatChipDate(publishDate) }}
              <span>×</span>
            </button>
          </div>
        </div>
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
        <el-table-column label="" width="44" align="center">
          <template #default="scope">
            <el-tooltip
                v-if="scope.row.hasQuestions"
                content="含需家長回覆的問卷"
                placement="top"
            >
              <el-icon class="has-questions-icon"><Notebook /></el-icon>
            </el-tooltip>
          </template>
        </el-table-column>
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
                  v-if="type === 'mySend'"
                  size="small"
                  type="success"
                  title="複製為新通知"
                  @click="handleCopyAsNew(scope.row)"
              >
                <el-icon><DocumentCopy /></el-icon>
                複製
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
          :layout="paginationLayout"
          :pager-count="paginationPagerCount"
          :total="pagination.total"
          :small="isMobileTable"
          background
      />
    </div>

    <!-- 詳情對話框 -->
    <el-dialog
        v-model="detailDialogVisible"
        title="通知詳情"
        :width="detailDialogWidth"
        :fullscreen="detailDialogFullscreen"
        class="notification-detail-dialog"
        :top="detailDialogFullscreen ? '0' : undefined"
        :align-center="!detailDialogFullscreen"
        :modal="true"
        :lock-scroll="true"
        :close-on-click-modal="true"
        :close-on-press-escape="true"
        :show-close="false"
        :append-to-body="true"
        @closed="onDetailClosed"
    >
      <NotificationDetail
          v-if="selectedNotification"
          :notification="selectedNotification"
          :detail-type="type"
          @close="handleDetailClose"
          @copy-as-new="handleCopyAsNewFromDetail"
          @recall="handleRecall"
      />
    </el-dialog>
  </div>
</template>

<script>
import { ElNotification } from 'element-plus'
import { Search, Refresh, View, List, RefreshLeft, Notebook, Calendar, DocumentCopy } from '@element-plus/icons-vue'
import NotificationDetail from './NotificationDetail.vue'
import request from '@/utils/request'

export default {
  name: 'NotificationList',
  components: {
    NotificationDetail,
    DocumentCopy
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
  emits: ['refresh', 'page-change', 'copy-as-new'],
  data() {
    return {
      loading: false,
      searchQuery: '',
      publishDate: '',
      datePanelVisible: false,
      panelYear: new Date().getFullYear(),
      panelMonth: new Date().getMonth(),
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
    paginationLayout() {
      return this.isMobileTable
          ? 'prev, pager, next'
          : 'total, sizes, prev, pager, next'
    },
    paginationPagerCount() {
      return this.isMobileTable ? 5 : 7
    },
    actionColumnWidth() {
      if (this.isMobileTable) {
        return this.type === 'mySend' ? 220 : 100
      }
      return this.type === 'mySend' ? 280 : 120
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
    },
    weekLabels() {
      return ['日', '一', '二', '三', '四', '五', '六']
    },
    todayValue() {
      return this.formatDateValue(new Date())
    },
    panelMonthLabel() {
      return `${this.panelYear}年${this.panelMonth + 1}月`
    },
    panelDays() {
      const year = this.panelYear
      const month = this.panelMonth
      const first = new Date(year, month, 1)
      const startWeekday = first.getDay()
      const daysInMonth = new Date(year, month + 1, 0).getDate()
      const prevDays = new Date(year, month, 0).getDate()
      const cells = []

      for (let i = startWeekday - 1; i >= 0; i--) {
        const day = prevDays - i
        const date = new Date(year, month - 1, day)
        cells.push({
          label: day,
          value: this.formatDateValue(date),
          currentMonth: false
        })
      }
      for (let day = 1; day <= daysInMonth; day++) {
        const date = new Date(year, month, day)
        cells.push({
          label: day,
          value: this.formatDateValue(date),
          currentMonth: true
        })
      }
      const remain = 42 - cells.length
      for (let day = 1; day <= remain; day++) {
        const date = new Date(year, month + 1, day)
        cells.push({
          label: day,
          value: this.formatDateValue(date),
          currentMonth: false
        })
      }
      return cells
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
      this.datePanelVisible = false
      this.$emit('refresh')
    },

    clearSearch() {
      this.searchQuery = ''
      this.handleSearch()
    },

    formatDateValue(date) {
      const y = date.getFullYear()
      const m = String(date.getMonth() + 1).padStart(2, '0')
      const d = String(date.getDate()).padStart(2, '0')
      return `${y}-${m}-${d}`
    },

    formatChipDate(value) {
      if (!value) return ''
      const m = String(value).match(/(\d{4})-(\d{2})-(\d{2})/)
      return m ? `${m[1]}-${m[2]}-${m[3]}` : value
    },

    clearPublishDate() {
      this.publishDate = ''
      this.datePanelVisible = false
      this.handleSearch()
    },

    toggleDatePanel() {
      this.datePanelVisible = !this.datePanelVisible
      if (this.datePanelVisible) {
        const base = this.publishDate ? new Date(this.publishDate) : new Date()
        this.panelYear = base.getFullYear()
        this.panelMonth = base.getMonth()
      }
    },

    shiftMonth(step) {
      const date = new Date(this.panelYear, this.panelMonth + step, 1)
      this.panelYear = date.getFullYear()
      this.panelMonth = date.getMonth()
    },

    selectPanelDate(value) {
      this.publishDate = value
      this.datePanelVisible = false
      this.handleSearch()
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
      this.$confirm('確定要撤回該通告嗎？撤回後家長及學生將無法查看。若需更正內容，可於撤回後使用「複製為新通知」修改後再發佈。', '提示', {
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
              message: '通告已成功撤回。如需更正，可使用「複製為新通知」。',
              type: 'success',
              duration: 3000
            })
            this.detailDialogVisible = false
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

    handleCopyAsNew(row) {
      this.$confirm('以此通知為範本建立新通知，原通知不受影響。是否繼續？', '提示', {
        confirmButtonText: '確定',
        cancelButtonText: '取消',
        type: 'info'
      }).then(() => {
        this.$emit('copy-as-new', { notificationId: row.notificationId })
      }).catch(() => {})
    },

    handleCopyAsNewFromDetail(payload) {
      this.detailDialogVisible = false
      this.$emit('copy-as-new', payload)
    },

    handleDetailClose() {
      this.detailDialogVisible = false
    },

    onDetailClosed() {
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
  gap: 20px;
  flex-shrink: 0;
  flex-wrap: wrap;
}

.header-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  height: 40px;
  padding: 0 4px 0 2px;
}

.header-icon {
  color: #2563eb;
  font-size: 18px;
  flex-shrink: 0;
}

.header-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #1e3a5f;
  letter-spacing: -0.02em;
  white-space: nowrap;
  line-height: 1.2;
}

.count-chip {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #2563eb;
  background: rgba(37, 99, 235, 0.10);
  border: 1px solid rgba(37, 99, 235, 0.14);
  white-space: nowrap;
}

.header-filters {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  flex: 1;
  min-width: 0;
  justify-content: flex-start;
}

.search-block {
  position: relative;
  flex: 1;
  max-width: 420px;
  min-width: 220px;
}

.search-shell {
  position: relative;
  display: flex;
  align-items: center;
  height: 40px;
  padding: 0 4px 0 12px;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid #cfe0f6;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04), 0 6px 16px rgba(37, 99, 235, 0.06);
  overflow: hidden;
  box-sizing: border-box;
  width: 100%;
}

.search-leading {
  color: #64748b;
  font-size: 16px;
  flex-shrink: 0;
  margin-right: 8px;
}

.search-input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  color: #0f172a;
  height: 100%;
}

.search-input::placeholder {
  color: #94a3b8;
}

.search-clear {
  border: none;
  background: #e2e8f0;
  color: #64748b;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  font-size: 14px;
  line-height: 18px;
  padding: 0;
  cursor: pointer;
  flex-shrink: 0;
}

.search-divider {
  width: 1px;
  height: 18px;
  background: #e2e8f0;
  margin: 0 2px 0 8px;
  flex-shrink: 0;
}

.date-icon-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #64748b;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  cursor: pointer;
}

.date-icon-btn.active {
  color: #2563eb;
  background: #eff6ff;
}

.date-icon-btn:hover {
  background: #f1f5f9;
}

.date-panel {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: 30;
  width: 300px;
  padding: 12px;
  border-radius: 14px;
  background: #ffffff;
  border: 1px solid #e2eaf8;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.12);
}

.date-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.month-label {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.nav-btn {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 8px;
  background: #f8fafc;
  color: #334155;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
}

.week-row {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  margin-bottom: 6px;
}

.week-row span {
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
  font-weight: 600;
  padding: 4px 0;
}

.day-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.day-cell {
  height: 32px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.day-cell.muted {
  color: #cbd5e1;
  font-weight: 500;
}

.day-cell.today {
  background: #e0f2fe;
  color: #0284c7;
}

.day-cell.selected {
  background: #2563eb;
  color: #ffffff;
}

.day-cell:hover:not(.selected) {
  background: #f1f5f9;
}

.date-panel-foot {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 10px;
}

.foot-btn {
  height: 30px;
  padding: 0 12px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.foot-btn.primary {
  border-color: #2563eb;
  background: #2563eb;
  color: #ffffff;
}

.date-chip-row {
  margin-top: 8px;
}

.date-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid #bfdbfe;
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.date-chip span {
  font-size: 14px;
  line-height: 1;
}

/* 刷新按鈕 */
.refresh-btn {
  border-radius: 10px !important;
  font-size: 13px !important;
  font-weight: 500 !important;
  padding: 0 16px !important;
  border-color: #d1d5db !important;
  color: #6b7280 !important;
  transition: all 0.2s ease;
  flex-shrink: 0;
  height: 40px !important;
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

.has-questions-icon {
  color: #1e40af;
  font-size: 16px;
  vertical-align: middle;
  cursor: default;
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
  flex-wrap: wrap;
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

  .search-block {
    max-width: 100%;
    flex: 1 1 240px;
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

  .search-block,
  .refresh-btn {
    width: 100% !important;
    max-width: 100% !important;
    min-width: 0 !important;
    margin: 0 !important;
  }

  .date-panel {
    left: 0;
    right: 0;
    width: auto;
  }

  .refresh-btn {
    height: 40px !important;
    justify-content: center;
  }

  .pagination-area {
    padding: 10px 8px;
    justify-content: center;
    overflow-x: auto;
  }

  .pagination-area :deep(.el-pagination) {
    display: flex;
    flex-wrap: nowrap;
    justify-content: center;
    align-items: center;
    column-gap: 2px;
    width: max-content;
    max-width: none;
    margin: 0 auto;
  }

  .pagination-area :deep(.el-pagination__total),
  .pagination-area :deep(.el-pagination__jump) {
    display: none;
  }

  .pagination-area :deep(.btn-prev),
  .pagination-area :deep(.btn-next),
  .pagination-area :deep(.el-pager) {
    flex-shrink: 0;
  }

  .pagination-area :deep(.el-pager) {
    display: inline-flex;
    flex-wrap: nowrap;
  }

  .pagination-area :deep(.el-pagination__sizes) {
    flex: none;
    margin: 0 0 0 4px !important;
  }

  .pagination-area :deep(.el-pagination__sizes .el-select) {
    width: 90px;
  }

  .pagination-area :deep(.el-pager li),
  .pagination-area :deep(.btn-prev),
  .pagination-area :deep(.btn-next) {
    min-width: 28px !important;
    height: 28px !important;
    line-height: 28px !important;
    margin: 0 1px !important;
  }

  .action-buttons {
    flex-direction: row;
    flex-wrap: wrap;
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