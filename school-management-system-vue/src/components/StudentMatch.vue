<template>
  <div class="student-match-container">
    <el-card class="box-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">
            <el-icon><User /></el-icon>
            學生信息管理
          </span>
          <div class="header-actions">
            <el-button type="info" :icon="Warning" @click="openUnmatchedDialog">
              查看未匹配數據
            </el-button>
            <el-button 
              type="warning" 
              :icon="Refresh" 
              @click="handleSyncData"
              :loading="syncingData"
            >
              同步數據
            </el-button>
            <el-button 
              type="primary" 
              :icon="Promotion" 
              :disabled="syncableSelection.length === 0"
              @click="handleBatchSync"
            >
              同步至企業微信 (已選可同步 {{ syncableSelection.length }} 筆)
            </el-button>
            <el-button 
              type="danger" 
              :icon="Delete" 
              :disabled="deletableSelection.length === 0"
              @click="handleBatchDelete"
            >
              批量刪除匹配記錄 (已選 {{ deletableSelection.length }} 筆)
            </el-button>
          </div>
        </div>
      </template>

      <!-- 搜索欄 -->
      <el-form :model="searchForm" ref="queryForm" :inline="true" class="search-form">
        <el-form-item label="學生姓名">
          <el-input 
            v-model="searchForm.idNameQuery" 
            placeholder="請輸入學生姓名或企微學生姓名"
            clearable 
            style="width: 220px;"
            @keyup.enter="handleSearch" 
          />
        </el-form-item>
        <el-form-item label="學生班級">
          <el-input 
            v-model="searchForm.classSectionQuery" 
            placeholder="例如 K1E" 
            clearable 
            style="width: 120px;"
            @keyup.enter="handleSearch" 
          />
        </el-form-item>
        <el-form-item label="匹配狀態">
          <el-select v-model="searchForm.matchStatus" placeholder="請選擇" style="width: 130px;" clearable>
            <el-option label="未匹配" value="0" />
            <el-option label="自動匹配成功" value="1" />
            <el-option label="手動匹配成功" value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 主列表 -->
      <el-table 
        v-loading="loading" 
        :data="matchList" 
        style="width: 100%" 
        :row-style="{ height: '60px' }"
        empty-text="暫無學籍數據"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
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
        <el-table-column label="操作" width="300" align="center" fixed="right">
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
                v-if="!scope.row.userId || scope.row.matchStatus === '0'"
                size="small" 
                type="primary" 
                :icon="Edit"
                @click="handleManualMatch(scope.row)"
              >
                手動匹配
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

    <!-- 彈窗一：未匹配學籍數據列表 -->
    <el-dialog draggable title="未匹配學籍數據" v-model="unmatchedVisible" width="1100px" top="5vh" append-to-body>
      <!-- 搜索欄 -->
      <el-form :model="unmatchedQuery" :inline="true" class="unmatched-search-form" style="margin-bottom: 15px; padding: 10px 14px; background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px;">
        <el-form-item label="學生姓名">
          <el-input 
            v-model="unmatchedQuery.idNameQuery" 
            placeholder="學生姓名/企微學生姓名"
            clearable 
            style="width: 180px;"
            @keyup.enter="handleUnmatchedSearch" 
          />
        </el-form-item>
        <el-form-item label="學生班級">
          <el-input 
            v-model="unmatchedQuery.classSectionQuery" 
            placeholder="例如 K1E" 
            clearable 
            style="width: 120px;"
            @keyup.enter="handleUnmatchedSearch" 
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleUnmatchedSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="resetUnmatchedSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="unmatchedList" v-loading="unmatchedLoading" max-height="520" empty-text="沒有未匹配的數據">
        <el-table-column prop="studentProfileNumber" label="學生個人編號" min-width="180" align="center" class-name="profile-number-cell" />
        <el-table-column prop="idName" label="姓名" width="120" align="center" />
        <el-table-column prop="classSection" label="班級" width="90" align="center" />
        <el-table-column prop="classNum" label="班號" width="80" align="center" />
        <el-table-column prop="dsejStudentId" label="學生證編號" width="130" align="center" />
        <el-table-column prop="adid" label="帳號" width="100" align="center" show-overflow-tooltip />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="scope">
            <el-button size="small" type="primary" @click="handleManualMatch(scope.row)">
              手動匹配
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分頁 -->
      <div class="pagination-container" v-if="unmatchedTotal > 0" style="margin-top: 15px; display: flex; justify-content: flex-end;">
        <el-pagination 
          v-model:current-page="unmatchedPagination.pageNum" 
          v-model:page-size="unmatchedPagination.pageSize" 
          :page-sizes="[10, 20, 50]" 
          :total="unmatchedTotal" 
          layout="total, sizes, prev, pager, next" 
          background 
          @size-change="loadUnmatchedList" 
          @current-change="loadUnmatchedList" 
        />
      </div>
    </el-dialog>

    <!-- 彈窗二：企微候選學生選擇器 -->
    <el-dialog
      title="手動綁定 - 選擇企業微信學生"
      v-model="candidatesVisible"
      width="1200px"
      top="5vh"
      append-to-body
      :close-on-click-modal="false"
      @opened="onCandidatesDialogOpened"
    >
      <div class="candidates-dialog-body">
        <div class="candidate-header-info" v-if="currentMatchingRow">
          待匹配學生：<strong>{{ currentMatchingRow.idName || '-' }}</strong>
          （班級：{{ currentMatchingRow.classSection || '-' }}）
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
            :data="candidatesList"
            border
            stripe
            height="460"
            style="width: 100%"
            :row-style="{ height: '48px' }"
            empty-text="未找到未匹配的企微學生，可嘗試清空班級或姓名條件後重新搜索"
          >
            <el-table-column prop="studentName" label="企微學生姓名" min-width="150" align="center" show-overflow-tooltip />
            <el-table-column prop="parentUserId" label="家長 user_id" min-width="160" align="center" show-overflow-tooltip />
            <el-table-column prop="classCodeWecom" label="企微班級代碼" min-width="120" align="center">
              <template #default="scope">
                <span v-if="scope.row.classCodeWecom">{{ scope.row.classCodeWecom }}</span>
                <span v-else class="text-placeholder">未對照班級</span>
              </template>
            </el-table-column>
            <el-table-column prop="mobile" label="家長手機號" min-width="160" align="center" show-overflow-tooltip />
            <el-table-column prop="relationDesc" label="家長關係" min-width="100" align="center" show-overflow-tooltip />
            <el-table-column label="操作" width="120" align="center">
              <template #default="scope">
                <el-button
                  size="small"
                  type="success"
                  :loading="bindingId === scope.row.parentUserId"
                  @click="submitBind(scope.row)"
                >
                  確認綁定
                </el-button>
              </template>
            </el-table-column>
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
import { User, Warning, Promotion, Search, Refresh, InfoFilled, Edit, Delete, Document } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { ElMessageBox, ElNotification } from 'element-plus'
import StudentPhoto from '@/components/StudentPhoto.vue'

export default {
  name: 'StudentMatch',
  components: {
    User, Warning, Promotion, Search, Refresh, InfoFilled, Delete, Document, StudentPhoto
  },
  data() {
    return {
      Warning, Promotion, Search, Refresh, Delete, Document,
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
        matchStatus: ''
      },
      multipleSelection: [],

      // 未匹配數據列表 (彈窗一)
      unmatchedVisible: false,
      unmatchedLoading: false,
      unmatchedList: [],
      unmatchedTotal: 0,
      unmatchedPagination: {
        pageNum: 1,
        pageSize: 10
      },
      unmatchedQuery: {
        idNameQuery: '',
        classSectionQuery: ''
      },

      // 企微候選人選擇 (彈窗二)
      candidatesVisible: false,
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
      bindingId: null,
      // 詳情彈窗
      detailVisible: false,
      detailForm: {}
    }
  },
  mounted() {
    this.loadMatchList()
  },
  computed: {
    deletableSelection() {
      return this.multipleSelection.filter(row => row.id)
    },
    syncableSelection() {
      return this.multipleSelection.filter(row => row.id && row.userId)
    }
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
        matchStatus: ''
      }
      this.handleSearch()
    },
    handleSelectionChange(val) {
      this.multipleSelection = val
    },
    canSelectRow(row) {
      return row.id && row.userId
    },
    
    // 狀態展示標籤樣式
    getMatchStatusTag(status) {
      const map = { '0': 'info', '1': 'success', '2': 'warning' }
      return map[status] || 'info'
    },
    getMatchStatusText(status) {
      const map = { '0': '未匹配', '1': '自動匹配成功', '2': '手動匹配成功' }
      return map[status] || '未知'
    },

    // 彈窗一：未匹配學籍數據
    async openUnmatchedDialog() {
      this.unmatchedVisible = true
      this.unmatchedPagination.pageNum = 1
      this.unmatchedQuery = {
        idNameQuery: '',
        classSectionQuery: ''
      }
      this.loadUnmatchedList()
    },
    async loadUnmatchedList() {
      this.unmatchedLoading = true
      try {
        const res = await request({
          url: '/system/student/match/unmatchedList',
          method: 'get',
          params: {
            pageNum: this.unmatchedPagination.pageNum,
            pageSize: this.unmatchedPagination.pageSize,
            idNameQuery: this.unmatchedQuery.idNameQuery,
            classSectionQuery: this.unmatchedQuery.classSectionQuery
          }
        })
        if (res.code === 200 || res.code === 0) {
          this.unmatchedList = res.rows || []
          this.unmatchedTotal = res.total || 0
        }
      } catch (e) {
        console.error(e)
      } finally {
        this.unmatchedLoading = false
      }
    },
    handleUnmatchedSearch() {
      this.unmatchedPagination.pageNum = 1
      this.loadUnmatchedList()
    },
    resetUnmatchedSearch() {
      this.unmatchedQuery = {
        idNameQuery: '',
        classSectionQuery: ''
      }
      this.handleUnmatchedSearch()
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

    // 手動匹配 (彈窗二)
    handleManualMatch(row) {
      this.currentMatchingRow = row
      this.candidatesQuery = {
        queryName: '',
        queryMobile: '',
        queryClass: ''
      }
      this.candidatesPagination.pageNum = 1
      this.candidatesList = []
      this.candidatesTotal = 0
      this.candidatesVisible = true
    },
    onCandidatesDialogOpened() {
      this.loadCandidates()
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
            queryClass: this.candidatesQuery.queryClass
          }
        })
        if (res.code === 200 || res.code === 0) {
          this.candidatesList = res.rows || []
          this.candidatesTotal = res.total || 0
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
    async submitBind(wecomStudent) {
      this.bindingId = wecomStudent.parentUserId
      try {
        const res = await request({
          url: '/system/student/match/bind',
          method: 'post',
          data: {
            matchId: this.currentMatchingRow.id || null,
            studentId: this.currentMatchingRow.studentId,
            userId: wecomStudent.parentUserId
          }
        })
        if (res.code === 200 || res.code === 0) {
          ElNotification({ title: '手動匹配成功', message: '匹配綁定已更新', type: 'success', duration: 3000 })
          this.candidatesVisible = false
          
          // 刷新未匹配彈窗中的列表
          if (this.unmatchedVisible) {
            this.loadUnmatchedList()
          }
          this.loadMatchList()
        }
      } catch (e) {
        console.error(e)
      } finally {
        this.bindingId = null
      }
    },

    // 確定匹配並批量同步更名至企微
    handleBatchSync() {
      if (this.syncableSelection.length === 0) return

      ElMessageBox.confirm(
        `確認要將選中的 ${this.syncableSelection.length} 筆學生姓名同步更名至企業微信並雙向對齊本地關係表嗎？<br/><small style="color: #ea580c;">注：同步過程中會自動保護學生的原有班級，防止退出班級。</small>`,
        '批量同步確認', 
        {
          confirmButtonText: '開始同步',
          cancelButtonText: '取消',
          type: 'warning',
          dangerouslyUseHTMLString: true
        }
      ).then(async () => {
        this.loading = true
        const ids = this.syncableSelection.map(x => x.id)
        try {
          const res = await request({
            url: '/system/student/match/sync',
            method: 'post',
            data: {
              matchIds: ids
            }
          })
          if (res.code === 200 || res.code === 0) {
            ElNotification({ 
              title: '同步執行完畢', 
              message: res.msg || res.data?.message || '選中學生的更名操作已同步至企業微信', 
              type: 'success', 
              duration: 5000,
              dangerouslyUseHTMLString: true 
            })
            this.loadMatchList()
          }
        } catch (e) {
          console.error(e)
        } finally {
          this.loading = false
        }
      }).catch(() => {})
    },

    // 批量刪除選中的數據
    handleBatchDelete() {
      if (this.deletableSelection.length === 0) return
      
      ElMessageBox.confirm(
        `確認要刪除選中的 ${this.deletableSelection.length} 筆學生匹配記錄嗎？<br/><small style="color: #ef4444;">注：此操作只會刪除匹配記錄，學籍資料仍從 student_profiles 讀取，不會影響企業微信或本地的學生關係數據。</small>`, 
        '批量刪除確認', 
        {
          confirmButtonText: '確定刪除',
          cancelButtonText: '取消',
          type: 'warning',
          dangerouslyUseHTMLString: true
        }
      ).then(async () => {
        this.loading = true
        const ids = this.deletableSelection.map(x => x.id)
        try {
          const res = await request({
            url: '/system/student/match/delete',
            method: 'post',
            data: {
              matchIds: ids
            }
          })
          if (res.code === 200 || res.code === 0) {
            ElNotification({ 
              title: '刪除成功', 
              message: res.msg || `成功刪除 ${ids.length} 筆匹配記錄`, 
              type: 'success', 
              duration: 3000
            })
            this.multipleSelection = []
            this.loadMatchList()
          }
        } catch (e) {
          console.error(e)
        } finally {
          this.loading = false
        }
      }).catch(() => {})
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
.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}
.search-form {
  margin-bottom: 15px;
  padding: 15px;
  background-color: #f9fbfd;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
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
.candidates-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.candidate-header-info {
  background-color: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #1e40af;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 14px;
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
  min-height: 520px;
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
  min-height: 52px;
  padding: 10px 16px;
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
