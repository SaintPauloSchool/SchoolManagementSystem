1<template>
  <div class="form-question-dialog-overlay" v-if="visible">
    <div class="form-question-dialog">
      <!-- 顶部工具栏 -->
      <div class="dialog-toolbar">
        <div class="toolbar-left">
          <h2 class="toolbar-title">
            <el-icon class="title-icon"><Edit /></el-icon>
            編輯表單問題
          </h2>
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
            <div class="question-type-section">
              <div class="section-title">題目類型</div>
              <div class="type-grid">
                <div class="type-btn" @click="addQuestion('1')">
                  <el-icon><CircleCheck /></el-icon>
                  <span>單選</span>
                </div>
                <div class="type-btn" @click="addQuestion('2')">
                  <el-icon><Checked /></el-icon>
                  <span>多選</span>
                </div>
                <div class="type-btn" @click="addQuestion('10')">
                  <el-icon><Edit /></el-icon>
                  <span>填空</span>
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
                  placeholder="請輸入標題"
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
                    placeholder="請輸入題目內容"
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
                  <!-- 单选/多选/下拉 -->
                  <div v-if="hasOptionType(question.type)">
                    <div class="options-container">
                      <div 
                        v-for="(option, optIndex) in question.options" 
                        :key="optIndex"
                        class="option-row"
                      >
                        <div class="option-prefix">
                          <el-radio v-if="isSingleChoice(question.type)" :label="optIndex" />
                          <el-checkbox v-else :label="optIndex" />
                          <span class="option-label">{{ getOptionLabel(optIndex) }}</span>
                        </div>
                        <el-input 
                          v-model="question.options[optIndex]" 
                          placeholder="請輸入選項內容"
                          class="option-input"
                          @click.stop
                        />
                        <div class="option-actions">
                          <el-button 
                            size="small" 
                            type="primary"
                            @click.stop="addOptionAtIndex(question, optIndex + 1)"
                            circle
                          >
                            <el-icon><Plus /></el-icon>
                          </el-button>
                          <el-button 
                            size="small" 
                            type="danger"
                            @click.stop="removeOptionAtIndex(question, optIndex)"
                            :disabled="question.options.length <= 2"
                            circle
                          >
                            <el-icon><Delete /></el-icon>
                          </el-button>
                        </div>
                      </div>
                      <!-- 底部添加选项按钮 -->
                      <div class="add-option-row">
                        <el-button 
                          size="small" 
                          type="primary" 
                          plain
                          @click.stop="addOptionToQuestion(question)"
                        >
                          <el-icon><Plus /></el-icon>
                          添加選項
                        </el-button>
                      </div>
                    </div>
                  </div>

                  <!-- 填空/文本 -->
                  <div v-else-if="['3', '9'].includes(question.type)">
                    <el-input
                      v-model="question.placeholder"
                      placeholder="设置占位文字"
                      size="small"
                      class="placeholder-input"
                    />
                  </div>

                  <!-- 填空題目（拖拽權限控制） -->
                  <div v-else-if="question.type === '10'">
                    <div class="fillblank-question-container">
                      <!-- 题目内容编辑框 -->
                      <div class="fillblank-editor">
                        <div class="content-editable-container">
                          <div 
                            class="content-editable-div" 
                            contenteditable="true"
                            @input="onContentInput"
                            @keydown="onContentKeydown"
                            ref="contentEditableDiv"
                            placeholder="請輸入題目內容，然後點擊「添加填空」在需要填空的位置插入下劃線佔位符"
                          ></div>
                        </div>
                      </div>
                      
                      <!-- 添加填空按钮 -->
                      <div class="fillblank-toolbar">
                        <el-button 
                          type="primary" 
                          size="small" 
                          @click="addFillBlank(question)"
                          @mousedown.prevent
                          class="add-fillblank-btn"
                        >
                          <el-icon><Plus /></el-icon>
                          添加填空
                        </el-button>
                        <el-tag type="info" size="small" class="tip-tag">
                          點擊按鈕將在光標位置插入下劃線佔位符
                        </el-tag>
                    </div>
                    </div>
                  </div>

                  <!-- 附件 -->
                  <div v-else-if="question.type === '4'">
                    <div class="attachment-placeholder">
                      <el-icon><Upload /></el-icon>
                      <span>點擊上傳附件 (最多 5 個)</span>
                    </div>
                  </div>

                  <!-- 分支 -->
                  <div v-else-if="question.type === '5'">
                    <div class="branch-preview">
                      <div v-for="(branch, bi) in question.branchOptions" :key="bi" class="branch-item">
                        <el-input v-model="branch.text" placeholder="選項文字" size="small" />
                        <el-tag size="small">{{ branch.action === 'continue' ? '繼續' : '結束' }}</el-tag>
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
                  <div class="section-header">題目類型</div>
                  <el-select
                    v-model="selectedQuestion.type"
                    placeholder="選擇題型"
                    class="type-select"
                    disabled
                  >
                    <el-option label="單選" value="1" />
                    <el-option label="多選" value="2" />
                    <el-option label="圖片單選" value="7" />
                    <el-option label="圖片多選" value="8" />
                    <el-option label="單行文本" value="9" />
                    <el-option label="多行文本" value="3" />
                    <el-option label="附件" value="4" />
                    <el-option label="分支" value="5" />
                    <el-option label="填空" value="10" />
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
                        placeholder="請輸入題目內容"
                      />
                    </el-form-item>

                    <el-form-item label="必答設置">
                      <el-checkbox v-model="selectedQuestion.required">必答題</el-checkbox>
                    </el-form-item>
                  </el-form>
                </div>




              </div>

              <!-- 选项设置 -->
              <div v-show="activeTab === 'options'">
                <div class="settings-section">
                  <div class="section-header">選項設置</div>

                  <div v-if="hasOptionType(selectedQuestion.type)" class="options-editor">
                    <div 
                      v-for="(option, index) in selectedQuestion.options" 
                      :key="index"
                      class="option-editor-item"
                    >
                      <span class="option-label">{{ getOptionLabel(index) }}</span>
                      <el-input
                        v-model="selectedQuestion.options[index]"
                        placeholder="選項內容"
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



                <div v-if="['7', '8'].includes(selectedQuestion.type)" class="settings-section">
                  <div class="section-header">圖片</div>
                  <el-form label-position="top" size="default">
                    <el-form-item label="默認圖片寬度">
                      <el-input v-model="selectedQuestion.imageWidth" placeholder="請輸入">
                        <template #append>像素</template>
                      </el-input>
                    </el-form-item>
                    <el-button class="apply-image-btn" size="small">
                      應用到本題所有圖片
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
  Edit, Close, Check, Menu, CircleCheck, Checked, 
  Delete, Plus, Upload, View, Download, Notebook, Grid, Connection,
  ArrowUp, ArrowDown, InfoFilled
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'FormQuestionDialog',
  components: {
    Edit, Close, Check, Menu, CircleCheck, Checked, 
    Delete, Plus, Upload, View, Download, Notebook, Grid, Connection,
    ArrowUp, ArrowDown, InfoFilled
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
  mounted() {
    // 注册全局删除方法（用于预览区域）
    window.removeFillBlank = (index) => {
      if (this.selectedQuestion) {
        this.removeFillBlank(this.selectedQuestion, index)
      }
    }
    
    // 初始化 contenteditable div 的事件委托
    this.$nextTick(() => {
      let editableDiv = this.$refs.contentEditableDiv
      // 如果是数组，取第一个元素
      if (Array.isArray(editableDiv) && editableDiv.length > 0) {
        editableDiv = editableDiv[0]
      }
      
      if (editableDiv) {
        editableDiv.addEventListener('click', (e) => {
          if (e.target.classList.contains('blank-tag-remove')) {
            const tag = e.target.closest('.editable-blank-tag')
            if (tag) {
              const index = parseInt(tag.dataset.index)
              this.removeFillBlank(this.selectedQuestion, index)
            }
          }
        })
      }
    })
  },
  beforeDestroy() {
    // 清理全局方法
    if (window.removeFillBlank) {
      delete window.removeFillBlank
    }
  },
  methods: {
    getOptionLabel(index) {
      return String.fromCharCode(65 + index)
    },
  
    isSingleChoice(type) {
      // 單選類：1=單選，7=圖片單選
      return ['1', '7'].includes(type)
    },
  
    hasOptionType(type) {
      // 需要選項的題型：1=單選，2=多選，7=圖片單選，8=圖片多選
      return ['1', '2', '7', '8'].includes(type)
    },

    initForm() {
      if (this.question) {
        // 編輯模式：加載現有數據
        this.questionnaireData = { title: '問卷調查', description: '' }
        this.questionList = [{
          ...this.question,
          id: this.question.id || Date.now(),
          description: this.question.description || '',
          placeholder: this.question.placeholder || '',
          defaultValue: this.question.defaultValue || '',
          validation: this.question.validation || [],
          randomOrder: this.question.randomOrder || false
        }]
        this.selectedQuestionId = this.questionList[0].id
      } else {
        this.resetForm()
      }
    },

    resetForm() {
      this.questionList = []
      this.questionnaireData = { title: '', description: '' }
      this.selectedQuestionId = null
      this.nextId = 1
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
      
      // 如果是填空题类型（新增的拖拽权限控制题目）
      if (type === '10') {
        question.fillBlanks = [] // 初始为空，由用户动态添加
        question.correctAnswers = [] // 正确答案数组，与 fillBlanks 对应
      }
      
      this.questionList.push(question)
      this.selectedQuestionId = question.id
      
      ElMessage.success({
        message: '已添加題目',
        offset: 100
      })
    },

    getDefaultOptions(type) {
      // 需要選項的題型返回初始選項
      if (['1', '2', '7', '8'].includes(type)) {
        return ['', '']
      }
      // 填空題類型返回空數組
      if (type === '10') {
        return null
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
      ElMessage.success({
        message: '刪除成功',
        offset: 100
      })
    },

    addOption() {
      if (this.selectedQuestion) {
        this.selectedQuestion.options.push('')
      }
    },

    addOptionToQuestion(question) {
      question.options.push('')
    },

    addOptionAtIndex(question, index) {
      question.options.splice(index, 0, '')
    },

    removeOption(index) {
      if (this.selectedQuestion && this.selectedQuestion.options.length > 2) {
        this.selectedQuestion.options.splice(index, 1)
      }
    },

    removeOptionAtIndex(question, index) {
      if (question.options.length > 2) {
        question.options.splice(index, 1)
      }
    },

    startOptionSettings() {
      // 已移除
    },

    handleFillBlankAnswer(index, value, question) {
      if (!question.userAnswers) {
        question.userAnswers = []
      }
      question.userAnswers[index] = value
    },

    addFillBlank(question) {
      if (!question.fillBlanks) {
        question.fillBlanks = []
      }
      const newBlank = {
        id: `fillblank-${Date.now()}`,
        type: 'fillblank',
        required: true,
        validate: 'unlimited'
      }
      question.fillBlanks.push(newBlank)
      if (!question.correctAnswers) {
        question.correctAnswers = []
      }
      question.correctAnswers.push('') // 添加空的答案占位
      
      // 在 contenteditable div 中插入占位符标签
      let editableDiv = this.$refs.contentEditableDiv
      // 如果是数组，取第一个元素
      if (Array.isArray(editableDiv) && editableDiv.length > 0) {
        editableDiv = editableDiv[0]
      }
      
      if (editableDiv) {
        const selection = window.getSelection()
        let range;
        
        if (selection && selection.rangeCount > 0) {
          range = selection.getRangeAt(0)
          // 確保光標在編輯區域內，否則默認添加到末尾
          if (!editableDiv.contains(range.commonAncestorContainer) && editableDiv !== range.commonAncestorContainer) {
            range = null
          }
        }
        
        if (!range) {
          range = document.createRange()
          range.selectNodeContents(editableDiv)
          range.collapse(false) // 光標移到最後
          if (selection) {
            selection.removeAllRanges()
            selection.addRange(range)
          }
        }
        
        // 创建占位符标签
        const blankIndex = question.fillBlanks.length
        const blankTag = document.createElement('span')
        blankTag.className = 'editable-blank-tag'
        blankTag.contentEditable = 'false'
        blankTag.style.cssText = 'display:inline-block;position:relative;text-align:center;padding:0 2px 14px 2px;margin:0 2px;vertical-align:baseline;white-space:nowrap;cursor:default;user-select:none;'
        blankTag.innerHTML = `<span style="display:inline-block;font-size:14px;color:transparent;border-bottom:1px solid #303133;min-width:80px;">____________</span><span style="position:absolute;left:50%;transform:translateX(-50%);bottom:0;font-size:11px;color:#E6A23C;white-space:nowrap;line-height:1;"><span style="color:#F56C6C;">*</span> 填空${blankIndex}</span>`
        blankTag.dataset.index = blankIndex - 1
        
        // 插入到光标位置（先收合選取範圍，避免刪除已選中的文字）
        range.collapse(false)
        
        // 在佔位符前後插入零寬空格，防止瀏覽器在輸入文字時誤刪非可編輯元素
        const zwsp = '\u200B'
        const afterText = document.createTextNode(zwsp)
        const beforeText = document.createTextNode(zwsp)
        
        range.insertNode(afterText)
        range.insertNode(blankTag)
        range.insertNode(beforeText)
        
        // 光标移到佔位符後面的零寬空格之後
        range.setStartAfter(afterText)
        range.setEndAfter(afterText)
        selection.removeAllRanges()
        selection.addRange(range)
          
          // 同步更新 content
          let html = editableDiv.innerHTML
          const tags = editableDiv.querySelectorAll('.editable-blank-tag')
          tags.forEach((tag) => {
            const index = parseInt(tag.dataset.index) + 1
            const placeholder = `{{fillblank-${index}}}`
            html = html.replace(tag.outerHTML, placeholder)
          })
          const tempDiv = document.createElement('div')
          tempDiv.innerHTML = html
          question.content = tempDiv.textContent || tempDiv.innerText || ''
      }
      
      ElMessage.success({
        message: '已添加填空',
        offset: 100
      })
    },

    onContentInput() {
      // 内容变化时不立即同步，避免光标跳动
      // 只在保存时才同步
    },

    onContentKeydown(e) {
      if (e.key === 'Backspace' || e.key === 'Delete') {
        const selection = window.getSelection()
        if (!selection || selection.rangeCount === 0) return
        
        const range = selection.getRangeAt(0)
        
        // 分别处理 Backspace 和 Delete
        if (e.key === 'Backspace') {
          // 查找光标前一个节点
          let prevNode = null
          if (range.startContainer.nodeType === 3) {
            // 在文本节点中
            if (range.startOffset === 0) {
              prevNode = range.startContainer.previousSibling
            }
          } else if (range.startContainer.nodeType === 1) {
            // 在元素节点中
            if (range.startOffset > 0) {
              prevNode = range.startContainer.childNodes[range.startOffset - 1]
            }
          }
          
          if (prevNode && prevNode.classList && prevNode.classList.contains('editable-blank-tag')) {
            e.preventDefault()
            prevNode.remove()
            return
          }
        } 
        else if (e.key === 'Delete') {
          // 查找光标后一个节点
          let nextNode = null
          if (range.startContainer.nodeType === 3) {
            // 在文本节点中
            if (range.startOffset === range.startContainer.length) {
              nextNode = range.startContainer.nextSibling
            }
          } else if (range.startContainer.nodeType === 1) {
            // 在元素节点中
            if (range.startOffset < range.startContainer.childNodes.length) {
              nextNode = range.startContainer.childNodes[range.startOffset]
            }
          }
          
          if (nextNode && nextNode.classList && nextNode.classList.contains('editable-blank-tag')) {
            e.preventDefault()
            nextNode.remove()
            return
          }
        }
      }
    },

    removeFillBlank(question, index) {
      if (question.fillBlanks && question.fillBlanks.length > 0) {
        question.fillBlanks.splice(index, 1)
        if (question.correctAnswers && question.correctAnswers.length > index) {
          question.correctAnswers.splice(index, 1)
        }
        ElMessage.success('已刪除填空')
      }
    },

    renderFillBlanks(question) {
      if (!question.content) return ''
      
      let html = question.content
      // 先转义 HTML 特殊字符
      html = html.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      
      // 匹配所有可能的占位符格式（包括损坏的）
      const allPlaceholderPatterns = [
        /\{\{fillblank-(\d+)\}\}/g,  // 完整格式
        /\{fillblank-(\d+)\}\}/g,     // 缺少一个 {
        /\{\{fillblank-(\d+)\}/g,     // 缺少一个 }
        /\{fillblank-(\d+)\}/g        // 缺少两个 {}
      ]
      
      // 收集所有找到的占位符
      const foundPlaceholders = new Set()
      allPlaceholderPatterns.forEach(pattern => {
        let match
        while ((match = pattern.exec(html)) !== null) {
          foundPlaceholders.add(parseInt(match[1]) - 1)
        }
      })
      
      // 按顺序替换所有找到的占位符
      const sortedIndices = Array.from(foundPlaceholders).sort((a, b) => a - b)
      sortedIndices.forEach(index => {
        // 匹配这个占位符的各种格式
        const patterns = [
          new RegExp(`\\{\\{fillblank-${index + 1}\\}\\}`, 'g'),
          new RegExp(`\\{fillblank-${index + 1}\\}\\}`, 'g'),
          new RegExp(`\\{\\{fillblank-${index + 1}\\}`, 'g'),
          new RegExp(`\\{fillblank-${index + 1}\\}`, 'g')
        ]
        
        const underlineBlank = `<span class="underline-placeholder" data-index="${index}"><span class="underline-text">__________</span><i class="remove-blank-icon" onclick="window.removeFillBlank(${index})">×</i></span>`
        
        patterns.forEach(pattern => {
          html = html.replace(pattern, underlineBlank)
        })
      })
      
      return html
    },

    scrollToLeftPanel() {
      ElMessage.info({
        message: '請從左側面板選擇題型添加題目',
        offset: 100
      })
    },

    handleClose() {
      this.$emit('update:visible', false)
    },

    handleSave() {
      if (this.questionList.length === 0) {
        ElMessage.warning('請至少添加一道題目')
        return
      }

      // 驗證必填項
      for (let i = 0; i < this.questionList.length; i++) {
        const q = this.questionList[i]
        if (!q.title?.trim()) {
          ElMessage.warning({
            message: `第 ${i + 1} 題的題目內容不能為空`,
            offset: 100
          })
          this.selectedQuestionId = q.id
          return
        }
        
        // 驗證選項
        if (this.hasOptionType(q.type)) {
          const validOptions = q.options.filter(opt => opt.trim())
          if (validOptions.length < 2) {
            ElMessage.warning({
              message: `第 ${i + 1} 題至少需要 2 個選項`,
              offset: 100
            })
            this.selectedQuestionId = q.id
            return
          }
        }
        
        // 驗證填空題
        if (q.type === '10') {
          if (q.fillBlanks && q.fillBlanks.length > 0) {
            for (let j = 0; j < q.fillBlanks.length; j++) {
              const blank = q.fillBlanks[j]
              const userAnswer = q.userAnswers ? q.userAnswers[j] : ''
              if (blank.required && (!userAnswer || !userAnswer.trim())) {
                ElMessage.warning({
                  message: `第 ${i + 1} 題的第 ${j + 1} 個填空不能為空`,
                  offset: 100
                })
                this.selectedQuestionId = q.id
                return
              }
            }
          }
        }
      }

      this.$emit('save', {
        questionnaire: this.questionnaireData,
        questions: this.questionList
      })
      this.$emit('update:visible', false)
      ElMessage.success({
        message: '保存成功',
        offset: 100
      })
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
  z-index: 2000; /* 降低 z-index，让 ElMessage 能正常显示 */
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.3s ease;
}

.form-question-dialog {
  width: 99vw;
  height: 98vh;
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

/* 頂部工具欄 */
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
  background: #409EFF;
  color: white;
  border: none;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.save-btn:hover {
  background: #66b1ff;
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.4);
  transform: translateY(-2px);
}

/* 主體內容區 - 三欄佈局 */
.dialog-main {
  flex: 1;
  display: flex;
  gap: 12px;
  padding: 12px;
  overflow: hidden;
  background: #f5f7fa;
  min-height: 0; /* 防止 flex 子項溢出 */
}

/* 左側面板 */
.left-panel {
  width: 320px;
  flex-shrink: 0;
  min-width: 280px; /* 設置最小寬度防止過度壓縮 */
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

/* 題區塊 */
.question-type-section {
  margin-bottom: 20px;
  padding: 14px;
  background: linear-gradient(135deg, #fafafa 0%, #ffffff 100%);
  border-radius: 10px;
  border: 1px solid #f0f0f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.question-type-section:hover {
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

/* 中間面板 */
.center-panel {
  flex: 1;
  min-width: 0; /* 允許 flex 子項縮小到內容以下 */
  display: flex;
  flex-direction: column;
  overflow: hidden; /* 防止溢出 */
}

.questionnaire-preview {
  flex: 1;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0; /* 防止 flex 子項溢出 */
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
  padding: 20px 24px;
  background: #f0f2f5;
  min-height: 0; /* 允許滾動區域正常滾動 */
}

.questionnaire-body::-webkit-scrollbar {
  width: 6px;
}

.questionnaire-body::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 3px;
}

/* 題目卡片 */
.question-card {
  background: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.question-card:hover {
  border-color: #409EFF;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.12);
  transform: translateY(-2px);
}

.question-card.active {
  border-color: #409EFF;
  box-shadow: 0 4px 20px rgba(64, 158, 255, 0.2);
  background: #ecf5ff;
}

.question-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.question-number {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  min-width: 24px;
  height: 24px;
  background: #409EFF;
  border-radius: 50%;
  flex-shrink: 0;
  position: relative;
}

.required-mark {
  position: absolute;
  top: -2px;
  right: -4px;
  color: #f56c6c;
  font-weight: 700;
  font-size: 14px;
  line-height: 1;
  text-shadow: 0 0 2px rgba(255, 255, 255, 0.8);
}

.question-title-input {
  flex: 1;
}

.question-title-input :deep(.el-input__inner) {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  background: transparent;
  padding: 6px 10px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.question-title-input :deep(.el-input) {
  border: none;
  transition: all 0.2s ease;
}

.question-title-input:hover :deep(.el-input__inner) {
  background: #f5f7fa;
}

.question-title-input:hover :deep(.el-input) {
  border-color: #dcdfe6;
}

.question-card.active .question-title-input:hover :deep(.el-input__inner) {
  background: #ffffff;
}

.question-card.active .question-title-input:hover :deep(.el-input) {
  border-color: #409EFF;
}

.question-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s ease;
  margin-left: auto;
  flex-shrink: 0;
}

.question-card:hover .question-actions {
  opacity: 1;
}

.question-actions .el-button {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  transition: all 0.2s ease;
}

.question-actions .el-button:hover {
  transform: scale(1.05);
}

/* 刪除按鈕樣式 */
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

/* 題目內容 */
.question-content {
  padding-left: 36px;
  background: #fafafa;
  border-radius: 6px;
  padding: 12px;
}

.placeholder-input {
  max-width: 400px;
}

.attachment-placeholder {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #f5f7fa;
  border: 2px dashed #dcdfe6;
  border-radius: 4px;
  color: #909399;
  font-size: 13px;
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

/* 選項列表樣式 */
.options-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  background: #ffffff;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.option-row:hover {
  background: #f5f7fa;
}

.option-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.add-option-row {
  display: flex;
  justify-content: center;
  padding: 12px 8px;
  border-top: 1px dashed #e4e7ed;
  margin-top: 8px;
}

.add-option-row .el-button {
  width: 100%;
  background: #ecf5ff;
  border-color: #409EFF;
  color: #409EFF;
}

.add-option-row .el-button:hover {
  background: #409EFF;
  border-color: #409EFF;
  color: #ffffff;
}

.option-prefix {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 50px;
}

.option-prefix :deep(.el-radio__label),
.option-prefix :deep(.el-checkbox__label) {
  display: none !important;
}

.option-prefix :deep(.el-radio),
.option-prefix :deep(.el-checkbox) {
  margin-right: 0;
}

.option-label {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  min-width: 18px;
  text-align: center;
}

.option-input {
  flex: 1;
}

.option-input :deep(.el-input__inner) {
  font-size: 13px;
  padding: 6px 10px;
  border-color: #e4e7ed;
  transition: all 0.2s ease;
}

.option-input :deep(.el-input__inner):hover {
  border-color: #409EFF;
}

.option-input :deep(.el-input__inner):focus {
  border-color: #409EFF;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.questionnaire-footer {
  padding: 16px 24px;
  border-top: 1px solid #e4e7ed;
  background: #f0f2f5;
  text-align: center;
}

.add-question-tips {
  border: 2px dashed #409EFF;
  background: #ecf5ff;
  color: #409EFF;
  border-radius: 6px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.add-question-tips:hover {
  background: #d9ecff;
  border-color: #67c23a;
  color: #67c23a;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

/* 空狀態 */
.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
  background: #ffffff;
  border-radius: 8px;
  border: 2px dashed #e4e7ed;
}

/* 右側面板 */
.right-panel {
  width: 340px;
  flex-shrink: 0;
  min-width: 300px; /* 設置最小寬度防止過度壓縮 */
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

/* 面板標籤頁 */
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

/* 設置區塊 */
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
  color: #606266;
  min-width: 18px;
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

.settings-form .el-form-item:last-child {
  margin-bottom: 0;
}

/* 未選中狀態 */
.no-selection {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

/* 填空題目樣式 */
.fillblank-question-container {
  padding: 20px;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-radius: 12px;
  border: 2px solid #e4e7ed;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.fillblank-editor {
  margin-bottom: 16px;
}

.content-textarea {
  width: 100%;
}

.content-textarea :deep(.el-textarea__inner) {
  font-size: 14px;
  line-height: 1.8;
  border: 2px solid #409EFF;
  border-radius: 8px;
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.content-textarea :deep(.el-textarea__inner):hover {
  border-color: #67c23a;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.content-textarea :deep(.el-textarea__inner):focus {
  border-color: #67c23a;
  box-shadow: 0 4px 16px rgba(103, 194, 58, 0.15);
  background: #ffffff;
}

.content-editable-wrapper {
  position: relative;
}

.editable-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  padding: 8px 12px;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  font-size: 12px;
  color: #909399;
}

.editable-hint .el-icon {
  font-size: 14px;
  color: #409EFF;
}

.content-editable-container {
  position: relative;
}

.content-editable-div {
  width: 100%;
  min-height: 150px;
  max-height: 400px;
  padding: 16px 16px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  font-size: 14px;
  line-height: 3;
  color: #303133;
  background: #ffffff;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
  white-space: pre-wrap;
  word-wrap: break-word;
  overflow-y: auto;
  overflow-x: hidden;
}

.content-editable-div:hover {
  border-color: #67c23a;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.content-editable-div:focus {
  border-color: #409EFF;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.15);
  background: #ffffff;
}

.content-editable-div:empty:before {
  content: attr(placeholder);
  color: #909399;
  opacity: 0.6;
}

.editable-blank-tag {
  display: inline-block;
  position: relative;
  text-align: center;
  padding: 0 2px 16px 2px;
  margin: 0 2px;
  background: transparent;
  border: none;
  border-radius: 0;
  cursor: default;
  user-select: none;
  vertical-align: baseline;
  white-space: nowrap;
}

.editable-blank-tag:hover .blank-tag-line {
  border-bottom-color: #409EFF;
}

.blank-tag-line {
  font-size: 14px;
  color: transparent;
  border-bottom: 1px solid #303133;
  min-width: 80px;
  display: inline-block;
}

.blank-tag-label {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: 0;
  font-size: 11px;
  color: #E6A23C;
  white-space: nowrap;
  line-height: 1;
}

.blank-tag-star {
  color: #F56C6C;
  font-size: 11px;
}

.fillblank-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px dashed #dcdfe6;
}

.tip-tag {
  font-size: 12px;
  padding: 4px 10px;
}

.add-fillblank-btn {
  background: linear-gradient(135deg, #409EFF 0%, #67c23a 100%);
  border: none;
  color: white;
  font-weight: 600;
  padding: 8px 16px;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.add-fillblank-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.4);
}

.fillblank-preview {
  margin-bottom: 16px;
  padding: 16px;
  background: #ffffff;
  border-radius: 8px;
  border: 2px solid #e4e7ed;
}

.preview-label {
  font-size: 13px;
  font-weight: 700;
  color: #67c23a;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.preview-label::before {
  content: '';
  width: 4px;
  height: 16px;
  background: linear-gradient(180deg, #409EFF 0%, #67c23a 100%);
  border-radius: 2px;
}

.fillblank-text {
  line-height: 2.5;
  font-size: 15px;
  color: #303133;
  display: block;
}

.underline-placeholder {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  position: relative;
  margin: 0 4px;
}

.underline-text {
  display: inline-block;
  color: #409EFF;
  font-weight: 600;
  font-size: 14px;
  padding: 4px 12px 2px;
  border-bottom: 2px solid #409EFF;
  min-width: 100px;
  text-align: center;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.05) 0%, rgba(64, 158, 255, 0.02) 100%);
  border-radius: 2px 2px 0 0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.underline-text:hover {
  color: #67c23a;
  border-bottom-color: #67c23a;
  background: linear-gradient(135deg, rgba(103, 194, 58, 0.08) 0%, rgba(103, 194, 58, 0.03) 100%);
  transform: translateY(-1px);
}

.remove-blank-icon {
  font-size: 14px;
  cursor: pointer;
  color: #909399;
  transition: all 0.2s ease;
  margin-left: 4px;
}

.remove-blank-icon:hover {
  color: #F56C6C;
  transform: scale(1.2);
}

.fillblank-tips {
  margin-top: 16px;
}

.mt-2 {
  margin-top: 8px;
}

/* 響應式設計 */
@media (max-width: 1400px) {
  .left-panel {
    width: 280px;
    min-width: 260px;
  }
  
  .right-panel {
    width: 300px;
    min-width: 280px;
  }
}

@media (max-width: 1200px) {
  .dialog-main {
    flex-direction: column;
  }
  
  .left-panel,
  .right-panel {
    width: 100%;
    min-width: auto;
    max-height: 200px;
    flex-shrink: 1; /* 允許壓縮 */
  }
  
  .center-panel {
    min-height: 400px; /* 確保中間區域有足夠高度 */
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
  
  .left-panel,
  .right-panel {
    max-height: none; /* 移動設備上取消高度限制 */
  }
}

</style>
