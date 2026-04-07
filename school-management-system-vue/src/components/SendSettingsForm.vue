1<template>
  <div class="send-settings-form">
    <el-form
      ref="formRef"
      :model="localFormData"
      label-width="120px"
      class="form-container"
    >
      <!-- 接收對象設置 -->
      <el-card class="settings-card">
        <template #header>
          <div class="card-header">
            <el-icon><User /></el-icon>
            <span>接收對象設置</span>
          </div>
        </template>
      
        <div class="receivers-section">
          <!-- 班級選擇 -->
          <el-form-item label="選擇班級">
            <div class="selection-item">
              <el-button 
                type="primary" 
                @click="openClassSelector"
                plain
              >
                已選擇 {{ selectedClasses.length }} 個班級
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <div v-if="selectedClasses.length > 0" class="selected-tags">
                <el-tag
                  v-for="cls in selectedClasses"
                  :key="cls.id"
                  closable
                  @close="removeClass(cls)"
                  class="tag-item"
                >
                  {{ cls.name }}
                </el-tag>
              </div>
            </div>
          </el-form-item>
      
          <!-- 學生/家長選擇 -->
          <el-form-item label="選擇學生/家長">
            <div class="selection-item">
              <el-button 
                type="primary" 
                @click="openStudentSelector"
                plain
              >
                已選擇 {{ selectedStudents.length }} 位學生/家長
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <div v-if="selectedStudents.length > 0" class="selected-tags">
                <el-tag
                  v-for="student in selectedStudents"
                  :key="student.id"
                  closable
                  @close="removeStudent(student)"
                  class="tag-item"
                >
                  {{ student.name }}
                </el-tag>
              </div>
            </div>
          </el-form-item>
      

        </div>
      </el-card>
      <el-card class="settings-card">
        <template #header>
          <div class="card-header">
            <el-icon><Message /></el-icon>
            <span>抄送設置</span>
          </div>
        </template>

        <div class="cc-section">
          <!-- 抄送教職員工 -->
          <el-form-item label="抄送教職員工">
            <div class="selection-item">
              <el-button 
                type="primary" 
                @click="openCcStaffSelector"
                plain
              >
                已抄送 {{ selectedCcStaff.length }} 位教職員工
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <div v-if="selectedCcStaff.length > 0" class="selected-tags">
                <el-tag
                  v-for="staff in selectedCcStaff"
                  :key="staff.id"
                  closable
                  @close="removeCcStaff(staff)"
                  class="tag-item"
                >
                  {{ staff.name }}
                </el-tag>
              </div>
            </div>
          </el-form-item>

          <!-- 抄送學校通訊錄 -->
          <el-form-item label="抄送學校通訊錄">
            <div class="selection-item">
              <el-button 
                type="primary" 
                @click="openCcDirectorySelector"
                plain
              >
                已抄送 {{ selectedCcDirectory.length }} 個通訊錄
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <div v-if="selectedCcDirectory.length > 0" class="selected-tags">
                <el-tag
                  v-for="dir in selectedCcDirectory"
                  :key="dir.id"
                  closable
                  @close="removeCcDirectory(dir)"
                  class="tag-item"
                >
                  {{ dir.name }}
                </el-tag>
              </div>
            </div>
          </el-form-item>
        </div>
      </el-card>

      <!-- 发送设置 -->
      <el-card class="settings-card">
        <template #header>
          <div class="card-header">
            <el-icon><Setting /></el-icon>
            <span>發送設置</span>
          </div>
        </template>

        <div class="send-options">
          <el-form-item label="回覆截止時間">
            <el-date-picker
              v-model="localFormData.replyDeadline"
              type="datetime"
              placeholder="請選擇回覆截止時間"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              :disabled-date="disabledDate"
            />
          </el-form-item>
        </div>
      </el-card>

      <!-- 操作按钮 -->
      <el-form-item class="form-actions">
        <el-button @click="goToPrev">
          <el-icon><ArrowLeft /></el-icon>
          上一步：內容編輯
        </el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          <el-icon><Promotion /></el-icon>
          提交發布
        </el-button>
      </el-form-item>
    </el-form>

    <!-- 选择器对话框 -->
    <ClassSelectorDialog
      v-model:visible="classSelectorVisible"
      :selected-classes="selectedClasses"
      @confirm="handleClassSelect"
    />

    <StudentSelectorDialog
      v-model:visible="studentSelectorVisible"
      :selected-students="selectedStudents"
      @confirm="handleStudentSelect"
    />



    <StaffSelectorDialog
      v-model:visible="ccStaffSelectorVisible"
      :selected-staff="selectedCcStaff"
      title="選擇抄送教職員工"
      @confirm="handleCcStaffSelect"
    />

    <DirectorySelectorDialog
      v-model:visible="directorySelectorVisible"
      :selected-directories="selectedCcDirectory"
      @confirm="handleDirectorySelect"
    />
  </div>
</template>

<script>
import { ArrowDown, ArrowLeft, Promotion, User, Message, Setting } from '@element-plus/icons-vue'
import ClassSelectorDialog from './selectors/ClassSelectorDialog.vue'
import StudentSelectorDialog from './selectors/StudentSelectorDialog.vue'
import StaffSelectorDialog from './selectors/StaffSelectorDialog.vue'
import DirectorySelectorDialog from './selectors/DirectorySelectorDialog.vue'
import dayjs from 'dayjs'

export default {
  name: 'SendSettingsForm',
  components: {
    ClassSelectorDialog,
    StudentSelectorDialog,
    StaffSelectorDialog,
    DirectorySelectorDialog
  },
  props: {
    formData: {
      type: Object,
      required: true
    },
    submitting: {
      type: Boolean,
      default: false
    }
  },
  emits: ['prev', 'submit'],
  data() {
    return {
      localFormData: { ...this.formData },
      
      classSelectorVisible: false,
      studentSelectorVisible: false,
      ccStaffSelectorVisible: false,
      directorySelectorVisible: false,
      
      selectedClasses: [],
      selectedStudents: [],
      selectedCcStaff: [],
      selectedCcDirectory: []
    }
  },
  watch: {
    formData: {
      handler(newVal) {
        this.localFormData = { ...newVal }
        this.initSelectedData()
      },
      deep: true
    }
  },
  mounted() {
    this.initSelectedData()
  },
  methods: {
    initSelectedData() {
      if (this.localFormData.receivers) {
        this.localFormData.receivers.forEach(receiver => {
          try {
            if (receiver.receiveType === '1') {
               if (receiver.receiveData) {
                  const dataArr = JSON.parse(receiver.receiveData);
                  dataArr.forEach(group => {
                     const ids = group.receive_ids || [];
                     const names = group.receive_names || [];
                     const type = group.type || 1;
                     ids.forEach((id, index) => {
                        const item = { id: id, name: names[index] || '', type: type };
                        if (!this.selectedClasses.some(c => c.id === id)) {
                           this.selectedClasses.push(item);
                        }
                     });
                  });
               }
            } else if (receiver.receiveType === '2') {
               if (receiver.receiveData) {
                  const dataArr = JSON.parse(receiver.receiveData);
                  dataArr.forEach(group => {
                     const ids = group.receive_ids || [];
                     const names = group.receive_names || [];
                     ids.forEach((id, index) => {
                       const item = { id: id, name: names[index] || '' }
                       if (!this.selectedStudents.some(s => s.id === id)) {
                         this.selectedStudents.push(item)
                       }
                     });
                  });
               }
            }
          } catch (e) {
            console.error('解析接收對象數據失敗:', e)
          }
        })
      }

      if (this.localFormData.ccs) {
        this.localFormData.ccs.forEach(cc => {
          try {
            const ids = JSON.parse(cc.ccIds)
            const names = JSON.parse(cc.ccNames)
            
            ids.forEach((id, index) => {
              const item = { id: id, name: names[index] || '' }
              switch(cc.ccType) {
                case '1':
                  if (!this.selectedCcStaff.some(s => s.id === id)) {
                    this.selectedCcStaff.push(item)
                  }
                  break
                case '2':
                  if (!this.selectedCcDirectory.some(d => d.id === id)) {
                    this.selectedCcDirectory.push(item)
                  }
                  break
              }
            })
          } catch (e) {
            console.error('解析抄送對象數據失敗:', e)
          }
        })
      }
    },

    validate() {
      return new Promise((resolve, reject) => {
        const hasReceivers = this.selectedClasses.length > 0 || 
                           this.selectedStudents.length > 0
        
        if (!hasReceivers) {
          this.$message.warning('請至少選擇一個接收對象')
          reject()
          return
        }

        if (this.localFormData.replyDeadline) {
          const deadline = dayjs(this.localFormData.replyDeadline)
          const now = dayjs()
          if (deadline.isBefore(now)) {
            this.$message.warning('回覆截止時間不能早於當前時間')
            reject()
            return
          }
        }

        resolve()
      })
    },

    resetForm() {
      this.selectedClasses = []
      this.selectedStudents = []
      this.selectedCcStaff = []
      this.selectedCcDirectory = []
      this.localFormData = {
        receivers: [],
        ccs: [],
        replyDeadline: null
      }
    },

    goToPrev() {
      this.syncData()
      this.$emit('prev')
    },

    handleSubmit() {
      this.validate().then(() => {
        this.syncData()
        this.$emit('submit')
      }).catch(() => {})
    },

    syncData() {
      const receivers = []
      
      if (this.selectedClasses.length > 0) {
        const type1Classes = this.selectedClasses.filter(c => c.type === 1 || !c.type);
        const type2Classes = this.selectedClasses.filter(c => c.type === 2);
        
        const receiveDataPayload = [];
        if (type1Classes.length > 0) {
           receiveDataPayload.push({
               receive_ids: type1Classes.map(c => c.id),
               type: 1,
               receive_names: type1Classes.map(c => c.name)
           });
        }
        if (type2Classes.length > 0) {
           receiveDataPayload.push({
               receive_ids: type2Classes.map(c => c.id),
               type: 2,
               receive_names: type2Classes.map(c => c.name)
           });
        }

        receivers.push({
          receiveType: '1',
          receiveData: JSON.stringify(receiveDataPayload)
        })
      }
      
      if (this.selectedStudents.length > 0) {
        const studentPayload = [{
          receive_ids: this.selectedStudents.map(s => s.id),
          receive_names: this.selectedStudents.map(s => s.name)
        }];
        receivers.push({
          receiveType: '2',
          receiveData: JSON.stringify(studentPayload)
        })
      }
      
      const ccs = []
      
      if (this.selectedCcStaff.length > 0) {
        ccs.push({
          ccType: '1',
          ccIds: JSON.stringify(this.selectedCcStaff.map(s => s.id)),
          ccNames: JSON.stringify(this.selectedCcStaff.map(s => s.name))
        })
      }
      
      if (this.selectedCcDirectory.length > 0) {
        ccs.push({
          ccType: '2',
          ccIds: JSON.stringify(this.selectedCcDirectory.map(d => d.id)),
          ccNames: JSON.stringify(this.selectedCcDirectory.map(d => d.name))
        })
      }
      
      this.localFormData.receivers = receivers
      this.localFormData.ccs = ccs
      
      Object.assign(this.formData, this.localFormData)
    },

    openClassSelector() {
      this.classSelectorVisible = true
    },

    openStudentSelector() {
      this.studentSelectorVisible = true
    },

    openCcStaffSelector() {
      this.ccStaffSelectorVisible = true
    },

    openCcDirectorySelector() {
      this.directorySelectorVisible = true
    },

    handleClassSelect(classes) {
      this.selectedClasses = classes
    },

    handleStudentSelect(students) {
      this.selectedStudents = students
    },

    handleCcStaffSelect(staff) {
      this.selectedCcStaff = staff
    },

    handleDirectorySelect(directories) {
      this.selectedCcDirectory = directories
    },

    removeClass(cls) {
      const index = this.selectedClasses.findIndex(c => c.id === cls.id)
      if (index > -1) {
        this.selectedClasses.splice(index, 1)
      }
    },

    removeStudent(student) {
      const index = this.selectedStudents.findIndex(s => s.id === student.id)
      if (index > -1) {
        this.selectedStudents.splice(index, 1)
      }
    },

    removeCcStaff(staff) {
      const index = this.selectedCcStaff.findIndex(s => s.id === staff.id)
      if (index > -1) {
        this.selectedCcStaff.splice(index, 1)
      }
    },

    removeCcDirectory(dir) {
      const index = this.selectedCcDirectory.findIndex(d => d.id === dir.id)
      if (index > -1) {
        this.selectedCcDirectory.splice(index, 1)
      }
    },

    disabledDate(date) {
      return date && date.valueOf() < Date.now() - 86400000
    }
  }
}
</script>

<style scoped>
.send-settings-form {
  width: 100%;
}

.form-container .el-form-item {
  margin-bottom: 24px;
}
/* 卡片容器 */
.settings-card {
  margin-bottom: 20px;
}

/* 选择区域布局 */
.receivers-section,
.cc-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.selection-item {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.selected-tags {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.tag-item {
  margin: 4px 0;
}

/* 发送选项布局 */
.send-options {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 表单提示文字 */
.form-tip {
  font-size: 12px;
  color: #6b7280;
  margin-top: 6px;
  line-height: 1.5;
}

/* 日期选择器 */
.el-date-picker {
  width: 100%;
}

/* 操作按钮区域 */
.form-actions {
  text-align: right;
  padding-top: 20px;
  margin-top: 20px;
  border-top: 1px solid #e5e7eb;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.form-actions .el-button {
  min-width: 120px;
  height: 38px;
  font-weight: 500;
  font-size: 14px;
  transition: all 0.2s ease;
}

.form-actions .el-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .form-container {
    padding: 0;
  }
  
  .receivers-section,
  .cc-section {
    gap: 12px;
  }
  
  .selected-tags {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
  }
  
  .form-actions {
    text-align: center;
    flex-direction: column;
    gap: 10px;
  }
  
  .form-actions .el-button {
    width: 100%;
    margin: 0;
  }
  
  .selection-item .el-button {
    width: 100%;
  }
}

</style>