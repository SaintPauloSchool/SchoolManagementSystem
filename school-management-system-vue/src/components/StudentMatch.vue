<template>
  <div class="student-match-container">
    <el-card class="box-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">
            <el-icon><User /></el-icon>
            學生數據匹配與更名
          </span>
          <div class="header-actions">
            <el-button type="success" :icon="Upload" @click="openUploadDialog">
              導入 Excel 對照
            </el-button>
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
              :disabled="multipleSelection.filter(x => x.studentUserIdWecom && x.syncStatus !== '1').length === 0"
              @click="handleBatchSync"
            >
              同步至企業微信 (已選可同步 {{ multipleSelection.filter(x => x.studentUserIdWecom && x.syncStatus !== '1').length }} 筆)
            </el-button>
            <el-button 
              type="danger" 
              :icon="Delete" 
              :disabled="multipleSelection.length === 0"
              @click="handleBatchDelete"
            >
              批量刪除 (已選 {{ multipleSelection.length }} 筆)
            </el-button>
            <el-button 
              type="danger" 
              plain
              :icon="Delete" 
              @click="handleClearAll"
            >
              清空對照數據
            </el-button>
          </div>
        </div>
      </template>

      <!-- 搜索欄 -->
      <el-form :model="searchForm" ref="queryForm" :inline="true" class="search-form">
        <el-form-item label="學生姓名">
          <el-input 
            v-model="searchForm.studentNameLocal" 
            placeholder="請輸入 Excel 姓名 或 企微姓名" 
            clearable 
            style="width: 220px;"
            @keyup.enter="handleSearch" 
          />
        </el-form-item>
        <el-form-item label="Excel 班級">
          <el-input 
            v-model="searchForm.classNameLocal" 
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
        <el-form-item label="同步狀態">
          <el-select v-model="searchForm.syncStatus" placeholder="請選擇" style="width: 130px;" clearable>
            <el-option label="未同步" value="0" />
            <el-option label="同步成功" value="1" />
            <el-option label="同步失敗" value="2" />
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
        :row-style="{ height: '56px' }"
        empty-text="暫無對照數據，請先點擊上方導入 Excel"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column prop="studentProfileNum" label="個人編號" min-width="160" align="center" show-overflow-tooltip />
        <el-table-column label="相片" width="80" align="center">
          <template #default="scope">
            <el-image
              v-if="scope.row.studentAlbumName"
              style="width: 40px; height: 40px; border-radius: 4px;"
              :src="getPhotoUrl(scope.row.classNameLocal, scope.row.studentAlbumName)"
              :preview-src-list="[getPhotoUrl(scope.row.classNameLocal, scope.row.studentAlbumName)]"
              :initial-index="0"
              fit="cover"
              loading="lazy"
              preview-teleported
            >
              <template #error>
                <div class="image-slot" style="display: flex; justify-content: center; align-items: center; width: 100%; height: 100%; background: #f5f7fa; color: #a8abb2;">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
            <span v-else class="text-placeholder">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="adid" label="帳號" min-width="100" align="center" show-overflow-tooltip />
        <el-table-column prop="studentNameLocal" label="Excel 姓名" min-width="110" align="center" />
        <el-table-column prop="classNameLocal" label="Excel 班級" min-width="90" align="center" />
        <el-table-column prop="classNum" label="班號" min-width="90" align="center" />
        <el-table-column prop="studentIdNum" label="學生證編號" min-width="130" align="center" show-overflow-tooltip />
        <el-table-column prop="idEnglishName" label="身份證英文名" min-width="150" show-overflow-tooltip />
        <el-table-column prop="englishFirstName" label="英文名" min-width="110" show-overflow-tooltip />
        <el-table-column prop="englishLastName" label="英文姓" min-width="100" show-overflow-tooltip />
        <el-table-column prop="studentNameWecom" label="企微姓名" min-width="110" align="center">
          <template #default="scope">
            <span v-if="scope.row.studentNameWecom">{{ scope.row.studentNameWecom }}</span>
            <span v-else class="text-placeholder">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="matchStatus" label="匹配狀態" min-width="120" align="center">
          <template #default="scope">
            <el-tag :type="getMatchStatusTag(scope.row.matchStatus)">
              {{ getMatchStatusText(scope.row.matchStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="syncStatus" label="同步狀態" min-width="120" align="center">
          <template #default="scope">
            <el-tooltip 
              v-if="scope.row.syncStatus === '2' && scope.row.errorMsg" 
              :content="scope.row.errorMsg" 
              placement="top"
            >
              <el-tag :type="getSyncStatusTag(scope.row.syncStatus)" class="clickable-tag">
                {{ getSyncStatusText(scope.row.syncStatus) }}
                <el-icon><info-filled /></el-icon>
              </el-tag>
            </el-tooltip>
            <el-tag v-else :type="getSyncStatusTag(scope.row.syncStatus)">
              {{ getSyncStatusText(scope.row.syncStatus) }}
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
                v-if="scope.row.syncStatus !== '1'"
                size="small" 
                type="primary" 
                :icon="Edit"
                @click="handleManualMatch(scope.row)"
              >
                手動匹配
              </el-button>
              <el-button 
                v-if="scope.row.syncStatus !== '1' && (scope.row.matchStatus === '1' || scope.row.matchStatus === '2')"
                size="small" 
                type="danger" 
                :icon="Delete"
                @click="handleClearMatch(scope.row)"
              >
                清除匹配
              </el-button>
              <span v-if="scope.row.syncStatus === '1'" class="text-success">已同步</span>
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

    <!-- 導入對話框 -->
    <el-dialog draggable title="導入 Excel 對照數據" v-model="uploadVisible" width="650px" append-to-body>
      <div class="upload-dialog-content">
        <div class="upload-instruction">
          <div class="instruction-title">
            <span style="display:flex;align-items:center;gap:5px;">
              <el-icon><info-filled /></el-icon> Excel 製作說明與範例：
            </span>
            <el-button
              type="primary"
              :icon="Download"
              :loading="templateDownloading"
              @click="downloadTemplate"
              style="margin-left:auto;"
            >
              下載對照模版
            </el-button>
          </div>
          
          <ul class="instruction-list">
            <li><strong>第一行：</strong> 說明行（系統會自動跳過）。</li>
            <li><strong>第二行：</strong> 表頭行（系統自動跳過），<strong>數據必須從第三行開始</strong>。</li>
            <li><strong>必填欄位：</strong> <code>IDName</code> (中文姓名) 與 <code>ClassSection</code> (班級簡寫) 為必填項。</li>
            <li><strong>班號與學生證編號：</strong> <code>ClassNum</code> (班號) 與 <code>DsejStudentID</code> (學生證編號) 可選填。導入時系統會自動去除學生證編號中的減號 <code>-</code>，用於自動匹配，並會與個人編號、帳號、班級、班號及姓名等自動拼接生成 <strong>學生相冊名</strong>。</li>
            <li><strong>系統匹配：</strong> 導入後數據為未匹配狀態。在勾選行點擊「同步至企業微信」時，系統將自動比對班級與姓名並執行同步更名。</li>
          </ul>

          <div class="excel-preview">
            <table class="excel-table">
              <thead>
                <tr>
                  <th></th>
                  <th>A列 (StudentProfileNumber)</th>
                  <th>B列 (ADID)</th>
                  <th>C列 (ClassSection)</th>
                  <th>D列 (ClassNum)</th>
                  <th>E列 (IDName)</th>
                  <th>F列 (IDEnglishName)</th>
                  <th>G列 (EnglishFirstName)</th>
                  <th>H列 (EnglishLastName)</th>
                  <th>I列 (DsejStudentID)</th>
                </tr>
              </thead>
              <tbody>
                <tr class="excel-header-row">
                  <td class="row-num">2</td>
                  <td>StudentProfileNumber</td>
                  <td>ADID</td>
                  <td>ClassSection</td>
                  <td>ClassNum</td>
                  <td>IDName</td>
                  <td>IDEnglishName</td>
                  <td>EnglishFirstName</td>
                  <td>EnglishLastName</td>
                  <td>DsejStudentID</td>
                </tr>
                <tr>
                  <td class="row-num">3</td>
                  <td>95339</td>
                  <td>s95339</td>
                  <td>K1E</td>
                  <td>10</td>
                  <td>張三</td>
                  <td>Cheong Sam</td>
                  <td>Sam</td>
                  <td>Cheong</td>
                  <td>1653086-1</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <el-upload
          ref="uploadRef"
          class="upload-demo"
          drag
          action=""
          :http-request="customUpload"
          :limit="1"
          accept=".xlsx, .xls"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            將學籍 Excel 檔案拖曳至此處，或 <em>點擊上傳</em>
          </div>
        </el-upload>
      </div>
    </el-dialog>

    <!-- 彈窗一：未匹配本地數據列表 -->
    <el-dialog draggable title="未匹配本地數據" v-model="unmatchedVisible" width="850px" append-to-body>
      <!-- 搜索欄 -->
      <el-form :model="unmatchedQuery" :inline="true" class="unmatched-search-form" style="margin-bottom: 15px; padding: 10px 14px; background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px;">
        <el-form-item label="學生姓名">
          <el-input 
            v-model="unmatchedQuery.studentNameLocal" 
            placeholder="Excel姓名/企微姓名" 
            clearable 
            style="width: 180px;"
            @keyup.enter="handleUnmatchedSearch" 
          />
        </el-form-item>
        <el-form-item label="Excel 班級">
          <el-input 
            v-model="unmatchedQuery.classNameLocal" 
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

      <el-table :data="unmatchedList" v-loading="unmatchedLoading" max-height="400" empty-text="沒有未匹配的數據">
        <el-table-column prop="studentProfileNum" label="個人編號" width="120" align="center" />
        <el-table-column prop="studentNameLocal" label="Excel 姓名" width="150" align="center" />
        <el-table-column prop="classNameLocal" label="Excel 班級" width="100" align="center" />
        <el-table-column prop="classNum" label="班號" width="90" align="center" />
        <el-table-column prop="studentIdNum" label="學生證編號" width="130" align="center" />
        <el-table-column prop="studentAlbumName" label="學生相冊名" width="220" align="center" show-overflow-tooltip />
        <el-table-column prop="idEnglishName" label="英文名" show-overflow-tooltip />
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
    <el-dialog draggable title="手動綁定 - 選擇企業微信學生" v-model="candidatesVisible" width="800px" append-to-body>
      <div class="candidate-header-info" v-if="currentMatchingRow">
        待匹配 Excel 學生：<strong>{{ currentMatchingRow.studentNameLocal }}</strong> (班級: {{ currentMatchingRow.classNameLocal }})
      </div>
      
      <!-- 搜索欄 -->
      <el-form :model="candidatesQuery" :inline="true" class="candidates-search-form">
        <el-form-item label="企微姓名">
          <el-input 
            v-model="candidatesQuery.queryName" 
            placeholder="支持簡繁體姓名查詢" 
            clearable 
            style="width: 180px;"
            @keyup.enter="handleCandidatesSearch" 
          />
        </el-form-item>
        <el-form-item label="家長手機號">
          <el-input 
            v-model="candidatesQuery.queryMobile" 
            placeholder="家長聯絡手機" 
            clearable 
            style="width: 160px;"
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
        </el-form-item>
      </el-form>

      <!-- 候選學生列表 -->
      <el-table :data="candidatesList" v-loading="candidatesLoading" max-height="350" empty-text="未找到匹配的企微學生">
        <el-table-column prop="studentName" label="企微學生姓名" width="150" align="center" />
        <el-table-column prop="classCodeWecom" label="企微班級代碼" width="150" align="center">
          <template #default="scope">
            <span v-if="scope.row.classCodeWecom">{{ scope.row.classCodeWecom }}</span>
            <span v-else class="text-placeholder">未對照班級</span>
          </template>
        </el-table-column>
        <el-table-column prop="mobile" label="家長手機號" width="180" align="center" />
        <el-table-column prop="relationDesc" label="家長關係" width="120" align="center" />
        <el-table-column label="操作" align="center" fixed="right">
          <template #default="scope">
            <el-button 
              size="small" 
              type="success" 
              :loading="bindingId === scope.row.studentUserId"
              @click="submitBind(scope.row)"
            >
              確認綁定
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分頁 -->
      <div class="pagination-container" v-if="candidatesTotal > 0" style="margin-top: 15px; display: flex; justify-content: flex-end;">
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
    </el-dialog>

    <!-- 學生對照數據詳情對話框 -->
    <el-dialog title="學生對照數據詳情" v-model="detailVisible" width="1000px" append-to-body destroy-on-close>
      <el-row :gutter="20" align="middle">
        <el-col :span="6" style="text-align: center;">
          <el-image
            v-if="detailForm.studentAlbumName"
            style="width: 190px; height: 253px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);"
            :src="getPhotoUrl(detailForm.classNameLocal, detailForm.studentAlbumName)"
            :preview-src-list="[getPhotoUrl(detailForm.classNameLocal, detailForm.studentAlbumName)]"
            fit="cover"
            preview-teleported
          >
            <template #error>
              <div class="image-slot" style="display: flex; flex-direction: column; justify-content: center; align-items: center; width: 100%; height: 253px; background: #f5f7fa; color: #909399; border-radius: 8px; border: 1px dashed #dcdfe6;">
                <el-icon :size="40"><Picture /></el-icon>
                <span style="font-size: 12px; margin-top: 8px;">暫無相片</span>
              </div>
            </template>
          </el-image>
          <div v-else style="display: flex; flex-direction: column; justify-content: center; align-items: center; width: 190px; height: 253px; background: #f5f7fa; color: #909399; border-radius: 8px; border: 1px dashed #dcdfe6; margin: 0 auto;">
            <el-icon :size="40"><Picture /></el-icon>
            <span style="font-size: 12px; margin-top: 8px;">暫無相片</span>
          </div>
        </el-col>
        <el-col :span="18">
          <el-descriptions :column="2" border size="default">
            <el-descriptions-item label="個人編號" :span="2">{{ detailForm.studentProfileNum || '-' }}</el-descriptions-item>
            <el-descriptions-item label="帳號">{{ detailForm.adid || '-' }}</el-descriptions-item>
            <el-descriptions-item label="學生證編號">{{ detailForm.studentIdNum || '-' }}</el-descriptions-item>
            <el-descriptions-item label="Excel 姓名">{{ detailForm.studentNameLocal || '-' }}</el-descriptions-item>
            <el-descriptions-item label="Excel 班級">{{ detailForm.classNameLocal || '-' }}</el-descriptions-item>
            <el-descriptions-item label="班號">{{ detailForm.classNum || '-' }}</el-descriptions-item>
            <el-descriptions-item label="身份證英文名">{{ detailForm.idEnglishName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="英文名">{{ detailForm.englishFirstName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="英文姓">{{ detailForm.englishLastName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="企微姓名">{{ detailForm.studentNameWecom || '-' }}</el-descriptions-item>
            <el-descriptions-item label="企微 UserID">{{ detailForm.studentUserIdWecom || '-' }}</el-descriptions-item>
            <el-descriptions-item label="匹配狀態">
              <el-tag :type="getMatchStatusTag(detailForm.matchStatus)">
                {{ getMatchStatusText(detailForm.matchStatus) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="同步狀態">
              <el-tag :type="getSyncStatusTag(detailForm.syncStatus)">
                {{ getSyncStatusText(detailForm.syncStatus) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="同步錯誤原因" :span="2" v-if="detailForm.syncStatus === '2'">
              <span style="color: #ef4444;">{{ detailForm.errorMsg || '未知錯誤' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="創建時間">{{ formatTime(detailForm.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="更新時間">{{ formatTime(detailForm.updateTime) }}</el-descriptions-item>
          </el-descriptions>
        </el-col>
      </el-row>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailVisible = false">關 閉</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { User, Upload, Warning, Promotion, Search, Refresh, UploadFilled, Download, InfoFilled, Edit, Delete, Picture, Document } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { ElMessageBox, ElNotification } from 'element-plus'
import { API_BASE_PATH } from '@/utils/deployment'

export default {
  name: 'StudentMatch',
  components: {
    User, Upload, Warning, Promotion, Search, Refresh, UploadFilled, Download, InfoFilled, Delete, Picture, Document
  },
  data() {
    return {
      Upload, Warning, Promotion, Search, Refresh, Download, Delete, Document,
      loading: false,
      syncingData: false,
      matchList: [],
      total: 0,
      pagination: {
        currentPage: 1,
        pageSize: 10
      },
      searchForm: {
        studentNameLocal: '',
        classNameLocal: '',
        matchStatus: '',
        syncStatus: ''
      },
      multipleSelection: [],
      
      // 導入 Excel
      uploadVisible: false,
      templateDownloading: false,

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
        studentNameLocal: '',
        classNameLocal: ''
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
  methods: {
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
            studentNameLocal: this.searchForm.studentNameLocal,
            classNameLocal: this.searchForm.classNameLocal,
            matchStatus: this.searchForm.matchStatus,
            syncStatus: this.searchForm.syncStatus
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
        studentNameLocal: '',
        classNameLocal: '',
        matchStatus: '',
        syncStatus: ''
      }
      this.handleSearch()
    },
    handleSelectionChange(val) {
      this.multipleSelection = val
    },
    canSelectRow(row) {
      // 只有已匹配 (自動或手動) 且未同步成功的行才能被勾選進行同步
      return row.studentUserIdWecom && row.syncStatus !== '1'
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
    getSyncStatusTag(status) {
      const map = { '0': 'info', '1': 'success', '2': 'danger' }
      return map[status] || 'info'
    },
    getSyncStatusText(status) {
      const map = { '0': '未同步', '1': '同步成功', '2': '同步失敗' }
      return map[status] || '未知'
    },

    // 導入 Excel
    openUploadDialog() {
      this.uploadVisible = true
      this.$nextTick(() => {
        if (this.$refs.uploadRef) {
          this.$refs.uploadRef.clearFiles()
        }
      })
    },
    async downloadTemplate() {
      this.templateDownloading = true
      try {
        const response = await request({
          url: '/system/student/match/importTemplate',
          method: 'get',
          responseType: 'blob'
        })
        const blob = new Blob([response], {
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = '學籍對照導入模版.xlsx'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
      } catch (err) {
        console.error('下載對照模版失敗:', err)
        ElNotification({ title: '下載失敗', message: '對照模版下載失敗，請稍後再試', type: 'error', duration: 4000 })
      } finally {
        this.templateDownloading = false
      }
    },
    async customUpload(options) {
      const formData = new FormData()
      formData.append('file', options.file)
      try {
        const res = await request({
          url: '/system/student/match/import',
          method: 'post',
          data: formData,
          headers: { 'Content-Type': 'multipart/form-data' }
        })
        if (res.code === 200 || res.code === 0) {
          ElNotification({ title: '導入成功', message: res.msg || '導入與自動匹配成功', type: 'success', duration: 5000 })
          this.uploadVisible = false
          this.loadMatchList()
        } else {
          this.$nextTick(() => { if (this.$refs.uploadRef) this.$refs.uploadRef.clearFiles() })
          ElNotification({ title: '導入失敗', message: res.msg || '導入對照數據失敗', type: 'error', duration: 5000 })
        }
      } catch (err) {
        this.$nextTick(() => { if (this.$refs.uploadRef) this.$refs.uploadRef.clearFiles() })
        ElNotification({ title: '導入錯誤', message: '上傳解析發生錯誤，請稍後再試', type: 'error', duration: 4000 })
      }
    },

    // 彈窗一：未匹配本地數據
    async openUnmatchedDialog() {
      this.unmatchedVisible = true
      this.unmatchedPagination.pageNum = 1
      this.unmatchedQuery = {
        studentNameLocal: '',
        classNameLocal: ''
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
            studentNameLocal: this.unmatchedQuery.studentNameLocal,
            classNameLocal: this.unmatchedQuery.classNameLocal
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
        studentNameLocal: '',
        classNameLocal: ''
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
        queryName: '', // 手動匹配時不默認加上企微姓名查詢
        queryMobile: '',
        queryClass: row.classNameLocal // 預填 Excel 班級鎖定同班學生
      }
      this.candidatesPagination.pageNum = 1
      this.candidatesList = []
      this.candidatesVisible = true
      this.loadCandidates()
    },
    handleCandidatesSearch() {
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
        }
      } catch (e) {
        console.error(e)
      } finally {
        this.candidatesLoading = false
      }
    },
    async submitBind(wecomStudent) {
      this.bindingId = wecomStudent.studentUserId
      try {
        const res = await request({
          url: '/system/student/match/bind',
          method: 'post',
          data: {
            matchId: this.currentMatchingRow.id,
            studentUserIdWecom: wecomStudent.studentUserId
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

    async handleClearMatch(row) {
      try {
        await ElMessageBox.confirm(
          `確認要清除學生【${row.studentNameLocal}】的匹配關係嗎？`,
          '清除匹配確認',
          {
            confirmButtonText: '確定清除',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        this.loading = true
        const res = await request({
          url: '/system/student/match/clear',
          method: 'post',
          data: {
            matchId: row.id
          }
        })
        if (res.code === 200 || res.code === 0) {
          ElNotification({
            title: '清除匹配成功',
            message: '匹配關係已成功清除',
            type: 'success',
            duration: 3000
          })
          this.loadMatchList()
        }
      } catch (e) {
        if (e !== 'cancel') {
          console.error(e)
        }
      } finally {
        this.loading = false
      }
    },

    // 確定匹配並批量同步更名至企微
    handleBatchSync() {
      if (this.multipleSelection.length === 0) return
      
      ElMessageBox.confirm(
        `確認要將選中的 ${this.multipleSelection.length} 筆學生姓名同步更名至企業微信並雙向對齊本地關係表嗎？<br/><small style="color: #ea580c;">注：同步過程中會自動保護學生的原有班級，防止退出班級。</small>`, 
        '批量同步確認', 
        {
          confirmButtonText: '開始同步',
          cancelButtonText: '取消',
          type: 'warning',
          dangerouslyUseHTMLString: true
        }
      ).then(async () => {
        this.loading = true
        const ids = this.multipleSelection.map(x => x.id)
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
              message: res.msg || '選中學生的更名操作已同步至企業微信', 
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
      if (this.multipleSelection.length === 0) return
      
      ElMessageBox.confirm(
        `確認要刪除選中的 ${this.multipleSelection.length} 筆學生對照數據嗎？<br/><small style="color: #ef4444;">注：此操作只會刪除本對照表中的數據，不會刪除企業微信或本地的學生關係數據。</small>`, 
        '批量刪除確認', 
        {
          confirmButtonText: '確定刪除',
          cancelButtonText: '取消',
          type: 'warning',
          dangerouslyUseHTMLString: true
        }
      ).then(async () => {
        this.loading = true
        const ids = this.multipleSelection.map(x => x.id)
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
              message: res.msg || `成功刪除 ${ids.length} 筆數據`, 
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

    // 清空所有對照數據
    handleClearAll() {
      ElMessageBox.confirm(
        `確認要清空<b>所有</b>學生對照數據嗎？此操作不可恢復！<br/><small style="color: #ef4444;">注：此操作只會清空本對照表中的所有導入數據，不會影響企業微信或本地的學生關係數據。</small>`, 
        '清空對照數據確認', 
        {
          confirmButtonText: '確定清空',
          cancelButtonText: '取消',
          type: 'danger',
          dangerouslyUseHTMLString: true
        }
      ).then(async () => {
        this.loading = true
        try {
          const res = await request({
            url: '/system/student/match/clearAll',
            method: 'post'
          })
          if (res.code === 200 || res.code === 0) {
            ElNotification({ 
              title: '清空成功', 
              message: '所有對照數據已成功清空', 
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
    // 獲取相片完整網址
    getPhotoUrl(className, albumName) {
      if (!className || !albumName) return ''
      return `${API_BASE_PATH}/studentPhotos/${className}/${albumName}.jpg`
    },
    // 查看詳情
    handleViewDetail(row) {
      this.detailForm = { ...row }
      this.detailVisible = true
    },
    // 格式化時間
    formatTime(timeStr) {
      if (!timeStr) return '暫無資料'
      try {
        const date = new Date(timeStr)
        if (isNaN(date.getTime())) return timeStr
        const pad = (n) => n.toString().padStart(2, '0')
        return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
      } catch (e) {
        return timeStr
      }
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

/* 導入對話框 */
.upload-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.upload-demo {
  text-align: center;
}
.upload-instruction {
  background-color: #f8fafc;
  border-radius: 8px;
  padding: 14px;
  border: 1px solid #e2e8f0;
}
.instruction-title {
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.instruction-list {
  margin: 6px 0 12px 0;
  padding-left: 20px;
  font-size: 13px;
  color: #475569;
  line-height: 1.8;
}
.instruction-list code {
  background-color: #f1f5f9;
  color: #e11d48;
  padding: 2px 4px;
  border-radius: 4px;
  font-family: monospace;
}

/* Excel 預覽 */
.excel-preview {
  overflow-x: auto;
  border-radius: 4px;
  box-shadow: 0 0 0 1px #cbd5e1;
}
.excel-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
  background-color: white;
}
.excel-table th, .excel-table td {
  border: 1px solid #cbd5e1;
  padding: 6px 8px;
  white-space: nowrap;
}
.excel-table thead th {
  background-color: #f1f5f9;
  color: #475569;
  text-align: center;
  font-weight: bold;
}
.excel-table .row-num {
  background-color: #f1f5f9;
  color: #475569;
  text-align: center;
  font-weight: bold;
  width: 30px;
}
.excel-header-row td {
  background-color: #e2e8f0;
  font-weight: 600;
  color: #1e293b;
}

/* 候選學生對話方塊 */
.candidate-header-info {
  background-color: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #1e40af;
  padding: 10px 14px;
  border-radius: 6px;
  margin-bottom: 15px;
  font-size: 14px;
}
.candidates-search-form {
  padding: 10px 14px;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  margin-bottom: 15px;
}

/* 詳情彈窗 Descriptions 樣式微調 */
:deep(.el-descriptions__label) {
  min-width: 120px;
  word-break: keep-all !important;
  white-space: nowrap !important;
}
</style>
