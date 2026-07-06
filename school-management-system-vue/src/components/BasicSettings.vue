<template>
  <div class="basic-settings-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">
            <el-icon><Setting /></el-icon>
            基本設置
          </span>
        </div>
      </template>

      <div class="settings-tabs">
        <button
          type="button"
          class="settings-tab"
          :class="{ active: activeTab === 'wecom' }"
          @click="activeTab = 'wecom'"
        >
          <span class="settings-tab-icon wecom">
            <el-icon><Notebook /></el-icon>
          </span>
          <span class="settings-tab-text">
            <span class="settings-tab-title">家校通訊錄學段</span>
            <span class="settings-tab-desc">指定家校通訊錄使用的學段</span>
          </span>
        </button>
        <button
          type="button"
          class="settings-tab"
          :class="{ active: activeTab === 'dailyNotice' }"
          @click="activeTab = 'dailyNotice'"
        >
          <span class="settings-tab-icon notice">
            <el-icon><Bell /></el-icon>
          </span>
          <span class="settings-tab-text">
            <span class="settings-tab-title">每日學生手冊通知</span>
            <span class="settings-tab-desc">指定每日學生手冊通知發送的班級範圍</span>
          </span>
        </button>
      </div>

      <div v-show="activeTab === 'wecom'" class="tab-panel">
          <div class="settings-layout">
            <div class="tree-panel" v-loading="segmentLoading">
              <el-tree
                ref="segmentTreeRef"
                :data="segmentTree"
                :props="treeProps"
                node-key="id"
              >
                <template #default="{ data }">
                  <span class="tree-node">
                    <el-checkbox
                      v-if="Number(data.type) === 3"
                      :model-value="segmentCheckedIds.includes(data.id)"
                      class="tree-node-checkbox"
                      @click.stop
                      @change="checked => handleSegmentCheckboxChange(data, checked)"
                    />
                    <span>{{ data.name }}</span>
                    <el-tag v-if="data.type === 3" size="small" type="success" class="type-tag">學段</el-tag>
                    <el-tag v-else-if="data.type === 5" size="small" class="type-tag">學校</el-tag>
                  </span>
                </template>
              </el-tree>
            </div>

            <div class="saved-panel">
              <div class="saved-panel-title">目前已保存</div>
              <div v-if="!savedSegmentInfo" class="saved-empty">尚未保存學段配置</div>
              <div v-else class="saved-content">
                <div class="saved-item">
                  <span class="saved-label">學段名稱</span>
                  <span class="saved-value">{{ savedSegmentInfo.name }}</span>
                </div>
                <div v-if="savedSegmentInfo.path" class="saved-path">{{ savedSegmentInfo.path }}</div>
              </div>
              <div
                v-if="savedSegmentInfo && segmentSelectionChanged"
                class="saved-pending-tip"
              >
                左側選擇已變更，請點擊「保存配置」後才會生效
              </div>
            </div>
          </div>

          <div class="action-bar">
            <el-button type="primary" :loading="segmentSaving" @click="handleSaveSegment">保存配置</el-button>
            <el-button @click="loadSegmentData">重新載入</el-button>
          </div>
      </div>

      <div v-show="activeTab === 'dailyNotice'" class="tab-panel">
          <div class="settings-layout">
            <div class="tree-panel" v-loading="dailyNoticeLoading">
              <el-tree
                ref="dailyNoticeTreeRef"
                :data="dailyNoticeTree"
                :props="treeProps"
                node-key="id"
              >
                <template #default="{ data }">
                  <span class="tree-node">
                    <el-checkbox
                      v-if="Number(data.type) === 1"
                      :model-value="dailyNoticeCheckedSet.has(data.id)"
                      class="tree-node-checkbox"
                      @click.stop
                      @change="checked => handleDailyNoticeCheckboxChange(data, checked)"
                    />
                    <span>{{ data.name }}</span>
                    <el-tag v-if="data.type === 1" size="small" type="warning" class="type-tag">班級</el-tag>
                    <el-tag v-else-if="data.type === 2" size="small" type="info" class="type-tag">年級</el-tag>
                    <el-tag v-else-if="data.type === 3" size="small" type="success" class="type-tag">學段</el-tag>
                    <span
                      v-if="data.type !== 1 && getDailyNoticeSelectedLabel(data)"
                      class="selected-count"
                    >{{ getDailyNoticeSelectedLabel(data) }}</span>
                  </span>
                </template>
              </el-tree>
            </div>

            <div class="saved-panel">
              <div class="saved-panel-title">目前已保存</div>
              <div v-if="savedDailyNoticeItems.length === 0" class="saved-empty">尚未保存班級配置</div>
              <div v-else class="saved-content">
                <div class="saved-summary">共 {{ savedDailyNoticeItems.length }} 個班級</div>
                <ul class="saved-list">
                  <li v-for="item in savedDailyNoticeItems" :key="item.id" class="saved-list-item">
                    <div class="saved-value">{{ item.name }}</div>
                    <div v-if="item.path" class="saved-path">{{ item.path }}</div>
                  </li>
                </ul>
              </div>
              <div
                v-if="savedDailyNoticeItems.length > 0 && dailyNoticeSelectionChanged"
                class="saved-pending-tip"
              >
                左側選擇已變更，請點擊「保存配置」後才會生效
              </div>
            </div>
          </div>

          <div class="action-bar">
            <span v-if="dailyNoticeCheckedIds.length > 0" class="action-selected-tip">
              已選 {{ dailyNoticeCheckedIds.length }} 個班級
            </span>
            <el-button type="primary" :loading="dailyNoticeSaving" @click="handleSaveDailyNotice">保存配置</el-button>
            <el-button @click="loadDailyNoticeData">重新載入</el-button>
          </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { Setting, Notebook, Bell } from '@element-plus/icons-vue'
import { ElNotification } from 'element-plus'
import request from '@/utils/request'

export default {
  name: 'BasicSettings',
  components: { Setting, Notebook, Bell },
  data() {
    return {
      activeTab: 'wecom',
      segmentLoading: false,
      segmentSaving: false,
      dailyNoticeLoading: false,
      dailyNoticeSaving: false,
      segmentTree: [],
      segmentCheckedIds: [],
      savedSegmentId: null,
      dailyNoticeTree: [],
      dailyNoticeCheckedIds: [],
      savedClassDepartmentIds: [],
      treeProps: {
        children: 'children',
        label: 'name'
      }
    }
  },
  computed: {
    dailyNoticeCheckedSet() {
      return new Set(this.dailyNoticeCheckedIds)
    },
    savedSegmentInfo() {
      if (this.savedSegmentId == null) {
        return null
      }
      const node = this.findNodeById(this.segmentTree, this.savedSegmentId)
      return {
        id: this.savedSegmentId,
        name: node?.name || '未知學段',
        path: this.buildNodePath(this.segmentTree, this.savedSegmentId)
      }
    },
    savedDailyNoticeItems() {
      return (this.savedClassDepartmentIds || []).map(id => {
        const node = this.findNodeById(this.dailyNoticeTree, id)
        return {
          id,
          name: node?.name || '未知班級',
          path: this.buildNodePath(this.dailyNoticeTree, id)
        }
      })
    },
    segmentSelectionChanged() {
      const currentId = this.segmentCheckedIds[0] ?? null
      return currentId !== this.savedSegmentId
    },
    dailyNoticeSelectionChanged() {
      const saved = [...(this.savedClassDepartmentIds || [])].sort((a, b) => a - b)
      const current = [...this.dailyNoticeCheckedIds].sort((a, b) => a - b)
      if (saved.length !== current.length) {
        return true
      }
      return saved.some((id, index) => id !== current[index])
    }
  },
  mounted() {
    this.loadSegmentData()
    this.loadDailyNoticeData()
  },
  methods: {
    async loadSegmentData() {
      this.segmentLoading = true
      try {
        const [treeRes, scopeRes] = await Promise.all([
          request({ url: '/system/basic/addressBook/segmentTree', method: 'get' }),
          request({ url: '/system/basic/addressBook/segmentSetting', method: 'get' })
        ])
        if (treeRes.code === 200 || treeRes.code === 0) {
          this.segmentTree = treeRes.data || []
        }
        if (scopeRes.code === 200 || scopeRes.code === 0) {
          const id = scopeRes.data?.segmentDepartmentId
          this.savedSegmentId = id != null ? Number(id) : null
          this.segmentCheckedIds = this.savedSegmentId != null ? [this.savedSegmentId] : []
        }
      } catch (e) {
        console.error(e)
      } finally {
        this.segmentLoading = false
      }
    },

    async loadDailyNoticeData() {
      this.dailyNoticeLoading = true
      try {
        const [treeRes, settingRes] = await Promise.all([
          request({ url: '/system/basic/dailyNotice/classTree', method: 'get' }),
          request({ url: '/system/basic/dailyNotice/classSetting', method: 'get' })
        ])
        if (treeRes.code === 200 || treeRes.code === 0) {
          this.dailyNoticeTree = treeRes.data || []
        }
        if (settingRes.code === 200 || settingRes.code === 0) {
          const ids = settingRes.data?.classDepartmentIds || []
          this.savedClassDepartmentIds = ids.map(id => Number(id))
          this.dailyNoticeCheckedIds = [...this.savedClassDepartmentIds]
        }
      } catch (e) {
        console.error(e)
      } finally {
        this.dailyNoticeLoading = false
      }
    },

    handleSegmentCheckboxChange(data, checked) {
      if (Number(data.type) !== 3) {
        return
      }
      if (checked) {
        this.segmentCheckedIds = [data.id]
      } else {
        this.segmentCheckedIds = []
      }
    },

    handleDailyNoticeCheckboxChange(data, checked) {
      if (Number(data.type) !== 1) {
        return
      }
      const id = data.id
      if (checked) {
        if (!this.dailyNoticeCheckedIds.includes(id)) {
          this.dailyNoticeCheckedIds = [...this.dailyNoticeCheckedIds, id]
        }
      } else {
        this.dailyNoticeCheckedIds = this.dailyNoticeCheckedIds.filter(item => item !== id)
      }
    },

    countClassSelectionUnder(node) {
      let total = 0
      let selected = 0
      const walk = (current) => {
        if (!current) return
        if (current.type === 1) {
          total++
          if (this.dailyNoticeCheckedSet.has(current.id)) {
            selected++
          }
          return
        }
        ;(current.children || []).forEach(walk)
      }
      walk(node)
      return { total, selected }
    },

    getDailyNoticeSelectedLabel(node) {
      if (!node || node.type === 1) {
        return ''
      }
      const { total, selected } = this.countClassSelectionUnder(node)
      if (selected === 0) {
        return ''
      }
      return total > 0 ? `已選 ${selected}/${total}` : `已選 ${selected}`
    },

    findNodeById(nodes, targetId) {
      if (!nodes || targetId == null) {
        return null
      }
      for (const node of nodes) {
        if (!node) continue
        if (Number(node.id) === Number(targetId)) {
          return node
        }
        const found = this.findNodeById(node.children, targetId)
        if (found) {
          return found
        }
      }
      return null
    },

    buildNodePath(nodes, targetId, ancestors = []) {
      if (!nodes || targetId == null) {
        return ''
      }
      for (const node of nodes) {
        if (!node) continue
        const nextAncestors = [...ancestors, node.name]
        if (Number(node.id) === Number(targetId)) {
          return nextAncestors.join(' / ')
        }
        const childPath = this.buildNodePath(node.children, targetId, nextAncestors)
        if (childPath) {
          return childPath
        }
      }
      return ''
    },

    async handleSaveSegment() {
      if (this.segmentCheckedIds.length === 0) {
        ElNotification({ title: '提示', message: '請選擇一個學段', type: 'warning', duration: 3000 })
        return
      }
      this.segmentSaving = true
      try {
        const res = await request({
          url: '/system/basic/addressBook/segmentSetting',
          method: 'post',
          data: { segmentDepartmentId: this.segmentCheckedIds[0] }
        })
        if (res.code === 200 || res.code === 0) {
          this.savedSegmentId = this.segmentCheckedIds[0] ?? null
          ElNotification({ title: '保存成功', message: '學段設置已保存', type: 'success', duration: 3000 })
        }
      } catch (e) {
        console.error(e)
      } finally {
        this.segmentSaving = false
      }
    },

    async handleSaveDailyNotice() {
      if (this.dailyNoticeCheckedIds.length === 0) {
        ElNotification({ title: '提示', message: '請至少選擇一個班級部門', type: 'warning', duration: 3000 })
        return
      }
      this.dailyNoticeSaving = true
      try {
        const res = await request({
          url: '/system/basic/dailyNotice/classSetting',
          method: 'post',
          data: { classDepartmentIds: this.dailyNoticeCheckedIds }
        })
        if (res.code === 200 || res.code === 0) {
          this.savedClassDepartmentIds = [...this.dailyNoticeCheckedIds]
          ElNotification({ title: '保存成功', message: '每日學生手冊通知班級範圍已保存', type: 'success', duration: 3000 })
        }
      } catch (e) {
        console.error(e)
      } finally {
        this.dailyNoticeSaving = false
      }
    }
  }
}
</script>

<style scoped>
.basic-settings-container {
  padding: 0;
}

.card-header .title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

.settings-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}

.settings-tab {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 14px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #f8fafc;
  cursor: pointer;
  text-align: left;
  transition: border-color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;
}

.settings-tab:hover {
  border-color: #cbd5e1;
  background: #fff;
}

.settings-tab.active {
  border-color: #3b82f6;
  background: #eff6ff;
  box-shadow: 0 1px 2px rgba(59, 130, 246, 0.08);
}

.settings-tab-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  flex-shrink: 0;
  font-size: 18px;
}

.settings-tab-icon.wecom {
  color: #059669;
  background: #ecfdf5;
}

.settings-tab-icon.notice {
  color: #2563eb;
  background: #dbeafe;
}

.settings-tab.active .settings-tab-icon.wecom {
  color: #047857;
  background: #d1fae5;
}

.settings-tab.active .settings-tab-icon.notice {
  color: #1d4ed8;
  background: #bfdbfe;
}

.settings-tab-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.settings-tab-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  line-height: 1.3;
}

.settings-tab-desc {
  font-size: 12px;
  color: #64748b;
  line-height: 1.4;
}

.settings-tab.active .settings-tab-title {
  color: #1d4ed8;
}

.tab-panel {
  animation: tab-fade-in 0.2s ease;
}

@keyframes tab-fade-in {
  from {
    opacity: 0;
    transform: translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.selected-count {
  margin-left: 4px;
  font-size: 12px;
  color: #2563eb;
  font-weight: 500;
}

.action-selected-tip {
  align-self: center;
  margin-right: 8px;
  font-size: 14px;
  color: #2563eb;
  font-weight: 500;
}

.settings-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.tree-panel {
  flex: 0 0 auto;
  width: fit-content;
  min-width: 400px;
  max-width: 640px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px 16px;
  min-height: 420px;
  max-height: 520px;
  overflow: auto;
  background: #f8fafc;
}

.saved-panel {
  flex: 1;
  min-width: 280px;
  max-width: 420px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
  min-height: 420px;
  max-height: 520px;
  overflow: auto;
  background: #fff;
}

.saved-panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e2e8f0;
}

.saved-empty {
  color: #94a3b8;
  font-size: 14px;
}

.saved-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.saved-summary {
  font-size: 14px;
  color: #2563eb;
  font-weight: 500;
}

.saved-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.saved-label {
  font-size: 12px;
  color: #64748b;
}

.saved-value {
  font-size: 14px;
  color: #1e293b;
  font-weight: 500;
  word-break: break-word;
}

.saved-path {
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
  word-break: break-word;
}

.saved-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.saved-list-item {
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.saved-list-item:last-child {
  padding-bottom: 0;
  border-bottom: none;
}

.saved-pending-tip {
  margin-top: 16px;
  padding: 10px 12px;
  border-radius: 6px;
  background: #fff7ed;
  color: #c2410c;
  font-size: 13px;
  line-height: 1.5;
}

.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.tree-node-checkbox {
  margin-right: 4px;
  height: auto;
}

.type-tag {
  margin-left: 4px;
}

.action-bar {
  margin-top: 20px;
  display: flex;
  gap: 12px;
}
</style>
