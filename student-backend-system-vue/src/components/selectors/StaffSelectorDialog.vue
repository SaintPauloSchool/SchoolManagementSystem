<template>
  <el-dialog
    v-model="dialogVisible"
    :title="title"
    width="600px"
    :before-close="handleClose"
  >
    <div class="selector-dialog">
      <!-- 搜索区域 -->
      <div class="search-area">
        <el-input
          v-model="searchKeyword"
          :placeholder="`搜索${title.replace('选择', '').replace('抄送', '')}`"
          clearable
          style="width: 300px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <!-- 教職員工列表 -->
      <div class="staff-list">
        <el-checkbox-group v-model="selectedStaffIds">
          <el-checkbox
            v-for="staff in filteredStaff"
            :key="staff.id"
            :label="staff.id"
            class="staff-checkbox"
          >
            <div class="staff-item">
              <div class="staff-name">{{ staff.name }}</div>
              <div class="staff-info">{{ staff.department }} - {{ staff.position }}</div>
            </div>
          </el-checkbox>
        </el-checkbox-group>
        
        <div v-if="filteredStaff.length === 0" class="no-data">
          暂无匹配的教职工
        </div>
      </div>

      <!-- 已选择区域 -->
      <div class="selected-area">
        <div class="selected-header">
          已选择 ({{ selectedStaffWithDetails.length }})
        </div>
        <div class="selected-tags">
          <el-tag
            v-for="staff in selectedStaffWithDetails"
            :key="staff.id"
            closable
            @close="removeSelectedStaff(staff)"
            class="selected-tag"
          >
            {{ staff.name }}
          </el-tag>
        </div>
      </div>
    </div>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleConfirm">确定</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script>
import { Search } from '@element-plus/icons-vue'
import axios from 'axios'

export default {
  name: 'StaffSelectorDialog',
  components: {
    Search
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    selectedStaff: {
      type: Array,
      default: () => []
    },
    title: {
      type: String,
      default: '选择教职员工'
    }
  },
  emits: ['update:visible', 'confirm'],
  data() {
    return {
      searchKeyword: '',
      allStaff: [],
      selectedStaffIds: []
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
    filteredStaff() {
      if (!this.searchKeyword) {
        return this.allStaff
      }
      const keyword = this.searchKeyword.toLowerCase()
      return this.allStaff.filter(staff => 
        staff.name.toLowerCase().includes(keyword) ||
        staff.department.toLowerCase().includes(keyword) ||
        staff.position.toLowerCase().includes(keyword)
      )
    },
    selectedStaffWithDetails() {
      return this.selectedStaffIds.map(id => {
        const staff = this.allStaff.find(s => s.id === id)
        return staff || { id, name: '未知教职工' }
      })
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
        // 这里应该调用实际的API获取教职工数据
        // 暂时使用模拟数据
        this.allStaff = [
          { id: 201, name: '张主任', department: '教务处', position: '主任' },
          { id: 202, name: '李副校长', department: '校领导', position: '副校长' },
          { id: 203, name: '王老师', department: '语文组', position: '教师' },
          { id: 204, name: '赵老师', department: '数学组', position: '教师' },
          { id: 205, name: '陈老师', department: '英语组', position: '教师' },
          { id: 206, name: '刘老师', department: '体育组', position: '教师' }
        ]
      } catch (error) {
        console.error('加载教职工数据失败:', error)
        this.$message.error('加载教职工数据失败')
      }
    },

    initSelected() {
      this.selectedStaffIds = this.selectedStaff.map(staff => staff.id)
    },

    removeSelectedStaff(staff) {
      const index = this.selectedStaffIds.indexOf(staff.id)
      if (index > -1) {
        this.selectedStaffIds.splice(index, 1)
      }
    },

    handleClose() {
      this.searchKeyword = ''
      this.selectedStaffIds = []
      this.$emit('update:visible', false)
    },

    handleConfirm() {
      this.$emit('confirm', this.selectedStaffWithDetails)
      this.handleClose()
    }
  }
}
</script>

<style scoped>
.selector-dialog {
  height: 500px;
  display: flex;
  flex-direction: column;
}

.search-area {
  padding: 15px;
  border-bottom: 1px solid #e4e7ed;
}

.staff-list {
  flex: 1;
  padding: 15px;
  overflow-y: auto;
}

.staff-checkbox {
  display: block;
  margin-bottom: 10px;
  padding: 10px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.staff-checkbox:hover {
  background-color: #f5f7fa;
}

.staff-item {
  display: flex;
  flex-direction: column;
}

.staff-name {
  font-weight: 500;
  color: #303133;
  margin-bottom: 5px;
}

.staff-info {
  font-size: 12px;
  color: #909399;
}

.no-data {
  text-align: center;
  color: #909399;
  padding: 50px 0;
}

.selected-area {
  border-top: 1px solid #e4e7ed;
  padding: 15px;
  background-color: #f5f7fa;
}

.selected-header {
  font-weight: 500;
  color: #303133;
  margin-bottom: 10px;
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.selected-tag {
  margin: 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>