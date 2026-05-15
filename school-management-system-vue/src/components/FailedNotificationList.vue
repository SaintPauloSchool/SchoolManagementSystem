<template>
  <div class="failed-notification-list">
    <el-card class="box-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">
            <el-icon><Warning /></el-icon>
            查詢失敗通知
          </span>
          <el-button type="primary" :icon="Refresh" @click="loadFailedNotifications" size="small">
            刷新
          </el-button>
        </div>
      </template>

      <!-- 数据表格 -->
      <el-table
        v-loading="loading"
        :data="failedNotifications"
        style="width: 100%"
        :row-style="{ height: '56px' }"
        :cell-style="{ padding: '14px 0' }"
        empty-text="暫無數據"
      >
        <el-table-column prop="title" label="通知標題" min-width="250" show-overflow-tooltip>
          <template #default="scope">
            <el-link 
              type="primary" 
              @click="viewDetail(scope.row)" 
              class="title-link"
              :underline="false"
            >
              {{ scope.row.title }}
            </el-link>
          </template>
        </el-table-column>
        
        <el-table-column prop="senderName" label="發送人" width="120" align="center" />
        
        <el-table-column label="發送狀態" width="120" align="center">
          <template #default="scope">
            <el-tag 
              :type="getStatusTagType(scope.row.sendStatus)"
              size="small"
              effect="light"
            >
              {{ getStatusText(scope.row.sendStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="totalCount" label="應發送人數" width="110" align="center" />
        
        <el-table-column prop="successCount" label="成功人數" width="100" align="center">
          <template #default="scope">
            <span style="color: #67c23a; font-weight: 500;">{{ scope.row.successCount }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="failCount" label="失敗人數" width="100" align="center">
          <template #default="scope">
            <span style="color: #f56c6c; font-weight: 500;">{{ scope.row.failCount }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="sendTime" label="發送時間" width="160" align="center">
          <template #default="scope">
            <div class="datetime-block">
              <span class="date-part">{{ scope.row.sendTime ? scope.row.sendTime.split(' ')[0] : '-' }}</span>
              <span class="time-part">{{ scope.row.sendTime ? scope.row.sendTime.split(' ')[1] : '-' }}</span>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="scope">
            <div class="action-buttons">
              <el-button 
                size="small" 
                type="primary"
                @click="viewDetail(scope.row)"
              >
                <el-icon><View /></el-icon>
                查看
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <el-empty 
        v-if="!loading && failedNotifications.length === 0" 
        description="暫無發送失敗的通知"
        :image-size="120"
      />

      <!-- 分页组件 -->
      <div class="pagination-container" v-if="total > 0">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 详情对话框 -->
    <FailedNotificationDetail 
      v-model:visible="detailDialogVisible" 
      :send-record-id="currentSendRecordId"
    />
  </div>
</template>

<script>
import { Warning, Refresh, View } from '@element-plus/icons-vue'
import FailedNotificationDetail from './FailedNotificationDetail.vue'
import request from '@/utils/request'

export default {
  name: 'FailedNotificationList',
  components: {
    FailedNotificationDetail
  },
  data() {
    return {
      loading: false,
      failedNotifications: [],
      detailDialogVisible: false,
      currentSendRecordId: null,
      pagination: {
        currentPage: 1,
        pageSize: 10
      },
      total: 0
    }
  },
  mounted() {
    this.loadFailedNotifications()
  },
  methods: {
    async loadFailedNotifications() {
      this.loading = true
      try {
        const response = await request({
          url: '/system/notification/failedList',
          method: 'get',
          params: {
            pageNum: this.pagination.currentPage,
            pageSize: this.pagination.pageSize
          }
        })
        
        if (response.code === 200 || response.code === 0) {
          this.failedNotifications = response.rows || []
          this.total = response.total || 0
        } else {
          this.$message.error(response.msg || '加载失败')
        }
      } catch (error) {
        console.error('加载失败通知列表错误:', error)
        this.$message.error('数据加载失败')
      } finally {
        this.loading = false
      }
    },

    handleSizeChange(val) {
      this.pagination.pageSize = val
      this.pagination.currentPage = 1
      this.loadFailedNotifications()
    },

    handleCurrentChange(val) {
      this.pagination.currentPage = val
      this.loadFailedNotifications()
    },

    viewDetail(row) {
      this.currentSendRecordId = row.sendRecordId
      this.detailDialogVisible = true
    },

    getStatusTagType(status) {
      const statusMap = {
        '3': 'danger',  // 发送失败
        '4': 'warning'  // 部分成功
      }
      return statusMap[status] || 'info'
    },

    getStatusText(status) {
      const statusMap = {
        '3': '發送失敗',
        '4': '部分成功'
      }
      return statusMap[status] || '未知'
    }
  }
}
</script>

<style scoped>
.failed-notification-list {
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
  color: #e6a23c;
}

.title-link {
  font-weight: 500;
  transition: all 0.3s;
}

.title-link:hover {
  transform: translateX(2px);
}

.datetime-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  line-height: 1.4;
}

.date-part {
  font-size: 13px;
  color: #606266;
}

.time-part {
  font-size: 12px;
  color: #909399;
}

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

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding: 14px 24px;
  background: #fafbfc;
  border-top: 1px solid #eef0f4;
}

.pagination-container :deep(.el-pagination) {
  --el-pagination-button-bg-color: #ffffff;
  --el-pagination-hover-color: #2563eb;
}

.pagination-container :deep(.el-pagination .btn-prev),
.pagination-container :deep(.el-pagination .btn-next),
.pagination-container :deep(.el-pager li) {
  border-radius: 8px;
  font-weight: 500;
}

.pagination-container :deep(.el-pager li.is-active) {
  background: linear-gradient(135deg, #2563eb 0%, #1e40af 100%) !important;
  color: #ffffff;
}
</style>
