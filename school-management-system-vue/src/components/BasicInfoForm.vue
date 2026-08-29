<template>
  <div class="basic-info-form">
    <el-form
      ref="formRef"
      :model="localFormData"
      :rules="rules"
      label-width="120px"
      class="form-container"
    >
      <!-- 通知標題 -->
      <el-form-item label="通知標題" prop="title">
        <el-input
          v-model="localFormData.title"
          placeholder="請輸入通知標題"
          maxlength="100"
          show-word-limit
          clearable
        />
      </el-form-item>

      <!-- 通知正文 -->
      <el-form-item label="通知正文" prop="content">
        <el-input
          v-model="localFormData.content"
          type="textarea"
          :rows="8"
          placeholder="請輸入通知正文內容"
          maxlength="2000"
          show-word-limit
        />
      </el-form-item>

      <!-- 發送人 -->
      <el-form-item label="發送人" prop="senderName">
        <el-input
          v-model="localFormData.senderName"
          disabled
          :placeholder="senderLoading ? '正在獲取發送人資訊…' : ''"
        />
        <div class="form-tip">發送人由系統自動設定，不可修改</div>
      </el-form-item>

      <!-- 跳轉連結 -->
      <el-form-item label="跳轉連結" prop="jumpUrl">
        <el-input
          v-model="localFormData.jumpUrl"
          placeholder="請輸入跳轉連結（選填）"
          clearable
        />
      </el-form-item>

      <!-- 附件上傳 -->
      <el-form-item label="附件/圖片">
        <div class="upload-section">
          <el-upload
            class="upload-demo"
            list-type="text"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :file-list="fileList"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :on-remove="handleUploadRemove"
            :before-upload="beforeUpload"
            :on-change="handleChange"
            :on-exceed="handleExceed"
            multiple
            :limit="5"
          >
            <el-button 
              class="custom-upload-btn" 
              size="large"
              :disabled="fileList.length >= 5"
              :class="{ 'is-disabled': fileList.length >= 5 }"
            >
              <el-icon class="btn-icon"><Upload /></el-icon>
              <span class="btn-text">
                {{ fileList.length >= 5 ? '最大附件數量已達標' : '點擊上傳附件' }}
              </span>
            </el-button>
          </el-upload>
        </div>
      </el-form-item>

      <!-- 問題設置 -->
      <el-form-item label="問題設置">
        <div class="questions-section">
          <div class="questions-header">
            <div class="title-wrapper">
              <span class="section-title">已添加的問題 ({{ localFormData.questions.length }})</span>
            </div>
            <div class="buttons-wrapper">
              <el-button class="add-form-question-btn" size="large" @click="addFormQuestion">
                <el-icon class="btn-icon"><Edit /></el-icon>
                <span class="btn-text">添加問題</span>
              </el-button>
            </div>
          </div>
          
          <div v-if="localFormData.questions.length > 0" class="questions-list">
            <div 
              v-for="(question, index) in localFormData.questions" 
              :key="question.id || index"
              class="question-item"
            >
              <div class="question-info">
                <div class="question-left">
                  <span class="question-number">{{ index + 1 }}.</span>
                  <span class="question-title" style="margin-right: 16px;">{{ question.title }}</span>
                </div>
                <div class="question-right">
                  <div class="question-actions">
                    <el-button size="small" @click="editFormQuestion(index)">
                      <el-icon><Edit /></el-icon>
                      編輯
                    </el-button>
                    <el-button size="small" type="danger" @click="removeQuestion(index)">
                      <el-icon><Delete /></el-icon>
                      刪除
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <div v-else class="no-questions">
            暫無問題，請點擊上方按鈕添加問題
          </div>
        </div>
      </el-form-item>

      <!-- 下一步按鈕 -->
      <el-form-item>
        <div class="form-actions">
          <el-button type="primary" @click="goToNext">
            <el-icon><ArrowRight /></el-icon>
            下一步：發送設置
          </el-button>
        </div>
      </el-form-item>
    </el-form>

    <!-- 表單問題編輯對話框 -->
    <FormQuestionDialog
      v-model:visible="showFormQuestionDialog"
      :question="editingFormQuestion"
      @save="saveFormQuestion"
    />
  </div>
</template>

<script>
import {ArrowRight, Delete, Edit, Upload} from '@element-plus/icons-vue'
import FormQuestionDialog from './FormQuestionDialog.vue'
import { ElNotification } from 'element-plus'
import {API_BASE_PATH, normalizeProfileUrl} from '@/utils/deployment'
import MD5 from 'crypto-js/md5'
import settings from '@/config/settings'
import request from '@/utils/request'

export default {
  name: 'BasicInfoForm',
  components: {
    FormQuestionDialog
  },
  props: {
    formData: {
      type: Object,
      required: true
    }
  },
  emits: ['next'],
  data() {
    return {
      localFormData: { ...this.formData },
      fileList: [],
      uploadUrl: `${API_BASE_PATH}/common/upload`,
      uploadHeaders: {},
      showFormQuestionDialog: false,
      editingFormQuestion: null,
      senderLoading: false,
      rules: {
        title: [
          { required: true, message: '請輸入通知標題', trigger: 'blur' },
          { max: 100, message: '標題長度不能超過 100 個字符', trigger: 'blur' }
        ],
        content: [
          { required: true, message: '請輸入通知正文', trigger: 'blur' },
          { max: 2000, message: '正文長度不能超過 2000 個字符', trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    // 初始化上傳驗簽 headers
    this.refreshUploadHeaders()
  },
  watch: {
    formData: {
      handler(newVal) {
        // 只在對話框關閉且數據真正變化時才同步
        // 避免在保存問題時覆蓋正在編輯的數據
        if (!this.showFormQuestionDialog && 
            JSON.stringify(newVal.questions || []) !== JSON.stringify(this.localFormData.questions || [])) {
          this.localFormData = { ...newVal }
          this.initFileList()
        }
      },
      deep: true
    }
  },
  mounted() {
    this.initFileList()
    this.loadSenderName()
  },
  methods: {
    async loadSenderName() {
      this.senderLoading = true
      try {
        const response = await request({
          url: '/system/userRole/checkCurrentUser',
          method: 'get'
        })
        if (response.code === 200 || response.code === 0) {
          const senderName = response.data?.senderName
            || this.$store?.state?.user?.username
            || ''
          this.localFormData.senderName = senderName
          this.formData.senderName = senderName
        }
      } catch (error) {
        const fallback = this.$store?.state?.user?.username || ''
        this.localFormData.senderName = fallback
        this.formData.senderName = fallback
      } finally {
        this.senderLoading = false
      }
    },
    /** 合併 el-upload 回傳列表與本地 fileList，保留已上傳文件的 _originalUrl 等自定義欄位 */
    mergeFileList(incomingList) {
      const existingByUid = new Map(
        (this.fileList || []).filter(f => f.uid != null).map(f => [f.uid, f])
      )
      return (incomingList || [])
        .filter(f => f.status !== 'removed')
        .map(f => {
          const existing = existingByUid.get(f.uid)
          return {
            ...f,
            name: existing?.name || f.name || f.raw?.name,
            url: existing?.url || f.url,
            status: existing?.status === 'success' ? 'success' : f.status,
            _originalUrl: existing?._originalUrl || f._originalUrl
          }
        })
    },

    /** 將已成功上傳的附件同步到 localFormData.attachmentUrls */
    syncAttachmentUrls() {
      this.localFormData.attachmentUrls = this.fileList
        .filter(f => f._originalUrl || f.status === 'success')
        .map(f => ({
          name: f.name || f.raw?.name || '附件',
          url: f._originalUrl || this.toStorageUrl(f.url)
        }))
        .filter(f => f.url)
    },

    /** 將帶 /sms-api 前綴的顯示 URL 還原為存儲用路徑 */
    toStorageUrl(url) {
      if (!url || typeof url !== 'string') {
        return url
      }
      const profilePrefix = `${API_BASE_PATH}/profile`
      if (url.startsWith(profilePrefix)) {
        return url.replace(profilePrefix, '/profile')
      }
      return url
    },

    initFileList() {
      if (this.localFormData.attachmentUrls && this.localFormData.attachmentUrls.length > 0) {
        try {
          const urls = typeof this.localFormData.attachmentUrls === 'string' 
            ? JSON.parse(this.localFormData.attachmentUrls)
            : this.localFormData.attachmentUrls
            
          // 確保始終根據最新的 urls 重新構建 fileList
          this.fileList = urls.map((item, index) => {
            const originalUrl = typeof item === 'string' ? item : (item._originalUrl || item.url)
            const name = typeof item === 'string'
              ? decodeURIComponent(item.substring(item.lastIndexOf('/') + 1)) || `附件${index + 1}`
              : (item.name || `附件${index + 1}`)
            return {
              uid: `init-${index}-${originalUrl}`,
              name,
              url: normalizeProfileUrl(originalUrl),
              status: 'success',
              _originalUrl: originalUrl
            }
          })
        } catch (e) {
          console.error('初始化文件列表失敗:', e)
          this.fileList = []
        }
      } else {
        this.fileList = []
      }
    },

    // 生成驗簽 headers，與 request.js 的攔截器邏輯一致
    // 簽名算法：MD5(appSecret + timestamp + nonce)
    // 每次調用都生成新的 timestamp + nonce，確保 nonce 不重複
    refreshUploadHeaders() {
      const timestamp = Date.now().toString()
      const nonce = (typeof crypto !== 'undefined' && crypto.randomUUID)
        ? crypto.randomUUID().replace(/-/g, '')
        : 'xxxxxxxxxxxx4xxxyxxxxxxxxxxxxxxx'.replace(/[xy]/g, c => {
            const r = Math.random() * 16 | 0
            return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16)
          })
      const signature = MD5(settings.appSecret + timestamp + nonce).toString()
      this.uploadHeaders = {
        'x-timestamp': timestamp,
        'x-nonces': nonce,
        'x-signature': signature
      }
    },

    goToNext() {
      this.$refs.formRef.validate((valid) => {
        if (valid) {
          this.syncAttachmentUrls()
          const {
            receivers,
            ccs,
            replyDeadline,
            reminderTime
          } = this.formData
          Object.assign(this.formData, this.localFormData)
          this.formData.attachmentUrls = JSON.parse(
            JSON.stringify(this.localFormData.attachmentUrls || [])
          )
          // 保留發送設置步驟已填寫的資料，避免覆蓋選人結果
          this.formData.receivers = receivers || []
          this.formData.ccs = ccs || []
          this.formData.replyDeadline = replyDeadline ?? null
          this.formData.reminderTime = reminderTime ?? null
          this.$emit('next')
        } else {
          ElNotification({ title: '請完善資訊', message: '請先完善基本資訊再進行下一步', type: 'warning', duration: 3000 })
        }
      })
    },

    beforeUpload(file) {
      const isLt10M = file.size / 1024 / 1024 < 10
      if (!isLt10M) {
        ElNotification({ title: '檔案過大', message: '上傳檔案大小不能超過 10MB！', type: 'error', duration: 4000 })
        return false
      }
      // 每次上傳前重新生成驗簽（防止 nonce 重複被後端攔截器拒絕）
      this.refreshUploadHeaders()
      return true
    },

    handleUploadSuccess(response, file, fileList) {
      if (response.code === 200) {
        // 保存到數據庫的URL
        const originalUrl = response.data.url
        
        // 前端顯示用的URL（帶 /sms-api 前綴）
        const displayUrl = normalizeProfileUrl(originalUrl)
        
        this.fileList = this.mergeFileList(fileList).map(f => {
          if (f.uid === file.uid) {
            return {
              ...f,
              url: displayUrl,
              status: 'success',
              name: file.name,
              _originalUrl: originalUrl
            }
          }
          return f
        })

        this.syncAttachmentUrls()
        
        ElNotification({ title: '上傳成功', message: '附件已成功上傳', type: 'success', duration: 3000 })
      } else {
        ElNotification({ title: '上傳失敗', message: response.msg || '上傳失敗', type: 'error', duration: 4000 })
        const index = this.fileList.findIndex(f => f.uid === file.uid)
        if (index > -1) {
          this.fileList.splice(index, 1)
        }
      }
    },

    handleUploadError(error, file) {
      console.error('上傳失敗:', error)
      ElNotification({ title: '上傳失敗', message: '上傳失敗，請重試', type: 'error', duration: 4000 })
      const index = this.fileList.findIndex(f => f.uid === file.uid)
      if (index > -1) {
        this.fileList.splice(index, 1)
      }
    },

    handleExceed(files, fileList) {
      ElNotification({ title: '附件數量限制', message: `最多只能上傳 5 個附件！您選擇了 ${files.length} 個檔案，加上現有共 ${files.length + fileList.length} 個。`, type: 'warning', duration: 4000 })
    },

    handleChange(file, fileList) {
      this.fileList = this.mergeFileList(fileList)
    },

    handleUploadRemove(file) {
      const fileIndex = this.fileList.findIndex(item => {
        const itemOriginalUrl = item._originalUrl || item.url
        const fileOriginalUrl = file._originalUrl || file.url
        return itemOriginalUrl === fileOriginalUrl || item.uid === file.uid
      })
      if (fileIndex > -1) {
        this.fileList.splice(fileIndex, 1)
      }
      this.syncAttachmentUrls()
    },

    addFormQuestion() {
      this.editingFormQuestion = null
      this.showFormQuestionDialog = true
    },
    
    editFormQuestion(index) {
      const questionData = this.localFormData.questions[index]
      // 傳遞完整的 questionnaireData 和 questions 數據
      this.editingFormQuestion = {
        ...questionData,
        questionnaireData: questionData.questionnaireData || { title: questionData.title || '問卷調查', description: questionData.description || '' },
        questions: questionData.questions || []
      }
      this.showFormQuestionDialog = true
    },
    
    saveFormQuestion(saveData) {
      // saveData 格式：{ questionnaire: {...}, questions: [...] }
      
      if (this.editingFormQuestion) {
        // 編輯模式：更新現有問題
        const index = this.localFormData.questions.findIndex(q => q.id === this.editingFormQuestion.id)
        if (index > -1) {
          // 將整個 questionnaire 和 questions 打包成一個問題對象
          const formQuestion = {
            id: this.editingFormQuestion.id,
            questionType: '5',
            type: '5',
            title: saveData.questionnaire?.title || '問卷調查',
            description: saveData.questionnaire?.description || '',
            questionnaireData: saveData.questionnaire,
            questions: JSON.parse(JSON.stringify(saveData.questions || []))
          }
          // Vue 3 無 $set，直接賦值即可保持響應式
          this.localFormData.questions.splice(index, 1, formQuestion)
          ElNotification({ title: '更新成功', message: '表單問題已成功更新', type: 'success', duration: 3000 })
        }
      } else {
        // 新增模式：創建新問題
        const newQuestion = {
          id: Date.now(),
          questionType: '5',
          type: '5',
          title: saveData.questionnaire?.title || '問卷調查',
          description: saveData.questionnaire?.description || '',
          questionnaireData: saveData.questionnaire,
          questions: JSON.parse(JSON.stringify(saveData.questions || []))
        }
        this.localFormData.questions.push(newQuestion)
        ElNotification({ title: '添加成功', message: '表單問題已成功添加', type: 'success', duration: 3000 })
      }
      
      // 同步到父組件（深拷貝），再關閉對話框
      this.formData.questions = JSON.parse(JSON.stringify(this.localFormData.questions))
      this.editingFormQuestion = null
      this.showFormQuestionDialog = false
    },

    removeQuestion(index) {
      this.$confirm('確認刪除此問題嗎？', '提示', {
        confirmButtonText: '確定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.localFormData.questions.splice(index, 1)
        ElNotification({ title: '刪除成功', message: '問題已成功刪除', type: 'success', duration: 3000 })
      }).catch(() => {})
    },

  }
}
</script>

<style scoped>
.basic-info-form {
  width: 100%;
}

.form-container .el-form-item__label {
  font-weight: 700;
  color: #374151;
  font-size: 14px;
}

.form-tip {
  font-size: 13px;
  color: #6b7280;
  margin-top: 6px;
  line-height: 1.6;
  font-weight: 500;
}

.questions-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
  padding: 20px 24px;
  background: linear-gradient(135deg, #60a5fa 0%, #60a5fa 100%);
  border-radius: 16px;
  border: 2px solid #60a5fa;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.questions-header .title-wrapper {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.questions-header .buttons-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.questions-header .section-title {
  font-weight: 700;
  color: #ffffff;
  font-size: 16px;
  letter-spacing: 0.5px;
  white-space: nowrap;
}

.question-item:hover {
  background: #f9fafb;
  border-color: #60a5fa;
  box-shadow: 0 6px 16px rgba(59, 130, 246, 0.2);
}

.question-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: transparent;
  border-radius: 12px;
  margin-bottom: 12px;
  border: 2px solid #e5e7eb;
}

.question-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.question-left {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  min-width: 0;
}

.question-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.question-type-tag.el-tag {
  font-size: 12px;
  padding: 6px 14px;
  border-radius: 20px;
  font-weight: 700;
  letter-spacing: 0.5px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  white-space: nowrap;
}

.question-number {
  font-weight: 700;
  color: #3b82f6;
  font-size: 15px;
  min-width: 28px;
  text-align: center;
}

.question-title {
  font-size: 15px;
  color: #374151;
  flex: 1;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.question-actions {
  display: flex;
  gap: 8px !important;
}

.question-actions .el-button {
  margin: 0 !important;
  font-weight: 600;
  padding: 8px 12px !important;
}

.question-actions .el-button .el-icon {
  margin-right: 4px;
}

.question-type-tag.el-tag:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.no-questions {
  text-align: center;
  color: #6b7280;
  padding: 40px;
  font-size: 14px;
  background: linear-gradient(135deg, #f9fafb 0%, #ffffff 100%);
  border-radius: 12px;
  border: 2px dashed #e5e7eb;
}

.no-questions:hover {
  border-color: #60a5fa;
  background: #f9fafb;
}

.form-actions {
  margin-top: 28px;
  padding-top: 28px;
  border-top: 2px solid #e5e7eb;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.form-actions .el-button {
  min-width: 120px;
  height: 38px;
  font-weight: 500;
  font-size: 14px;
  transition: all 0.2s ease;
}

.form-actions .el-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 上傳區域 - 與問題設置協調的風格 */
.upload-section {
  width: 100%;
  padding: 20px 24px;
  background: linear-gradient(135deg, #f9fafb 0%, #ffffff 100%);
  border-radius: 16px;
  border: 2px solid #e5e7eb;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.upload-section:hover {
  border-color: #60a5fa;
  box-shadow: 0 4px 12px rgba(96, 165, 250, 0.15);
}

.upload-demo {
  width: 100%;
}

.upload-demo :deep(.el-upload) {
  width: auto;
  display: inline-block;
}

/* 上傳按鈕 - 與問題設置一致的漸變藍色風格 */
.custom-upload-btn.el-button {
  height: 40px;
  padding: 0 20px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
  border: none;
  border-radius: 10px;
  box-shadow: 0 3px 10px rgba(59, 130, 246, 0.25);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  gap: 8px;
  letter-spacing: 0.5px;
}

.custom-upload-btn.el-button:hover:not(.is-disabled) {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  box-shadow: 0 5px 16px rgba(59, 130, 246, 0.35);
  transform: translateY(-1px);
}

.custom-upload-btn.el-button:active:not(.is-disabled) {
  transform: translateY(0);
  box-shadow: 0 2px 6px rgba(59, 130, 246, 0.2);
}

.custom-upload-btn.is-disabled {
  background: #e5e7eb;
  color: #9ca3af;
  box-shadow: none;
  cursor: not-allowed;
  border: 1px solid #d1d5db;
}

.add-form-question-btn.el-button {
  height: 40px;
  padding: 0 18px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
  border: none;
  border-radius: 10px;
  box-shadow: 0 3px 10px rgba(59, 130, 246, 0.25);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  gap: 6px;
  letter-spacing: 0.5px;
  white-space: nowrap;
}

.add-form-question-btn.el-button {
  background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
  box-shadow: 0 3px 10px rgba(59, 130, 246, 0.25);
}

.add-form-question-btn.el-button:hover {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.35);
  transform: translateY(-2px);
}

.btn-icon {
  font-size: 17px;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.custom-upload-btn.el-button:hover .btn-icon {
  transform: scale(1.08) rotate(3deg);
}

.upload-demo :deep(.el-upload-dragger) {
  border-radius: 12px;
  border: 2px dashed #d1d5db;
  background-color: #f9fafb;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.upload-demo :deep(.el-upload-dragger:hover) {
  border-color: #60a5fa;
  background-color: #eff6ff;
}

/* 已上傳文件列表 */
.upload-demo :deep(.el-upload-list) {
  margin-top: 12px;
}

.upload-demo :deep(.el-upload-list__item) {
  border-radius: 8px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.upload-demo :deep(.el-upload-list__item:hover) {
  background-color: #f3f4f6;
}

</style>
