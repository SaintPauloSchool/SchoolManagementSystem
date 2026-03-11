<template>
  <div class="fill-in-blank-editor">
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

      <!-- 答案 -->
      <el-form-item label="正確答案" prop="answer">
        <div class="answer-input-wrapper">
          <el-input
            v-model="localQuestionData.answer"
            placeholder="請輸入正確答案（支持多個答案，用逗號分隔）"
            clearable
            class="answer-input"
            size="large"
          >
            <template #prefix>
              <el-icon class="answer-icon"><Check /></el-icon>
            </template>
          </el-input>
          <div class="answer-hint">
            <el-icon><InfoFilled /></el-icon>
            <span>如果是多空填空，請用英文逗號 , 分隔多個答案</span>
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
  </div>
</template>

<script>
import { ChatDotRound, Check, InfoFilled, Bell } from '@element-plus/icons-vue'

export default {
  name: 'FillInBlankEditor',
  components: {
    ChatDotRound,
    Check,
    InfoFilled,
    Bell
  },
  props: {
    questionData: {
      type: Object,
      required: true
    }
  },
  emits: ['save', 'update:questionData'],
  data() {
    return {
      localQuestionData: {
        type: '3',
        title: this.questionData.title || '',
        answer: this.questionData.answer || '',
        required: this.questionData.required || false
      },
      rules: {
        title: [
          { required: true, message: '請輸入題目內容', trigger: 'blur' },
          { max: 200, message: '標題長度不能超過 200 個字符', trigger: 'blur' }
        ],
        answer: [
          { required: true, message: '請輸入正確答案', trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    handleSave() {
      this.$refs.formRef.validate((valid) => {
       if (valid) {
         const data = {
            ...this.localQuestionData,
            options: [this.localQuestionData.answer] // 將答案存入 options
          }
          this.$emit('save', data)
        }
      })
    }
  }
}
</script>

<style scoped>
.fill-in-blank-editor {
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
  border-color: #f59e0b;
  background: #ffffff;
  box-shadow: 0 4px 16px rgba(245, 158, 11, 0.15);
  transform: translateY(-1px);
}

.title-input :deep(.el-input__wrapper.is-focus) {
  border-color: #d97706;
  background: #ffffff;
  box-shadow: 0 0 0 5px rgba(245, 158, 11, 0.1), 0 6px 20px rgba(245, 158, 11, 0.2);
}

.title-icon {
  color: #f59e0b;
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

.answer-input-wrapper {
  width: 100%;
}

.answer-input :deep(.el-input__wrapper) {
  border-radius: 12px;
  border: 2px solid #fcd34d;
  padding: 14px 18px;
  background: linear-gradient(135deg, #fef3c7 0%, #ffffff 100%);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.answer-input :deep(.el-input__wrapper:hover) {
  border-color: #f59e0b;
  background: #ffffff;
  box-shadow: 0 4px 16px rgba(245, 158, 11, 0.2);
  transform: translateY(-1px);
}

.answer-input :deep(.el-input__wrapper.is-focus) {
  border-color: #d97706;
  background: #ffffff;
  box-shadow: 0 0 0 5px rgba(245, 158, 11, 0.15), 0 6px 20px rgba(245, 158, 11, 0.3);
}

.answer-icon {
  color: #d97706;
  font-size: 18px;
}

.answer-hint {
  margin-top: 8px;
  padding: 10px 14px;
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border-radius: 8px;
  border: 1px solid #fcd34d;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #92400e;
  font-weight: 500;
}

.answer-hint .el-icon {
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
  background: linear-gradient(135deg, #fef3c7 0%, #ffffff 100%);
  border-radius: 14px;
  border: 2px solid #fde68a;
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
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  border-color: #f59e0b;
  box-shadow: 0 2px 12px rgba(245, 158, 11, 0.4);
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
  color: #f59e0b;
  font-weight: 600;
}

.required-hint.is-required .hint-icon {
  color: #f59e0b;
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
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  border: none;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(245, 158, 11, 0.35);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  gap: 8px;
}

.save-btn:hover {
  background: linear-gradient(135deg, #d97706 0%, #b45309 100%);
  box-shadow: 0 6px 24px rgba(245, 158, 11, 0.5);
  transform: translateY(-2px) scale(1.02);
}

.save-btn:active {
  transform: translateY(0) scale(0.98);
  box-shadow: 0 2px 10px rgba(245, 158, 11, 0.25);
}
</style>
