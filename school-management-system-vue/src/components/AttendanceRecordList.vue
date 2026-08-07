<template>
  <div class="attendance-record-list">
    <el-card class="box-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">
            <el-icon><Clock /></el-icon>
            考勤機記錄查詢
          </span>
          <div class="header-actions">
            <el-button
              type="success"
              :icon="Download"
              :loading="exporting"
              @click="handleExport"
            >
              導出 Excel
            </el-button>
          </div>
        </div>
      </template>

      <el-form :model="searchForm" :inline="true" class="search-form attendance-search-form">
        <el-form-item label="學生姓名">
          <el-input
            v-model="searchForm.idNameQuery"
            placeholder="請輸入姓名"
            clearable
            class="search-input-name"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="班級">
          <el-input
            v-model="searchForm.classSectionQuery"
            placeholder="例如 F1A"
            clearable
            class="search-input-class"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="班號">
          <el-input
            v-model="searchForm.classNumQuery"
            placeholder="請輸入班號"
            clearable
            class="search-input-num"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="學生 ID">
          <el-input
            v-model="searchForm.employeeIdQuery"
            placeholder="請輸入學生 ID"
            clearable
            class="search-input-id"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="拍卡日期">
          <el-date-picker
            v-model="searchForm.accessDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="開始日期"
            end-placeholder="結束日期"
            value-format="YYYY-MM-DD"
            class="search-input-date"
            clearable
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜尋</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table
        v-loading="loading"
        :data="tableData"
        style="width: 100%"
        :row-style="{ height: '56px' }"
        empty-text="暫無考勤記錄"
      >
        <el-table-column prop="accessDatetime" label="拍卡時間" min-width="170" align="center" />
        <el-table-column prop="classSection" label="班級" min-width="90" align="center" show-overflow-tooltip />
        <el-table-column prop="classNum" label="班號" min-width="80" align="center" />
        <el-table-column prop="idName" label="姓名" min-width="110" align="center" show-overflow-tooltip />
        <el-table-column prop="employeeId" label="學生 ID" min-width="150" align="center" show-overflow-tooltip />
        <el-table-column prop="cardNumber" label="卡號" min-width="120" align="center" show-overflow-tooltip />
        <el-table-column prop="direction" label="方向" width="90" align="center">
          <template #default="scope">
            {{ formatDirection(scope.row.direction) }}
          </template>
        </el-table-column>
        <el-table-column prop="deviceName" label="裝置" min-width="100" align="center" show-overflow-tooltip />
        <el-table-column prop="resourceName" label="資源" min-width="100" align="center" show-overflow-tooltip />
        <el-table-column prop="personName" label="拍卡人員" min-width="120" align="center" show-overflow-tooltip />
        <el-table-column prop="accessResult" label="結果" width="80" align="center">
          <template #default="scope">
            {{ formatAccessResult(scope.row.accessResult) }}
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container" v-if="total > 0">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>
  </div>
</template>

<script>
import { Clock, Search, Refresh, Download } from '@element-plus/icons-vue'
import { ElMessageBox, ElNotification } from 'element-plus'
import request from '@/utils/request'

export default {
  name: 'AttendanceRecordList',
  components: { Clock },
  data() {
    return {
      Search,
      Refresh,
      Download,
      loading: false,
      exporting: false,
      tableData: [],
      total: 0,
      pagination: {
        currentPage: 1,
        pageSize: 10
      },
      searchForm: {
        idNameQuery: '',
        classSectionQuery: '',
        classNumQuery: '',
        employeeIdQuery: '',
        accessDateRange: null
      }
    }
  },
  mounted() {
    this.loadList()
  },
  methods: {
    formatDirection(direction) {
      if (direction === '1') return '離開'
      if (direction === '0') return '進入'
      return direction || '-'
    },
    formatAccessResult(result) {
      if (result === '1') return '成功'
      if (result === '0') return '失敗'
      return result || '-'
    },
    buildFilterParams() {
      const params = {
        idNameQuery: this.searchForm.idNameQuery,
        classSectionQuery: this.searchForm.classSectionQuery,
        classNumQuery: this.searchForm.classNumQuery,
        employeeIdQuery: this.searchForm.employeeIdQuery
      }
      if (this.searchForm.accessDateRange && this.searchForm.accessDateRange.length === 2) {
        params.accessDateBegin = this.searchForm.accessDateRange[0]
        params.accessDateEnd = this.searchForm.accessDateRange[1]
      }
      return params
    },
    buildQueryParams() {
      return {
        pageNum: this.pagination.currentPage,
        pageSize: this.pagination.pageSize,
        ...this.buildFilterParams()
      }
    },
    async loadList() {
      this.loading = true
      try {
        const response = await request({
          url: '/system/student/attendance/list',
          method: 'get',
          params: this.buildQueryParams()
        })
        if (response.code === 200 || response.code === 0) {
          this.tableData = response.rows || []
          this.total = response.total || 0
        }
      } catch (error) {
        console.error(error)
        ElNotification({
          title: '加載失敗',
          message: '考勤記錄加載失敗，請檢查是否具有管理員權限',
          type: 'error',
          duration: 4000
        })
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.pagination.currentPage = 1
      this.loadList()
    },
    resetSearch() {
      this.searchForm = {
        idNameQuery: '',
        classSectionQuery: '',
        classNumQuery: '',
        employeeIdQuery: '',
        accessDateRange: null
      }
      this.handleSearch()
    },
    async handleExport() {
      try {
        await ElMessageBox.confirm(
          this.total > 0
            ? `將按當前篩選條件導出共 ${this.total} 條考勤記錄，是否繼續？`
            : '將按當前篩選條件導出考勤記錄，是否繼續？',
          '導出確認',
          {
            confirmButtonText: '確定導出',
            cancelButtonText: '取消',
            type: 'info'
          }
        )
      } catch {
        return
      }

      this.exporting = true
      try {
        const response = await request({
          url: '/system/student/attendance/export',
          method: 'get',
          params: this.buildFilterParams(),
          responseType: 'blob',
          timeout: 120000
        })
        const blob = new Blob([response], {
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = '考勤機記錄.xlsx'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
        ElNotification({
          title: '導出成功',
          message: '考勤機記錄已導出',
          type: 'success',
          duration: 3000
        })
      } catch (error) {
        console.error(error)
        ElNotification({
          title: '導出失敗',
          message: '考勤記錄導出失敗，請稍後再試',
          type: 'error',
          duration: 4000
        })
      } finally {
        this.exporting = false
      }
    }
  }
}
</script>

<style scoped>
.attendance-record-list {
  height: 100%;
}

.box-card {
  border-radius: 12px;
  border: none;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.search-form {
  margin-bottom: 16px;
}

.attendance-search-form :deep(.el-form-item__label) {
  padding-right: 8px;
}

.attendance-search-form :deep(.search-input-name) {
  width: 140px;
}

.attendance-search-form :deep(.search-input-class) {
  width: 100px;
}

.attendance-search-form :deep(.search-input-num) {
  width: 110px;
}

.attendance-search-form :deep(.search-input-id) {
  width: 180px;
}

.attendance-search-form :deep(.search-input-date) {
  width: 240px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
