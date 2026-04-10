<template>
  <div class="sub-question-item">
    <div class="sub-q-item-header">
      <span class="sub-q-index">問題 {{ indexStr }}</span>
      <el-tag size="small" effect="plain" round class="sub-q-type-tag">
        {{ getSubQuestionTypeText(subQ.type) }}
      </el-tag>
      <!-- 多選題選項數量限制 -->
      <span v-if="subQ.type === '2' && formatOptionsLimit(subQ.minOptions, subQ.maxOptions)" class="sub-q-limits">
        (可選 {{ formatOptionsLimit(subQ.minOptions, subQ.maxOptions) }} 項)
      </span>
      <el-tag v-if="subQ.required" size="small" type="danger" effect="light" round class="required-tag">
        必答
      </el-tag>
    </div>
    
    <div class="sub-q-fields">
      <div class="field-item">
        <span class="field-label">問題標題：</span>
        <span class="sub-q-name">{{ subQ.title }}</span>
      </div>
      <div class="field-item" v-if="subQ.description">
        <span class="field-label">問題描述：</span>
        <span class="sub-q-item-desc">{{ subQ.description }}</span>
      </div>
    </div>

    <!-- 子問題選項 -->
    <div v-if="subQ.options && subQ.options.length > 0" class="sub-q-options">
      <div v-for="(opt, oIdx) in subQ.options" :key="oIdx" class="sub-q-opt-row">
        <!-- 選項主體與徽章 -->
        <div class="sub-q-opt-main">
          <span class="sub-q-opt">
            {{ String.fromCharCode(65 + oIdx) }}. {{ opt }}
          </span>
          <div v-if="getSubQuestionLogicForOption(subQ, oIdx)" class="sub-q-logic-badge">
            <el-icon :size="12"><Connection /></el-icon>
            <span>{{ formatJumpTarget(getSubQuestionLogicForOption(subQ, oIdx).jumpTarget, indexStr, oIdx) }}</span>
          </div>
        </div>
        
        <!-- RECURSIVE NESTING: 跳轉目標的問題 -->
        <div v-if="hasValidJumpTarget(subQ, oIdx)" class="logic-nested-container">
          <LogicQuestionItem 
            :subQ="getJumpTargetQuestion(subQ, oIdx)" 
            :allQuestions="allQuestions" 
            :indexStr="indexStr + '.' + (oIdx + 1)" 
            :visited="new Set([...visited, subQ.id])" 
          />
        </div>
      </div>
    </div>

    <!-- 子問題填空 -->
    <div v-if="subQ.type === '3' && subQ.content" class="sub-q-fill-text-container">
      <p class="sub-q-fill-text">{{ formatFillBlankContent(subQ.content) }}</p>
    </div>
    <div v-else-if="subQ.fillBlanks && subQ.fillBlanks.length > 0 && typeof subQ.fillBlanks[0] === 'string'" class="sub-q-options">
      <div v-for="(blank, bIdx) in subQ.fillBlanks" :key="bIdx" class="sub-q-opt-row">
        <div class="sub-q-opt-main">
          <span class="sub-q-opt">
            填空 {{ bIdx + 1 }}：{{ blank }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { Connection } from '@element-plus/icons-vue'

export default {
  name: 'LogicQuestionItem',
  components: {
    Connection
  },
  props: {
    subQ: {
      type: Object,
      required: true
    },
    allQuestions: {
      type: Array,
      required: true
    },
    indexStr: {
      type: String,
      required: true
    },
    visited: {
      type: Set,
      default: () => new Set()
    }
  },
  methods: {
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
      return content.replace(/\{\{.*?\}\}/g, ' ________ ')
    },

    getSubQuestionLogicForOption(subQ, index) {
      if (!subQ.logicRuleList || subQ.logicRuleList.length === 0) return null
      return subQ.logicRuleList.find(rule => rule.optionIndex === index)
    },

    formatJumpTarget(target, currentIndexStr, optIndex) {
      if (target === 'next') return '跳轉至下一題'
      if (target === 'end') return '跳轉至結束作答'
      
      const targetId = Number(target)
      if (isNaN(targetId)) return '跳轉失敗'
      
      return `跳轉至問題 ${currentIndexStr}.${optIndex + 1}`
    },

    hasValidJumpTarget(subQ, index) {
      const rule = this.getSubQuestionLogicForOption(subQ, index)
      if (!rule) return false
      if (rule.jumpTarget === 'next' || rule.jumpTarget === 'end') return false
      const targetId = Number(rule.jumpTarget)
      if (isNaN(targetId)) return false
      //防止循環依賴無限遞迴
      if (this.visited && this.visited.has(targetId)) return false
      
      const targetQ = this.allQuestions.find(q => q.id === targetId)
      return !!targetQ
    },

    getJumpTargetQuestion(subQ, index) {
      const rule = this.getSubQuestionLogicForOption(subQ, index)
      const targetId = Number(rule.jumpTarget)
      return this.allQuestions.find(q => q.id === targetId)
    }
  }
}
</script>

<style scoped>
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

.sub-question-item {
  padding: 18px 16px;
  border-radius: 6px;
  background-color: #ffffff;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 遞迴時去掉底部的虛線或給予特殊間距 */
.logic-nested-container > .sub-question-item {
  padding: 12px;
  border: 1px dashed #cbd5e1;
  background-color: #f8fafc;
  margin-top: 8px;
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
  padding-left: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sub-q-opt-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-start;
}

.sub-q-opt-main {
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

.logic-nested-container {
  width: 100%;
  padding-left: 16px;
  margin-bottom: 4px;
}

.sub-q-fill-text-container {
  padding-left: 10px;
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

.required-tag {
  margin-left: 4px;
}
</style>
