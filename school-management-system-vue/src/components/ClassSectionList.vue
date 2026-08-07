<template>
  <div class="class-section-list">
    <el-card class="box-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">
            <el-icon><Collection /></el-icon>
            班級對照表
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
            <el-button type="primary" :icon="Plus" @click="handleAdd">新增班級</el-button>
          </div>
        </div>
      </template>

      <el-form :model="searchForm" :inline="true" class="search-form">
        <el-form-item label="DSEDJ 班級">
          <el-input
            v-model="searchForm.classSectionDsedj"
            placeholder="請輸入 DSEDJ 班級名稱"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="SP 班級">
          <el-input
            v-model="searchForm.classSectionSp"
            placeholder="請輸入 SP 班級代碼"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="學部">
          <el-select v-model="searchForm.division" placeholder="全部" style="width: 120px;" clearable>
            <el-option label="幼稚園" :value="0" />
            <el-option label="小學" :value="1" />
            <el-option label="中學" :value="2" />
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
        <el-table-column prop="classSectionDsedj" label="DSEDJ 班級" width="150" show-overflow-tooltip />
        <el-table-column prop="classSectionSp" label="SP 班級" width="90" align="center" />
        <el-table-column prop="division" label="學部" width="90" align="center">
          <template #default="scope">
            <span :class="['division-tag', `division-tag-${scope.row.division}`]">
              {{ getDivisionText(scope.row.division) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column class-name="table-gap" min-width="1" />
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

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="480px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="DSEDJ 班級" prop="classSectionDsedj">
          <el-input v-model="form.classSectionDsedj" placeholder="如：P1_A_家長" maxlength="8" show-word-limit />
        </el-form-item>
        <el-form-item label="SP 班級" prop="classSectionSp">
          <el-input v-model="form.classSectionSp" placeholder="如：P1A" maxlength="8" show-word-limit />
        </el-form-item>
        <el-form-item label="學部" prop="division">
          <el-select v-model="form.division" placeholder="請選擇學部" style="width: 100%">
            <el-option label="幼稚園" :value="0" />
            <el-option label="小學" :value="1" />
            <el-option label="中學" :value="2" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">確 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { Plus, Search, Refresh, Edit, Delete, Collection } from '@element-plus/icons-vue'
import { ElMessageBox, ElNotification } from 'element-plus'
import request from '@/utils/request'

export default {
  name: 'ClassSectionList',
  components: { Collection },
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
        classSectionDsedj: '',
        classSectionSp: '',
        division: null
      },
      dialogVisible: false,
      dialogTitle: '',
      isEditMode: false,
      form: {
        id: null,
        classSectionDsedj: '',
        classSectionSp: '',
        division: 0
      },
      rules: {
        classSectionDsedj: [{ required: true, message: 'DSEDJ 班級名稱不能為空', trigger: 'blur' }],
        classSectionSp: [{ required: true, message: 'SP 班級代碼不能為空', trigger: 'blur' }],
        division: [{ required: true, message: '請選擇學部', trigger: 'change' }]
      }
    }
  },
  mounted() {
    this.loadList()
  },
  methods: {
    getDivisionText(division) {
      const map = { 0: '幼稚園', 1: '小學', 2: '中學' }
      return map[division] ?? '未知'
    },
    async loadList() {
      this.loading = true
      try {
        const response = await request({
          url: '/system/basic/classSection/list',
          method: 'get',
          params: {
            pageNum: this.pagination.currentPage,
            pageSize: this.pagination.pageSize,
            classSectionDsedj: this.searchForm.classSectionDsedj,
            classSectionSp: this.searchForm.classSectionSp,
            division: this.searchForm.division
          }
        })
        if (response.code === 200 || response.code === 0) {
          this.tableData = response.rows || []
          this.total = response.total || 0
          this.selectedRows = []
          this.$nextTick(() => this.$refs.tableRef?.clearSelection())
        }
      } catch (error) {
        console.error(error)
        ElNotification({
          title: '加載失敗',
          message: '資料加載失敗，請檢查是否具有管理員權限',
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
        classSectionDsedj: '',
        classSectionSp: '',
        division: null
      }
      this.handleSearch()
    },
    handleAdd() {
      this.isEditMode = false
      this.form = {
        id: null,
        classSectionDsedj: '',
        classSectionSp: '',
        division: 0
      }
      this.dialogTitle = '新增班級對照'
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.formRef?.clearValidate())
    },
    handleEdit(row) {
      this.isEditMode = true
      this.form = {
        id: row.id,
        classSectionDsedj: row.classSectionDsedj,
        classSectionSp: row.classSectionSp,
        division: row.division
      }
      this.dialogTitle = '修改班級對照'
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.formRef?.clearValidate())
    },
    async submitForm() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) return
        this.submitting = true
        try {
          const res = await request({
            url: '/system/basic/classSection',
            method: this.isEditMode ? 'put' : 'post',
            data: this.form
          })
          if (res.code === 200 || res.code === 0) {
            ElNotification({
              title: this.isEditMode ? '修改成功' : '新增成功',
              message: '班級對照已保存',
              type: 'success',
              duration: 3000
            })
            this.dialogVisible = false
            this.loadList()
          } else {
            ElNotification({
              title: '操作失敗',
              message: res.msg || '操作失敗',
              type: 'error',
              duration: 4000
            })
          }
        } catch (error) {
          console.error(error)
        } finally {
          this.submitting = false
        }
      })
    },
    async handleDelete(row) {
      await this.deleteByIds([row.id], `確定刪除「${row.classSectionDsedj}」嗎？`)
    },
    handleSelectionChange(selection) {
      this.selectedRows = selection
    },
    async handleBatchDelete() {
      if (this.selectedRows.length === 0) {
        ElNotification({ title: '提示', message: '請至少選擇一條記錄', type: 'warning', duration: 3000 })
        return
      }
      const names = this.selectedRows.map(row => row.classSectionDsedj).join('、')
      await this.deleteByIds(
        this.selectedRows.map(row => row.id),
        `確定刪除選中的 ${this.selectedRows.length} 條記錄嗎？\n${names}`
      )
    },
    async deleteByIds(ids, confirmMessage) {
      try {
        await ElMessageBox.confirm(confirmMessage, '刪除確認', {
          type: 'warning',
          confirmButtonText: '確定',
          cancelButtonText: '取消'
        })
        const res = await request({
          url: `/system/basic/classSection/${ids.join(',')}`,
          method: 'delete'
        })
        if (res.code === 200 || res.code === 0) {
          ElNotification({
            title: '刪除成功',
            message: ids.length > 1 ? `已成功刪除 ${ids.length} 條記錄` : '班級對照已刪除',
            type: 'success',
            duration: 3000
          })
          this.loadList()
        } else {
          ElNotification({ title: '刪除失敗', message: res.msg || '刪除失敗', type: 'error', duration: 4000 })
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error(error)
        }
      }
    }
  }
}
</script>

<style scoped>
.class-section-list {
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

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding: 14px 0 0;
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

.division-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
}

.division-tag-0 {
  background-color: #dcfce7;
  color: #166534;
}

.division-tag-1 {
  background-color: #ffedd5;
  color: #9a3412;
}

.division-tag-2 {
  background-color: #fee2e2;
  color: #991b1b;
}

:deep(.table-gap) {
  padding: 0;
}

:deep(th.table-gap),
:deep(td.table-gap) {
  background: transparent;
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
