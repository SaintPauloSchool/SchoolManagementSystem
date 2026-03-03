<template>
  <div class="publish-notification">
    <!-- 步驟指示器 -->
    <div class="steps-wrapper">
      <el-steps :active="currentStep" align-center finish-status="success">
        <el-step title="基本信息" />
        <el-step title="發送設置" />
      </el-steps>
    </div>

    <!-- 表單內容 -->
    <div class="form-wrapper">
      <transition name="slide" mode="out-in">
        <component :is="currentStep === 0 ? 'div' : 'div'" :key="currentStep" class="step-content">
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
            @prev="handlePrev"
            @submit="handleSubmit"
          />
        </component>
      </transition>
    </div>
  </div>
</template>

<script>
import BasicInfoForm from './BasicInfoForm.vue'
import SendSettingsForm from './SendSettingsForm.vue'

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
    handleNext() {
      this.currentStep = 1
    },

    handlePrev() {
      this.currentStep = 0
    },

    async handleSubmit() {
      try {
        this.formData.senderId = this.$store?.state?.user?.userId || null
        this.formData.senderName = this.$store?.state?.user?.username || '當前用戶'
        this.formData.status = '1'
        
        console.log('發布通知:', this.formData)
        
        // 模擬提交
        this.$message.success('通知發布成功')
        this.$emit('publish-success')
        this.resetForm()
      } catch (error) {
        console.error('發布失敗:', error)
        this.$message.error('發布失敗，請稍後重試')
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
    }
  }
}
</script>

<style scoped>
.publish-notification {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: #f9fafb;
}

.steps-wrapper {
  padding: 24px 32px;
  background-color: #ffffff;
  border-bottom: 1px solid #e5e7eb;
}

.form-wrapper {
  flex: 1;
  overflow-y: auto;
  padding: 32px;
}

.step-content {
  max-width: 1000px;
  margin: 0 auto;
  background-color: #ffffff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

/* 動畫效果 */
.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.slide-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* 響應式設計 */
@media (max-width: 768px) {
  .steps-wrapper {
    padding: 16px 20px;
  }
  
  .form-wrapper {
    padding: 16px;
  }
  
  .step-content {
    padding: 20px;
  }
}

@media (max-width: 576px) {
  .steps-wrapper {
    padding: 12px 16px;
  }
  
  .step-content {
    padding: 16px;
  }
}
</style>
