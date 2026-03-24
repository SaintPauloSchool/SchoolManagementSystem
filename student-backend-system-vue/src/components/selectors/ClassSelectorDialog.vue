<template>
  <el-dialog
    v-model="dialogVisible"
    title="选择班级"
    width="750px"
    :before-close="handleClose"
    class="class-selector-dialog"
  >
    <div class="selector-wrapper">
      <!-- 左侧树形结构 -->
      <div class="left-panel">
        <div class="panel-title">
          <el-icon><School /></el-icon>
          <span>组织架构</span>
        </div>
        
        <el-input
          v-model="searchKeyword"
          placeholder="搜索班级..."
          clearable
          class="search-input"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <div class="tree-container">
          <div v-if="loading" class="loading">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>加载中...</span>
          </div>
          <div v-else-if="departmentTree.length === 0" class="empty">
            <el-icon><DocumentDelete /></el-icon>
            <span>暂无数据</span>
          </div>
          <el-tree
            v-else
            ref="classTree"
            :data="departmentTree"
            :props="treeProps"
            :expand-on-click-node="false"
            :check-on-click-node="true"
            node-key="id"
            show-checkbox
          >
            <template #default="{ node, data }">
              <span class="tree-node">
                <el-icon v-if="data.type === 5" class="node-icon school-icon"><School /></el-icon>
                <el-icon v-else-if="data.type === 4" class="node-icon campus-icon"><OfficeBuilding /></el-icon>
                <el-icon v-else-if="data.type === 3" class="node-icon stage-icon"><Reading /></el-icon>
                <el-icon v-else-if="data.type === 2" class="node-icon grade-icon"><Notebook /></el-icon>
                <el-icon v-else-if="data.type === 1" class="node-icon class-icon"><User /></el-icon>
                <span class="node-label">{{ node.label }}</span>
                <el-tag v-if="data.type === 1 && data.standardGrade" size="small" class="grade-tag">
                  {{ getGradeName(data.standardGrade) }}
                </el-tag>
              </span>
            </template>
          </el-tree>
        </div>
      </div>

      <!-- 右侧已选区域 -->
      <div class="right-panel">
        <div class="panel-title">
          <el-icon><Checked /></el-icon>
          <span>已选择 ({{ selectedClasses.length }})</span>
        </div>

        <div class="selected-container">
          <div
            v-for="cls in selectedClasses"
            :key="cls.id"
            class="selected-item"
          >
            <div class="item-info">
              <el-icon class="item-icon"><User /></el-icon>
              <span>{{ cls.name }}</span>
            </div>
            <el-button link type="danger" size="small" @click="removeSelectedClass(cls)">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
          <div v-if="selectedClasses.length === 0" class="empty-selected">
            <el-empty :image-size="80" description="请从左侧选择班级" />
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button 
          type="primary" 
          @click="handleConfirm"
          :disabled="selectedClasses.length === 0"
        >
          确定 ({{ selectedClasses.length }})
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
import { 
  Search, Loading, DocumentDelete, School, OfficeBuilding, 
  Reading, Notebook, User, Checked, Close 
} from '@element-plus/icons-vue'
import request from '@/utils/request'

export default {
  name: 'ClassSelectorDialog',
  components: {
    Search, Loading, DocumentDelete, School, OfficeBuilding, 
    Reading, Notebook, User, Checked, Close
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
      departmentTree: [],
      selectedClassIds: [],
      loading: false
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
    treeProps() {
      return {
        children: 'children',
        label: 'name',
        isLeaf: (data) => data.type === 1
      }
    },
    selectedClassesWithDetails() {
      return this.selectedClassIds.map(id => {
        const cls = this.findClassInTree(id, this.departmentTree)
        return cls || { id, name: '未知班级' }
      })
    }
  },
  watch: {
    visible(newVal) {
      if (newVal) {
        this.loadData()
        this.$nextTick(() => {
          this.initSelectedTree()
        })
      }
    }
  },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const response = await request({
          url: '/system/department/tree',
          method: 'get'
        })
        if (response.code === 200 || response.code === 0) {
          this.departmentTree = response.data || []
          this.$nextTick(() => {
            this.initSelectedTree()
          })
        } else {
          this.$message.error('加载班级数据失败')
        }
      } catch (error) {
        console.error('加载班级数据失败:', error)
        this.$message.error('加载班级数据失败')
      } finally {
        this.loading = false
      }
    },

    initSelectedTree() {
      if (this.$refs.classTree && this.selectedClasses && this.selectedClasses.length > 0) {
        const classIds = this.selectedClasses.map(cls => cls.id)
        this.$refs.classTree.setCheckedKeys(classIds)
      }
    },

    findClassInTree(id, tree) {
      for (const node of tree) {
        if (node.id === id) {
          return node
        }
        if (node.children) {
          const found = this.findClassInTree(id, node.children)
          if (found) return found
        }
      }
      return null
    },

    getGradeName(standardGrade) {
      if (!standardGrade) return ''
      const grades = ['一年级', '二年级', '三年级', '四年级', '五年级', '六年级', 
                      '七年级', '八年级', '九年级', '高一', '高二', '高三']
      return grades[standardGrade - 1] || `${standardGrade}年级`
    },

    handleClose() {
      this.searchKeyword = ''
      if (this.$refs.classTree) {
        this.$refs.classTree.setCheckedKeys([])
      }
      this.selectedClassIds = []
      this.$emit('update:visible', false)
    },

    handleConfirm() {
      if (this.$refs.classTree) {
        const checkedKeys = this.$refs.classTree.getCheckedKeys()
        this.selectedClassIds = Array.from(checkedKeys)
      }
      this.$emit('confirm', this.selectedClassesWithDetails)
      this.handleClose()
    },

    removeSelectedClass(cls) {
      const index = this.selectedClassIds.indexOf(cls.id)
      if (index > -1) {
        this.selectedClassIds.splice(index, 1)
      }
    }
  }
}
</script>

<style scoped>
.class-selector-dialog .el-dialog__body {
  padding: 0;
}

.selector-wrapper {
  display: flex;
  height: 450px;
  gap: 16px;
  padding: 16px;
}

/* 左侧面板 */
.left-panel {
  flex: 1.5;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 右侧面板 */
.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 15px;
  color: #303133;
  padding-bottom: 8px;
  border-bottom: 1px solid #e4e7ed;
}

.panel-title .el-icon {
  font-size: 18px;
  color: #409EFF;
}

.search-input {
  width: 100%;
}

.tree-container {
  flex: 1;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 12px;
  background: #f5f7fa;
}

.tree-container::-webkit-scrollbar {
  width: 6px;
}

.tree-container::-webkit-scrollbar-track {
  background: transparent;
}

.tree-container::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}

.loading, .empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  gap: 12px;
  color: #909399;
}

.loading .el-icon {
  font-size: 32px;
  color: #409EFF;
}

.empty .el-icon {
  font-size: 48px;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.node-icon {
  font-size: 16px;
}

.school-icon { color: #E6A23C; }
.campus-icon { color: #409EFF; }
.stage-icon { color: #67C23A; }
.grade-icon { color: #909399; }
.class-icon { color: #F56C6C; }

.node-label {
  flex: 1;
  font-size: 14px;
}

.grade-tag {
  font-size: 11px;
}

.selected-container {
  flex: 1;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 12px;
  background: #f5f7fa;
}

.selected-container::-webkit-scrollbar {
  width: 6px;
}

.selected-container::-webkit-scrollbar-track {
  background: transparent;
}

.selected-container::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}

.selected-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  margin-bottom: 8px;
  background: #ffffff;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

.item-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.item-icon {
  color: #409EFF;
  font-size: 16px;
}

.item-info span {
  font-size: 14px;
  color: #606266;
}

.empty-selected {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #e4e7ed;
}

/* 树节点样式优化 */
:deep(.el-tree) {
  background: transparent;
}

:deep(.el-tree-node__content) {
  height: auto;
  padding: 6px 0;
  border-radius: 4px;
}

:deep(.el-tree-node__content:hover) {
  background-color: #ecf5ff;
}

:deep(.el-checkbox__inner) {
  border-radius: 2px;
}
</style>
