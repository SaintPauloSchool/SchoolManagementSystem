<template>
  <el-dialog
    v-model="dialogVisible"
    title="選擇學校通訊錄"
    width="600px"
    :before-close="handleClose"
  >
    <div class="selector-dialog">
      <!-- 搜索區域 -->
      <div class="search-area">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索通訊錄名稱"
          clearable
          style="width: 300px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <!-- 通訊錄列表 -->
      <div class="directory-list">
        <el-tree
          ref="treeRef"
          :data="directoryTree"
          :props="treeProps"
          show-checkbox
          node-key="id"
          :default-checked-keys="selectedDirectoryIds"
          :filter-node-method="filterNode"
          @check="handleCheck"
        >
          <template #default="{ node, data }">
            <div class="tree-node">
              <span>{{ data.name }}</span>
              <span v-if="data.count" class="count-tag">({{ data.count }})</span>
            </div>
          </template>
        </el-tree>
        
        <div v-if="directoryTree.length === 0" class="no-data">
          暫無通訊錄數據
        </div>
      </div>

      <!-- 已選擇區域 -->
      <div class="selected-area">
        <div class="selected-header">
          已選擇 ({{ selectedDirectories.length }})
        </div>
        <div class="selected-tags">
          <el-tag
            v-for="dir in selectedDirectories"
            :key="dir.id"
            closable
            @close="removeSelectedDirectory(dir)"
            class="selected-tag"
          >
            {{ dir.name }}
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
  name: 'DirectorySelectorDialog',
  components: {
    Search
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    selectedDirectories: {
      type: Array,
      default: () => []
    }
  },
  emits: ['update:visible', 'confirm'],
  data() {
    return {
      searchKeyword: '',
      directoryTree: [],
      selectedDirectoryIds: [],
      treeProps: {
        children: 'children',
        label: 'name'
      }
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
    }
  },
  watch: {
    visible(newVal) {
      if (newVal) {
        this.loadData()
        this.initSelected()
      }
    },
    searchKeyword(val) {
      this.$refs.treeRef.filter(val)
    }
  },
  methods: {
    async loadData() {
      try {
        // 這裡應該調用實際的 API 獲取通訊錄數據
        // 暫時使用模擬數據
        this.directoryTree = [
          {
            id: 301,
            name: '校领导',
            count: 5,
            children: [
              { id: 3011, name: '校长办公室', count: 1 },
              { id: 3012, name: '副校长办公室', count: 2 }
            ]
          },
          {
            id: 302,
            name: '行政部门',
            count: 12,
            children: [
              { id: 3021, name: '教务处', count: 3 },
              { id: 3022, name: '德育处', count: 2 },
              { id: 3023, name: '总务处', count: 4 }
            ]
          },
          {
            id: 303,
            name: '教学部门',
            count: 25,
            children: [
              { id: 3031, name: '语文教研组', count: 6 },
              { id: 3032, name: '数学教研组', count: 5 },
              { id: 3033, name: '英语教研组', count: 4 }
            ]
          },
          {
            id: 304,
            name: '其他部门',
            count: 8,
            children: [
              { id: 3041, name: '图书馆', count: 2 },
              { id: 3042, name: '医务室', count: 1 }
            ]
          }
        ]
      } catch (error) {
        console.error('加載通訊錄數據失敗:', error)
        this.$message.error('加載通訊錄數據失敗')
      }
    },

    initSelected() {
      this.selectedDirectoryIds = this.selectedDirectories.map(dir => dir.id)
    },

    filterNode(value, data) {
      if (!value) return true
      return data.name.toLowerCase().includes(value.toLowerCase())
    },

    handleCheck(data, checkedInfo) {
      this.selectedDirectoryIds = checkedInfo.checkedKeys
    },

    removeSelectedDirectory(dir) {
      const index = this.selectedDirectoryIds.indexOf(dir.id)
      if (index > -1) {
        this.selectedDirectoryIds.splice(index, 1)
        // 同步更新树的选中状态
        this.$refs.treeRef.setCheckedKeys(this.selectedDirectoryIds)
      }
    },

    getSelectedNodes() {
      const getAllNodes = (nodes) => {
        let result = []
        nodes.forEach(node => {
          if (this.selectedDirectoryIds.includes(node.id)) {
            result.push({
              id: node.id,
              name: node.name
            })
          }
          if (node.children && node.children.length > 0) {
            result = result.concat(getAllNodes(node.children))
          }
        })
        return result
      }
      return getAllNodes(this.directoryTree)
    },

    handleClose() {
      this.searchKeyword = ''
      this.selectedDirectoryIds = []
      this.$emit('update:visible', false)
    },

    handleConfirm() {
      const selectedDirs = this.getSelectedNodes()
      this.$emit('confirm', selectedDirs)
      this.handleClose()
    }
  }
}
</script>

<style scoped>
.selector-dialog {
  height: 550px;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #ffffff 0%, #fafbfc 100%);
}

.search-area {
  padding: 20px;
  border-bottom: 2px solid #e5e7eb;
  background: linear-gradient(135deg, #f8fafc 0%, #ffffff 100%);
}

.directory-list {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background: #ffffff;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
}

.count-tag {
  margin-left: 8px;
  font-size: 12px;
  color: #6b7280;
  font-weight: 600;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  color: #1e40af;
  padding: 2px 8px;
  border-radius: 6px;
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

/* 美化树组件 */
:deep(.el-tree) {
  background: transparent;
  border: none;
}

:deep(.el-tree-node__content) {
  padding: 8px 0;
  border-radius: 8px;
  transition: all 0.3s ease;
}

:deep(.el-tree-node__content:hover) {
  background-color: #eff6ff;
  transform: translateX(4px);
}

:deep(.el-checkbox__inner) {
  border-radius: 6px;
  border: 2px solid #d1d5db;
}

:deep(.el-checkbox__input.is-checked + .el-checkbox__label) {
  color: #3b82f6;
  font-weight: 600;
}

:deep(.el-checkbox__inner.is-checked) {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border-color: #3b82f6;
}

/* 美化滚动条 */
.directory-list::-webkit-scrollbar {
  width: 8px;
}

.directory-list::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 4px;
}

.directory-list::-webkit-scrollbar-thumb {
  background: linear-gradient(to bottom, #93c5fd, #3b82f6);
  border-radius: 4px;
  transition: all 0.3s ease;
}

.directory-list::-webkit-scrollbar-thumb:hover {
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