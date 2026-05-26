<template>
  <el-dialog
    v-model="dialogVisible"
    title="失敗通知詳情"
    width="90%"
    top="1vh"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div v-loading="loading" class="detail-container">
      <!-- 基本信息 -->
      <el-card class="info-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span class="title">
              <el-icon><InfoFilled /></el-icon>
              基本信息
            </span>
          </div>
        </template>
        
        <el-descriptions :column="3" border>
          <el-descriptions-item label="通知標題">{{ detail.title }}</el-descriptions-item>
          <el-descriptions-item label="發送人">{{ detail.senderName }}</el-descriptions-item>
          <el-descriptions-item label="發送時間">{{ detail.sendTime }}</el-descriptions-item>
          <el-descriptions-item label="發送狀態">
            <el-tag :type="getStatusTagType(detail.sendStatus)" size="small">
              {{ getStatusText(detail.sendStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="應發送人數">{{ detail.totalCount }}</el-descriptions-item>
          <el-descriptions-item label="成功人數">
            <span style="color: #67c23a; font-weight: 500;">{{ detail.successCount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="失敗人數">
            <span style="color: #f56c6c; font-weight: 500;">{{ detail.failCount }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 发送失败的用户列表 -->
      <el-card class="info-card" shadow="never" style="margin-top: 16px;">
        <template #header>
          <div class="card-header">
            <span class="title">
              <el-icon><User /></el-icon>
              發送失敗用戶列表
            </span>
          </div>
        </template>

        <el-table
          :data="failedReadRecords"
          v-loading="failedReadLoading"
          style="width: 100%"
          empty-text="暫無數據"
        >
          <el-table-column prop="userId" label="用戶ID" width="150" show-overflow-tooltip />
          <el-table-column label="用戶類型" width="100" align="center">
            <template #default="scope">
              <el-tag size="small">{{ getUserTypeText(scope.row.userType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="studentUserId" label="關聯學生ID" width="150" show-overflow-tooltip />
          <el-table-column prop="studentName" label="學生名字" width="120" align="center">
            <template #default="scope">
              {{ scope.row.studentName || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="relation" label="關係" width="100" align="center">
            <template #default="scope">
              {{ scope.row.relation || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="是否已讀" width="100" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.isRead === '1' ? 'success' : 'info'" size="small">
                {{ scope.row.isRead === '1' ? '已讀' : '未讀' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="回覆狀態" width="100" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.replyStatus === '1' ? 'success' : 'warning'" size="small">
                {{ scope.row.replyStatus === '1' ? '已回覆' : '未回覆' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="創建時間" width="160" align="center" />
        </el-table>

        <!-- 分页组件 -->
        <div class="table-pagination" v-if="failedReadTotal > 0">
          <el-pagination
            v-model:current-page="failedReadPagination.currentPage"
            v-model:page-size="failedReadPagination.pageSize"
            :page-sizes="[10, 20, 50]"
            :total="failedReadTotal"
            layout="total, sizes, prev, pager, next"
            background
            @size-change="loadFailedReadRecords"
            @current-change="loadFailedReadRecords"
          />
        </div>
      </el-card>

      <!-- 重发失败记录 -->
      <el-card class="info-card" shadow="never" style="margin-top: 16px;">
        <template #header>
          <div class="card-header">
            <span class="title">
              <el-icon><Warning /></el-icon>
              重發失敗記錄
            </span>
          </div>
        </template>

        <el-table
          :data="resendFailRecords"
          v-loading="resendFailLoading"
          style="width: 100%"
          empty-text="暫無數據"
        >
          <el-table-column prop="userId" label="用戶ID" width="150" show-overflow-tooltip />
          <el-table-column label="用戶類型" width="100" align="center">
            <template #default="scope">
              <el-tag size="small">{{ getUserTypeText(scope.row.userType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="studentUserId" label="關聯學生ID" width="150" show-overflow-tooltip />
          <el-table-column prop="studentName" label="學生名字" width="120" align="center">
            <template #default="scope">
              {{ scope.row.studentName || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="relation" label="關係" width="100" align="center">
            <template #default="scope">
              {{ scope.row.relation || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="失敗次數" width="100" align="center">
            <template #default="scope">
              <el-tag type="danger" size="small">{{ scope.row.failCount }} 次</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="狀態" width="100" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.status === '1' ? 'info' : 'warning'" size="small">
                {{ scope.row.status === '1' ? '已放棄' : '待重發' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="失敗原因" min-width="300">
            <template #default="scope">
              <div class="fail-reasons">
                <div v-if="scope.row.failReason1" class="fail-item">
                  <span class="fail-label">第1次:</span>
                  <span class="fail-text">{{ scope.row.failReason1 }}</span>
                  <el-tooltip v-if="scope.row.failMessage1" :content="scope.row.failMessage1" placement="top">
                    <el-icon class="info-icon"><InfoFilled /></el-icon>
                  </el-tooltip>
                </div>
                <div v-if="scope.row.failReason2" class="fail-item">
                  <span class="fail-label">第2次:</span>
                  <span class="fail-text">{{ scope.row.failReason2 }}</span>
                  <el-tooltip v-if="scope.row.failMessage2" :content="scope.row.failMessage2" placement="top">
                    <el-icon class="info-icon"><InfoFilled /></el-icon>
                  </el-tooltip>
                </div>
                <div v-if="scope.row.failReason3" class="fail-item">
                  <span class="fail-label">第3次:</span>
                  <span class="fail-text">{{ scope.row.failReason3 }}</span>
                  <el-tooltip v-if="scope.row.failMessage3" :content="scope.row.failMessage3" placement="top">
                    <el-icon class="info-icon"><InfoFilled /></el-icon>
                  </el-tooltip>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="updateTime" label="最後更新時間" width="160" align="center" />
        </el-table>

        <!-- 分页组件 -->
        <div class="table-pagination" v-if="resendFailTotal > 0">
          <el-pagination
            v-model:current-page="resendFailPagination.currentPage"
            v-model:page-size="resendFailPagination.pageSize"
            :page-sizes="[10, 20, 50]"
            :total="resendFailTotal"
            layout="total, sizes, prev, pager, next"
            background
            @size-change="loadResendFailRecords"
            @current-change="loadResendFailRecords"
          />
        </div>
      </el-card>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">關閉</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { ElNotification } from 'element-plus'
import { InfoFilled, User, Warning } from '@element-plus/icons-vue'
import request from '@/utils/request'

export default {
  name: 'FailedNotificationDetail',
  components: {
    InfoFilled,
    User,
    Warning
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    sendRecordId: {
      type: Number,
      default: null
    }
  },
  emits: ['update:visible'],
  data() {
    return {
      loading: false,
      detail: {},
      // 发送失败用户列表
      failedReadLoading: false,
      failedReadRecords: [],
      failedReadTotal: 0,
      failedReadPagination: {
        currentPage: 1,
        pageSize: 10
      },
      // 重发失败记录
      resendFailLoading: false,
      resendFailRecords: [],
      resendFailTotal: 0,
      resendFailPagination: {
        currentPage: 1,
        pageSize: 10
      }
    }
  },
  computed: {
    dialogVisible: {
      get() {
        return this.visible
      },
      set(val) {
        this.$emit('update:visible', val)
      }
    }
  },
  watch: {
    visible(val) {
      if (val && this.sendRecordId) {
        this.loadDetail()
        this.loadFailedReadRecords()
        this.loadResendFailRecords()
      }
    }
  },
  methods: {
    async loadDetail() {
      this.loading = true
      try {
        const response = await request({
          url: `/system/notification/failedDetail/${this.sendRecordId}`,
          method: 'get'
        })
        
        if (response.code === 200 || response.code === 0) {
          this.detail = response.data || {}
        } else {
          ElNotification({ title: "操作失敗", message: response.msg || '加载失败', type: "error", duration: 4000 })
        }
      } catch (error) {
        console.error('加载详情错误:', error)
        ElNotification({ title: "操作失敗", message: '数据加载失败', type: "error", duration: 4000 })
      } finally {
        this.loading = false
      }
    },

    async loadFailedReadRecords() {
      this.failedReadLoading = true
      try {
        const response = await request({
          url: `/system/notification/failedReadRecords/${this.sendRecordId}`,
          method: 'get',
          params: {
            pageNum: this.failedReadPagination.currentPage,
            pageSize: this.failedReadPagination.pageSize
          }
        })
        
        if (response.code === 200 || response.code === 0) {
          this.failedReadRecords = response.rows || []
          this.failedReadTotal = response.total || 0
        } else {
          ElNotification({ title: "操作失敗", message: response.msg || '加载失败', type: "error", duration: 4000 })
        }
      } catch (error) {
        console.error('加载失败用户列表错误:', error)
        ElNotification({ title: "操作失敗", message: '数据加载失败', type: "error", duration: 4000 })
      } finally {
        this.failedReadLoading = false
      }
    },

    async loadResendFailRecords() {
      this.resendFailLoading = true
      try {
        const response = await request({
          url: `/system/notification/resendFailRecords/${this.sendRecordId}`,
          method: 'get',
          params: {
            pageNum: this.resendFailPagination.currentPage,
            pageSize: this.resendFailPagination.pageSize
          }
        })
        
        if (response.code === 200 || response.code === 0) {
          this.resendFailRecords = response.rows || []
          this.resendFailTotal = response.total || 0
        } else {
          ElNotification({ title: "操作失敗", message: response.msg || '加载失败', type: "error", duration: 4000 })
        }
      } catch (error) {
        console.error('加载重发失败记录错误:', error)
        ElNotification({ title: "操作失敗", message: '数据加载失败', type: "error", duration: 4000 })
      } finally {
        this.resendFailLoading = false
      }
    },

    handleClose() {
      this.detail = {}
      this.failedReadRecords = []
      this.failedReadTotal = 0
      this.failedReadPagination.currentPage = 1
      this.resendFailRecords = []
      this.resendFailTotal = 0
      this.resendFailPagination.currentPage = 1
    },

    getStatusTagType(status) {
      const statusMap = {
        '3': 'danger',
        '4': 'warning'
      }
      return statusMap[status] || 'info'
    },

    getStatusText(status) {
      const statusMap = {
        '3': '發送失敗',
        '4': '部分成功'
      }
      return statusMap[status] || '未知'
    },

    getUserTypeText(type) {
      const typeMap = {
        '1': '學生',
        '2': '家長',
        '3': '教師'
      }
      return typeMap[type] || '未知'
    }
  }
}
</script>

<style scoped>
.detail-container {
  /* 移除固定高度和内部滚动，交由 el-dialog 自身统一处理滚动，避免双重滚动条 */
}

.info-card {
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
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.title .el-icon {
  font-size: 18px;
  color: #409eff;
}

.fail-reasons {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.fail-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.fail-label {
  color: #909399;
  font-weight: 500;
  flex-shrink: 0;
}

.fail-text {
  color: #606266;
  flex: 1;
}

.info-icon {
  color: #409eff;
  cursor: pointer;
  font-size: 14px;
  flex-shrink: 0;
}

.info-icon:hover {
  color: #66b1ff;
}

.table-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding: 14px 0 0 0;
  border-top: 1px solid #eef0f4;
}

.table-pagination :deep(.el-pagination) {
  --el-pagination-button-bg-color: #ffffff;
  --el-pagination-hover-color: #2563eb;
}

.table-pagination :deep(.el-pagination .btn-prev),
.table-pagination :deep(.el-pagination .btn-next),
.table-pagination :deep(.el-pager li) {
  border-radius: 8px;
  font-weight: 500;
}

.table-pagination :deep(.el-pager li.is-active) {
  background: linear-gradient(135deg, #2563eb 0%, #1e40af 100%) !important;
  color: #ffffff;
}
</style>
