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
          <div class="tree-panel" v-loading="loading">
            <el-tree
              ref="segmentTreeRef"
              :data="segmentTree"
              :props="treeProps"
              node-key="id"
              show-checkbox
              check-strictly
              default-expand-all
              :default-checked-keys="checkedIds"
              @check="handleCheck"
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
            <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
            <el-button @click="loadData">重新載入</el-button>
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
      loading: false,
      saving: false,
      segmentTree: [],
      checkedIds: [],
      treeProps: {
        children: 'children',
        label: 'name'
      }
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const [treeRes, scopeRes] = await Promise.all([
          request({ url: '/system/basic/addressBook/segmentTree', method: 'get' }),
          request({ url: '/system/basic/addressBook/segmentSetting', method: 'get' })
        ])
        if (treeRes.code === 200 || treeRes.code === 0) {
          this.segmentTree = this.markTreeSelectable(treeRes.data || [])
        }
        if (scopeRes.code === 200 || scopeRes.code === 0) {
          const id = scopeRes.data?.segmentDepartmentId
          this.checkedIds = id != null ? [id] : []
          this.$nextTick(() => {
            if (this.$refs.segmentTreeRef) {
              this.$refs.segmentTreeRef.setCheckedKeys(this.checkedIds)
            }
          })
        }
      } catch (e) {
        console.error(e)
      } finally {
        this.loading = false
      }
    },
    markTreeSelectable(nodes) {
      return (nodes || []).map(node => ({
        ...node,
        disabled: node.type !== 3,
        children: node.children ? this.markTreeSelectable(node.children) : undefined
      }))
    },
    handleCheck(data, { checkedKeys }) {
      const treeRef = this.$refs.segmentTreeRef
      if (!treeRef || data.type !== 3) {
        return
      }

      const isChecked = checkedKeys.includes(data.id)
      if (isChecked) {
        treeRef.setCheckedKeys([data.id])
        this.checkedIds = [data.id]
      } else {
        this.checkedIds = []
      }
    },
    async handleSave() {
      if (this.checkedIds.length === 0) {
        ElNotification({ title: '提示', message: '請選擇一個學段', type: 'warning', duration: 3000 })
        return
      }
      this.saving = true
      try {
        const res = await request({
          url: '/system/basic/addressBook/segmentSetting',
          method: 'post',
          data: { segmentDepartmentId: this.checkedIds[0] }
        })
        if (res.code === 200 || res.code === 0) {
          ElNotification({ title: '保存成功', message: '學段設置已保存', type: 'success', duration: 3000 })
        }
      } catch (e) {
        console.error(e)
      } finally {
        this.saving = false
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
