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

      <el-tabs v-model="activeTab">
        <el-tab-pane label="家校通訊錄選擇學段" name="wecom">
          <div class="tree-panel" v-loading="segmentLoading">
            <el-tree
              ref="segmentTreeRef"
              :data="segmentTree"
              :props="treeProps"
              node-key="id"
              show-checkbox
              check-strictly
              :default-checked-keys="segmentCheckedIds"
              @check="handleSegmentCheck"
            >
              <template #default="{ data }">
                <span class="tree-node">
                  <span>{{ data.name }}</span>
                  <el-tag v-if="data.type === 3" size="small" type="success" class="type-tag">學段</el-tag>
                  <el-tag v-else-if="data.type === 5" size="small" class="type-tag">學校</el-tag>
                </span>
              </template>
            </el-tree>
          </div>

          <div class="action-bar">
            <el-button type="primary" :loading="segmentSaving" @click="handleSaveSegment">保存配置</el-button>
            <el-button @click="loadSegmentData">重新載入</el-button>
          </div>
        </el-tab-pane>

        <el-tab-pane label="每日學校通知選擇班級" name="dailyNotice">
          <div class="tree-panel" v-loading="dailyNoticeLoading">
            <el-tree
              ref="dailyNoticeTreeRef"
              :data="dailyNoticeTree"
              :props="treeProps"
              node-key="id"
              show-checkbox
              check-strictly
              :default-checked-keys="dailyNoticeCheckedIds"
              @check="handleDailyNoticeCheck"
            >
              <template #default="{ data }">
                <span class="tree-node">
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

          <div class="action-bar">
            <span v-if="dailyNoticeCheckedIds.length > 0" class="action-selected-tip">
              已選 {{ dailyNoticeCheckedIds.length }} 個班級
            </span>
            <el-button type="primary" :loading="dailyNoticeSaving" @click="handleSaveDailyNotice">保存配置</el-button>
            <el-button @click="loadDailyNoticeData">重新載入</el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script>
import { Setting } from '@element-plus/icons-vue'
import { ElNotification } from 'element-plus'
import request from '@/utils/request'

export default {
  name: 'BasicSettings',
  components: { Setting },
  data() {
    return {
      activeTab: 'wecom',
      segmentLoading: false,
      segmentSaving: false,
      dailyNoticeLoading: false,
      dailyNoticeSaving: false,
      segmentTree: [],
      segmentCheckedIds: [],
      dailyNoticeTree: [],
      dailyNoticeCheckedIds: [],
      treeProps: {
        children: 'children',
        label: 'name'
      }
    }
  },
  computed: {
    dailyNoticeCheckedSet() {
      return new Set(this.dailyNoticeCheckedIds)
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
          this.segmentTree = this.markSegmentTreeSelectable(treeRes.data || [])
        }
        if (scopeRes.code === 200 || scopeRes.code === 0) {
          const id = scopeRes.data?.segmentDepartmentId
          this.segmentCheckedIds = id != null ? [id] : []
          this.$nextTick(() => {
            if (this.$refs.segmentTreeRef) {
              this.$refs.segmentTreeRef.setCheckedKeys(this.segmentCheckedIds)
            }
          })
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
          this.dailyNoticeTree = this.markDailyNoticeTreeSelectable(treeRes.data || [])
        }
        if (settingRes.code === 200 || settingRes.code === 0) {
          const ids = settingRes.data?.classDepartmentIds || []
          this.dailyNoticeCheckedIds = ids.map(id => Number(id))
          this.$nextTick(() => {
            if (this.$refs.dailyNoticeTreeRef) {
              this.$refs.dailyNoticeTreeRef.setCheckedKeys(this.dailyNoticeCheckedIds)
            }
          })
        }
      } catch (e) {
        console.error(e)
      } finally {
        this.dailyNoticeLoading = false
      }
    },

    markSegmentTreeSelectable(nodes) {
      return (nodes || []).map(node => ({
        ...node,
        disabled: node.type !== 3,
        children: node.children ? this.markSegmentTreeSelectable(node.children) : undefined
      }))
    },

    markDailyNoticeTreeSelectable(nodes) {
      return (nodes || []).map(node => ({
        ...node,
        disabled: node.type !== 1,
        children: node.children ? this.markDailyNoticeTreeSelectable(node.children) : undefined
      }))
    },

    handleSegmentCheck(data, { checkedKeys }) {
      const treeRef = this.$refs.segmentTreeRef
      if (!treeRef || data.type !== 3) {
        return
      }

      const isChecked = checkedKeys.includes(data.id)
      if (isChecked) {
        treeRef.setCheckedKeys([data.id])
        this.segmentCheckedIds = [data.id]
      } else {
        this.segmentCheckedIds = []
      }
    },

    handleDailyNoticeCheck() {
      const treeRef = this.$refs.dailyNoticeTreeRef
      if (!treeRef) {
        return
      }
      const checkedNodes = treeRef.getCheckedNodes(false, true)
      this.dailyNoticeCheckedIds = checkedNodes
        .filter(node => node.type === 1)
        .map(node => node.id)
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
          ElNotification({ title: '保存成功', message: '每日學校通知班級範圍已保存', type: 'success', duration: 3000 })
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

.tree-panel {
  display: inline-block;
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

.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 8px;
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
