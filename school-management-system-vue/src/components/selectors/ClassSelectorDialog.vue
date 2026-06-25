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
      <!-- 左側樹形結構 -->
      <div class="left-panel">
        <div class="panel-tabs">
          <el-tabs v-model="activeTab" class="custom-tabs">
            <el-tab-pane name="wecom">
              <template #label>
                <div class="tab-label"><el-icon><School /></el-icon> WeCom家校通訊錄</div>
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
            check-strictly
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
            check-strictly
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

      <!-- 右側已選區域 -->
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
import { ElNotification } from 'element-plus'
import { 
  Loading, DocumentDelete, School, OfficeBuilding, 
  Reading, Notebook, User, Checked, CloseBold, Menu 
} from '@element-plus/icons-vue'
import request from '@/utils/request'
import {
  buildTreeCheckedKeys,
  applyStrictTreeCheckSelection,
  findNodeInTree
} from '@/utils/strictTreeSelector'

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
          ElNotification({ title: "操作失敗", message: '加載WeCom班級數據失敗', type: "error", duration: 4000 })
        }
      } catch (error) {
        console.error('加載WeCom班級數據失敗:', error)
        ElNotification({ title: "操作失敗", message: '加載WeCom班級數據失敗', type: "error", duration: 4000 })
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
          ElNotification({ title: "操作失敗", message: '加載自定義班級數據失敗', type: "error", duration: 4000 })
        }
      } catch (error) {
        console.error('加載自定義班級數據失敗:', error)
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
      if (this.selectedClasses?.length > 0) {
        this.selectedClassIds = this.selectedClasses.map(cls => cls.id)
      } else {
        this.selectedClassIds = []
      }
      this.syncTreeCheckedKeys()
    },

    syncTreeCheckedKeys() {
      if (this.$refs.classTree) {
        this.$refs.classTree.setCheckedKeys(
          buildTreeCheckedKeys(this.$refs.classTree, this.selectedClassIds)
        )
      }
      if (this.$refs.customClassTree) {
        this.$refs.customClassTree.setCheckedKeys(
          buildTreeCheckedKeys(this.$refs.customClassTree, this.selectedClassIds)
        )
      }
    },

    applyCheckSelection(sourceTree, data, isChecked) {
      applyStrictTreeCheckSelection({
        sourceTree,
        data,
        isChecked,
        getSelectedIds: () => this.selectedClassIds,
        setSelectedIds: (ids) => { this.selectedClassIds = ids },
        syncTreeCheckedKeys: () => this.syncTreeCheckedKeys(),
        onAncestorBlock: () => {
          ElNotification({
            title: '提示',
            message: '已選中上級組織，請先取消上級節點',
            type: 'warning',
            duration: 3000
          })
        }
      })
    },

    findClassInTree(id, tree) {
      return findNodeInTree(id, tree)
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
        this.syncTreeCheckedKeys()
      }
    },

    handleCheckChange(data, checkInfo) {
      const sourceTree = this.activeTab === 'wecom' ? this.$refs.classTree : this.$refs.customClassTree
      if (!sourceTree) return

      const isChecked = checkInfo.checkedKeys.includes(data.id)
      this.applyCheckSelection(sourceTree, data, isChecked)

      if (isChecked) {
        this.$nextTick(() => {
          if (this.$refs.selectedContainer) {
            this.$refs.selectedContainer.scrollTop = this.$refs.selectedContainer.scrollHeight
          }
        })
      }
    }
  }
}
</script>

<style scoped>
.school-icon { color: #E6A23C; }
.campus-icon { color: #409EFF; }
.stage-icon { color: #67C23A; }
.grade-icon { color: #909399; }
.class-icon { color: #F56C6C; }
</style>
