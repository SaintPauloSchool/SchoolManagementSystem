<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="800px"
    :before-close="handleClose"
    class="add-question-wizard"
    top="5vh"
    destroy-on-close
  >
    <div class="wizard-content">
      <!-- 步驟指示器 -->
      <div class="steps-wrapper">
        <div class="simple-steps">
          <div 
            :class="['step-item', { active: currentStep === 0, completed: currentStep > 0 }]"
            @click="currentStep > 0 ? goToStep(0) : null"
          >
            <span class="step-number">{{ currentStep > 0 ? '✓' : '1' }}</span>
            <span class="step-title">選擇題型</span>
          </div>
          <div class="step-line" :class="{ completed: currentStep > 0 }"></div>
          <div 
            :class="['step-item', { active: currentStep === 1 }]"
          >
            <span class="step-number">2</span>
            <span class="step-title">填寫內容</span>
          </div>
        </div>
      </div>

      <!-- 表單內容 -->
      <div class="form-wrapper">
        <transition name="slide" mode="out-in">
          <div :key="currentStep" class="step-content">
            <!-- 第一步：選擇題型 -->
            <QuestionTypeSelector
              v-if="currentStep === 0"
              v-model:selected-type="selectedType"
              @next="goToStep(1)"
            />
            
            <!-- 第二步：根據題型顯示不同的編輯組件 -->
            <SingleChoiceEditor
              v-else-if="currentStep === 1 && selectedType === '1'"
              v-model:question-data="questionData"
              @prev="goToStep(0)"
              @save="handleSave"
            />
            <MultipleChoiceEditor
              v-else-if="currentStep === 1 && selectedType === '2'"
              v-model:question-data="questionData"
              @prev="goToStep(0)"
              @save="handleSave"
            />
            <FillInBlankEditor
              v-else-if="currentStep === 1 && selectedType === '3'"
              v-model:question-data="questionData"
              @prev="goToStep(0)"
              @save="handleSave"
            />
            <AttachmentEditor
              v-else-if="currentStep === 1 && selectedType === '4'"
              v-model:question-data="questionData"
              @prev="goToStep(0)"
              @save="handleSave"
            />
            <BranchEditor
              v-else-if="currentStep === 1 && selectedType === '5'"
              v-model:question-data="questionData"
              @prev="goToStep(0)"
              @save="handleSave"
            />
          </div>
        </transition>
      </div>
    </div>
  </el-dialog>
</template>

<script>
import QuestionTypeSelector from './question-editors/QuestionTypeSelector.vue'
import SingleChoiceEditor from './question-editors/SingleChoiceEditor.vue'
import MultipleChoiceEditor from './question-editors/MultipleChoiceEditor.vue'
import FillInBlankEditor from './question-editors/FillInBlankEditor.vue'
import AttachmentEditor from './question-editors/AttachmentEditor.vue'
import BranchEditor from './question-editors/BranchEditor.vue'

export default {
  name: 'AddQuestionWizard',
  components: {
    QuestionTypeSelector,
    SingleChoiceEditor,
    MultipleChoiceEditor,
    FillInBlankEditor,
    AttachmentEditor,
    BranchEditor
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
      currentStep: 0,
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
      return this.question ? '編輯問題' : '添加問題'
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
        this.currentStep = 1
      } else {
        this.selectedType = ''
        this.questionData = {
          type: '',
          title: '',
          options: null,
          required: false,
          attachments: null
        }
        this.currentStep = 0
      }
    },

    goToStep(step) {
      this.currentStep = step
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
.add-question-wizard {
  border-radius: 20px;
  overflow: hidden;
}

:deep(.el-dialog__header) {
  padding: 24px 36px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
}

:deep(.el-dialog__title) {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

:deep(.el-dialog__headerbtn) {
  color: white;
}

:deep(.el-dialog__headerbtn:hover) {
  opacity: 0.8;
}

:deep(.el-dialog__body) {
  padding: 0;
  background: linear-gradient(180deg, #ffffff 0%, #fafbfc 100%);
}

.wizard-content {
  display: flex;
  flex-direction: column;
}

.steps-wrapper {
  padding: 24px 36px;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
}

.simple-steps {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 40px;
  max-width: 500px;
  margin: 0 auto;
}

.step-item {
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.step-item:not(.active):not(.completed):hover {
  transform: scale(1.05);
}

.step-number {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #e5e7eb 0%, #d1d5db 100%);
  color: #6b7280;
  font-weight: 700;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.step-item.active .step-number {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
  transform: scale(1.1);
}

.step-item.completed .step-number {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);
}

.step-title {
  font-size: 13px;
  color: #6b7280;
  font-weight: 500;
  transition: all 0.3s ease;
}

.step-item.active .step-title {
  color: #3b82f6;
  font-weight: 700;
}

.step-item.completed .step-title {
  color: #10b981;
  font-weight: 600;
}

.step-line {
  width: 60px;
  height: 3px;
  background: #e5e7eb;
  transition: all 0.3s ease;
  border-radius: 2px;
}

.step-line.completed {
  background: linear-gradient(90deg, #10b981 0%, #059669 100%);
}

.form-wrapper {
  flex: 1;
  overflow-y: auto;
  padding: 32px 36px;
  max-height: 70vh;
}

.step-content {
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

/* 動畫效果 */
.slide-enter-active,
.slide-leave-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-enter-from {
  opacity: 0;
  transform: translateX(30px);
}

.slide-leave-to {
  opacity: 0;
  transform: translateX(-30px);
}

/* 響應式設計 */
@media (max-width: 768px) {
  :deep(.el-dialog) {
    width: 95% !important;
  }
  
  .steps-wrapper {
    padding: 20px 24px;
  }
  
  .form-wrapper {
    padding: 20px;
  }
  
  .step-content {
    padding: 20px;
  }
}
</style>
