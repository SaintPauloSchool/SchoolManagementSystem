<template>
  <div class="task-log-container">
    <el-card class="box-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">
            <el-icon><VideoPlay /></el-icon>
            定時任務執行日誌
          </span>

          <div class="header-actions">
            <div class="toolbar-group">
              <span class="toolbar-label">任務配置</span>
              <el-button plain @click="openTaskManageDialog">
                <el-icon class="btn-icon"><Setting /></el-icon>
                定時任務管理
              </el-button>
            </div>

            <div class="toolbar-divider" />

            <div class="toolbar-group">
              <span class="toolbar-label">手動執行</span>
              <el-select
                v-model="selectedTask"
                placeholder="請選擇要執行的任務"
                class="task-select"
                popper-class="task-select-popper"
                placement="bottom-start"
                :offset="4"
                :show-arrow="false"
                :fit-input-width="true"
              >
                <el-option
                  v-for="item in taskOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
              <el-button
                type="primary"
                @click="executeSelectedTask"
                :loading="isExecuting"
                :disabled="!selectedTask"
              >
                <el-icon class="btn-icon"><VideoPlay /></el-icon>
                手動觸發
              </el-button>
            </div>
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
    <div class="table-wrapper">
    <el-table
      v-loading="loading"
      :data="logList"
      style="width: 100%"
      :row-style="{ height: '56px' }"
      :cell-style="{ padding: '14px 0' }"
      :header-cell-style="{background:'#f5f7fa',color:'#606266'}"
      empty-text="暫無數據"
    >
      <el-table-column label="日誌編號" prop="logId" width="90" align="center" />
      <el-table-column label="任務名稱" prop="taskName" min-width="150" show-overflow-tooltip />
      <el-table-column label="Bean名稱" prop="beanName" min-width="130" show-overflow-tooltip />
      <el-table-column label="方法名稱" prop="methodName" min-width="130" show-overflow-tooltip />
      <el-table-column label="成功數量" prop="successCount" align="center" width="85">
        <template #default="scope">
          {{ scope.row.successCount != null ? scope.row.successCount : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="失敗數量" prop="failCount" align="center" width="85">
        <template #default="scope">
          <span :class="{'error-text': scope.row.failCount > 0}">{{ scope.row.failCount != null ? scope.row.failCount : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="執行耗時" align="center" width="105">
        <template #default="scope">
          {{ scope.row.duration }} 毫秒
        </template>
      </el-table-column>
      <el-table-column label="執行時間" prop="executionTime" align="center" width="170" />
      <el-table-column label="失敗原因" prop="failReason" min-width="180" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="scope.row.status === '1' || scope.row.status === '2'" class="error-text">{{ scope.row.failReason }}</span>
          <span v-else class="success-text">-</span>
        </template>
      </el-table-column>
      <el-table-column label="執行狀態" align="center" width="105">
        <template #default="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : (scope.row.status === '2' ? 'warning' : 'danger')">
            {{ scope.row.status === '0' ? '成功' : (scope.row.status === '2' ? '部分失敗' : '失敗') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="日誌已處理" align="center" width="115" fixed="right">
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
    </div>

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

  <!-- 定時任務管理彈窗 -->
  <el-dialog
    v-model="taskManageVisible"
    title="定時任務管理"
    width="1100px"
    align-center
    class="task-manage-dialog"
    destroy-on-close
    @open="loadScheduledTasks"
  >
    <el-table
      v-loading="taskManageLoading"
      :data="pagedScheduledTaskList"
      class="task-manage-table"
      style="width: 100%"
      :row-style="{ height: '52px' }"
      :cell-style="{ padding: '14px 0', fontSize: '15px' }"
      :header-cell-style="{ background: '#f5f7fa', color: '#303133', fontSize: '15px', fontWeight: '600', padding: '13px 0' }"
      empty-text="暫無定時任務配置"
    >
      <el-table-column label="任務名稱" prop="taskName" min-width="180" show-overflow-tooltip />
      <el-table-column label="定時設定" min-width="260">
        <template #default="scope">
          <div class="cron-display">
            <span class="cron-desc">{{ describeCron(scope.row.cronExpression) }}</span>
            <span class="cron-text">{{ scope.row.cronExpression }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="最近執行" align="center" width="180">
        <template #default="scope">
          {{ scope.row.lastExecutionTime || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="最近狀態" align="center" width="100">
        <template #default="scope">
          <el-tag
            v-if="scope.row.lastStatus != null"
            :type="scope.row.lastStatus === '0' ? 'success' : (scope.row.lastStatus === '2' ? 'warning' : 'danger')"
            size="default"
          >
            {{ formatLastStatus(scope.row.lastStatus) }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="自動執行" align="center" width="100">
        <template #default="scope">
          <el-switch
            v-model="scope.row.enabled"
            active-value="1"
            inactive-value="0"
            :loading="scope.row._updating"
            @change="handleTaskEnabledChange(scope.row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="100" fixed="right">
        <template #default="scope">
          <div class="action-buttons">
            <el-button size="small" type="primary" :icon="Edit" @click="openCronDialog(scope.row)">
              編輯
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination-container" v-if="scheduledTaskList.length > 0">
      <el-pagination
        v-model:current-page="taskManagePagination.pageNum"
        v-model:page-size="taskManagePagination.pageSize"
        :page-sizes="[5, 10, 20]"
        :total="scheduledTaskList.length"
        layout="total, sizes, prev, pager, next"
        background
      />
    </div>
  </el-dialog>

  <el-dialog
    v-model="cronDialogVisible"
    title="設定執行時間"
    width="560px"
    class="cron-edit-dialog"
    destroy-on-close
    @closed="resetCronDialog"
  >
    <el-form label-width="108px" class="cron-edit-form">
      <el-form-item label="任務名稱">
        <span class="cron-task-name">{{ cronEditRow?.taskName || '-' }}</span>
      </el-form-item>
      <el-form-item label="定時方式">
        <el-select v-model="cronForm.type" style="width: 100%;">
          <el-option
            v-for="item in cronTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item v-if="cronForm.type === CRON_TYPES.EVERY_N_MINUTES" label="執行間隔">
        <el-input-number v-model="cronForm.intervalMinutes" :min="1" :max="59" />
        <span class="form-unit">分鐘</span>
      </el-form-item>

      <el-form-item
        v-if="[CRON_TYPES.DAILY, CRON_TYPES.WEEKDAYS, CRON_TYPES.WEEKLY].includes(cronForm.type)"
        label="執行時間"
      >
        <el-time-picker
          v-model="cronForm.time"
          format="HH:mm"
          value-format="HH:mm"
          placeholder="選擇時間"
          style="width: 100%;"
        />
      </el-form-item>

      <el-form-item v-if="cronForm.type === CRON_TYPES.WEEKLY" label="星期">
        <el-checkbox-group v-model="cronForm.weekDays" class="weekday-group">
          <el-checkbox
            v-for="item in weekdayOptions"
            :key="item.value"
            :label="item.value"
          >
            {{ item.label }}
          </el-checkbox>
        </el-checkbox-group>
      </el-form-item>

      <el-form-item v-if="cronForm.type === CRON_TYPES.HOURLY_RANGE" label="執行時段">
        <div class="hour-range">
          <el-select v-model="cronForm.startHour" style="width: 120px;">
            <el-option
              v-for="hour in hourOptions"
              :key="'start-' + hour"
              :label="`${String(hour).padStart(2, '0')}:00`"
              :value="hour"
            />
          </el-select>
          <span class="range-separator">至</span>
          <el-select v-model="cronForm.endHour" style="width: 120px;">
            <el-option
              v-for="hour in hourOptions"
              :key="'end-' + hour"
              :label="`${String(hour).padStart(2, '0')}:00`"
              :value="hour"
            />
          </el-select>
          <span class="form-unit">每小時執行</span>
        </div>
      </el-form-item>

      <el-form-item label="執行說明">
        <span class="cron-preview-desc">{{ cronPreviewDesc }}</span>
      </el-form-item>
      <el-form-item label="Cron 表達式">
        <code class="cron-preview-code">{{ cronPreviewExpression }}</code>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="cronDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="cronSaving" @click="saveCronFromDialog">保存</el-button>
    </template>
  </el-dialog>
  </div>
</template>

<script>
import { ElMessageBox, ElNotification } from 'element-plus'
import { Search, Refresh, VideoPlay, Setting, Edit } from '@element-plus/icons-vue'
import request from '@/utils/request'
import {
  CRON_TYPES,
  CRON_TYPE_OPTIONS,
  WEEKDAY_OPTIONS,
  buildCronExpression,
  createDefaultCronForm,
  describeCronExpression,
  describeCronForm,
  parseCronExpression,
  validateCronForm
} from '@/utils/cronSchedule'

export default {
  name: 'SysTaskLogList',
  components: {
    Search,
    Refresh,
    VideoPlay,
    Setting
  },
  data() {
    return {
      Edit,
      CRON_TYPES,
      cronTypeOptions: CRON_TYPE_OPTIONS,
      weekdayOptions: WEEKDAY_OPTIONS,
      hourOptions: Array.from({ length: 24 }, (_, index) => index),
      loading: true,
      isExecuting: false,
      total: 0,
      logList: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        taskName: undefined,
        status: undefined
      },
      selectedTask: '',
      taskOptions: [],
      taskManageVisible: false,
      taskManageLoading: false,
      scheduledTaskList: [],
      taskManagePagination: {
        pageNum: 1,
        pageSize: 5
      },
      cronDialogVisible: false,
      cronEditRow: null,
      cronForm: createDefaultCronForm(),
      cronSaving: false
    }
  },
  computed: {
    cronPreviewExpression() {
      return buildCronExpression(this.cronForm)
    },
    cronPreviewDesc() {
      return describeCronForm(this.cronForm)
    },
    pagedScheduledTaskList() {
      const pageSize = this.taskManagePagination.pageSize
      const pageNum = this.taskManagePagination.pageNum
      const start = (pageNum - 1) * pageSize
      return this.scheduledTaskList.slice(start, start + pageSize)
    }
  },
  created() {
    this.loadScheduledTasks()
    this.getList()
  },
  methods: {
    async loadScheduledTasks() {
      try {
        const response = await request({
          url: '/system/scheduledTask/list',
          method: 'get'
        })
        if (response.code === 200 || response.code === 0) {
          const list = response.data || []
          this.scheduledTaskList = list.map(item => ({
            ...item,
            _updating: false
          }))
          this.taskOptions = list.map(item => ({
            label: item.taskName,
            value: `${item.taskBean}|${item.methodName || 'executeTask'}`
          }))
        }
      } catch (error) {
        console.error('獲取定時任務配置失敗', error)
      }
    },

    openTaskManageDialog() {
      this.taskManagePagination.pageNum = 1
      this.taskManageVisible = true
    },

    formatLastStatus(status) {
      if (status === '0') return '成功'
      if (status === '2') return '部分失敗'
      if (status === '1') return '失敗'
      return '-'
    },

    describeCron(cronExpression) {
      return describeCronExpression(cronExpression)
    },

    openCronDialog(row) {
      this.cronEditRow = row
      this.cronForm = parseCronExpression(row.cronExpression)
      this.cronDialogVisible = true
    },

    resetCronDialog() {
      this.cronEditRow = null
      this.cronForm = createDefaultCronForm()
      this.cronSaving = false
    },

    async saveCronFromDialog() {
      const validationMessage = validateCronForm(this.cronForm)
      if (validationMessage) {
        ElNotification({
          title: '保存失敗',
          message: validationMessage,
          type: 'warning',
          duration: 3000
        })
        return
      }

      const cronExpression = buildCronExpression(this.cronForm)
      const row = this.cronEditRow
      if (!row) {
        return
      }
      if (cronExpression === row.cronExpression) {
        this.cronDialogVisible = false
        return
      }

      this.cronSaving = true
      try {
        const response = await request({
          url: '/system/scheduledTask/cron',
          method: 'put',
          data: {
            taskKey: row.taskKey,
            cronExpression
          }
        })
        if (response.code === 200 || response.code === 0) {
          row.cronExpression = cronExpression
          this.cronDialogVisible = false
          ElNotification({
            title: '保存成功',
            message: `「${row.taskName}」已更新為 ${describeCronForm(this.cronForm)}`,
            type: 'success',
            duration: 3000
          })
        } else {
          ElNotification({
            title: '保存失敗',
            message: response.msg || 'Cron 更新失敗',
            type: 'error',
            duration: 4000
          })
        }
      } catch (error) {
        console.error('更新 Cron 失敗', error)
        ElNotification({
          title: '保存失敗',
          message: error?.response?.data?.msg || 'Cron 更新發生錯誤，請稍後再試',
          type: 'error',
          duration: 4000
        })
      } finally {
        this.cronSaving = false
      }
    },

    async handleTaskEnabledChange(row) {
      const previous = row.enabled === '1' ? '0' : '1'
      row._updating = true
      try {
        const response = await request({
          url: '/system/scheduledTask/status',
          method: 'put',
          data: {
            taskKey: row.taskKey,
            enabled: row.enabled
          }
        })
        if (response.code === 200 || response.code === 0) {
          ElNotification({
            title: '更新成功',
            message: `「${row.taskName}」已${row.enabled === '1' ? '啟用' : '停用'}自動執行`,
            type: 'success',
            duration: 3000
          })
        } else {
          row.enabled = previous
          ElNotification({
            title: '更新失敗',
            message: response.msg || '狀態更新失敗',
            type: 'error',
            duration: 4000
          })
        }
      } catch (error) {
        row.enabled = previous
        console.error('更新定時任務狀態失敗', error)
        ElNotification({
          title: '更新失敗',
          message: '狀態更新發生錯誤，請稍後再試',
          type: 'error',
          duration: 4000
        })
      } finally {
        row._updating = false
      }
    },

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

    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },

    resetQuery() {
      this.queryParams.taskName = undefined
      this.queryParams.status = undefined
      this.handleQuery()
    },

    handleSizeChange(val) {
      this.queryParams.pageSize = val
      this.getList()
    },

    handleCurrentChange(val) {
      this.queryParams.pageNum = val
      this.getList()
    },

    async refreshListAfterExecute(previousTotal = 0, maxAttempts = 5, intervalMs = 1500) {
      for (let attempt = 0; attempt < maxAttempts; attempt++) {
        await this.getList()
        if (this.total > previousTotal || attempt === maxAttempts - 1) {
          break
        }
        await new Promise(resolve => setTimeout(resolve, intervalMs))
      }
    },

    executeSelectedTask() {
      if (!this.selectedTask) return

      const selectedOption = this.taskOptions.find(item => item.value === this.selectedTask)
      const [beanName, methodName] = this.selectedTask.split('|')

      ElMessageBox.confirm(
        `確定要手動執行「${selectedOption.label}」嗎？`,
        '提示',
        {
          confirmButtonText: '確定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(async () => {
        const previousTotal = this.total
        this.isExecuting = true
        try {
          const response = await request({
            url: '/system/taskLog/execute',
            method: 'post',
            data: { beanName, methodName }
          })

          if (response.code === 200 || response.code === 0) {
            ElNotification({
              title: '觸發成功',
              message: response.msg || '任務已手動觸發執行',
              type: 'success',
              duration: 3000
            })
            setTimeout(async () => {
              await this.refreshListAfterExecute(previousTotal)
              if (this.taskManageVisible) {
                this.loadScheduledTasks()
              }
            }, 800)
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
          this.isExecuting = false
        }
      }).catch(() => {})
    },

    async handleStatusChange(row) {
      try {
        const response = await request({
          url: '/system/taskLog',
          method: 'put',
          data: {
            logId: row.logId,
            isProcessed: row.isProcessed
          }
        })
        if (response.code === 200 || response.code === 0) {
          ElNotification({
            title: '修改成功',
            message: row.isProcessed === '1' ? '已成功標記為已處理' : '已成功標記為未處理',
            type: 'success',
            duration: 3000
          })
        } else {
          row.isProcessed = row.isProcessed === '0' ? '1' : '0'
          ElNotification({
            title: '修改失敗',
            message: response.msg || '狀態修改失敗',
            type: 'error',
            duration: 4000
          })
        }
      } catch (error) {
        row.isProcessed = row.isProcessed === '0' ? '1' : '0'
        console.error('修改狀態失敗', error)
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
  gap: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 2px;
  padding: 8px 14px;
  background: linear-gradient(180deg, #fafbfc 0%, #f4f6f8 100%);
  border: none;
  border-radius: 10px;
  box-shadow: 0 1px 6px rgba(15, 23, 42, 0.05);
}

.toolbar-group {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 2px 10px;
}

.toolbar-divider {
  width: 1px;
  height: 22px;
  margin: 0 8px;
  background: linear-gradient(180deg, transparent, #e0e3e9 25%, #e0e3e9 75%, transparent);
  flex-shrink: 0;
}

.toolbar-label {
  font-size: 13px;
  color: #a8abb2;
  font-weight: 400;
  white-space: nowrap;
}

.btn-icon {
  margin-right: 4px;
}

.header-actions :deep(.el-button) {
  height: 34px;
  padding: 0 14px;
  border-radius: 8px;
  font-weight: 400;
  box-shadow: none;
  transition: color 0.2s ease, border-color 0.2s ease, background-color 0.2s ease;
}

.header-actions :deep(.el-button.is-plain) {
  color: #606266;
  border-color: #e4e7ed;
  background-color: #fff;
}

.header-actions :deep(.el-button.is-plain:hover) {
  color: #409eff;
  border-color: #c6e2ff;
  background-color: #ecf5ff;
}

.header-actions :deep(.el-select) {
  width: 200px;
}

.header-actions :deep(.el-select .el-input__wrapper) {
  height: 34px;
  border-radius: 8px;
  box-shadow: none !important;
  background-color: #fff;
  transition: border-color 0.2s ease;
}

.header-actions :deep(.el-select .el-input__wrapper:hover) {
  border-color: #c0c4cc;
}

.header-actions :deep(.el-select .el-input.is-focus .el-input__wrapper) {
  border-color: #a0cfff;
}

.header-actions :deep(.el-select .el-input__inner) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  flex-shrink: 0;
}

.title .el-icon {
  font-size: 20px;
  color: #409eff;
}

.task-manage-dialog :deep(.el-dialog) {
  display: flex;
  flex-direction: column;
}

.task-manage-dialog :deep(.el-dialog__header) {
  padding: 18px 22px;
}

.task-manage-dialog :deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 600;
}

.task-manage-dialog :deep(.el-dialog__body) {
  padding: 14px 22px 22px;
  max-height: 72vh;
  overflow-y: auto;
}

.task-manage-table :deep(.el-table__cell) {
  font-size: 15px;
}

.task-manage-table :deep(.el-table__header .el-table__cell) {
  font-size: 15px;
}

.task-manage-table .action-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
  flex-wrap: wrap;
}

.task-manage-table .action-buttons .el-button {
  border-radius: 6px;
  font-weight: 500;
  font-size: 13px;
  padding: 6px 12px;
  margin: 0;
  box-shadow: none !important;
}

.task-manage-table :deep(.el-table-fixed-column--right),
.task-manage-table :deep(.el-table-fixed-column--left) {
  box-shadow: none !important;
}

.task-manage-table :deep(.el-button) {
  box-shadow: none !important;
}

.task-manage-table :deep(.el-button:hover),
.task-manage-table :deep(.el-button:focus) {
  box-shadow: none !important;
  transform: none !important;
}

.cron-text {
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 12px;
  color: #909399;
}

.cron-display {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.cron-desc {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.cron-edit-form .cron-task-name {
  color: #303133;
  font-weight: 600;
}

.cron-edit-form .form-unit {
  margin-left: 8px;
  color: #909399;
}

.cron-edit-form .weekday-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
}

.cron-edit-form .hour-range {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.cron-edit-form .range-separator {
  color: #606266;
}

.cron-preview-desc {
  color: #303133;
  font-weight: 500;
}

.cron-preview-code {
  display: inline-block;
  padding: 6px 10px;
  border-radius: 6px;
  background: #f5f7fa;
  color: #303133;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 13px;
}

.filter-section {
  margin-bottom: 20px;
  background-color: #f8f9fa;
  padding: 16px;
  border-radius: 6px;
}

.table-wrapper {
  width: 100%;
  overflow-x: auto;
}

.table-wrapper :deep(.el-table) {
  min-width: 1240px;
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

:deep(.el-button) {
  box-shadow: none !important;
}

:deep(.el-button:hover),
:deep(.el-button:focus) {
  box-shadow: none !important;
  transform: none !important;
}
</style>

<style>
.task-select-popper.el-popper {
  border: none;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.12);
  overflow: hidden;
  margin-top: 0 !important;
}

.task-select-popper .el-select-dropdown__list {
  padding: 4px 0;
}

.task-select-popper .el-select-dropdown__item {
  padding: 8px 12px;
  font-size: 14px;
  line-height: 1.4;
  transition: background-color 0.15s ease;
}

.task-select-popper .el-select-dropdown__item.hover,
.task-select-popper .el-select-dropdown__item:hover {
  background-color: #f0f7ff;
}
</style>
