<template>
  <el-dialog
    v-model="dialogVisible"
    :title="title"
    width="1180px"
    :before-close="handleClose"
    class="student-selector-dialog student-selector-modern"
    top="4vh"
    destroy-on-close
  >
    <div class="selector-wrapper modern-selector">
      <div class="selector-panel left-panel">
        <div class="panel-head">
          <el-tabs v-model="activeTab" class="source-tabs">
            <el-tab-pane name="wecom">
              <template #label>
                <span class="tab-label"><el-icon><School /></el-icon> WeCom老師通訊錄</span>
              </template>
            </el-tab-pane>
            <el-tab-pane v-if="!wecomOnly" name="custom">
              <template #label>
                <span class="tab-label"><el-icon><Menu /></el-icon> 自定義老師通訊錄</span>
              </template>
            </el-tab-pane>
          </el-tabs>
          <el-input
            v-model="searchKeyword"
            placeholder="搜尋教職員工..."
            clearable
            class="tree-search"
            :prefix-icon="Search"
          />
        </div>

        <div v-show="activeTab === 'wecom'" class="tree-container">
          <div v-if="loading" class="loading">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>加載中...</span>
          </div>
          <div v-else-if="filteredWecomTree.length === 0" class="empty">
            <el-icon><DocumentDelete /></el-icon>
            <span>{{ searchKeyword ? '未找到匹配結果' : '暫無數據' }}</span>
          </div>
          <el-tree
            v-else
            ref="staffTree"
            :data="filteredWecomTree"
            :props="treeProps"
            :expand-on-click-node="false"
            :check-on-click-node="false"
            show-checkbox
            node-key="treeKey"
            @check="handleTreeCheck"
          >
            <template #default="{ node, data }">
              <span class="tree-node dept-node">
                <el-icon v-if="data.type === 20" class="node-icon department-icon"><OfficeBuilding /></el-icon>
                <el-icon v-else-if="data.type === 10" class="node-icon position-icon"><UserFilled /></el-icon>
                <el-icon v-else-if="data.isLeaf" class="node-icon staff-icon"><User /></el-icon>
                <el-icon v-else class="node-icon folder-icon"><Folder /></el-icon>
                <span class="node-label">{{ node.label }}</span>
              </span>
            </template>
          </el-tree>
        </div>

        <div v-show="!wecomOnly && activeTab === 'custom'" class="tree-container">
          <div v-if="customLoading" class="loading">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>加載中...</span>
          </div>
          <div v-else-if="filteredCustomTree.length === 0" class="empty">
            <el-icon><DocumentDelete /></el-icon>
            <span>{{ searchKeyword ? '未找到匹配結果' : '暫無數據' }}</span>
          </div>
          <el-tree
            v-else
            ref="customStaffTree"
            :data="filteredCustomTree"
            :props="treeProps"
            :expand-on-click-node="false"
            :check-on-click-node="false"
            show-checkbox
            node-key="treeKey"
            @check="handleTreeCheck"
          >
            <template #default="{ node, data }">
              <span class="tree-node dept-node">
                <el-icon v-if="data.isLeaf" class="node-icon staff-icon"><User /></el-icon>
                <el-icon v-else class="node-icon folder-icon"><Folder /></el-icon>
                <span class="node-label">{{ node.label }}</span>
              </span>
            </template>
          </el-tree>
        </div>
      </div>

      <div class="selector-panel right-panel">
        <div class="panel-head panel-head-right">
          <div class="panel-head-title">
            <el-icon><Checked /></el-icon>
            <span>已選擇</span>
            <span class="count-badge">{{ selectedItems.length }}</span>
          </div>
          <button
            v-if="selectedItems.length > 0"
            type="button"
            class="clear-all-btn"
            @click="clearAllSelected"
          >
            清空
          </button>
        </div>
        <div class="selected-container" ref="selectedContainer">
          <div v-if="groupedSelectedDisplay.length > 0" class="selected-list">
            <div
              v-for="group in groupedSelectedDisplay"
              :key="group.key"
              class="selected-card"
              :class="{ 'selected-card-dept': group.isDepartment }"
            >
              <div class="selected-card-top" :class="{ 'selected-card-top-only': group.isDepartment }">
                <span v-if="group.isDepartment" class="selected-dept-icon">
                  <el-icon v-if="group.deptType === 20" class="node-icon department-icon"><OfficeBuilding /></el-icon>
                  <el-icon v-else-if="group.deptType === 10" class="node-icon position-icon"><UserFilled /></el-icon>
                  <el-icon v-else class="node-icon folder-icon"><Folder /></el-icon>
                </span>
                <span v-else class="staff-avatar">{{ getNameInitial(group.name) }}</span>
                <span class="selected-name">{{ group.name }}</span>
                <span v-if="group.isDepartment" class="dept-member-count">{{ group.items.length }} 人</span>
                <button
                  type="button"
                  class="remove-dept-btn"
                  title="移除"
                  @click="removeGroup(group)"
                >
                  <el-icon><CloseBold /></el-icon>
                </button>
              </div>
            </div>
          </div>
          <div v-else class="empty-selected">
            <el-empty :image-size="72" description="請從左側選擇教職員工" />
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer modern-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleConfirm">
          確定 ({{ selectedItems.length }})
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
import { ElNotification } from 'element-plus'
import {
  Loading, DocumentDelete, School, OfficeBuilding,
  User, UserFilled, Checked, CloseBold, Menu, Folder, Search
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
    },
    title: {
      type: String,
      default: '選擇教職員工'
    },
    /** 僅顯示 WeCom 老師通訊錄（隱藏自定義通訊錄） */
    wecomOnly: {
      type: Boolean,
      default: false
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
      customLoading: false,
      searchKeyword: '',
      Search
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
        isLeaf: (data) => data.isLeaf === true
      }
    },
    filteredWecomTree() {
      return this.filterTree(this.departmentTree, this.searchKeyword)
    },
    filteredCustomTree() {
      return this.filterTree(this.customTree, this.searchKeyword)
    },
    selectedStaffWithDetails() {
      return this.selectedItems
    },
    groupedSelectedDisplay() {
      const deptGroups = new Map()
      const individuals = []

      this.selectedItems.forEach(item => {
        if (item.sourceDeptId != null) {
          const key = `dept_${item.type}_${item.sourceDeptId}`
          if (!deptGroups.has(key)) {
            deptGroups.set(key, {
              key,
              isDepartment: true,
              name: item.sourceDeptName || '部門',
              sourceDeptId: item.sourceDeptId,
              deptType: item.sourceDeptType ?? null,
              type: item.type,
              items: []
            })
          }
          deptGroups.get(key).items.push(item)
        } else {
          individuals.push({
            key: `person_${item.type}_${item.id}`,
            isDepartment: false,
            name: item.name || '',
            type: item.type,
            item
          })
        }
      })

      return [...Array.from(deptGroups.values()), ...individuals]
    }
  },
  watch: {
    visible(newVal) {
      if (newVal) {
        this.searchKeyword = ''
        this.activeTab = 'wecom'
        this.loadData()
        if (!this.wecomOnly) {
          this.loadCustomData()
        }
        this.$nextTick(() => {
          this.initSelectedTree()
        })
      }
    }
  },
  methods: {
    getNameInitial(name) {
      const text = (name || '').trim()
      return text ? text.charAt(0) : '?'
    },

    annotateTreeKeys(nodes, type = 1) {
      if (!Array.isArray(nodes)) {
        return []
      }
      nodes.forEach(node => {
        if (!node) {
          return
        }
        if (node.isLeaf) {
          const deptId = node.classDepartmentId != null
            ? node.classDepartmentId
            : (node.parentId != null ? node.parentId : 'none')
          node.treeKey = `leaf_${type}_${node.id}_${deptId}`
        } else {
          node.treeKey = `dept_${type}_${node.id}`
        }
        if (node.children?.length) {
          this.annotateTreeKeys(node.children, type)
        }
      })
      return nodes
    },

    filterTree(nodes, keyword) {
      if (!keyword?.trim()) {
        return nodes
      }
      const kw = keyword.trim().toLowerCase()
      const walk = (list) => {
        if (!Array.isArray(list)) {
          return []
        }
        const result = []
        for (const node of list) {
          if (!node) {
            continue
          }
          const nameMatch = (node.name || '').toLowerCase().includes(kw)
          const filteredChildren = walk(node.children)
          if (nameMatch) {
            result.push(node)
          } else if (filteredChildren.length > 0) {
            result.push({ ...node, children: filteredChildren })
          }
        }
        return result
      }
      return walk(nodes)
    },

    async loadData() {
      this.loading = true
      try {
        const response = await request({
          url: '/wecomSchoolDepartment/treeWithMembers',
          method: 'get'
        })
        if (response.code === 200 || response.code === 0) {
          this.departmentTree = this.annotateTreeKeys(response.data || [], 1)
        } else {
          ElNotification({ title: '操作失敗', message: '加載WeCom教職員工數據失敗', type: 'error', duration: 4000 })
        }
      } catch (error) {
        console.error('加載WeCom教職員工數據失敗:', error)
        ElNotification({ title: '操作失敗', message: '加載WeCom教職員工數據失敗', type: 'error', duration: 4000 })
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
          this.customTree = this.annotateTreeKeys(response.data || [], 2)
        } else {
          ElNotification({ title: '操作失敗', message: '加載自定義教職員工數據失敗', type: 'error', duration: 4000 })
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
      this.selectedItems = (this.selectedStaff || []).map(staff => ({
        id: staff.id,
        name: staff.name || '',
        position: staff.position || '',
        type: staff.type === 2 ? 2 : 1,
        staffUserId: staff.staffUserId || staff.userid || null,
        sourceDeptId: staff.sourceDeptId ?? null,
        sourceDeptName: staff.sourceDeptName ?? null,
        sourceDeptType: staff.sourceDeptType ?? null
      }))
      this.syncTreeCheckboxes()
    },

    toInternalId(item) {
      return item.type === 2 ? -Math.abs(item.id) : item.id
    },

    fromLeafNode(node, type, sourceDept = null) {
      const item = {
        id: type === 2 ? Math.abs(node.id) : node.id,
        name: node.name,
        position: node.position || '',
        type,
        // WeCom userid（寫入 sys_user_role.user_id）；自定義通訊錄可能為空
        staffUserId: node.staffUserId || node.userid || null
      }
      if (sourceDept) {
        item.sourceDeptId = sourceDept.id
        item.sourceDeptName = sourceDept.name
        item.sourceDeptType = sourceDept.type ?? null
      }
      return item
    },

    collectLeafIds(node) {
      if (!node) {
        return []
      }
      if (node.isLeaf) {
        return [node.id]
      }
      const ids = []
      for (const child of node.children || []) {
        ids.push(...this.collectLeafIds(child))
      }
      return ids
    },

    collectDepartmentsWithDepth(nodes, depth = 0, result = []) {
      for (const node of nodes || []) {
        if (!node.isLeaf) {
          result.push({ node, depth })
          this.collectDepartmentsWithDepth(node.children, depth + 1, result)
        }
      }
      return result
    },

    rebuildSelectedItems() {
      const items = []

      const processTree = (treeRef, treeData, type) => {
        if (!treeRef || !Array.isArray(treeData) || treeData.length === 0) {
          return
        }
        const checkedLeaves = treeRef.getCheckedNodes(true, false)
        const checkedSet = new Set(checkedLeaves.map(node => node.id))
        const assigned = new Set()
        const depts = this.collectDepartmentsWithDepth(treeData)
        depts.sort((a, b) => b.depth - a.depth)

        for (const { node: dept } of depts) {
          const leafIds = this.collectLeafIds(dept)
          if (leafIds.length === 0) {
            continue
          }
          const allChecked = leafIds.every(id => checkedSet.has(id))
          if (!allChecked) {
            continue
          }
          for (const leafId of leafIds) {
            if (!checkedSet.has(leafId) || assigned.has(leafId)) {
              continue
            }
            const staffNode = this.findStaffInTree(leafId, treeData)
            if (!staffNode) {
              continue
            }
            assigned.add(leafId)
            items.push(this.fromLeafNode(staffNode, type, dept))
          }
        }

        for (const leaf of checkedLeaves) {
          if (assigned.has(leaf.id)) {
            continue
          }
          items.push(this.fromLeafNode(leaf, type))
        }
      }

      processTree(this.$refs.staffTree, this.departmentTree, 1)
      processTree(this.$refs.customStaffTree, this.customTree, 2)
      this.selectedItems = items
    },

    syncTreeCheckboxes() {
      const collectTreeKeys = (items, tree) => items
        .map(item => this.findStaffInTree(this.toInternalId(item), tree)?.treeKey)
        .filter(Boolean)

      const wecomKeys = collectTreeKeys(
        this.selectedItems.filter(item => item.type === 1),
        this.departmentTree
      )
      const customKeys = collectTreeKeys(
        this.selectedItems.filter(item => item.type === 2),
        this.customTree
      )

      this.$nextTick(() => {
        if (this.$refs.staffTree) {
          this.$refs.staffTree.setCheckedKeys(wecomKeys)
        }
        if (this.$refs.customStaffTree) {
          this.$refs.customStaffTree.setCheckedKeys(customKeys)
        }
      })
    },

    findStaffInTree(id, tree) {
      for (const node of tree) {
        if (node.isLeaf && node.id === id) {
          return node
        }
        if (node.children) {
          const found = this.findStaffInTree(id, node.children)
          if (found) {
            return found
          }
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
          if (found) {
            return found
          }
        }
      }
      return null
    },

    findNearestDepartment(staffNode, tree) {
      if (staffNode.parentId) {
        const deptByParentId = this.findDepartmentByParentId(staffNode.parentId, tree)
        if (deptByParentId && !deptByParentId.isLeaf) {
          return deptByParentId
        }
      }
      return this.findDepartmentByPath(staffNode.id, tree, null)
    },

    findDepartmentByPath(staffId, tree, currentDept) {
      for (const node of tree) {
        const newCurrentDept = (!node.isLeaf) ? node : currentDept
        if (node.id === staffId) {
          return newCurrentDept
        }
        if (node.children) {
          const found = this.findDepartmentByPath(staffId, node.children, newCurrentDept)
          if (found) {
            return found
          }
        }
      }
      return null
    },

    handleClose() {
      if (this.$refs.staffTree) {
        this.$refs.staffTree.setCheckedKeys([])
      }
      if (this.$refs.customStaffTree) {
        this.$refs.customStaffTree.setCheckedKeys([])
      }
      this.selectedItems = []
      this.$emit('update:visible', false)
    },

    handleConfirm() {
      this.$emit('confirm', this.selectedStaffWithDetails)
      this.handleClose()
    },

    handleTreeCheck() {
      this.rebuildSelectedItems()
      this.$nextTick(() => {
        if (this.$refs.selectedContainer) {
          this.$refs.selectedContainer.scrollTop = this.$refs.selectedContainer.scrollHeight
        }
      })
    },

    clearAllSelected() {
      this.selectedItems = []
      if (this.$refs.staffTree) {
        this.$refs.staffTree.setCheckedKeys([])
      }
      if (this.$refs.customStaffTree) {
        this.$refs.customStaffTree.setCheckedKeys([])
      }
    },

    removeGroup(group) {
      if (group.isDepartment) {
        this.selectedItems = this.selectedItems.filter(
          item => !(item.sourceDeptId === group.sourceDeptId && item.type === group.type)
        )
      } else {
        const staff = group.item
        this.selectedItems = this.selectedItems.filter(
          item => !(item.id === staff.id && item.type === staff.type && item.sourceDeptId == null)
        )
      }
      this.syncTreeCheckboxes()
    }
  }
}
</script>

<style scoped>
.modern-selector {
  gap: 24px;
  padding: 0;
  background: transparent;
  height: 640px;
}

.selector-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  border: 1px solid #e8ecf1;
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.06);
}

.left-panel {
  flex: 1.55;
}

.panel-head {
  padding: 16px 18px 12px;
  border-bottom: 1px solid #eef1f5;
  background: #fafbfc;
}

.panel-head-right {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-weight: 600;
  font-size: 14px;
  color: #1f2937;
  padding: 16px 18px;
}

.panel-head-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-head-right .el-icon {
  color: #3b82f6;
}

.clear-all-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 28px;
  padding: 0 12px;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  background: #eff6ff;
  font-size: 13px;
  font-weight: 500;
  color: #2563eb;
  cursor: pointer;
  box-shadow: none;
  flex-shrink: 0;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}

.clear-all-btn:hover {
  background: #dbeafe;
  border-color: #93c5fd;
  color: #1d4ed8;
}

.count-badge {
  min-width: 22px;
  height: 22px;
  padding: 0 7px;
  border-radius: 11px;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.source-tabs :deep(.el-tabs__header) {
  margin: 0 0 14px;
}

.source-tabs :deep(.el-tabs__item) {
  height: 36px;
  line-height: 36px;
  font-size: 13px;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tree-search :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

.tree-container {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px 16px;
  border: none;
  border-radius: 0;
  margin-top: 0;
  background: #fff;
}

.dept-node {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.dept-node .node-label {
  font-weight: 500;
  color: #374151;
}

.selected-container {
  flex: 1;
  border: none;
  border-radius: 0;
  padding: 14px 16px 18px;
  background: #f9fafb;
  overflow-y: auto;
}

.selected-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.selected-card {
  padding: 12px 14px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.selected-card:hover {
  border-color: #d1d5db;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
}

.selected-card-top {
  display: flex;
  align-items: center;
  gap: 10px;
}

.selected-card-top-only {
  margin-bottom: 0;
}

.staff-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #dbeafe 0%, #e0e7ff 100%);
  color: #3730a3;
  font-size: 13px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.selected-name {
  flex: 1;
  min-width: 0;
  font-weight: 600;
  font-size: 14px;
  color: #111827;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selected-dept-icon {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.selected-dept-icon .node-icon {
  font-size: 18px;
}

.dept-member-count {
  font-size: 12px;
  color: #6b7280;
  flex-shrink: 0;
}

.selected-card-dept {
  padding: 10px 12px;
}

.remove-dept-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: #9ca3af;
  cursor: pointer;
  flex-shrink: 0;
  transition: color 0.15s, background 0.15s;
}

.remove-dept-btn:hover {
  color: #ef4444;
  background: #fef2f2;
}

.department-icon { color: #409EFF; }
.position-icon { color: #E6A23C; }
.staff-icon { color: #67C23A; }
.folder-icon { color: #909399; }
</style>
