<template>
  <div class="wecom-account-container">
    <!-- 左側部門成員樹狀面板 -->
    <div class="left-panel">
      <div class="panel-header">
        <h3 class="panel-title">
          <el-icon><OfficeBuilding /></el-icon>
          學校組織架構
        </h3>
        <el-input
          v-model="filterText"
          placeholder="搜尋部門或成員姓名"
          clearable
          prefix-icon="Search"
          class="filter-input"
        />
      </div>
      
      <div class="tree-wrapper" v-loading="treeLoading">
        <el-tree
          ref="memberTree"
          :data="treeData"
          :props="defaultProps"
          :filter-node-method="filterNode"
          @node-click="handleNodeClick"
          node-key="treeNodeKey"
          highlight-current
          class="custom-tree"
          :expand-on-click-node="false"
        >
          <template #default="{ node, data }">
            <span class="tree-node-item" :class="{ 'is-member': data.isLeaf }">
              <el-icon v-if="!data.isLeaf" class="node-icon dept-icon"><OfficeBuilding /></el-icon>
              <el-icon v-else class="node-icon member-icon"><User /></el-icon>
              <span class="node-label">{{ node.label }}</span>
            </span>
          </template>
        </el-tree>
      </div>
    </div>

    <!-- 右側帳號管理與設定卡片 -->
    <div class="right-panel">
      <div v-if="!selectedMember" class="empty-state">
        <el-empty description="請在左側通訊錄中點選成員以進行帳號管理" />
      </div>

      <div v-else class="detail-card" v-loading="infoLoading">
        <div class="detail-header">
          <div class="user-profile">
            <div class="avatar-placeholder">
              {{ selectedMember.name ? selectedMember.name.substring(0, Math.min(2, selectedMember.name.length)) : 'U' }}
            </div>
            <div class="user-meta">
              <h2 class="user-name">{{ selectedMember.name }}</h2>
              <p class="user-dept">
                <el-icon><OfficeBuilding /></el-icon>
                {{ selectedMemberDeptName }}
              </p>
            </div>
          </div>
          <div class="account-actions">
            <el-button 
              v-if="hasAccount" 
              type="primary" 
              icon="Edit" 
              class="action-btn"
              @click="openAccountDialog('edit')"
            >
              修改帳號資訊
            </el-button>
            <el-button 
              v-else 
              type="success" 
              icon="Plus" 
              class="action-btn"
              @click="openAccountDialog('add')"
            >
              配置登入帳號
            </el-button>
          </div>
        </div>

        <div class="detail-body">
          <div class="info-section">
            <h4 class="section-title">成員基本資料</h4>
            <div class="info-grid">
              <div class="info-item">
                <span class="info-label">職位</span>
                <span class="info-value">{{ selectedMember.position || '一般職員' }}</span>
              </div>
            </div>
          </div>

          <div class="info-section last-section">
            <h4 class="section-title">系統帳號狀態</h4>
            
            <div v-if="hasAccount" class="account-details">
              <div class="info-grid">
                <div class="info-item">
                  <span class="info-label">登入帳號</span>
                  <span class="info-value highlight">{{ accountInfo.username }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">帳號狀態</span>
                  <span class="info-value">
                    <el-tag :type="accountInfo.status === '0' ? 'success' : 'danger'" class="status-tag">
                      {{ accountInfo.status === '0' ? '正常啟用' : '停用中' }}
                    </el-tag>
                  </span>
                </div>
                <div class="info-item">
                  <span class="info-label">創建時間</span>
                  <span class="info-value">{{ formatTime(accountInfo.createTime) }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">最後更新時間</span>
                  <span class="info-value">{{ formatTime(accountInfo.updateTime) }}</span>
                </div>
              </div>
            </div>

            <div v-else class="no-account-notice">
              <el-icon class="warning-icon"><Warning /></el-icon>
              <div class="notice-content">
                <h5>尚未配置登入帳號</h5>
                <p>該成員目前無法以帳號密碼登入系統。如需開放此成員登入，請點擊上方按鈕為其配置專屬帳號名稱與登入密碼。</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 帳號管理 Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '新增成員登入帳號' : '修改成員登入帳號'"
      width="500px"
      append-to-body
      destroy-on-close
      class="account-dialog"
      :before-close="closeDialog"
    >
      <el-form 
        ref="accountForm" 
        :model="form" 
        :rules="rules" 
        label-width="100px" 
        label-position="right"
        class="custom-form"
      >
        <el-form-item label="成員姓名">
          <el-input :model-value="selectedMember ? selectedMember.name : ''" disabled class="disabled-input" />
        </el-form-item>

        <el-form-item label="登入帳號" prop="username">
          <el-input 
            v-model="form.username" 
            placeholder="請輸入英文或數字組成的帳號" 
            clearable
            maxlength="30"
          />
        </el-form-item>

        <el-form-item :label="dialogType === 'add' ? '登入密碼' : '修改密碼'" prop="password">
          <el-input 
            v-model="form.password" 
            type="password" 
            show-password
            :placeholder="dialogType === 'add' ? '請輸入登入密碼' : '若不修改密碼，請在此留空'" 
            clearable
            maxlength="50"
          />
        </el-form-item>

        <el-form-item label="確認密碼" prop="confirmPassword" v-if="form.password && form.password.trim() !== ''">
          <el-input 
            v-model="form.confirmPassword" 
            type="password" 
            show-password
            placeholder="請再次輸入密碼以進行確認" 
            clearable
            maxlength="50"
          />
        </el-form-item>

        <el-form-item label="帳號狀態" prop="status">
          <el-radio-group v-model="form.status" class="status-radio-group">
            <el-radio label="0">啟用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeDialog">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">確定儲存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { OfficeBuilding, User, Search, Edit, Plus, CopyDocument, Lock, Warning } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { ElNotification } from 'element-plus'

export default {
  name: 'WecomMemberAccount',
  components: {
    OfficeBuilding,
    User,
    Search,
    Edit,
    Plus,
    CopyDocument,
    Lock,
    Warning
  },
  data() {
    const validateUsername = (rule, value, callback) => {
      if (!value) {
        callback(new Error('請輸入登入帳號'))
      } else if (!/^[a-zA-Z0-9_@.-]{3,30}$/.test(value)) {
        callback(new Error('帳號長度須為 3-30 位，且只能包含英文、數字、底線、中劃線、@ 或點'))
      } else {
        callback()
      }
    }
    
    const validatePassword = (rule, value, callback) => {
      if (this.dialogType === 'add' && !value) {
        callback(new Error('請輸入密碼'))
      } else if (value && value.length < 6) {
        callback(new Error('密碼長度不能小於 6 位'))
      } else {
        callback()
      }
    }

    const validateConfirmPassword = (rule, value, callback) => {
      if (this.form.password && !value) {
        callback(new Error('請再次輸入密碼進行確認'))
      } else if (value !== this.form.password) {
        callback(new Error('兩次輸入的密碼不一致'))
      } else {
        callback()
      }
    }

    return {
      filterText: '',
      treeLoading: false,
      infoLoading: false,
      submitLoading: false,
      treeData: [],
      defaultProps: {
        children: 'children',
        label: 'name',
        isLeaf: (data) => data.isLeaf === true
      },
      selectedMember: null,
      selectedMemberDeptName: '',
      hasAccount: false,
      accountInfo: {
        username: '',
        status: '0',
        createTime: null,
        updateTime: null
      },
      
      // Dialog 相關
      dialogVisible: false,
      dialogType: 'add', // add 或 edit
      form: {
        username: '',
        password: '',
        confirmPassword: '',
        status: '0'
      },
      rules: {
        username: [{ required: true, validator: validateUsername, trigger: 'blur' }],
        password: [{ required: this.dialogType === 'add', validator: validatePassword, trigger: 'blur' }],
        confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
        status: [{ required: true, message: '請選擇帳號狀態', trigger: 'change' }]
      }
    }
  },
  watch: {
    filterText(val) {
      this.$refs.memberTree.filter(val)
    }
  },
  mounted() {
    this.loadTreeData()
  },
  methods: {
    async loadTreeData() {
      this.treeLoading = true
      try {
        const response = await request({
          url: '/wecomSchoolDepartment/treeWithMembers',
          method: 'get'
        })
        if (response.code === 200 || response.code === 0) {
          this.treeData = this.generateUniqueKeys(response.data || [])
        } else {
          ElNotification({ title: '載入失敗', message: '無法載入通訊錄數據', type: 'error', duration: 4000 })
        }
      } catch (err) {
        console.error('載入通訊錄樹失敗:', err)
        ElNotification({ title: '載入失敗', message: '系統通訊錄接口異常', type: 'error', duration: 4000 })
      } finally {
        this.treeLoading = false
      }
    },

    generateUniqueKeys(nodes) {
      if (!nodes) return []
      return nodes.map(node => {
        const newNode = { ...node }
        newNode.treeNodeKey = node.isLeaf ? `member_${node.id}` : `dept_${node.id}`
        if (node.children && node.children.length > 0) {
          newNode.children = this.generateUniqueKeys(node.children)
        }
        return newNode
      })
    },
    
    filterNode(value, data) {
      if (!value) return true
      return data.name && data.name.indexOf(value) !== -1
    },

    handleNodeClick(data, node) {
      // 只有葉子節點是成員，才載入詳情
      if (data.isLeaf) {
        this.selectedMember = data
        this.selectedMemberDeptName = this.findDeptPath(node)
        this.loadAccountInfo(data.id)
      } else {
        // 點擊了部門，清空選中狀態與右側面板
        this.selectedMember = null
        this.selectedMemberDeptName = ''
        this.hasAccount = false
      }
    },

    findDeptPath(node) {
      let names = []
      let parent = node.parent
      while (parent && parent.level > 0) {
        if (parent.data && !parent.data.isLeaf) {
          names.unshift(parent.data.name)
        }
        parent = parent.parent
      }
      return names.length > 0 ? names.join(' / ') : '本校'
    },

    async loadAccountInfo(userid) {
      this.infoLoading = true
      this.hasAccount = false
      try {
        const response = await request({
          url: '/system/wecom/account/info',
          method: 'get',
          params: { userid }
        })
        if (response.code === 200 || response.code === 0) {
          if (response.data) {
            this.accountInfo = response.data
            this.hasAccount = true
          } else {
            this.accountInfo = {
              username: '',
              status: '0',
              createTime: null,
              updateTime: null
            }
            this.hasAccount = false
          }
        } else {
          ElNotification({ title: '查詢失敗', message: response.msg || '無法獲取成員帳號狀態', type: 'error', duration: 4000 })
        }
      } catch (err) {
        console.error('查詢帳號資訊失敗:', err)
        ElNotification({ title: '查詢失敗', message: '帳號資訊接口異常', type: 'error', duration: 4000 })
      } finally {
        this.infoLoading = false
      }
    },

    openAccountDialog(type) {
      this.dialogType = type
      this.dialogVisible = true
      
      if (type === 'edit') {
        this.form = {
          username: this.accountInfo.username,
          password: '',
          confirmPassword: '',
          status: this.accountInfo.status || '0'
        }
      } else {
        this.form = {
          username: '',
          password: '',
          confirmPassword: '',
          status: '0'
        }
      }
      
      // 清理以前的驗證提示
      this.$nextTick(() => {
        if (this.$refs.accountForm) {
          this.$refs.accountForm.clearValidate()
        }
      })
    },

    closeDialog() {
      this.dialogVisible = false
      this.form = {
        username: '',
        password: '',
        confirmPassword: '',
        status: '0'
      }
    },

    submitForm() {
      this.$refs.accountForm.validate(async (valid) => {
        if (!valid) return
        
        this.submitLoading = true
        try {
          const payload = {
            userid: this.selectedMember.id,
            username: this.form.username,
            status: this.form.status,
            password: this.form.password || ''
          }
          
          const response = await request({
            url: '/system/wecom/account/save',
            method: 'post',
            data: payload
          })
          
          if (response.code === 200 || response.code === 0) {
            ElNotification({ title: '儲存成功', message: response.msg || '儲存帳號資訊成功', type: 'success', duration: 4000 })
            this.closeDialog()
            // 重新載入帳號詳情
            this.loadAccountInfo(this.selectedMember.id)
          } else {
            ElNotification({ title: '儲存失敗', message: response.msg || '儲存帳號資訊失敗', type: 'error', duration: 4000 })
          }
        } catch (err) {
          console.error('儲存帳號失敗:', err)
          ElNotification({ title: '儲存失敗', message: err.message || '儲存帳號資訊接口異常', type: 'error', duration: 4000 })
        } finally {
          this.submitLoading = false
        }
      })
    },

    copyText(text) {
      if (!text) return
      navigator.clipboard.writeText(text).then(() => {
        ElNotification({ title: '複製成功', message: '已複製 UserID 至剪貼簿', type: 'success', duration: 2000 })
      }).catch(err => {
        console.error('複製失敗:', err)
      })
    },

    formatTime(timeStr) {
      if (!timeStr) return '暫無資料'
      try {
        const date = new Date(timeStr)
        if (isNaN(date.getTime())) return timeStr
        
        const pad = (n) => n.toString().padStart(2, '0')
        return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
      } catch (e) {
        return timeStr
      }
    }
  }
}
</script>

<style scoped>
.wecom-account-container {
  display: flex;
  height: calc(100vh - 120px);
  gap: 24px;
  background: transparent;
  color: #1e293b;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

/* 左側面板 */
.left-panel {
  width: 320px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 4px 20px -2px rgba(148, 163, 184, 0.12), 0 2px 8px -1px rgba(148, 163, 184, 0.08);
  display: flex;
  flex-direction: column;
  border: 1px solid rgba(226, 232, 240, 0.8);
  overflow: hidden;
  flex-shrink: 0;
}

.panel-header {
  padding: 20px 20px 14px;
  border-bottom: 1px solid #f1f5f9;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 12px 0;
}

.panel-title .el-icon {
  color: #2563eb;
  font-size: 20px;
}

.filter-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  background-color: #f8fafc;
  box-shadow: none !important;
  border: 1px solid #e2e8f0;
  transition: all 0.25s;
}

.filter-input :deep(.el-input__wrapper.is-focus),
.filter-input :deep(.el-input__wrapper:hover) {
  border-color: #3b82f6;
  background-color: #ffffff;
  box-shadow: 0 0 0 1px #3b82f6 !important;
}

.tree-wrapper {
  flex: 1;
  overflow-y: auto;
  padding: 10px 14px;
}

.tree-wrapper::-webkit-scrollbar {
  width: 6px;
}

.tree-wrapper::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

/* 樹節點外觀 */
.custom-tree :deep(.el-tree-node__content) {
  height: 38px;
  border-radius: 8px;
  margin-bottom: 2px;
  transition: all 0.2s;
  padding-left: 8px !important;
}

.custom-tree :deep(.el-tree-node__content:hover) {
  background-color: #f1f5f9;
}

.custom-tree :deep(.el-tree-node.is-current > .el-tree-node__content) {
  background-color: #eff6ff;
  color: #2563eb;
  font-weight: 500;
}

.tree-node-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #334155;
  width: 100%;
}

.custom-tree :deep(.el-tree-node.is-current > .el-tree-node__content) .tree-node-item {
  color: #2563eb;
}

.node-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.dept-icon {
  color: #64748b;
}

.member-icon {
  color: #10b981;
}

/* 右側詳細面板 */
.right-panel {
  flex: 1;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 4px 20px -2px rgba(148, 163, 184, 0.12), 0 2px 8px -1px rgba(148, 163, 184, 0.08);
  border: 1px solid rgba(226, 232, 240, 0.8);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.detail-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.detail-header {
  padding: 24px 32px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(to right, #f8fafc, #ffffff);
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-placeholder {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: #ffffff;
  font-size: 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
}

.user-name {
  font-size: 20px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 4px 0;
}

.user-dept {
  font-size: 13px;
  color: #64748b;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 4px;
}

.action-btn {
  border-radius: 8px;
  padding: 10px 18px;
  font-weight: 500;
  box-shadow: none !important;
  transition: all 0.2s;
}

.action-btn:hover {
  transform: translateY(-1px);
}

.detail-body {
  flex: 1;
  overflow-y: auto;
  padding: 32px;
}

.info-section {
  margin-bottom: 32px;
}

.last-section {
  margin-bottom: 0;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin: 0 0 16px 0;
  padding-left: 8px;
  border-left: 3px solid #3b82f6;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.info-item {
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  border-radius: 10px;
  padding: 14px 18px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  transition: all 0.2s;
}

.info-item:hover {
  border-color: #cbd5e1;
  background: #f1f5f9;
}

.info-label {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 500;
}

.info-value {
  font-size: 14px;
  color: #334155;
  font-weight: 600;
  word-break: break-all;
}

.info-value.highlight {
  color: #2563eb;
  font-size: 15px;
}

.info-value.copyable {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: color 0.2s;
}

.info-value.copyable:hover {
  color: #2563eb;
}

.copy-icon {
  font-size: 13px;
  color: #94a3b8;
}

.info-value.copyable:hover .copy-icon {
  color: #2563eb;
}

.status-tag {
  font-weight: 500;
  padding: 4px 10px;
  border-radius: 6px;
}

/* 安全面板 banner */
.security-banner {
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
  border: 1px solid #bbf7d0;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.banner-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #10b981;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  box-shadow: 0 4px 10px rgba(16, 185, 129, 0.2);
  flex-shrink: 0;
}

.banner-text h5 {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 600;
  color: #14532d;
}

.banner-text p {
  margin: 0;
  font-size: 12.5px;
  color: #166534;
  line-height: 1.5;
}

/* 暫無帳號提示 */
.no-account-notice {
  background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
  border: 1px solid #fde68a;
  border-radius: 12px;
  padding: 20px 24px;
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.warning-icon {
  font-size: 24px;
  color: #d97706;
  margin-top: 2px;
  flex-shrink: 0;
}

.notice-content h5 {
  margin: 0 0 6px 0;
  font-size: 15px;
  font-weight: 600;
  color: #78350f;
}

.notice-content p {
  margin: 0;
  font-size: 13px;
  color: #92400e;
  line-height: 1.5;
}

/* Dialog Form 樣式優化 */
:deep(.account-dialog) {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

:deep(.account-dialog .el-dialog__header) {
  margin-right: 0;
  padding: 20px 24px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

:deep(.account-dialog .el-dialog__title) {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

:deep(.account-dialog .el-dialog__body) {
  padding: 24px;
}

:deep(.account-dialog .el-dialog__footer) {
  padding: 16px 24px;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
}

.custom-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.custom-form :deep(.el-form-item__error) {
  position: relative;
  display: block;
  padding-top: 4px;
  line-height: 1.4;
  color: #f56c6c;
}

.custom-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #475569;
}

.custom-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: none !important;
  border: 1px solid #cbd5e1;
  transition: border-color 0.2s;
}

.custom-form :deep(.el-input__wrapper:hover),
.custom-form :deep(.el-input__wrapper.is-focus) {
  border-color: #3b82f6;
}

.disabled-input :deep(.el-input__inner) {
  color: #64748b !important;
  font-weight: 500;
}

.disabled-input :deep(.el-input__wrapper) {
  background-color: #f1f5f9;
  border-color: #e2e8f0;
}

.status-radio-group {
  box-shadow: none !important;
}

.status-radio-group :deep(.el-radio) {
  margin-right: 20px;
}

.dialog-footer .el-button {
  border-radius: 8px;
  padding: 8px 16px;
}
</style>
