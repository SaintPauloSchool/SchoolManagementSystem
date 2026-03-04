2  <template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="750px"
    :before-close="handleClose"
    class="question-dialog"
    top="8vh"
    destroy-on-close
  >
    <div class="dialog-content">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="90px"
        label-position="left"
        class="question-form"
      >
        <!-- 问题类型选择 -->
        <el-form-item label="题型" prop="type">
          <div class="type-selector">
            <div class="type-selector-title">選擇題目類型</div>
            <el-radio-group v-model="form.type" @change="handleTypeChange" :disabled="!!question">
              <el-radio-button value="1" title="单选">
                <el-icon><CircleCheck /></el-icon>
                <span>單選</span>
              </el-radio-button>
              <el-radio-button value="2" title="多选">
                <el-icon><Checked /></el-icon>
                <span>多選</span>
              </el-radio-button>
              <el-radio-button value="3" title="填空">
                <el-icon><Edit /></el-icon>
                <span>填空</span>
              </el-radio-button>
              <el-radio-button value="4" title="附件">
                <el-icon><Paperclip /></el-icon>
                <span>附件</span>
              </el-radio-button>
              <el-radio-button value="5" title="分支">
                <el-icon><Share /></el-icon>
                <span>分支</span>
              </el-radio-button>
            </el-radio-group>
          </div>
        </el-form-item>

        <!-- 问题标题 -->
        <el-form-item label="题目" prop="title">
          <div class="title-input-wrapper">
            <el-input
              v-model="form.title"
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

        <!-- 选项设置（单选/多选） -->
        <transition name="slide-fade">
          <div v-if="form.type === '1' || form.type === '2'">
            <el-form-item label="选项" required>
              <div class="options-container">
                <div class="options-header">
                  <span class="options-label">
                    <el-icon class="label-icon"><List /></el-icon>
                    請設定選項內容
                  </span>
                  <el-tag :type="form.type === '1' ? '' : 'warning'" size="small" effect="light">
                    {{ form.type === '1' ? '單選題' : '多選題' }}
                  </el-tag>
                </div>
                <transition-group name="option-list">
                  <div 
                    v-for="(option, index) in form.options" 
                    :key="`option-${index}`"
                    class="option-item-wrapper"
                  >
                    <div class="option-item">
                      <span class="option-index">{{ getOptionLabel(index) }}</span>
                      <el-input
                        v-model="form.options[index]"
                        placeholder="请输入选项内容"
                        class="option-input"
                        clearable
                        size="default"
                      />
                      <el-button 
                        type="danger" 
                        size="small" 
                        @click="removeOption(index)"
                        :disabled="form.options.length <= 2"
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
          </div>
        </transition>

        <!-- 必答设置 -->
        <el-form-item label="必填" prop="required">
          <div class="required-setting">
            <div class="required-toggle">
              <el-switch
                v-model="form.required"
                class="required-switch"
                inline-prompt
                active-text="必"
                inactive-text="選"
              />
              <span class="required-hint" :class="{ 'is-required': form.required }">
                <el-icon class="hint-icon"><Bell /></el-icon>
                {{ form.required ? '此題為必答' : '此題為選答' }}
              </span>
            </div>
          </div>
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose" class="cancel-btn" size="default">
          <el-icon><Close /></el-icon>
          取消
        </el-button>
        <el-button type="primary" @click="handleSubmit" class="submit-btn" size="default">
          <el-icon><Check /></el-icon>
          確定儲存
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
import { Plus, Delete, ChatDotRound, Check, InfoFilled, CircleCheck, Checked, Edit, Paperclip, Share, List, Bell, Close } from '@element-plus/icons-vue'

export default {
  name: 'QuestionDialog',
  components: {
    Plus,
    Delete,
    ChatDotRound,
    Check,
    InfoFilled,
    CircleCheck,
    Checked,
    Edit,
    Paperclip,
    Share,
    List,
    Bell,
    Close
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    question: {
      type: Object,
      default: null
    },
    questionType: {
      type: String,
      default: ''
    }
  },
  emits: ['update:visible', 'save'],
  data() {
    return {
      form: {
        type: '',
        title: '',
        options: ['', ''],
        required: false
      },
      rules: {
        type: [
          { required: true, message: '请选择问题类型', trigger: 'change' }
        ],
        title: [
          { required: true, message: '请输入问题标题', trigger: 'blur' },
          { max: 200, message: '标题长度不能超过 200 个字符', trigger: 'blur' }
        ]
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
    },
    questionType(newVal) {
      if (newVal) {
        this.form.type = newVal
        // 初始化选项
        this.handleTypeChange()
      }
    },
    'form.type'(newVal) {
      // 监听类型变化，初始化选项
      this.handleTypeChange()
    }
  },
  methods: {
    getOptionLabel(index) {
      // 返回选项标签，如 A、B、C、D...
      return String.fromCharCode(65 + index)
    },

    initForm() {
      if (this.question) {
        this.form = {
          type: this.question.type,
          title: this.question.title,
          options: this.question.options ? [...this.question.options] : ['', ''],
          required: this.question.required || false
        }
      } else {
        this.form = {
          type: this.questionType || '',
          title: '',
          options: ['', ''],
          required: false
        }
      }
      // 确保选项数组正确初始化
      this.handleTypeChange()
    },

    handleTypeChange() {
      if (this.form.type === '1' || this.form.type === '2') {
        if (this.form.options.length < 2) {
          this.form.options = ['', '']
        }
      } else {
        this.form.options = []
      }
    },

    addOption() {
      this.form.options.push('')
    },

    removeOption(index) {
      if (this.form.options.length > 2) {
        this.form.options.splice(index, 1)
      }
    },

    handleClose() {
      this.$refs.formRef.resetFields()
      // 重置编辑状态
      this.$emit('update:visible', false)
    },

    handleSubmit() {
      this.$refs.formRef.validate((valid) => {
        if (valid) {
          let options = null
          if (this.form.type === '1' || this.form.type === '2') {
            options = this.form.options.filter(option => option.trim() !== '')
            if (options.length < 2) {
              // 通过 $emit 触发父组件的验证失败事件
              this.$emit('validate-error', '单选和多选题至少需要 2 个选项')
              return
            }
          }
    
          const questionData = {
            type: this.form.type,
            title: this.form.title,
            options: options,
            required: this.form.required
          }
    
          this.$emit('save', questionData)
          // 直接关闭，不重置表单，让父组件处理
          this.$emit('update:visible', false)
        }
      })
    },
  }
}
</script>

<style scoped>
/* 對話框主體容器 */
.dialog-content {
  padding: 8px 0;
}

/* 強制對話框主體顯示滾動條 */
:deep(.el-dialog__body) {
  max-height: 72vh;
  overflow-y: auto;
  padding: 32px 36px;
  background: linear-gradient(180deg, #ffffff 0%, #fafbfc 100%);
}

/* 美化對話框滾動條 */
:deep(.el-dialog__body::-webkit-scrollbar) {
  width: 8px;
}

:deep(.el-dialog__body::-webkit-scrollbar-track) {
  background: #f1f5f9;
  border-radius: 4px;
}

:deep(.el-dialog__body::-webkit-scrollbar-thumb) {
  background: linear-gradient(to bottom, #93c5fd, #3b82f6);
  border-radius: 4px;
  transition: all 0.3s ease;
}

:deep(.el-dialog__body::-webkit-scrollbar-thumb:hover) {
  background: linear-gradient(to bottom, #3b82f6, #2563eb);
}

/* 頂部提示區域 - 全新設計 */
.info-banner {
  margin-bottom: 28px;
  padding: 18px 20px;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border-radius: 14px;
  border-left: 4px solid #3b82f6;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.12);
  animation: slideDown 0.4s ease;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.banner-inner {
  display: flex;
  align-items: center;
  gap: 14px;
}

.banner-icon {
  font-size: 24px;
  color: #3b82f6;
  flex-shrink: 0;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.8;
  }
}

.banner-text-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.banner-title {
  font-weight: 700;
  color: #1e40af;
  font-size: 15px;
  letter-spacing: 0.3px;
}

.banner-text {
  color: #3b82f6;
  font-size: 14px;
  line-height: 1.5;
}

/* 題型選擇器 - 全新設計 */
.type-selector {
  width: 100%;
}

.type-selector-title {
  font-size: 14px;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 12px;
  padding-left: 4px;
  letter-spacing: 0.3px;
}

.type-selector :deep(.el-radio-group) {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 12px;
  width: 100%;
}

.type-selector :deep(.el-radio-button) {
  width: 100%;
}

.type-selector :deep(.el-radio-button__inner) {
  min-width: 100%;
  height: 70px;
  border-radius: 14px;
  border: 2px solid #e5e7eb;
  background: linear-gradient(135deg, #ffffff 0%, #f9fafb 100%);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  text-align: center;
  position: relative;
  overflow: hidden;
}

.type-selector :deep(.el-radio-button__inner::before) {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.05) 0%, rgba(37, 99, 235, 0.05) 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.type-selector :deep(.el-radio-button__inner:hover) {
  border-color: #60a5fa;
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.15);
  transform: translateY(-2px);
}

.type-selector :deep(.el-radio-button__inner:hover::before) {
  opacity: 1;
}

.type-selector :deep(.el-radio-button.is-active .el-radio-button__inner) {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border-color: #3b82f6;
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.35);
  transform: translateY(-2px);
}

.type-selector :deep(.el-radio-button__inner *) {
  color: #6b7280;
  transition: all 0.3s ease;
}

.type-selector :deep(.el-radio-button.is-active .el-radio-button__inner *) {
  color: #ffffff;
}

.type-selector :deep(.el-radio-button__inner .el-icon) {
  font-size: 24px;
  margin-bottom: 4px;
}

.type-selector :deep(.el-radio-button__inner span) {
  font-weight: 600;
  font-size: 14px;
  letter-spacing: 0.5px;
}

/* 題目輸入框 - 增強設計 */
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

/* 選項容器 - 現代化設計 */
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
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
  box-shadow: 0 2px 6px rgba(239, 68, 68, 0.15);
  border: 2px solid transparent;
}

.remove-btn:hover {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
  border-color: #fca5a5;
  transform: scale(1.08);
}

.remove-btn:active {
  transform: scale(0.95);
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.2);
}

.remove-btn .el-icon {
  font-size: 16px;
  color: #dc2626;
  transition: all 0.3s ease;
}

.remove-btn:hover .el-icon {
  color: #ffffff;
  transform: scale(1.1);
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

.add-option-btn::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  transform: translate(-50%, -50%);
  transition: width 0.6s, height 0.6s;
}

.add-option-btn:hover::before {
  width: 300px;
  height: 300px;
}

.add-option-btn:hover {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  box-shadow: 0 6px 24px rgba(59, 130, 246, 0.45);
  transform: translateY(-2px) scale(1.02);
}

.add-option-btn:active {
  transform: translateY(0) scale(0.98);
  box-shadow: 0 2px 10px rgba(59, 130, 246, 0.2);
}

/* 必答設置 - 開關美化 */
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

.required-switch :deep(.el-switch__core .el-switch__action) {
  border-radius: 50%;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
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

/* 表單標籤美化 */
:deep(.el-form-item__label) {
  font-weight: 700;
  color: #374151;
  font-size: 15px;
  letter-spacing: 0.5px;
  padding-left: 4px;
}

/* 底部按鈕區 - 緊湊設計 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 36px;
  background: linear-gradient(to bottom, #f8fafc 0%, #ffffff 100%);
  border-radius: 0 0 24px 24px;
}

.cancel-btn,
.submit-btn {
  min-width: 100px;
  height: 38px;
  font-weight: 600;
  font-size: 14px;
  border-radius: 10px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  letter-spacing: 0.3px;
  position: relative;
  overflow: hidden;
}

.cancel-btn {
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  color: #6b7280;
  border: 2px solid #d1d5db;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.cancel-btn:hover {
  background: linear-gradient(135deg, #e5e7eb 0%, #d1d5db 100%);
  color: #374151;
  border-color: #9ca3af;
  box-shadow: 0 3px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.submit-btn {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: #ffffff;
  border: none;
  box-shadow: 0 3px 12px rgba(59, 130, 246, 0.35);
}

.submit-btn::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  transform: translate(-50%, -50%);
  transition: width 0.6s, height 0.6s;
}

.submit-btn:hover::before {
  width: 300px;
  height: 300px;
}

.submit-btn:hover {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  box-shadow: 0 5px 18px rgba(59, 130, 246, 0.5);
  transform: translateY(-2px) scale(1.02);
}

.submit-btn:active {
  transform: translateY(0) scale(0.98);
  box-shadow: 0 2px 10px rgba(59, 130, 246, 0.25);
}

.submit-btn .el-icon {
  animation: checkBounce 2s ease-in-out infinite;
}

@keyframes checkBounce {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

/* 動畫效果增強 */
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.slide-fade-enter-from {
  opacity: 0;
  transform: translateY(-20px);
}

.slide-fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

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

/* 響應式調整 */
@media (max-width: 768px) {
  :deep(.el-dialog) {
    width: 90% !important;
    margin: 0 auto;
  }
  
  :deep(.el-dialog__body) {
    padding: 24px 20px;
  }
  
  .type-selector :deep(.el-radio-group) {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .dialog-footer {
    padding: 16px 20px;
  }
  
  .cancel-btn,
  .submit-btn {
    min-width: 100px;
    height: 40px;
    font-size: 14px;
  }
}
</style>
