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
            <span>接收對象設置</span>
          </div>
        </template>
      
        <div class="receivers-section">
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
                  v-for="group in selectedStudentsDisplayGroups"
                  :key="group.key"
                  closable
                  :type="group.isDepartment ? 'warning' : undefined"
                  @close="group.isDepartment ? removeDepartmentGroup(group) : removeStudent(group.student)"
                  class="tag-item"
                  :class="{ 'dept-tag-item': group.isDepartment }"
                >
                  {{ group.isDepartment ? `${group.name}（${group.count} 人）` : (group.student?.name || '') }}
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
          <!-- 抄送教職工 -->
          <el-form-item label="抄送教職工">
            <div class="selection-item">
              <el-button 
                type="primary" 
                @click="openCcStaffSelector"
                plain
              >
                已抄送 {{ selectedCcStaff.length }} 位教職工
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <div v-if="selectedCcStaff.length > 0" class="selected-tags">
                <el-tag
                  v-for="group in selectedCcStaffDisplayGroups"
                  :key="group.key"
                  closable
                  :type="group.isDepartment ? 'warning' : undefined"
                  @close="group.isDepartment ? removeCcDepartmentGroup(group) : removeCcStaff(group.staff)"
                  class="tag-item"
                  :class="{ 'dept-tag-item': group.isDepartment }"
                >
                  {{ group.isDepartment ? `${group.name}（${group.count} 人）` : group.name }}
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

          <el-form-item label="提示回覆時間">
            <el-date-picker
              v-model="localFormData.reminderTime"
              type="date"
              placeholder="請先選擇回覆截止時間"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              :disabled-date="disabledReminderDate"
              :disabled="!localFormData.replyDeadline"
            />
          </el-form-item>
        </div>
      </el-card>

      <!-- 操作按鈕 -->
      <el-form-item class="form-actions">
        <el-button @click="goToPrev">
          <el-icon><ArrowLeft /></el-icon>
          上一步：內容編輯
        </el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          <el-icon><Promotion /></el-icon>
          提交發佈
        </el-button>
      </el-form-item>
    </el-form>

    <!-- 選擇器對話框 -->
    <StudentSelectorDialog
      v-model:visible="studentSelectorVisible"
      :selected-students="selectedStudents"
      @confirm="handleStudentSelect"
    />



    <StaffSelectorDialog
      v-model:visible="ccStaffSelectorVisible"
      :selected-staff="selectedCcStaff"
      title="選擇抄送教職工"
      @confirm="handleCcStaffSelect"
    />
  </div>
</template>

<script>
import { ElNotification } from 'element-plus'
import { ArrowDown, ArrowLeft, Promotion, User, Message, Setting } from '@element-plus/icons-vue'
import StudentSelectorDialog from './selectors/StudentSelectorDialog.vue'
import StaffSelectorDialog from './selectors/StaffSelectorDialog.vue'
import dayjs from 'dayjs'

export default {
  name: 'SendSettingsForm',
  components: {
    StudentSelectorDialog,
    StaffSelectorDialog
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
      
      studentSelectorVisible: false,
      ccStaffSelectorVisible: false,
      
      selectedStudents: [],
      selectedCcStaff: []
    }
  },
  computed: {
    publishDate() {
      return this.localFormData.createTime || new Date()
    },
    selectedStudentsDisplayGroups() {
      const deptGroups = new Map()
      const individuals = []

      this.selectedStudents.forEach(student => {
        const groupDeptId = student.type === 2
          ? (student.schoolDepartmentId ?? student.sourceDeptId)
          : student.sourceDeptId
        if (groupDeptId != null) {
          const key = `dept_${student.type || 1}_${groupDeptId}`
          if (!deptGroups.has(key)) {
            deptGroups.set(key, {
              key,
              isDepartment: true,
              name: student.sourceDeptName || '部門',
              sourceDeptId: groupDeptId,
              type: student.type || 1,
              count: 0
            })
          }
          deptGroups.get(key).count += 1
        } else {
          individuals.push({
            key: `person_${student.type || 1}_${student.parentUserId || student.id}_${student.departmentId || ''}`,
            isDepartment: false,
            name: student.name || '',
            student
          })
        }
      })

      return [...Array.from(deptGroups.values()), ...individuals]
    },
    selectedCcStaffDisplayGroups() {
      const deptGroups = new Map()
      const individuals = []

      this.selectedCcStaff.forEach(staff => {
        if (staff.sourceDeptId != null) {
          const key = `dept_${staff.type || 1}_${staff.sourceDeptId}`
          if (!deptGroups.has(key)) {
            deptGroups.set(key, {
              key,
              isDepartment: true,
              name: staff.sourceDeptName || '部門',
              sourceDeptId: staff.sourceDeptId,
              type: staff.type || 1,
              count: 0
            })
          }
          deptGroups.get(key).count += 1
        } else if (staff.name?.trim()) {
          individuals.push({
            key: `person_${staff.type || 1}_${staff.id}`,
            isDepartment: false,
            name: staff.name,
            staff
          })
        }
      })

      return [...Array.from(deptGroups.values()), ...individuals]
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
          if (receiver.receiveType !== '1' && receiver.receiveType !== '2') {
            return
          }
          try {
            const type = receiver.receiveType === '2' ? 2 : 1
            const parsed = JSON.parse(receiver.receiveData)
            const names = receiver.receiveNames || []

            const pushStudent = (parentUserId, studentId, departmentId, schoolDepartmentId, name) => {
              const item = {
                parentUserId,
                studentId: studentId || '',
                departmentId: departmentId || null,
                schoolDepartmentId: schoolDepartmentId || null,
                name: name || '',
                type
              }
              if (!this.selectedStudents.some(s =>
                s.parentUserId === parentUserId
                && (s.type || 1) === type
                && (s.departmentId || null) === (departmentId || null)
                && (s.studentId || '') === (studentId || '')
              )) {
                this.selectedStudents.push(item)
              }
            }

            if (!Array.isArray(parsed)) {
              return
            }

            parsed.forEach((entry, index) => {
              if (entry && typeof entry === 'object' && entry.parentUserId) {
                const matchedName = names[index] || ''
                pushStudent(
                  entry.parentUserId,
                  entry.studentId,
                  entry.departmentId,
                  entry.schoolDepartmentId,
                  matchedName
                )
              } else if (typeof entry === 'string') {
                pushStudent(entry, '', null, null, names[index] || '')
              }
            })
          } catch (e) {
            console.error('解析接收對象數據失敗:', e)
          }
        })
      }

      if (this.localFormData.ccs) {
        this.localFormData.ccs.forEach(cc => {
          if (cc.ccType !== '1' && cc.ccType !== '2') {
            return
          }
          try {
            const type = cc.ccType === '2' ? 2 : 1
            const parsed = JSON.parse(cc.ccData)
            const names = cc.ccNames || []

            const pushStaff = (id, name) => {
              const item = { id, name: name || '', type }
              if (!this.selectedCcStaff.some(s => s.id === id && (s.type || 1) === type)) {
                this.selectedCcStaff.push(item)
              }
            }

            if (!Array.isArray(parsed)) {
              return
            }

            parsed.forEach((id, index) => pushStaff(id, names[index]))
          } catch (e) {
            console.error('解析抄送對象數據失敗:', e)
          }
        })
      }
    },

    validate() {
      return new Promise((resolve, reject) => {
        const hasReceivers = this.selectedStudents.length > 0
        
        if (!hasReceivers) {
          ElNotification({ title: "提示", message: '請至少選擇一個接收對象', type: "warning", duration: 3000 })
          reject()
          return
        }

        if (this.localFormData.replyDeadline) {
          const deadline = dayjs(this.localFormData.replyDeadline)
          const now = dayjs()
          if (deadline.isBefore(now)) {
            ElNotification({ title: "提示", message: '回覆截止時間不能早於當前時間', type: "warning", duration: 3000 })
            reject()
            return
          }
        }

        // 驗證提示回覆時間
        if (this.localFormData.reminderTime) {
          const reminderDate = dayjs(this.localFormData.reminderTime)
          const tomorrow = dayjs().add(1, 'day').startOf('day')
          
          // 必須從明天開始（不能是今天或更早）
          if (reminderDate.isBefore(tomorrow)) {
            ElNotification({ title: "提示", message: '提示回覆時間必須從明天開始', type: "warning", duration: 3000 })
            reject()
            return
          }
          
          // 如果設置了回覆截止時間，不能晚於回覆截止時間
          if (this.localFormData.replyDeadline) {
            const deadline = dayjs(this.localFormData.replyDeadline).startOf('day')
            if (reminderDate.isAfter(deadline)) {
              ElNotification({ title: "提示", message: '提示回覆時間不能晚於回覆截止時間', type: "warning", duration: 3000 })
              reject()
              return
            }
          }
        }

        resolve()
      })
    },

    resetForm() {
      this.selectedStudents = []
      this.selectedCcStaff = []
      this.localFormData = {
        receivers: [],
        ccs: [],
        replyDeadline: null,
        reminderTime: null
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

      if (this.selectedStudents.length > 0) {
        const type1Students = this.selectedStudents.filter(s => s.type === 1 || !s.type)
        const type2Students = this.selectedStudents.filter(s => s.type === 2)

        if (type1Students.length > 0) {
          receivers.push({
            receiveType: '1',
            receiveData: JSON.stringify(
              type1Students
                .filter(s => s.parentUserId)
                .map(s => ({
                  parentUserId: s.parentUserId,
                  studentId: s.studentId || '',
                  departmentId: s.departmentId || null
                }))
            )
          })
        }
        if (type2Students.length > 0) {
          receivers.push({
            receiveType: '2',
            receiveData: JSON.stringify(
              type2Students
                .filter(s => s.parentUserId)
                .map(s => ({
                  parentUserId: s.parentUserId,
                  studentId: s.studentId || '',
                  departmentId: s.departmentId || null,
                  schoolDepartmentId: s.schoolDepartmentId || s.sourceDeptId || null
                }))
            )
          })
        }
      }
      
      const ccs = []

      if (this.selectedCcStaff.length > 0) {
        const type1Staff = this.selectedCcStaff.filter(s => s.type === 1 || !s.type)
        const type2Staff = this.selectedCcStaff.filter(s => s.type === 2)

        if (type1Staff.length > 0) {
          ccs.push({
            ccType: '1',
            ccData: JSON.stringify(type1Staff.map(s => s.id))
          })
        }
        if (type2Staff.length > 0) {
          ccs.push({
            ccType: '2',
            ccData: JSON.stringify(type2Staff.map(s => s.id))
          })
        }
      }
      
      this.localFormData.receivers = receivers
      this.localFormData.ccs = ccs
      
      Object.assign(this.formData, this.localFormData)
    },

    openStudentSelector() {
      this.studentSelectorVisible = true
    },

    openCcStaffSelector() {
      this.ccStaffSelectorVisible = true
    },

    handleStudentSelect(students) {
      this.selectedStudents = students
    },

    handleCcStaffSelect(staff) {
      this.selectedCcStaff = staff
    },

    removeStudent(student) {
      const index = this.selectedStudents.findIndex(s =>
        s.parentUserId === student.parentUserId
        && s.departmentId === student.departmentId
        && (s.studentId || '') === (student.studentId || '')
        && (s.type || 1) === (student.type || 1)
      )
      if (index > -1) {
        this.selectedStudents.splice(index, 1)
      }
    },

    removeDepartmentGroup(group) {
      this.selectedStudents = this.selectedStudents.filter(student => {
        const studentDeptId = student.type === 2
          ? (student.schoolDepartmentId ?? student.sourceDeptId)
          : student.sourceDeptId
        return studentDeptId !== group.sourceDeptId
      })
    },

    removeCcStaff(staff) {
      const index = this.selectedCcStaff.findIndex(s =>
        s.id === staff.id && (s.type || 1) === (staff.type || 1)
      )
      if (index > -1) {
        this.selectedCcStaff.splice(index, 1)
      }
    },

    removeCcDepartmentGroup(group) {
      this.selectedCcStaff = this.selectedCcStaff.filter(
        staff => !(staff.sourceDeptId === group.sourceDeptId && (staff.type || 1) === group.type)
      )
    },

    disabledDate(date) {
      return date && date.valueOf() < Date.now() - 86400000
    },

    disabledReminderDate(date) {
      // 提示回覆時間必須從明天開始（不能是今天或更早）
      const tomorrow = new Date()
      tomorrow.setDate(tomorrow.getDate() + 1)
      tomorrow.setHours(0, 0, 0, 0)
      
      if (date && date < tomorrow) {
        return true
      }
      
      // 如果設置了回覆截止時間，提示回覆時間不能晚於回覆截止時間
      if (this.localFormData.replyDeadline) {
        const deadline = new Date(this.localFormData.replyDeadline)
        deadline.setHours(0, 0, 0, 0)
        if (date && date > deadline) {
          return true
        }
      }
      
      return false
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

/* 選擇區域布局 */
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

/* 發送選項布局 */
.send-options {
  display: flex;
  flex-direction: column;
  gap: 16px;
}


/* 日期選擇器 */
.el-date-picker {
  width: 100%;
}

/* 操作按鈕區域 */
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

/* 響應式設計 */
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