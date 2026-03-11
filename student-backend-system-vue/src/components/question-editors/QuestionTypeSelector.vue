<template>
  <div class="question-type-selector">
    <div class="selector-header">
      <div class="header-icon">
        <el-icon :size="32"><List /></el-icon>
      </div>
      <div class="header-text">
        <h3 class="title">選擇題目類型</h3>
        <p class="subtitle">請從下方選擇一種題目類型</p>
      </div>
    </div>

    <div class="type-grid">
      <!-- 單選 -->
      <div 
        class="type-card"
        :class="{ selected: selectedType === '1' }"
        @click="selectType('1')"
      >
        <div class="card-icon single">
          <el-icon :size="40"><CircleCheck /></el-icon>
        </div>
        <h4 class="card-title">單選題</h4>
        <p class="card-desc">只有一個正確答案</p>
        <div class="selected-indicator" v-if="selectedType === '1'">
          <el-icon :size="24"><Check /></el-icon>
        </div>
      </div>

      <!-- 多選 -->
      <div 
        class="type-card"
        :class="{ selected: selectedType === '2' }"
        @click="selectType('2')"
      >
        <div class="card-icon multiple">
          <el-icon :size="40"><Checked /></el-icon>
        </div>
        <h4 class="card-title">多選題</h4>
        <p class="card-desc">可有多個正確答案</p>
        <div class="selected-indicator" v-if="selectedType === '2'">
          <el-icon :size="24"><Check /></el-icon>
        </div>
      </div>

      <!-- 填空 -->
      <div 
        class="type-card"
        :class="{ selected: selectedType === '3' }"
        @click="selectType('3')"
      >
        <div class="card-icon fill">
          <el-icon :size="40"><Edit /></el-icon>
        </div>
        <h4 class="card-title">填空題</h4>
        <p class="card-desc">填寫文字或數字答案</p>
        <div class="selected-indicator" v-if="selectedType === '3'">
          <el-icon :size="24"><Check /></el-icon>
        </div>
      </div>

      <!-- 附加题 -->
      <div 
        class="type-card"
        :class="{ selected: selectedType === '4' }"
        @click="selectType('4')"
      >
        <div class="card-icon attachment">
          <el-icon :size="40"><Paperclip /></el-icon>
        </div>
        <h4 class="card-title">附加题</h4>
        <p class="card-desc">上传附件文件</p>
        <div class="selected-indicator" v-if="selectedType === '4'">
          <el-icon :size="24"><Check /></el-icon>
        </div>
      </div>
    </div>

    <div class="action-buttons">
      <el-button @click="$emit('next')" :disabled="!selectedType" class="next-btn">
        <span class="btn-text">下一步</span>
        <el-icon class="btn-icon"><ArrowRight /></el-icon>
      </el-button>
    </div>
  </div>
</template>

<script>
import { CircleCheck, Checked, Edit, Paperclip, Check, List, ArrowRight } from '@element-plus/icons-vue'

export default {
  name: 'QuestionTypeSelector',
  components: {
    CircleCheck,
    Checked,
    Edit,
    Paperclip,
    Check,
    List,
    ArrowRight
  },
  props: {
    selectedType: {
      type: String,
      required: true
    }
  },
  emits: ['update:selectedType', 'next'],
  methods: {
    selectType(type) {
      this.$emit('update:selectedType', type)
    }
  }
}
</script>

<style scoped>
.question-type-selector {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.selector-header {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border-radius: 16px;
  border: 2px solid #bfdbfe;
}

.header-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.header-text {
  flex: 1;
}

.title {
  font-size: 20px;
  font-weight: 700;
  color: #1e40af;
  margin: 0 0 4px 0;
}

.subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.type-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 20px;
}

.type-card {
  position: relative;
  padding: 28px 20px;
  background: linear-gradient(135deg, #ffffff 0%, #f9fafb 100%);
  border: 2px solid #e5e7eb;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.type-card:hover {
  border-color: #60a5fa;
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.15);
  transform: translateY(-4px);
}

.type-card.selected {
  border-color: #3b82f6;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  box-shadow: 0 6px 24px rgba(59, 130, 246, 0.25);
  transform: translateY(-4px);
}

.card-icon {
  width: 72px;
  height: 72px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.card-icon.single {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.card-icon.multiple {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

.card-icon.fill {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);
}

.card-icon.attachment {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
}

.type-card.selected .card-icon {
  transform: scale(1.1);
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
  margin: 0;
}

.type-card.selected .card-title {
  color: #1e40af;
}

.card-desc {
  font-size: 12px;
  color: #6b7280;
  margin: 0;
  line-height: 1.5;
}

.selected-indicator {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
  animation: checkBounce 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes checkBounce {
  0% {
    transform: scale(0);
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
  }
}

.action-buttons {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 2px solid #e5e7eb;
}

.next-btn {
  height: 46px;
  padding: 0 32px;
  font-size: 15px;
  font-weight: 700;
  color: #ffffff !important;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.35);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  gap: 8px;
}

.next-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  box-shadow: 0 6px 24px rgba(59, 130, 246, 0.5);
  transform: translateY(-2px);
}

.next-btn:active:not(:disabled) {
  transform: translateY(0) scale(0.98);
  box-shadow: 0 2px 10px rgba(59, 130, 246, 0.25);
}

.btn-icon {
  font-size: 18px;
  animation: slideRight 2s ease-in-out infinite;
}

@keyframes slideRight {
  0%, 100% {
    transform: translateX(0);
  }
  50% {
    transform: translateX(5px);
  }
}

/* 響應式設計 */
@media (max-width: 768px) {
  .type-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .selector-header {
    flex-direction: column;
    text-align: center;
  }
}
</style>
