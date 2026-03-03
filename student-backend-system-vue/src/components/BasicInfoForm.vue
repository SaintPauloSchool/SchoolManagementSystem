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
      <el-form-item label="发送人" prop="senderName">
        <el-input
          v-model="localFormData.senderName"
          disabled
          placeholder="系統自動填充當前登錄用戶"
        />
        <div class="form-tip">发送人默認為當前登錄用戶，不可修改</div>
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
        <el-upload
          class="upload-demo"
          :action="uploadUrl"
          :headers="uploadHeaders"
          :file-list="fileList"
          :on-success="handleUploadSuccess"
          :on-remove="handleUploadRemove"
          :before-upload="beforeUpload"
          multiple
          :limit="5"
        >
          <el-button size="small" type="primary">
            <el-icon><Upload /></el-icon>
            點擊上傳
          </el-button>
          <template #tip>
            <div class="el-upload__tip">
              支持上傳 jpg/png/gif/pdf/doc/docx/xls/xlsx 文件，最多 5 個文件，單個文件不超過 10MB
            </div>
          </template>
        </el-upload>
      </el-form-item>

      <!-- 問題設置 -->
      <el-form-item label="問題設置">
        <div class="questions-section">
          <div class="questions-header">
            <span>已添加的問題 ({{ localFormData.questions.length }})</span>
            <el-button 
              type="primary" 
              size="small" 
              @click="showQuestionDialog = true"
            >
              <el-icon><Plus /></el-icon>
              添加問題
            </el-button>
          </div>
          
          <div v-if="localFormData.questions.length > 0" class="questions-list">
            <div 
              v-for="(question, index) in localFormData.questions" 
              :key="question.id"
              class="question-item"
            >
              <div class="question-info">
                <span class="question-number">{{ index + 1 }}.</span>
                <span class="question-title">{{ question.title }}</span>
                <el-tag 
                  size="small" 
                  :type="getQuestionTypeColor(question.type)"
                  class="question-type-tag"
                >
                  {{ getQuestionTypeText(question.type) }}
                </el-tag>
              </div>
              <div class="question-actions">
                <el-button size="small" @click="editQuestion(index)">
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

    <!-- 添加/編輯問題對話框 -->
    <QuestionDialog
      v-model:visible="showQuestionDialog"
      :question="editingQuestion"
      :question-type="questionType"
      @save="saveQuestion"
    />
  </div>
</template>

<script>
import { Upload, Plus, Edit, Delete, ArrowRight } from '@element-plus/icons-vue'
import QuestionDialog from './QuestionDialog.vue'

export default {
  name: 'BasicInfoForm',
  components: {
    QuestionDialog
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
      uploadUrl: '/api/common/upload',
      uploadHeaders: {
        'Authorization': 'Bearer ' + localStorage.getItem('token')
      },
      showQuestionDialog: false,
      editingQuestion: null,
      questionType: '',
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
  watch: {
    formData: {
      handler(newVal) {
        this.localFormData = { ...newVal }
        this.initFileList()
      },
      deep: true
    }
  },
  mounted() {
    this.initFileList()
  },
  methods: {
    initFileList() {
      if (this.localFormData.attachmentUrls && this.localFormData.attachmentUrls.length > 0) {
        try {
          const urls = typeof this.localFormData.attachmentUrls === 'string' 
            ? JSON.parse(this.localFormData.attachmentUrls)
            : this.localFormData.attachmentUrls
            
          this.fileList = urls.map((url, index) => ({
            name: `附件${index + 1}`,
            url: url
          }))
        } catch (e) {
          this.fileList = []
        }
      } else {
        this.fileList = []
      }
    },

    validate() {
      return new Promise((resolve, reject) => {
        this.$refs.formRef.validate((valid) => {
          if (valid) {
            resolve()
          } else {
            reject()
          }
        })
      })
    },

    resetForm() {
      this.$refs.formRef.resetFields()
      this.fileList = []
      this.localFormData = {
        title: '',
        content: '',
        senderName: '',
        jumpUrl: '',
        attachmentUrls: [],
        questions: []
      }
    },

    goToNext() {
      this.validate().then(() => {
        Object.assign(this.formData, this.localFormData)
        this.$emit('next')
      }).catch(() => {
        this.$message.warning('請完善基本信息')
      })
    },

    beforeUpload(file) {
      const isLt10M = file.size / 1024 / 1024 < 10
      if (!isLt10M) {
        this.$message.error('上傳文件大小不能超過 10MB!')
        return false
      }
      return true
    },

    handleUploadSuccess(response, file) {
      if (response.code === 0) {
        const url = response.data.url
        this.localFormData.attachmentUrls.push(url)
        this.fileList.push({
          name: file.name,
          url: url
        })
        this.$message.success('上傳成功')
      } else {
        this.$message.error(response.msg || '上傳失敗')
      }
    },

    handleUploadRemove(file) {
      const index = this.localFormData.attachmentUrls.indexOf(file.url)
      if (index > -1) {
        this.localFormData.attachmentUrls.splice(index, 1)
      }
      
      const fileIndex = this.fileList.findIndex(item => item.url === file.url)
      if (fileIndex > -1) {
        this.fileList.splice(fileIndex, 1)
      }
    },

    getQuestionTypeText(type) {
      const typeMap = {
        '1': '單選',
        '2': '多選',
        '3': '填空',
        '4': '附件上傳',
        '5': '分支'
      }
      return typeMap[type] || '未知'
    },

    getQuestionTypeColor(type) {
      const colorMap = {
        '1': 'primary',
        '2': 'success',
        '3': 'warning',
        '4': 'danger',
        '5': 'info'
      }
      return colorMap[type] || 'info'
    },

    editQuestion(index) {
      this.editingQuestion = { ...this.localFormData.questions[index] }
      this.questionType = this.editingQuestion.type
      this.showQuestionDialog = true
    },

    removeQuestion(index) {
      this.$confirm('確認刪除此問題嗎？', '提示', {
        confirmButtonText: '確定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.localFormData.questions.splice(index, 1)
        this.$message.success('刪除成功')
      }).catch(() => {})
    },

    saveQuestion(question) {
      if (this.editingQuestion) {
        const index = this.localFormData.questions.findIndex(q => q.id === this.editingQuestion.id)
        if (index > -1) {
          this.localFormData.questions.splice(index, 1, question)
        }
      } else {
        question.id = Date.now()
        this.localFormData.questions.push(question)
      }
      
      this.editingQuestion = null
      this.showQuestionDialog = false
      this.$message.success('保存成功')
    }
  }
}
</script>

<style scoped>
.basic-info-form {
  width: 100%;
}

.form-container .el-form-item__label {
  font-weight: 600;
  color: #374151;
}

.form-tip {
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
  line-height: 1.5;
}

.questions-section {
  width: 100%;
}

.questions-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 14px 18px;
  background: linear-gradient(135deg, #f9fafb 0%, #ffffff 100%);
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.questions-header span {
  font-weight: 600;
  color: #111827;
}

.questions-list {
  min-height: 80px;
}

.question-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  background: linear-gradient(135deg, #f9fafb 0%, #ffffff 100%);
  border-radius: 8px;
  margin-bottom: 10px;
  border: 1px solid #e5e7eb;
  transition: all 0.2s ease;
}

.question-item:hover {
  background: #f9fafb;
  border-color: #d1d5db;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.question-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.question-number {
  font-weight: 700;
  color: #3b82f6;
  font-size: 14px;
  min-width: 24px;
}

.question-title {
  font-size: 14px;
  color: #374151;
  flex: 1;
}

.question-actions {
  display: flex;
  gap: 8px;
}

.no-questions {
  text-align: center;
  color: #6b7280;
  padding: 32px;
  font-size: 14px;
  background: #f9fafb;
  border-radius: 8px;
  border: 1px dashed #e5e7eb;
}

.form-actions {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e5e7eb;
  text-align: right;
}

.upload-demo {
  width: 100%;
}

.question-type-tag.el-tag {
  font-size: 12px;
  padding: 2px 8px;
}

/* 響應式設計 */
@media (max-width: 768px) {
  .form-container {
    padding: 0;
  }
  
  .questions-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .question-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .question-actions {
    width: 100%;
  }
  
  .form-actions {
    text-align: center;
  }
}
</style>
