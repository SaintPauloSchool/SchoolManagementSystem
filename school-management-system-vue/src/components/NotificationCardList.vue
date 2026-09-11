<template>
  <div class="notification-card-list" v-loading="loading">
    <div class="list-header">
      <div class="header-meta">
        <div class="title-block">
          <span class="header-icon-wrap" aria-hidden="true">
            <el-icon :size="18"><List /></el-icon>
          </span>
          <span class="header-title">{{ listTitle }}</span>
          <span class="count-chip">{{ pagination.total }} 條</span>
        </div>
        <button type="button" class="refresh-btn" aria-label="刷新" @click="handleRefresh">
          <el-icon :size="16"><Refresh /></el-icon>
        </button>
      </div>

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
          <el-icon :size="18"><Calendar /></el-icon>
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

    <div
        ref="cardScroll"
        class="card-scroll"
        @scroll="handleScroll"
    >
      <div v-if="displayData.length === 0 && !loading" class="empty-tip">
        暫無通知
      </div>

      <article
          v-for="item in displayData"
          :key="item.notificationId"
          class="notice-card"
          :class="cardToneClass(item)"
          @click="viewNotification(item)"
      >
        <div class="card-head">
          <div class="card-badges">
            <span class="status-dot" :class="'status-' + (item.status || '0')">
              {{ getStatusText(item.status) }}
            </span>
            <el-tooltip
                v-if="item.hasQuestions"
                content="含需家長回覆的問卷"
                placement="top"
            >
              <el-icon class="survey-icon"><Notebook /></el-icon>
            </el-tooltip>
          </div>
          <h3 class="card-title">{{ item.title }}</h3>
        </div>

        <div class="info-list">
          <div class="info-row">
            <span class="info-label">發送人</span>
            <span class="info-value">{{ item.senderName || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">發佈時間</span>
            <span class="info-value">{{ formatFullTime(item.createTime) }}</span>
          </div>
          <div v-if="item.replyDeadline" class="info-row">
            <span class="info-label">回覆截止</span>
            <span class="info-value is-deadline">{{ formatFullTime(item.replyDeadline) }}</span>
          </div>
        </div>

        <div class="card-foot" @click.stop>
          <button
              v-if="type === 'mySend' && item.status === '1'"
              type="button"
              class="btn-ghost"
              @click="handleRecall(item)"
          >
            撤回
          </button>
          <button
              v-if="type === 'mySend'"
              type="button"
              class="btn-ghost btn-copy"
              @click="handleCopyAsNew(item)"
          >
            複製
          </button>
          <button type="button" class="btn-main" @click="viewNotification(item)">
            查看詳情
          </button>
        </div>
      </article>

      <div class="load-status">
        <span v-if="loadingMore">加載中...</span>
        <span v-else-if="!hasMore && displayData.length > 0">—— 已經到底啦 ——</span>
      </div>
    </div>

    <div
        class="back-to-top"
        v-show="showBackToTop && !menuOpen"
        @click="scrollToTop"
    >
      <span>回到頂部</span>
    </div>

    <el-dialog
        v-model="detailDialogVisible"
        title="通知詳情"
        width="100%"
        fullscreen
        class="notification-detail-dialog"
        top="0"
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
import {
  Search,
  Refresh,
  Notebook,
  Calendar,
  List
} from '@element-plus/icons-vue'
import NotificationDetail from './NotificationDetail.vue'
import request from '@/utils/request'

export default {
  name: 'NotificationCardList',
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
    },
    menuOpen: {
      type: Boolean,
      default: false
    }
  },
  emits: ['refresh', 'page-change', 'load-more', 'copy-as-new'],
  data() {
    return {
      loading: false,
      loadingMore: false,
      searchQuery: '',
      publishDate: '',
      datePanelVisible: false,
      panelYear: new Date().getFullYear(),
      panelMonth: new Date().getMonth(), // 0-11
      detailDialogVisible: false,
      selectedNotification: null,
      showBackToTop: false
    }
  },
  computed: {
    listTitle() {
      const titleMap = {
        ccToMe: '抄送我的',
        mySend: '我發送的'
      }
      return titleMap[this.type] || '通知列表'
    },
    hasMore() {
      return this.notifications.length < (this.pagination.total || 0)
    },
    displayData() {
      let result = [...this.notifications]
      if (this.searchQuery) {
        const query = this.searchQuery.toLowerCase()
        result = result.filter(n =>
            (n.title || '').toLowerCase().includes(query) ||
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
  watch: {
    notifications() {
      this.loadingMore = false
    }
  },
  methods: {
    cardToneClass(item) {
      if (item.status === '2') return 'tone-recalled'
      if (item.hasQuestions) return 'tone-survey'
      return 'tone-normal'
    },
    getStatusText(status) {
      const statusMap = { '0': '草稿', '1': '已發佈', '2': '已撤回' }
      return statusMap[status] || '未知'
    },
    formatFullTime(value) {
      if (!value) return '-'
      const text = String(value).trim()
      // 已是完整格式直接返回
      if (/^\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}/.test(text)) {
        return text.slice(0, 19)
      }
      // 只有日期
      if (/^\d{4}-\d{2}-\d{2}$/.test(text)) {
        return `${text} 00:00:00`
      }
      // 缺秒
      const m = text.match(/^(\d{4}-\d{2}-\d{2})\s+(\d{2}):(\d{2})(?::(\d{2}))?/)
      if (m) {
        return `${m[1]} ${m[2]}:${m[3]}:${m[4] || '00'}`
      }
      return text
    },
    handleSearch() {
      this.$emit('page-change', {
        pageNum: 1,
        pageSize: this.pagination.pageSize,
        publishDate: this.publishDate
      })
    },
    handleRefresh() {
      this.searchQuery = ''
      this.publishDate = ''
      this.$emit('refresh')
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
      return m ? `${m[2]}-${m[3]}` : value
    },
    clearSearch() {
      this.searchQuery = ''
      this.handleSearch()
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
    handleScroll() {
      const el = this.$refs.cardScroll
      if (!el) return

      // 當滾動超過300px時顯示回到頂部按鈕（與 StudentHandbook 一致）
      this.showBackToTop = el.scrollTop > 300

      if (this.loading || this.loadingMore || !this.hasMore) {
        return
      }
      const distance = el.scrollHeight - el.scrollTop - el.clientHeight
      if (distance <= 80) {
        this.loadMore()
      }
    },
    scrollToTop() {
      const el = this.$refs.cardScroll
      if (!el) return
      el.scrollTo({
        top: 0,
        behavior: 'smooth'
      })
    },
    loadMore() {
      if (this.loadingMore || !this.hasMore) {
        return
      }
      this.loadingMore = true
      this.$emit('load-more', {
        pageNum: (this.pagination.currentPage || 1) + 1,
        pageSize: this.pagination.pageSize,
        publishDate: this.publishDate,
        done: () => {
          this.loadingMore = false
        }
      })
    },
    async viewNotification(notification) {
      try {
        this.loading = true
        const response = await request({
          url: `/system/notification/${notification.notificationId}`,
          method: 'get'
        })
        if (response.code === 200 || response.code === 0) {
          const vo = response.data
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
        ElNotification({ title: '操作失敗', message: '獲取詳情失敗', type: 'error', duration: 4000 })
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
      }).catch(() => {})
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
.notification-card-list {
  --brand: #2563eb;
  --brand-deep: #1d4ed8;
  --surface: #eaf1fb;
  --card: #ffffff;
  position: relative;
  display: flex;
  flex-direction: column;
  height: calc(100dvh - 52px - 32px);
  max-height: calc(100dvh - 52px - 32px);
  background: linear-gradient(180deg, #dbeafe 0%, #eaf1fb 42%, #f3f6fb 100%);
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid #c7dbf5;
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.10);
}

.list-header {
  padding: 12px 14px 12px;
  background: linear-gradient(180deg, rgba(239, 246, 255, 0.98) 0%, rgba(232, 242, 255, 0.94) 100%);
  border-bottom: 1px solid #d7e6f8;
  flex-shrink: 0;
  max-width: 100%;
  box-sizing: border-box;
}

.header-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
  height: 32px;
}

.title-block {
  min-width: 0;
  height: 32px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
}

.header-icon-wrap {
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #2563eb;
  flex-shrink: 0;
}

.header-icon-wrap :deep(.el-icon) {
  width: 18px;
  height: 18px;
  display: block;
}

.header-icon-wrap :deep(svg) {
  display: block;
  width: 18px;
  height: 18px;
}

.header-title {
  display: inline-flex;
  align-items: center;
  height: 32px;
  margin: 0;
  padding: 0;
  font-size: 16px;
  font-weight: 700;
  color: #1e3a5f;
  letter-spacing: -0.02em;
  line-height: 1;
  white-space: nowrap;
}

.count-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #2563eb;
  background: rgba(37, 99, 235, 0.10);
  border: 1px solid rgba(37, 99, 235, 0.14);
  white-space: nowrap;
  line-height: 1;
  flex-shrink: 0;
  box-sizing: border-box;
}

.refresh-btn {
  width: 32px;
  height: 32px;
  border: 1px solid #cfe0f6;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.85);
  color: #475569;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  cursor: pointer;
}

.refresh-btn:active {
  background: #eff6ff;
}

.search-shell {
  position: relative;
  display: flex;
  align-items: center;
  height: 48px;
  padding: 0 6px 0 14px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #cfe0f6;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04), 0 8px 20px rgba(37, 99, 235, 0.08);
  overflow: hidden;
  box-sizing: border-box;
  width: 100%;
  max-width: 100%;
}

.search-leading {
  color: #64748b;
  font-size: 18px;
  flex-shrink: 0;
  margin-right: 8px;
}

.search-input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  font-size: 15px;
  color: #0f172a;
  height: 100%;
}

.search-input::-webkit-search-cancel-button,
.search-input::-webkit-search-decoration {
  -webkit-appearance: none;
  appearance: none;
  display: none;
}

.search-input::placeholder {
  color: #94a3b8;
}

.search-clear {
  border: none;
  background: #e2e8f0;
  color: #64748b;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  font-size: 16px;
  line-height: 20px;
  padding: 0;
  cursor: pointer;
  flex-shrink: 0;
}

.search-divider {
  width: 1px;
  height: 22px;
  background: #e2e8f0;
  margin: 0 2px 0 8px;
  flex-shrink: 0;
}

.date-icon-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 10px;
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

.date-icon-btn:active {
  background: #f1f5f9;
}

.date-panel {
  margin-top: 12px;
  padding: 12px;
  border-radius: 16px;
  background: #f8fbff;
  border: 1px solid #e2eaf8;
}

.date-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.month-label {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.nav-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 10px;
  background: #ffffff;
  color: #334155;
  font-size: 20px;
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
  height: 36px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: #0f172a;
  font-size: 14px;
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
  box-shadow: 0 6px 14px rgba(37, 99, 235, 0.28);
}

.date-panel-foot {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
}

.foot-btn {
  border: none;
  background: transparent;
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
  padding: 6px 4px;
  cursor: pointer;
}

.foot-btn.primary {
  color: #2563eb;
}

.date-chip-row {
  display: flex;
  margin-top: 10px;
}

.date-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: none;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 600;
  padding: 6px 10px;
  cursor: pointer;
}

.card-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  min-height: 0;
  -webkit-overflow-scrolling: touch;
  background:
    radial-gradient(circle at top right, rgba(147, 197, 253, 0.28), transparent 42%),
    linear-gradient(180deg, #e8f1fc 0%, #eef3f9 100%);
}

.empty-tip {
  text-align: center;
  color: #64748b;
  padding: 56px 12px;
  font-size: 14px;
}

.notice-card {
  background: linear-gradient(180deg, #ffffff 0%, #f7faff 100%);
  border-radius: 14px;
  margin-bottom: 12px;
  padding: 16px;
  border: 1px solid #d5e4f7;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.06);
}

.notice-card:active {
  background: #f4f8ff;
}

.tone-survey {
  border-color: #93c5fd;
  background: linear-gradient(180deg, #ffffff 0%, #ebf4ff 100%);
}

.tone-recalled {
  border-color: #fdba74;
  background: linear-gradient(180deg, #ffffff 0%, #fff7ed 100%);
  opacity: 1;
}

.card-head {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 14px;
}

.card-badges {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 24px;
}

.card-title {
  width: 100%;
  margin: 0;
  padding-left: 1px;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.5;
  letter-spacing: 0.01em;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
}

.status-dot {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
  padding: 5px 9px;
  border-radius: 999px;
  letter-spacing: 0.02em;
}

.status-1 {
  background: #ecfdf5;
  color: #059669;
}

.status-0 {
  background: #f1f5f9;
  color: #64748b;
}

.status-2 {
  background: #fff7ed;
  color: #ea580c;
}

.survey-icon {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #eff6ff;
  color: #2563eb;
  font-size: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border-radius: 12px;
  background: rgba(219, 234, 254, 0.45);
  border: 1px solid rgba(191, 219, 254, 0.7);
}

.info-row {
  display: grid;
  grid-template-columns: 4.5em minmax(0, 1fr);
  column-gap: 12px;
  align-items: center;
  font-size: 13px;
  line-height: 1.4;
}

.info-label {
  color: #94a3b8;
  white-space: nowrap;
}

.info-value {
  color: #334155;
  text-align: right;
  word-break: break-all;
  font-variant-numeric: tabular-nums;
  font-feature-settings: "tnum" 1;
  font-weight: 500;
}

.info-value.is-deadline {
  color: #ea580c;
  font-weight: 500;
}

.card-foot {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.btn-main,
.btn-ghost {
  height: 34px;
  padding: 0 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.btn-main {
  border: none;
  background: #2563eb;
  color: #ffffff;
}

.btn-main:active {
  background: #1d4ed8;
}

.btn-ghost {
  border: 1px solid #fed7aa;
  background: #fff7ed;
  color: #c2410c;
}

.btn-copy {
  border-color: #bbf7d0;
  background: #f0fdf4;
  color: #15803d;
}

.load-status {
  text-align: center;
  color: #94a3b8;
  font-size: 12px;
  padding: 8px 0 18px;
  min-height: 28px;
}

/* 回到頂部：佔用底部固定高度，不覆蓋滾動條 */
.back-to-top {
  flex-shrink: 0;
  height: 50px;
  background: #3b82f6;
  color: #fff;
  border-radius: 0 0 20px 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 -2px 8px rgba(59, 130, 246, 0.25);
  transition: background 0.2s ease;
  font-weight: 600;
  font-size: 16px;
}

.back-to-top:active {
  background: #2563eb;
}

.back-to-top span {
  color: #fff;
}
</style>

<style>
.el-dialog.notification-detail-dialog .el-dialog__header,
.notification-detail-dialog.el-dialog .el-dialog__header {
  display: none;
}

.el-dialog.notification-detail-dialog.is-fullscreen,
.notification-detail-dialog.el-dialog.is-fullscreen {
  border-radius: 0;
}

.el-dialog.notification-detail-dialog .el-dialog__body,
.notification-detail-dialog.el-dialog .el-dialog__body {
  padding: 8px;
  max-height: calc(100vh - 48px);
  overflow-y: auto;
  box-sizing: border-box;
}
</style>
