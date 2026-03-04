<template>
  <el-dialog
    v-model="dialogVisible"
    title="選擇學生/家長"
    width="700px"
    :before-close="handleClose"
  >
    <div class="selector-dialog">
      <!-- 搜索和篩選區域 -->
      <div class="search-area">
        <el-row :gutter="10">
          <el-col :span="12">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索學生姓名或學號"
              clearable
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-col>
          <el-col :span="12">
            <el-select
              v-model="selectedGrade"
              placeholder="選擇年級"
              clearable
              style="width: 100%"
            >
              <el-option label="一年级" value="一年级" />
              <el-option label="二年级" value="二年级" />
              <el-option label="三年级" value="三年级" />
              <el-option label="四年级" value="四年级" />
              <el-option label="五年级" value="五年级" />
              <el-option label="六年级" value="六年级" />
            </el-select>
          </el-col>
        </el-row>
      </div>

      <!-- 学生列表 -->
      <div class="student-list">
        <el-table
          :data="filteredStudents"
          style="width: 100%"
          max-height="300"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column prop="studentId" label="學號" width="100" align="center" />
          <el-table-column prop="name" label="學生姓名" width="120" align="center" />
          <el-table-column prop="grade" label="年級" width="100" align="center" />
          <el-table-column prop="className" label="班級" width="120" align="center" />
          <el-table-column prop="parentName" label="家長姓名" align="center" />
          <el-table-column prop="phone" label="聯繫電話" width="120" align="center" />
        </el-table>
        
        <div v-if="filteredStudents.length === 0" class="no-data">
          暫無匹配的學生數據
        </div>
      </div>

      <!-- 已選擇區域 -->
      <div class="selected-area">
        <div class="selected-header">
          已選擇 ({{ selectedStudents.length }})
        </div>
        <div class="selected-tags">
          <el-tag
            v-for="student in selectedStudents"
            :key="student.id"
            closable
            @close="removeSelectedStudent(student)"
            class="selected-tag"
          >
            {{ student.name }}({{ student.parentName }})
          </el-tag>
        </div>
      </div>
    </div>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleConfirm">確定</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script>
import { Search } from '@element-plus/icons-vue'
import axios from 'axios'

export default {
  name: 'StudentSelectorDialog',
  components: {
    Search
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    selectedStudents: {
      type: Array,
      default: () => []
    }
  },
  emits: ['update:visible', 'confirm'],
  data() {
    return {
      searchKeyword: '',
      selectedGrade: '',
      allStudents: [],
      selectedStudentIds: []
    }
  },
  computed: {
    dialogVisible: {
      get() {
        return this.visible
      },
      set(value) {
        this.$emit('update:visible', value)
      }
    },
    filteredStudents() {
      let result = [...this.allStudents]
      
      // 应用搜索关键词
      if (this.searchKeyword) {
        const keyword = this.searchKeyword.toLowerCase()
        result = result.filter(student =>
          student.name.toLowerCase().includes(keyword) ||
          student.studentId.toLowerCase().includes(keyword) ||
          student.parentName.toLowerCase().includes(keyword)
        )
      }
      
      // 应用年级筛选
      if (this.selectedGrade) {
        result = result.filter(student => student.grade === this.selectedGrade)
      }
      
      return result
    }
  },
  watch: {
    visible(newVal) {
      if (newVal) {
        this.loadData()
        this.initSelected()
      }
    }
  },
  methods: {
    async loadData() {
      try {
        // 這裡應該調用實際的 API 獲取學生數據
        // 暫時使用模擬數據
        this.allStudents = [
          { id: 101, studentId: '2023001', name: '张小明', grade: '一年级', className: '1班', parentName: '张父', phone: '13800138001' },
          { id: 102, studentId: '2023002', name: '李小红', grade: '一年级', className: '1班', parentName: '李母', phone: '13800138002' },
          { id: 103, studentId: '2023003', name: '王小华', grade: '一年级', className: '2班', parentName: '王父', phone: '13800138003' },
          { id: 104, studentId: '2023004', name: '赵小丽', grade: '二年级', className: '1班', parentName: '赵母', phone: '13800138004' },
          { id: 105, studentId: '2023005', name: '陈小强', grade: '二年级', className: '2班', parentName: '陈父', phone: '13800138005' },
          { id: 106, studentId: '2023006', name: '刘小美', grade: '三年级', className: '1班', parentName: '刘母', phone: '13800138006' }
        ]
      } catch (error) {
        console.error('加載學生數據失敗:', error)
        this.$message.error('加載學生數據失敗')
      }
    },

    initSelected() {
      this.selectedStudentIds = this.selectedStudents.map(student => student.id)
    },

    handleSelectionChange(selection) {
      this.selectedStudentIds = selection.map(item => item.id)
    },

    removeSelectedStudent(student) {
      const index = this.selectedStudentIds.indexOf(student.id)
      if (index > -1) {
        this.selectedStudentIds.splice(index, 1)
      }
    },

    handleClose() {
      this.searchKeyword = ''
      this.selectedGrade = ''
      this.selectedStudentIds = []
      this.$emit('update:visible', false)
    },

    handleConfirm() {
      const selectedStudents = this.allStudents.filter(student => 
        this.selectedStudentIds.includes(student.id)
      )
      this.$emit('confirm', selectedStudents)
      this.handleClose()
    }
  }
}
</script>

<style scoped>
.selector-dialog {
  height: 600px;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #ffffff 0%, #fafbfc 100%);
}

.search-area {
  padding: 20px;
  border-bottom: 2px solid #e5e7eb;
  background: linear-gradient(135deg, #f8fafc 0%, #ffffff 100%);
}

.student-list {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background: #ffffff;
}

.no-data {
  text-align: center;
  color: #9ca3af;
  padding: 60px 0;
  font-size: 15px;
  font-weight: 500;
}

.selected-area {
  border-top: 2px solid #e5e7eb;
  padding: 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #ffffff 100%);
  max-height: 180px;
  overflow-y: auto;
}

.selected-header {
  font-weight: 700;
  color: #374151;
  margin-bottom: 12px;
  font-size: 14px;
  letter-spacing: 0.3px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.selected-header::before {
  content: '';
  width: 4px;
  height: 16px;
  background: linear-gradient(to bottom, #3b82f6, #2563eb);
  border-radius: 2px;
  display: inline-block;
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.selected-tag {
  margin: 0;
  border-radius: 8px;
  font-weight: 600;
  transition: all 0.3s ease;
}

.selected-tag:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 24px;
  background: linear-gradient(to bottom, #f8fafc 0%, #ffffff 100%);
  border-top: 2px solid #e5e7eb;
}

.dialog-footer .el-button {
  min-width: 100px;
  height: 42px;
  font-weight: 600;
  font-size: 15px;
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 美化表格 */
:deep(.el-table) {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #f3f4f6;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

:deep(.el-table__header-wrapper th) {
  background: linear-gradient(to bottom, #f9fafb 0%, #ffffff 100%);
  color: #6b7280;
  font-weight: 700;
  font-size: 13px;
  border-bottom: 2px solid #e5e7eb;
}

:deep(.el-table__row td) {
  padding: 14px 0;
  border-bottom: 1px solid #f3f4f6;
}

:deep(.el-table__row:hover td) {
  background: linear-gradient(to right, #eff6ff 0%, #ffffff 100%);
}

/* 美化滚动条 */
.student-list::-webkit-scrollbar {
  width: 8px;
}

.student-list::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 4px;
}

.student-list::-webkit-scrollbar-thumb {
  background: linear-gradient(to bottom, #93c5fd, #3b82f6);
  border-radius: 4px;
  transition: all 0.3s ease;
}

.student-list::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(to bottom, #3b82f6, #2563eb);
}

.selected-area::-webkit-scrollbar {
  width: 8px;
}

.selected-area::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 4px;
}

.selected-area::-webkit-scrollbar-thumb {
  background: linear-gradient(to bottom, #93c5fd, #3b82f6);
  border-radius: 4px;
}
</style>