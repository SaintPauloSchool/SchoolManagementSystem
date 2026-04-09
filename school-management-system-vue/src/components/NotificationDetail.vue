<template>
  <div class="notification-detail">
    <!-- 顶部标题横幅 -->
    <div class="detail-hero">
      <div class="hero-icon-wrap">
        <el-icon :size="28"><Bell /></el-icon>
      </div>
      <div class="hero-text">
        <h2 class="hero-title">{{ notification.title }}</h2>
        <div class="hero-meta">
          <span class="meta-item">
            <el-icon :size="14"><User /></el-icon>
            {{ notification.senderName }}
          </span>
          <span class="meta-divider">·</span>
          <span class="meta-item">
            <el-icon :size="14"><Clock /></el-icon>
            {{ notification.createTime }}
          </span>
          <span class="meta-divider">·</span>
          <el-tag
            :type="getStatusType(notification.status)"
            size="small"
            effect="dark"
            round
            class="status-tag"
          >
            {{ getStatusText(notification.status) }}
          </el-tag>
        </div>
      </div>
    </div>

    <!-- 信息卡片 -->
    <div class="info-cards">
      <div class="info-card" v-if="notification.jumpUrl">
        <div class="info-card-icon link-icon">
          <el-icon :size="18"><Link /></el-icon>
        </div>
        <div class="info-card-body">
          <span class="info-card-label">跳轉連結</span>
          <el-link type="primary" :href="notification.jumpUrl" target="_blank" :underline="false" class="jump-link">
            {{ notification.jumpUrl }}
          </el-link>
        </div>
      </div>
      <div class="info-card" v-if="notification.replyDeadline">
        <div class="info-card-icon deadline-icon">
          <el-icon :size="18"><AlarmClock /></el-icon>
        </div>
        <div class="info-card-body">
          <span class="info-card-label">回复截止時間</span>
          <span class="info-card-value deadline-value">{{ notification.replyDeadline }}</span>
        </div>
      </div>
    </div>

    <!-- 通知正文 -->
    <div class="section-card content-card">
      <div class="section-header">
        <div class="section-icon content-icon">
          <el-icon :size="16"><Document /></el-icon>
        </div>
        <h3 class="section-label">通知正文</h3>
      </div>
      <div class="content-body" v-html="notification.content"></div>
    </div>

    <!-- 附件 -->
    <div v-if="attachmentUrls.length > 0" class="section-card">
      <div class="section-header">
        <div class="section-icon attachment-icon">
          <el-icon :size="16"><FolderOpened /></el-icon>
        </div>
        <h3 class="section-label">附件列表</h3>
        <span class="section-count">{{ attachmentUrls.length }}</span>
      </div>
      <div class="chip-list">
        <div
          v-for="(url, index) in attachmentUrls"
          :key="index"
          class="chip attachment-chip"
        >
          <el-icon :size="14"><Document /></el-icon>
          <span>附件 {{ index + 1 }}</span>
        </div>
      </div>
    </div>

    <!-- 問題列表 -->
    <div v-if="notification.questions && notification.questions.length > 0" class="section-card">
      <div class="section-header">
        <div class="section-icon question-icon">
          <el-icon :size="16"><QuestionFilled /></el-icon>
        </div>
        <h3 class="section-label">相關問題</h3>
        <span class="section-count">{{ notification.questions.length }}</span>
      </div>
      <div class="questions-grid">
        <div
          v-for="question in notification.questions"
          :key="question.questionId"
          class="question-card"
        >
          <div class="question-index">{{ getQuestionIndex(question) }}</div>
          <div class="question-info">
            <span class="question-title">{{ question.questionTitle }}</span>
            <el-tag size="small" effect="plain" round class="question-type-tag">
              {{ getQuestionTypeText(question.questionType) }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 接收對象 -->
    <div v-if="notification.receivers && notification.receivers.length > 0" class="section-card">
      <div class="section-header">
        <div class="section-icon receiver-icon">
          <el-icon :size="16"><User /></el-icon>
        </div>
        <h3 class="section-label">接收對象</h3>
        <span class="section-count">{{ notification.receivers.length }}</span>
      </div>
      <div class="chip-list">
        <div
          v-for="receiver in notification.receivers"
          :key="receiver.receiverId"
          class="chip receiver-chip"
        >
          <el-icon :size="14"><User /></el-icon>
          <span>{{ getReceiveTypeText(receiver.receiveType) }}: {{ getReceiverDisplayText(receiver) }}</span>
        </div>
      </div>
    </div>

    <!-- 抄送對象 -->
    <div v-if="notification.ccs && notification.ccs.length > 0" class="section-card">
      <div class="section-header">
        <div class="section-icon cc-icon">
          <el-icon :size="16"><Message /></el-icon>
        </div>
        <h3 class="section-label">抄送對象</h3>
        <span class="section-count">{{ notification.ccs.length }}</span>
      </div>
      <div class="chip-list">
        <div
          v-for="cc in notification.ccs"
          :key="cc.ccId"
          class="chip cc-chip"
        >
          <el-icon :size="14"><Message /></el-icon>
          <span>{{ getCcTypeText(cc.ccType) }}: {{ cc.ccNames }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { Document, FolderOpened, QuestionFilled, User, Message, Bell, Clock, Link, AlarmClock } from '@element-plus/icons-vue'

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
        '2': '學生/家長'
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
    },

    getReceiverDisplayText(receiver) {
      if (receiver.receiveData) {
        try {
          const dataArr = JSON.parse(receiver.receiveData);
          let allNames = [];
          dataArr.forEach(group => {
            if (group.receive_names && Array.isArray(group.receive_names)) {
              allNames.push(...group.receive_names);
            }
          });
          return allNames.join(', ');
        } catch (e) {
          return '解析錯誤';
        }
      }
      return '';
    }
  }
}
</script>

<style scoped>
.notification-detail {
  padding: 0;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* ===== 顶部横幅 ===== */
.detail-hero {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 24px 28px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 14px;
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
}

.detail-hero::before {
  content: '';
  position: absolute;
  top: -40%;
  right: -10%;
  width: 200px;
  height: 200px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 50%;
  pointer-events: none;
}

.detail-hero::after {
  content: '';
  position: absolute;
  bottom: -30%;
  left: 20%;
  width: 120px;
  height: 120px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 50%;
  pointer-events: none;
}

.hero-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.hero-text {
  flex: 1;
  min-width: 0;
}

.hero-title {
  margin: 0 0 10px;
  font-size: 20px;
  font-weight: 700;
  color: #ffffff;
  line-height: 1.3;
  letter-spacing: 0.01em;
}

.hero-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
}

.meta-divider {
  color: rgba(255, 255, 255, 0.4);
  font-size: 12px;
}

.status-tag {
  font-weight: 600;
  letter-spacing: 0.05em;
}

/* ===== 信息卡片行 ===== */
.info-cards {
  display: flex;
  gap: 14px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.info-card {
  flex: 1;
  min-width: 240px;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  background: #ffffff;
  border: 1px solid #e8ecf1;
  border-radius: 12px;
  transition: all 0.25s ease;
}

.info-card:hover {
  border-color: #c7d2fe;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.1);
  transform: translateY(-1px);
}

.info-card-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.link-icon {
  background: linear-gradient(135deg, #e0e7ff, #c7d2fe);
  color: #4f46e5;
}

.deadline-icon {
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  color: #d97706;
}

.info-card-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.info-card-label {
  font-size: 12px;
  color: #9ca3af;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.info-card-value {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.deadline-value {
  color: #d97706;
}

.jump-link {
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

/* ===== 区块卡片 ===== */
.section-card {
  background: #ffffff;
  border: 1px solid #e8ecf1;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 16px;
  transition: all 0.25s ease;
}

.section-card:hover {
  border-color: #d1d5db;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid #f3f4f6;
}

.section-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.content-icon {
  background: linear-gradient(135deg, #dbeafe, #bfdbfe);
  color: #2563eb;
}

.attachment-icon {
  background: linear-gradient(135deg, #d1fae5, #a7f3d0);
  color: #059669;
}

.question-icon {
  background: linear-gradient(135deg, #fce7f3, #fbcfe8);
  color: #db2777;
}

.receiver-icon {
  background: linear-gradient(135deg, #e0e7ff, #c7d2fe);
  color: #4f46e5;
}

.cc-icon {
  background: linear-gradient(135deg, #f3e8ff, #e9d5ff);
  color: #7c3aed;
}

.section-label {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  flex: 1;
}

.section-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 22px;
  padding: 0 7px;
  border-radius: 11px;
  background: #f3f4f6;
  color: #6b7280;
  font-size: 12px;
  font-weight: 600;
}

/* ===== 正文区域 ===== */
.content-body {
  padding: 18px 20px;
  background: #f9fafb;
  border-radius: 10px;
  min-height: 80px;
  line-height: 1.85;
  color: #374151;
  font-size: 14px;
  border: 1px solid #f3f4f6;
}

/* ===== 芯片/标签列表 ===== */
.chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  cursor: default;
  transition: all 0.2s ease;
  line-height: 1.3;
}

.chip:hover {
  transform: translateY(-1px);
}

.attachment-chip {
  background: #ecfdf5;
  color: #065f46;
  border: 1px solid #a7f3d0;
}

.attachment-chip:hover {
  background: #d1fae5;
  box-shadow: 0 3px 10px rgba(5, 150, 105, 0.12);
}

.receiver-chip {
  background: #eef2ff;
  color: #3730a3;
  border: 1px solid #c7d2fe;
}

.receiver-chip:hover {
  background: #e0e7ff;
  box-shadow: 0 3px 10px rgba(79, 70, 229, 0.12);
}

.cc-chip {
  background: #f5f3ff;
  color: #5b21b6;
  border: 1px solid #ddd6fe;
}

.cc-chip:hover {
  background: #ede9fe;
  box-shadow: 0 3px 10px rgba(124, 58, 237, 0.12);
}

/* ===== 问题卡片网格 ===== */
.questions-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.question-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  background: #fafbfc;
  border: 1px solid #f0f0f4;
  border-radius: 10px;
  transition: all 0.2s ease;
}

.question-card:hover {
  background: #f5f6fa;
  border-color: #e0e3eb;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.question-index {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}

.question-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.question-title {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.question-type-tag {
  flex-shrink: 0;
  font-size: 12px;
  border-color: #e0e7ff !important;
  color: #4f46e5 !important;
  background: #eef2ff !important;
}

/* ===== 动画 ===== */
.section-card {
  animation: fadeSlideUp 0.35s ease both;
}

.section-card:nth-child(2) { animation-delay: 0.06s; }
.section-card:nth-child(3) { animation-delay: 0.12s; }
.section-card:nth-child(4) { animation-delay: 0.18s; }
.section-card:nth-child(5) { animation-delay: 0.24s; }
.section-card:nth-child(6) { animation-delay: 0.30s; }

@keyframes fadeSlideUp {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.detail-hero {
  animation: fadeSlideUp 0.3s ease both;
}

.info-cards {
  animation: fadeSlideUp 0.3s ease 0.05s both;
}
</style>
