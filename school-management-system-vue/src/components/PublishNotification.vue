<template>
  <div class="publish-notification">
    <!-- 步驟指示器 -->
    <div class="steps-wrapper">
      <div class="simple-steps">
        <div :class="['step-item', { active: currentStep === 0 }]">
          <span class="step-title">基本資訊</span>
        </div>
        <div class="step-line"></div>
        <div :class="['step-item', { active: currentStep === 1 }]">
          <span class="step-title">發送設置</span>
        </div>
      </div>
    </div>

    <div v-if="isCopyMode" class="copy-banner">
      目前為複製模式：資料已由原通知帶入，請檢查並修改後再發佈。發佈後將產生新通知，原通知狀態不變。
    </div>

    <!-- 表單內容 -->
    <div class="form-wrapper" v-loading="copyLoading">
      <transition name="slide" mode="out-in">
        <div v-if="formReady" class="step-content">
          <BasicInfoForm 
            v-show="currentStep === 0"
            ref="basicFormRef"
            :form-data="formData"
            @next="handleNext"
          />
          <SendSettingsForm
            v-show="currentStep === 1"
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
import { ElNotification } from 'element-plus'
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
      formReady: false,
      copyLoading: false,
      isCopyMode: false,
      formData: this.createEmptyForm()
    }
  },
  async created() {
    const copyFromId = sessionStorage.getItem('copyFromNoticeId')
    if (copyFromId) {
      sessionStorage.removeItem('copyFromNoticeId')
      this.isCopyMode = true
      this.copyLoading = true
      try {
        await this.loadFromNotification(copyFromId)
        ElNotification({
          title: '提示',
          message: '已帶入原通知內容，請修改後發佈',
          type: 'success',
          duration: 3000
        })
      } catch (error) {
        console.error('複製通知失敗:', error)
        this.isCopyMode = false
        ElNotification({
          title: '操作失敗',
          message: error.message || '無法載入原通知內容，請稍後重試',
          type: 'error',
          duration: 4000
        })
      } finally {
        this.copyLoading = false
      }
    }
    this.formReady = true
  },
  methods: {
    createEmptyForm() {
      return {
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
        reminderTime: null,
        questions: []
      }
    },

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

    async loadFromNotification(notificationId) {
      const response = await request({
        url: `/system/notification/${notificationId}`,
        method: 'get'
      })
      if (!(response.code === 200 || response.code === 0) || !response.data) {
        throw new Error(response.msg || '無法載入原通知內容，請稍後重試')
      }
      this.formData = this.mapDetailToForm(response.data)
    },

    mapDetailToForm(vo) {
      const notification = vo.notification || {}
      const now = Date.now()

      let attachmentUrls = []
      if (notification.attachmentUrls) {
        try {
          const parsed = typeof notification.attachmentUrls === 'string'
            ? JSON.parse(notification.attachmentUrls)
            : notification.attachmentUrls
          if (Array.isArray(parsed)) {
            attachmentUrls = parsed.map((item, index) => {
              if (typeof item === 'string') {
                return {
                  name: decodeURIComponent(item.substring(item.lastIndexOf('/') + 1)) || `附件${index + 1}`,
                  url: item
                }
              }
              return {
                name: item.name || `附件${index + 1}`,
                url: item.url || item._originalUrl || ''
              }
            }).filter(item => item.url)
          }
        } catch (e) {
          console.warn('解析附件失敗:', e)
          attachmentUrls = []
        }
      }

      let replyDeadline = notification.replyDeadline || null
      let reminderTime = notification.reminderTime || null
      if (replyDeadline && new Date(replyDeadline).getTime() < now) {
        replyDeadline = null
        reminderTime = null
      } else if (reminderTime) {
        const tomorrow = new Date()
        tomorrow.setHours(0, 0, 0, 0)
        tomorrow.setDate(tomorrow.getDate() + 1)
        if (new Date(reminderTime).getTime() < tomorrow.getTime()) {
          reminderTime = null
        }
      }

      const receivers = (vo.receivers || []).map(receiver => ({
        receiveType: receiver.receiveType,
        receiveData: receiver.receiveData,
        receiveNames: receiver.receiveNames || [],
        receiveDeptGroups: receiver.receiveDeptGroups || []
      }))

      const ccs = (vo.ccs || []).map(cc => ({
        ccType: cc.ccType,
        ccData: cc.ccData,
        ccNames: cc.ccNames || [],
        ccDeptGroups: cc.ccDeptGroups || []
      }))

      const questions = (vo.questions || []).map((q, index) => this.mapQuestionToForm(q, index))

      return {
        title: notification.title || '',
        content: notification.content || '',
        senderId: null,
        senderName: '',
        jumpUrl: notification.jumpUrl || '',
        attachmentUrls,
        status: '0',
        receivers,
        ccs,
        replyDeadline,
        reminderTime,
        questions
      }
    },

    mapQuestionToForm(question, index) {
      const questionType = String(question.questionType || question.type || '')
      if (questionType === '5') {
        const parsed = this.parseQuestionnaireContent(question.content)
        return {
          id: Date.now() + index,
          questionType: '5',
          type: '5',
          title: parsed.title || question.questionTitle || '問卷調查',
          description: parsed.description || '',
          questionnaireData: parsed.questionnaireData,
          questions: parsed.questions
        }
      }

      let options = question.options
      let logicRules = question.logicRules
      let fillBlanks = question.fillBlanks
      let correctAnswers = question.correctAnswers
      try {
        if (typeof options === 'string') options = JSON.parse(options)
      } catch (e) { options = null }
      try {
        if (typeof logicRules === 'string') logicRules = JSON.parse(logicRules)
      } catch (e) { logicRules = null }
      try {
        if (typeof fillBlanks === 'string') fillBlanks = JSON.parse(fillBlanks)
      } catch (e) { fillBlanks = null }
      try {
        if (typeof correctAnswers === 'string') correctAnswers = JSON.parse(correctAnswers)
      } catch (e) { correctAnswers = null }

      return {
        id: Date.now() + index,
        title: question.questionTitle || '',
        type: questionType,
        questionType,
        options: options || [],
        required: question.isRequired === '1' || question.isRequired === 1 || question.required === true,
        content: question.content || null,
        uploadNote: question.uploadNote || (questionType === '4' ? '此處由用戶端上傳...' : ''),
        logicRuleList: Array.isArray(logicRules) ? logicRules : [],
        fillBlanks: Array.isArray(fillBlanks) ? fillBlanks : [],
        correctAnswers: correctAnswers || null
      }
    },

    parseQuestionnaireContent(content) {
      const fallback = {
        title: '問卷調查',
        description: '',
        questionnaireData: { title: '問卷調查', description: '' },
        questions: []
      }
      if (!content) return fallback
      try {
        let parsed = content
        if (typeof parsed === 'string') parsed = JSON.parse(parsed)
        if (typeof parsed === 'string') parsed = JSON.parse(parsed)
        if (!parsed || typeof parsed !== 'object') return fallback

        const title = parsed.questionnaire
          ? (parsed.questionnaire.title || '問卷調查')
          : (parsed.title || '問卷調查')
        const description = parsed.questionnaire
          ? (parsed.questionnaire.description || '')
          : (parsed.description || '')

        // 注意：空陣列 [] 在 JS 是 truthy，不能用 || 回退
        let questionsArray = []
        if (Array.isArray(parsed.questions) && parsed.questions.length > 0) {
          questionsArray = parsed.questions
        } else if (Array.isArray(parsed.questionnaire?.questions) && parsed.questionnaire.questions.length > 0) {
          questionsArray = parsed.questionnaire.questions
        }

        const questionnaireData = parsed.questionnaire
          ? {
              title: parsed.questionnaire.title || title,
              description: parsed.questionnaire.description || description
            }
          : { title, description }

        return {
          title,
          description,
          questionnaireData,
          questions: this.ensureUniqueNestedIds(
            questionsArray.map((q, index) => this.normalizeNestedQuestion(q, index))
          )
        }
      } catch (e) {
        console.warn('解析問卷內容失敗:', e)
        return fallback
      }
    },

    ensureUniqueNestedIds(questions) {
      const used = new Set()
      let next = 1
      const alloc = () => {
        while (used.has(String(next))) next += 1
        const id = next
        used.add(String(id))
        next += 1
        return id
      }
      return (questions || []).map(q => {
        const key = q.id == null || q.id === '' ? null : String(q.id)
        if (key == null || used.has(key)) {
          return { ...q, id: alloc() }
        }
        used.add(key)
        const n = Number(q.id)
        if (!Number.isNaN(n) && n >= next) next = n + 1
        return q
      })
    },

    normalizeNestedQuestion(question, index) {
      const type = String(question?.type || question?.questionType || '')
      let options = question?.options
      let logicRuleList = question?.logicRuleList || question?.logicRules || []
      let fillBlanks = question?.fillBlanks
      let correctAnswers = question?.correctAnswers

      if (typeof options === 'string') {
        try { options = JSON.parse(options) } catch (e) { options = [] }
      }
      if (typeof logicRuleList === 'string') {
        try { logicRuleList = JSON.parse(logicRuleList) } catch (e) { logicRuleList = [] }
      }
      if (typeof fillBlanks === 'string') {
        try { fillBlanks = JSON.parse(fillBlanks) } catch (e) { fillBlanks = [] }
      }
      if (typeof correctAnswers === 'string') {
        try { correctAnswers = JSON.parse(correctAnswers) } catch (e) { correctAnswers = [] }
      }

      const normalizedId = question?.id != null && question?.id !== ''
        ? question.id
        : Date.now() + index

      return {
        ...question,
        id: normalizedId,
        type,
        questionType: type,
        title: question?.title || '',
        description: question?.description || '',
        required: question?.required === true || question?.required === '1' || question?.isRequired === '1',
        options: Array.isArray(options) ? options : (type === '1' || type === '2' ? ['', ''] : []),
        logicRuleList: (Array.isArray(logicRuleList) ? logicRuleList : []).map((rule, ruleIndex) => ({
          ...rule,
          id: rule?.id || `rule-${normalizedId}-${ruleIndex}`,
          optionIndex: rule?.optionIndex ?? ruleIndex,
          jumpTarget: rule?.jumpTarget === 'next' || rule?.jumpTarget === 'end' || rule?.jumpTarget == null
            ? (rule?.jumpTarget || 'next')
            : rule.jumpTarget
        })),
        content: question?.content || '',
        uploadNote: question?.uploadNote || (type === '4' ? '此處由用戶端上傳...' : ''),
        fillBlanks: Array.isArray(fillBlanks) ? fillBlanks : [],
        correctAnswers: Array.isArray(correctAnswers) ? correctAnswers : [],
        placeholder: question?.placeholder || '',
        minOptions: question?.minOptions || 1,
        maxOptions: question?.maxOptions ?? null
      }
    },

    async handleSubmit() {
      if (this.submitting) return
      
      try {
        this.submitting = true
        this.formData.senderId = this.$store?.state?.user?.userId || null
        this.formData.status = '1'
        
        // 準備提交數據
        const submitData = {
          // 通知基本資訊
          title: this.formData.title,
          content: this.formData.content,
          jumpUrl: this.formData.jumpUrl || null,
          attachmentUrls: this.formData.attachmentUrls ? JSON.stringify(this.formData.attachmentUrls) : null,
          status: this.formData.status,
          replyDeadline: this.formData.replyDeadline ? this.formatDate(this.formData.replyDeadline) : null,
          reminderTime: this.formData.reminderTime || null,
          
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
          ElNotification({
            title: '操作成功',
            message: this.isCopyMode ? '新通知已發佈' : '已發佈',
            type: 'success',
            duration: 3000
          })
          this.$emit('publish-success')
          this.resetForm()
        } else {
          throw new Error(response.msg || '發佈失敗')
        }
      } catch (error) {
        console.error('發佈失敗:', error)
        ElNotification({ title: "操作失敗", message: error.message || '發佈失敗，請稍後重試', type: "error", duration: 4000 })
      } finally {
        this.submitting = false
      }
    },

    resetForm() {
      this.formData = this.createEmptyForm()
      this.currentStep = 0
      this.isCopyMode = false
      this.$nextTick(() => {
        this.$refs.sendFormRef?.resetForm()
        this.$refs.basicFormRef?.loadSenderName()
      })
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

.copy-banner {
  margin: 0;
  padding: 12px 36px;
  background: #ecfdf5;
  border-bottom: 1px solid #bbf7d0;
  color: #166534;
  font-size: 14px;
  line-height: 1.5;
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
  cursor: default;
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

  .copy-banner {
    padding: 12px 24px;
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

  .copy-banner {
    padding: 12px 20px;
    font-size: 13px;
  }
  
  .step-content {
    padding: 20px;
  }
}
</style>
