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
          placeholder="系統自動填充當前登錄用戶"
        />
        <div class="form-tip">發送人默認為當前登錄用戶，不可修改</div>
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
            :action="uploadUrl"
            :headers="uploadHeaders"
            :file-list="fileList"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :on-remove="handleUploadRemove"
            :before-upload="beforeUpload"
            :on-change="handleChange"
            multiple
            :limit="5"
          >
            <el-button class="custom-upload-btn" size="large">
              <el-icon class="btn-icon"><Upload /></el-icon>
              <span class="btn-text">點擊上傳附件</span>
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
import { Upload, Edit, Delete, ArrowRight } from '@element-plus/icons-vue'
import FormQuestionDialog from './FormQuestionDialog.vue'
import { ElMessage } from 'element-plus'
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
      uploadUrl: '/api/common/upload',
      uploadHeaders: {},
      showFormQuestionDialog: false,
      editingFormQuestion: null,
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
    // 初始化上传头
    this.uploadHeaders = {
      'Authorization': 'Bearer ' + localStorage.getItem('token')
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
            
          // 只在 fileList 为空或有变化时才初始化，保留已有的文件名
          if (!this.fileList || this.fileList.length === 0) {
            // 如果是空的，创建新的 fileList，使用默认名称
            this.fileList = urls.map((url, index) => ({
              name: `附件${index + 1}`,
              url: url
            }))
          } else {
            // 如果 fileList 已有数据，只更新 URL，不改变文件名
            const newFileList = []
            urls.forEach((url, index) => {
              // 查找是否有相同 URL 的文件
              const existingFile = this.fileList.find(f => f.url === url)
              if (existingFile) {
                // 保留原有文件名和 uid
                newFileList.push({
                  ...existingFile,
                  url: url
                })
              } else if (index < this.fileList.length) {
                // 如果没有找到匹配的 URL，但索引存在，保留原文件名
                newFileList.push({
                  ...this.fileList[index],
                  url: url
                })
              } else {
                // 如果是新增的 URL，使用默认名称
                newFileList.push({
                  name: `附件${index + 1}`,
                  url: url
                })
              }
            })
            this.fileList = newFileList
          }
        } catch (e) {
          console.error('初始化文件列表失败:', e)
          this.fileList = []
        }
      } else {
        this.fileList = []
      }
    },

    goToNext() {
      this.$refs.formRef.validate((valid) => {
        if (valid) {
          Object.assign(this.formData, this.localFormData)
          this.$emit('next')
        } else {
          ElMessage.warning('請完善基本信息')
        }
      })
    },

    beforeUpload(file) {
      const isLt10M = file.size / 1024 / 1024 < 10
      if (!isLt10M) {
        ElMessage.error('上傳文件大小不能超過 10MB!')
        return false
      }
      return true
    },

    handleUploadSuccess(response, file) {
      if (response.code === 0) {
        const url = response.data.url
        const uploadedFile = this.fileList.find(f => f.uid === file.uid)
        if (uploadedFile) {
          uploadedFile.url = url
          // 保留原始文件名，不要覆盖
          if (!uploadedFile.name || uploadedFile.name.startsWith('附件')) {
            uploadedFile.name = file.name
          }
        } else {
          this.fileList.push({
            name: file.name,
            url: url,
            uid: file.uid
          })
        }
        
        const urls = this.fileList.map(f => f.url).filter(url => url)
        this.localFormData.attachmentUrls = urls
        
        ElMessage.success('上傳成功')
      } else {
        ElMessage.error(response.msg || '上傳失敗')
        const index = this.fileList.findIndex(f => f.uid === file.uid)
        if (index > -1) {
          this.fileList.splice(index, 1)
        }
      }
    },

    handleUploadError(error, file) {
      console.error('上传失败:', error)
      ElMessage.error('上傳失敗，請重試')
      const index = this.fileList.findIndex(f => f.uid === file.uid)
      if (index > -1) {
        this.fileList.splice(index, 1)
      }
    },

    handleChange(file, fileList) {
      // 保留原始文件名，不要用 Element Plus 的默认名称
      this.fileList = fileList.filter(f => f.status !== 'removed').map(f => {
        // 如果是新添加的文件且名字是默认的，使用原始文件名
        if (!f.name || f.name === file.name && f.raw && f.raw.name) {
          f.name = f.raw.name
        }
        return f
      })
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




    
    addFormQuestion() {
      this.editingFormQuestion = null
      this.showFormQuestionDialog = true
    },
    
    editFormQuestion(index) {
      const questionData = this.localFormData.questions[index]
      // 传递完整的 questionnaireData 和 questions 数据
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
            questionnaireData: saveData.questionnaire, // 保存完整的問卷數據
            questions: saveData.questions || [] // 保存所有子問題
          }
          this.localFormData.questions[index] = formQuestion
          ElMessage.success('更新表單問題成功')
        }
      } else {
        // 新增模式：創建新問題
        const newQuestion = {
          id: Date.now(),
          questionType: '5',
          type: '5',
          title: saveData.questionnaire?.title || '問卷調查',
          description: saveData.questionnaire?.description || '',
          questionnaireData: saveData.questionnaire, // 保存完整的問卷數據
          questions: saveData.questions || [] // 保存所有子問題
        }
        this.localFormData.questions.push(newQuestion)
        ElMessage.success('添加表單問題成功')
      }
      
      // 同步到父組件的 formData
      Object.assign(this.formData, this.localFormData)
      
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
        ElMessage.success('刪除成功')
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
  text-align: right;
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

.custom-upload-btn.el-button:hover {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  box-shadow: 0 5px 16px rgba(59, 130, 246, 0.35);
  transform: translateY(-1px);
}

.custom-upload-btn.el-button:active {
  transform: translateY(0);
  box-shadow: 0 2px 6px rgba(59, 130, 246, 0.2);
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
