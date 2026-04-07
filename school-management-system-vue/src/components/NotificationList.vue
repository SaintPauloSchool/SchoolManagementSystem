<template>
  <div class="notification-list">
    <!-- 搜索欄 -->
    <div class="search-bar">
      <el-input
        v-model="searchQuery"
        placeholder="搜索通知標題或發送人..."
        clearable
        prefix-icon="Search"
        class="search-input"
      />
      <div class="filter-actions">
        <el-select 
          v-model="statusFilter" 
          placeholder="狀態篩選" 
          clearable
          class="status-select"
        >
          <el-option label="草稿" value="0" />
          <el-option label="已發布" value="1" />
          <el-option label="已撤回" value="2" />
        </el-select>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
      </div>
    </div>

    <!-- 列表頭部 -->
    <div class="list-header">
      <div class="header-info">
        <el-icon class="header-icon"><List /></el-icon>
        <span class="header-title">通知列表</span>
        <el-tag type="info" size="small" class="count-tag">
          {{ filteredNotifications.length }} 條記錄
        </el-tag>
      </div>
      <el-button plain @click="$emit('refresh')">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <!-- 表格 -->
    <div class="table-container">
      <el-table 
        :data="filteredNotifications" 
        style="width: 100%"
        v-loading="loading"
        stripe
      >
        <el-table-column prop="title" label="通知標題" min-width="220" show-overflow-tooltip>
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
              round
            >
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="發布時間" width="180" align="center" />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="scope">
            <div class="action-buttons">
              <el-button 
                size="small" 
                type="primary" 
                link
                @click="viewNotification(scope.row)"
              >
                <el-icon><View /></el-icon>
                查看
              </el-button>
              
              <el-button 
                v-if="type === 'mySend' && scope.row.status === '1'"
                size="small" 
                type="warning" 
                link
                @click="withdrawNotification(scope.row)"
              >
                <el-icon><Close /></el-icon>
                撤回
              </el-button>
              
              <el-button 
                v-if="type === 'mySend'"
                size="small" 
                type="danger" 
                link
                @click="deleteNotification(scope.row)"
              >
                <el-icon><Delete /></el-icon>
                刪除
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
      width="60%"
      :before-close="handleDetailClose"
      class="notification-detail-dialog"
      top="10vh"
    >
      <NotificationDetail 
        v-if="selectedNotification"
        :notification="selectedNotification"
      />
    </el-dialog>
  </div>
</template>

<script>
import { Search, Refresh, View, Close, Delete, List } from '@element-plus/icons-vue'
import NotificationDetail from './NotificationDetail.vue'

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
    }
  },
  emits: ['refresh'],
  data() {
    return {
      loading: false,
      searchQuery: '',
      statusFilter: '',
      pagination: {
        currentPage: 1,
        pageSize: 10,
        total: 0
      },
      detailDialogVisible: false,
      selectedNotification: null
    }
  },
  computed: {
    filteredNotifications() {
      let result = [...this.notifications]
      
      if (this.searchQuery) {
        const query = this.searchQuery.toLowerCase()
        result = result.filter(n => 
          n.title.toLowerCase().includes(query) ||
          n.senderName.toLowerCase().includes(query)
        )
      }
      
      if (this.statusFilter) {
        result = result.filter(n => n.status === this.statusFilter)
      }
      
      this.pagination.total = result.length
      
      const start = (this.pagination.currentPage - 1) * this.pagination.pageSize
      const end = start + this.pagination.pageSize
      return result.slice(start, end)
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
      this.pagination.currentPage = 1
    },

    handleSizeChange(val) {
      this.pagination.pageSize = val
      this.pagination.currentPage = 1
    },

    handleCurrentChange(val) {
      this.pagination.currentPage = val
    },

    async viewNotification(notification) {
      try {
        // 可以跳轉詳情頁或彈出詳情對話框
        // this.$router.push(`/notification/detail/${id}`)
        this.selectedNotification = notification
        this.detailDialogVisible = true
      } catch (error) {
        console.error('獲取詳情失敗:', error)
        this.$message.error('獲取詳情失敗')
      }
    },

    async withdrawNotification(notification) {
      try {
        await this.$confirm('確認撤回此通知嗎？', '提示', {
          confirmButtonText: '確定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        this.$message.success('撤回成功')
        this.$emit('refresh')
      } catch (error) {
        if (error !== 'cancel') {
          console.error('撤回失敗:', error)
          this.$message.error('撤回失敗')
        }
      }
    },

    async deleteNotification(notification) {
      try {
        await this.$confirm('確認刪除嗎？刪除後無法恢復！', '警告', {
          confirmButtonText: '確定',
          cancelButtonText: '取消',
          type: 'error'
        })
        
        this.$message.success('刪除成功')
        this.$emit('refresh')
      } catch (error) {
        if (error !== 'cancel') {
          console.error('刪除失敗:', error)
          this.$message.error('刪除失敗')
        }
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
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

/* 搜索欄 */
.search-bar {
  display: flex;
  gap: 16px;
  padding: 24px 28px;
  border-bottom: 2px solid #e5e7eb;
  background: linear-gradient(135deg, #f9fafb 0%, #ffffff 100%);
  transition: all 0.3s ease;
}

.search-bar:hover {
  background: linear-gradient(135deg, #ffffff 0%, #f9fafb 100%);
}

.search-input {
  flex: 1;
  max-width: 400px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  font-weight: 600;
}

.filter-actions {
  display: flex;
  gap: 12px;
}

.filter-actions .el-button {
  border-radius: 8px;
  font-weight: 600;
}

.status-select {
  width: 140px;
}

.status-select :deep(.el-input__wrapper) {
  border-radius: 8px;
  font-weight: 600;
}

/* 列表頭部 */
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 28px;
  background: linear-gradient(135deg, #ffffff 0%, #f9fafb 100%);
  border-bottom: 2px solid #e5e7eb;
  transition: all 0.3s ease;
}

.list-header:hover {
  background: linear-gradient(135deg, #f9fafb 0%, #ffffff 100%);
}

.header-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  color: #3b82f6;
  font-size: 20px;
  animation: iconFloat 3s ease-in-out infinite;
}

@keyframes iconFloat {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-4px);
  }
}

.header-title {
  font-size: 17px;
  font-weight: 700;
  color: #111827;
  letter-spacing: 0.3px;
}

.count-tag {
  margin-left: 8px;
  border-radius: 8px;
  font-weight: 600;
  padding: 4px 12px;
}

/* 表格區域 */
.table-container {
  flex: 1;
  overflow: auto;
  padding: 0;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
}

.table-container :deep(.el-table) {
  border: none;
}

/* 表格列對齊優化 */
.table-container :deep(.el-table__header th) {
  text-align: center;
}

.table-container :deep(.el-table__body td) {
  text-align: center;
}

/* 標題列保持左對齊 */
.table-container :deep(.el-table__body tr td:first-child) {
  text-align: left;
}

.title-link {
  font-weight: 600;
  color: #3b82f6;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.title-link:hover {
  color: #2563eb;
  transform: translateX(4px);
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.action-buttons .el-button {
  border-radius: 8px;
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.action-buttons .el-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* 對話框樣式優化 */
.notification-detail-dialog :deep(.el-dialog) {
  border-radius: 24px;
}

.notification-detail-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
}

.notification-detail-dialog :deep(.el-dialog__title) {
  color: #1e40af;
}

/* 分頁區域 */
.pagination-area {
  padding: 20px 28px;
  background: linear-gradient(135deg, #f9fafb 0%, #ffffff 100%);
  border-top: 2px solid #e5e7eb;
  display: flex;
  justify-content: flex-end;
  transition: all 0.3s ease;
}

.pagination-area:hover {
  background: linear-gradient(135deg, #ffffff 0%, #f9fafb 100%);
}

/* 響應式設計 */
@media (max-width: 1200px) {
  .search-bar {
    flex-wrap: wrap;
    padding: 20px 24px;
  }
  
  .search-input {
    max-width: 100%;
  }
  
  .filter-actions {
    width: 100%;
    justify-content: space-between;
  }
}

@media (max-width: 768px) {
  .search-bar {
    padding: 16px 20px;
  }
  
  .list-header {
    padding: 16px 20px;
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }
  
  .pagination-area {
    padding: 16px 20px;
    justify-content: center;
  }
  
  .action-buttons {
    flex-direction: column;
  }
}
</style>
