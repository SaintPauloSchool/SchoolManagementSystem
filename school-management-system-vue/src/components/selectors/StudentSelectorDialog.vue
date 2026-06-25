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
      <div class="left-panel">
        <el-tabs v-model="activeTab" class="custom-tabs">
          <!-- WeCom 家校通訊錄 -->
          <el-tab-pane name="wecom">
            <template #label>
              <div class="tab-label"><el-icon><School /></el-icon> WeCom家校通訊錄</div>
            </template>
          </el-tab-pane>
          
          <!-- 自定義家校通訊錄 -->
          <el-tab-pane name="custom">
            <template #label>
              <div class="tab-label"><el-icon><Menu /></el-icon> 自定義家校通訊錄</div>
            </template>
          </el-tab-pane>
        </el-tabs>

        <!-- WeCom Tree -->
        <div v-show="activeTab === 'wecom'" class="tree-container">
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
            node-key="treeKey"
          >
            <template #default="{ node, data }">
              <span class="tree-node">
                <el-checkbox
                  v-if="data.isLeaf"
                  :model-value="isItemSelected(data, 1)"
                  @change="() => handleLeafNodeClick(data, 1)"
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

        <!-- Custom Tree -->
        <div v-show="activeTab === 'custom'" class="tree-container">
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
            ref="customTreeRef"
            :data="customTree"
            :props="treeProps"
            :expand-on-click-node="false"
            :check-on-click-node="false"
            :check-strictly="true"
            node-key="treeKey"
          >
            <template #default="{ node, data }">
              <span class="tree-node">
                <el-checkbox
                  v-if="data.isLeaf"
                  :model-value="isItemSelected(data, 2)"
                  @change="() => handleLeafNodeClick(data, 2)"
                  class="node-checkbox"
                />
                <el-icon v-if="data.isLeaf" class="node-icon relation-icon"><User /></el-icon>
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
          <span>已選擇 ({{ selectedStudentsWithDetails.length }})</span>
        </div>

        <div class="selected-container" ref="selectedContainer">
          <div v-if="selectedStudentsWithDetails.length > 0" class="selected-list">
            <div
              v-for="student in selectedStudentsWithDetails"
              :key="`${student.type}-${student.id}-${student.departmentId}`"
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
            <el-empty :image-size="80" description="請從左側選擇學生/家長" />
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
import { ElNotification } from 'element-plus'
import { 
  Loading, DocumentDelete, School, OfficeBuilding, 
  Reading, Notebook, User, UserFilled, Checked, CloseBold, Menu, Folder 
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
      activeTab: 'wecom',
      departmentTree: [],
      customTree: [],
      selectedItems: [],
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
    selectedStudentsWithDetails() {
      return this.selectedItems
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
      this.customLoading = true
      try {
        const [response, customResponse] = await Promise.all([
           request({ url: '/system/department/treeWithParents', method: 'get' }),
           request({ url: '/system/schoolDepartment/treeWithMembers?type=2', method: 'get' })
        ])
        if (response.code === 200 || response.code === 0) {
          this.departmentTree = this.annotateTreeKeys(response.data || [])
        }
        if (customResponse.code === 200 || customResponse.code === 0) {
          this.customTree = this.annotateTreeKeys(customResponse.data || [])
        }
      } catch (error) {
        ElNotification({ title: "操作失敗", message: '載入學生通訊錄失敗', type: "error", duration: 4000 })
      } finally {
        this.loading = false
        this.customLoading = false
        this.$nextTick(() => {
          if (this.visible) {
            this.initSelectedTree()
          }
        })
      }
    },

    annotateTreeKeys(nodes) {
      if (!Array.isArray(nodes)) {
        return []
      }
      nodes.forEach(node => {
        if (!node) return
        if (node.isLeaf) {
          const deptId = node.classDepartmentId != null ? node.classDepartmentId : 'none'
          node.treeKey = `leaf_${node.id}_${deptId}`
        } else {
          node.treeKey = `dept_${node.id}`
        }
        if (node.children && node.children.length > 0) {
          this.annotateTreeKeys(node.children)
        }
      })
      return nodes
    },

    initSelectedTree() {
      this.selectedItems = (this.selectedStudents || []).map(student => ({
        id: student.id,
        name: student.name || '',
        departmentId: student.departmentId || null,
        studentUserId: student.studentUserId,
        parentUserId: student.parentUserId,
        relationDesc: student.relationDesc,
        mobile: student.mobile,
        type: student.type === 2 ? 2 : 1
      }))
    },

    buildSelectedItem(data, type) {
      if (type === 1) {
        const name = data.name || ''
        const nameParts = name.split('-')
        return {
          id: data.id,
          studentUserId: data.studentUserId,
          parentUserId: data.parentUserId,
          name,
          studentName: nameParts[0] || '',
          parentName: nameParts[1] || '',
          relationDesc: data.relationDesc,
          mobile: data.mobile,
          departmentId: data.classDepartmentId || null,
          type: 1
        }
      }
      return {
        id: Math.abs(data.id),
        name: data.name || '',
        departmentId: data.classDepartmentId || null,
        type: 2
      }
    },

    findSelectedIndex(item) {
      return this.selectedItems.findIndex(selected =>
        selected.id === item.id
        && selected.departmentId === item.departmentId
        && selected.type === item.type
      )
    },

    isItemSelected(data, type) {
      return this.findSelectedIndex(this.buildSelectedItem(data, type)) > -1
    },

    handleClose() {
      this.selectedItems = []
      this.$emit('update:visible', false)
    },

    handleConfirm() {
      this.$emit('confirm', this.selectedStudentsWithDetails)
      this.handleClose()
    },

    handleLeafNodeClick(data, type) {
      if (!data || data.id == null) return
      const item = this.buildSelectedItem(data, type)
      const index = this.findSelectedIndex(item)
      if (index > -1) {
        this.selectedItems.splice(index, 1)
      } else {
        this.selectedItems.push(item)
        this.$nextTick(() => {
          if (this.$refs.selectedContainer) {
            this.$refs.selectedContainer.scrollTop = this.$refs.selectedContainer.scrollHeight
          }
        })
      }
    },

    removeSelectedStudent(student) {
      const index = this.findSelectedIndex(student)
      if (index > -1) {
        this.selectedItems.splice(index, 1)
      }
    },

  }
}
</script>

<style scoped>
.node-checkbox {
  margin-right: 4px;
}

.school-icon { color: #E6A23C; }
.campus-icon { color: #409EFF; }
.stage-icon { color: #67C23A; }
.grade-icon { color: #909399; }
.class-icon { color: #F56C6C; }
.parent-icon { color: #67C23A; }
.relation-icon { color: #E6A23C; }
</style>