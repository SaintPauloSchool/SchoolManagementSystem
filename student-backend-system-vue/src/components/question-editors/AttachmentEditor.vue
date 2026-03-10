<template>
  <div class="attachment-editor">
    <el-form
      ref="formRef"
      :model="localQuestionData"
      :rules="rules"
      label-width="100px"
      label-position="left"
    >
      <!-- 題目 -->
      <el-form-item label="題目內容" prop="title">
        <div class="title-input-wrapper">
          <el-input
            v-model="localQuestionData.title"
            placeholder="請輸入題目內容，最多 200 字"
            maxlength="200"
            show-word-limit
            clearable
            class="title-input"
            size="large"
          >
            <template #prefix>
              <el-icon class="title-icon"><ChatDotRound /></el-icon>
            </template>
          </el-input>
        </div>
      </el-form-item>

      <!-- 附件上傳 -->
      <el-form-item label="附件上傳" required>
        <div class="upload-section">
          <div class="upload-header">
            <span class="upload-label">
              <el-icon class="label-icon"><Paperclip /></el-icon>
              請上傳附件文件
            </span>
            <el-tag type="info" size="small" effect="light">
              支持多文件上傳，最多 5 個
            </el-tag>
          </div>
          
          <el-upload
            class="question-upload"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :file-list="fileList"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :on-remove="handleRemove"
            :before-upload="beforeUpload"
            :limit="5"
            multiple
          >
            <el-button class="custom-upload-btn" size="large">
              <el-icon class="btn-icon"><Upload /></el-icon>
              <span class="btn-text">點擊上傳附件</span>
            </el-button>
          </el-upload>
          
          <div class="upload-hint">
            <el-icon><InfoFilled /></el-icon>
            <span>支持 PDF、Word、Excel、圖片等格式，單個文件不超過 50MB</span>
          </div>
        </div>
      </el-form-item>

      <!-- 必答設置 -->
      <el-form-item label="必填" prop="required">
        <div class="required-setting">
          <div class="required-toggle">
            <el-switch
              v-model="localQuestionData.required"
              class="required-switch"
              inline-prompt
              active-text="必"
              inactive-text="選"
            />
            <span class="required-hint" :class="{ 'is-required': localQuestionData.required }">
              <el-icon class="hint-icon"><Bell /></el-icon>
              {{ localQuestionData.required ? '此題為必答' : '此題為選答' }}
            </span>
          </div>
        </div>
      </el-form-item>
    </el-form>

    <div class="action-buttons">
      <el-button @click="$emit('prev')" class="prev-btn">
        <el-icon><ArrowLeft /></el-icon>
        <span class="btn-text">上一步</span>
      </el-button>
      <el-button type="primary" @click="handleSave" class="save-btn" :disabled="fileList.length === 0">
        <el-icon><Check /></el-icon>
        <span class="btn-text">儲存題目</span>
      </el-button>
    </div>
  </div>
</template>

<script>
import { ChatDotRound, Paperclip, Upload, InfoFilled, Bell, ArrowLeft } from '@element-plus/icons-vue'

export default {
  name: 'AttachmentEditor',
  components: {
    ChatDotRound,
    Paperclip,
    Upload,
    InfoFilled,
    Bell,
    ArrowLeft
  },
  props: {
    questionData: {
      type: Object,
      required: true
    }
  },
  emits: ['prev', 'save', 'update:questionData'],
  data() {
    return {
      localQuestionData: {
        type: '4',
        title: this.questionData.title || '',
        attachments: [],
        required: this.questionData.required || false
      },
      fileList: this.questionData.attachments ? [...this.questionData.attachments] : [],
     uploadUrl: '/api/common/upload',
     uploadHeaders: {
        'Authorization': 'Bearer' + localStorage.getItem('token')
      },
      rules: {
        title: [
          { required: true, message: '請輸入題目內容', trigger: 'blur' },
          { max: 200, message: '標題長度不能超過 200 個字符', trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    beforeUpload(file) {
     const maxSize = 50 * 1024 * 1024 // 50MB
     if (file.size > maxSize) {
        this.$message.error('文件大小不能超过 50MB')
        return false
      }
      return true
    },

    handleUploadSuccess(response, file, fileList) {
     if (response.code === 0 || response.code === 200) {
        this.$message.success('上传成功')
        this.fileList = fileList.filter(f => 
          f.status === 'success' || f.response?.code === 0 || f.response?.code === 200
        )
      } else {
        this.$message.error(response.msg || '上传失败')
      }
    },

    handleUploadError(error, file, fileList) {
     console.error('上传错误:', error)
      this.$message.error('上传失败：' + (error.message || '未知错误'))
    },

    handleRemove(file, fileList) {
      this.fileList = fileList
    },

    handleSave() {
      this.$refs.formRef.validate((valid) => {
      if (valid) {
        if (this.fileList.length === 0) {
            this.$message.warning('请至少上传一个附件')
            return
          }
          
        const attachments = this.fileList.map(file => ({
           name: file.name,
            url: file.url || file.response?.data?.url
          }))
          
        const data = {
            ...this.localQuestionData,
            attachments: attachments,
            options: attachments // 将附件信息存入 options
          }
          
          this.$emit('save', data)
        }
      })
    }
  }
}
</script>

<style scoped>
.attachment-editor {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.title-input-wrapper {
  width: 100%;
}

.title-input :deep(.el-input__wrapper) {
  border-radius: 12px;
  border: 2px solid #e5e7eb;
  padding: 14px 18px;
  background: linear-gradient(135deg, #f9fafb 0%, #ffffff 100%);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.title-input :deep(.el-input__wrapper:hover) {
  border-color: #ef4444;
  background: #ffffff;
  box-shadow: 0 4px 16px rgba(239, 68, 68, 0.15);
  transform: translateY(-1px);
}

.title-input :deep(.el-input__wrapper.is-focus) {
  border-color: #dc2626;
  background: #ffffff;
  box-shadow: 0 0 0 5px rgba(239, 68, 68, 0.1), 0 6px 20px rgba(239, 68, 68, 0.2);
}

.title-icon {
  color: #ef4444;
  font-size: 18px;
  animation: bounce 2s ease-in-out infinite;
}

@keyframes bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-3px);
  }
}

.upload-section {
  width: 100%;
  padding: 24px;
  background: linear-gradient(135deg, #fef2f2 0%, #ffffff 100%);
  border-radius: 16px;
  border: 2px solid #fecaca;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.upload-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 2px solid #fecaca;
}

.upload-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #991b1b;
  font-size: 14px;
  letter-spacing: 0.3px;
}

.label-icon {
  color: #dc2626;
  font-size: 18px;
  animation: rotate 3s ease-in-out infinite;
}

@keyframes rotate {
  0%, 100% {
    transform: rotate(0deg);
  }
  50% {
    transform: rotate(15deg);
  }
}

.custom-upload-btn {
  height: 46px;
  padding: 0 28px;
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  border: none;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(239, 68, 68, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  gap: 10px;
}

.custom-upload-btn:hover {
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  box-shadow: 0 6px 24px rgba(239, 68, 68, 0.45);
  transform: translateY(-2px) scale(1.02);
}

.btn-icon {
  font-size: 20px;
  animation: upload 2s ease-in-out infinite;
}

@keyframes upload {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-4px);
  }
}

.upload-hint {
  margin-top: 16px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
  border-radius: 8px;
  border: 1px solid #fca5a5;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #991b1b;
  font-weight: 500;
}

.upload-hint .el-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.required-setting {
  width: 100%;
}

.required-toggle {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  padding: 16px 20px;
  background: linear-gradient(135deg, #fef2f2 0%, #ffffff 100%);
  border-radius: 14px;
  border: 2px solid #fecaca;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.required-switch :deep(.el-switch__core) {
  border-radius: 12px;
  height: 28px;
  background: linear-gradient(135deg, #e5e7eb 0%, #d1d5db 100%);
  border: 2px solid #d1d5db;
  transition: all 0.3s ease;
}

.required-switch :deep(.el-switch.is-checked .el-switch__core) {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  border-color: #ef4444;
  box-shadow: 0 2px 12px rgba(239, 68, 68, 0.4);
}

.required-hint {
  flex: 1;
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s ease;
}

.hint-icon {
  font-size: 18px;
  color: #9ca3af;
  transition: all 0.3s ease;
}

.required-hint.is-required {
  color: #ef4444;
  font-weight: 600;
}

.required-hint.is-required .hint-icon {
  color: #ef4444;
  animation: ring 1.5s ease-in-out infinite;
}

@keyframes ring {
  0%, 100% {
    transform: rotate(0deg);
  }
  25% {
    transform: rotate(-10deg);
  }
  75% {
    transform: rotate(10deg);
  }
}

:deep(.el-form-item__label) {
  font-weight: 700;
  color: #374151;
  font-size: 15px;
  letter-spacing: 0.5px;
  padding-left: 4px;
}

.action-buttons {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding-top: 24px;
  border-top: 2px solid #e5e7eb;
}

.prev-btn {
  height: 46px;
  padding: 0 32px;
  font-size: 15px;
  font-weight: 600;
  color: #6b7280;
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  border: 2px solid #d1d5db;
  border-radius: 12px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  gap: 8px;
}

.prev-btn:hover {
  background: linear-gradient(135deg, #e5e7eb 0%, #d1d5db 100%);
  color: #374151;
  border-color: #9ca3af;
  box-shadow: 0 3px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.save-btn {
  height: 46px;
  padding: 0 32px;
  font-size: 15px;
  font-weight: 700;
  color: #ffffff !important;
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  border: none;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(239, 68, 68, 0.35);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  gap: 8px;
}

.save-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  box-shadow: 0 6px 24px rgba(239, 68, 68, 0.5);
  transform: translateY(-2px) scale(1.02);
}

.save-btn:active:not(:disabled) {
  transform: translateY(0) scale(0.98);
  box-shadow: 0 2px 10px rgba(239, 68, 68, 0.25);
}

.save-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
