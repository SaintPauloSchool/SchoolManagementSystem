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
}

.search-area {
  padding: 15px;
  border-bottom: 1px solid #e4e7ed;
}

.directory-list {
  flex: 1;
  padding: 15px;
  overflow-y: auto;
}

.tree-node {
  display: flex;
  align-items: center;
}

.count-tag {
  margin-left: 8px;
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
  max-height: 120px;
  overflow-y: auto;
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