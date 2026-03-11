<template>
  <div class="notification-detail">
    <!-- 基本信息 -->
    <div class="detail-section">
      <el-descriptions :column="1" border size="default">
        <el-descriptions-item label="通知標題">{{ notification.title }}</el-descriptions-item>
        <el-descriptions-item label="發送人">{{ notification.senderName }}</el-descriptions-item>
        <el-descriptions-item label="發布時間">{{ notification.createTime }}</el-descriptions-item>
        <el-descriptions-item label="狀態">
          <el-tag :type="getStatusType(notification.status)" size="small">
            {{ getStatusText(notification.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="跳轉連結" v-if="notification.jumpUrl">
          <el-link type="primary" :href="notification.jumpUrl" target="_blank">
            {{ notification.jumpUrl }}
          </el-link>
        </el-descriptions-item>
        <el-descriptions-item label="回复截止时间" v-if="notification.replyDeadline">
          <el-tag type="warning" size="small">
            {{ notification.replyDeadline }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <!-- 通知正文 -->
    <div class="content-section">
      <h3 class="section-title">
        <el-icon><Document /></el-icon>
        通知正文
      </h3>
      <div class="content-text" v-html="notification.content"></div>
    </div>

    <!-- 附件 -->
    <div v-if="attachmentUrls.length > 0" class="attachments-section">
      <h3 class="section-title">
        <el-icon><FolderOpened /></el-icon>
        附件列表
      </h3>
      <div class="attachment-list">
        <el-tag
          v-for="(url, index) in attachmentUrls"
          :key="index"
          type="info"
          class="attachment-tag"
        >
          <el-icon><Document /></el-icon>
          附件 {{ index + 1 }}
        </el-tag>
      </div>
    </div>

    <!-- 問題列表 -->
    <div v-if="notification.questions && notification.questions.length > 0" class="questions-section">
      <h3 class="section-title">
        <el-icon><QuestionFilled /></el-icon>
        相關問題 ({{ notification.questions.length }})
      </h3>
      <div class="questions-list">
        <div 
          v-for="question in notification.questions" 
          :key="question.questionId" 
          class="question-item"
        >
          <div class="question-header">
            <span class="question-number">{{ getQuestionIndex(question) }}.</span>
            <span class="question-title">{{ question.questionTitle }}</span>
            <el-tag size="small" type="success">
              {{ getQuestionTypeText(question.questionType) }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 接收對象 -->
    <div v-if="notification.receivers && notification.receivers.length > 0" class="receivers-section">
      <h3 class="section-title">
        <el-icon><User /></el-icon>
        接收對象 ({{ notification.receivers.length }})
      </h3>
      <div class="receiver-list">
        <el-tag 
          v-for="receiver in notification.receivers" 
          :key="receiver.receiverId"
          type="primary"
          class="receiver-tag"
        >
          {{ getReceiveTypeText(receiver.receiveType) }}: {{ receiver.receiveNames }}
        </el-tag>
      </div>
    </div>

    <!-- 抄送對象 -->
    <div v-if="notification.ccs && notification.ccs.length > 0" class="ccs-section">
      <h3 class="section-title">
        <el-icon><Message /></el-icon>
        抄送對象 ({{ notification.ccs.length }})
      </h3>
      <div class="cc-list">
        <el-tag 
          v-for="cc in notification.ccs" 
          :key="cc.ccId"
          type="info"
          class="cc-tag"
        >
          {{ getCcTypeText(cc.ccType) }}: {{ cc.ccNames }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script>
import { Document, FolderOpened, QuestionFilled, User, Message } from '@element-plus/icons-vue'

export default {
  name: 'NotificationDetail',
  props: {
    notification: {
      type: Object,
      required: true
    }
  },
  computed: {
    attachmentUrls() {
      if (this.notification.attachmentUrls) {
        try {
          return JSON.parse(this.notification.attachmentUrls)
        } catch (e) {
          return []
        }
      }
      return []
    }
  },
  methods: {
    getStatusType(status) {
      const statusMap = {
        '0': 'info',
        '1': 'success',
        '2': 'warning'
      }
      return statusMap[status] || 'info'
    },

    getStatusText(status) {
      const statusMap = {
        '0': '草稿',
        '1': '已發布',
        '2': '已撤回'
      }
      return statusMap[status] || '未知'
    },

    getQuestionTypeText(type) {
      const typeMap = {
        '1': '單選',
        '2': '多選',
        '3': '填空',
        '4': '附件上傳'
      }
      return typeMap[type] || '未知'
    },

    getReceiveTypeText(type) {
      const typeMap = {
        '1': '班級',
        '2': '學生/家長',
        '3': '教职员工'
      }
      return typeMap[type] || '未知'
    },

    getCcTypeText(type) {
      const typeMap = {
        '1': '教职员工',
        '2': '學校通訊錄'
      }
      return typeMap[type] || '未知'
    },

    getQuestionIndex(question) {
      const index = this.notification.questions.findIndex(q => q.questionId === question.questionId)
      return index + 1
    }
  }
}
</script>

<style scoped>
.notification-detail {
  padding: 8px;
}

.detail-section {
  margin-bottom: 24px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.section-title .el-icon {
  color: #3b82f6;
  font-size: 18px;
}

.content-section {
  margin-bottom: 24px;
}

.content-text {
  padding: 20px;
  background-color: #f9fafb;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  min-height: 100px;
  line-height: 1.8;
  color: #374151;
}

.attachments-section,
.questions-section,
.receivers-section,
.ccs-section {
  margin-bottom: 24px;
}

.attachment-list,
.receiver-list,
.cc-list,
.questions-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.attachment-tag,
.receiver-tag,
.cc-tag {
  cursor: pointer;
  transition: all 0.2s ease;
}

.attachment-tag:hover,
.receiver-tag:hover,
.cc-tag:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.question-item {
  padding: 14px 18px;
  background-color: #f9fafb;
  border-radius: 8px;
  margin-bottom: 10px;
  border: 1px solid #e5e7eb;
}

.question-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.question-number {
  font-weight: 700;
  color: #3b82f6;
  font-size: 14px;
  min-width: 24px;
}

.question-title {
  flex: 1;
  font-weight: 500;
  color: #374151;
}
</style>
