<template>
  <div class="notification-detail">
    <!-- 顶部标题横幅 -->
    <div class="detail-hero">
      <div class="hero-text">
        <h2 class="hero-title">通知標題：{{ notification.title }}</h2>
        <div class="hero-meta">
          <el-tag
            :type="getStatusType(notification.status)"
            size="small"
            effect="dark"
            round
            class="status-tag"
          >
            {{ getStatusText(notification.status) }}
          </el-tag>
          <span class="meta-divider">·</span>
          <span class="meta-item">
            <el-icon :size="14"><User /></el-icon>
            發送人：{{ notification.senderName }}
          </span>
          <span class="meta-divider">·</span>
          <span class="meta-item">
            <el-icon :size="14"><Clock /></el-icon>
            發佈時間：{{ notification.createTime }}
          </span>
          <span v-if="notification.replyDeadline" class="meta-divider">·</span>
          <span v-if="notification.replyDeadline" class="meta-item">
            <el-icon :size="14"><AlarmClock /></el-icon>
            回复截止：{{ notification.replyDeadline }}
          </span>
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
      
      <!-- 外部連結區塊 -->
      <div v-if="notification.jumpUrl" class="content-jump-link">
        <div class="jump-link-header">
          <el-icon :size="15"><Link /></el-icon>
          <span>外部跳轉連結：</span>
        </div>
        <a :href="notification.jumpUrl" target="_blank" class="actual-link">
          {{ notification.jumpUrl }}
        </a>
      </div>
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
        <a
          v-for="(item, index) in attachmentUrls"
          :key="index"
          :href="getAttachmentUrl(item)"
          target="_blank"
          class="chip attachment-chip attachment-link"
        >
          <el-icon :size="14"><Document /></el-icon>
          <span>{{ getAttachmentName(item, index) }}</span>
        </a>
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
          :class="{ 'is-expanded': isQuestionExpanded(question.questionId) }"
        >
          <!-- 問題頭部（可點擊展開） -->
          <div class="question-header" @click="toggleQuestion(question.questionId)">
            <div class="question-index">{{ getQuestionIndex(question) }}</div>
            <div class="question-info">
              <span class="question-title">{{ question.questionTitle }}</span>
              <el-tag v-if="question.questionType !== '5'" size="small" effect="plain" round class="question-type-tag">
                {{ getQuestionTypeText(question.questionType) }}
              </el-tag>
              <el-tag v-if="question.isRequired === '1'" size="small" type="danger" effect="light" round class="required-tag">
                必答
              </el-tag>
            </div>
            <el-icon class="expand-arrow" :class="{ 'is-rotated': isQuestionExpanded(question.questionId) }">
              <ArrowDown />
            </el-icon>
          </div>

          <!-- 展開內容 -->
          <transition name="expand">
            <div v-show="isQuestionExpanded(question.questionId)" class="question-detail">
              <!-- 選項列表（單選/多選） -->
              <div v-if="getQuestionOptions(question).length > 0" class="detail-block">
                <div class="detail-block-label">
                  <el-icon :size="14"><List /></el-icon>
                  <span>{{ question.questionType === '1' ? '單選選項' : '多選選項' }}</span>
                </div>
                <div class="options-list">
                  <div v-for="(opt, idx) in getQuestionOptions(question)" :key="idx" class="option-item">
                    <span class="option-letter">{{ String.fromCharCode(65 + idx) }}</span>
                    <span class="option-text">{{ opt }}</span>
                  </div>
                </div>
              </div>

              <!-- 填空題欄位 -->
              <div v-if="getQuestionFillBlanks(question).length > 0" class="detail-block">
                <div class="detail-block-label">
                  <el-icon :size="14"><EditPen /></el-icon>
                  <span>填空欄位</span>
                </div>
                <div class="options-list">
                  <div v-for="(blank, idx) in getQuestionFillBlanks(question)" :key="idx" class="option-item">
                    <span class="option-letter">{{ idx + 1 }}</span>
                    <span class="option-text">{{ blank }}</span>
                  </div>
                </div>
              </div>

              <!-- 題目內容 -->
              <template v-if="question.content">
                <!-- 邼輯表單類型：解析JSON問卷結構 -->
                <div v-if="parseQuestionContent(question)" class="detail-block">
                  <div class="detail-block-label">
                    <el-icon :size="14"><Document /></el-icon>
                    <span>表單內容</span>
                  </div>
                  <div class="sub-questionnaire">
                    <!-- 表單標題與描述 -->
                    <div class="sub-q-header">
                      <div class="field-item">
                        <span class="field-label">表單標題：</span>
                        <h4 class="sub-q-title">{{ parseQuestionContent(question).title || '無標題' }}</h4>
                      </div>
                      <div class="field-item" v-if="parseQuestionContent(question).description">
                        <span class="field-label">表單描述：</span>
                        <p class="sub-q-desc">{{ parseQuestionContent(question).description }}</p>
                      </div>
                    </div>
                    <!-- 子問題列表 -->
                    <div class="sub-questions-list">
                      <LogicQuestionItem
                        v-for="(subQ, sIdx) in getRootQuestions(parseQuestionContent(question))"
                        :key="subQ.id"
                        :subQ="subQ"
                        :allQuestions="parseQuestionContent(question).questions"
                        :indexStr="String(sIdx + 1)"
                      />
                    </div>
                  </div>
                </div>
                <!-- 普通HTML內容 -->
                <div v-else class="detail-block">
                  <div class="detail-block-label">
                    <el-icon :size="14"><Document /></el-icon>
                    <span>題目內容</span>
                  </div>
                  <div class="detail-content" v-html="question.content"></div>
                </div>
              </template>

              <!-- 邏輯規則 -->
              <div v-if="getLogicRules(question).length > 0" class="detail-block logic-block">
                <div class="detail-block-label">
                  <el-icon :size="14"><Connection /></el-icon>
                  <span>邏輯規則</span>
                </div>
                <div class="logic-rules-list">
                  <div v-for="(rule, idx) in getLogicRules(question)" :key="idx" class="logic-rule-item">
                    <div class="logic-condition">
                      <span class="logic-keyword">當</span>
                      <span class="logic-text">選擇「{{ rule.optionText || ('選項 ' + (rule.optionIndex + 1)) }}」</span>
                    </div>
                    <el-icon class="logic-arrow-icon" :size="14"><Right /></el-icon>
                    <div class="logic-action">
                      <span class="logic-keyword">{{ rule.action === 'skip' ? '跳轉至' : '顯示' }}</span>
                      <span class="logic-text">問題 {{ rule.targetQuestionIndex || rule.targetQuestionId }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 無內容提示 -->
              <div v-if="!question.content && getQuestionOptions(question).length === 0 && getQuestionFillBlanks(question).length === 0 && getLogicRules(question).length === 0" class="empty-detail">
                <span>無額外內容</span>
              </div>
            </div>
          </transition>
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
      </div>
      <div class="receiver-groups">
        <template v-for="receiver in notification.receivers" :key="receiver.receiverId">
          <div class="receiver-group-card">
            <div class="group-header">
              <el-icon :size="16"><User /></el-icon>
              <span class="group-title">{{ getReceiveTypeText(receiver.receiveType) }}</span>
            </div>
            <div class="group-content">
              <div
                v-for="(groupData, index) in getReceiverGroupedData(receiver)"
                :key="index"
                class="source-chip"
              >
                <span class="source-label">{{ groupData.source }}</span>
                <span class="source-names">{{ groupData.names.join(', ') }}</span>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- 抄送對象 -->
    <div v-if="notification.ccs && notification.ccs.length > 0" class="section-card">
      <div class="section-header">
        <div class="section-icon cc-icon">
          <el-icon :size="16"><Message /></el-icon>
        </div>
        <h3 class="section-label">抄送對象</h3>
      </div>
      <div class="cc-groups">
        <template v-for="cc in [...notification.ccs].sort((a, b) => b.ccType - a.ccType)" :key="cc.ccId">
          <div class="cc-group-card">
            <div class="group-header">
              <el-icon :size="16"><Message /></el-icon>
              <span class="group-title">{{ getCcTypeText(cc.ccType) }}</span>
            </div>
            <div class="group-content">
              <div
                v-for="(ccData, index) in getCcGroupedData(cc)"
                :key="index"
                class="source-chip"
              >
                <span class="source-label">{{ ccData.source }}</span>
                <span class="source-names">{{ ccData.names.join(', ') }}</span>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script>
import {
  Document,
  Connection,
  Right,
  User,
  Paperclip,
  Download,
  EditPen,
  QuestionFilled,
  ArrowDown,
  List,
  Message,
  Clock,
  Bell,
  AlarmClock,
  Link
} from '@element-plus/icons-vue'
import LogicQuestionItem from './LogicQuestionItem.vue'
import { normalizeProfileUrl } from '../utils/deployment'

export default {
  name: 'NotificationDetail',
  components: {
    Document,
    Connection,
    Right,
    User,
    Paperclip,
    Download,
    EditPen,
    QuestionFilled,
    ArrowDown,
    List,
    Message,
    Clock,
    Bell,
    AlarmClock,
    Link,
    LogicQuestionItem
  },
  props: {
    notification: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      expandedQuestions: []
    }
  },
  computed: {
    attachmentUrls() {
      if (this.notification.attachmentUrls) {
        try {
          return JSON.parse(this.notification.attachmentUrls).map(item => {
            if (typeof item === 'string') {
              return normalizeProfileUrl(item)
            }

            return {
              ...item,
              url: normalizeProfileUrl(item.url)
            }
          })
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

    getAttachmentName(item, index) {
      if (!item) return `附件 ${index + 1}`
      
      let urlStr = ''
      let providedName = ''
      
      if (typeof item === 'string') {
        urlStr = item
        providedName = ''
      } else {
        urlStr = item.url || ''
        providedName = item.name || ''
      }
      
      // 如果沒有名字，我們強制從 URL 提取
      if (!providedName || /^附件\s*\d+$/.test(providedName.trim())) {
        if (urlStr) {
          const parsed = urlStr.substring(urlStr.lastIndexOf('/') + 1)
          if (parsed) {
            return decodeURIComponent(parsed)
          }
        }
      }
      
      return providedName || `附件 ${index + 1}`
    },

    getAttachmentUrl(item) {
      return typeof item === 'string' ? normalizeProfileUrl(item) : normalizeProfileUrl(item.url)
    },

    getRootQuestions(parsed) {
      if (!parsed || !parsed.questions) return []
      const jumpedTargetIds = new Set()
      parsed.questions.forEach(sq => {
        if (sq.logicRuleList) {
          sq.logicRuleList.forEach(rule => {
            if (rule.jumpTarget !== 'next' && rule.jumpTarget !== 'end') {
              const tid = Number(rule.jumpTarget)
              if (!isNaN(tid)) {
                jumpedTargetIds.add(tid)
              }
            }
          })
        }
      })
      // 過濾出從未被當作 jumpTarget 的問題（即根節點）
      return parsed.questions.filter(sq => !jumpedTargetIds.has(sq.id))
    },

    parseQuestionContent(question) {
      if (!question.content) return null
      try {
        let parsed = question.content
        if (typeof parsed === 'string') {
          parsed = JSON.parse(parsed)
        }
        // Handle double serialization just in case
        if (typeof parsed === 'string') {
          parsed = JSON.parse(parsed)
        }

        if (parsed && typeof parsed === 'object') {
          // It could be directly the questionnaire object or wrapped in { questionnaire: ... }
          const title = parsed.questionnaire ? parsed.questionnaire.title : (parsed.title || '')
          const description = parsed.questionnaire ? parsed.questionnaire.description : (parsed.description || '')
          const questionsArray = parsed.questions || (parsed.questionnaire ? parsed.questionnaire.questions : []) || []
          
          if (questionsArray.length > 0) {
            return {
              title: title,
              description: description,
              questions: questionsArray.map(sq => ({
                id: sq.id,
                title: sq.title || '',
                type: sq.type || '',
                required: sq.required || false,
                options: Array.isArray(sq.options) ? sq.options : [],
                uploadNote: sq.uploadNote || '',
                logicRuleList: Array.isArray(sq.logicRuleList) ? sq.logicRuleList : [],
                description: sq.description || '',
                minOptions: sq.minOptions,
                maxOptions: sq.maxOptions,
                fillBlanks: Array.isArray(sq.fillBlanks) ? sq.fillBlanks : [],
                content: sq.content || ''
              }))
            }
          }
        }
        return null
      } catch (e) {
        console.error('Error parsing question content JSON:', e)
        return null
      }
    },

    getSubQuestionLogicForOption(subQ, index) {
      if (!subQ.logicRuleList || subQ.logicRuleList.length === 0) return null
      return subQ.logicRuleList.find(rule => rule.optionIndex === index)
    },

    formatJumpTarget(target, questionsArray) {
      if (target === 'next') return '跳轉至下一題'
      if (target === 'end') return '跳轉至結束作答'
      
      const targetId = Number(target)
      if (questionsArray && Array.isArray(questionsArray)) {
        const idx = questionsArray.findIndex(q => q.id === targetId)
        if (idx > -1) {
          return `跳轉至第 ${idx + 1} 題`
        }
      }
      return `跳轉至第 ${targetId} 題`
    },

    formatOptionsLimit(min, max) {
      const hasMin = min !== undefined && min !== null && min !== ''
      const hasMax = max !== undefined && max !== null && max !== ''
      
      if (hasMin && hasMax && min === max) return `${min}`
      if (hasMin && hasMax) return `${min} ~ ${max}`
      if (hasMin) return `至少 ${min}`
      if (hasMax) return `最多 ${max}`
      return ''
    },

    formatFillBlankContent(content) {
      if (!content) return ''
      // 將 {{something}} 替換為可視化的橫線填空
      return content.replace(/\{\{.*?\}\}/g, ' ________ ')
    },

    getSubQuestionTypeText(type) {
      const typeMap = {
        '1': '單選',
        '2': '多選',
        '3': '填空',
        '4': '附件上傳',
        '5': '邏輯表單'
      }
      return typeMap[String(type)] || '未知'
    },



    getReceiveTypeText(type) {
      const typeMap = {
        '1': '班級群組',
        '2': '個別學生/家長'
      }
      return typeMap[type] || '未知'
    },

    getCcTypeText(type) {
      const typeMap = {
        '1': '個別教職工',
        '2': '教職工群組'
      }
      return typeMap[type] || '未知'
    },

    getQuestionIndex(question) {
      const index = this.notification.questions.findIndex(q => q.questionId === question.questionId)
      return index + 1
    },

    getQuestionTypeText(type) {
      const typeMap = {
        '1': '單選',
        '2': '多選',
        '3': '填空',
        '4': '附件上傳',
        '5': '邏輯表單'
      }
      return typeMap[type] || '未知'
    },

    isQuestionExpanded(questionId) {
      return this.expandedQuestions.includes(questionId)
    },

    toggleQuestion(questionId) {
      const idx = this.expandedQuestions.indexOf(questionId)
      if (idx > -1) {
        this.expandedQuestions.splice(idx, 1)
      } else {
        this.expandedQuestions.push(questionId)
      }
    },

    getQuestionOptions(question) {
      if (!question.options) return []
      try {
        const opts = JSON.parse(question.options)
        return Array.isArray(opts) ? opts : []
      } catch (e) {
        return []
      }
    },

    getQuestionFillBlanks(question) {
      if (!question.fillBlanks) return []
      try {
        const blanks = JSON.parse(question.fillBlanks)
        return Array.isArray(blanks) ? blanks : []
      } catch (e) {
        return []
      }
    },

    getLogicRules(question) {
      if (!question.logicRules) return []
      try {
        const rules = JSON.parse(question.logicRules)
        if (!Array.isArray(rules)) return []
        const options = this.getQuestionOptions(question)
        return rules.map(rule => {
          const optIndex = rule.optionIndex !== undefined ? rule.optionIndex : 0
          return {
            ...rule,
            optionText: options[optIndex] || null,
            targetQuestionIndex: this.getTargetQuestionIndex(rule.targetQuestionId)
          }
        })
      } catch (e) {
        return []
      }
    },

    getTargetQuestionIndex(targetId) {
      if (!targetId || !this.notification.questions) return '?'
      const idx = this.notification.questions.findIndex(q => q.questionId === targetId)
      return idx > -1 ? idx + 1 : '?'
    },

    getReceiverGroupedData(receiver) {
      if (!receiver.receiveData) {
        return [];
      }
      
      try {
        const dataArr = JSON.parse(receiver.receiveData);
        const grouped = {};
        
        dataArr.forEach(group => {
          if (group.receive_names && Array.isArray(group.receive_names) && group.type) {
            const type = group.type;
            const sourceText = type === 1 ? 'WeCom家校通訊錄：' : '自定義家校通訊錄：';
            
            if (!grouped[type]) {
              grouped[type] = {
                source: sourceText,
                names: []
              };
            }
            grouped[type].names.push(...group.receive_names);
          }
        });
        
        return Object.values(grouped);
      } catch (e) {
        return [{ source: '解析錯誤', names: [] }];
      }
    },

    getCcGroupedData(cc) {
      if (!cc.ccData) {
        return [];
      }
      
      try {
        const dataArr = JSON.parse(cc.ccData);
        const grouped = {};
        
        dataArr.forEach(group => {
          if (group.cc_names && Array.isArray(group.cc_names) && group.type) {
            const type = group.type;
            const sourceText = type === 1 ? 'WeCom老師通訊錄：' : '自定義老師通訊錄：';
            
            if (!grouped[type]) {
              grouped[type] = {
                source: sourceText,
                names: []
              };
            }
            grouped[type].names.push(...group.cc_names);
          }
        });
        
        return Object.values(grouped);
      } catch (e) {
        return [{ source: '解析錯誤', names: [] }];
      }
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
  padding: 24px 28px;
  background: linear-gradient(135deg, #2563eb 0%, #1e40af 100%);
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

/* ===== 外部跳轉連結 (正文底部) ===== */
.content-jump-link {
  margin-top: 16px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 10px;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.jump-link-header {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #1e40af;
  font-weight: 600;
  font-size: 14px;
}

.actual-link {
  color: #2563eb;
  font-size: 14px;
  word-break: break-all;
  line-height: 1.5;
  text-decoration: underline;
  text-underline-offset: 3px;
  transition: all 0.2s ease;
}

.actual-link:hover {
  color: #1d4ed8;
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
  background: #dbeafe;
  color: #2563eb;
}

.attachment-icon {
  background: #dbeafe;
  color: #1d4ed8;
}

.question-icon {
  background: #dbeafe;
  color: #1e40af;
}

.receiver-icon {
  background: #dbeafe;
  color: #2563eb;
}

.cc-icon {
  background: #dbeafe;
  color: #1d4ed8;
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
  background: #dbeafe;
  color: #2563eb;
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
  white-space: pre-wrap;
  word-break: break-word;
}

/* ===== 芯片/标签列表 ===== */
.chip-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  cursor: default;
  transition: all 0.2s ease;
  line-height: 1.4;
}

.chip:hover {
  transform: translateY(-1px);
}

.attachment-chip {
  background: #eff6ff;
  color: #1e40af;
  border: 1px solid #bfdbfe;
}

.attachment-link {
  text-decoration: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.attachment-chip:hover {
  background: #dbeafe;
  box-shadow: 0 3px 10px rgba(37, 99, 235, 0.1);
}

.receiver-chip {
  background: #eff6ff;
  color: #1e40af;
  border: 1px solid #bfdbfe;
}

.receiver-chip:hover {
  background: #dbeafe;
  box-shadow: 0 3px 10px rgba(37, 99, 235, 0.1);
}

.cc-chip {
  background: #eff6ff;
  color: #1e40af;
  border: 1px solid #bfdbfe;
}

.cc-chip:hover {
  background: #dbeafe;
}

/* ===== 接收对象分组卡片 ===== */
.receiver-groups {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.receiver-group-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  overflow: hidden;
  transition: all 0.2s ease;
}

.receiver-group-card:hover {
  border-color: #93c5fd;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border-bottom: 1px solid #e2e8f0;
  color: #1e40af;
  font-weight: 600;
  font-size: 14px;
}

.group-header .el-icon {
  color: #2563eb;
  flex-shrink: 0;
}

.group-content {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.source-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.5;
  transition: all 0.2s ease;
}

.source-chip:hover {
  background: #eff6ff;
  border-color: #bfdbfe;
}

.source-label {
  color: #64748b;
  font-weight: 500;
  white-space: nowrap;
  min-width: 120px;
}

.source-names {
  color: #334155;
  font-weight: 500;
  flex: 1;
}

/* ===== 抄送对象分组卡片 ===== */
.cc-groups {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cc-group-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  overflow: hidden;
  transition: all 0.2s ease;
}

.cc-group-card:hover {
  border-color: #93c5fd;
}

/* ===== 問題卡片网格 ===== */
.questions-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.question-card {
  background: #fafbfc;
  border: 1px solid #f0f0f4;
  border-radius: 10px;
  transition: all 0.25s ease;
  overflow: hidden;
}

.question-card:hover {
  border-color: #d0d5dd;
}

.question-card.is-expanded {
  border-color: #93c5fd;
  box-shadow: 0 2px 12px rgba(37, 99, 235, 0.08);
}

/* 問題頭部 */
.question-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  cursor: pointer;
  transition: background-color 0.2s ease;
  user-select: none;
}

.question-header:hover {
  background: #f0f4f8;
}

.question-index {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: linear-gradient(135deg, #2563eb, #1e40af);
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
  gap: 8px;
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
  border-color: #bfdbfe !important;
  color: #2563eb !important;
  background: #eff6ff !important;
}

.required-tag {
  flex-shrink: 0;
  font-size: 11px;
}

.expand-arrow {
  color: #9ca3af;
  font-size: 14px;
  flex-shrink: 0;
  transition: transform 0.3s ease;
}

.expand-arrow.is-rotated {
  transform: rotate(180deg);
  color: #2563eb;
}

/* 展開過渡動畫 */
.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s ease;
  max-height: 600px;
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  max-height: 0;
  opacity: 0;
}

/* 展開內容區 */
.question-detail {
  padding: 0 18px 16px;
  border-top: 1px solid #eef0f4;
  margin-top: 0;
}

.detail-block {
  margin-top: 14px;
}

.detail-block-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 8px;
}

.detail-block-label .el-icon {
  color: #2563eb;
}

/* 選項列表 */
.options-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: #ffffff;
  border: 1px solid #e8ecf1;
  border-radius: 8px;
  font-size: 13px;
  transition: all 0.15s ease;
}

.option-item:hover {
  background: #f8fafc;
  border-color: #bfdbfe;
}

.option-letter {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: #eff6ff;
  color: #2563eb;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.option-text {
  color: #374151;
  font-weight: 400;
  flex: 1;
}

/* 題目內容 */
.detail-content {
  padding: 12px 14px;
  background: #ffffff;
  border: 1px solid #e8ecf1;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.7;
  color: #374151;
}

/* 邏輯規則 */
.logic-block {
  margin-top: 14px;
}

.logic-rules-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.logic-rule-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 8px;
  font-size: 13px;
}

.logic-condition,
.logic-action {
  display: flex;
  align-items: center;
  gap: 4px;
}

.logic-keyword {
  font-weight: 600;
  color: #b45309;
  font-size: 12px;
}

.logic-text {
  color: #92400e;
  font-weight: 500;
}

.logic-arrow-icon {
  color: #d97706;
  flex-shrink: 0;
}

/* 無內容 */
.empty-detail {
  padding: 16px 0 4px;
  text-align: center;
  color: #d1d5db;
  font-size: 13px;
}

/* 子問卷 (邏輯表單) UI */
.sub-questionnaire {
  background: #ffffff;
  border: 1px solid #e8ecf1;
  border-radius: 10px;
  overflow: hidden;
}

.sub-q-header {
  padding: 16px;
  background: #f8fafc;
  border-bottom: 1px solid #e8ecf1;
}

.field-item {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
}

.field-item:last-child {
  margin-bottom: 0;
}

.field-label {
  color: #94a3b8;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
}

.sub-q-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #1e3a8a;
  flex: 1;
}

.sub-q-desc {
  margin: 0;
  font-size: 13px;
  color: #475569;
  line-height: 1.5;
  flex: 1;
}

.sub-questions-list {
  display: flex;
  flex-direction: column;
}

.sub-question-item {
  padding: 18px 16px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sub-question-item:last-child {
  border-bottom: none;
}

.sub-q-item-header {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}

.sub-q-fields {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-left: 4px;
}

.sub-q-item-desc {
  font-size: 13px;
  color: #475569;
  flex: 1;
}

.sub-q-index {
  padding: 2px 10px;
  background: #eff6ff;
  color: #2563eb;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

.sub-q-name {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
  line-height: 1.5;
  flex: 1;
}

.sub-q-limits {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}

.sub-q-type-tag {
  background: #f8fafc !important;
  color: #64748b !important;
  border-color: #e2e8f0 !important;
}

.sub-q-options {
  width: 100%;
  padding-left: 34px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sub-q-opt-wrapper {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.sub-q-opt {
  font-size: 13px;
  color: #475569;
  background: #f8fafc;
  padding: 6px 12px;
  border-radius: 6px;
  border: 1px solid #f1f5f9;
}

.sub-q-fill-text-container {
  padding-left: 34px;
  margin-top: 4px;
}

.sub-q-fill-text {
  font-size: 14px;
  color: #1e293b;
  line-height: 1.8;
  margin: 0;
  padding: 12px;
  background: #f8fafc;
  border-radius: 6px;
  border: 1px dashed #cbd5e1;
}

.sub-q-logic-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: #fffbeb;
  border: 1px solid #fef08a;
  color: #b45309;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
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
