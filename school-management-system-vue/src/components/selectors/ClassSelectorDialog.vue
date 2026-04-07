<template>
  <el-dialog
    v-model="dialogVisible"
    title="選擇班級"
    width="900px"
    :before-close="handleClose"
    class="class-selector-dialog"
    top="10vh"
  >
    <div class="selector-wrapper">
      <!-- 左侧树形结构 -->
      <div class="left-panel">
        <div class="panel-tabs">
          <el-tabs v-model="activeTab" class="custom-tabs">
            <el-tab-pane name="wecom">
              <template #label>
                <div class="tab-label"><el-icon><School /></el-icon> WeCom通訊錄</div>
              </template>
            </el-tab-pane>
            <el-tab-pane name="custom">
              <template #label>
                <div class="tab-label"><el-icon><Menu /></el-icon> 自定義家校通訊錄</div>
              </template>
            </el-tab-pane>
          </el-tabs>
        </div>
        
        <!-- WeCom Tree -->
        <div class="tree-container" v-show="activeTab === 'wecom'">
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
            :check-on-click-node="true"
            node-key="id"
            show-checkbox
            @check="handleCheckChange"
          >
            <template #default="{ node, data }">
              <span class="tree-node">
                <el-icon v-if="data.type === 5" class="node-icon school-icon"><School /></el-icon>
                <el-icon v-else-if="data.type === 4" class="node-icon campus-icon"><OfficeBuilding /></el-icon>
                <el-icon v-else-if="data.type === 3" class="node-icon stage-icon"><Reading /></el-icon>
                <el-icon v-else-if="data.type === 2" class="node-icon grade-icon"><Notebook /></el-icon>
                <el-icon v-else-if="data.type === 1" class="node-icon class-icon"><User /></el-icon>
                <span class="node-label">{{ node.label }}</span>
              </span>
            </template>
          </el-tree>
        </div>

        <!-- Custom Tree -->
        <div class="tree-container" v-show="activeTab === 'custom'">
          <div v-if="customLoading" class="loading">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>加載中...</span>
          </div>
          <div v-else-if="customTree.length === 0" class="empty">
            <el-icon><DocumentDelete /></el-icon>
            <span>暫無數據</span>
          </div>
          <el-tree
            v-else
            ref="customClassTree"
            :data="customTree"
            :props="treeProps"
            :expand-on-click-node="false"
            :check-on-click-node="true"
            node-key="id"
            show-checkbox
            @check="handleCheckChange"
          >
            <template #default="{ node, data }">
              <span class="tree-node">
                <el-icon v-if="data.type === 5" class="node-icon school-icon"><School /></el-icon>
                <el-icon v-else-if="data.type === 4" class="node-icon campus-icon"><OfficeBuilding /></el-icon>
                <el-icon v-else-if="data.type === 3" class="node-icon stage-icon"><Reading /></el-icon>
                <el-icon v-else-if="data.type === 2" class="node-icon grade-icon"><Notebook /></el-icon>
                <el-icon v-else-if="data.type === 1" class="node-icon class-icon"><User /></el-icon>
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
          <span>已選擇 ({{ selectedClassesWithDetails.length }})</span>
        </div>

        <div class="selected-container" ref="selectedContainer">
          <div v-if="selectedClassesWithDetails.length > 0" class="selected-list">
            <div
              v-for="cls in selectedClassesWithDetails"
              :key="cls.id"
              class="selected-tag"
            >
              <span class="selected-tag-name">{{ cls.name }}</span>
              <el-button 
                link 
                type="danger" 
                size="small" 
                @click="removeSelectedClass(cls)"
                class="remove-btn"
              >
                <el-icon><CloseBold /></el-icon>
              </el-button>
            </div>
          </div>
          <div v-else class="empty-selected">
            <el-empty :image-size="80" description="請從左側選擇班級" />
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
          :disabled="selectedClassesWithDetails.length === 0"
        >
          確定 ({{ selectedClassesWithDetails.length }})
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
import { 
  Loading, DocumentDelete, School, OfficeBuilding, 
  Reading, Notebook, User, Checked, CloseBold, Menu 
} from '@element-plus/icons-vue'
import request from '@/utils/request'

export default {
  name: 'ClassSelectorDialog',
  components: {
    Loading, DocumentDelete, School, OfficeBuilding, 
    Reading, Notebook, User, Checked, CloseBold, Menu
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
      activeTab: 'wecom',
      departmentTree: [],
      customTree: [],
      selectedClassIds: [],
      loading: false,
      customLoading: false
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
        let cls = this.findClassInTree(id, this.departmentTree)
        if (cls) return { ...cls, type: 1 }
        
        cls = this.findClassInTree(id, this.customTree)
        if (cls) return { ...cls, type: 2 }
        
        return { id, name: '未知班級', type: 1 }
      })
    }
  },
  watch: {
    visible(newVal) {
      if (newVal) {
        this.loadData()
        this.loadCustomData()
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
        } else {
          this.$message.error('加載WeCom班級數據失敗')
        }
      } catch (error) {
        console.error('加載WeCom班級數據失敗:', error)
        this.$message.error('加載WeCom班級數據失敗')
      } finally {
        this.loading = false
        this.$nextTick(() => {
          if (this.visible) {
            this.initSelectedTree()
          }
        })
      }
    },

    async loadCustomData() {
      this.customLoading = true
      try {
        const response = await request({
          url: '/system/schoolDepartment/tree',
          method: 'get',
          params: { type: 2 }
        })
        if (response.code === 200 || response.code === 0) {
          this.customTree = response.data || []
        } else {
          this.$message.error('加載自定義班級數據失敗')
        }
      } catch (error) {
        console.error('加載自定義班級數據失敗:', error)
        // this.$message.error('加載自定義班級數據失敗')
      } finally {
        this.customLoading = false
        this.$nextTick(() => {
          if (this.visible) {
            this.initSelectedTree()
          }
        })
      }
    },

    initSelectedTree() {
      if (this.selectedClasses && this.selectedClasses.length > 0) {
        const classIds = this.selectedClasses.map(cls => cls.id)
        if (this.$refs.classTree) this.$refs.classTree.setCheckedKeys(classIds)
        if (this.$refs.customClassTree) this.$refs.customClassTree.setCheckedKeys(classIds)
        setTimeout(() => {
          this.updateSelectedClassIds()
        }, 50)
      } else {
        if (this.$refs.classTree) this.$refs.classTree.setCheckedKeys([])
        if (this.$refs.customClassTree) this.$refs.customClassTree.setCheckedKeys([])
        this.selectedClassIds = []
      }
    },

    updateSelectedClassIds() {
      let wecomNodes = []
      let customNodes = []
      
      if (this.$refs.classTree) {
        wecomNodes = this.$refs.classTree.getCheckedNodes(false, false).filter(nodeData => {
           let node = this.$refs.classTree.getNode(nodeData.id);
           if (!node) return false;
           let parent = node.parent;
           while(parent && parent.level > 0) {
              if (parent.checked) return false;
              parent = parent.parent;
           }
           return true;
        });
      }
      
      if (this.$refs.customClassTree) {
        customNodes = this.$refs.customClassTree.getCheckedNodes(false, false).filter(nodeData => {
           let node = this.$refs.customClassTree.getNode(nodeData.id);
           if (!node) return false;
           let parent = node.parent;
           while(parent && parent.level > 0) {
              if (parent.checked) return false;
              parent = parent.parent;
           }
           return true;
        });
      }
      
      // Merge unique IDs
      const allIds = new Set([
        ...wecomNodes.map(n => n.id), 
        ...customNodes.map(n => n.id)
      ]);
      this.selectedClassIds = Array.from(allIds);
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

    handleClose() {
      if (this.$refs.classTree) this.$refs.classTree.setCheckedKeys([])
      if (this.$refs.customClassTree) this.$refs.customClassTree.setCheckedKeys([])
      this.selectedClassIds = []
      this.$emit('update:visible', false)
    },

    handleConfirm() {
      this.$emit('confirm', this.selectedClassesWithDetails)
      this.handleClose()
    },

    removeSelectedClass(cls) {
      const index = this.selectedClassIds.indexOf(cls.id)
      if (index > -1) {
        this.selectedClassIds.splice(index, 1)
        // 更新两边树的勾选状态
        if (this.$refs.classTree) {
          this.$refs.classTree.setCheckedKeys(this.selectedClassIds)
        }
        if (this.$refs.customClassTree) {
          this.$refs.customClassTree.setCheckedKeys(this.selectedClassIds)
        }
      }
    },

    handleCheckChange(data, checkInfo) {
      // 找到事件源树
      const sourceTree = this.activeTab === 'wecom' ? this.$refs.classTree : this.$refs.customClassTree;
      if (!sourceTree) return;
      
      const isChecked = checkInfo.checkedKeys.includes(data.id);
      
      if (!isChecked) {
        let node = sourceTree.getNode(data.id);
        let parent = node ? node.parent : null;
        let ancestorInSelected = false;
        while(parent && parent.level > 0) {
           if (this.selectedClassIds.includes(parent.data.id)) {
               ancestorInSelected = true;
               break;
           }
           parent = parent.parent;
        }
        
        if (ancestorInSelected) {
           this.$message.warning('已选中上级组织，无法单独取消子项');
           sourceTree.setCheckedKeys(this.selectedClassIds);
           return;
        }
      }
      
      this.updateSelectedClassIds();
      
      // 如果是选中操作，自动滚动到底部
      if (isChecked) {
        this.$nextTick(() => {
          if (this.$refs.selectedContainer) {
            this.$refs.selectedContainer.scrollTop = this.$refs.selectedContainer.scrollHeight;
          }
        });
      }
    }
  }
}
</script>

<style scoped>
.class-selector-dialog .el-dialog__body {
  padding: 0;
}

.class-selector-dialog .el-dialog__header {
  padding: 16px 20px;
  background: linear-gradient(90deg, #409EFF 0%, #66b1ff 100%);
  border-radius: 0;
  margin-right: 0;
}

.class-selector-dialog .el-dialog__title {
  color: #ffffff;
  font-size: 18px;
  font-weight: 600;
}

.class-selector-dialog .el-dialog__headerbtn .el-dialog__close {
  color: #ffffff;
  transition: all 0.3s;
}

.class-selector-dialog .el-dialog__headerbtn .el-dialog__close:hover {
  transform: rotate(90deg);
  color: #f0f0f0;
}

.class-selector-dialog .el-dialog__footer {
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

.panel-tabs {
  border-bottom: 1px solid #e4e7ed;
}

:deep(.custom-tabs .el-tabs__header) {
  margin: 0;
}

:deep(.custom-tabs .el-tabs__nav-wrap::after) {
  height: 0;
}

:deep(.custom-tabs .el-tabs__item) {
  height: 40px;
  line-height: 40px;
  font-size: 14px;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tree-container {
  flex: 1;
  overflow-y: auto;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 12px;
  background: #ffffff;
  margin-top: 12px;
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

.node-icon {
  font-size: 16px;
}

.school-icon { color: #E6A23C; }
.campus-icon { color: #409EFF; }
.stage-icon { color: #67C23A; }
.grade-icon { color: #909399; }
.class-icon { color: #F56C6C; }

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
