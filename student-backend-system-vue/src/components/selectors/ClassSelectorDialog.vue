<template>
  <el-dialog
    v-model="dialogVisible"
    title="選擇班級"
    width="600px"
    :before-close="handleClose"
  >
    <div class="selector-dialog">
      <!-- 搜索區域 -->
      <div class="search-area">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索班級名稱"
          clearable
          style="width: 300px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <!-- 班級列表 -->
      <div class="class-list">
        <el-checkbox-group v-model="selectedClassIds">
          <el-checkbox
            v-for="cls in filteredClasses"
            :key="cls.id"
            :label="cls.id"
            class="class-checkbox"
          >
            <div class="class-item">
              <div class="class-name">{{ cls.name }}</div>
              <div class="class-info">{{ cls.grade }} {{ cls.teacher }}</div>
            </div>
          </el-checkbox>
        </el-checkbox-group>
        
        <div v-if="filteredClasses.length === 0" class="no-data">
          暫無匹配的班級
        </div>
      </div>

      <!-- 已選擇區域 -->
      <div class="selected-area">
        <div class="selected-header">
          已選擇 ({{ selectedClasses.length }})
        </div>
        <div class="selected-tags">
          <el-tag
            v-for="cls in selectedClasses"
            :key="cls.id"
            closable
            @close="removeSelectedClass(cls)"
            class="selected-tag"
          >
            {{ cls.name }}
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
  name: 'ClassSelectorDialog',
  components: {
    Search
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    selectedClasses: {
      type: Array,
      default: () => []
    }
  },
  emits: ['update:visible', 'confirm'],
  data() {
    return {
      searchKeyword: '',
      allClasses: [],
      selectedClassIds: []
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
    filteredClasses() {
      if (!this.searchKeyword) {
        return this.allClasses
      }
      const keyword = this.searchKeyword.toLowerCase()
      return this.allClasses.filter(cls => 
        cls.name.toLowerCase().includes(keyword) ||
        cls.grade.toLowerCase().includes(keyword) ||
        cls.teacher.toLowerCase().includes(keyword)
      )
    },
    selectedClassesWithDetails() {
      return this.selectedClassIds.map(id => {
        const cls = this.allClasses.find(c => c.id === id)
        return cls || { id, name: '未知班級' }
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
        // 這裡應該調用實際的 API 獲取班級數據
        // 暫時使用模擬數據
        this.allClasses = [
          { id: 1, name: '一年级1班', grade: '一年级', teacher: '张老师' },
          { id: 2, name: '一年级2班', grade: '一年级', teacher: '李老师' },
          { id: 3, name: '二年级1班', grade: '二年级', teacher: '王老师' },
          { id: 4, name: '二年级2班', grade: '二年级', teacher: '赵老师' },
          { id: 5, name: '三年级1班', grade: '三年级', teacher: '陈老师' },
          { id: 6, name: '三年级2班', grade: '三年级', teacher: '刘老师' }
        ]
      } catch (error) {
        console.error('加載班級數據失敗:', error)
        this.$message.error('加載班級數據失敗')
      }
    },

    initSelected() {
      this.selectedClassIds = this.selectedClasses.map(cls => cls.id)
    },

    removeSelectedClass(cls) {
      const index = this.selectedClassIds.indexOf(cls.id)
      if (index > -1) {
        this.selectedClassIds.splice(index, 1)
      }
    },

    handleClose() {
      this.searchKeyword = ''
      this.selectedClassIds = []
      this.$emit('update:visible', false)
    },

    handleConfirm() {
      this.$emit('confirm', this.selectedClassesWithDetails)
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
  background: linear-gradient(180deg, #ffffff 0%, #fafbfc 100%);
}

.search-area {
  padding: 20px;
  border-bottom: 2px solid #e5e7eb;
  background: linear-gradient(135deg, #f8fafc 0%, #ffffff 100%);
}

.class-list {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background: #ffffff;
}

.class-checkbox {
  display: block;
  margin-bottom: 12px;
  padding: 14px 16px;
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 2px solid transparent;
}

.class-checkbox:hover {
  background-color: #eff6ff;
  border-color: #bfdbfe;
  transform: translateX(4px);
}

.class-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.class-name {
  font-weight: 700;
  color: #111827;
  font-size: 15px;
  letter-spacing: 0.3px;
}

.class-info {
  font-size: 13px;
  color: #6b7280;
  font-weight: 500;
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

/* 美化滚动条 */
.class-list::-webkit-scrollbar {
  width: 8px;
}

.class-list::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 4px;
}

.class-list::-webkit-scrollbar-thumb {
  background: linear-gradient(to bottom, #93c5fd, #3b82f6);
  border-radius: 4px;
  transition: all 0.3s ease;
}

.class-list::-webkit-scrollbar-thumb:hover {
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