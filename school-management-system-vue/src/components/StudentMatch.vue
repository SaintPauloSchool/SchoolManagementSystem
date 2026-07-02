<template>
  <div class="student-match-container">
    <el-card class="box-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">
            <el-icon><User /></el-icon>
            學生信息管理
          </span>
        </div>
      </template>

      <!-- 搜索欄 -->
      <el-form :model="searchForm" ref="queryForm" :inline="true" class="search-form match-search-form">
        <el-form-item label="學生姓名">
          <el-input 
            v-model="searchForm.idNameQuery" 
            placeholder="請輸入姓名"
            clearable 
            class="search-input-name"
            @keyup.enter="handleSearch" 
          />
        </el-form-item>
        <el-form-item label="學生班級">
          <el-input 
            v-model="searchForm.classSectionQuery" 
            placeholder="例如 K1E" 
            clearable 
            class="search-input-class"
            @keyup.enter="handleSearch" 
          />
        </el-form-item>
        <el-form-item label="匹配狀態">
          <el-select v-model="searchForm.matchStatus" placeholder="請選擇" class="search-input-status" clearable>
            <el-option label="未匹配" :value="0" />
            <el-option label="自動匹配成功" :value="1" />
            <el-option label="手動匹配成功" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item class="search-actions">
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
          <el-button
            type="warning"
            :icon="Refresh"
            @click="handleSyncData"
            :loading="syncingData"
          >
            同步數據
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 主列表 -->
      <el-table 
        v-loading="loading" 
        :data="matchList" 
        :row-key="getRowKey"
        style="width: 100%" 
        :row-style="{ height: '60px' }"
        empty-text="暫無學籍數據"
      >
        <el-table-column label="照片" width="72" align="center">
          <template #default="scope">
            <StudentPhoto :profile-number="resolveProfileNumber(scope.row)" :size="44" />
          </template>
        </el-table-column>
        <el-table-column prop="studentProfileNumber" label="學生個人編號" min-width="180" align="center" class-name="profile-number-cell" />
        <el-table-column prop="inSchool" label="在校" width="80" align="center">
          <template #default="scope">
            {{ formatInSchool(scope.row.inSchool) }}
          </template>
        </el-table-column>
        <el-table-column prop="schoolYear" label="學年" min-width="90" align="center" show-overflow-tooltip />
        <el-table-column prop="adid" label="帳號" min-width="100" align="center" show-overflow-tooltip />
        <el-table-column prop="classSection" label="班級" min-width="90" align="center" />
        <el-table-column prop="classNum" label="班號" min-width="80" align="center" />
        <el-table-column prop="idName" label="姓名" min-width="110" align="center" />
        <el-table-column prop="dsejStudentId" label="學生證編號" min-width="130" align="center" show-overflow-tooltip />
        <el-table-column prop="matchStatus" label="匹配狀態" min-width="120" align="center">
          <template #default="scope">
            <el-tag :type="getMatchStatusTag(scope.row.matchStatus)">
              {{ getMatchStatusText(scope.row.matchStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="userId" label="家長 user_id" min-width="140" align="center" show-overflow-tooltip>
          <template #default="scope">
            <span v-if="scope.row.userId">{{ scope.row.userId }}</span>
            <span v-else class="text-placeholder">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" align="center" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <el-button 
                size="small" 
                type="info" 
                :icon="Document"
                @click="handleViewDetail(scope.row)"
              >
                詳情
              </el-button>
              <el-button
                v-if="!scope.row.userId"
                size="small"
                type="primary"
                :icon="Edit"
                @click="handleManualMatch(scope.row)"
              >
                手動匹配
              </el-button>
              <el-button
                v-if="isMatched(scope.row)"
                size="small"
                type="warning"
                :icon="Edit"
                @click="handleUpdateMatch(scope.row)"
              >
                更改信息
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分頁 -->
      <div class="pagination-container" v-if="total > 0">
        <el-pagination 
          v-model:current-page="pagination.currentPage" 
          v-model:page-size="pagination.pageSize" 
          :page-sizes="[10, 20, 50]" 
          :total="total" 
          layout="total, sizes, prev, pager, next" 
          background 
          @size-change="loadMatchList" 
          @current-change="loadMatchList" 
        />
      </div>
    </el-card>

    <!-- 手動綁定：企微候選家長選擇器 -->
    <el-dialog
      draggable
      align-center
      :title="candidatesMode === 'update' ? '更改信息 - 重新選擇家長' : '手動綁定 - 選擇企業微信學生'"
      v-model="candidatesVisible"
      width="1200px"
      class="candidates-dialog"
      append-to-body
      :close-on-click-modal="false"
      @opened="onCandidatesDialogOpened"
    >
      <div class="candidates-dialog-body">
        <div class="candidate-header-info" v-if="currentMatchingRow">
          學生：<strong>{{ currentMatchingRow.idName || '-' }}</strong>
          （班級：{{ currentMatchingRow.classSection || '-' }}）
          <span v-if="candidatesMode === 'bind'" class="candidate-header-hint">可多選家長後一次批量綁定</span>
          <span v-else class="candidate-header-hint">點擊列表中的家長行進行選擇</span>
        </div>

        <el-form :model="candidatesQuery" :inline="true" class="candidates-search-form search-form">
          <el-form-item label="企微學生姓名">
            <el-input
              v-model="candidatesQuery.queryName"
              placeholder="支持簡繁體姓名查詢"
              clearable
              style="width: 200px;"
              @keyup.enter="handleCandidatesSearch"
            />
          </el-form-item>
          <el-form-item label="家長手機號">
            <el-input
              v-model="candidatesQuery.queryMobile"
              placeholder="家長聯絡手機"
              clearable
              style="width: 180px;"
              @keyup.enter="handleCandidatesSearch"
            />
          </el-form-item>
          <el-form-item label="企微班級">
            <el-input
              v-model="candidatesQuery.queryClass"
              placeholder="例如 F6F"
              clearable
              style="width: 120px;"
              @keyup.enter="handleCandidatesSearch"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleCandidatesSearch">搜索</el-button>
            <el-button :icon="Refresh" @click="resetCandidatesSearch">重置</el-button>
          </el-form-item>
        </el-form>

        <div class="candidates-table-panel" v-loading="candidatesLoading">
          <el-table
            ref="candidatesTable"
            :data="candidatesList"
            row-key="parentUserId"
            border
            :stripe="candidatesMode === 'bind'"
            :height="candidatesTableHeight"
            style="width: 100%"
            :row-style="{ height: '48px' }"
            :row-class-name="getCandidateRowClassName"
            :class="{ 'candidates-table--update': candidatesMode === 'update' }"
            empty-text="未找到可選家長，可嘗試清空班級或姓名條件後重新搜索"
            @selection-change="handleCandidatesSelectionChange"
            @row-click="handleCandidateRowClick"
          >
            <el-table-column v-if="candidatesMode === 'bind'" type="selection" width="55" align="center" />
            <el-table-column prop="studentName" label="企微學生姓名" min-width="150" align="center" show-overflow-tooltip>
              <template #default="scope">
                <span class="candidate-name-cell" :class="{ 'is-selected': isCandidateSelected(scope.row) }">
                  <el-icon v-if="isCandidateSelected(scope.row)" class="candidate-check-icon"><CircleCheck /></el-icon>
                  {{ scope.row.studentName }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="classCodeWecom" label="企微班級代碼" min-width="120" align="center">
              <template #default="scope">
                <span v-if="scope.row.classCodeWecom">{{ scope.row.classCodeWecom }}</span>
                <span v-else class="text-placeholder">未對照班級</span>
              </template>
            </el-table-column>
            <el-table-column prop="mobile" label="家長手機號" min-width="160" align="center" show-overflow-tooltip />
            <el-table-column prop="relationDesc" label="家長關係" min-width="100" align="center" show-overflow-tooltip />
          </el-table>

          <div class="candidates-pagination">
            <el-pagination
              v-model:current-page="candidatesPagination.pageNum"
              v-model:page-size="candidatesPagination.pageSize"
              :page-sizes="[10, 20, 50]"
              :total="candidatesTotal"
              layout="total, sizes, prev, pager, next"
              background
              @size-change="loadCandidates"
              @current-change="loadCandidates"
            />
          </div>
        </div>
      </div>
      <template #footer>
        <div class="candidates-dialog-footer">
          <el-button
            v-if="candidatesMode === 'bind'"
            type="primary"
            :disabled="candidatesSelection.length === 0"
            :loading="submittingCandidates"
            @click="submitBatchBind"
          >
            批量綁定（已選 {{ candidatesSelection.length }}）
          </el-button>
          <el-button
            v-else
            type="primary"
            :disabled="!selectedUpdateCandidate"
            :loading="submittingCandidates"
            @click="submitUpdateMatch"
          >
            確認更改
          </el-button>
          <el-button @click="candidatesVisible = false">完成</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 學生對照數據詳情對話框 -->
    <el-dialog title="學生對照數據詳情" v-model="detailVisible" width="900px" append-to-body destroy-on-close>
      <div class="detail-photo-wrap" v-if="resolveProfileNumber(detailForm)">
        <StudentPhoto :profile-number="resolveProfileNumber(detailForm)" :size="120" />
        <div class="detail-photo-name">{{ detailForm.idName || '-' }}</div>
      </div>
      <el-descriptions :column="2" border size="default">
        <el-descriptions-item label="學生個人編號">{{ detailForm.studentProfileNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="在校">{{ formatInSchool(detailForm.inSchool) }}</el-descriptions-item>
        <el-descriptions-item label="學年">{{ detailForm.schoolYear || '-' }}</el-descriptions-item>
        <el-descriptions-item label="帳號">{{ detailForm.adid || '-' }}</el-descriptions-item>
        <el-descriptions-item label="班級">{{ detailForm.classSection || '-' }}</el-descriptions-item>
        <el-descriptions-item label="班號">{{ detailForm.classNum || '-' }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ detailForm.idName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="學生證編號">{{ detailForm.dsejStudentId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="家長 user_id">{{ detailForm.userId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="匹配狀態">
          <el-tag :type="getMatchStatusTag(detailForm.matchStatus)">
            {{ getMatchStatusText(detailForm.matchStatus) }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailVisible = false">關 閉</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { User, Search, Refresh, InfoFilled, Edit, Document, CircleCheck } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { ElMessageBox, ElNotification } from 'element-plus'
import StudentPhoto from '@/components/StudentPhoto.vue'

export default {
  name: 'StudentMatch',
  components: {
    User, Search, Refresh, InfoFilled, Edit, Document, CircleCheck, StudentPhoto
  },
  data() {
    return {
      Search, Refresh, Document,
      loading: false,
      syncingData: false,
      matchList: [],
      total: 0,
      pagination: {
        currentPage: 1,
        pageSize: 10
      },
      searchForm: {
        idNameQuery: '',
        classSectionQuery: '',
        matchStatus: null
      },

      // 企微候選家長選擇
      candidatesVisible: false,
      candidatesMode: 'bind',
      candidatesLoading: false,
      candidatesList: [],
      currentMatchingRow: null,
      candidatesQuery: {
        queryName: '',
        queryMobile: '',
        queryClass: ''
      },
      candidatesTotal: 0,
      candidatesPagination: {
        pageNum: 1,
        pageSize: 10
      },
      candidatesSelection: [],
      selectedParentUserId: null,
      submittingCandidates: false,
      candidatesTableHeight: 200,
      // 詳情彈窗
      detailVisible: false,
      detailForm: {}
    }
  },
  computed: {
    selectedUpdateCandidate() {
      if (!this.selectedParentUserId) {
        return null
      }
      return this.candidatesList.find(item => item.parentUserId === this.selectedParentUserId) || this.candidatesSelection[0] || null
    }
  },
  mounted() {
    this.loadMatchList()
  },
  methods: {
    resolveProfileNumber(row) {
      if (!row) {
        return ''
      }
      if (row.studentProfileNumber !== null && row.studentProfileNumber !== undefined && row.studentProfileNumber !== '') {
        return String(row.studentProfileNumber)
      }
      return ''
    },
    getRowKey(row) {
      if (!row) {
        return ''
      }
      return `${row.studentId || ''}_${row.userId || ''}_${row.id || ''}_${row.studentProfileNumber || ''}`
    },

    // 查詢主列表
    async loadMatchList() {
      this.loading = true
      try {
        const response = await request({
          url: '/system/student/match/list',
          method: 'get',
          params: {
            pageNum: this.pagination.currentPage,
            pageSize: this.pagination.pageSize,
            idNameQuery: this.searchForm.idNameQuery,
            classSectionQuery: this.searchForm.classSectionQuery,
            matchStatus: this.searchForm.matchStatus
          }
        })
        if (response.code === 200 || response.code === 0) {
          this.matchList = response.rows || []
          this.total = response.total || 0
        }
      } catch (error) {
        console.error('加載匹配數據失敗:', error)
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.pagination.currentPage = 1
      this.loadMatchList()
    },
    resetSearch() {
      this.searchForm = {
        idNameQuery: '',
        classSectionQuery: '',
        matchStatus: null
      }
      this.handleSearch()
    },

    // 狀態展示標籤樣式（matchStatus 為 null 表示未匹配，不入庫）
    getMatchStatusTag(status) {
      const code = this.normalizeMatchStatus(status)
      const map = { 0: 'info', 1: 'success', 2: 'warning' }
      return map[code] || 'info'
    },
    getMatchStatusText(status) {
      const code = this.normalizeMatchStatus(status)
      const map = { 0: '未匹配', 1: '自動匹配成功', 2: '手動匹配成功' }
      return map[code] || '未匹配'
    },
    normalizeMatchStatus(status) {
      if (status === null || status === undefined || status === '') {
        return 0
      }
      const num = Number(status)
      return Number.isNaN(num) ? 0 : num
    },
    isMatched(row) {
      const code = this.normalizeMatchStatus(row?.matchStatus)
      return code === 1 || code === 2
    },

    // 同步對照數據 (本地匹配)
    async handleSyncData() {
      this.syncingData = true
      try {
        const res = await request({
          url: '/system/student/match/syncData',
          method: 'post'
        })
        if (res.code === 200 || res.code === 0) {
          ElNotification({
            title: '同步對照完成',
            message: res.msg || '數據比對已完成',
            type: 'success',
            duration: 5000
          })
          this.loadMatchList()
        } else {
          ElNotification({
            title: '同步失敗',
            message: res.msg || '執行同步比對失敗',
            type: 'error',
            duration: 5000
          })
        }
      } catch (err) {
        console.error(err)
        ElNotification({
          title: '同步出錯',
          message: '請求過程發生錯誤，請稍後再試',
          type: 'error',
          duration: 4000
        })
      } finally {
        this.syncingData = false
      }
    },

    // 手動匹配
    handleManualMatch(row) {
      this.candidatesMode = 'bind'
      this.currentMatchingRow = row
      this.candidatesQuery = {
        queryName: '',
        queryMobile: '',
        queryClass: ''
      }
      this.candidatesPagination.pageNum = 1
      this.candidatesList = []
      this.candidatesTotal = 0
      this.candidatesSelection = []
      this.candidatesVisible = true
    },
    handleUpdateMatch(row) {
      if (!row?.id) {
        ElNotification({
          title: '無法更改',
          message: '缺少匹配記錄，無法更正家長信息',
          type: 'warning',
          duration: 3000
        })
        return
      }
      this.candidatesMode = 'update'
      this.currentMatchingRow = row
      this.candidatesQuery = {
        queryName: '',
        queryMobile: '',
        queryClass: ''
      }
      this.candidatesPagination.pageNum = 1
      this.candidatesList = []
      this.candidatesTotal = 0
      this.candidatesSelection = []
      this.candidatesVisible = true
    },
    onCandidatesDialogOpened() {
      this.updateCandidatesTableHeight()
      this.loadCandidates()
    },
    updateCandidatesTableHeight() {
      const rowHeight = 48
      const headerHeight = 44
      const pageSize = this.candidatesPagination.pageSize || 10
      const displayRows = Math.max(this.candidatesList.length, pageSize)
      const idealHeight = headerHeight + displayRows * rowHeight
      // 彈窗整體約 84vh，扣除固定區後為表格可用高度，確保上下留白
      const dialogChrome = 320
      const maxHeight = Math.max(300, Math.floor(window.innerHeight * 0.84 - dialogChrome))
      this.candidatesTableHeight = Math.min(idealHeight, maxHeight)
    },
    handleCandidatesSelectionChange(selection) {
      if (this.candidatesMode !== 'bind') {
        return
      }
      this.candidatesSelection = selection || []
    },
    selectUpdateCandidate(row) {
      if (!row?.parentUserId) {
        this.selectedParentUserId = null
        this.candidatesSelection = []
        return
      }
      this.selectedParentUserId = row.parentUserId
      this.candidatesSelection = [row]
    },
    handleCandidateRowClick(row) {
      if (this.candidatesMode !== 'update' || !row?.parentUserId) {
        return
      }
      this.selectUpdateCandidate(row)
    },
    getCandidateRowClassName({ row }) {
      if (this.candidatesMode === 'update' && row?.parentUserId === this.selectedParentUserId) {
        return 'candidate-row-selected'
      }
      return ''
    },
    isCandidateSelected(row) {
      return this.candidatesMode === 'update' && row?.parentUserId === this.selectedParentUserId
    },
    clearCandidatesSelection() {
      this.candidatesSelection = []
      this.selectedParentUserId = null
      this.$refs.candidatesTable?.clearSelection()
    },
    handleCandidatesSearch() {
      this.candidatesPagination.pageNum = 1
      this.loadCandidates()
    },
    resetCandidatesSearch() {
      this.candidatesQuery = {
        queryName: '',
        queryMobile: '',
        queryClass: ''
      }
      this.candidatesPagination.pageNum = 1
      this.loadCandidates()
    },
    async loadCandidates() {
      if (!this.submittingCandidates) {
        this.clearCandidatesSelection()
      }
      this.candidatesLoading = true
      try {
        const res = await request({
          url: '/system/student/match/wecomCandidates',
          method: 'get',
          params: {
            pageNum: this.candidatesPagination.pageNum,
            pageSize: this.candidatesPagination.pageSize,
            queryName: this.candidatesQuery.queryName,
            queryMobile: this.candidatesQuery.queryMobile,
            queryClass: this.candidatesQuery.queryClass,
            studentId: this.currentMatchingRow?.studentId || ''
          }
        })
        if (res.code === 200 || res.code === 0) {
          this.candidatesList = res.rows || []
          this.candidatesTotal = res.total || 0
          this.updateCandidatesTableHeight()
        } else {
          ElNotification({
            title: '查詢失敗',
            message: res.msg || '獲取企微學生候選列表失敗',
            type: 'error',
            duration: 4000
          })
        }
      } catch (e) {
        console.error(e)
        ElNotification({
          title: '查詢出錯',
          message: e?.response?.data?.msg || e?.message || '獲取企微學生候選列表失敗，請稍後再試',
          type: 'error',
          duration: 4000
        })
      } finally {
        this.candidatesLoading = false
      }
    },
    async submitBatchBind() {
      if (!this.currentMatchingRow?.studentId || this.candidatesSelection.length === 0) {
        return
      }

      const userIds = this.candidatesSelection
        .map(row => row?.parentUserId)
        .filter(Boolean)

      this.submittingCandidates = true
      try {
        const res = await request({
          url: '/system/student/match/bindBatch',
          method: 'post',
          data: {
            studentId: this.currentMatchingRow.studentId,
            userIds
          }
        })

        if (res.code === 200 || res.code === 0) {
          ElNotification({
            title: '批量綁定完成',
            message: res.msg || `成功綁定 ${userIds.length} 位家長`,
            type: 'success',
            duration: 4000
          })
          this.candidatesVisible = false
          this.loadMatchList()
        } else {
          ElNotification({
            title: '綁定失敗',
            message: res.msg || '所選家長均未能綁定，請稍後再試',
            type: 'error',
            duration: 3000
          })
        }
      } catch (e) {
        console.error(e)
        ElNotification({
          title: '綁定出錯',
          message: e?.response?.data?.msg || e?.message || '批量綁定失敗，請稍後再試',
          type: 'error',
          duration: 4000
        })
      } finally {
        this.submittingCandidates = false
      }
    },
    async submitUpdateMatch() {
      const selected = this.candidatesSelection[0]
      if (!this.currentMatchingRow?.id || !selected?.parentUserId) {
        return
      }

      this.submittingCandidates = true
      try {
        const res = await request({
          url: '/system/student/match/update',
          method: 'put',
          data: {
            id: this.currentMatchingRow.id,
            userId: selected.parentUserId
          }
        })

        if (res.code === 200 || res.code === 0) {
          ElNotification({
            title: '更新成功',
            message: res.msg || '家長信息已更新',
            type: 'success',
            duration: 4000
          })
          this.candidatesVisible = false
          this.loadMatchList()
        } else {
          ElNotification({
            title: '更新失敗',
            message: res.msg || '更正家長信息失敗，請稍後再試',
            type: 'error',
            duration: 3000
          })
        }
      } catch (e) {
        console.error(e)
        ElNotification({
          title: '更新出錯',
          message: e?.response?.data?.msg || e?.message || '更正家長信息失敗，請稍後再試',
          type: 'error',
          duration: 4000
        })
      } finally {
        this.submittingCandidates = false
      }
    },

    formatInSchool(value) {
      if (value === 1 || value === true || value === '1') return '在校'
      if (value === 0 || value === false || value === '0') return '離校'
      return '-'
    },
    handleViewDetail(row) {
      this.detailForm = { ...row }
      this.detailVisible = true
    }
  }
}
</script>

<style scoped>
.student-match-container {
  width: 100%;
}
.box-card {
  border-radius: 8px;
  box-shadow: none !important;
}
:deep(.profile-number-cell .cell) {
  white-space: nowrap;
  overflow: visible;
  text-overflow: clip;
}
:deep(.el-button) {
  box-shadow: none !important;
}
:deep(.el-table .el-button:hover) {
  transform: none !important;
  box-shadow: none !important;
}
:deep(.el-button--danger.is-plain) {
  background: #fef2f2 !important;
  border: 1px solid #fca5a5 !important;
  color: #ef4444 !important;
}
:deep(.el-button--danger.is-plain:hover) {
  background: #fee2e2 !important;
  border: 1px solid #ef4444 !important;
  color: #dc2626 !important;
}
.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
}
:deep(.action-buttons .el-button) {
  border-radius: 6px !important;
  font-weight: 500 !important;
  font-size: 12px !important;
  padding: 6px 15px !important;
  border: none !important;
  box-shadow: none !important;
  transition: all 0.2s ease !important;
}
:deep(.action-buttons .el-button:hover) {
  transform: translateY(-1px) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15) !important;
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
.search-form {
  margin-bottom: 15px;
  padding: 15px;
  background-color: #f9fbfd;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}
.match-search-form {
  margin-bottom: 12px;
  padding: 12px 18px;
}
.match-search-form :deep(.el-form-item) {
  margin-bottom: 0;
  margin-right: 20px;
}
.match-search-form :deep(.el-form-item__label) {
  padding-right: 8px;
}
.match-search-form :deep(.search-input-name) {
  width: 160px;
}
.match-search-form :deep(.search-input-class) {
  width: 100px;
}
.match-search-form :deep(.search-input-status) {
  width: 148px;
}
.match-search-form :deep(.search-input-status .el-select__selected-item),
.match-search-form :deep(.search-input-status .el-select__selection-text) {
  max-width: none;
  overflow: visible;
  text-overflow: clip;
}
.match-search-form :deep(.search-actions) {
  margin-right: 0;
  margin-left: 6px;
}
.match-search-form :deep(.search-actions .el-button + .el-button) {
  margin-left: 10px;
}
.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding: 14px 24px;
}

.text-placeholder {
  color: #909399;
  font-style: italic;
}
.text-success {
  color: #67c23a;
  font-weight: 500;
}
.clickable-tag {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

/* 候選學生對話方塊 */
:deep(.candidates-dialog.el-dialog) {
  margin: 0 auto !important;
  max-height: 84vh;
  display: flex;
  flex-direction: column;
}

:deep(.candidates-dialog .el-dialog__header) {
  flex-shrink: 0;
  padding-bottom: 12px;
}

:deep(.candidates-dialog .el-dialog__body) {
  padding-top: 12px;
  padding-bottom: 12px;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

:deep(.candidates-dialog .el-dialog__footer) {
  flex-shrink: 0;
  padding-top: 16px;
  padding-bottom: 20px;
  border-top: 1px solid #ebeef5;
}

.candidates-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.candidates-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.candidate-header-info {
  margin-bottom: 12px;
  padding: 10px 14px;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
  color: #475569;
  line-height: 1.6;
}

.candidate-header-hint {
  margin-left: 8px;
  font-size: 13px;
  color: #64748b;
  font-weight: normal;
}

.candidates-search-form {
  margin-bottom: 0;
  flex-wrap: nowrap;
}

.candidates-search-form :deep(.el-form-item) {
  margin-bottom: 0;
  margin-right: 12px;
}

.candidates-search-form :deep(.el-form-item__content) {
  flex-wrap: nowrap;
}

.candidates-table-panel {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  background-color: #fff;
}

.candidates-table--update :deep(.el-table__body tr) {
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.candidates-table--update :deep(.el-table__body tr:hover > td.el-table__cell) {
  background-color: #f8fafc !important;
}

:deep(.candidate-row-selected > td.el-table__cell) {
  background-color: #f0f7ff !important;
}

:deep(.candidate-row-selected > td:first-child) {
  box-shadow: inset 3px 0 0 #409eff;
}

.candidate-name-cell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.candidate-name-cell.is-selected {
  color: #409eff;
  font-weight: 500;
}

.candidate-check-icon {
  font-size: 16px;
  color: #67c23a;
}

.candidates-table-panel :deep(.el-table) {
  border: none;
}

.candidates-table-panel :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.candidates-pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 10px 12px;
  margin-top: 4px;
  border-top: 1px solid #ebeef5;
  background-color: #fafafa;
}

/* 詳情彈窗 Descriptions 樣式微調 */
.detail-photo-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.detail-photo-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

:deep(.el-descriptions__label) {
  min-width: 120px;
  word-break: keep-all !important;
  white-space: nowrap !important;
}
</style>
