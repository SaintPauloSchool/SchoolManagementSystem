<template>
  <div class="branch-editor">
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

      <!-- 分支選項 -->
      <el-form-item label="分支設置" required>
        <div class="branch-container">
          <div class="branch-header">
            <span class="branch-label">
              <el-icon class="label-icon"><Share /></el-icon>
              請設定兩個分支選項
            </span>
            <el-tag type="info" size="small" effect="light">
              一個選項繼續，另一個選項結束
            </el-tag>
          </div>

          <!-- 選項 1 -->
          <div class="branch-option">
            <div class="option-header">
              <span class="option-badge">選項 1</span>
            </div>
            <el-input
              v-model="branchOptions[0].text"
              placeholder="请输入选项文字"
              class="option-input"
              clearable
              size="large"
            />
            <div class="option-action">
              <span class="action-label">此選項將：</span>
              <el-radio-group v-model="branchOptions[0].action" size="default">
                <el-radio value="end">結束</el-radio>
                <el-radio value="continue">繼續下一題</el-radio>
              </el-radio-group>
            </div>
            <transition name="slide-down">
              <div v-if="branchOptions[0].action === 'continue'" class="next-title-wrapper">
                <el-input
                  v-model="branchOptions[0].nextTitle"
                  placeholder="请输入下一题的題目內容"
                  clearable
                  size="large"
                  class="next-title-input"
                >
                  <template #prefix>
                    <el-icon><ChatDotRound /></el-icon>
                  </template>
                </el-input>
              </div>
            </transition>
          </div>

          <el-divider />

          <!-- 選項 2 -->
          <div class="branch-option">
            <div class="option-header">
              <span class="option-badge">選項 2</span>
            </div>
            <el-input
              v-model="branchOptions[1].text"
              placeholder="请输入选项文字"
              class="option-input"
              clearable
              size="large"
            />
            <div class="option-action">
              <span class="action-label">此選項將：</span>
              <el-radio-group v-model="branchOptions[1].action" size="default">
                <el-radio value="end">結束</el-radio>
                <el-radio value="continue">繼續下一題</el-radio>
              </el-radio-group>
            </div>
            <transition name="slide-down">
              <div v-if="branchOptions[1].action === 'continue'" class="next-title-wrapper">
                <el-input
                  v-model="branchOptions[1].nextTitle"
                  placeholder="请输入下一题的題目內容"
                  clearable
                  size="large"
                  class="next-title-input"
                >
                  <template #prefix>
                    <el-icon><ChatDotRound /></el-icon>
                  </template>
                </el-input>
              </div>
            </transition>
          </div>

          <el-alert
            :title="validationMessage"
            :type="isValidBranch ? 'success' : 'warning'"
            :closable="false"
            show-icon
            class="validation-alert"
          />
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
      <el-button type="primary" @click="handleSave" class="save-btn" :disabled="!isValidBranch">
        <el-icon><Check /></el-icon>
        <span class="btn-text">儲存題目</span>
      </el-button>
    </div>
  </div>
</template>

<script>
import { ChatDotRound, Share, Bell, ArrowLeft } from '@element-plus/icons-vue'

export default {
  name: 'BranchEditor',
  components: {
    ChatDotRound,
    Share,
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
        type: '5',
        title: this.questionData.title || '',
        branchOptions: [],
        required: this.questionData.required || false
      },
      branchOptions: [
        { text: '', action: 'continue', nextTitle: '' },
        { text: '', action: 'end', nextTitle: '' }
      ],
      rules: {
        title: [
          { required: true, message: '請輸入題目內容', trigger: 'blur' },
          { max: 200, message: '標題長度不能超過 200 個字符', trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    validationMessage() {
     const valid = this.validateBranchOptions()
     if (valid) {
        return '分支選項設置正確：一個繼續，一個結束'
      } else {
        return '請注意：兩個選項中，必須一個是「繼續下一題」，另一個是「結束」'
      }
    },
    isValidBranch() {
     return this.validateBranchOptions()
    }
  },
  methods: {
    validateBranchOptions() {
      // 验证两个选项不能相同
     if (this.branchOptions[0].action === this.branchOptions[1].action) {
        return false
      }
      
      // 验证文字不能为空
     if (!this.branchOptions[0].text?.trim() || !this.branchOptions[1].text?.trim()) {
        return false
      }
      
      // 验证 continue 类型的选项必须填写下一题题目
     if (this.branchOptions[0].action === 'continue' && !this.branchOptions[0].nextTitle?.trim()) {
        return false
      }
     if (this.branchOptions[1].action === 'continue' && !this.branchOptions[1].nextTitle?.trim()) {
        return false
      }
      
      return true
    },

    handleSave() {
      this.$refs.formRef.validate((valid) => {
      if (valid) {
        if (!this.isValidBranch) {
           this.$message.warning('请完善分支选项设置')
           return
         }
         
       const data = {
            ...this.localQuestionData,
            options: this.branchOptions // 将分支选项存入 options
          }
          
          this.$emit('save', data)
        }
      })
    }
  }
}
</script>

<style scoped>
.branch-editor {
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
  border-color: #8b5cf6;
  background: #ffffff;
  box-shadow: 0 4px 16px rgba(139, 92, 246, 0.15);
  transform: translateY(-1px);
}

.title-input :deep(.el-input__wrapper.is-focus) {
  border-color: #7c3aed;
  background: #ffffff;
  box-shadow: 0 0 0 5px rgba(139, 92, 246, 0.1), 0 6px 20px rgba(139, 92, 246, 0.2);
}

.title-icon {
  color: #8b5cf6;
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

.branch-container {
  width: 100%;
  padding: 24px;
  background: linear-gradient(135deg, #f5f3ff 0%, #ffffff 100%);
  border-radius: 16px;
  border: 2px solid #ddd6fe;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.branch-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #ddd6fe;
}

.branch-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #6b21a8;
  font-size: 14px;
  letter-spacing: 0.3px;
}

.label-icon {
  color: #7c3aed;
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

.branch-option {
  padding: 20px;
  background: linear-gradient(135deg, #ffffff 0%, #faf5ff 100%);
  border-radius: 12px;
  border: 2px solid #e9d5ff;
  margin-bottom: 16px;
  transition: all 0.3s ease;
}

.branch-option:hover {
  border-color: #c084fc;
  box-shadow: 0 4px 16px rgba(139, 92, 246, 0.15);
  transform: translateY(-2px);
}

.option-header {
  margin-bottom: 12px;
}

.option-badge {
  display: inline-block;
  padding: 6px 16px;
  background: linear-gradient(135deg, #c084fc 0%, #a855f7 100%);
  color: white;
  border-radius: 8px;
  font-weight: 700;
  font-size: 14px;
  box-shadow: 0 2px 8px rgba(139, 92, 246, 0.3);
}

.option-input {
  margin-bottom: 16px;
}

.option-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 12px 16px;
  background: #ffffff;
  border: 2px solid #e5e7eb;
  box-shadow: none;
}

.option-input :deep(.el-input__wrapper:hover) {
  border-color: #c084fc;
}

.option-input :deep(.el-input__wrapper.is-focus) {
  border-color: #a855f7;
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.1);
}

.option-action {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-top: 16px;
  border-top: 1px dashed #e9d5ff;
}

.action-label {
  font-weight: 600;
  color: #6b21a8;
  font-size: 14px;
  white-space: nowrap;
}

.next-title-wrapper {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed #e9d5ff;
}

.next-title-input {
  width: 100%;
}

.next-title-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 12px 16px;
  background: #fef3c7;
  border: 2px solid #fcd34d;
}

.next-title-input :deep(.el-input__wrapper:hover) {
  border-color: #f59e0b;
}

.next-title-input :deep(.el-input__wrapper.is-focus) {
  border-color: #d97706;
  box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.1);
}

.validation-alert {
  margin-top: 16px;
  border-radius: 8px;
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
  background: linear-gradient(135deg, #f5f3ff 0%, #ffffff 100%);
  border-radius: 14px;
  border: 2px solid #e9d5ff;
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
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
  border-color: #8b5cf6;
  box-shadow: 0 2px 12px rgba(139, 92, 246, 0.4);
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
  color: #8b5cf6;
  font-weight: 600;
}

.required-hint.is-required .hint-icon {
  color: #8b5cf6;
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
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
  border: none;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(139, 92, 246, 0.35);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  gap: 8px;
}

.save-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%);
  box-shadow: 0 6px 24px rgba(139, 92, 246, 0.5);
  transform: translateY(-2px) scale(1.02);
}

.save-btn:active:not(:disabled) {
  transform: translateY(0) scale(0.98);
  box-shadow: 0 2px 10px rgba(139, 92, 246, 0.25);
}

.save-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
}

.slide-down-enter-from {
  opacity: 0;
  transform: translateY(-10px);
}

.slide-down-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
