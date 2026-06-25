<template>
  <el-dialog
    v-model="dialogVisible"
    title="選擇學校通訊錄"
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
          <div v-else-if="directoryTree.length === 0" class="empty">
            <el-icon><DocumentDelete /></el-icon>
            <span>暫無數據</span>
          </div>
          <el-tree
            v-else
            ref="treeRef"
            :data="directoryTree"
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
                <el-icon v-else class="node-icon department-icon"><User /></el-icon>
                <span class="node-label">{{ node.label }}</span>
                <span v-if="data.count" class="count-tag">({{ data.count }})</span>
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
            ref="customTreeRef"
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
                <el-icon v-else class="node-icon department-icon"><User /></el-icon>
                <span class="node-label">{{ node.label }}</span>
                <span v-if="data.count" class="count-tag">({{ data.count }})</span>
              </span>
            </template>
          </el-tree>
        </div>
      </div>

      <!-- 右側已選區域 -->
      <div class="right-panel">
        <div class="panel-title">
          <el-icon><Checked /></el-icon>
          <span>已選擇 ({{ selectedDirectoriesWithDetails.length }})</span>
        </div>

        <div class="selected-container" ref="selectedContainer">
          <div v-if="selectedDirectoriesWithDetails.length > 0" class="selected-list">
            <div
              v-for="dir in selectedDirectoriesWithDetails"
              :key="dir.id"
              class="selected-tag"
            >
              <span class="selected-tag-name">{{ dir.name }}</span>
              <el-button 
                link 
                type="danger" 
                size="small" 
                @click="removeSelectedDirectory(dir)"
                class="remove-btn"
              >
                <el-icon><CloseBold /></el-icon>
              </el-button>
            </div>
          </div>
          <div v-else class="empty-selected">
            <el-empty :image-size="80" description="請從左側選擇通訊錄" />
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
          :disabled="selectedDirectoriesWithDetails.length === 0"
        >
          確定 ({{ selectedDirectoriesWithDetails.length }})
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
  name: 'DirectorySelectorDialog',
  components: {
    Loading, DocumentDelete, School, OfficeBuilding, 
    Reading, Notebook, User, Checked, CloseBold, Menu
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
      activeTab: 'wecom',
      directoryTree: [],
      customTree: [],
      selectedDirectoryIds: [],
      loading: false,
      customLoading: false,
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
    },
    selectedDirectoriesWithDetails() {
      return this.selectedDirectoryIds.map(id => {
        let dir = this.findDirectoryInTree(id, this.directoryTree)
        if (dir) return { ...dir, type: 1 }
        
        dir = this.findDirectoryInTree(id, this.customTree)
        if (dir) return { ...dir, type: 2 }
        
        return { id, name: '未知通訊錄', type: 1 }
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
          url: '/wecomSchoolDepartment/tree',
          method: 'get'
        })
        if (response.code === 200 || response.code === 0) {
          this.directoryTree = response.data || []
        } else {
          ElNotification({ title: "操作失敗", message: '加載WeCom通訊錄數據失敗', type: "error", duration: 4000 })
        }
      } catch (error) {
        console.error('加載WeCom通訊錄數據失敗:', error)
        ElNotification({ title: "操作失敗", message: '加載WeCom通訊錄數據失敗', type: "error", duration: 4000 })
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
          params: { type: 1 }
        })
        if (response.code === 200 || response.code === 0) {
          this.customTree = response.data || []
        } else {
          ElNotification({ title: "操作失敗", message: '加載自定義通訊錄數據失敗', type: "error", duration: 4000 })
        }
      } catch (error) {
        console.error('加載自定義通訊錄數據失敗:', error)
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
      if (this.selectedDirectories?.length > 0) {
        this.selectedDirectoryIds = this.selectedDirectories.map(dir => dir.id)
      } else {
        this.selectedDirectoryIds = []
      }
      this.syncTreeCheckedKeys()
    },

    syncTreeCheckedKeys() {
      if (this.$refs.treeRef) {
        this.$refs.treeRef.setCheckedKeys(
          buildTreeCheckedKeys(this.$refs.treeRef, this.selectedDirectoryIds)
        )
      }
      if (this.$refs.customTreeRef) {
        this.$refs.customTreeRef.setCheckedKeys(
          buildTreeCheckedKeys(this.$refs.customTreeRef, this.selectedDirectoryIds)
        )
      }
    },

    applyCheckSelection(sourceTree, data, isChecked) {
      applyStrictTreeCheckSelection({
        sourceTree,
        data,
        isChecked,
        getSelectedIds: () => this.selectedDirectoryIds,
        setSelectedIds: (ids) => { this.selectedDirectoryIds = ids },
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

    findDirectoryInTree(id, tree) {
      return findNodeInTree(id, tree)
    },

    handleCheckChange(data, checkInfo) {
      const sourceTree = this.activeTab === 'wecom' ? this.$refs.treeRef : this.$refs.customTreeRef
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
    },

    removeSelectedDirectory(dir) {
      const index = this.selectedDirectoryIds.indexOf(dir.id)
      if (index > -1) {
        this.selectedDirectoryIds.splice(index, 1)
        this.syncTreeCheckedKeys()
      }
    },

    handleClose() {
      if (this.$refs.treeRef) this.$refs.treeRef.setCheckedKeys([])
      if (this.$refs.customTreeRef) this.$refs.customTreeRef.setCheckedKeys([])
      this.selectedDirectoryIds = []
      this.$emit('update:visible', false)
    },
    
    handleConfirm() {
      this.$emit('confirm', this.selectedDirectoriesWithDetails)
      this.handleClose()
    }
  }
}
</script>