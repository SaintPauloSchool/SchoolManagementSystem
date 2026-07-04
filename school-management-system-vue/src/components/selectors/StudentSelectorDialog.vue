<template>
  <el-dialog
    v-model="dialogVisible"
    title="選擇學生/家長"
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
                <span class="tab-label"><el-icon><School /></el-icon> WeCom家校通訊錄</span>
              </template>
            </el-tab-pane>
            <el-tab-pane name="custom">
              <template #label>
                <span class="tab-label"><el-icon><Menu /></el-icon> 自定義家校通訊錄</span>
              </template>
            </el-tab-pane>
          </el-tabs>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索學生或家長..."
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
            ref="classTree"
            :data="filteredWecomTree"
            :props="treeProps"
            :expand-on-click-node="false"
            :check-on-click-node="false"
            :check-strictly="true"
            node-key="treeKey"
            :default-expanded-keys="wecomExpandedKeys"
          >
            <template #default="{ node, data }">
              <StudentTreeNode
                v-if="data.isStudentGroup"
                :group="data"
                :type="1"
                :is-item-selected="isItemSelected"
                :is-group-all-selected="isGroupAllSelected"
                :is-group-indeterminate="isGroupIndeterminate"
                :get-relation-label="getRelationLabel"
                @toggle-group="toggleGroupAll"
                @toggle-parent="handleLeafNodeClick"
              />
              <span v-else class="tree-node dept-node" @click.stop>
                <el-checkbox
                  v-if="hasSelectableLeaves(data, 1)"
                  :model-value="isDeptAllSelected(data, 1)"
                  :indeterminate="isDeptIndeterminate(data, 1)"
                  @change="toggleDept(data, 1)"
                  class="dept-node-checkbox"
                />
                <el-icon v-if="data.type === 5" class="node-icon school-icon"><School /></el-icon>
                <el-icon v-else-if="data.type === 4" class="node-icon campus-icon"><OfficeBuilding /></el-icon>
                <el-icon v-else-if="data.type === 3" class="node-icon stage-icon"><Reading /></el-icon>
                <el-icon v-else-if="data.type === 2" class="node-icon grade-icon"><Notebook /></el-icon>
                <el-icon v-else-if="data.type === 1" class="node-icon class-icon"><User /></el-icon>
                <el-icon v-else class="node-icon folder-icon"><Folder /></el-icon>
                <span class="node-label">{{ node.label }}</span>
              </span>
            </template>
          </el-tree>
        </div>

        <div v-show="activeTab === 'custom'" class="tree-container">
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
            ref="customTreeRef"
            :data="filteredCustomTree"
            :props="treeProps"
            :expand-on-click-node="false"
            :check-on-click-node="false"
            :check-strictly="true"
            node-key="treeKey"
            :default-expanded-keys="customExpandedKeys"
          >
            <template #default="{ node, data }">
              <StudentTreeNode
                v-if="data.isStudentGroup"
                :group="data"
                :type="2"
                :is-item-selected="isItemSelected"
                :is-group-all-selected="isGroupAllSelected"
                :is-group-indeterminate="isGroupIndeterminate"
                :get-relation-label="getRelationLabel"
                @toggle-group="toggleGroupAll"
                @toggle-parent="handleLeafNodeClick"
              />
              <span v-else class="tree-node dept-node" @click.stop>
                <el-checkbox
                  v-if="hasSelectableLeaves(data, 2)"
                  :model-value="isDeptAllSelected(data, 2)"
                  :indeterminate="isDeptIndeterminate(data, 2)"
                  @change="toggleDept(data, 2)"
                  class="dept-node-checkbox"
                />
                <el-icon class="node-icon folder-icon"><Folder /></el-icon>
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
            <span class="count-badge">{{ selectedStudentsWithDetails.length }}</span>
          </div>
          <button
            v-if="selectedStudentsWithDetails.length > 0"
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
                  <el-icon v-if="group.deptType === 5" class="node-icon school-icon"><School /></el-icon>
                  <el-icon v-else-if="group.deptType === 4" class="node-icon campus-icon"><OfficeBuilding /></el-icon>
                  <el-icon v-else-if="group.deptType === 3" class="node-icon stage-icon"><Reading /></el-icon>
                  <el-icon v-else-if="group.deptType === 2" class="node-icon grade-icon"><Notebook /></el-icon>
                  <el-icon v-else-if="group.deptType === 1" class="node-icon class-icon"><User /></el-icon>
                  <el-icon v-else class="node-icon folder-icon"><Folder /></el-icon>
                </span>
                <span v-else class="student-avatar">{{ getNameInitial(group.studentName) }}</span>
                <span class="selected-name">{{ group.studentName }}</span>
                <span v-if="group.isDepartment" class="dept-member-count">{{ group.items.length }} 人</span>
                <button
                  v-if="group.isDepartment"
                  type="button"
                  class="remove-dept-btn"
                  title="移除該部門"
                  @click="removeGroup(group)"
                >
                  <el-icon><CloseBold /></el-icon>
                </button>
                <button
                  v-else-if="group.items.length > 1"
                  type="button"
                  class="remove-group-btn"
                  title="移除該學生全部家長"
                  @click="removeGroup(group)"
                >
                  全部移除
                </button>
              </div>
              <div v-if="!group.isDepartment" class="selected-tags">
                <button
                  v-for="item in group.items"
                  :key="`${item.type}-${item.parentUserId}-${item.departmentId || ''}`"
                  type="button"
                  class="selected-parent-tag"
                  @click="removeSelectedStudent(item)"
                >
                  <span class="selected-parent-tag-label">{{ getRelationLabel(item.name) }}</span>
                  <el-icon class="tag-close"><CloseBold /></el-icon>
                </button>
              </div>
            </div>
          </div>
          <div v-else class="empty-selected">
            <el-empty :image-size="72" description="請從左側選擇學生/家長" />
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer modern-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleConfirm">
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
  Reading, Notebook, User, Checked, CloseBold, Menu, Folder, Search
} from '@element-plus/icons-vue'
import request from '@/utils/request'
import StudentTreeNode from './StudentTreeNode.vue'

export default {
  name: 'StudentSelectorDialog',
  components: {
    Loading, DocumentDelete, School, OfficeBuilding,
    Reading, Notebook, User, Checked, CloseBold, Menu, Folder,
    StudentTreeNode
  },
  props: {
    visible: { type: Boolean, default: false },
    selectedStudents: { type: Array, default: () => [] }
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
      wecomExpandedKeys: [],
      customExpandedKeys: [],
      Search
    }
  },
  computed: {
    dialogVisible: {
      get() { return this.visible },
      set(value) { this.$emit('update:visible', value) }
    },
    treeProps() {
      return {
        children: 'children',
        label: 'name',
        isLeaf: (data) => data.isLeaf === true || data.isStudentGroup === true
      }
    },
    selectedStudentsWithDetails() {
      return this.selectedItems
    },
    filteredWecomTree() {
      return this.filterTree(this.departmentTree, this.searchKeyword)
    },
    filteredCustomTree() {
      return this.filterTree(this.customTree, this.searchKeyword)
    },
    groupedSelectedDisplay() {
      const deptGroups = new Map()
      const ungrouped = []

      this.selectedItems.forEach(item => {
        if (item.sourceDeptId != null) {
          const groupKey = `dept_${item.type}_${item.sourceDeptId}`
          if (!deptGroups.has(groupKey)) {
            deptGroups.set(groupKey, {
              key: groupKey,
              isDepartment: true,
              studentName: item.sourceDeptName || '部門',
              sourceDeptId: item.sourceDeptId,
              deptType: item.sourceDeptType ?? null,
              items: []
            })
          }
          deptGroups.get(groupKey).items.push(item)
        } else {
          ungrouped.push(item)
        }
      })

      const studentGroups = new Map()
      ungrouped.forEach(item => {
        const studentName = this.extractStudentName(item.name)
        const groupKey = `${item.type}_${item.departmentId || 'none'}_${studentName}`
        if (!studentGroups.has(groupKey)) {
          studentGroups.set(groupKey, { key: groupKey, studentName, items: [] })
        }
        studentGroups.get(groupKey).items.push(item)
      })

      return [
        ...Array.from(deptGroups.values()),
        ...Array.from(studentGroups.values()).map(group => ({
          ...group,
          relationText: group.items.map(i => this.getRelationLabel(i.name)).join('、')
        }))
      ]
    }
  },
  watch: {
    visible(newVal) {
      if (newVal) {
        this.searchKeyword = ''
        this.loadData()
        this.$nextTick(() => this.initSelectedTree())
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
          const tree = this.annotateTreeKeys(response.data || [])
          this.departmentTree = this.transformTreeForDisplay(tree)
          this.wecomExpandedKeys = this.collectRootKeys(this.departmentTree)
        }
        if (customResponse.code === 200 || customResponse.code === 0) {
          const tree = this.annotateTreeKeys(customResponse.data || [])
          this.customTree = this.transformTreeForDisplay(tree)
          this.customExpandedKeys = this.collectRootKeys(this.customTree)
        }
      } catch (error) {
        ElNotification({ title: '操作失敗', message: '載入學生通訊錄失敗', type: 'error', duration: 4000 })
      } finally {
        this.loading = false
        this.customLoading = false
        this.$nextTick(() => {
          if (this.visible) this.initSelectedTree()
        })
      }
    },

    collectRootKeys(nodes) {
      if (!Array.isArray(nodes)) return []
      return nodes.map(n => n?.treeKey).filter(Boolean)
    },

    isLeafNode(node) {
      if (!node) return false
      if (node.isLeaf === true) return true
      if (node.isStudentGroup) return false
      if (node.id != null && node.id < 0) return true
      return node.classDepartmentId != null
        && (!node.children || node.children.length === 0)
        && node.type == null
    },

    annotateTreeKeys(nodes) {
      if (!Array.isArray(nodes)) return []
      nodes.forEach(node => {
        if (!node) return
        if (this.isLeafNode(node)) {
          node.isLeaf = true
          const deptId = node.classDepartmentId != null ? node.classDepartmentId : 'none'
          node.treeKey = `leaf_${node.id}_${deptId}`
        } else {
          node.treeKey = `dept_${node.id}`
        }
        if (node.children?.length) {
          this.annotateTreeKeys(node.children)
        }
      })
      return nodes
    },

    transformTreeForDisplay(nodes) {
      if (!Array.isArray(nodes)) return []
      nodes.forEach(node => {
        if (!node?.children?.length) return
        const leafChildren = node.children.filter(child => this.isLeafNode(child))
        const branchChildren = node.children.filter(child => child && !this.isLeafNode(child))
        if (leafChildren.length > 0) {
          node.children = [...branchChildren, ...this.groupLeavesByStudent(leafChildren)]
        }
        this.transformTreeForDisplay(branchChildren)
      })
      return nodes
    },

    parseMemberName(name) {
      const text = (name || '').trim()
      if (!text) return { studentName: '', relation: '' }
      const dashIdx = text.indexOf('-')
      if (dashIdx > 0) {
        return {
          studentName: text.substring(0, dashIdx).trim(),
          relation: text.substring(dashIdx + 1).trim()
        }
      }
      const lastUnderscore = text.lastIndexOf('_')
      if (lastUnderscore > 0) {
        const relation = text.substring(lastUnderscore + 1).trim()
        const studentName = text.substring(0, lastUnderscore).trim()
        return { studentName, relation }
      }
      return { studentName: text, relation: text }
    },

    groupLeavesByStudent(leaves) {
      const groups = new Map()
      leaves.forEach(leaf => {
        const { studentName } = this.parseMemberName(leaf.name)
        const key = studentName || leaf.name || String(leaf.id)
        if (!groups.has(key)) groups.set(key, { studentName: key, parents: [] })
        groups.get(key).parents.push(leaf)
      })

      return Array.from(groups.values()).map(({ studentName, parents }) => {
        const classDeptId = parents[0].classDepartmentId != null ? parents[0].classDepartmentId : 'none'
        return {
          isStudentGroup: true,
          isLeaf: true,
          studentName,
          name: studentName,
          parents,
          classDepartmentId: parents[0].classDepartmentId,
          treeKey: `group_${classDeptId}_${studentName}`
        }
      })
    },

    extractStudentName(name) {
      return this.parseMemberName(name).studentName
    },

    getRelationLabel(name) {
      const { relation } = this.parseMemberName(name)
      return relation || name
    },

    filterTree(nodes, keyword) {
      if (!keyword?.trim()) return nodes
      const kw = keyword.trim().toLowerCase()
      const walk = (list) => {
        if (!Array.isArray(list)) return []
        const result = []
        for (const node of list) {
          if (!node) continue
          if (node.isStudentGroup) {
            const studentMatch = (node.studentName || '').toLowerCase().includes(kw)
            const parentMatch = (node.parents || []).some(p => (p.name || '').toLowerCase().includes(kw))
            if (studentMatch || parentMatch) result.push(node)
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

    initSelectedTree() {
      this.selectedItems = (this.selectedStudents || []).map(student => ({
        parentUserId: student.parentUserId,
        name: student.name || '',
        departmentId: student.departmentId || null,
        studentUserId: student.studentUserId,
        studentId: student.studentId,
        relationDesc: student.relationDesc,
        mobile: student.mobile,
        type: student.type === 2 ? 2 : 1,
        sourceDeptId: student.sourceDeptId ?? null,
        sourceDeptName: student.sourceDeptName ?? null,
        sourceDeptType: student.sourceDeptType ?? null
      }))
    },

    buildSelectedItem(data, type) {
      if (type === 1) {
        const name = data.name || ''
        const parsed = this.parseMemberName(name)
        return {
          parentUserId: data.parentUserId,
          studentUserId: data.studentUserId,
          studentId: data.studentId || '',
          name,
          studentName: parsed.studentName,
          parentName: parsed.relation,
          relationDesc: data.relationDesc || parsed.relation,
          mobile: data.mobile,
          departmentId: data.classDepartmentId || null,
          type: 1
        }
      }
      return {
        parentUserId: data.parentUserId || '',
        name: data.name || '',
        departmentId: data.classDepartmentId || null,
        studentId: data.studentId || '',
        type: 2
      }
    },

    itemKey(item) {
      return `${item.type}_${item.parentUserId}_${item.departmentId || ''}_${item.studentId || ''}`
    },

    collectSelectableLeaves(node, type) {
      const items = []
      const walk = (n) => {
        if (!n) return
        if (n.isStudentGroup) {
          ;(n.parents || []).forEach(parent => {
            items.push(this.buildSelectedItem(parent, type))
          })
          return
        }
        if (this.isLeafNode(n)) {
          items.push(this.buildSelectedItem(n, type))
          return
        }
        if (Array.isArray(n.children)) {
          n.children.forEach(walk)
        }
      }

      if (node.isStudentGroup || this.isLeafNode(node)) {
        walk(node)
      } else if (Array.isArray(node.children)) {
        node.children.forEach(walk)
      }
      return items
    },

    hasSelectableLeaves(node, type) {
      return this.collectSelectableLeaves(node, type).length > 0
    },

    isDeptAllSelected(dept, type) {
      const leaves = this.collectSelectableLeaves(dept, type)
      return leaves.length > 0 && leaves.every(item => this.findSelectedIndex(item) > -1)
    },

    isDeptIndeterminate(dept, type) {
      const leaves = this.collectSelectableLeaves(dept, type)
      if (leaves.length === 0) return false
      const selectedCount = leaves.filter(item => this.findSelectedIndex(item) > -1).length
      return selectedCount > 0 && selectedCount < leaves.length
    },

    toggleDept(dept, type) {
      const leaves = this.collectSelectableLeaves(dept, type)
      if (leaves.length === 0) return

      const selectAll = !this.isDeptAllSelected(dept, type)
      if (selectAll) {
        leaves.forEach(item => {
          const enriched = {
            ...item,
            sourceDeptId: dept.id,
            sourceDeptName: dept.name,
            sourceDeptType: dept.type ?? null
          }
          const index = this.findSelectedIndex(item)
          if (index === -1) {
            this.selectedItems.push(enriched)
          } else {
            this.selectedItems[index] = { ...this.selectedItems[index], ...enriched }
          }
        })
      } else {
        const removeKeys = new Set(leaves.map(leaf => this.itemKey(leaf)))
        this.selectedItems = this.selectedItems.filter(
          selected => !removeKeys.has(this.itemKey(selected))
        )
      }
      this.$nextTick(() => {
        this.$refs.selectedContainer?.scrollTo?.({
          top: this.$refs.selectedContainer.scrollHeight,
          behavior: 'smooth'
        })
      })
    },

    findSelectedIndex(item) {
      return this.selectedItems.findIndex(selected =>
        selected.parentUserId === item.parentUserId
        && selected.departmentId === item.departmentId
        && (selected.studentId || '') === (item.studentId || '')
        && selected.type === item.type
      )
    },

    isItemSelected(data, type) {
      return this.findSelectedIndex(this.buildSelectedItem(data, type)) > -1
    },

    isGroupAllSelected(group, type) {
      const parents = group.parents || []
      return parents.length > 0 && parents.every(p => this.isItemSelected(p, type))
    },

    isGroupIndeterminate(group, type) {
      const parents = group.parents || []
      const n = parents.filter(p => this.isItemSelected(p, type)).length
      return n > 0 && n < parents.length
    },

    toggleGroupAll(group, type) {
      const selectAll = !this.isGroupAllSelected(group, type)
      ;(group.parents || []).forEach(parent => {
        const item = this.buildSelectedItem(parent, type)
        const index = this.findSelectedIndex(item)
        if (selectAll && index === -1) this.selectedItems.push(item)
        else if (!selectAll && index > -1) this.selectedItems.splice(index, 1)
      })
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
      if (!data || !data.parentUserId) return
      const item = this.buildSelectedItem(data, type)
      const index = this.findSelectedIndex(item)
      if (index > -1) {
        this.selectedItems.splice(index, 1)
      } else {
        this.selectedItems.push(item)
        this.$nextTick(() => {
          this.$refs.selectedContainer?.scrollTo?.({ top: this.$refs.selectedContainer.scrollHeight, behavior: 'smooth' })
        })
      }
    },

    removeGroup(group) {
      if (group.isDepartment) {
        this.selectedItems = this.selectedItems.filter(
          item => item.sourceDeptId !== group.sourceDeptId
        )
        return
      }
      group.items.forEach(item => this.removeSelectedStudent(item))
    },

    clearAllSelected() {
      this.selectedItems = []
    },

    getNameInitial(name) {
      const text = (name || '').trim()
      return text ? text.charAt(0) : '?'
    },

    removeSelectedStudent(student) {
      const index = this.findSelectedIndex(student)
      if (index > -1) this.selectedItems.splice(index, 1)
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

.clear-all-btn:active {
  background: #bfdbfe;
}

.clear-all-btn:focus {
  outline: none;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2);
}

.clear-all-btn:focus:not(:focus-visible) {
  box-shadow: none;
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

.tree-container:hover {
  border-color: transparent;
  box-shadow: none;
}

.dept-node {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.dept-node-checkbox {
  margin: 0;
  height: 14px;
  --el-checkbox-height: 14px;
  flex-shrink: 0;
}

.dept-node .node-label {
  font-weight: 500;
  color: #374151;
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

.folder-icon {
  color: #909399;
}

.dept-member-count {
  font-size: 12px;
  color: #6b7280;
  flex-shrink: 0;
}

.selected-container {
  flex: 1;
  border: none;
  border-radius: 0;
  padding: 14px 16px 18px;
  background: #f9fafb;
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
  margin-bottom: 10px;
}

.student-avatar {
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

.remove-group-btn {
  border: none;
  background: transparent;
  padding: 0;
  font-size: 12px;
  color: #9ca3af;
  cursor: pointer;
  flex-shrink: 0;
  transition: color 0.15s;
}

.selected-card-dept {
  padding: 10px 12px;
}

.selected-card-top-only {
  margin-bottom: 0;
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

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-left: 38px;
}

.selected-parent-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 5px 10px 5px 12px;
  min-height: 28px;
  box-sizing: border-box;
  border: 1px solid #bfdbfe;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 13px;
  line-height: 1;
  cursor: pointer;
  transition: all 0.15s;
}

.selected-parent-tag-label {
  display: inline-flex;
  align-items: center;
  line-height: 1;
  padding-top: 1px;
}

.selected-parent-tag:hover {
  background: #dbeafe;
  border-color: #93c5fd;
}

.selected-parent-tag .tag-close {
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  color: #60a5fa;
}

.selected-parent-tag:hover .tag-close {
  color: #2563eb;
}

.school-icon { color: #E6A23C; }
.campus-icon { color: #409EFF; }
.stage-icon { color: #67C23A; }
.grade-icon { color: #909399; }
.class-icon { color: #F56C6C; }
</style>
