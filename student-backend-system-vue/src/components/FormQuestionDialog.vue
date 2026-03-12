1<template>
  <div class="form-question-dialog-overlay" v-if="visible">
    <div class="form-question-dialog">
      <!-- 顶部工具栏 -->
      <div class="dialog-toolbar">
        <div class="toolbar-left">
          <h2 class="toolbar-title">
            <el-icon class="title-icon"><Edit /></el-icon>
            表單設計器
          </h2>
          <el-divider direction="vertical" />
          <span class="toolbar-subtitle">{{ isEdit ? '編輯模式' : '新增模式' }}</span>
        </div>
        <div class="toolbar-center">
          <el-button-group class="view-mode-group">
            <el-button 
              :type="viewMode === 'edit' ? 'primary' : ''" 
              @click="viewMode = 'edit'"
              size="small"
            >
              <el-icon><Edit /></el-icon>
              編輯
            </el-button>
            <el-button 
              :type="viewMode === 'preview' ? 'primary' : ''" 
              @click="viewMode = 'preview'"
              size="small"
            >
              <el-icon><View /></el-icon>
              預覽
            </el-button>
          </el-button-group>
        </div>
        <div class="toolbar-right">
          <el-button @click="handleClose" class="close-btn">
            <el-icon><Close /></el-icon>
            關閉
          </el-button>
          <el-button type="success" @click="handleSave" class="save-btn">
            <el-icon><Check /></el-icon>
            保存
          </el-button>
        </div>
      </div>

      <!-- 主体内容区 - 三栏布局 -->
      <div class="dialog-main">
        <!-- 左侧：题型选择器 -->
        <div class="left-panel">
          <div class="panel-header">
            <el-icon><Menu /></el-icon>
            <span>題型選擇</span>
          </div>
          <div class="panel-content">
            <!-- 题目类型 -->
            <div class="题型-section">
              <div class="section-title">題目類型</div>
              <div class="type-grid">
                <div 
                  class="type-btn"
                  @click="addQuestion('1')"
                >
                  <el-icon><CircleCheck /></el-icon>
                  <span>單選</span>
                </div>
                <div 
                  class="type-btn"
                  @click="addQuestion('2')"
                >
                  <el-icon><Checked /></el-icon>
                  <span>多選</span>
                </div>
                <div 
                  class="type-btn"
                  @click="addQuestion('10')"
                >
                  <el-icon><Notebook /></el-icon>
                  <span>多項填空</span>
                </div>
                <div 
                  class="type-btn"
                  @click="addQuestion('6')"
                >
                  <el-icon><Download /></el-icon>
                  <span>下拉</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 中间：问卷预览/编辑区 -->
        <div class="center-panel">
          <div class="questionnaire-preview">
            <!-- 问卷头部 -->
            <div class="questionnaire-header">
              <div class="header-row">
                <span class="header-label">標題:</span>
                <el-input
                  v-model="questionnaireData.title"
                  placeholder="请输入问卷标题"
                  class="questionnaire-title-input"
                  size="large"
                />
              </div>
              <div class="header-desc-row">
                <span class="header-label">表單描述:</span>
                <el-input
                  v-model="questionnaireData.description"
                  type="textarea"
                  :rows="3"
                  class="questionnaire-desc-input"
                />
              </div>
            </div>

            <!-- 题目列表 -->
            <div class="questionnaire-body">
              <div
                v-for="(question, index) in questionList"
                :key="question.id"
                class="question-card"
                :class="{ 'active': selectedQuestionId === question.id }"
                @click="selectQuestion(question.id)"
              >
                <div class="question-header">
                  <div class="question-number">
                    <span class="required-mark" v-if="question.required">*</span>
                    <span>{{ index + 1 }}</span>
                  </div>
                  <el-input
                    v-model="question.title"
                    placeholder="请输入题目内容"
                    class="question-title-input"
                    @click.stop
                  />
                  <div class="question-actions">
                    <el-button size="small" @click.stop="moveUp(index)">
                      <el-icon><ArrowUp /></el-icon>
                    </el-button>
                    <el-button size="small" @click.stop="moveDown(index)">
                      <el-icon><ArrowDown /></el-icon>
                    </el-button>
                    <el-button 
                      size="small" 
                      class="delete-btn"
                      @click.stop="deleteQuestion(index)"
                    >
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                </div>

                <!-- 题目内容预览 -->
                <div class="question-content">
                  <!-- 单选/多选 -->
                  <div v-if="['1', '2', '6', '7', '8'].includes(question.type)">
                    <div class="options-container">
                      <div 
                        v-for="(option, optIndex) in question.options" 
                        :key="optIndex"
                        class="option-row"
                      >
                        <div class="option-prefix">
                          <span class="option-label">選項</span>
                          <el-radio v-if="question.type === '1' || question.type === '7'" :label="optIndex" />
                          <el-checkbox v-else :label="optIndex" />
                        </div>
                        <el-input 
                          v-model="question.options[optIndex]" 
                          placeholder="请输入选项内容"
                          class="option-input"
                          @click.stop
                        />
                      </div>
                    </div>
                  </div>

                  <!-- 填空/文本 -->
                  <div v-if="['3', '9', '10'].includes(question.type)">
                    <el-input
                      v-model="question.placeholder"
                      placeholder="设置占位文字"
                      size="small"
                      class="placeholder-input"
                    />
                  </div>

                  <!-- 附件 -->
                  <div v-if="question.type === '4'">
                    <div class="attachment-placeholder">
                      <el-icon><Upload /></el-icon>
                      <span>点击上传附件 (最多 5 个)</span>
                    </div>
                  </div>

                  <!-- 分支 -->
                  <div v-if="question.type === '5'">
                    <div class="branch-preview">
                      <div v-for="(branch, bi) in question.branchOptions" :key="bi" class="branch-item">
                        <el-input v-model="branch.text" placeholder="选项文字" size="small" />
                        <el-tag size="small">{{ branch.action === 'continue' ? '继续' : '结束' }}</el-tag>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 空状态 -->
              <div v-if="questionList.length === 0" class="empty-state">
                <el-empty description="請從左側選擇題型添加題目" />
              </div>
            </div>

            <!-- 底部添加按钮 -->
            <div class="questionnaire-footer">
              <el-button class="add-question-tips" size="large" @click="scrollToLeftPanel">
                <el-icon><Plus /></el-icon>
                從左側添加題目
              </el-button>
            </div>
          </div>
        </div>

        <!-- 右侧：属性设置面板 -->
        <div class="right-panel">
          <!-- 顶部标签页 -->
          <div class="panel-tabs">
            <el-tabs v-model="activeTab" type="border-card">
              <el-tab-pane label="題目" name="question" />
              <el-tab-pane label="選項" name="options" />
            </el-tabs>
          </div>
          
          <div class="panel-content">
            <div v-if="selectedQuestion" class="settings-form">
              <!-- 题目设置 -->
              <div v-show="activeTab === 'question'">
                <div class="settings-section">
                  <div class="section-header">題目</div>
                  <el-select
                    v-model="selectedQuestion.type"
                    placeholder="选择题型"
                    class="type-select"
                    disabled
                  >
                    <el-option label="单选" value="1" />
                    <el-option label="多选" value="2" />
                    <el-option label="下拉" value="6" />
                    <el-option label="图片单选" value="7" />
                    <el-option label="图片多选" value="8" />
                    <el-option label="单行文本" value="9" />
                    <el-option label="多行文本" value="3" />
                    <el-option label="多项填空" value="10" />
                    <el-option label="附件" value="4" />
                    <el-option label="分支" value="5" />
                  </el-select>
                </div>

                <div class="settings-section">
                  <div class="section-header">基礎設置</div>
                  <el-form label-position="top" size="default">
                    <el-form-item label="題目標題">
                      <el-input
                        v-model="selectedQuestion.title"
                        type="textarea"
                        :rows="2"
                        placeholder="请输入题目内容"
                      />
                    </el-form-item>

                    <el-form-item label="題目說明">
                      <el-input
                        v-model="selectedQuestion.description"
                        placeholder="请输入题目说明/提示"
                      />
                    </el-form-item>

                    <el-form-item label="別名">
                      <el-input
                        v-model="selectedQuestion.alias"
                        placeholder="请输入别名"
                      />
                    </el-form-item>

                    <el-form-item label="必答設置">
                      <el-checkbox v-model="selectedQuestion.required">必答題</el-checkbox>
                    </el-form-item>

                    <el-form-item label="敏感信息收集題">
                      <el-checkbox v-model="selectedQuestion.sensitive">敏感信息收集題</el-checkbox>
                      <div class="form-tip">如身份证号、手机号等敏感信息</div>
                    </el-form-item>
                  </el-form>
                </div>

                <div class="settings-section">
                  <div class="section-header">顯示設置</div>
                  <el-form label-position="top" size="default">
                    <el-form-item label="每行顯示">
                      <el-select v-model="selectedQuestion.perLine" placeholder="选择每行显示数量">
                        <el-option label="1" value="1" />
                        <el-option label="2" value="2" />
                        <el-option label="3" value="3" />
                        <el-option label="4" value="4" />
                        <el-option label="5" value="5" />
                      </el-select>
                    </el-form-item>

                    <el-form-item label="選項順序隨機">
                      <el-switch v-model="selectedQuestion.randomOrder" />
                    </el-form-item>
                  </el-form>
                </div>

                <div class="settings-section">
                  <div class="section-header">邏輯</div>
                  <el-form label-position="top" size="default">
                    <el-form-item label="邏輯設置">
                      <el-button class="logic-btn" size="small">
                        <el-icon><Connection /></el-icon>
                        設置顯示/跳轉邏輯
                      </el-button>
                    </el-form-item>

                    <el-form-item label="選項引用">
                      <el-switch v-model="selectedQuestion.optionQuote" />
                      <div class="form-tip">引用其他题目的选项</div>
                    </el-form-item>
                  </el-form>
                </div>
              </div>

              <!-- 选项设置 -->
              <div v-show="activeTab === 'options'">
                <div class="settings-section">
                  <div class="section-header">選項設置</div>
                  <el-alert
                    title="点击选项可设置【添加填空】,【选项别名】"
                    type="info"
                    :closable="false"
                    show-icon
                    class="mb-3"
                  >
                    <template #default>
                      <el-link type="primary" @click="startOptionSettings">开始设置</el-link>
                    </template>
                  </el-alert>

                  <div v-if="['1', '2', '6', '7', '8'].includes(selectedQuestion.type)" class="options-editor">
                    <div 
                      v-for="(option, index) in selectedQuestion.options" 
                      :key="index"
                      class="option-editor-item"
                    >
                      <span class="option-label">{{ getOptionLabel(index) }}</span>
                      <el-input
                        v-model="selectedQuestion.options[index]"
                        placeholder="选项内容"
                        size="small"
                      />
                      <el-button
                        size="small"
                        type="danger"
                        @click="removeOption(index)"
                        :disabled="selectedQuestion.options.length <= 2"
                        circle
                      >
                        <el-icon><Delete /></el-icon>
                      </el-button>
                    </div>
                    <el-button 
                      type="primary" 
                      @click="addOption" 
                      size="small" 
                      plain
                      class="add-option-btn"
                    >
                      <el-icon><Plus /></el-icon>
                      添加選項
                    </el-button>
                  </div>
                </div>

                <div class="settings-section">
                  <div class="section-header">選項配額設置</div>
                  <el-button class="quota-btn" size="small">
                    <el-icon><Grid /></el-icon>
                    開啟配額
                  </el-button>
                  <el-link type="primary" class="settings-link">设置</el-link>
                </div>

                <div v-if="['7', '8'].includes(selectedQuestion.type)" class="settings-section">
                  <div class="section-header">圖片</div>
                  <el-form label-position="top" size="default">
                    <el-form-item label="默認圖片寬度">
                      <el-input v-model="selectedQuestion.imageWidth" placeholder="请输入">
                        <template #append>像素</template>
                      </el-input>
                    </el-form-item>
                    <el-button class="apply-image-btn" size="small">
                      应用到本题所有图片
                    </el-button>
                  </el-form>
                </div>
              </div>
            </div>
            <div v-else class="no-selection">
              <el-empty description="請選擇題目進行設置" :image-size="80" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { 
  Edit, Close, Check, Menu, CircleCheck, Checked, EditPen, 
  Paperclip, Share, Delete, Plus, Bell, Upload, View, 
  Download, Picture, PictureFilled, Document, Notebook, Grid, 
  TrendCharts, Coin, Star, StarFilled, Sort, Connection, 
  Calendar, Location, ScaleToOriginal, ChatLineRound, Setting,
  ArrowUp, ArrowDown
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'FormQuestionDialog',
  components: {
    Edit, Close, Check, Menu, CircleCheck, Checked, EditPen, 
    Paperclip, Share, Delete, Plus, Bell, Upload, View,
    Download, Picture, PictureFilled, Document, Notebook, Grid,
    TrendCharts, Coin, Star, StarFilled, Sort, Connection,
    Calendar, Location, ScaleToOriginal, ChatLineRound, Setting,
    ArrowUp, ArrowDown
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    question: {
      type: Object,
      default: null
    }
  },
  emits: ['update:visible', 'save'],
  data() {
    return {
      viewMode: 'edit', // edit | preview
      activeTab: 'question', // question | options
      selectedQuestionId: null,
      questionList: [],
      questionnaireData: {
        title: '',
        description: ''
      },
      nextId: 1
    }
  },
  computed: {
    isEdit() {
      return !!this.question
    },
    selectedQuestion() {
      return this.questionList.find(q => q.id === this.selectedQuestionId) || null
    }
  },
  watch: {
    visible(newVal) {
      if (newVal) {
        this.initForm()
      }
    }
  },
  methods: {
    getOptionLabel(index) {
      return String.fromCharCode(65 + index)
    },

    initForm() {
      if (this.question) {
        // 如果是编辑模式，加载现有数据
        this.questionnaireData = {
          title: '問卷調查',
          description: ''
        }
        this.questionList = [{
          ...this.question,
          id: this.question.id || Date.now(),
          description: this.question.description || '',
          placeholder: this.question.placeholder || '',
          defaultValue: this.question.defaultValue || '',
          validation: this.question.validation || [],
          randomOrder: this.question.randomOrder || false
        }]
        if (this.questionList.length > 0) {
          this.selectedQuestionId = this.questionList[0].id
        }
      } else {
        // 新增模式，初始化空数据
        this.questionList = []
        this.questionnaireData = {
          title: '',
          description: ''
        }
        this.selectedQuestionId = null
        this.nextId = 1
      }
    },

    addQuestion(type) {
      const question = {
        id: this.nextId++,
        type: type,
        title: '',
        description: '',
        required: false,
        options: this.getDefaultOptions(type),
        placeholder: '',
        defaultValue: '',
        validation: [],
        minLength: 0,
        maxLength: 200,
        randomOrder: false,
        branchOptions: type === '5' ? [
          { text: '', action: 'continue', nextTitle: '' },
          { text: '', action: 'end', nextTitle: '' }
        ] : null
      }
      
      this.questionList.push(question)
      this.selectedQuestionId = question.id
      
      ElMessage.success('已添加題目')
    },

    getDefaultOptions(type) {
      // 需要选项的题型
      if (['1', '2', '6', '7', '8'].includes(type)) {
        return ['', '']
      }
      return null
    },

    selectQuestion(id) {
      this.selectedQuestionId = id
    },

    moveUp(index) {
      if (index > 0) {
        const temp = this.questionList[index]
        this.questionList[index] = this.questionList[index - 1]
        this.questionList[index - 1] = temp
      }
    },

    moveDown(index) {
      if (index < this.questionList.length - 1) {
        const temp = this.questionList[index]
        this.questionList[index] = this.questionList[index + 1]
        this.questionList[index + 1] = temp
      }
    },

    deleteQuestion(index) {
      const deletedId = this.questionList[index].id
      this.questionList.splice(index, 1)
      if (this.selectedQuestionId === deletedId) {
        this.selectedQuestionId = null
      }
      ElMessage.success('刪除成功')
    },

    deleteSelectedQuestion() {
      if (!this.selectedQuestionId) return
      const index = this.questionList.findIndex(q => q.id === this.selectedQuestionId)
      if (index > -1) {
        this.deleteQuestion(index)
      }
    },

    addOption() {
      if (this.selectedQuestion) {
        this.selectedQuestion.options.push('')
      }
    },

    removeOption(index) {
      if (this.selectedQuestion && this.selectedQuestion.options.length > 2) {
        this.selectedQuestion.options.splice(index, 1)
      }
    },

    startOptionSettings() {
      ElMessage.info('點擊選項後可設置添加填空和選項別名')
    },

    scrollToLeftPanel() {
      // 提示用户从左侧添加题目
      ElMessage.info('請從左側面板選擇題型添加題目')
    },

    handleClose() {
      this.$emit('update:visible', false)
    },

    handleSave() {
      if (this.questionList.length === 0) {
        ElMessage.warning('請至少添加一道題目')
        return
      }

      // 验证必填项
      for (let i = 0; i < this.questionList.length; i++) {
        const q = this.questionList[i]
        if (!q.title?.trim()) {
          ElMessage.warning(`第 ${i + 1} 題的題目內容不能為空`)
          this.selectedQuestionId = q.id
          return
        }
        
        // 验证选项
        if (['1', '2', '6', '7', '8'].includes(q.type)) {
          const validOptions = q.options.filter(opt => opt.trim())
          if (validOptions.length < 2) {
            ElMessage.warning(`第 ${i + 1} 題至少需要 2 個選項`)
            this.selectedQuestionId = q.id
            return
          }
        }
      }

      // 保存所有题目数据
      const savedData = {
        questionnaire: this.questionnaireData,
        questions: this.questionList
      }

      this.$emit('save', savedData)
      this.$emit('update:visible', false)
      ElMessage.success('保存成功')
    }
  }
}
</script>

<style scoped>
.form-question-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.3s ease;
}

.form-question-dialog {
  width: 98vw;
  height: 96vh;
  background: #f5f7fa;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: slideIn 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideIn {
  from {
    transform: translateY(-30px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

/* 顶部工具栏 */
.dialog-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: white;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-icon {
  font-size: 22px;
  color: #409EFF;
}

.toolbar-subtitle {
  font-size: 13px;
  color: #909399;
  padding: 4px 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.toolbar-center {
  display: flex;
  justify-content: center;
}

.view-mode-group {
  display: flex;
}

.view-mode-group .el-button {
  padding: 8px 16px;
}

.toolbar-right {
  display: flex;
  gap: 12px;
}

.close-btn,
.save-btn {
  height: 36px;
  padding: 0 20px;
  border-radius: 6px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.close-btn {
  background: #f4f4f5;
  color: #909399;
  border: 1px solid #e9e9eb;
}

.close-btn:hover {
  background: #e9e9eb;
  border-color: #dcdfe6;
  color: #606266;
}

.save-btn {
  background: linear-gradient(135deg, #67c23a 0%, #529b33 100%);
  color: white;
  border: none;
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.3);
}

.save-btn:hover {
  box-shadow: 0 6px 20px rgba(103, 194, 58, 0.4);
  transform: translateY(-2px);
}

/* 主体内容区 - 三栏布局 */
.dialog-main {
  flex: 1;
  display: flex;
  gap: 12px;
  padding: 12px;
  overflow: hidden;
  background: #f5f7fa;
}

/* 左侧面板 */
.left-panel {
  width: 320px;
  flex-shrink: 0;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.left-panel .panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  padding: 16px;
  background: #fafafa;
  border-bottom: 1px solid #ebeef5;
}

.left-panel .panel-header .el-icon {
  font-size: 20px;
  color: #409EFF;
}

.left-panel .panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.left-panel .panel-content::-webkit-scrollbar {
  width: 6px;
}

.left-panel .panel-content::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}

/* 题型区块 */
.题型-section {
  margin-bottom: 20px;
  padding: 14px;
  background: linear-gradient(135deg, #fafafa 0%, #ffffff 100%);
  border-radius: 10px;
  border: 1px solid #f0f0f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.题型-section:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.section-title {
  font-size: 13px;
  font-weight: 700;
  color: #606266;
  margin-bottom: 14px;
  padding: 6px 10px;
  background: linear-gradient(135deg, #e3f2fd 0%, #f3e5f5 100%);
  border-radius: 6px;
  letter-spacing: 1px;
  text-transform: uppercase;
  display: flex;
  align-items: center;
  gap: 6px;
}

.section-title::before {
  content: '';
  width: 4px;
  height: 14px;
  background: linear-gradient(180deg, #409EFF 0%, #67c23a 100%);
  border-radius: 2px;
}

.type-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.type-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 14px 10px;
  background: linear-gradient(135deg, #ffffff 0%, #fafafa 100%);
  border: 2px solid #e8eaed;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  color: #606266;
  position: relative;
  overflow: hidden;
}

.type-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.05) 0%, rgba(103, 194, 58, 0.05) 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.type-btn:hover::before {
  opacity: 1;
}

.type-btn:hover {
  border-color: #409EFF;
  color: #409EFF;
  transform: translateY(-3px) scale(1.02);
  box-shadow: 0 8px 20px rgba(64, 158, 255, 0.2);
}

.type-btn .el-icon {
  font-size: 22px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  z-index: 1;
}

.type-btn:hover .el-icon {
  transform: scale(1.15) rotate(5deg);
  filter: drop-shadow(0 4px 8px rgba(64, 158, 255, 0.3));
}

.type-btn span {
  font-size: 12px;
  font-weight: 600;
  text-align: center;
  position: relative;
  z-index: 1;
  letter-spacing: 0.5px;
}

/* 中间面板 */
.center-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.questionnaire-preview {
  flex: 1;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.questionnaire-header {
  padding: 24px 32px;
  border-bottom: 1px solid #e4e7ed;
  background: white;
}

.header-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.header-desc-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.header-label {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  min-width: 100px;
  flex-shrink: 0;
  padding-top: 8px;
}

.questionnaire-title-input {
  flex: 1;
}

.questionnaire-title-input :deep(.el-input__inner) {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  border: none;
  background: white;
  padding: 10px 14px;
}

.questionnaire-title-input :deep(.el-input) {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.questionnaire-title-input :deep(.el-input):hover {
  border-color: #c0c4cc;
}

.questionnaire-title-input :deep(.el-input):focus-within {
  border-color: #409EFF;
}

.questionnaire-desc-input {
  flex: 1;
}

.questionnaire-desc-input :deep(.el-textarea__inner) {
  font-size: 14px;
  color: #606266;
  border: none;
  background: white;
  padding: 10px 14px;
}

.questionnaire-desc-input :deep(.el-textarea) {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.questionnaire-desc-input :deep(.el-textarea):hover {
  border-color: #c0c4cc;
}

.questionnaire-desc-input :deep(.el-textarea):focus-within {
  border-color: #409EFF;
}

.questionnaire-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px 32px;
  background: #fafafa;
}

.questionnaire-body::-webkit-scrollbar {
  width: 8px;
}

.questionnaire-body::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 4px;
}

/* 题目卡片 */
.question-card {
  background: white;
  border: 2px solid #e4e7ed;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.question-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(180deg, #409EFF 0%, #67c23a 100%);
  transform: scaleY(0);
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.question-card:hover::before {
  transform: scaleY(1);
}

.question-card:hover {
  border-color: #409EFF;
  box-shadow: 0 8px 24px rgba(64, 158, 255, 0.15);
  transform: translateY(-4px);
}

.question-card.active {
  border-color: #409EFF;
  box-shadow: 0 8px 32px rgba(64, 158, 255, 0.25);
  background: linear-gradient(135deg, #ecf5ff 0%, #f5f7fa 100%);
  transform: translateY(-4px);
}

.question-card.active::before {
  transform: scaleY(1);
}

.question-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

.question-number {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 16px;
  font-weight: 700;
  color: #303133;
  min-width: 28px;
  background: linear-gradient(135deg, #409EFF 0%, #67c23a 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.required-mark {
  color: #f56c6c;
  font-weight: 700;
  font-size: 18px;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.8;
  }
}

.question-title-input {
  flex: 1;
}

.question-title-input :deep(.el-input__inner) {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  border: none;
  background: transparent;
  padding: 8px 12px;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.question-title-input:hover :deep(.el-input__inner) {
  background: #f5f7fa;
}

.question-card.active .question-title-input:hover :deep(.el-input__inner) {
  background: white;
}

.question-actions {
  display: flex;
  gap: 6px;
  opacity: 0;
  transition: opacity 0.3s ease;
  margin-left: auto;
}

.question-card:hover .question-actions {
  opacity: 1;
}

.question-actions .el-button {
  padding: 6px 10px;
  border-radius: 6px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.question-actions .el-button:hover {
  transform: scale(1.1);
}

/* 删除按钮样式 */
.question-actions .el-button.delete-btn {
  background: #fef0f0 !important;
  border: 1px solid #fde2e2 !important;
  color: #f56c6c !important;
}

.question-actions .el-button.delete-btn:hover {
  background: #f56c6c !important;
  border-color: #f56c6c !important;
  color: white !important;
}

.question-actions .el-button.delete-btn .el-icon {
  color: #f56c6c !important;
}

.question-actions .el-button.delete-btn:hover .el-icon {
  color: white !important;
}

/* 题目内容 */
.question-content {
  padding-left: 40px;
}

.placeholder-input {
  max-width: 400px;
}

.attachment-placeholder {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  background: #fafafa;
  border: 2px dashed #dcdfe6;
  border-radius: 6px;
  color: #909399;
}

.branch-preview {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.branch-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.branch-item .el-input {
  flex: 1;
}

.questionnaire-footer {
  padding: 20px 32px;
  border-top: 1px solid #ebeef5;
  background: #fafafa;
  text-align: center;
}

.add-question-tips {
  border: 2px dashed #409EFF;
  background: #ecf5ff;
  color: #409EFF;
}

.add-question-tips:hover {
  background: #f5f7fa;
}

/* 空状态 */
.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

/* 右侧面板 */
.right-panel {
  width: 340px;
  flex-shrink: 0;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.right-panel .panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  padding: 16px;
  background: #fafafa;
  border-bottom: 1px solid #ebeef5;
}

.right-panel .panel-header .el-icon {
  font-size: 20px;
  color: #409EFF;
}

/* 面板标签页 */
.panel-tabs {
  padding: 0;
  background: white;
  border-bottom: 1px solid #e4e7ed;
}

.panel-tabs :deep(.el-tabs) {
  width: 100%;
}

.panel-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0;
  background: white;
}

.panel-tabs :deep(.el-tabs__item) {
  height: 50px;
  line-height: 50px;
  font-size: 14px;
  color: #606266;
  border: none;
  padding: 0 20px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.panel-tabs :deep(.el-tabs__item::after) {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%) scaleX(0);
  width: 80%;
  height: 3px;
  background: linear-gradient(90deg, #409EFF 0%, #67c23a 100%);
  border-radius: 3px 3px 0 0;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.panel-tabs :deep(.el-tabs__item.is-active) {
  color: #409EFF;
  font-weight: 600;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.05) 0%, rgba(103, 194, 58, 0.05) 100%);
}

.panel-tabs :deep(.el-tabs__item.is-active::after) {
  transform: translateX(-50%) scaleX(1);
}

.panel-tabs :deep(.el-tabs__item:hover) {
  color: #409EFF;
  background: rgba(64, 158, 255, 0.03);
}

.panel-tabs :deep(.el-tabs__item:hover::after) {
  transform: translateX(-50%) scaleX(0.5);
}

.panel-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: #e4e7ed;
  opacity: 0.5;
}

.panel-tabs :deep(.el-tabs__content) {
  padding: 0;
}

.right-panel .panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #fafafa;
}

.right-panel .panel-content::-webkit-scrollbar {
  width: 6px;
}

.right-panel .panel-content::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}

/* 设置区块 */
.settings-section {
  margin-bottom: 24px;
  padding: 18px;
  background: linear-gradient(135deg, #ffffff 0%, #fafafa 100%);
  border-radius: 10px;
  border: 1px solid #f0f0f0;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.settings-section:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.settings-section:last-child {
  border-bottom: none;
}

.section-header {
  font-size: 14px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 16px;
  padding: 10px 14px;
  background: linear-gradient(135deg, #e3f2fd 0%, #f3e5f5 100%);
  border-radius: 8px;
  border-left: 4px solid #409EFF;
  letter-spacing: 1px;
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.section-header:hover {
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.15);
  transform: translateX(4px);
}

.settings-form .el-form-item {
  margin-bottom: 16px;
}

.settings-form .el-form-item__label {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
  padding: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.settings-form .el-form-item__label::before {
  content: '';
  width: 3px;
  height: 14px;
  background: linear-gradient(180deg, #409EFF 0%, #67c23a 100%);
  border-radius: 2px;
}

.type-select {
  width: 100%;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
  line-height: 1.6;
  padding: 6px 10px;
  background: #f8f9fa;
  border-radius: 6px;
  border-left: 3px solid #e6a23c;
}

.mb-3 {
  margin-bottom: 16px;
}

.options-editor {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-editor-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.option-label {
  font-size: 13px;
  font-weight: 600;
  color: #909399;
  min-width: 24px;
  text-align: center;
}

.option-editor-item .el-input {
  flex: 1;
}

.add-option-btn {
  width: 100%;
  margin-top: 8px;
}

.logic-btn {
  width: 100%;
  border: 2px dashed #409EFF;
  background: linear-gradient(135deg, #ecf5ff 0%, #f5f7fa 100%);
  color: #409EFF;
  font-weight: 600;
  border-radius: 8px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.logic-btn:hover {
  background: linear-gradient(135deg, #f5f7fa 0%, #ffffff 100%);
  border-color: #67c23a;
  color: #67c23a;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.2);
}

.quota-btn {
  width: 100%;
  border: 2px dashed #409EFF;
  background: linear-gradient(135deg, #ecf5ff 0%, #f5f7fa 100%);
  color: #409EFF;
  font-weight: 600;
  border-radius: 8px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.quota-btn:hover {
  background: linear-gradient(135deg, #f5f7fa 0%, #ffffff 100%);
  border-color: #67c23a;
  color: #67c23a;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.2);
}

.settings-link {
  margin-left: 8px;
  font-weight: 600;
}

.apply-image-btn {
  width: 100%;
  margin-top: 8px;
  background: linear-gradient(135deg, #67c23a 0%, #529b33 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.apply-image-btn:hover {
  background: linear-gradient(135deg, #529b33 0%, #3d7a2a 100%);
  box-shadow: 0 6px 20px rgba(103, 194, 58, 0.4);
  transform: translateY(-2px);
}

/* 未选中状态 */
.no-selection {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

/* 响应式设计 */
@media (max-width: 1400px) {
  .left-panel {
    width: 280px;
  }
  
  .right-panel {
    width: 300px;
  }
}

@media (max-width: 1200px) {
  .dialog-main {
    flex-direction: column;
  }
  
  .left-panel,
  .right-panel {
    width: 100%;
    max-height: 200px;
  }
  
  .type-grid {
    grid-template-columns: repeat(auto-fit, minmax(100px, 1fr));
  }
}

@media (max-width: 768px) {
  .form-question-dialog {
    width: 100vw;
    height: 100vh;
    border-radius: 0;
  }
  
  .dialog-toolbar {
    padding: 12px 16px;
  }
  
  .dialog-main {
    padding: 8px;
  }
  
  .questionnaire-preview {
    border-radius: 4px;
  }
}
</style>
