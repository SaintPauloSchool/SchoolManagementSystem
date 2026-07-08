<template>
  <div class="task-log-container">
    <el-card class="box-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">
            <el-icon><VideoPlay /></el-icon>
            定時任務執行日誌
          </span>
          
          <!-- 手動執行區塊 -->
          <div class="manual-execute-section">
            <span class="execute-label">手動執行定時任務：</span>
            <el-select 
              v-model="selectedTask" 
              placeholder="請選擇要執行的任務" 
              class="task-select"
              style="width: 250px"
            >
              <el-option
                v-for="item in taskOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              >
              </el-option>
            </el-select>
            <el-button 
              type="primary" 
              @click="executeSelectedTask" 
              :loading="isExecuting"
              :disabled="!selectedTask"
            >
              <el-icon style="margin-right: 4px"><VideoPlay /></el-icon>
              手動觸發
            </el-button>
          </div>
        </div>
      </template>

    <!-- 查詢過濾區 -->
    <div class="filter-section">
      <el-form :inline="true" :model="queryParams" class="demo-form-inline">
        <el-form-item label="任務名稱">
          <el-input v-model="queryParams.taskName" placeholder="輸入任務名稱" clearable @keyup.enter="handleQuery"></el-input>
        </el-form-item>
        <el-form-item label="執行狀態">
          <el-select v-model="queryParams.status" placeholder="全部狀態" clearable style="width: 120px">
            <el-option label="成功" value="0"></el-option>
            <el-option label="失敗" value="1"></el-option>
            <el-option label="部分失敗" value="2"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery"><el-icon><Search /></el-icon> 查詢</el-button>
          <el-button @click="resetQuery"><el-icon><Refresh /></el-icon> 重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 列表區 -->
    <el-table 
      v-loading="loading" 
      :data="logList" 
      style="width: 100%"
      :row-style="{ height: '56px' }"
      :cell-style="{ padding: '14px 0' }"
      :header-cell-style="{background:'#f5f7fa',color:'#606266'}"
      empty-text="暫無數據"
    >
      <el-table-column label="日誌編號" prop="logId" width="100" align="center" />
        <el-table-column label="任務名稱" prop="taskName" min-width="180" show-overflow-tooltip />
        <el-table-column label="Bean名稱" prop="beanName" min-width="180" show-overflow-tooltip />
        <el-table-column label="方法名稱" prop="methodName" min-width="180" show-overflow-tooltip />
        <el-table-column label="成功數量" prop="successCount" align="center" width="90">
          <template #default="scope">
            {{ scope.row.successCount != null ? scope.row.successCount : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="失敗數量" prop="failCount" align="center" width="90">
          <template #default="scope">
            <span :class="{'error-text': scope.row.failCount > 0}">{{ scope.row.failCount != null ? scope.row.failCount : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="執行耗時" align="center" width="120">
          <template #default="scope">
            {{ scope.row.duration }} 毫秒
          </template>
        </el-table-column>
        <el-table-column label="執行時間" prop="executionTime" align="center" width="180" />
        <el-table-column label="失敗原因" prop="failReason" min-width="250" show-overflow-tooltip>
          <template #default="scope">
            <span v-if="scope.row.status === '1' || scope.row.status === '2'" class="error-text">{{ scope.row.failReason }}</span>
            <span v-else class="success-text">-</span>
          </template>
        </el-table-column>
        <el-table-column label="執行狀態" align="center" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === '0' ? 'success' : (scope.row.status === '2' ? 'warning' : 'danger')">
              {{ scope.row.status === '0' ? '成功' : (scope.row.status === '2' ? '部分失敗' : '失敗') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="是否處理" align="center" width="100">
          <template #default="scope">
            <el-switch
              v-model="scope.row.isProcessed"
              active-value="1"
              inactive-value="0"
              @change="handleStatusChange(scope.row)"
            ></el-switch>
          </template>
        </el-table-column>
    </el-table>

    <!-- 分頁 -->
    <div class="pagination-container" v-if="total > 0">
      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :page-sizes="[10, 20, 30, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        background
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </el-card>
  </div>
</template>

<script>
import { ElMessageBox, ElNotification } from 'element-plus'
import { Search, Refresh, VideoPlay } from '@element-plus/icons-vue'
import request from '@/utils/request'

export default {
  name: 'SysTaskLogList',
  components: {
    Search,
    Refresh,
    VideoPlay
  },
  data() {
    return {
      // 遮罩層
      loading: true,
      // 執行中狀態
      isExecuting: false,
      // 總筆數
      total: 0,
      // 日誌列表
      logList: [],
      // 查詢參數
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        taskName: undefined,
        status: undefined
      },
      // 選擇要手動執行的任務
      selectedTask: '',
      // 定時任務選項
      taskOptions: [
        { label: '檢查失敗任務通知', value: 'failedTaskNotifierTask|executeTask' },
        { label: '家校通訊錄部門數據同步', value: 'departmentSyncTask|executeTask' },
        { label: '定時提示家長回復通知', value: 'notificationReminderTask|executeTask' },
        { label: '定時重新發送失敗通知', value: 'notificationResendTask|executeTask' },
        { label: '家校通訊錄同步', value: 'schoolFamilyContactSyncTask|executeTask' },
        { label: '每日學生手冊通知發送', value: 'schoolNoticeTask|executeTask' },
        { label: '企業微信部門與成員同步', value: 'wecomSchoolDepartmentTask|executeTask' },
        { label: '考勤拍卡通知發送', value: 'attendanceNotifyTask|executeTask' }
      ]
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查詢日誌列表 */
    async getList() {
      this.loading = true
      try {
        const response = await request({
          url: '/system/taskLog/list',
          method: 'get',
          params: this.queryParams
        })
        if (response.code === 200 || response.code === 0) {
          this.logList = response.rows
          this.total = response.total
        }
      } catch (error) {
        console.error('獲取任務日誌失敗', error)
      } finally {
        this.loading = false
      }
    },
    
    /** 搜尋按鈕操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    
    /** 重置按鈕操作 */
    resetQuery() {
      this.queryParams.taskName = undefined
      this.queryParams.status = undefined
      this.handleQuery()
    },
    
    /** 分頁大小改變 */
    handleSizeChange(val) {
      this.queryParams.pageSize = val
      this.getList()
    },
    
    /** 分頁頁碼改變 */
    handleCurrentChange(val) {
      this.queryParams.pageNum = val
      this.getList()
    },

    /** 執行選擇的任務 */
    executeSelectedTask() {
      if (!this.selectedTask) return;
      
      const selectedOption = this.taskOptions.find(item => item.value === this.selectedTask);
      const [beanName, methodName] = this.selectedTask.split('|');

      ElMessageBox.confirm(
        `確定要手動執行「${selectedOption.label}」嗎？`,
        '提示',
        {
          confirmButtonText: '確定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(async () => {
        this.isExecuting = true;
        try {
          const response = await request({
            url: '/system/taskLog/execute',
            method: 'post',
            data: { beanName, methodName }
          });
          
          if (response.code === 200 || response.code === 0) {
            ElNotification({
              title: '觸發成功',
              message: response.msg || '任務已手動觸發執行',
              type: 'success',
              duration: 3000
            })
            // 延遲一下刷新列表，等待任務執行完畢產生紀錄
            setTimeout(() => {
              this.getList();
            }, 1000);
          } else {
            ElNotification({
              title: '觸發失敗',
              message: response.msg || '任務觸發失敗',
              type: 'error',
              duration: 4000
            })
          }
        } catch (error) {
          console.error('執行任務失敗', error)
          ElNotification({
            title: '執行失敗',
            message: '任務執行發生錯誤，請稍後再試',
            type: 'error',
            duration: 4000
          })
        } finally {
          this.isExecuting = false;
        }
      }).catch(() => {
        // 取消
      });
    },

    /** 修改處理狀態 */
    async handleStatusChange(row) {
      try {
        const response = await request({
          url: '/system/taskLog',
          method: 'put',
          data: {
            logId: row.logId,
            isProcessed: row.isProcessed
          }
        });
        if (response.code === 200 || response.code === 0) {
          ElNotification({
            title: '修改成功',
            message: row.isProcessed === '1' ? '已成功標記為已處理' : '已成功標記為未處理',
            type: 'success',
            duration: 3000
          })
        } else {
          row.isProcessed = row.isProcessed === "0" ? "1" : "0";
          ElNotification({
            title: '修改失敗',
            message: response.msg || '狀態修改失敗',
            type: 'error',
            duration: 4000
          })
        }
      } catch (error) {
        row.isProcessed = row.isProcessed === "0" ? "1" : "0";
        console.error('修改狀態失敗', error);
        ElNotification({
          title: '修改失敗',
          message: '狀態修改發生錯誤，請稍後再試',
          type: 'error',
          duration: 4000
        })
      }
    }
  }
}
</script>

<style scoped>
.task-log-container {
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

.manual-execute-section {
  display: flex;
  align-items: center;
  background-color: #f0f9eb;
  padding: 8px 16px;
  border-radius: 6px;
  border: 1px solid #e1f3d8;
}

.execute-label {
  font-size: 14px;
  color: #67c23a;
  font-weight: 500;
  margin-right: 12px;
}

.task-select {
  margin-right: 12px;
}

.filter-section {
  margin-bottom: 20px;
  background-color: #f8f9fa;
  padding: 16px;
  border-radius: 6px;
}

.demo-form-inline {
  display: flex;
  flex-wrap: wrap;
}

.el-form-item {
  margin-bottom: 0;
  margin-right: 24px;
}


.error-text {
  color: #F56C6C;
}

.success-text {
  color: #909399;
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
