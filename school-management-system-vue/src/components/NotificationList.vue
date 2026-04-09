<template>
  <div class="notification-list">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchQuery"
        placeholder="搜索通知标题或发送人..."
        clearable
        prefix-icon="Search"
        class="search-input"
        @keyup.enter="handleSearch"
      />
      <div class="filter-actions">
        <el-select 
          v-model="statusFilter" 
          placeholder="状态筛选" 
          clearable
          class="status-select"
        >
          <el-option label="草稿" value="0" />
          <el-option label="已发布" value="1" />
          <el-option label="已撤回" value="2" />
        </el-select>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
      </div>
    </div>

    <!-- 列表头部 -->
    <div class="list-header">
      <div class="header-info">
        <el-icon class="header-icon"><List /></el-icon>
        <span class="header-title">{{ listTitle }}</span>
        <el-tag type="info" size="small" class="count-tag">
          {{ pagination.total }} 条记录
        </el-tag>
      </div>
      <el-button plain @click="handleRefresh">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <!-- 表格 -->
    <div class="table-container">
      <el-table 
        :data="displayData" 
        style="width: 100%"
        v-loading="loading"
        stripe
        empty-text="暂无数据"
        :row-style="{ height: '56px' }"
        :cell-style="{ padding: '14px 0' }"
      >
        <el-table-column prop="title" label="通知标题" min-width="200" show-overflow-tooltip>
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
        <el-table-column prop="senderName" label="发送人" width="120" align="center" />
        <el-table-column label="状态" width="100" align="center">
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
        <el-table-column prop="replyDeadline" label="回复截止" width="160" align="center">
          <template #default="scope">
            <span v-if="scope.row.replyDeadline" class="deadline-text">
              {{ scope.row.replyDeadline }}
            </span>
            <span v-else class="no-deadline">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="160" align="center" />
        <el-table-column label="操作" width="240" fixed="right" align="center">
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
                @click="withdrawNotification(scope.row)"
              >
                <el-icon><Close /></el-icon>
                撤回
              </el-button>
              
              <el-button 
                v-if="type === 'mySend'"
                size="small" 
                type="danger"
                @click="deleteNotification(scope.row)"
              >
                <el-icon><Delete /></el-icon>
                删除
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
      title=""
      width="65%"
      :before-close="handleDetailClose"
      class="notification-detail-dialog"
      top="6vh"
      :show-close="true"
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
      statusFilter: '',
      detailDialogVisible: false,
      selectedNotification: null
    }
  },
  computed: {
    listTitle() {
      const titleMap = {
        'ccToMe': '抄送我的',
        'mySend': '我发送的'
      }
      return titleMap[this.type] || '通知列表'
    },
    displayData() {
      // 直接使用后端返回的分页数据，不做前端切片
      let result = [...this.notifications]
      
      // 搜索过滤（仅在前端过滤当前页数据）
      if (this.searchQuery) {
        const query = this.searchQuery.toLowerCase()
        result = result.filter(n => 
          n.title.toLowerCase().includes(query) ||
          (n.senderName && n.senderName.toLowerCase().includes(query))
        )
      }
      
      // 状态过滤
      if (this.statusFilter) {
        result = result.filter(n => n.status === this.statusFilter)
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
        '1': '已发布',
        '2': '已撤回'
      }
      return statusMap[status] || '未知'
    },

    handleSearch() {
      this.$emit('page-change', {
        pageNum: 1,
        pageSize: this.pagination.pageSize
      })
    },

    handleSizeChange(val) {
      this.$emit('page-change', {
        pageNum: 1,
        pageSize: val
      })
    },

    handleCurrentChange(val) {
      this.$emit('page-change', {
        pageNum: val,
        pageSize: this.pagination.pageSize
      })
    },

    handleRefresh() {
      this.searchQuery = ''
      this.statusFilter = ''
      this.$emit('refresh')
    },

    async viewNotification(notification) {
      try {
        this.loading = true
        // 获取完整的通知详情（包含接收对象、抄送对象、问题等）
        const response = await request({
          url: `/system/notification/${notification.notificationId}`,
          method: 'get'
        })
        
        if (response.code === 200 || response.code === 0) {
          this.selectedNotification = response.data
          this.detailDialogVisible = true
        }
      } catch (error) {
        console.error('获取详情失败:', error)
        this.$message.error('获取详情失败')
      } finally {
        this.loading = false
      }
    },

    async withdrawNotification(notification) {
      try {
        await this.$confirm('确认撤回此通知吗？撤回后接收者将无法查看。', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        this.loading = true
        const response = await request({
          url: `/system/notification/withdraw/${notification.notificationId}`,
          method: 'put'
        })
        
        if (response.code === 200 || response.code === 0) {
          this.$message.success('撤回成功')
          this.$emit('refresh')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('撤回失败:', error)
          this.$message.error('撤回失败')
        }
      } finally {
        this.loading = false
      }
    },

    async deleteNotification(notification) {
      try {
        await this.$confirm('确认删除吗？删除后将无法恢复！', '警告', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'error'
        })
        
        this.loading = true
        const response = await request({
          url: `/system/notification/${notification.notificationId}`,
          method: 'delete'
        })
        
        if (response.code === 200 || response.code === 0) {
          this.$message.success('删除成功')
          this.$emit('refresh')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除失败:', error)
          this.$message.error('删除失败')
        }
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
  background-color: #ffffff;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
}

/* 搜索栏 */
.search-bar {
  display: flex;
  gap: 16px;
  padding: 20px 24px;
  border-bottom: 1px solid #e5e7eb;
  background-color: #f9fafb;
  transition: background-color 0.2s ease;
}

.search-bar:hover {
  background-color: #f3f4f6;
}

.search-input {
  flex: 1;
  max-width: 400px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 8px;
}

.filter-actions {
  display: flex;
  gap: 12px;
}

.filter-actions .el-button {
  border-radius: 8px;
}

.status-select {
  width: 140px;
}

.status-select :deep(.el-input__wrapper) {
  border-radius: 8px;
}

/* 列表头部 */
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background-color: #ffffff;
  border-bottom: 1px solid #e5e7eb;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon {
  color: #3b82f6;
  font-size: 20px;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.count-tag {
  margin-left: 8px;
  border-radius: 6px;
  padding: 2px 10px;
}

/* 表格区域 */
.table-container {
  overflow: visible;
  padding: 0;
  background-color: #ffffff;
}

/* 增加表格行高和单元格内边距 */
.table-container :deep(.el-table) {
  border: none;
}

.table-container :deep(.el-table__row) {
  height: 64px;
}

.table-container :deep(.el-table__row td) {
  padding: 18px 0;
}

.table-container :deep(.el-table th) {
  background-color: #f9fafb;
  color: #374151;
  font-weight: 600;
  text-align: center;
  height: 52px;
  padding: 16px 0;
}

.table-container :deep(.el-table td) {
  text-align: center;
}

/* 标题列保持左对齐 */
.table-container :deep(.el-table__body tr td:first-child) {
  text-align: left;
}

.title-link {
  font-weight: 500;
  color: #3b82f6;
  transition: color 0.2s ease;
}

.title-link:hover {
  color: #2563eb;
}

.deadline-text {
  color: #f59e0b;
  font-weight: 500;
}

.no-deadline {
  color: #9ca3af;
}

.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.action-buttons .el-button {
  border-radius: 6px;
  font-weight: 500;
  padding: 6px 14px;
  border: none;
  box-shadow: none !important;
}

.action-buttons .el-button:hover {
  box-shadow: none !important;
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

/* 对话框样式 */
.notification-detail-dialog :deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
}

.notification-detail-dialog :deep(.el-dialog__header) {
  display: none;
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

/* 分页区域 */
.pagination-area {
  padding: 16px 24px;
  background-color: #f9fafb;
  border-top: 1px solid #e5e7eb;
  display: flex;
  justify-content: flex-end;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .search-bar {
    flex-wrap: wrap;
    padding: 16px 20px;
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
    padding: 12px 16px;
  }
  
  .list-header {
    padding: 12px 16px;
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
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
