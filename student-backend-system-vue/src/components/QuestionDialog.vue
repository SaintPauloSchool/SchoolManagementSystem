2  <template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="650px"
    :before-close="handleClose"
    class="question-dialog"
    top="8vh"
  >
    <div class="dialog-content">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        label-position="left"
      >
        <!-- 问题类型选择 -->
        <el-form-item label="问题类型" prop="type">
          <div class="form-item-content">
            <el-select 
              v-model="form.type" 
              placeholder="请选择问题类型"
              @change="handleTypeChange"
              :disabled="!!question"
              class="type-select"
            >
              <el-option label="📝 单选" value="1" />
              <el-option label="✅ 多选" value="2" />
              <el-option label="✏️ 填空" value="3" />
              <el-option label="📎 附件上传" value="4" />
              <el-option label="🔀 分支" value="5" />
            </el-select>
          </div>
        </el-form-item>

        <!-- 问题标题 -->
        <el-form-item label="问题标题" prop="title">
          <div class="form-item-content">
            <el-input
              v-model="form.title"
              placeholder="请输入问题标题，例如：您的姓名是？"
              maxlength="200"
              show-word-limit
              clearable
              class="title-input"
            >
              <template #prefix>
                <el-icon><ChatDotRound /></el-icon>
              </template>
            </el-input>
          </div>
        </el-form-item>

        <!-- 选项设置（单选/多选） -->
        <transition name="slide-fade">
          <div v-if="form.type === '1' || form.type === '2'">
            <el-form-item label="备选项">
              <div class="options-container">
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
                      />
                      <el-button 
                        type="danger" 
                        size="small" 
                        @click="removeOption(index)"
                        :disabled="form.options.length <= 2"
                        class="remove-btn"
                        circle
                      >
                        <el-icon><Minus /></el-icon>
                      </el-button>
                    </div>
                  </div>
                </transition-group>
                
                <el-button 
                  type="primary" 
                  @click="addOption"
                  class="add-option-btn"
                  plain
                >
                  <el-icon><Plus /></el-icon>
                  添加选项
                </el-button>
              </div>
            </el-form-item>
          </div>
        </transition>

        <!-- 必答設置 -->
        <el-form-item label="是否必答">
          <div class="form-item-content">
            <el-switch
              v-model="form.required"
              active-text="必答"
              inactive-text="选答"
              class="required-switch"
            />
          </div>
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose" class="cancel-btn">
          取消
        </el-button>
        <el-button type="primary" @click="handleSubmit" class="submit-btn">
          <el-icon><Check /></el-icon>
          确定
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
import { Plus, Minus, ChatDotRound, Check } from '@element-plus/icons-vue'

export default {
  name: 'QuestionDialog',
  components: {
    Plus,
    Minus,
    ChatDotRound,
    Check
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
.dialog-content {
  /* 不设置固定高度和滚动，让对话框本身处理滚动 */
}

/* 强制对话框主体显示滚动条 */
:deep(.el-dialog__body) {
  max-height: 70vh;
  overflow-y: auto;
}

/* 美化对话框滚动条 */
:deep(.el-dialog__body::-webkit-scrollbar) {
  width: 6px;
}

:deep(.el-dialog__body::-webkit-scrollbar-track) {
  background: #f1f5f9;
  border-radius: 3px;
}

:deep(.el-dialog__body::-webkit-scrollbar-thumb) {
  background: linear-gradient(to bottom, #60a5fa 0%, #3b82f6 100%);
  border-radius: 3px;
}

:deep(.el-dialog__body::-webkit-scrollbar-thumb:hover) {
  background: linear-gradient(to bottom, #3b82f6 0%, #2563eb 100%);
}

.form-item-content {
  width: 100%;
}

.type-select {
  width: 100%;
}

.title-input {
  width: 100%;
}

.options-container {
  width: 100%;
}

.option-item-wrapper {
  margin-bottom: 12px;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.option-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #f9fafb 0%, #ffffff 100%);
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  transition: all 0.3s ease;
}

.option-item:hover {
  background: #f9fafb;
  border-color: #60a5fa;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
}

.option-index {
  font-weight: 700;
  color: #3b82f6;
  font-size: 14px;
  min-width: 28px;
  text-align: center;
}

.option-input {
  flex: 1;
}

.option-input :deep(.el-input__wrapper) {
  border-radius: 8px;
}

.remove-btn {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
}

/* 移除按钮红色美化 */
.remove-btn.el-button--danger {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.25);
  transition: all 0.3s ease;
}

.remove-btn.el-button--danger:hover {
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  box-shadow: 0 4px 14px rgba(239, 68, 68, 0.35);
  transform: scale(1.05);
}

.remove-btn.el-button--danger:active {
  transform: scale(0.98);
  box-shadow: 0 1px 4px rgba(239, 68, 68, 0.2);
}

.add-option-btn {
  width: 100%;
  margin-top: 8px;
  height: 42px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  border-radius: 10px;
  box-shadow: 0 3px 12px rgba(59, 130, 246, 0.25);
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  letter-spacing: 0.5px;
}

.add-option-btn:hover {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  box-shadow: 0 5px 18px rgba(59, 130, 246, 0.35);
  transform: translateY(-1px);
}

.add-option-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.15);
}

.required-switch {
  width: 100%;
}

/* 動畫效果 */
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease;
}

.slide-fade-enter-from {
  opacity: 0;
  transform: translateY(-10px);
}

.slide-fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

.option-list-enter-active,
.option-list-leave-active {
  transition: all 0.3s ease;
}

.option-list-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}

.option-list-leave-to {
  opacity: 0;
  transform: translateX(20px);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.cancel-btn,
.submit-btn {
  min-width: 100px;
  font-weight: 600;
  border-radius: 10px;
}

.submit-btn {
  position: relative;
  overflow: hidden;
  border-radius: 10px;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

.submit-btn:hover {
  animation: pulse 0.6s ease;
}
</style>
