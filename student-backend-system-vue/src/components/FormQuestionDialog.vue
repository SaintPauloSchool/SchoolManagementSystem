<template>
  <div class="form-question-dialog-overlay" v-if="visible">
    <div class="form-question-dialog">
      <!-- 頂部工具欄 -->
      <div class="dialog-toolbar" v-if="viewMode === 'edit'">
        <div class="toolbar-left">
          <h2 class="toolbar-title">
            <el-icon class="title-icon"><Edit /></el-icon>
            編輯表單問題
          </h2>
        </div>
        <div class="toolbar-center">
          <el-button 
            :type="viewMode === 'edit' ? 'primary' : 'info'" 
            @click="switchViewMode('edit')"
            class="view-mode-btn"
            size="default"
          >
            <el-icon><Edit /></el-icon>
            編輯
          </el-button>
          <el-button 
            :type="viewMode === 'preview' ? 'success' : 'info'" 
            @click="switchViewMode('preview')"
            class="view-mode-btn"
            size="default"
          >
            <el-icon><Connection /></el-icon>
            邏輯編輯
          </el-button>
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

      <!-- 預覽模式工具欄 -->
      <div class="dialog-toolbar preview-toolbar" v-else>
        <div class="toolbar-center">
          <el-button 
            :type="viewMode === 'edit' ? 'info' : 'primary'" 
            @click="switchViewMode('edit')"
            class="view-mode-btn"
            size="default"
          >
            <el-icon><Edit /></el-icon>
            返回編輯
          </el-button>
        </div>
      </div>

      <!-- 主體內容區 - 三欄佈局 -->
      <div class="dialog-main" :class="{ 'preview-mode': viewMode === 'preview' }">
        <!-- 左側：題型選擇器 -->
        <div class="left-panel" :class="{ 'hidden': viewMode === 'preview' }">
          <!-- 可折疊的面板組 -->
          <el-collapse v-model="activePanels" accordion @change="handlePanelChange">
            <!-- 題型選擇面板 -->
            <el-collapse-item name="questionType">
              <template #title>
                <div class="panel-title-with-icon">
                  <el-icon><Menu /></el-icon>
                  <span>題型選擇</span>
                </div>
              </template>
              <div class="panel-content">
                <!-- 題目類型 -->
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
            </el-collapse-item>

            <!-- 邏輯編輯面板（只在預覽模式顯示） -->
            <el-collapse-item name="logicEdit" v-if="viewMode === 'preview'">
              <template #title>
                <div class="panel-title-with-icon">
                  <el-icon><Connection /></el-icon>
                  <span>邏輯編輯</span>
                </div>
              </template>
              <div class="panel-content logic-edit-panel-content">
                <div class="logic-edit-section">
                  <div v-if="selectedQuestion" class="logic-editor">
                    <div class="question-type-hint">
                      <el-tag :type="getLogicTypeTag(selectedQuestion.type)" size="small">
                        {{ getQuestionTypeName(selectedQuestion.type) }}
                      </el-tag>
                      <span class="hint-text">此題型支援跳轉邏輯</span>
                    </div>

                    <div v-if="hasOptionType(selectedQuestion.type)" class="logic-rules-wrapper">
                      <div class="logic-rules-header">
                        <span class="rules-title">跳轉規則列表</span>
                        <div class="header-buttons">
                          <el-button 
                            v-if="!selectedQuestion.logicRuleList || selectedQuestion.logicRuleList.length === 0"
                            type="primary" 
                            size="small"
                            @click="addLogicRule"
                            class="add-rule-btn"
                          >
                            <el-icon><Plus /></el-icon>
                            新增規則
                          </el-button>
                          <el-button 
                            v-else
                            type="danger" 
                            size="small"
                            @click="removeAllLogicRules"
                            class="remove-all-rules-btn"
                          >
                            <el-icon><Delete /></el-icon>
                            刪除所有規則
                          </el-button>
                        </div>
                      </div>

                      <div class="rules-grid">
                        <div 
                          v-for="(rule, ruleIndex) in selectedQuestion.logicRuleList" 
                          :key="rule.id"
                          class="logic-rule-card"
                        >
                          <div class="rule-card-header">
                            <div class="rule-index">
                              <el-icon><Connection /></el-icon>
                              <span>規則 {{ ruleIndex + 1 }}</span>
                            </div>
                          </div>

                          <div class="rule-card-body">
                            <div class="rule-row">
                              <span class="rule-label">如果</span>
                              <span class="rule-question-name">{{ selectedQuestion.title || '本題' }}</span>
                            </div>

                            <div class="rule-row">
                              <span class="rule-label">選中</span>
                              <el-tag :type="getOptionTagType(rule.optionIndex)" size="default" class="option-tag">
                                {{ getOptionLabel(rule.optionIndex) }}. {{ selectedQuestion.options[rule.optionIndex] || '未命名' }}
                              </el-tag>
                            </div>

                            <div class="rule-row">
                              <span class="rule-label">則</span>
                              <span class="rule-action-label">跳轉至</span>
                              <el-select 
                                v-model="rule.jumpTarget" 
                                placeholder="選擇跳轉目標"
                                size="default"
                                class="jump-select"
                                clearable
                                filterable
                              >
                                <el-option-group label="常用選項">
                                  <el-option label="➡️ 下一題" value="next" />
                                  <el-option label="⏹️ 結束問卷" value="end" />
                                </el-option-group>
                                <el-option-group label="題目列表">
                                  <el-option 
                                    v-for="(q, qIndex) in questionList" 
                                    :key="q.id"
                                    :label="`${qIndex + 1}. ${q.title || '未命名題目'}${q.id === selectedQuestion.id ? ' (當前題目)' : ''}`"
                                    :value="q.id"
                                    :disabled="q.id === selectedQuestion.id"
                                  />
                                </el-option-group>
                              </el-select>
                            </div>

                            <div class="rule-preview">
                              <el-tag :type="getRulePreviewTag(rule.jumpTarget)" size="small">
                                當選擇 {{ getOptionName(rule.optionIndex) }} 時 → {{ getJumpTargetName(rule.jumpTarget) }}
                              </el-tag>
                            </div>
                          </div>
                        </div>
                      </div>

                      <div v-if="!selectedQuestion.logicRuleList || selectedQuestion.logicRuleList.length === 0" class="empty-rules">
                        <el-empty description="暫無跳轉規則" :image-size="60" />
                        <el-tag type="info" size="small">💡 點擊上方「新增規則」按鈕開始設置</el-tag>
                      </div>
                    </div>

                    <div v-else-if="['3', '9', '10'].includes(selectedQuestion.type)" class="no-logic-hint">
                      <el-icon><InfoFilled /></el-icon>
                      <div class="hint-content">
                        <span class="hint-title">填空/文本類題型</span>
                        <span class="hint-desc">此類題型通常不需要設置選項跳轉，可直接使用顯示邏輯</span>
                      </div>
                    </div>

                    <div v-else class="no-logic-hint">
                      <el-icon><CircleClose /></el-icon>
                      <span>該題型不支援跳轉邏輯</span>
                    </div>
                  </div>
                  <div v-else class="no-selection-logic">
                    <el-empty description="請在中間區域選擇題目進行邏輯設置" :image-size="80" />
                    <div class="empty-tips">
                      <el-tag type="info" size="small">💡 點擊任意題目即可開始設置邏輯</el-tag>
                    </div>
                  </div>
                </div>
              </div>
            </el-collapse-item>

          </el-collapse>
        </div>

        <!-- 中間：問卷預覽/編輯區 -->
        <div class="center-panel">
          <div class="questionnaire-preview">
            <!-- 問卷頭部 - 只在編輯模式顯示 -->
            <div class="questionnaire-header" v-if="viewMode === 'edit'">
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

            <!-- 題目列表 -->
            <div class="questionnaire-body">
              <!-- 預覽模式：結構化列表 -->
              <div v-if="viewMode === 'preview'" class="preview-flowchart-container">
                <!-- SVG 流程線層 -->
                <svg class="flow-connector-svg" xmlns="http://www.w3.org/2000/svg">
                  <defs>
                    <marker id="arrowhead" markerWidth="10" markerHeight="7" 
                            refX="9" refY="3.5" orient="auto">
                      <polygon points="0 0, 10 3.5, 0 7" fill="#67c23a" />
                    </marker>
                    <linearGradient id="lineGradient" x1="0%" y1="0%" x2="100%" y2="0%">
                      <stop offset="0%" style="stop-color:#409EFF;stop-opacity:1" />
                      <stop offset="100%" style="stop-color:#67c23a;stop-opacity:1" />
                    </linearGradient>
                  </defs>
                </svg>
                              
                <div 
                  v-for="(node, nodeIndex) in structuredQuestionList" 
                  :key="node.id"
                  class="preview-structured-item"
                  :class="{ 'active': selectedQuestionId === node.id }"
                  :style="{ marginLeft: (node.level * 40) + 'px' }"
                  :data-has-source="!!node.condition"
                  @click="selectQuestion(node.id)"
                >
                  <div class="preview-question-title-wrapper">
                    <!-- 顯示這題是從哪個選項跳過來的 -->
                    <div class="q-source-badge" v-if="node.condition">
                      <el-tag size="small" type="success" effect="dark" class="source-tag">
                        <el-icon class="source-icon"><Connection /></el-icon>
                        從：{{ node.condition.optionLabel }}. {{ node.condition.optionName || '未命名選項' }}
                      </el-tag>
                      <span class="jump-arrow">⬇</span>
                    </div>
              
                    <div class="preview-question-title">
                      <span class="q-number">{{ node.displayNum }}</span>
                      <span class="q-text">{{ node.title || '未命名題目' }}</span>
                      <el-tag :type="getQuestionTypeTag(node.type)" size="small" class="q-type-tag">
                        {{ getQuestionTypeName(node.type) }}
                      </el-tag>
                    </div>
                  </div>
                                
                  <!-- 邏輯規則預覽（帶流程線） -->
                  <div v-if="node.logicRuleList && node.logicRuleList.length > 0" class="preview-logic-flow-chart">
                    <div class="logic-flow-title">
                      <el-icon><Connection /></el-icon>
                      <span>邏輯分支</span>
                    </div>
                    <div class="logic-flow-lines">
                      <div 
                        v-for="(rule, ruleIdx) in node.logicRuleList" 
                        :key="rule.id"
                        class="logic-flow-line"
                      >
                        <div class="flow-line-connector">
                          <div class="option-node">
                            <span class="option-letter">{{ getOptionLabel(rule.optionIndex) }}</span>
                          </div>
                          <div class="flow-line-path">
                            <div class="flow-line"></div>
                            <div class="flow-arrow">➜</div>
                          </div>
                          <div class="target-node">
                            <el-tag :type="getRulePreviewTag(rule.jumpTarget)" size="small" effect="dark">
                              {{ getJumpTargetName(rule.jumpTarget) }}
                            </el-tag>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                                
                  <div v-if="['3', '9', '10'].includes(node.type)" class="preview-simple-hint">
                    <el-icon><Edit /></el-icon>
                    <span>填寫類題目</span>
                  </div>
                </div>
                              
                <div v-if="structuredQuestionList.length === 0" class="empty-state">
                  <el-empty description="暫無題目" />
                </div>
              </div>
              
              <!-- 編輯模式：原有樣式 -->
              <div v-else>
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
                  <!-- 題型標籤 -->
                  <el-tag 
                    :type="getQuestionTypeTag(question.type)" 
                    size="small"
                    class="question-type-tag"
                  >
                    {{ getQuestionTypeName(question.type) }}
                  </el-tag>
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

                <!-- 題目內容預覽 -->
                <div class="question-content">
                  <!-- 單選/多選/下拉 -->
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
                          :disabled="viewMode === 'preview'"
                        />
                        <div class="option-actions" v-if="viewMode === 'edit'">
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
                      <!-- 底部添加選項按鈕 -->
                      <div class="add-option-row" v-if="viewMode === 'edit'">
                        <el-button 
                          size="small" 
                          type="primary" 
                          plain
                          @click.stop="addOptionToQuestion(question)"
                        >
                          <el-icon><Plus /></el-icon>
                          新增選項
                        </el-button>
                      </div>
                    </div>
                    
                    <!-- 預覽模式下顯示邏輯規則流程圖 -->
                    <div v-if="viewMode === 'preview' && question.logicRuleList && question.logicRuleList.length > 0" class="logic-flowchart">
                      <div class="flowchart-title">
                        <el-icon><Connection /></el-icon>
                        <span>邏輯流程圖</span>
                      </div>
                      <div class="flowchart-content">
                        <div class="flow-start">
                          <span class="start-dot"></span>
                          <span class="start-text">開始</span>
                        </div>
                        <div class="flow-steps">
                          <div 
                            v-for="(rule, ruleIndex) in question.logicRuleList" 
                            :key="rule.id"
                            class="flow-step"
                          >
                            <div class="step-condition">
                              <span class="condition-badge">{{ getOptionLabel(rule.optionIndex) }}</span>
                              <span class="condition-text">{{ question.options[rule.optionIndex] || '未命名選項' }}</span>
                            </div>
                            <div class="step-arrow">↓</div>
                            <div class="step-action">
                              <el-tag :type="getRulePreviewTag(rule.jumpTarget)" size="small" effect="dark" round>
                                {{ getJumpTargetName(rule.jumpTarget) }}
                              </el-tag>
                            </div>
                          </div>
                        </div>
                        <div class="flow-end">
                          <span class="end-dot"></span>
                          <span class="end-text">結束</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- 填空/文本 -->
                  <div v-else-if="['3', '9'].includes(question.type)">
                    <el-input
                      v-model="question.placeholder"
                      placeholder="設置佔位文字"
                      size="small"
                      class="placeholder-input"
                      :disabled="viewMode === 'preview'"
                    />
                  </div>

                  <!-- 填空題目（拖拽權限控制） -->
                  <div v-else-if="question.type === '10'">
                    <div class="fillblank-question-container">
                      <!-- 題目內容編輯框 -->
                      <div class="fillblank-editor" v-if="viewMode === 'edit'">
                        <div class="content-editable-container">
                          <div 
                            class="content-editable-div" 
                            contenteditable="true"
                            @input="onContentInput"
                            @keydown="onContentKeydown"
                            ref="contentEditableDiv"
                            placeholder="請輸入題目內容，然後點擊「新增填空」在需要填空的位置插入下劃線佔位符"
                          ></div>
                        </div>
                      </div>
                      
                      <!-- 預覽模式下的題目顯示 -->
                      <div v-else-if="viewMode === 'preview'" class="fillblank-preview-display">
                        <div class="preview-text" v-html="renderFillBlankText(question)"></div>
                      </div>
                      
                      <!-- 添加填空按鈕 -->
                      <div class="fillblank-toolbar" v-if="viewMode === 'edit'">
                        <el-button 
                          type="primary" 
                          size="small" 
                          @click="addFillBlank(question)"
                          @mousedown.prevent
                          class="add-fillblank-btn"
                        >
                          <el-icon><Plus /></el-icon>
                          新增填空
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
              
              <!-- 空狀態 - 編輯模式 -->
              <div v-if="questionList.length === 0" class="empty-state">
                <el-empty description="請從左側選擇題型新增題目" />
              </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右側：屬性設置面板 -->
        <div class="right-panel" :class="{ 'hidden': viewMode === 'preview' && !selectedQuestion }">
          <!-- 編輯模式：屬性設置 -->
          <template v-if="viewMode === 'edit'">
            <div class="panel-tabs">
              <el-tabs v-model="activeTab" type="border-card">
                <el-tab-pane label="題目" name="question" />
                <el-tab-pane label="選項" name="options" />
              </el-tabs>
            </div>
            
            <div class="panel-content">
              <div v-if="selectedQuestion" class="settings-form">
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
                        新增選項
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
          </template>

          <!-- 預覽模式下的邏輯設置 -->
          <template v-else-if="viewMode === 'preview'">
            <div class="panel-header">
              <el-icon><Connection /></el-icon>
              <span>邏輯編輯</span>
            </div>
            <div class="panel-content logic-edit-panel">
              <div class="logic-edit-section">
                <div v-if="selectedQuestion" class="logic-editor">
                  <div class="question-type-hint">
                    <el-tag :type="getLogicTypeTag(selectedQuestion.type)" size="small">
                      {{ getQuestionTypeName(selectedQuestion.type) }}
                    </el-tag>
                    <span class="hint-text">此題型支援跳轉邏輯</span>
                  </div>

                  <div v-if="hasOptionType(selectedQuestion.type)" class="logic-rules-wrapper">
                    <div class="logic-rules-header">
                      <span class="rules-title">跳轉規則列表</span>
                      <div class="header-buttons">
                        <el-button 
                          v-if="!selectedQuestion.logicRuleList || selectedQuestion.logicRuleList.length === 0"
                          type="primary" 
                          size="small"
                          @click="addLogicRule"
                          class="add-rule-btn"
                        >
                          <el-icon><Plus /></el-icon>
                          新增規則
                        </el-button>
                        <el-button 
                          v-else
                          type="danger" 
                          size="small"
                          @click="removeAllLogicRules"
                          class="remove-all-rules-btn"
                        >
                          <el-icon><Delete /></el-icon>
                          刪除所有規則
                        </el-button>
                      </div>
                    </div>

                    <div class="rules-grid">
                      <div 
                        v-for="(rule, ruleIndex) in selectedQuestion.logicRuleList" 
                        :key="rule.id"
                        class="logic-rule-card"
                      >
                        <div class="rule-card-header">
                          <div class="rule-index">
                            <el-icon><Connection /></el-icon>
                            <span>規則 {{ ruleIndex + 1 }}</span>
                          </div>
                        </div>

                        <div class="rule-card-body">
                          <div class="rule-row">
                            <span class="rule-label">如果</span>
                            <span class="rule-question-name">{{ selectedQuestion.title || '本題' }}</span>
                          </div>

                          <div class="rule-row">
                            <span class="rule-label">選中</span>
                            <el-tag :type="getOptionTagType(rule.optionIndex)" size="default" class="option-tag">
                              {{ getOptionLabel(rule.optionIndex) }}. {{ selectedQuestion.options[rule.optionIndex] || '未命名' }}
                            </el-tag>
                          </div>

                          <div class="rule-row">
                            <span class="rule-label">則</span>
                            <span class="rule-action-label">跳轉至</span>
                            <el-select 
                              v-model="rule.jumpTarget" 
                              placeholder="選擇跳轉目標"
                              size="default"
                              class="jump-select"
                              clearable
                              filterable
                            >
                              <el-option-group label="常用選項">
                                <el-option label="➡️ 下一題" value="next" />
                                <el-option label="⏹️ 結束問卷" value="end" />
                              </el-option-group>
                              <el-option-group label="題目列表">
                                <el-option 
                                  v-for="(q, qIndex) in questionList" 
                                  :key="q.id"
                                  :label="`${qIndex + 1}. ${q.title || '未命名題目'}${q.id === selectedQuestion.id ? ' (當前題目)' : ''}`"
                                  :value="q.id"
                                  :disabled="q.id === selectedQuestion.id"
                                />
                              </el-option-group>
                            </el-select>
                          </div>

                          <div class="rule-preview">
                            <el-tag :type="getRulePreviewTag(rule.jumpTarget)" size="small">
                              當選擇 {{ getOptionName(rule.optionIndex) }} 時 → {{ getJumpTargetName(rule.jumpTarget) }}
                            </el-tag>
                          </div>
                        </div>
                      </div>
                    </div>

                    <div v-if="!selectedQuestion.logicRuleList || selectedQuestion.logicRuleList.length === 0" class="empty-rules">
                      <el-empty description="暫無跳轉規則" :image-size="60" />
                      <el-tag type="info" size="small">💡 點擊上方「新增規則」按鈕開始設置</el-tag>
                    </div>
                  </div>

                  <div v-else-if="['3', '9', '10'].includes(selectedQuestion.type)" class="no-logic-hint">
                    <el-icon><InfoFilled /></el-icon>
                    <div class="hint-content">
                      <span class="hint-title">填空/文本類題型</span>
                      <span class="hint-desc">此類題型通常不需要設置選項跳轉，可直接使用顯示邏輯</span>
                    </div>
                  </div>

                  <div v-else class="no-logic-hint">
                    <el-icon><CircleClose /></el-icon>
                    <span>該題型不支援跳轉邏輯</span>
                  </div>
                </div>
                <div v-else class="no-selection-logic">
                  <el-empty description="請在中間區域選擇題目進行邏輯設置" :image-size="80" />
                  <div class="empty-tips">
                    <el-tag type="info" size="small">💡 點擊任意題目即可開始設置邏輯</el-tag>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { 
  Edit, Close, Check, Menu, CircleCheck, Checked, 
  Delete, Plus, Upload, Connection,
  ArrowUp, ArrowDown, InfoFilled, CircleClose
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'FormQuestionDialog',
  components: {
    Edit, Close, Check, Menu, CircleCheck, Checked, 
    Delete, Plus, Upload, Connection,
    ArrowUp, ArrowDown, InfoFilled, CircleClose
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
      activePanels: ['questionType'], // 默認展開題型選擇面板
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
    },
    structuredQuestionList() {
      const displayList = [];
      const processedIds = new Set();
      
      const targetedMap = new Map();
      this.questionList.forEach(q => {
        if (q.logicRuleList) {
          q.logicRuleList.forEach(rule => {
            if (rule.jumpTarget && rule.jumpTarget !== 'next' && rule.jumpTarget !== 'end') {
              let tId = rule.jumpTarget;
              if (typeof tId === 'object') tId = tId.jumpTarget || tId.id;
              
              targetedMap.set(tId, {
                sourceId: q.id,
                optionIndex: rule.optionIndex,
                optionLabel: this.getOptionLabel(rule.optionIndex),
                optionName: q.options[rule.optionIndex]
              });
            }
          });
        }
      });

      let rootIndex = 1;
      
      const processNode = (nodeQ, parentNum, childIdx, condition) => {
        const num = parentNum ? `${parentNum}.${childIdx}` : `${rootIndex++}`;
        displayList.push({
          ...nodeQ,
          displayNum: num,
          condition: condition,
          isChild: !!parentNum,
          level: parentNum ? parentNum.split('.').length : 0
        });
        
        let localChildIdx = 1;
        if (nodeQ.logicRuleList) {
          nodeQ.logicRuleList.forEach(rule => {
            if (rule.jumpTarget && rule.jumpTarget !== 'next' && rule.jumpTarget !== 'end') {
              let tId = rule.jumpTarget;
              if (typeof tId === 'object') tId = tId.jumpTarget || tId.id;
              
              const targetQ = this.questionList.find(t => t.id === tId);
              if (targetQ && !processedIds.has(targetQ.id)) {
                processedIds.add(targetQ.id);
                processNode(targetQ, num, localChildIdx++, {
                  optionIndex: rule.optionIndex,
                  optionLabel: this.getOptionLabel(rule.optionIndex),
                  optionName: nodeQ.options[rule.optionIndex]
                });
              }
            }
          });
        }
      };

      this.questionList.forEach(q => {
        if (!targetedMap.has(q.id) && !processedIds.has(q.id)) {
          processedIds.add(q.id);
          processNode(q, null, 0, null);
        }
      });
      
      this.questionList.forEach((q) => {
        if (!processedIds.has(q.id)) {
          processedIds.add(q.id);
          processNode(q, null, 0, null);
        }
      });

      return displayList;
    }
  },
  watch: {
    visible(newVal) {
      if (newVal) {
        this.initForm()
      }
    },
    // 監聽所選題目的選項變化（只同步選項索引，不自動生成規則）
    selectedQuestion: {
      handler(newVal) {
        if (newVal && this.hasOptionType(newVal.type)) {
          // 只同步現有規則的 optionIndex，不會自動新增規則
          if (newVal.logicRuleList && newVal.logicRuleList.length > 0) {
            const optionsCount = newVal.options.length
            const currentRulesCount = newVal.logicRuleList.length
            
            // 如果規則數量大於選項數量，則刪除多餘的規則
            if (currentRulesCount > optionsCount) {
              for (let i = currentRulesCount - 1; i >= optionsCount; i--) {
                newVal.logicRuleList.splice(i, 1)
              }
            }
            // 重新綁定每個規則的 optionIndex
            newVal.logicRuleList.forEach((rule, index) => {
              rule.optionIndex = index
            })
          }
        }
      },
      deep: true
    }
  },
  mounted() {
    // 註冊全局刪除方法（用於預覽區域）
    window.removeFillBlank = (index) => {
      if (this.selectedQuestion) {
        this.removeFillBlank(this.selectedQuestion, index)
      }
    }
    
    // 初始化 contenteditable div 的事件委託
    this.$nextTick(() => {
      let editableDiv = this.$refs.contentEditableDiv
      // 如果是數組，取第一個元素
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

    // 獲取題型名稱
    getQuestionTypeName(type) {
      const typeMap = {
        '1': '單選題',
        '2': '多選題',
        '3': '多行文本',
        '4': '附件上傳',
        '5': '分支題',
        '7': '圖片單選',
        '8': '圖片多選',
        '9': '單行文本',
        '10': '填空題'
      }
      return typeMap[type] || '未知題型'
    },

    // 獲取題型標籤顏色
    getLogicTypeTag(type) {
      const tagMap = {
        '1': 'success',
        '2': 'success',
        '3': 'info',
        '4': 'warning',
        '5': 'danger',
        '7': 'success',
        '8': 'success',
        '9': 'info',
        '10': 'primary'
      }
      return tagMap[type] || 'info'
    },

    // 獲取題目類型標籤顏色（用於題目列表）
    getQuestionTypeTag(type) {
      const tagMap = {
        '1': 'success',
        '2': 'success',
        '3': 'info',
        '4': 'warning',
        '5': 'danger',
        '7': 'success',
        '8': 'success',
        '9': 'info',
        '10': 'primary'
      }
      return tagMap[type] || 'info'
    },

    // 獲取跳轉目標顯示名稱
    getJumpTargetName(target, optionIndex) {
      if (!target) {
        return '未設置'
      }
      if (target === 'next') {
        return '下一題'
      }
      if (target === 'end') {
        return '結束問卷'
      }
      // 查找對應的題目
      const question = this.questionList.find(q => q.id === target)
      if (question) {
        const index = this.questionList.findIndex(q => q.id === target)
        return `${index + 1}. ${question.title || '未命名題目'}`
      }
      return '未知目標'
    },



    // 獲取規則預覽標籤顏色
    getRulePreviewTag(target) {
      if (!target) {
        return 'info'
      }
      if (target === 'next') {
        return 'success'
      }
      if (target === 'end') {
        return 'danger'
      }
      return 'warning'
    },

    // 獲取選項名稱
    getOptionName(optionIndex) {
      if (optionIndex === null || optionIndex === undefined) {
        return '未選擇'
      }
      if (!this.selectedQuestion) {
        return '未選擇題目'
      }
      const option = this.selectedQuestion.options[optionIndex]
      return `${this.getOptionLabel(optionIndex)}. ${option || '未命名'}`
    },

    // 獲取選項標籤顏色
    getOptionTagType(optionIndex) {
      const types = ['success', 'warning', 'primary', 'danger', 'info']
      return types[optionIndex % types.length] || 'info'
    },



    // 新增邏輯規則（一次性生成所有選項的規則）
    addLogicRule() {
      if (!this.selectedQuestion) {
        ElMessage.warning('請先選擇題目')
        return
      }
      
      if (!this.hasOptionType(this.selectedQuestion.type)) {
        ElMessage.warning('該題型不需要設置跳轉規則')
        return
      }
      
      if (!this.selectedQuestion.logicRuleList) {
        this.selectedQuestion.logicRuleList = []
      }
      
      // 如果已經有規則，則不允許重複新增
      if (this.selectedQuestion.logicRuleList.length > 0) {
        ElMessage.warning('已經設置了跳轉規則')
        return
      }
      
      // 為所有選項生成規則
      const optionsCount = this.selectedQuestion.options.length
      for (let i = 0; i < optionsCount; i++) {
        const newRule = {
          id: `rule-${Date.now()}-${i}`,
          optionIndex: i,
          jumpTarget: 'next' // 默認跳轉至下一題
        }
        this.selectedQuestion.logicRuleList.push(newRule)
      }
      
      ElMessage.success({
        message: `已為 ${optionsCount} 個選項生成跳轉規則`,
        offset: 100
      })
    },

    // 刪除所有邏輯規則
    removeAllLogicRules() {
      if (!this.selectedQuestion || !this.selectedQuestion.logicRuleList) {
        return
      }
      
      this.selectedQuestion.logicRuleList = []
      
      ElMessage.success({
        message: '已刪除所有跳轉規則',
        offset: 100
      })
    },

    // 刪除邏輯規則
    removeLogicRule(ruleIndex) {
      if (!this.selectedQuestion || !this.selectedQuestion.logicRuleList) {
        return
      }
      
      this.selectedQuestion.logicRuleList.splice(ruleIndex, 1)
      
      ElMessage.success({
        message: '已刪除跳轉規則',
        offset: 100
      })
    },



    // 切換視圖模式（編輯/預覽）
    switchViewMode(mode) {
      if (mode === this.viewMode) {
        return
      }
      this.viewMode = mode
      ElMessage.success({
        message: mode === 'edit' ? '已切換至編輯模式' : '已切換至預覽模式',
        offset: 100
      })
    },

    initForm() {
      if (this.question) {
        // 編輯模式：加載現有數據
        if (this.question.questionnaireData) {
          // 如果有 questionnaireData，使用它
          this.questionnaireData = {
            title: this.question.questionnaireData.title || '問卷調查',
            description: this.question.questionnaireData.description || ''
          }
        } else {
          // 否則使用 title 和 description
          this.questionnaireData = {
            title: this.question.title || '問卷調查',
            description: this.question.description || ''
          }
        }
        
        if (this.question.questions && this.question.questions.length > 0) {
          // 如果有 questions 數組，使用它
          this.questionList = this.question.questions.map((q, index) => ({
            ...q,
            id: q.id || Date.now() + index,
            description: q.description || '',
            placeholder: q.placeholder || '',
            defaultValue: q.defaultValue || '',
            validation: q.validation || [],
            randomOrder: q.randomOrder || false,
            logicRuleList: q.logicRuleList || []
          }))
        } else {
          // 否則將 question 本身作為第一題
          this.questionList = [{
            ...this.question,
            id: this.question.id || Date.now(),
            description: this.question.description || '',
            placeholder: this.question.placeholder || '',
            defaultValue: this.question.defaultValue || '',
            validation: this.question.validation || [],
            randomOrder: this.question.randomOrder || false,
            logicRuleList: this.question.logicRuleList || []
          }]
        }
        
        this.selectedQuestionId = this.questionList[0]?.id || null
      } else {
        this.resetForm()
      }
    },

    resetForm() {
      this.questionList = []
      this.questionnaireData = { title: '', description: '' }
      this.selectedQuestionId = null
      this.nextId = 1
      this.activePanels = ['questionType'] // 重置為只展開題型選擇
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
        logicRuleList: [], // 邏輯規則列表（支援多條規則）
        branchOptions: type === '5' ? [
          { text: '', action: 'continue', nextTitle: '' },
          { text: '', action: 'end', nextTitle: '' }
        ] : null
      }
      
      // 如果是填空題類型（新增的拖拽權限控制題目）
      if (type === '10') {
        question.fillBlanks = [] // 初始為空，由用戶動態新增
        question.correctAnswers = [] // 正確答案數組，與 fillBlanks 對應
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
        const oldOptionsCount = this.selectedQuestion.options.length
        this.selectedQuestion.options.push('')
        
        // 如果存在跳轉規則，為新選項添加規則
        if (this.selectedQuestion.logicRuleList && this.selectedQuestion.logicRuleList.length > 0) {
          const newRule = {
            id: `rule-${Date.now()}`,
            optionIndex: oldOptionsCount,
            jumpTarget: 'next'
          }
          this.selectedQuestion.logicRuleList.push(newRule)
          ElMessage.success({
            message: '已為新選項添加跳轉規則',
            offset: 100
          })
        }
      }
    },

    addOptionToQuestion(question) {
      const oldOptionsCount = question.options.length
      question.options.push('')
      
      // 如果存在跳轉規則，為新選項添加規則
      if (question.logicRuleList && question.logicRuleList.length > 0) {
        const newRule = {
          id: `rule-${Date.now()}`,
          optionIndex: oldOptionsCount,
          jumpTarget: 'next'
        }
        question.logicRuleList.push(newRule)
      }
    },

    addOptionAtIndex(question, index) {
      question.options.splice(index, 0, '')
      
      // 如果存在跳轉規則，為新選項添加規則並調整後續規則的索引
      if (question.logicRuleList && question.logicRuleList.length > 0) {
        // 為新選項添加規則
        const newRule = {
          id: `rule-${Date.now()}`,
          optionIndex: index,
          jumpTarget: 'next'
        }
        question.logicRuleList.splice(index, 0, newRule)
        
        // 調整後續規則的 optionIndex
        for (let i = index + 1; i < question.logicRuleList.length; i++) {
          question.logicRuleList[i].optionIndex = i
        }
      }
    },

    removeOption(index) {
      if (this.selectedQuestion && this.selectedQuestion.options.length > 2) {
        this.selectedQuestion.options.splice(index, 1)
        
        // 如果存在跳轉規則，刪除對應的規則並調整後續規則的索引
        if (this.selectedQuestion.logicRuleList && this.selectedQuestion.logicRuleList.length > 0) {
          this.selectedQuestion.logicRuleList.splice(index, 1)
          
          // 調整後續規則的 optionIndex
          for (let i = index; i < this.selectedQuestion.logicRuleList.length; i++) {
            this.selectedQuestion.logicRuleList[i].optionIndex = i
          }
        }
      }
    },

    removeOptionAtIndex(question, index) {
      if (question.options.length > 2) {
        question.options.splice(index, 1)
        
        // 如果存在跳轉規則，刪除對應的規則並調整後續規則的索引
        if (question.logicRuleList && question.logicRuleList.length > 0) {
          question.logicRuleList.splice(index, 1)
          
          // 調整後續規則的 optionIndex
          for (let i = index; i < question.logicRuleList.length; i++) {
            question.logicRuleList[i].optionIndex = i
          }
        }
      }
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
      question.correctAnswers.push('') // 新增空的答案佔位
      
      // 在 contenteditable div 中插入佔位符標籤
      let editableDiv = this.$refs.contentEditableDiv
      // 如果是數組，取第一個元素
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
        
        // 創建佔位符標籤
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
        message: '已新增填空',
        offset: 100
      })
    },

    onContentInput() {
      // 內容變化時不立即同步，避免光標跳動
      // 只在保存時才同步
    },

    onContentKeydown(e) {
      if (e.key === 'Backspace' || e.key === 'Delete') {
        const selection = window.getSelection()
        if (!selection || selection.rangeCount === 0) return
        
        const range = selection.getRangeAt(0)
        
        // 分別處理 Backspace 和 Delete
        if (e.key === 'Backspace') {
          // 查找光標前一個節點
          let prevNode = null
          if (range.startContainer.nodeType === 3) {
            // 在文本節點中
            if (range.startOffset === 0) {
              prevNode = range.startContainer.previousSibling
            }
          } else if (range.startContainer.nodeType === 1) {
            // 在元素節點中
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
          // 查找光標後一個節點
          let nextNode = null
          if (range.startContainer.nodeType === 3) {
            // 在文本節點中
            if (range.startOffset === range.startContainer.length) {
              nextNode = range.startContainer.nextSibling
            }
          } else if (range.startContainer.nodeType === 1) {
            // 在元素節點中
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
      // 先轉義 HTML 特殊字符
      html = html.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      
      // 匹配所有可能的佔位符格式（包括損壞的）
      const allPlaceholderPatterns = [
        /\{\{fillblank-(\d+)\}\}/g,  // 完整格式
        /\{fillblank-(\d+)\}\}/g,     // 缺少一個 {
        /\{\{fillblank-(\d+)\}/g,     // 缺少一個 }
        /\{fillblank-(\d+)\}/g        // 缺少兩個 {}
      ]
      
      // 收集所有找到的佔位符
      const foundPlaceholders = new Set()
      allPlaceholderPatterns.forEach(pattern => {
        let match
        while ((match = pattern.exec(html)) !== null) {
          foundPlaceholders.add(parseInt(match[1]) - 1)
        }
      })
      
      // 按順序替換所有找到的佔位符
      const sortedIndices = Array.from(foundPlaceholders).sort((a, b) => a - b)
      sortedIndices.forEach(index => {
        // 匹配這個佔位符的各種格式
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

    // 預覽模式下渲染填空題目（不含刪除按鈕）
    renderFillBlankText(question) {
      if (!question.content) return ''
      
      let html = question.content
      // 先轉義 HTML 特殊字符
      html = html.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      
      // 匹配所有可能的佔位符格式
      const allPlaceholderPatterns = [
        /\{\{fillblank-(\d+)\}\}/g,
        /\{fillblank-(\d+)\}\}/g,
        /\{\{fillblank-(\d+)\}/g,
        /\{fillblank-(\d+)\}/g
      ]
      
      // 收集所有找到的佔位符
      const foundPlaceholders = new Set()
      allPlaceholderPatterns.forEach(pattern => {
        let match
        while ((match = pattern.exec(html)) !== null) {
          foundPlaceholders.add(parseInt(match[1]) - 1)
        }
      })
      
      // 按順序替換所有找到的佔位符
      const sortedIndices = Array.from(foundPlaceholders).sort((a, b) => a - b)
      sortedIndices.forEach(index => {
        const patterns = [
          new RegExp(`\\{\\{fillblank-${index + 1}\\}\\}`, 'g'),
          new RegExp(`\\{fillblank-${index + 1}\\}\\}`, 'g'),
          new RegExp(`\\{\\{fillblank-${index + 1}\\}`, 'g'),
          new RegExp(`\\{fillblank-${index + 1}\\}`, 'g')
        ]
        
        // 預覽模式下不顯示刪除按鈕
        const underlineBlank = `<span class="underline-placeholder-preview"><span class="underline-text-preview">__________</span></span>`
        
        patterns.forEach(pattern => {
          html = html.replace(pattern, underlineBlank)
        })
      })
      
      return html
    },

    handleClose() {
      this.$emit('update:visible', false)
    },

    handleSave() {
      if (this.questionList.length === 0) {
        ElMessage.warning('請至少添加一道題目')
        return
      }
    
      // 同步填空題的 content 內容
      this.questionList.forEach(q => {
        if (q.type === '10' && q.fillBlanks && q.fillBlanks.length > 0) {
          // 重新同步 content 內容
          let editableDiv = this.$refs.contentEditableDiv
          if (Array.isArray(editableDiv) && editableDiv.length > 0) {
            editableDiv = editableDiv[0]
          }
              
          if (editableDiv) {
            let html = editableDiv.innerHTML
            const tags = editableDiv.querySelectorAll('.editable-blank-tag')
            tags.forEach((tag) => {
              const index = parseInt(tag.dataset.index) + 1
              const placeholder = `{{fillblank-${index}}}`
              html = html.replace(tag.outerHTML, placeholder)
            })
            const tempDiv = document.createElement('div')
            tempDiv.innerHTML = html
            q.content = tempDiv.textContent || tempDiv.innerText || ''
          }
        }
      })
    
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
            
        // 驗證填空題（只驗證內容是否為空）
        if (q.type === '10') {
          if (!q.content || !q.content.trim()) {
            ElMessage.warning({
              message: `第 ${i + 1} 題的題目內容不能為空`,
              offset: 100
            })
            this.selectedQuestionId = q.id
            return
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
    },
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
  z-index: 2000; /* 降低 z-index，讓 ElMessage 能正常顯示 */
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
  flex: 1;
}

.toolbar-center {
  display: flex;
  align-items: center;
  gap: 10px;
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
}

/* 預覽模式工具欄 */
.preview-toolbar {
  justify-content: center !important;
  border-bottom: none !important;
  box-shadow: none !important;
  background: transparent !important;
  padding: 16px 24px !important;
}

.view-mode-btn {
  height: 36px;
  padding: 0 18px;
  border-radius: 6px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.view-mode-btn .el-icon {
  margin-right: 6px;
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

/* 預覽模式下的佈局 */
.dialog-main.preview-mode {
  padding: 0;
  background: white;
}

.dialog-main.preview-mode .center-panel {
  width: 100%;
  max-width: 100%;
}

.dialog-main.preview-mode .questionnaire-preview {
  border-radius: 0;
  box-shadow: none;
}

.dialog-main.preview-mode .questionnaire-body {
  padding: 24px 32px;
}

/* 左側面板隱藏 */
.left-panel.hidden {
  display: none !important;
}

/* 右側面板已經有 .hidden 樣式 */

/* 左側面板 */
.left-panel {
  width: 360px;
  flex-shrink: 0;
  min-width: 340px; /* 設置最小寬度防止過度壓縮 */
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #e8eaed;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 邏輯面板展開時的左側面板樣式 */
.left-panel.logic-expanded {
  width: 450px;
  min-width: 420px;
}

.left-panel .panel-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 16px;
  font-weight: 700;
  color: #303133;
  padding: 20px 20px 16px 20px;
  background: linear-gradient(135deg, #fafafa 0%, #ffffff 100%);
  border-bottom: 1px solid #ebeef5;
}

.left-panel .panel-header .el-icon {
  font-size: 22px;
  color: #409EFF;
}

.left-panel .panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px 20px 20px;
}

/* 邏輯編輯面板內容區域 */
.left-panel .panel-content.logic-edit-panel-content {
  padding: 12px 16px 16px 16px;
  background: linear-gradient(135deg, #f8faff 0%, #ffffff 100%);
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
  padding: 16px;
  background: linear-gradient(135deg, #fafafa 0%, #ffffff 100%);
  border-radius: 12px;
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
  margin-bottom: 16px;
  padding: 8px 12px;
  background: linear-gradient(135deg, #e3f2fd 0%, #f3e5f5 100%);
  border-radius: 8px;
  letter-spacing: 1px;
  text-transform: uppercase;
  display: flex;
  align-items: center;
  gap: 8px;
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
  gap: 10px;
  padding: 16px 12px;
  background: linear-gradient(135deg, #ffffff 0%, #fafafa 100%);
  border: 2px solid #e8eaed;
  border-radius: 12px;
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
  font-size: 24px;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  z-index: 1;
  flex-shrink: 0;
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

/* 預覽模式簡化流程圖容器 */
.preview-flowchart-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 24px 32px;
  background: white;
  position: relative;
}

/* SVG 流程線層 */
.flow-connector-svg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
}

.preview-question-flow,
.preview-structured-item {
  background: linear-gradient(135deg, #ffffff 0%, #fafafa 100%);
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.25s ease;
  position: relative;
  z-index: 2;
}

.preview-structured-item:hover {
  border-color: #b3d8ff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.12);
  transform: translateY(-1px);
}

.preview-structured-item.active {
  border-color: #409EFF;
  background: linear-gradient(135deg, #ecf5ff 0%, #f0f7ff 100%);
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.2);
  border-left: 4px solid #409EFF;
}

.preview-structured-item.active .q-number {
  background: linear-gradient(135deg, #409EFF 0%, #337ecc 100%);
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.3);
}

.preview-structured-item.active .q-text {
  color: #409EFF;
  font-weight: 700;
}

.q-source-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  font-size: 12px;
  padding: 6px 10px;
  background: linear-gradient(135deg, rgba(103, 194, 58, 0.1) 0%, rgba(64, 158, 255, 0.1) 100%);
  border-radius: 6px;
  border: 1px solid rgba(103, 194, 58, 0.3);
}

.source-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 600;
}

.source-icon {
  font-size: 14px;
}

.jump-arrow {
  color: #67c23a;
  font-weight: 700;
  font-size: 16px;
  animation: bounce 1s infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(3px); }
}

.preview-question-title-wrapper {
  display: flex;
  flex-direction: column;
}

.preview-question-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px dashed #ecf5ff;
  flex-wrap: wrap;
}

.q-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  border-radius: 12px;
  background: linear-gradient(135deg, #409EFF 0%, #67c23a 100%);
  color: white;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
  padding: 0 6px;
  line-height: 1;
}

.q-text {
  flex: 1;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.q-type-tag {
  flex-shrink: 0;
}

/* 邏輯規則預覽（帶流程線） */
.preview-logic-flow-chart {
  margin-top: 12px;
  padding: 12px;
  background: linear-gradient(135deg, #f8faff 0%, #ffffff 100%);
  border-radius: 8px;
  border: 2px solid #e4e7ed;
  box-shadow: inset 0 2px 8px rgba(0, 0, 0, 0.03);
}

.logic-flow-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
  padding-bottom: 6px;
  border-bottom: 1px dashed #dcdfe6;
}

.logic-flow-title .el-icon {
  font-size: 14px;
  color: #409EFF;
}

.logic-flow-lines {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.logic-flow-line {
  display: flex;
  align-items: center;
}

.flow-line-connector {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.option-node {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409EFF 0%, #67c23a 100%);
  color: white;
  font-size: 14px;
  font-weight: 700;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
  flex-shrink: 0;
  z-index: 3;
}

.option-letter {
  display: inline-block;
}

.flow-line-path {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
}

.flow-line {
  flex: 1;
  height: 3px;
  background: linear-gradient(90deg, #409EFF 0%, #67c23a 100%);
  border-radius: 2px;
  position: relative;
}

.flow-arrow {
  font-size: 18px;
  color: #67c23a;
  font-weight: 700;
  margin-left: -4px;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.7; transform: scale(1.1); }
}

.target-node {
  flex-shrink: 0;
  min-width: 100px;
}

.target-node .el-tag {
  font-size: 12px;
  padding: 6px 12px;
  font-weight: 600;
  border-radius: 6px;
}

.preview-simple-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 6px;
  margin-left: 34px;
  color: #909399;
  font-size: 12px;
}

.preview-simple-hint .el-icon {
  font-size: 14px;
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

.question-type-tag {
  margin-left: 8px;
  font-weight: 600;
  border-radius: 4px;
  padding: 2px 8px;
  font-size: 12px;
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

/* 預覽模式下的邏輯規則流程圖 */
.logic-flowchart {
  margin-top: 20px;
  padding: 16px;
  background: linear-gradient(135deg, #f0f9ff 0%, #ffffff 100%);
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.flowchart-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px dashed #dcdfe6;
}

.flowchart-title .el-icon {
  font-size: 16px;
  color: #409EFF;
}

.flowchart-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.flow-start {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #ffffff;
  border-radius: 6px;
  border-left: 3px solid #67c23a;
}

.start-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #67c23a;
  box-shadow: 0 0 8px rgba(103, 194, 58, 0.4);
}

.start-text {
  font-size: 12px;
  font-weight: 600;
  color: #67c23a;
}

.flow-steps {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-left: 12px;
}

.flow-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.step-condition {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: #ffffff;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
  width: 100%;
}

.condition-badge {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409EFF 0%, #67c23a 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 700;
  color: white;
}

.condition-text {
  font-size: 12px;
  font-weight: 500;
  color: #606266;
  flex: 1;
}

.step-arrow {
  font-size: 16px;
  color: #409EFF;
  font-weight: 700;
  line-height: 1;
}

.step-action {
  padding: 4px 0;
}

.step-action .el-tag {
  font-size: 11px;
  padding: 4px 12px;
}

.flow-end {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #ffffff;
  border-radius: 6px;
  border-left: 3px solid #f56c6c;
  margin-top: 4px;
}

.end-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #f56c6c;
  box-shadow: 0 0 8px rgba(245, 108, 108, 0.4);
}

.end-text {
  font-size: 12px;
  font-weight: 600;
  color: #f56c6c;
}

/* 填空題目預覽模式樣式 */
.logic-preview-section {
  margin-top: 16px;
  padding: 14px;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.logic-preview-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ecf5ff;
}

.logic-preview-header .el-icon {
  font-size: 16px;
  color: #409EFF;
}

.logic-preview-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.logic-preview-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: #ffffff;
  border-radius: 6px;
  border: 1px solid #f0f0f0;
}

.logic-preview-item .option-tag {
  flex-shrink: 0;
  min-width: 80px;
}

.logic-preview-item .arrow {
  font-size: 14px;
  color: #909399;
  font-weight: 700;
}

/* 填空題目預覽模式樣式 */
.fillblank-preview-display {
  padding: 16px;
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  min-height: 80px;
}

.preview-text {
  line-height: 2.5;
  font-size: 15px;
  color: #303133;
}

.underline-placeholder-preview {
  display: inline-flex;
  align-items: center;
  position: relative;
  margin: 0 4px;
}

.underline-text-preview {
  display: inline-block;
  color: #606266;
  font-weight: 500;
  font-size: 14px;
  padding: 4px 12px 2px;
  border-bottom: 2px solid #909399;
  min-width: 100px;
  text-align: center;
  background: #f8f9fa;
  border-radius: 2px 2px 0 0;
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
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 邏輯面板展開時隱藏右側面板 */
.right-panel.hidden {
  display: none;
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

/* 邏輯編輯區域樣式 */
.logic-edit-section {
  padding: 0;
}

.logic-editor {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.logic-rules-wrapper {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rules-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 12px;
}

.logic-rule-card {
  background: #ffffff;
  border-radius: 8px;
  border: 2px solid #e4e7ed;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  margin-bottom: 8px;
}

.logic-rule-card:hover {
  border-color: #409EFF;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.15);
  transform: translateY(-2px);
}

.rule-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-bottom: 1px solid #f0f0f0;
}

.rule-index {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #303133;
}

.rule-index .el-icon {
  font-size: 14px;
  color: #409EFF;
}

.rule-card-body {
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rule-row {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.rule-label {
  font-size: 11px;
  font-weight: 600;
  color: #606266;
  min-width: 28px;
  flex-shrink: 0;
}

.rule-question-name {
  font-size: 12px;
  font-weight: 600;
  color: #303133;
  padding: 3px 6px;
  background: #f8f9fa;
  border-radius: 4px;
  border-left: 2px solid #409EFF;
}

.rule-action-label {
  font-size: 11px;
  font-weight: 600;
  color: #606266;
  min-width: 40px;
  flex-shrink: 0;
}

.option-tag {
  font-weight: 600;
  padding: 4px 10px;
  font-size: 12px;
}

.jump-select {
  flex: 1;
  min-width: 150px;
}

.jump-select :deep(.el-input__inner) {
  font-size: 12px;
  padding: 5px 8px;
}

.rule-preview {
  margin-top: 3px;
  padding: 6px;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-radius: 4px;
  border: 1px dashed #dcdfe6;
}

.rule-preview .el-tag {
  font-size: 11px;
  padding: 3px 8px;
}

.empty-rules {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px dashed #dcdfe6;
}

.no-selection-logic {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  gap: 16px;
}

.empty-tips {
  display: flex;
  justify-content: center;
}

.no-logic-hint {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  color: #606266;
  font-size: 13px;
}

.no-logic-hint .el-icon {
  font-size: 20px;
  color: #909399;
}

.hint-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.hint-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.hint-desc {
  font-size: 12px;
  color: #909399;
}

.question-type-hint {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-radius: 6px;
  border: 1px solid #e4e7ed;
}

.hint-text {
  font-size: 12px;
  color: #606266;
}

.logic-rules-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 4px;
}

.rules-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  padding-left: 8px;
  border-left: 3px solid #409EFF;
}

.header-buttons {
  display: flex;
  gap: 8px;
}

.add-rule-btn,
.remove-all-rules-btn {
  padding: 6px 12px;
  font-size: 12px;
  border-radius: 4px;
}

/* 預覽模式下的流程線樣式 */
.preview-flowchart-container {
  position: relative;
}

.preview-structured-item {
  position: relative;
  margin-left: 20px !important;
}

/* 流程連接線 - 使用 CSS 僞元素 */
.preview-structured-item::before {
  content: '↘';
  position: absolute;
  left: -18px;
  top: 50%;
  width: 18px;
  height: 18px;
  font-size: 14px;
  color: #67c23a;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  transform: translateY(-50%) rotate(0deg);
  background: transparent;
  z-index: 1;
  transition: all 0.3s ease;
}

/* 有來源的問題顯示更明顯的箭頭 */
.preview-structured-item[data-has-source="true"]::before {
  content: '⬇';
  font-size: 16px;
  color: #409EFF;
  animation: arrowBounce 1.5s infinite;
}

@keyframes arrowBounce {
  0%, 100% { transform: translateY(-50%) scale(1); opacity: 1; }
  50% { transform: translateY(-45%) scale(1.2); opacity: 0.8; }
}

/* 第一層級的問題不顯示連接線 */
.preview-structured-item[style*="margin-left: 0px"]::before,
.preview-structured-item[style*="margin-left: 0px"]::after {
  display: none;
}

/* 第二層級及以上顯示垂直連接線 */
.preview-structured-item:not([style*="margin-left: 0px"])::after {
  content: '';
  position: absolute;
  left: -18px;
  top: -20px;
  width: 2px;
  height: 20px;
  background: linear-gradient(180deg, #409EFF 0%, #67c23a 100%);
  border-radius: 2px;
  z-index: 1;
}


/* 題型提示 */
.question-type-hint {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.hint-text {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}


/* 無邏輯提示 */
.no-logic-hint {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 20px;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-radius: 10px;
  border: 2px dashed #dcdfe6;
  color: #909399;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.no-logic-hint:hover {
  border-color: #409EFF;
  background: linear-gradient(135deg, #ecf5ff 0%, #ffffff 100%);
}

.no-logic-hint .el-icon {
  font-size: 20px;
  color: #409EFF;
  flex-shrink: 0;
  margin-top: 2px;
}

.hint-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.hint-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.hint-desc {
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
}

/* 邏輯規則列表容器 */
.logic-rules-wrapper {
  display: block; /* 改為 block，讓內容自然垂直排列 */
  padding: 0 4px;
}

/* 規則卡片網格容器 - 新增一個包裝容器 */
.rules-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 12px;
}

.logic-rules-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-radius: 10px;
  border: 1px solid #e4e7ed;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  width: 100%;
}

.header-buttons {
  display: flex;
  gap: 8px;
  align-items: center;
}

.rules-title {
  font-size: 14px;
  font-weight: 700;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

.rules-title::before {
  content: '';
  width: 4px;
  height: 16px;
  background: linear-gradient(180deg, #409EFF 0%, #67c23a 100%);
  border-radius: 2px;
}

.add-rule-btn {
  background: linear-gradient(135deg, #409EFF 0%, #67c23a 100%);
  border: none;
  color: white;
  font-weight: 600;
  padding: 6px 14px;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.add-rule-btn .el-icon {
  margin-right: 6px;
}

.add-rule-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
}

.remove-all-rules-btn {
  background: linear-gradient(135deg, #F56C6C 0%, #f56c6c 100%);
  border: none;
  color: white;
  font-weight: 600;
  padding: 6px 14px;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(245, 108, 108, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.remove-all-rules-btn .el-icon {
  margin-right: 6px;
}

.remove-all-rules-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(245, 108, 108, 0.4);
}

/* 邏輯規則卡片 */
.logic-rule-card {
  background: #ffffff;
  border-radius: 10px;
  border: 2px solid #e4e7ed;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  min-width: 0; /* 允許網格壓縮 */
}

.logic-rule-card:hover {
  border-color: #409EFF;
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.15);
  transform: translateY(-2px);
}

.logic-rule-card.has-error {
  border-color: #F56C6C;
  box-shadow: 0 2px 12px rgba(245, 108, 108, 0.2);
}

.rule-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-bottom: 1px solid #f0f0f0;
}

.rule-index {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 700;
  color: #303133;
}

.rule-index .el-icon {
  font-size: 14px;
  color: #409EFF;
}

.remove-rule-btn {
  padding: 4px 8px;
  font-size: 11px;
}

.rule-card-body {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: #ffffff;
}

.rule-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rule-label {
  font-size: 12px;
  font-weight: 600;
  color: #606266;
  min-width: 40px;
  flex-shrink: 0;
}

.rule-question-name {
  font-size: 12px;
  font-weight: 600;
  color: #303133;
  flex: 1;
  padding: 4px 8px;
  background: linear-gradient(135deg, #ecf5ff 0%, #ffffff 100%);
  border-radius: 4px;
  border: 1px solid #d9ecff;
}

.rule-action-label {
  font-size: 12px;
  font-weight: 600;
  color: #606266;
  min-width: 50px;
  flex-shrink: 0;
}


/* 選項標籤樣式 */
.option-tag {
  flex: 1;
  height: auto;
  min-height: 28px;
  padding: 4px 8px;
  display: flex;
  align-items: center;
  font-weight: 600;
  font-size: 12px;
  border-radius: 4px;
}

.jump-select {
  flex: 1;
}

.jump-select :deep(.el-input__inner) {
  font-size: 12px;
  padding: 6px 8px;
  border-color: #dcdfe6;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.jump-select :deep(.el-input__inner):hover {
  border-color: #409EFF;
}

.jump-select :deep(.el-input__inner):focus {
  border-color: #409EFF;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}


.rule-preview {
  padding: 10px 12px;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-radius: 8px;
  border-top: 1px dashed #e4e7ed;
  margin-top: 4px;
  width: 100%;
}

.rule-preview .el-tag {
  font-weight: 600;
  white-space: normal;
  word-break: break-word;
  line-height: 1.5;
  padding: 6px 12px;
  height: auto;
  min-height: 32px;
  display: inline-flex;
  align-items: center;
}

/* 空規則狀態 */
.empty-rules {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px 20px;
  margin-top: 12px;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-radius: 12px;
  border: 2px dashed #dcdfe6;
}

/* 未選擇狀態 */
.no-selection-logic {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-height: 260px;
  gap: 16px;
}

.empty-tips {
  display: flex;
  gap: 8px;
}

/* 面板標題樣式 */
.panel-title-with-icon {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  padding: 4px 0;
}

.panel-title-with-icon .el-icon {
  font-size: 18px;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* 強制覆蓋 el-collapse 的默認樣式 */
.left-panel :deep(.el-collapse-item__header) {
  padding: 16px 20px !important;
  font-size: 16px !important;
  font-weight: 700 !important;
  display: flex;
  align-items: center !important;
}

.left-panel :deep(.el-collapse-item__content) {
  padding: 0 !important;
}

.left-panel :deep(.el-collapse-item) {
  border: none !important;
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
  
  /* 小屏幕下規則卡片改為單列 */
  .rules-grid {
    grid-template-columns: 1fr;
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
