<template>
  <el-dialog
    v-model="dialogVisible"
    title="選擇教職員工"
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
          <span>组织架构</span>
        </div>
        
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
            ref="staffTree"
            :data="departmentTree"
            :props="treeProps"
            :expand-on-click-node="false"
            :check-on-click-node="false"
            node-key="id"
          >
            <template #default="{ node, data }">
              <span class="tree-node">
                <!-- 只在叶子节点显示选中状态 -->
                <el-checkbox
                  v-if="data.isLeaf"
                  :checked="selectedStaffIds.includes(data.id)"
                  @click.stop="() => handleLeafNodeClick(data)"
                  class="node-checkbox"
                />
                <el-icon v-if="data.type === 20" class="node-icon department-icon"><OfficeBuilding /></el-icon>
                <el-icon v-else-if="data.type === 10" class="node-icon position-icon"><UserFilled /></el-icon>
                <el-icon v-else-if="data.isLeaf" class="node-icon staff-icon"><User /></el-icon>
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
          <span>已选择 ({{ selectedStaffWithDetails.length }})</span>
        </div>

        <div class="selected-container" ref="selectedContainer">
          <div v-if="selectedStaffWithDetails.length > 0" class="selected-list">
            <div
              v-for="staff in selectedStaffWithDetails"
              :key="staff.id"
              class="selected-tag"
            >
              <span class="selected-tag-name">{{ staff.name }}</span>
              <el-button 
                link 
                type="danger" 
                size="small" 
                @click="removeSelectedStaff(staff)"
                class="remove-btn"
              >
                <el-icon><CloseBold /></el-icon>
              </el-button>
            </div>
          </div>
          <div v-else class="empty-selected">
            <el-empty :image-size="80" description="请从左侧选择教职员工" />
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
          :disabled="selectedStaffWithDetails.length === 0"
        >
          确定 ({{ selectedStaffWithDetails.length }})
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
import { 
  Loading, DocumentDelete, School, OfficeBuilding, 
  User, UserFilled, Checked, CloseBold 
} from '@element-plus/icons-vue'
import request from '@/utils/request'

export default {
  name: 'StaffSelectorDialog',
  components: {
    Loading, DocumentDelete, School, OfficeBuilding, 
    User, UserFilled, Checked, CloseBold
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    selectedStaff: {
      type: Array,
      default: () => []
    }
  },
  emits: ['update:visible', 'confirm'],
  data() {
    return {
      departmentTree: [],
      selectedStaffIds: [],
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
    selectedStaffWithDetails() {
      const result = this.selectedStaffIds.map(id => {
        const staff = this.findStaffInTree(id, this.departmentTree)
        if (staff) {
          // 查找所属部门：沿着树形结构向上查找最近的部门节点
          const department = this.findNearestDepartment(staff, this.departmentTree)
          return {
            id: staff.id,
            name: staff.name,
            position: staff.position,
            department: department ? department.name : '未知部门'
          }
        }
        return { id, name: '未知教职工', position: '', department: '' }
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
          url: '/system/schoolDepartment/treeWithMembers',
          method: 'get'
        })
        if (response.code === 200 || response.code === 0) {
          this.departmentTree = response.data || []
        } else {
          this.$message.error('加载教职员工数据失败')
        }
      } catch (error) {
        console.error('加载教职员工数据失败:', error)
        this.$message.error('加载教职员工数据失败')
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
      if (!this.$refs.staffTree) return;
  
      if (this.selectedStaff && this.selectedStaff.length > 0) {
        const staffIds = this.selectedStaff.map(staff => staff.id)
        
        // 直接设置勾选状态，不展开所有节点
        this.$refs.staffTree.setCheckedKeys(staffIds)
        // 直接使用传入的 selectedStaff 数组，不从树中重新获取
        this.selectedStaffIds = staffIds
      } else {
        this.$refs.staffTree.setCheckedKeys([])
        this.selectedStaffIds = []
      }
    },

    updateSelectedStaffIds() {
      // 不再从树中获取，直接使用 selectedStaffIds
      // 这个方法保留是为了兼容性，但实际不再使用
    },
  
    findStaffInTree(id, tree) {
      for (const node of tree) {
        // isLeaf=true 的节点是教职员工节点
        if (node.isLeaf && node.id === id) {
          return node
        }
        if (node.children) {
          const found = this.findStaffInTree(id, node.children)
          if (found) return found
        }
      }
      return null
    },

    findDepartmentByParentId(parentId, tree) {
      for (const node of tree) {
        if (node.id === parentId) {
          return node
        }
        if (node.children) {
          const found = this.findDepartmentByParentId(parentId, node.children)
          if (found) return found
        }
      }
      return null
    },

    // 沿着树形结构向上查找，找到最近的部门节点（非叶子节点）
    findNearestDepartment(staffNode, tree) {
      // 方法 1：先尝试用 parentId 查找
      if (staffNode.parentId) {
        const deptByParentId = this.findDepartmentByParentId(staffNode.parentId, tree)
        // 如果找到的是部门节点（非叶子），直接返回
        if (deptByParentId && !deptByParentId.isLeaf) {
          return deptByParentId
        }
      }
      
      // 方法 2：如果 parentId 指向的是叶子节点（员工），则在树中查找该员工所在的部门路径
      return this.findDepartmentByPath(staffNode.id, tree, null)
    },

    // 递归查找节点所在的部门路径
    findDepartmentByPath(staffId, tree, currentDept) {
      for (const node of tree) {
        // 如果当前节点是部门（非叶子），更新 currentDept
        const newCurrentDept = (!node.isLeaf) ? node : currentDept
        
        // 找到目标员工节点
        if (node.id === staffId) {
          return newCurrentDept
        }
        
        // 递归在子节点中查找
        if (node.children) {
          const found = this.findDepartmentByPath(staffId, node.children, newCurrentDept)
          if (found) return found
        }
      }
      return null
    },

    handleClose() {
      if (this.$refs.staffTree) {
        this.$refs.staffTree.setCheckedKeys([])
      }
      this.selectedStaffIds = []
      this.$emit('update:visible', false)
    },

    handleConfirm() {
      this.$emit('confirm', this.selectedStaffWithDetails)
      this.handleClose()
    },

    handleLeafNodeClick(data) {
      if (!data || !data.id) return;
      // 切换选中状态
      const index = this.selectedStaffIds.indexOf(data.id);
      if (index > -1) {
        // 取消选中
        this.selectedStaffIds.splice(index, 1);
      } else {
        // 选中
        this.selectedStaffIds.push(data.id);
        // 自动滚动到底部
        this.$nextTick(() => {
          if (this.$refs.selectedContainer) {
            this.$refs.selectedContainer.scrollTop = this.$refs.selectedContainer.scrollHeight;
          }
        });
      }
    },

    removeSelectedStaff(staff) {
      const index = this.selectedStaffIds.indexOf(staff.id)
      if (index > -1) {
        this.selectedStaffIds.splice(index, 1)
        // 更新树的勾选状态
        if (this.$refs.staffTree) {
          this.$refs.staffTree.setCheckedKeys(this.selectedStaffIds)
        }
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

.node-icon {
  font-size: 16px;
}

.department-icon { color: #409EFF; }
.position-icon { color: #E6A23C; }
.staff-icon { color: #67C23A; }

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