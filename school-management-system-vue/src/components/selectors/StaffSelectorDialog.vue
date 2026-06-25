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
      <!-- 左側樹形結構 -->
      <div class="left-panel">
        <div class="panel-tabs">
          <el-tabs v-model="activeTab" class="custom-tabs">
            <el-tab-pane name="wecom">
              <template #label>
                <div class="tab-label"><el-icon><School /></el-icon> WeCom老師通訊錄</div>
              </template>
            </el-tab-pane>
            <el-tab-pane name="custom">
              <template #label>
                <div class="tab-label"><el-icon><Menu /></el-icon> 自定義老師通訊錄</div>
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
            ref="staffTree"
            :data="departmentTree"
            :props="treeProps"
            :expand-on-click-node="false"
            :check-on-click-node="false"
            show-checkbox
            node-key="id"
            @check="handleTreeCheck"
          >
            <template #default="{ node, data }">
              <span class="tree-node">
                <el-icon v-if="data.type === 20" class="node-icon department-icon"><OfficeBuilding /></el-icon>
                <el-icon v-else-if="data.type === 10" class="node-icon position-icon"><UserFilled /></el-icon>
                <el-icon v-else-if="data.isLeaf" class="node-icon staff-icon"><User /></el-icon>
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
            ref="customStaffTree"
            :data="customTree"
            :props="treeProps"
            :expand-on-click-node="false"
            :check-on-click-node="false"
            show-checkbox
            node-key="id"
            @check="handleTreeCheck"
          >
            <template #default="{ node, data }">
              <span class="tree-node">
                <el-icon v-if="data.isLeaf" class="node-icon staff-icon"><User /></el-icon>
                <el-icon v-else class="node-icon folder-icon"><Folder /></el-icon>
                <span class="node-label">{{ node.label }}</span>
              </span>
            </template>
          </el-tree>
        </div>
      </div>

      <!-- 右側已選區域 -->
      <div class="right-panel">
        <div class="panel-title">
          <el-icon><Checked /></el-icon>
          <span>已選擇 ({{ selectedStaffWithDetails.length }})</span>
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
            <el-empty :image-size="80" description="請從左側選擇教職員工" />
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
          確定 ({{ selectedStaffWithDetails.length }})
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
import { ElNotification } from 'element-plus'
import { 
  Loading, DocumentDelete, School, OfficeBuilding, 
  User, UserFilled, Checked, CloseBold, Menu, Folder 
} from '@element-plus/icons-vue'
import request from '@/utils/request'

export default {
  name: 'StaffSelectorDialog',
  components: {
    Loading, DocumentDelete, School, OfficeBuilding, 
    User, UserFilled, Checked, CloseBold, Menu, Folder
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
      activeTab: 'wecom',
      departmentTree: [],
      customTree: [],
      selectedStaffIds: [],
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
        isLeaf: (data) => {
          // 使用 isLeaf 字段判斷是否爲葉子節點
          return data.isLeaf === true;
        }
      }
    },
    selectedStaffWithDetails() {
      const result = this.selectedStaffIds.map(id => {
        let staff = this.findStaffInTree(id, this.departmentTree)
        if (staff) {
          // 查找所屬部門：沿着樹形結構向上查找最近的部門節點
          const department = this.findNearestDepartment(staff, this.departmentTree)
          return {
            id: staff.id,
            name: staff.name,
            position: staff.position,
            department: department ? department.name : '未知部門',
            type: 1
          }
        }
        staff = this.findStaffInTree(id, this.customTree)
        if (staff) {
          const department = this.findNearestDepartment(staff, this.customTree)
          return {
            id: Math.abs(staff.id),
            name: staff.name,
            position: staff.position,
            department: department ? department.name : '未知部門',
            type: 2
          }
        }
        return { id, name: '未知教職員工', position: '', department: '', type: 1 }
      })
      return result
    },
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
          url: '/wecomSchoolDepartment/treeWithMembers',
          method: 'get'
        })
        if (response.code === 200 || response.code === 0) {
          this.departmentTree = response.data || []
        } else {
          ElNotification({ title: "操作失敗", message: '加載WeCom教職員工數據失敗', type: "error", duration: 4000 })
        }
      } catch (error) {
        console.error('加載WeCom教職員工數據失敗:', error)
        ElNotification({ title: "操作失敗", message: '加載WeCom教職員工數據失敗', type: "error", duration: 4000 })
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
          url: '/system/schoolDepartment/treeWithMembers',
          method: 'get',
          params: { type: 1 }
        })
        if (response.code === 200 || response.code === 0) {
          this.customTree = response.data || []
        } else {
          ElNotification({ title: "操作失敗", message: '加載自定義教職員工數據失敗', type: "error", duration: 4000 })
        }
      } catch (error) {
        console.error('加載自定義教職員工數據失敗:', error)
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
      let mappedIds = []
      if (this.selectedStaff && this.selectedStaff.length > 0) {
        mappedIds = this.selectedStaff.map(staff => {
           return staff.type === 2 ? -Math.abs(staff.id) : staff.id
        })
      }
      this.selectedStaffIds = mappedIds
      
      // 直接設置勾選狀態，不展開所有節點
      this.$nextTick(() => {
        if (this.$refs.staffTree) this.$refs.staffTree.setCheckedKeys(mappedIds)
        if (this.$refs.customStaffTree) this.$refs.customStaffTree.setCheckedKeys(mappedIds)
      })
    },

    findStaffInTree(id, tree) {
      for (const node of tree) {
        // isLeaf=true 的節點是教職員工節點
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

    // 沿着樹形結構向上查找，找到最近的部門節點（非葉子節點）
    findNearestDepartment(staffNode, tree) {
      // 方法 1：先嘗試用 parentId 查找
      if (staffNode.parentId) {
        const deptByParentId = this.findDepartmentByParentId(staffNode.parentId, tree)
        // 如果找到的是部門節點（非葉子），直接返回
        if (deptByParentId && !deptByParentId.isLeaf) {
          return deptByParentId
        }
      }
      
      // 方法 2：如果 parentId 指向的是葉子節點（員工），則在樹中查找該員工所在的部門路徑
      return this.findDepartmentByPath(staffNode.id, tree, null)
    },

    // 遞歸查找節點所在的部門路徑
    findDepartmentByPath(staffId, tree, currentDept) {
      for (const node of tree) {
        // 如果當前節點是部門（非葉子），更新 currentDept
        const newCurrentDept = (!node.isLeaf) ? node : currentDept
        
        // 找到目標員工節點
        if (node.id === staffId) {
          return newCurrentDept
        }
        
        // 遞歸在子節點中查找
        if (node.children) {
          const found = this.findDepartmentByPath(staffId, node.children, newCurrentDept)
          if (found) return found
        }
      }
      return null
    },

    handleClose() {
      if (this.$refs.staffTree) this.$refs.staffTree.setCheckedKeys([])
      if (this.$refs.customStaffTree) this.$refs.customStaffTree.setCheckedKeys([])
      this.selectedStaffIds = []
      this.$emit('update:visible', false)
    },

    handleConfirm() {
      this.$emit('confirm', this.selectedStaffWithDetails)
      this.handleClose()
    },

    handleTreeCheck() {
      const wecomCheckedNodes = this.$refs.staffTree ? this.$refs.staffTree.getCheckedNodes(true, false) : [];
      const customCheckedNodes = this.$refs.customStaffTree ? this.$refs.customStaffTree.getCheckedNodes(true, false) : [];
      
      this.selectedStaffIds = [
        ...wecomCheckedNodes.map(n => n.id),
        ...customCheckedNodes.map(n => n.id)
      ];
      
      this.$nextTick(() => {
        if (this.$refs.selectedContainer) {
          this.$refs.selectedContainer.scrollTop = this.$refs.selectedContainer.scrollHeight;
        }
      });
    },

    removeSelectedStaff(staff) {
      const internalId = staff.type === 2 ? -Math.abs(staff.id) : staff.id
      const index = this.selectedStaffIds.indexOf(internalId)
      if (index > -1) {
        this.selectedStaffIds.splice(index, 1)
        // 更新兩邊樹的勾選狀態
        this.$nextTick(() => {
          if (this.$refs.staffTree) {
            this.$refs.staffTree.setCheckedKeys(this.selectedStaffIds)
          }
          if (this.$refs.customStaffTree) {
            this.$refs.customStaffTree.setCheckedKeys(this.selectedStaffIds)
          }
        })
      }
    },

  }
}
</script>

<style scoped>
.department-icon { color: #409EFF; }
.position-icon { color: #E6A23C; }
.staff-icon { color: #67C23A; }
.folder-icon { color: #909399; }
</style>