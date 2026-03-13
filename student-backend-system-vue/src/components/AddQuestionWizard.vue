<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="90vw"
    :before-close="handleClose"
    class="add-question-dialog"
    top="1vh"
    destroy-on-close
  >
    <div class="dialog-body">
      <!-- 题目类型选择 -->
      <div class="type-selector-section">
        <div class="section-header">
          <div class="header-icon">
            <el-icon :size="32"><List /></el-icon>
          </div>
          <div class="header-text">
            <h3 class="title">选择题目类型</h3>
            <p class="subtitle">请选择一种题目类型</p>
          </div>
        </div>

        <div class="type-grid">
          <!-- 单选题 -->
          <div 
            class="type-card"
            :class="{ selected: selectedType === '1' }"
            @click="selectType('1')"
          >
            <div class="card-icon single">
              <el-icon :size="36"><CircleCheck /></el-icon>
            </div>
            <h4 class="card-title">单选题</h4>
            <p class="card-desc">只有一个正确答案</p>
            <div class="selected-mark" v-if="selectedType === '1'">
              <el-icon :size="20"><Check /></el-icon>
            </div>
          </div>

          <!-- 多选题 -->
          <div 
            class="type-card"
            :class="{ selected: selectedType === '2' }"
            @click="selectType('2')"
          >
            <div class="card-icon multiple">
              <el-icon :size="36"><Checked /></el-icon>
            </div>
            <h4 class="card-title">多选题</h4>
            <p class="card-desc">可有多个正确答案</p>
            <div class="selected-mark" v-if="selectedType === '2'">
              <el-icon :size="20"><Check /></el-icon>
            </div>
          </div>

          <!-- 填空题 -->
          <div 
            class="type-card"
            :class="{ selected: selectedType === '3' }"
            @click="selectType('3')"
          >
            <div class="card-icon fill">
              <el-icon :size="36"><EditPen /></el-icon>
            </div>
            <h4 class="card-title">填空题</h4>
            <p class="card-desc">填写文字或数字</p>
            <div class="selected-mark" v-if="selectedType === '3'">
              <el-icon :size="20"><Check /></el-icon>
            </div>
          </div>

          <!-- 附加题 -->
          <div 
            class="type-card"
            :class="{ selected: selectedType === '4' }"
            @click="selectType('4')"
          >
            <div class="card-icon attachment">
              <el-icon :size="36"><Paperclip /></el-icon>
            </div>
            <h4 class="card-title">附加题</h4>
            <p class="card-desc">上传附件文件</p>
            <div class="selected-mark" v-if="selectedType === '4'">
              <el-icon :size="20"><Check /></el-icon>
            </div>
          </div>
        </div>
      </div>

      <!-- 题目内容和设置 -->
      <div class="question-content-section" v-if="selectedType">
        <div class="section-divider"></div>
        
        <!-- 单选题编辑器 -->
        <SingleChoiceEditor
          ref="editorComponent"
          v-if="selectedType === '1'"
          v-model:question-data="questionData"
          @save="handleSave"
        />
        
        <!-- 多选题编辑器 -->
        <MultipleChoiceEditor
          ref="editorComponent"
          v-else-if="selectedType === '2'"
          v-model:question-data="questionData"
          @save="handleSave"
        />
        
        <!-- 填空题编辑器 -->
        <FillInBlankEditor
          ref="editorComponent"
          v-else-if="selectedType === '3'"
          v-model:question-data="questionData"
          @save="handleSave"
        />
        
        <!-- 附加题编辑器 -->
        <AttachmentEditor
          ref="editorComponent"
          v-else-if="selectedType === '4'"
          v-model:question-data="questionData"
          @save="handleSave"
        />
      </div>

      <!-- 底部操作按钮 -->
      <div class="dialog-footer" v-if="selectedType">
        <el-button @click="handleClose" class="cancel-btn">取消</el-button>
        <el-button type="primary" @click="triggerSave" class="save-btn">
          保存题目
          <el-icon><Check /></el-icon>
        </el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script>
import { CircleCheck, Checked, EditPen, Paperclip, Check, List, ArrowRight } from '@element-plus/icons-vue'
import SingleChoiceEditor from './question-editors/SingleChoiceEditor.vue'
import MultipleChoiceEditor from './question-editors/MultipleChoiceEditor.vue'
import FillInBlankEditor from './question-editors/FillInBlankEditor.vue'
import AttachmentEditor from './question-editors/AttachmentEditor.vue'

export default {
  name: 'AddQuestionWizard',
  components: {
    SingleChoiceEditor,
    MultipleChoiceEditor,
    FillInBlankEditor,
    AttachmentEditor
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    question: {
      type: Object,
      default: null
    }
  },
  emits: ['update:visible', 'save'],
  data() {
    return {
      selectedType: '',
      questionData: {
        type: '',
        title: '',
        options: null,
        required: false,
        attachments: null
      }
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
    dialogTitle() {
      return this.question ? '编辑问题' : '添加问题'
    }
  },
  watch: {
    visible(newVal) {
      if (newVal) {
        this.initForm()
      }
    }
  },
  methods: {
    initForm() {
      if (this.question) {
        this.selectedType = this.question.type
        this.questionData = {
          type: this.question.type,
          title: this.question.title,
          options: this.question.options ? [...this.question.options] : null,
          required: this.question.required || false,
          attachments: this.question.attachments ? [...this.question.attachments] : null
        }
      } else {
        this.selectedType = ''
        this.questionData = {
          type: '',
          title: '',
          options: null,
          required: false,
          attachments: null
        }
      }
    },

    selectType(type) {
      this.selectedType = type
    },

    triggerSave() {
      // 调用当前编辑器组件的保存方法
      if (this.$refs.editorComponent) {
        this.$refs.editorComponent.handleSave()
      }
    },

    handleSave(questionData) {
      this.$emit('save', questionData)
      this.$emit('update:visible', false)
    },

    handleClose() {
      this.$emit('update:visible', false)
    }
  }
}
</script>

<style scoped>
.add-question-dialog {
  border-radius: 16px;
  overflow: hidden;
}

:deep(.el-dialog__header) {
  padding: 20px 28px;
  background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
  color: white;
  border-bottom: none;
}

:deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

:deep(.el-dialog__headerbtn) {
  color: white;
}

:deep(.el-dialog__headerbtn:hover) {
  opacity: 0.85;
}

:deep(.el-dialog__body) {
  padding: 0;
  background: #f9fafb;
  overflow-y: auto;
}

.dialog-body {
  display: flex;
  flex-direction: column;
}

/* 类型选择器区域 */
.type-selector-section {
  padding: 24px 28px 0 28px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border-radius: 12px;
  border: 2px solid #bfdbfe;
  margin-bottom: 20px;
}

.header-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.25);
}

.header-text {
  flex: 1;
}

.title {
  font-size: 17px;
  font-weight: 600;
  color: #1e40af;
  margin: 0 0 4px 0;
}

.subtitle {
  font-size: 13px;
  color: #6b7280;
  margin: 0;
}

/* 类型网格 */
.type-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 14px;
  margin-bottom: 0;
}

.type-card {
  position: relative;
  padding: 22px 16px;
  background: linear-gradient(135deg, #ffffff 0%, #f9fafb 100%);
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 10px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
}

.type-card:hover {
  border-color: #60a5fa;
  box-shadow: 0 6px 18px rgba(59, 130, 246, 0.15);
  transform: translateY(-3px);
}

.type-card.selected {
  border-color: #3b82f6;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.25);
  transform: translateY(-3px);
}

.card-icon {
  width: 60px;
  height: 60px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.card-icon.single {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.25);
}

.card-icon.multiple {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  box-shadow: 0 4px 10px rgba(16, 185, 129, 0.25);
}

.card-icon.fill {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: white;
  box-shadow: 0 4px 10px rgba(245, 158, 11, 0.25);
}

.card-icon.attachment {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
  box-shadow: 0 4px 10px rgba(239, 68, 68, 0.25);
}

.type-card.selected .card-icon {
  transform: scale(1.08);
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.type-card.selected .card-title {
  color: #1e40af;
}

.card-desc {
  font-size: 12px;
  color: #6b7280;
  margin: 0;
  line-height: 1.4;
}

/* 分割线 */
.section-divider {
  height: 2px;
  background: linear-gradient(90deg, transparent 0%, #e5e7eb 50%, transparent 100%);
  margin: 20px 0;
}

/* 题目内容区域 */
.question-content-section {
  padding: 0 28px 24px 28px;
}

/* 底部操作栏 */
.dialog-footer {
  display: flex;
  justify-content: space-between;
  padding: 16px 28px;
  background: #ffffff;
  border-top: 2px solid #e5e7eb;
}

.cancel-btn {
  height: 40px;
  padding: 0 24px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 10px;
}

.save-btn {
  height: 40px;
  padding: 0 24px;
  font-size: 14px;
  font-weight: 600;
  color: #ffffff !important;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  border-radius: 10px;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  gap: 6px;
}

.save-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.45);
  transform: translateY(-2px);
}

.save-btn:active:not(:disabled) {
  transform: translateY(0) scale(0.97);
  box-shadow: 0 3px 10px rgba(59, 130, 246, 0.25);
}

.save-btn .el-icon {
  font-size: 16px;
}

/* 底部操作栏 */
.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 28px;
  background: #ffffff;
  border-top: 2px solid #e5e7eb;
  gap: 12px;
}

.cancel-btn,
.save-btn {
  white-space: nowrap;
}

.cancel-btn {
  height: 40px;
  padding: 0 24px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 10px;
}

.save-btn {
  height: 40px;
  padding: 0 24px;
  font-size: 14px;
  font-weight: 600;
  color: #ffffff !important;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  border-radius: 10px;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  gap: 6px;
}

.save-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.45);
  transform: translateY(-2px);
}

.save-btn:active:not(:disabled) {
  transform: translateY(0) scale(0.97);
  box-shadow: 0 3px 10px rgba(59, 130, 246, 0.25);
}

.save-btn .el-icon {
  font-size: 16px;
}

.selected-mark {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(16, 185, 129, 0.3);
  animation: checkAppear 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes checkAppear {
  0% {
    transform: scale(0);
  }
  50% {
    transform: scale(1.15);
  }
  100% {
    transform: scale(1);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  :deep(.el-dialog) {
    width: 92% !important;
  }
  
  .step-content {
    padding: 20px;
  }
  
  .type-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .type-selector-header {
    flex-direction: column;
    text-align: center;
  }
}
</style>
