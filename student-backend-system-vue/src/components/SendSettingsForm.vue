<template>
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
            <span>接收对象设置</span>
          </div>
        </template>

        <div class="receivers-section">
          <!-- 班级选择 -->
          <el-form-item label="选择班级">
            <div class="selection-item">
              <el-button 
                type="primary" 
                @click="openClassSelector"
                plain
              >
                已选择 {{ selectedClasses.length }} 个班级
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

          <!-- 学生/家长选择 -->
          <el-form-item label="选择学生/家长">
            <div class="selection-item">
              <el-button 
                type="success" 
                @click="openStudentSelector"
                plain
              >
                已选择 {{ selectedStudents.length }} 位学生/家长
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

          <!-- 教職員工選擇 -->
          <el-form-item label="选择教职员工">
            <div class="selection-item">
              <el-button 
                type="warning" 
                @click="openStaffSelector"
                plain
              >
                已选择 {{ selectedStaff.length }} 位教职员工
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <div v-if="selectedStaff.length > 0" class="selected-tags">
                <el-tag
                  v-for="staff in selectedStaff"
                  :key="staff.id"
                  closable
                  @close="removeStaff(staff)"
                  class="tag-item"
                >
                  {{ staff.name }}
                </el-tag>
              </div>
            </div>
          </el-form-item>
        </div>
      </el-card>

      <!-- 抄送設置 -->
      <el-card class="settings-card">
        <template #header>
          <div class="card-header">
            <el-icon><Message /></el-icon>
            <span>抄送设置</span>
          </div>
        </template>

        <div class="cc-section">
          <!-- 抄送教职员工 -->
          <el-form-item label="抄送教职员工">
            <div class="selection-item">
              <el-button 
                type="info" 
                @click="openCcStaffSelector"
                plain
              >
                已抄送 {{ selectedCcStaff.length }} 位教职员工
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

          <!-- 抄送学校通讯录 -->
          <el-form-item label="抄送学校通讯录">
            <div class="selection-item">
              <el-button 
                type="danger" 
                @click="openCcDirectorySelector"
                plain
              >
                已抄送 {{ selectedCcDirectory.length }} 个通讯录
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

      <!-- 發送設置 -->
      <el-card class="settings-card">
        <template #header>
          <div class="card-header">
            <el-icon><Setting /></el-icon>
            <span>发送设置</span>
          </div>
        </template>

        <div class="send-options">
          <el-form-item label="回复截止时间">
            <el-date-picker
              v-model="localFormData.replyDeadline"
              type="datetime"
              placeholder="请选择回复截止时间"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              :disabled-date="disabledDate"
            />
          </el-form-item>

          <el-form-item label="发送提醒">
            <el-switch
              v-model="sendRemind"
              active-text="开启"
              inactive-text="关闭"
            />
            <div class="form-tip">开启后将在发送前发送提醒通知</div>
          </el-form-item>
        </div>
      </el-card>

      <!-- 操作按钮 -->
      <el-form-item class="form-actions">
        <el-button @click="goToPrev">
          <el-icon><ArrowLeft /></el-icon>
          上一步：內容編輯
        </el-button>
        <el-button type="primary" @click="handleSubmit">
          <el-icon><Promotion /></el-icon>
          提交发布
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
      v-model:visible="staffSelectorVisible"
      :selected-staff="selectedStaff"
      @confirm="handleStaffSelect"
    />

    <StaffSelectorDialog
      v-model:visible="ccStaffSelectorVisible"
      :selected-staff="selectedCcStaff"
      title="选择抄送教职员工"
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
    }
  },
  emits: ['prev', 'submit'],
  data() {
    return {
      localFormData: { ...this.formData },
      sendRemind: false,
      
      classSelectorVisible: false,
      studentSelectorVisible: false,
      staffSelectorVisible: false,
      ccStaffSelectorVisible: false,
      directorySelectorVisible: false,
      
      selectedClasses: [],
      selectedStudents: [],
      selectedStaff: [],
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
            const ids = JSON.parse(receiver.receiveIds)
            const names = JSON.parse(receiver.receiveNames)
            
            ids.forEach((id, index) => {
              const item = { id: id, name: names[index] || '' }
              switch(receiver.receiveType) {
                case '1':
                  if (!this.selectedClasses.some(c => c.id === id)) {
                    this.selectedClasses.push(item)
                  }
                  break
                case '2':
                  if (!this.selectedStudents.some(s => s.id === id)) {
                    this.selectedStudents.push(item)
                  }
                  break
                case '3':
                  if (!this.selectedStaff.some(s => s.id === id)) {
                    this.selectedStaff.push(item)
                  }
                  break
              }
            })
          } catch (e) {
            console.error('解析接收对象数据失败:', e)
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
            console.error('解析抄送对象数据失败:', e)
          }
        })
      }
    },

    validate() {
      return new Promise((resolve, reject) => {
        const hasReceivers = this.selectedClasses.length > 0 || 
                           this.selectedStudents.length > 0 || 
                           this.selectedStaff.length > 0
        
        if (!hasReceivers) {
          this.$message.warning('请至少选择一个接收对象')
          reject()
          return
        }

        if (this.localFormData.replyDeadline) {
          const deadline = dayjs(this.localFormData.replyDeadline)
          const now = dayjs()
          if (deadline.isBefore(now)) {
            this.$message.warning('回复截止时间不能早于当前时间')
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
      this.selectedStaff = []
      this.selectedCcStaff = []
      this.selectedCcDirectory = []
      this.sendRemind = false
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
        receivers.push({
          receiveType: '1',
          receiveIds: JSON.stringify(this.selectedClasses.map(c => c.id)),
          receiveNames: JSON.stringify(this.selectedClasses.map(c => c.name))
        })
      }
      
      if (this.selectedStudents.length > 0) {
        receivers.push({
          receiveType: '2',
          receiveIds: JSON.stringify(this.selectedStudents.map(s => s.id)),
          receiveNames: JSON.stringify(this.selectedStudents.map(s => s.name))
        })
      }
      
      if (this.selectedStaff.length > 0) {
        receivers.push({
          receiveType: '3',
          receiveIds: JSON.stringify(this.selectedStaff.map(s => s.id)),
          receiveNames: JSON.stringify(this.selectedStaff.map(s => s.name))
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

    openStaffSelector() {
      this.staffSelectorVisible = true
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

    handleStaffSelect(staff) {
      this.selectedStaff = staff
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

    removeStaff(staff) {
      const index = this.selectedStaff.findIndex(s => s.id === staff.id)
      if (index > -1) {
        this.selectedStaff.splice(index, 1)
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

.form-container .el-form-item__label {
  font-weight: 600;
  color: #374151;
}

.settings-card {
  margin-bottom: 20px;
  border: none;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.settings-card .el-card__header {
  background: linear-gradient(135deg, #f9fafb 0%, #ffffff 100%);
  border-bottom: 1px solid #e5e7eb;
  padding: 16px 20px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #111827;
  font-size: 15px;
}

.card-header .el-icon {
  color: #3b82f6;
  font-size: 18px;
}

.selection-item {
  width: 100%;
}

.selected-tags {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px;
  background: #f9fafb;
  border-radius: 8px;
  min-height: 40px;
  align-items: center;
}

.tag-item {
  margin: 0;
}

.form-actions {
  text-align: right;
  padding-top: 24px;
  margin-top: 24px;
  border-top: 1px solid #e5e7eb;
}

.form-tip {
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
  line-height: 1.5;
}

.el-date-picker {
  width: 100%;
}

/* 響應式設計 */
@media (max-width: 768px) {
  .form-container {
    padding: 0;
  }
  
  .selected-tags {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
  
  .form-actions {
    text-align: center;
  }
  
  .form-actions .el-button {
    margin: 4px;
    width: calc(50% - 10px);
  }
}
</style>
