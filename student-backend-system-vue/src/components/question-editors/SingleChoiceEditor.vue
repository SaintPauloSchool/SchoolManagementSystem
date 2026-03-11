<template>
  <div class="single-choice-editor">
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

      <!-- 選項 -->
      <el-form-item label="選項設置" required>
        <div class="options-container">
          <div class="options-header">
            <span class="options-label">
              <el-icon class="label-icon"><List /></el-icon>
              請設定選項內容
            </span>
            <el-tag type="" size="small" effect="light">
              單選題（至少 2 個選項）
            </el-tag>
          </div>
          
          <transition-group name="option-list">
            <div 
              v-for="(option, index) in localQuestionData.options" 
              :key="`option-${index}`"
              class="option-item-wrapper"
            >
              <div class="option-item">
                <span class="option-index">{{ getOptionLabel(index) }}</span>
                <el-input
                  v-model="localQuestionData.options[index]"
                  placeholder="請輸入選項內容"
                  class="option-input"
                  clearable
                  size="default"
                />
                <el-button 
                  type="danger" 
                  size="small" 
                  @click="removeOption(index)"
                  :disabled="localQuestionData.options.length <= 2"
                  class="remove-btn"
                  circle
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
          </transition-group>
          
          <el-button 
            type="primary" 
            @click="addOption"
            class="add-option-btn"
            plain
            size="default"
          >
            <el-icon><Plus /></el-icon>
            新增選項
          </el-button>
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
import { Plus, Delete, ChatDotRound, Check, CircleCheck, List, Bell } from '@element-plus/icons-vue'

export default {
  name: 'SingleChoiceEditor',
  components: {
    Plus,
    Delete,
    ChatDotRound,
    Check,
    CircleCheck,
    List,
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
        type: '1',
        title: this.questionData.title || '',
        options: this.questionData.options && this.questionData.options.length > 0 
          ? [...this.questionData.options] 
          : ['', ''],
        required: this.questionData.required || false
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
    getOptionLabel(index) {
      return String.fromCharCode(65 + index)
    },

    addOption() {
      this.localQuestionData.options.push('')
    },

    removeOption(index) {
     if (this.localQuestionData.options.length > 2) {
        this.localQuestionData.options.splice(index, 1)
      }
    },

    handleSave() {
      this.$refs.formRef.validate((valid) => {
       if (valid) {
         const options = this.localQuestionData.options.filter(opt => opt.trim() !== '')
         if (options.length < 2) {
            this.$message.warning('單選題至少需要 2 個選項')
            return
          }
          
         const data = {
            ...this.localQuestionData,
            options: options
          }
          
          this.$emit('save', data)
        }
      })
    }
  }
}
</script>

<style scoped>
.single-choice-editor {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 題目輸入框 */
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
  border-color: #60a5fa;
  background: #ffffff;
  box-shadow: 0 4px 16px rgba(96, 165, 250, 0.15);
  transform: translateY(-1px);
}

.title-input :deep(.el-input__wrapper.is-focus) {
  border-color: #3b82f6;
  background: #ffffff;
  box-shadow: 0 0 0 5px rgba(59, 130, 246, 0.1), 0 6px 20px rgba(59, 130, 246, 0.2);
}

.title-icon {
  color: #3b82f6;
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

/* 選項容器 */
.options-container {
  width: 100%;
  padding: 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #ffffff 100%);
  border-radius: 16px;
  border: 2px solid #e5e7eb;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.options-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 2px solid #e5e7eb;
}

.options-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #374151;
  font-size: 14px;
  letter-spacing: 0.3px;
}

.label-icon {
  color: #3b82f6;
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

.option-item-wrapper {
  margin-bottom: 14px;
  animation: slideIn 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(-30px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateX(0) scale(1);
  }
}

.option-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  background: linear-gradient(135deg, #ffffff 0%, #f9fafb 100%);
  border-radius: 12px;
  border: 2px solid #e5e7eb;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.option-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(to bottom, #3b82f6, #2563eb);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.option-item:hover {
  border-color: #60a5fa;
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.15);
  transform: translateX(4px);
}

.option-item:hover::before {
  opacity: 1;
}

.option-index {
  font-weight: 800;
  color: #3b82f6;
  font-size: 16px;
  min-width: 32px;
  text-align: center;
  height: 32px;
  line-height: 32px;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border-radius: 8px;
  box-shadow: 0 2px 6px rgba(59, 130, 246, 0.15);
}

.option-input {
  flex: 1;
}

.option-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 10px 14px;
  background: #ffffff;
  border: 2px solid #e5e7eb;
  box-shadow: none;
}

.option-input :deep(.el-input__wrapper:hover) {
  border-color: #60a5fa;
}

.option-input :deep(.el-input__wrapper.is-focus) {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.remove-btn {
  flex-shrink: 0 !important;
  width: 32px !important;
  height: 32px !important;
  border-radius: 8px !important;
  padding: 0 !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%) !important;
  box-shadow: 0 2px 6px rgba(239, 68, 68, 0.3) !important;
  border: 2px solid transparent !important;
}

.add-option-btn {
  width: 100%;
  margin-top: 12px;
  height: 46px;
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  letter-spacing: 0.8px;
  position: relative;
  overflow: hidden;
}

.add-option-btn:hover {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  box-shadow: 0 6px 24px rgba(59, 130, 246, 0.45);
  transform: translateY(-2px) scale(1.02);
}

/* 必答設置 */
.required-setting {
  width: 100%;
}

.required-toggle {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  padding: 16px 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #ffffff 100%);
  border-radius: 14px;
  border: 2px solid #e5e7eb;
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
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border-color: #3b82f6;
  box-shadow: 0 2px 12px rgba(59, 130, 246, 0.4);
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
  color: #3b82f6;
  font-weight: 600;
}

.required-hint.is-required .hint-icon {
  color: #3b82f6;
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

/* 表單標籤 */
:deep(.el-form-item__label) {
  font-weight: 700;
  color: #374151;
  font-size: 15px;
  letter-spacing: 0.5px;
  padding-left: 4px;
}

/* 底部按鈕 */
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
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.35);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  gap: 8px;
}

.save-btn:hover {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  box-shadow: 0 6px 24px rgba(59, 130, 246, 0.5);
  transform: translateY(-2px) scale(1.02);
}

.save-btn:active {
  transform: translateY(0) scale(0.98);
  box-shadow: 0 2px 10px rgba(59, 130, 246, 0.25);
}

/* 動畫效果 */
.option-list-enter-active,
.option-list-leave-active {
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.option-list-enter-from {
  opacity: 0;
  transform: translateX(-30px) scale(0.9);
}

.option-list-leave-to {
  opacity: 0;
  transform: translateX(30px) scale(0.9);
}
</style>
