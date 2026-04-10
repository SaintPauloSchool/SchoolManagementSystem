<template>
  <div class="publish-notification">
    <!-- 步驟指示器 -->
    <div class="steps-wrapper">
      <div class="simple-steps">
        <div 
          :class="['step-item', { active: currentStep === 0 }]"
          @click="currentStep = 0"
        >
          <span class="step-title">基本信息</span>
        </div>
        <div class="step-line"></div>
        <div 
          :class="['step-item', { active: currentStep === 1 }]"
          @click="currentStep = 1"
        >
          <span class="step-title">發送設置</span>
        </div>
      </div>
    </div>

    <!-- 表單內容 -->
    <div class="form-wrapper">
      <transition name="slide" mode="out-in">
        <div :key="currentStep" class="step-content">
          <BasicInfoForm 
            v-if="currentStep === 0"
            ref="basicFormRef"
            :form-data="formData"
            @next="handleNext"
          />
          <SendSettingsForm
            v-else
            ref="sendFormRef"
            :form-data="formData"
            :submitting="submitting"
            @prev="handlePrev"
            @submit="handleSubmit"
          />
        </div>
      </transition>
    </div>
  </div>
</template>

<script>
import BasicInfoForm from './BasicInfoForm.vue'
import SendSettingsForm from './SendSettingsForm.vue'
import request from '@/utils/request'

export default {
  name: 'PublishNotification',
  components: {
    BasicInfoForm,
    SendSettingsForm
  },
  emits: ['publish-success'],
  data() {
    return {
      currentStep: 0,
      submitting: false,
      formData: {
        title: '',
        content: '',
        senderId: null,
        senderName: '',
        jumpUrl: '',
        attachmentUrls: [],
        status: '0',
        receivers: [],
        ccs: [],
        replyDeadline: null,
        questions: []
      }
    }
  },
  methods: {
    scrollToTop() {
      this.$nextTick(() => {
        const wrapper = document.querySelector('.content-wrapper')
        if (wrapper) {
          wrapper.scrollTo({ top: 0, behavior: 'smooth' })
        }
      })
    },

    handleNext() {
      this.currentStep = 1
      this.scrollToTop()
    },

    handlePrev() {
      this.currentStep = 0
      this.scrollToTop()
    },

    async handleSubmit() {
      if (this.submitting) return
      
      try {
        this.submitting = true
        this.formData.senderId = this.$store?.state?.user?.userId || null
        this.formData.senderName = this.$store?.state?.user?.username || '當前用戶'
        this.formData.status = '1'
        
        // 準備提交數據
        const submitData = {
          // 通知基本信息
          title: this.formData.title,
          content: this.formData.content,
          jumpUrl: this.formData.jumpUrl || null,
          attachmentUrls: this.formData.attachmentUrls ? JSON.stringify(this.formData.attachmentUrls) : null,
          status: this.formData.status,
          replyDeadline: this.formData.replyDeadline ? this.formatDate(this.formData.replyDeadline) : null,
          
          // 接收對象
          receivers: this.formData.receivers,
          ccs: this.formData.ccs,
          
          // 問題列表 - 需要處理表單問題的嵌套結構
          questions: (this.formData.questions || []).map(q => {
            if (q.questionType === '5' && q.questions) {
              // 表單問題：需要將嵌套的問題列表展開
              return {
                questionTitle: q.title || '問卷調查',
                questionType: '5',
                isRequired: '0',
                content: JSON.stringify({
                  questionnaire: q.questionnaireData,
                  questions: q.questions
                })
              }
            } else {
              // 普通問題
              return {
                questionTitle: q.title,
                questionType: q.type || q.questionType,
                options: q.options ? JSON.stringify(q.options) : null,
                isRequired: q.required ? '1' : '0',
                content: q.content || null,
                logicRules: q.logicRuleList ? JSON.stringify(q.logicRuleList) : null,
                fillBlanks: q.fillBlanks ? JSON.stringify(q.fillBlanks) : null,
                correctAnswers: q.correctAnswers ? JSON.stringify(q.correctAnswers) : null
              }
            }
          })
        }
        
        // 調用後端 API
        const response = await request({
          url: '/system/notification',
          method: 'post',
          data: submitData
        })
        
        if (response.code === 200 || response.code === 0) {
          this.$message.success('通知發布成功')
          this.$emit('publish-success')
          this.resetForm()
        } else {
          throw new Error(response.msg || '發布失敗')
        }
      } catch (error) {
        console.error('發布失敗:', error)
        this.$message.error(error.message || '發布失敗，請稍後重試')
      } finally {
        this.submitting = false
      }
    },

    resetForm() {
      this.formData = {
        title: '',
        content: '',
        senderId: null,
        senderName: '',
        jumpUrl: '',
        attachmentUrls: [],
        status: '0',
        receivers: [],
        ccs: [],
        replyDeadline: null,
        questions: []
      }
      this.currentStep = 0
    },
    
    formatDate(date) {
      if (!date) return null
      const d = new Date(date)
      const year = d.getFullYear()
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      const hours = String(d.getHours()).padStart(2, '0')
      const minutes = String(d.getMinutes()).padStart(2, '0')
      const seconds = String(d.getSeconds()).padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
    }
  }
}
</script>

<style scoped>
.publish-notification {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  background-color: #f9fafb;
}

.steps-wrapper {
  padding: 16px 36px;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
}

.simple-steps {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 40px;
  max-width: 400px;
  margin: 0 auto;
}

.step-item {
  cursor: pointer;
  transition: all 0.3s ease;
}

.step-title {
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
  padding: 6px 12px;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.step-item.active .step-title {
  color: #3b82f6;
  font-weight: 600;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
}

.step-line {
  width: 30px;
  height: 2px;
  background: #e5e7eb;
  transition: all 0.3s ease;
}

.step-item:first-child.active ~ .step-line,
.step-item:last-child.active ~ .step-line {
  background: #3b82f6;
}

.form-wrapper {
  flex: 1;
  padding: 32px;
}

.step-content {
  max-width: 1000px;
  margin: 0 auto;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border-radius: 16px;
  padding: 36px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

.step-content:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
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
  .steps-wrapper {
    padding: 20px 24px;
  }
  
  .form-wrapper {
    padding: 20px;
  }
  
  .step-content {
    padding: 24px;
  }
}

@media (max-width: 576px) {
  .steps-wrapper {
    padding: 16px 20px;
  }
  
  .step-content {
    padding: 20px;
  }
}
</style>
