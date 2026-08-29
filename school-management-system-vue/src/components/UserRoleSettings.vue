<template>
  <div class="user-role-settings">
    <el-card class="box-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">
            <el-icon><UserFilled /></el-icon>
            用戶角色設置
          </span>
          <div class="header-actions">
            <el-button
              type="danger"
              plain
              :icon="Delete"
              :disabled="selectedRows.length === 0"
              @click="handleBatchDelete"
            >
              批量刪除
            </el-button>
            <el-button type="primary" :icon="Plus" @click="openSelector">新增用戶角色</el-button>
          </div>
        </div>
      </template>

      <el-form :model="searchForm" :inline="true" class="search-form">
        <el-form-item label="姓名">
          <el-input
            v-model="searchForm.userName"
            placeholder="請輸入姓名"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="類型">
          <el-select v-model="searchForm.type" placeholder="全部" clearable style="width: 140px">
            <el-option label="超級管理員" value="0" />
            <el-option label="管理員" value="1" />
            <el-option label="其他" value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="狀態">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="正常" value="0" />
            <el-option label="停用" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜尋</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="tableData"
        style="width: 100%"
        :row-style="{ height: '52px' }"
        empty-text="暫無數據"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" align="center" />
        <el-table-column prop="userName" label="姓名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="senderDisplayName" label="對外顯示名稱" min-width="140" show-overflow-tooltip />
        <el-table-column prop="type" label="類型" width="120" align="center">
          <template #default="scope">
            <el-tag :type="typeTagType(scope.row.type)" size="small" effect="light">
              {{ typeLabel(scope.row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="狀態" width="90" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="small" effect="light">
              {{ scope.row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="備註" min-width="140" show-overflow-tooltip />
        <el-table-column prop="createTime" label="創建時間" width="170" align="center" />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <el-button size="small" type="primary" :icon="Edit" @click="handleEdit(scope.row)">修改</el-button>
              <el-button size="small" type="danger" :icon="Delete" @click="handleDelete(scope.row)">刪除</el-button>
            </div>
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

    <!-- 新增：設定類型後選人 -->
    <el-dialog
      v-model="addDialogVisible"
      title="新增用戶角色"
      width="480px"
      append-to-body
      @closed="resetAddForm"
    >
      <el-form label-width="100px">
        <el-form-item label="類型">
          <el-radio-group v-model="addForm.type">
            <el-radio label="1">管理員</el-radio>
            <el-radio label="0">超級管理員</el-radio>
            <el-radio label="2">其他</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="對外顯示名稱">
          <el-input
            v-model="addForm.senderDisplayName"
            placeholder="如：小學教務處；留空則使用登錄名"
            clearable
          />
        </el-form-item>
        <el-form-item label="備註">
          <el-input v-model="addForm.remark" type="textarea" :rows="2" placeholder="選填" />
        </el-form-item>
        <el-form-item label="選擇成員">
          <el-button type="primary" @click="selectorVisible = true">
            從 WeCom 老師通訊錄選擇
            <span v-if="pendingStaff.length">（已選 {{ pendingStaff.length }} 人）</span>
          </el-button>
          <div v-if="pendingStaff.length" class="selected-preview">
            <el-tag
              v-for="s in pendingStaff"
              :key="s.staffUserId || s.id"
              type="info"
              class="preview-tag"
              closable
              @close="removePending(s)"
            >
              {{ s.name }}
            </el-tag>
          </div>
          <div v-else class="selected-empty">尚未選擇成員</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAdd">確 定</el-button>
      </template>
    </el-dialog>

    <!-- 修改 -->
    <el-dialog v-model="editDialogVisible" title="修改用戶角色" width="480px" append-to-body>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="110px">
        <el-form-item label="姓名">
          <el-input v-model="editForm.userName" disabled />
        </el-form-item>
        <el-form-item label="對外顯示名稱">
          <el-input
            v-model="editForm.senderDisplayName"
            placeholder="如：小學教務處；留空則使用登錄名"
            clearable
          />
        </el-form-item>
        <el-form-item label="類型" prop="type">
          <el-radio-group v-model="editForm.type">
            <el-radio label="1">管理員</el-radio>
            <el-radio label="0">超級管理員</el-radio>
            <el-radio label="2">其他</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="狀態" prop="status">
          <el-radio-group v-model="editForm.status">
            <el-radio label="0">正常</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="備註">
          <el-input v-model="editForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEdit">確 定</el-button>
      </template>
    </el-dialog>

    <StaffSelectorDialog
      v-model:visible="selectorVisible"
      title="選擇用戶 — WeCom 老師通訊錄"
      :selected-staff="pendingStaff"
      :wecom-only="true"
      @confirm="handleStaffConfirm"
    />
  </div>
</template>

<script>
import { Plus, Search, Refresh, Edit, Delete, UserFilled } from '@element-plus/icons-vue'
import { ElMessageBox, ElNotification } from 'element-plus'
import request from '@/utils/request'
import StaffSelectorDialog from './selectors/StaffSelectorDialog.vue'

export default {
  name: 'UserRoleSettings',
  components: { UserFilled, StaffSelectorDialog },
  data() {
    return {
      Plus,
      Search,
      Refresh,
      Edit,
      Delete,
      loading: false,
      submitting: false,
      tableData: [],
      total: 0,
      selectedRows: [],
      pagination: {
        currentPage: 1,
        pageSize: 10
      },
      searchForm: {
        userName: '',
        type: '',
        status: ''
      },
      addDialogVisible: false,
      addForm: {
        type: '1',
        senderDisplayName: '',
        remark: ''
      },
      pendingStaff: [],
      selectorVisible: false,
      editDialogVisible: false,
      editForm: {
        id: null,
        userName: '',
        userId: '',
        type: '1',
        status: '0',
        senderDisplayName: '',
        remark: ''
      },
      editRules: {
        type: [{ required: true, message: '請選擇類型', trigger: 'change' }],
        status: [{ required: true, message: '請選擇狀態', trigger: 'change' }]
      }
    }
  },
  mounted() {
    this.loadList()
  },
  methods: {
    typeLabel(type) {
      if (type === '0') return '超級管理員'
      if (type === '2') return '其他'
      return '管理員'
    },
    typeTagType(type) {
      if (type === '0') return 'danger'
      if (type === '2') return 'warning'
      return 'primary'
    },
    handleSearch() {
      this.pagination.currentPage = 1
      this.loadList()
    },
    resetSearch() {
      this.searchForm = { userName: '', type: '', status: '' }
      this.handleSearch()
    },
    handleSelectionChange(rows) {
      this.selectedRows = rows || []
    },
    async loadList() {
      this.loading = true
      try {
        const response = await request({
          url: '/system/userRole/list',
          method: 'get',
          params: {
            pageNum: this.pagination.currentPage,
            pageSize: this.pagination.pageSize,
            userName: this.searchForm.userName || undefined,
            type: this.searchForm.type || undefined,
            status: this.searchForm.status || undefined
          }
        })
        if (response.code === 200 || response.code === 0) {
          this.tableData = response.rows || []
          this.total = response.total || 0
          this.selectedRows = []
          this.$nextTick(() => this.$refs.tableRef?.clearSelection())
        } else {
          ElNotification({
            title: '加載失敗',
            message: response.msg || '無權限或加載失敗',
            type: 'error',
            duration: 4000
          })
        }
      } catch (error) {
        console.error(error)
        ElNotification({
          title: '加載失敗',
          message: '資料加載失敗，請確認是否為超級管理員',
          type: 'error',
          duration: 4000
        })
      } finally {
        this.loading = false
      }
    },
    openSelector() {
      this.addForm = { type: '1', senderDisplayName: '', remark: '' }
      this.pendingStaff = []
      this.addDialogVisible = true
    },
    resetAddForm() {
      this.pendingStaff = []
      this.addForm = { type: '1', senderDisplayName: '', remark: '' }
    },
    handleStaffConfirm(staffList) {
      // WeCom 老師通訊錄（type=1），保留全部已選成員供預覽
      this.pendingStaff = (staffList || [])
        .filter(s => s && s.type !== 2 && s.type !== '2')
        .map(s => ({
          id: s.id,
          name: s.name || s.userName || '未命名',
          type: 1,
          staffUserId: s.staffUserId || s.userid || s.userId || '',
          sourceDeptId: s.sourceDeptId ?? null,
          sourceDeptName: s.sourceDeptName ?? null
        }))
    },
    removePending(staff) {
      const key = staff.staffUserId || staff.id
      this.pendingStaff = this.pendingStaff.filter(s => (s.staffUserId || s.id) !== key)
    },
    async submitAdd() {
      if (!this.pendingStaff.length) {
        ElNotification({ title: '提示', message: '請先選擇成員', type: 'warning', duration: 3000 })
        return
      }
      const invalid = this.pendingStaff.filter(s => !s.staffUserId)
      if (invalid.length) {
        ElNotification({
          title: '提示',
          message: '部分成員缺少 WeCom 用戶ID，請重新從老師通訊錄選擇',
          type: 'warning',
          duration: 4000
        })
        return
      }
      this.submitting = true
      try {
        const response = await request({
          url: '/system/userRole/batch',
          method: 'post',
          data: {
            type: this.addForm.type,
            senderDisplayName: this.addForm.senderDisplayName || null,
            remark: this.addForm.remark || null,
            userRoles: this.pendingStaff.map(s => ({
              userId: s.staffUserId,
              userName: s.name,
              type: this.addForm.type,
              senderDisplayName: this.addForm.senderDisplayName || null
            }))
          }
        })
        if (response.code === 200 || response.code === 0) {
          const data = response.data || {}
          ElNotification({
            title: '操作成功',
            message: data.message || '新增成功',
            type: 'success',
            duration: 4000
          })
          this.addDialogVisible = false
          this.loadList()
        } else {
          ElNotification({
            title: '操作失敗',
            message: response.msg || '新增失敗',
            type: 'error',
            duration: 4000
          })
        }
      } catch (error) {
        ElNotification({
          title: '操作失敗',
          message: error.message || '新增失敗',
          type: 'error',
          duration: 4000
        })
      } finally {
        this.submitting = false
      }
    },
    handleEdit(row) {
      this.editForm = {
        id: row.id,
        userName: row.userName,
        userId: row.userId,
        type: row.type || '1',
        status: row.status || '0',
        senderDisplayName: row.senderDisplayName || '',
        remark: row.remark || ''
      }
      this.editDialogVisible = true
    },
    async submitEdit() {
      this.submitting = true
      try {
        const response = await request({
          url: '/system/userRole',
          method: 'put',
          data: {
            id: this.editForm.id,
            type: this.editForm.type,
            status: this.editForm.status,
            remark: this.editForm.remark,
            userName: this.editForm.userName,
            senderDisplayName: this.editForm.senderDisplayName
          }
        })
        if (response.code === 200 || response.code === 0) {
          ElNotification({ title: '操作成功', message: '修改成功', type: 'success', duration: 3000 })
          this.editDialogVisible = false
          this.loadList()
        } else {
          ElNotification({
            title: '操作失敗',
            message: response.msg || '修改失敗',
            type: 'error',
            duration: 4000
          })
        }
      } catch (error) {
        ElNotification({
          title: '操作失敗',
          message: error.message || '修改失敗',
          type: 'error',
          duration: 4000
        })
      } finally {
        this.submitting = false
      }
    },
    handleDelete(row) {
      ElMessageBox.confirm(`確定刪除用戶角色「${row.userName || row.userId}」嗎？`, '刪除確認', {
        confirmButtonText: '確定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const response = await request({
            url: `/system/userRole/${row.id}`,
            method: 'delete'
          })
          if (response.code === 200 || response.code === 0) {
            ElNotification({ title: '操作成功', message: '刪除成功', type: 'success', duration: 3000 })
            this.loadList()
          } else {
            ElNotification({
              title: '操作失敗',
              message: response.msg || '刪除失敗',
              type: 'error',
              duration: 4000
            })
          }
        } catch (error) {
          ElNotification({
            title: '操作失敗',
            message: error.message || '刪除失敗',
            type: 'error',
            duration: 4000
          })
        }
      }).catch(() => {})
    },
    handleBatchDelete() {
      if (!this.selectedRows.length) {
        ElNotification({ title: '提示', message: '請至少選擇一條記錄', type: 'warning', duration: 3000 })
        return
      }
      const names = this.selectedRows.map(r => r.userName || r.userId).join('、')
      ElMessageBox.confirm(`確定刪除以下 ${this.selectedRows.length} 位用戶角色？\n${names}`, '批量刪除', {
        confirmButtonText: '確定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const ids = this.selectedRows.map(r => r.id).join(',')
          const response = await request({
            url: `/system/userRole/${ids}`,
            method: 'delete'
          })
          if (response.code === 200 || response.code === 0) {
            ElNotification({ title: '操作成功', message: '批量刪除成功', type: 'success', duration: 3000 })
            this.loadList()
          } else {
            ElNotification({
              title: '操作失敗',
              message: response.msg || '刪除失敗',
              type: 'error',
              duration: 4000
            })
          }
        } catch (error) {
          ElNotification({
            title: '操作失敗',
            message: error.message || '刪除失敗',
            type: 'error',
            duration: 4000
          })
        }
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.user-role-settings {
  padding: 0;
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

.header-actions {
  display: flex;
  gap: 10px;
}

.search-form {
  margin-bottom: 15px;
  padding: 15px;
  background-color: #f9fbfd;
  border-radius: 8px;
}

.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.action-buttons .el-button {
  border-radius: 6px;
  font-weight: 500;
  font-size: 13px;
  padding: 6px 12px;
  border: none;
  box-shadow: none !important;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding: 14px 0 0;
}

.selected-preview {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 32px;
  padding: 8px;
  background: #f8fafc;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
}

.selected-empty {
  margin-top: 10px;
  padding: 8px;
  color: #909399;
  font-size: 13px;
  background: #f8fafc;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
}

.preview-tag {
  margin: 0;
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
