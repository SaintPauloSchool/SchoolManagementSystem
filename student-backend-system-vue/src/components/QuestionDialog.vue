<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="560px"
    :before-close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="90px"
    >
      <!-- 问题类型选择 -->
      <el-form-item label="问题类型" prop="type">
        <el-select 
          v-model="form.type" 
          placeholder="请选择问题类型"
          @change="handleTypeChange"
          :disabled="!!question"
        >
          <el-option label="单选" value="1" />
          <el-option label="多选" value="2" />
          <el-option label="填空" value="3" />
          <el-option label="附件上传" value="4" />
          <el-option label="分支" value="5" />
        </el-select>
      </el-form-item>

      <!-- 问题标题 -->
      <el-form-item label="问题标题" prop="title">
        <el-input
          v-model="form.title"
          placeholder="请输入问题标题"
          maxlength="200"
          show-word-limit
          clearable
        />
      </el-form-item>

      <!-- 选项设置（单选/多选） -->
      <div v-if="form.type === '1' || form.type === '2'">
        <el-form-item label="备选项">
          <div class="options-container">
            <div 
              v-for="(option, index) in form.options" 
              :key="index"
              class="option-item"
            >
              <el-input
                v-model="form.options[index]"
                placeholder="请输入选项内容"
                style="flex: 1"
                clearable
              />
              <el-button 
                type="danger" 
                size="small" 
                @click="removeOption(index)"
                :disabled="form.options.length <= 2"
                circle
              >
                <el-icon><Minus /></el-icon>
              </el-button>
            </div>
            
            <el-button 
              type="primary" 
              size="small" 
              @click="addOption"
              style="margin-top: 10px"
            >
              <el-icon><Plus /></el-icon>
              添加选项
            </el-button>
          </div>
        </el-form-item>
      </div>

      <!-- 必答設置 -->
      <el-form-item label="是否必答">
        <el-switch
          v-model="form.required"
          active-text="是"
          inactive-text="否"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script>
import { Plus, Minus } from '@element-plus/icons-vue'

export default {
  name: 'QuestionDialog',
  components: {
    Plus,
    Minus
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
      }
    }
  },
  methods: {
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
      this.$emit('update:visible', false)
    },

    handleSubmit() {
      this.$refs.formRef.validate((valid) => {
        if (valid) {
          let options = null
          if (this.form.type === '1' || this.form.type === '2') {
            options = this.form.options.filter(option => option.trim() !== '')
            if (options.length < 2) {
              this.$message.warning('单选和多选题至少需要 2 个选项')
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
          this.handleClose()
        }
      })
    }
  }
}
</script>

<style scoped>
.options-container {
  width: 100%;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
