<template>
  <el-dialog
    v-model="dialogVisible"
    title="選擇學生/家長"
    width="900px"
    :before-close="handleClose"
    class="student-selector-dialog"
    top="10vh"
  >
    <div class="selector-wrapper">
      <!-- 左侧树形结构 -->
      <div class="left-panel">
        <div class="panel-title">
          <el-icon><School /></el-icon>
          <span>組織架構</span>
        </div>
        
        <div class="tree-container">
          <div v-if="loading" class="loading">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>加載中...</span>
          </div>
          <div v-else-if="departmentTree.length === 0" class="empty">
            <el-icon><DocumentDelete /></el-icon>
            <span>暫無數據</span>
          </div>
          <el-tree
            v-else
            ref="classTree"
            :data="departmentTree"
            :props="treeProps"
            :expand-on-click-node="false"
            :check-on-click-node="false"
            :check-strictly="true"
            node-key="id"
          >
            <template #default="{ node, data }">
              <span class="tree-node">
                <!-- 只在葉子節點顯示選中狀態 -->
                <el-checkbox
                  v-if="data.isLeaf"
                  :model-value="selectedStudentIds.includes(data.id)"
                  @click.stop="() => handleLeafNodeClick(data)"
                  class="node-checkbox"
                />
                <el-icon v-if="data.type === 5" class="node-icon school-icon"><School /></el-icon>
                <el-icon v-else-if="data.type === 4" class="node-icon campus-icon"><OfficeBuilding /></el-icon>
                <el-icon v-else-if="data.type === 3" class="node-icon stage-icon"><Reading /></el-icon>
                <el-icon v-else-if="data.type === 2" class="node-icon grade-icon"><Notebook /></el-icon>
                <el-icon v-else-if="data.type === 1" class="node-icon class-icon"><User /></el-icon>
                <el-icon v-else-if="data.type === 10" class="node-icon parent-icon"><UserFilled /></el-icon>
                <el-icon v-else-if="data.isLeaf" class="node-icon relation-icon"><User /></el-icon>
                <span class="node-label">{{ node.label }}</span>
              </span>
            </template>
          </el-tree>
        </div>
      </div>

      <!-- 右侧已选区域 -->
      <div class="right-panel">
        <div class="panel-title">
          <el-icon><Checked /></el-icon>
          <span>已選擇 ({{ selectedStudentsWithDetails.length }})</span>
        </div>

        <div class="selected-container" ref="selectedContainer">
          <div v-if="selectedStudentsWithDetails.length > 0" class="selected-list">
            <div
              v-for="student in selectedStudentsWithDetails"
              :key="student.id"
              class="selected-tag"
            >
              <span class="selected-tag-name">{{ student.name }}</span>
              <el-button 
                link 
                type="danger" 
                size="small" 
                @click="removeSelectedStudent(student)"
                class="remove-btn"
              >
                <el-icon><CloseBold /></el-icon>
              </el-button>
            </div>
          </div>
          <div v-else class="empty-selected">
            <el-empty :image-size="80" description="請從左側選擇學生" />
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
          :disabled="selectedStudentsWithDetails.length === 0"
        >
          確定 ({{ selectedStudentsWithDetails.length }})
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
import { 
  Loading, DocumentDelete, School, OfficeBuilding, 
  Reading, Notebook, User, UserFilled, Checked, CloseBold 
} from '@element-plus/icons-vue'
import request from '@/utils/request'

export default {
  name: 'StudentSelectorDialog',
  components: {
    Loading, DocumentDelete, School, OfficeBuilding, 
    Reading, Notebook, User, UserFilled, Checked, CloseBold
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
      departmentTree: [],
      selectedStudentIds: [],
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
        isLeaf: (data) => {
          // 使用 isLeaf 字段判断是否为叶子节点
          return data.isLeaf === true;
        }
      }
    },
    selectedStudentsWithDetails() {
      const result = this.selectedStudentIds.map(id => {
        const relation = this.findRelationInTree(id, this.departmentTree)
        if (relation) {
          return {
            id: relation.id, // sys_parent_student_relation 的 ID
            studentUserId: relation.studentUserId,
            parentUserId: relation.parentUserId,
            name: relation.name, // 学生姓名 - 关系
            studentName: relation.name.split('-')[0],
            parentName: relation.name.split('-')[1],
            relationDesc: relation.relationDesc,
            mobile: relation.mobile
          }
        }
        return { id, name: '未知學生' }
      })
      return result
    },
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
          url: '/system/department/treeWithParents',
          method: 'get'
        })
        if (response.code === 200 || response.code === 0) {
          this.departmentTree = response.data || []

        } else {
          this.$message.error('加載學生數據失敗')
        }
      } catch (error) {
        this.$message.error('加载学生数据失败')
      } finally {
        this.loading = false
        this.$nextTick(() => {
          if (this.visible) {
            this.initSelectedTree()
          }
        })
      }
    },
  
    initSelectedTree() {
      if (!this.$refs.classTree) return;
  
      if (this.selectedStudents && this.selectedStudents.length > 0) {
        const studentIds = this.selectedStudents.map(student => student.id)
        this.$refs.classTree.setCheckedKeys(studentIds)
        setTimeout(() => {
          this.updateSelectedStudentIds()
        }, 50)
      } else {
        this.$refs.classTree.setCheckedKeys([])
        this.selectedStudentIds = []
      }
    },
  
    updateSelectedStudentIds() {
      if (!this.$refs.classTree) return;
      const checkedNodes = this.$refs.classTree.getCheckedNodes(false, false);
      const topLevelNodes = checkedNodes.filter(nodeData => {
         let node = this.$refs.classTree.getNode(nodeData.id);
         if (!node) return false;
         let parent = node.parent;
         while(parent && parent.level > 0) {
            if (parent.checked) return false;
            parent = parent.parent;
         }
         return true;
      });
      this.selectedStudentIds = topLevelNodes.map(n => n.id);
    },
  
    findRelationInTree(id, tree) {
      for (const node of tree) {
        // isLeaf=true 的节点是家长学生关系节点
        if (node.isLeaf && node.id === id) {
          return node
        }
        if (node.children) {
          const found = this.findRelationInTree(id, node.children)
          if (found) return found
        }
      }
      return null
    },

    handleClose() {
      if (this.$refs.classTree) {
        this.$refs.classTree.setCheckedKeys([])
      }
      this.selectedStudentIds = []
      this.$emit('update:visible', false)
    },

    handleConfirm() {
      this.$emit('confirm', this.selectedStudentsWithDetails)
      this.handleClose()
    },

    handleLeafNodeClick(data) {
      if (!data || !data.id) return;
      // 切换选中状态
      const index = this.selectedStudentIds.indexOf(data.id);
      if (index > -1) {
        // 取消选中
        this.selectedStudentIds = this.selectedStudentIds.filter(id => id !== data.id);
        // 取消树的选中状态
        this.$nextTick(() => {
          if (this.$refs.classTree) {
            this.$refs.classTree.setChecked(data, false);
          }
        });
      } else {
        // 选中
        this.selectedStudentIds.push(data.id);
        // 设置树的选中状态
        this.$nextTick(() => {
          if (this.$refs.classTree) {
            this.$refs.classTree.setChecked(data, true);
          }
          // 自动滚动到底部
          if (this.$refs.selectedContainer) {
            this.$refs.selectedContainer.scrollTop = this.$refs.selectedContainer.scrollHeight;
          }
        });
      }
    },

    removeSelectedStudent(student) {
      const index = this.selectedStudentIds.indexOf(student.id)
      if (index > -1) {
        // 使用新数组赋值以确保 Vue 响应式更新
        this.selectedStudentIds = this.selectedStudentIds.filter(id => id !== student.id)
        // 取消树的选中状态 - 从树中找到对应的节点
        this.$nextTick(() => {
          if (this.$refs.classTree) {
            const node = this.$refs.classTree.getNode(student.id)
            if (node) {
              this.$refs.classTree.setChecked(node, false)
            }
          }
        })
      }
    },

  }
}
</script>

<style scoped>
.student-selector-dialog .el-dialog__body {
  padding: 0;
}

.student-selector-dialog .el-dialog__header {
  padding: 16px 20px;
  background: linear-gradient(90deg, #409EFF 0%, #66b1ff 100%);
  border-radius: 0;
  margin-right: 0;
}

.student-selector-dialog .el-dialog__title {
  color: #ffffff;
  font-size: 18px;
  font-weight: 600;
}

.student-selector-dialog .el-dialog__headerbtn .el-dialog__close {
  color: #ffffff;
  transition: all 0.3s;
}

.student-selector-dialog .el-dialog__headerbtn .el-dialog__close:hover {
  transform: rotate(90deg);
  color: #f0f0f0;
}

.student-selector-dialog .el-dialog__footer {
  padding: 12px 20px;
  border-top: 1px solid #e8ecf1;
  background: #ffffff;
  border-radius: 0;
}

.selector-wrapper {
  display: flex;
  height: 500px;
  gap: 20px;
  padding: 20px;
  background: #ffffff;
}

/* 左侧面板 */
.left-panel {
  flex: 1.5;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #ffffff;
  padding: 0;
  border-radius: 0;
  box-shadow: none;
}

/* 右侧面板 */
.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #ffffff;
  padding: 0;
  border-radius: 0;
  box-shadow: none;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 14px;
  color: #303133;
  padding-bottom: 10px;
  border-bottom: 1px solid #e4e7ed;
}

.panel-title::after {
  content: none;
}

.panel-title .el-icon {
  font-size: 16px;
  color: #409EFF;
}

.tree-container {
  flex: 1;
  overflow-y: auto;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 12px;
  background: #ffffff;
}

.tree-container:hover {
  border-color: #409EFF;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.tree-container::-webkit-scrollbar {
  width: 6px;
}

.tree-container::-webkit-scrollbar-track {
  background: #f0f2f8;
  border-radius: 3px;
}

.tree-container::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}

.tree-container::-webkit-scrollbar-thumb:hover {
  background: #c0c4cc;
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
  color: #c0c4cc;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.node-checkbox {
  margin-right: 4px;
}

.parent-select-wrapper {
  margin-left: 8px;
  flex-shrink: 0;
}

.parent-select-wrapper .el-select {
  width: 120px;
}

.node-icon {
  font-size: 16px;
}

.school-icon { color: #E6A23C; }
.campus-icon { color: #409EFF; }
.stage-icon { color: #67C23A; }
.grade-icon { color: #909399; }
.class-icon { color: #F56C6C; }
.parent-icon { color: #67C23A; }
.relation-icon { color: #E6A23C; }

.grade-tag {
  font-size: 11px;
  border-radius: 3px;
}

.selected-container {
  flex: 1;
  overflow-y: auto;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 12px;
  background: #ffffff;
}

.selected-container:hover {
  border-color: #409EFF;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.selected-container::-webkit-scrollbar {
  width: 6px;
}

.selected-container::-webkit-scrollbar-track {
  background: #f0f2f8;
  border-radius: 3px;
}

.selected-container::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}

.selected-container::-webkit-scrollbar-thumb:hover {
  background: #c0c4cc;
}

.selected-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.selected-tag {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: #f5f7fa;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
  transition: all 0.3s;
  font-size: 14px;
  color: #606266;
}

.selected-tag:hover {
  background: #ecf5ff;
  border-color: #409EFF;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.12);
  transform: translateY(-1px);
}

.selected-tag-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-right: 8px;
}

.remove-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border-radius: 50%;
  opacity: 0.6;
  transition: all 0.3s;
  flex-shrink: 0;
}

.remove-btn:hover {
  opacity: 1;
  background: rgba(245, 108, 108, 0.1);
  transform: scale(1.1);
}

.remove-btn:active {
  transform: scale(0.95);
}

.remove-btn .el-icon {
  font-size: 14px;
  color: #F56C6C;
}

.empty-selected {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #c0c4cc;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
}

.dialog-footer .el-button {
  min-width: 80px;
  border-radius: 6px;
  font-weight: 500;
  transition: all 0.3s;
}

.dialog-footer .el-button--primary {
  background: #409EFF;
  border-color: #409EFF;
}

.dialog-footer .el-button--primary:hover:not(:disabled) {
  background: #66b1ff;
  border-color: #66b1ff;
}

.dialog-footer .el-button--primary:active:not(:disabled) {
  background: #3a8ee6;
  border-color: #3a8ee6;
}

/* 树节点样式优化 */
:deep(.el-tree) {
  background: transparent;
}

:deep(.el-tree-node__content) {
  height: auto;
  padding: 8px 10px;
  border-radius: 6px;
  margin-bottom: 4px;
  transition: all 0.3s;
}

:deep(.el-tree-node__content:hover) {
  background-color: #ecf5ff;
}

:deep(.el-tree-node__content.is-current) {
  background-color: #ecf5ff;
  color: #409EFF;
  font-weight: 600;
}

:deep(.el-checkbox__inner) {
  border-radius: 2px;
  border: 2px solid #dcdfe6;
  transition: all 0.3s;
}

:deep(.el-checkbox__inner:hover) {
  border-color: #667eea;
}

:deep(.el-checkbox.is-checked .el-checkbox__inner) {
  background: #409EFF;
  border-color: #409EFF;
}

:deep(.el-checkbox__input.is-indeterminate .el-checkbox__inner::before) {
  background: #409EFF;
}

/* 空状态优化 */
:deep(.el-empty__description) {
  color: #909399;
  font-size: 14px;
}

:deep(.el-empty__image) {
  opacity: 0.5;
}
</style>