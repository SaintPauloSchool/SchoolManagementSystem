  <template>
  <div class="school-department-container">
    <div class="layout-content">
      <!-- 左侧部门树 -->
      <div class="left-sidebar">
        <div class="sidebar-header">
          <div class="sidebar-actions">
            <el-button
              type="primary"
              @click="handleAddDepartment"
              class="add-department-btn"
              icon="Plus"
            >
              新增部門
            </el-button>
          </div>
        </div>
        
        <el-tree
          ref="departmentTree"
          :data="departmentTree"
          :props="defaultProps"
          node-key="id"
          :expand-on-click-node="false"
          highlight-current
          @node-click="handleNodeClick"
          class="department-tree"
        >
          <template #default="{ node, data }">
            <span class="custom-tree-node">
              <el-icon class="tree-icon"><OfficeBuilding /></el-icon>
              <span>{{ data.name }}</span>
            </span>
          </template>
        </el-tree>
      </div>

      <!-- 右侧成员列表 -->
      <div class="main-content">
        <div class="content-header">
          <div class="header-info">
            <h2 class="department-title">{{ currentDepartment?.name || '選擇部門' }}</h2>
            <span class="member-count">{{ memberCount }} 位成員</span>
          </div>
        </div>

        <!-- 成员表格 -->
        <div class="table-wrapper">
          <el-table :data="currentMemberList" style="width: 100%" :max-height="'calc(100vh - 200px)'">
            <el-table-column prop="name" label="姓名" width="150" />
            <el-table-column prop="departmentName" label="所屬部門" width="180" />
            <el-table-column label="操作" width="100" fixed="right" align="center">
              <template #default="scope">
                <el-button 
                  type="danger" 
                  circle 
                  size="default"
                  @click="handleDelete(scope.row)"
                  class="delete-circle-btn"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <!-- 新增/编辑部门对话框 -->
    <el-dialog
      :title="dialogTitle"
      :visible.sync="departmentDialogVisible"
      width="500px"
      @close="resetDepartmentForm"
    >
      <el-form ref="departmentForm" :model="departmentForm" label-width="100px" :rules="departmentRules">
        <el-form-item label="上級部門" prop="parentId">
          <el-select
            v-model="departmentForm.parentId"
            placeholder="請選擇上級部門（不選則為根部門）"
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="item in treeSelectData"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            >
              <span style="float: left">{{ item.name }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="部門名稱" prop="name">
          <el-input v-model="departmentForm.name" placeholder="請輸入部門名稱" />
        </el-form-item>
        <el-form-item label="英文名稱" prop="nameEn">
          <el-input v-model="departmentForm.nameEn" placeholder="請輸入部門英文名稱" />
        </el-form-item>
        <el-form-item label="排序" prop="orderNum">
          <el-input-number v-model="departmentForm.orderNum" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="部門負責人" prop="departmentLeader">
          <el-input v-model="departmentForm.departmentLeader" placeholder="請輸入部門負責人 UserID" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="departmentDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitDepartmentForm">確 定</el-button>
      </div>
    </el-dialog>


  </div>
</template>

<script>
import { OfficeBuilding, Delete } from '@element-plus/icons-vue'
import request from '@/utils/request'

export default {
  name: 'SchoolDepartment',
  components: {
    OfficeBuilding,
    Delete
  },
  data() {
    return {
      departmentTree: [],
      currentDepartment: null,
      currentMemberList: [],
      memberCount: 0,
      defaultProps: {
        children: 'children',
        label: 'name',
        isLeaf: 'isLeaf'
      },
      departmentRules: {
        name: [
          { required: true, message: '請輸入部門名稱', trigger: 'blur' }
        ]
      },
      departmentDialogVisible: false,
      dialogTitle: '',
      departmentForm: {
        id: null,
        parentId: null,
        name: '',
        nameEn: '',
        orderNum: 0,
        departmentLeader: ''
      },
      treeSelectData: []
    }
  },
  mounted() {
    this.loadDepartmentTree()
  },
  methods: {
    async loadDepartmentTree() {
      try {
        const response = await request({
          url: '/system/schoolDepartment/tree',
          method: 'get'
        })
        
        if (response.code === 200 || response.code === 0) {
          this.departmentTree = response.data || []
          this.treeSelectData = this.buildTreeSelectData(this.departmentTree)
          
          // 默认选中第一个部门
          if (this.departmentTree.length > 0) {
            this.currentDepartment = this.departmentTree[0]
            this.loadMemberList(this.departmentTree[0])
            
            // 设置树组件的选中状态
            this.$nextTick(() => {
              if (this.$refs.departmentTree) {
                this.$refs.departmentTree.setCurrentKey(this.departmentTree[0].id)
              }
            })
          } else {
            this.$message.info('暫無部門數據，請新增部門')
          }
        } else {
          this.$message.error('加載失敗：' + (response.msg || '未知錯誤'))
        }
      } catch (error) {
        this.$message.error('加載失敗：' + (error.message || '網絡錯誤'))
      }
    },

    buildTreeSelectData(tree) {
      const result = []
      tree.forEach(node => {
        if (!node.isLeaf) {
          const newNode = {
            id: node.id,
            name: node.name,
            children: node.children ? this.buildTreeSelectData(node.children) : []
          }
          result.push(newNode)
        }
      })
      return result
    },

    handleNodeClick(data) {
      this.currentDepartment = data
      this.loadMemberList(data)
      
      // 如果有子节点，自动展开
      if (data.children && data.children.length > 0) {
        this.$nextTick(() => {
          const node = this.$refs.departmentTree.getNode(data.id)
          if (node && !node.expanded) {
            node.expand()
          }
        })
      }
    },

    loadMemberList(department) {
      // 如果没有选中部门，清空列表
      if (!department || !department.id) {
        this.currentMemberList = []
        this.memberCount = 0
        return
      }
      
      // 收集当前部门及所有子部门的 ID
      const departmentIds = this.collectDepartmentIds(department)
      
      // 调用后端接口批量查询成员数据
      request({
        url: '/system/schoolDepartment/members',
        method: 'post',
        data: departmentIds
      }).then(response => {
        if (response.code === 200 || response.code === 0) {
          const members = response.data || []
          this.currentMemberList = members.map(member => ({
            ...member,
            departmentName: department.name
          }))
          this.memberCount = members.length
        } else {
          this.$message.error('加载成员失败')
          this.currentMemberList = []
          this.memberCount = 0
        }
      }).catch(error => {
        this.$message.error('加载失败')
        this.currentMemberList = []
        this.memberCount = 0
      })
    },

    collectDepartmentIds(department) {
      const ids = [department.id]
      if (department.children && department.children.length > 0) {
        department.children.forEach(child => {
          if (!child.isLeaf) {
            ids.push(...this.collectDepartmentIds(child))
          }
        })
      }
      return ids
    },

    handleAddDepartment() {
      this.dialogTitle = '新增部門'
      this.resetDepartmentForm()
      this.departmentDialogVisible = true
    },

    handleDelete(member) {
      this.$confirm(`確定要刪除成員 "${member.name}" 嗎？`, '提示', {
        confirmButtonText: '確定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const response = await request({
            url: `/system/schoolDepartment/member/${member.id}`,
            method: 'delete'
          })
          
          if (response.code === 200 || response.code === 0) {
            this.$message.success('刪除成功')
            // 重新加载当前部门成员列表
            if (this.currentDepartment) {
              this.loadMemberList(this.currentDepartment)
            }
          } else {
            this.$message.error('刪除失敗：' + (response.msg || '未知錯誤'))
          }
        } catch (error) {
          if (error !== 'cancel') {
            this.$message.error('刪除失敗：' + (error.message || '網絡錯誤'))
          }
        }
      }).catch(() => {
        // 用户取消删除
      })
    },

    submitDepartmentForm() {
      if (!this.$refs.departmentForm) {
        return
      }
      
      this.$refs.departmentForm.validate(async (valid) => {
        if (valid) {
          try {
            const url = '/system/schoolDepartment'
            const method = this.departmentForm.id ? 'put' : 'post'
            
            const response = await request({
              url: url,
              method: method,
              data: this.departmentForm
            })
            
            if (response.code === 200 || response.code === 0) {
              this.$message.success(this.departmentForm.id ? '更新成功' : '新增成功')
              this.departmentDialogVisible = false
              this.loadDepartmentTree()
            }
          } catch (error) {
            this.$message.error('提交失敗')
          }
        }
      })
    },

    resetDepartmentForm() {
      this.departmentForm = {
        id: null,
        parentId: null,
        name: '',
        nameEn: '',
        orderNum: 0,
        departmentLeader: ''
      }
      if (this.$refs.departmentForm) {
        this.$refs.departmentForm.clearValidate()
      }
    }
  }
}
</script>

<style scoped>
.school-department-container {
  height: calc(100vh - 84px);
  padding: 16px;
  background: #f0f2f5;
}

/* 主体布局 */
.layout-content {
  display: flex;
  height: 100%;
  gap: 16px;
  overflow: hidden;
}

/* 左侧边栏 */
.left-sidebar {
  width: 280px;
  background: #fff;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  height: 100%;
}

:deep(.sidebar-actions) {
  padding: 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.add-department-btn {
  width: 100%;
  height: 36px;
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s;
}

.add-department-btn:hover {
  opacity: 0.9;
}

.department-tree {
  flex: 1;
  overflow-y: auto;
  padding: 12px 8px;
}

.department-tree::-webkit-scrollbar {
  width: 6px;
}

.department-tree::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 3px;
}

/* 树节点内容样式 */
:deep(.el-tree-node__content) {
  height: 40px;
  border-radius: 6px;
  margin: 4px 0;
  transition: all 0.3s;
}

:deep(.el-tree-node__content:hover) {
  background-color: #f5f5f5;
}

/* 选中节点样式 */
:deep(.el-tree-node.is-current > .el-tree-node__content) {
  background-color: #e6f7ff;
}

.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  padding: 8px 12px;
  font-size: 14px;
  color: #595959;
  cursor: pointer;
}

.custom-tree-node .tree-icon {
  margin-right: 8px;
  font-size: 16px;
  color: #1890ff;
}

/* 选中节点的文字和图标颜色 */
:deep(.el-tree-node.is-current > .el-tree-node__content) .custom-tree-node {
  color: #1890ff;
}

:deep(.el-tree-node.is-current > .el-tree-node__content) .custom-tree-node .tree-icon {
  color: #1890ff;
}

/* 树节点展开箭头样式 */
:deep(.el-tree-node__expand-icon) {
  font-size: 14px;
  color: #8c8c8c;
  transition: all 0.3s ease;
}

:deep(.el-tree-node__expand-icon.is-leaf) {
  color: transparent;
}

:deep(.el-tree-node__expand-icon:hover) {
  color: #1890ff;
}

:deep(.el-tree-node--expanded > .el-tree-node__content .el-tree-node__expand-icon) {
  transform: rotate(90deg);
}

/* 选中节点的展开箭头 */
:deep(.el-tree-node.is-current > .el-tree-node__content .el-tree-node__expand-icon) {
  color: #1890ff;
}

/* 主内容区 */
.main-content {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  height: 100%;
}

.content-header {
  padding: 20px 24px;
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.department-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #262626;
}

.member-count {
  font-size: 14px;
  color: #8c8c8c;
  background: #f5f5f5;
  padding: 4px 12px;
  border-radius: 12px;
}

.table-wrapper {
  flex: 1;
  overflow: hidden;
  padding: 16px 24px;
  display: flex;
  flex-direction: column;
}

:deep(.el-table) {
  border: none;
}

:deep(.el-table::before) {
  height: 0;
}

:deep(.el-table th) {
  font-size: 14px;
  padding: 12px 0;
}

:deep(.el-table td) {
  padding: 12px 0;
  font-size: 14px;
}

:deep(.el-table__row:hover) {
  background: #fafafa;
}

/* 对话框样式 */
:deep(.el-dialog) {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.el-dialog__header) {
  padding: 20px 24px;
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  margin: 0;
}

:deep(.el-dialog__title) {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
}

:deep(.el-dialog__body) {
  padding: 24px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #262626;
}

:deep(.el-input__inner) {
  border-radius: 6px;
  border-color: #d9d9d9;
}

:deep(.el-input__inner:focus) {
  border-color: #40a9ff;
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  border: none;
}

:deep(.el-button--primary:hover) {
  opacity: 0.9;
}

/* 删除按钮 - 红色实心圆形 */
.delete-circle-btn {
  width: 32px;
  height: 32px;
  min-width: 32px;
  min-height: 32px;
  padding: 0 !important;
  border-radius: 50% !important;
  background: linear-gradient(135deg, #ff7875 0%, #ff4d4f 100%) !important;
  border: none !important;
  box-shadow: none !important;
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1) !important;
  overflow: hidden !important;
  vertical-align: middle;
}

/* 覆盖全局样式的伪元素 */
.delete-circle-btn::before {
  display: none !important;
}

.delete-circle-btn:hover {
  background: linear-gradient(135deg, #ff9c99 0%, #ff7875 100%) !important;
  transform: translateY(-1px) !important;
}

.delete-circle-btn:active {
  transform: translateY(0) !important;
}

/* 删除图标容器 */
.delete-circle-btn .el-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

/* 删除图标 */
.delete-circle-btn .el-icon svg {
  color: #fff !important;
  width: 18px;
  height: 18px;
  font-size: 18px;
}
</style>
