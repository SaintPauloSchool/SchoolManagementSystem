<template>
  <div class="notification-system">
    <!-- 側邊欄 -->
    <aside class="sidebar" :class="{ 'collapsed': isCollapsed, 'mobile-visible': isMobileMenuOpen }">
      <div class="sidebar-header">
        <div class="logo-wrapper">
          <img src="@/logo/sp.jpg" alt="School Logo" class="logo-icon" />
          <span class="logo-text" v-show="!isCollapsed">保祿家校通</span>
        </div>
      </div>

      <nav class="sidebar-nav">
        <!-- 家校通知 -->
        <div class="nav-section">
          <div class="nav-section-title" @click="toggleSection('homeSchool')">
            <el-icon><Bell /></el-icon>
            <span v-show="!isCollapsed">家校通知</span>
            <el-icon v-show="!isCollapsed" class="expand-icon">
              <ArrowRight v-if="!expandedSections.homeSchool" />
              <ArrowDown v-else />
            </el-icon>
          </div>
          
          <ul v-show="!isCollapsed && expandedSections.homeSchool" class="nav-list nav-sublist">
            <li 
              v-for="item in menuItems" 
              :key="item.index"
              class="nav-item nav-subitem"
              :class="{ active: activeMenu === item.index }"
              @click="handleMenuSelect(item.index)"
            >
              <el-icon class="nav-icon"><component :is="item.icon" /></el-icon>
              <span class="nav-text">{{ item.title }}</span>
              <el-badge 
                v-if="item.badge" 
                :value="item.badge"
                class="nav-badge"
              />
            </li>
          </ul>
        </div>
        
        <!-- 通訊錄管理 -->
        <div class="nav-section">
          <div class="nav-section-title" @click="toggleSection('contact')">
            <el-icon><User /></el-icon>
            <span v-show="!isCollapsed">通訊錄管理</span>
            <el-icon v-show="!isCollapsed" class="expand-icon">
              <ArrowRight v-if="!expandedSections.contact" />
              <ArrowDown v-else />
            </el-icon>
          </div>
          
          <ul v-show="!isCollapsed && expandedSections.contact" class="nav-list nav-sublist">
            <li 
              class="nav-item nav-subitem"
              :class="{ active: activeMenu === '2-1' }"
              @click="handleMenuSelect('2-1')"
            >
              <el-icon class="nav-icon"><OfficeBuilding /></el-icon>
              <span class="nav-text">老師通訊錄</span>
            </li>
            <li 
              class="nav-item nav-subitem"
              :class="{ active: activeMenu === '2-2' }"
              @click="handleMenuSelect('2-2')"
            >
              <el-icon class="nav-icon"><User /></el-icon>
              <span class="nav-text">家校通訊錄</span>
            </li>
          </ul>
        </div>
        
        <!-- 學生相關功能管理（僅管理員可見） -->
        <div class="nav-section" v-if="hasUserRole">
          <div class="nav-section-title" @click="toggleSection('student')">
            <el-icon><User /></el-icon>
            <span v-show="!isCollapsed">學生相關功能管理</span>
            <el-icon v-show="!isCollapsed" class="expand-icon">
              <ArrowRight v-if="!expandedSections.student" />
              <ArrowDown v-else />
            </el-icon>
          </div>
          
          <ul v-show="!isCollapsed && expandedSections.student" class="nav-list nav-sublist">
            <li 
              class="nav-item nav-subitem"
              :class="{ active: activeMenu === '3-6' }"
              @click="handleMenuSelect('3-6')"
            >
              <el-icon class="nav-icon"><Collection /></el-icon>
              <span class="nav-text">班級對照表</span>
            </li>
            <li 
              class="nav-item nav-subitem"
              :class="{ active: activeMenu === '3-4' }"
              @click="handleMenuSelect('3-4')"
            >
              <el-icon class="nav-icon"><User /></el-icon>
              <span class="nav-text">學生資訊管理</span>
            </li>
            <li 
              class="nav-item nav-subitem"
              :class="{ active: activeMenu === '3-2' }"
              @click="handleMenuSelect('3-2')"
            >
              <el-icon class="nav-icon"><Calendar /></el-icon>
              <span class="nav-text">行事曆管理</span>
            </li>
            <li 
              class="nav-item nav-subitem"
              :class="{ active: activeMenu === '3-7' }"
              @click="handleMenuSelect('3-7')"
            >
              <el-icon class="nav-icon"><Clock /></el-icon>
              <span class="nav-text">考勤機記錄查詢</span>
            </li>
          </ul>
        </div>

        <!-- 系統管理（僅管理員可見） -->
        <div class="nav-section" v-if="hasUserRole">
          <div class="nav-section-title" @click="toggleSection('system')">
            <el-icon><Setting /></el-icon>
            <span v-show="!isCollapsed">系統管理</span>
            <el-icon v-show="!isCollapsed" class="expand-icon">
              <ArrowRight v-if="!expandedSections.system" />
              <ArrowDown v-else />
            </el-icon>
          </div>
          
          <ul v-show="!isCollapsed && expandedSections.system" class="nav-list nav-sublist">
            <li 
              class="nav-item nav-subitem"
              :class="{ active: activeMenu === '3-5' }"
              @click="handleMenuSelect('3-5')"
            >
              <el-icon class="nav-icon"><Tools /></el-icon>
              <span class="nav-text">基礎設置</span>
            </li>
            <li 
              class="nav-item nav-subitem"
              :class="{ active: activeMenu === '3-1' }"
              @click="handleMenuSelect('3-1')"
            >
              <el-icon class="nav-icon"><Warning /></el-icon>
              <span class="nav-text">查詢失敗通知</span>
            </li>
            <li 
              class="nav-item nav-subitem"
              :class="{ active: activeMenu === '3-3' }"
              @click="handleMenuSelect('3-3')"
            >
              <el-icon class="nav-icon"><Document /></el-icon>
              <span class="nav-text">定時任務日誌</span>
            </li>
            <li
              v-if="hasSuperUserRole"
              class="nav-item nav-subitem"
              :class="{ active: activeMenu === '3-8' }"
              @click="handleMenuSelect('3-8')"
            >
              <el-icon class="nav-icon"><UserFilled /></el-icon>
              <span class="nav-text">用戶角色設置</span>
            </li>
          </ul>
        </div>
      </nav>

      <!-- 收起按鈕 - 放在底部 -->
      <div class="sidebar-footer">
        <el-button
          link
          class="collapse-btn"
          @click="isMobile ? toggleMobileMenu() : toggleCollapse()"
        >
          <el-icon><Fold /></el-icon>
          <span class="collapse-text" v-show="isMobile || !isCollapsed">收起</span>
        </el-button>
      </div>
    </aside>

    <!-- 主內容區 -->
    <main class="main-content">
      <!-- 手機端頂欄：打開側邊導航 -->
      <header v-if="isMobile" class="mobile-topbar">
        <button type="button" class="mobile-menu-btn" aria-label="打開選單" @click="toggleMobileMenu">
          <el-icon :size="22"><Menu /></el-icon>
        </button>
        <img src="@/logo/sp.jpg" alt="School Logo" class="mobile-topbar-logo" />
        <span class="mobile-topbar-title">保祿家校通</span>
      </header>
      <!-- 內容區域 -->
      <div class="content-wrapper">
        <transition name="fade" mode="out-in">
          <!-- 發佈通知 -->
          <PublishNotification 
            v-if="activeMenu === '1-1'"
            @publish-success="handlePublishSuccess" 
          />
          
          <!-- 抄送我的（桌面表格） -->
          <NotificationList 
            v-else-if="activeMenu === '1-2' && !isMobile"
            :notifications="ccToMeNotifications"
            :pagination="ccPagination"
            @refresh="handleCcRefresh"
            @page-change="handleCcPageChange"
            type="ccToMe"
            ref="ccList"
          />

          <!-- 抄送我的（手機卡片） -->
          <NotificationCardList
            v-else-if="activeMenu === '1-2' && isMobile"
            :notifications="ccToMeNotifications"
            :pagination="ccPagination"
            :menu-open="isMobileMenuOpen"
            @refresh="handleCcRefresh"
            @page-change="handleCcPageChange"
            @load-more="handleCcLoadMore"
            type="ccToMe"
            ref="ccList"
          />
          
          <!-- 我發送的（桌面表格） -->
          <NotificationList 
            v-else-if="activeMenu === '1-3' && !isMobile"
            :notifications="mySendNotifications"
            :pagination="mySendPagination"
            @refresh="handleMySendRefresh"
            @page-change="handleMySendPageChange"
            type="mySend"
          />

          <!-- 我發送的（手機卡片） -->
          <NotificationCardList
            v-else-if="activeMenu === '1-3' && isMobile"
            :notifications="mySendNotifications"
            :pagination="mySendPagination"
            :menu-open="isMobileMenuOpen"
            @refresh="handleMySendRefresh"
            @page-change="handleMySendPageChange"
            @load-more="handleMySendLoadMore"
            type="mySend"
          />
          
          <!-- 老師部門通訊錄 -->
          <SchoolDepartment
            v-else-if="activeMenu === '2-1'"
          />
          
          <!-- 家校通訊錄 -->
          <HomeSchoolContacts
            v-else-if="activeMenu === '2-2'"
          />
          
          <!-- 查詢失敗通知 -->
          <FailedNotificationList
            v-else-if="activeMenu === '3-1'"
          />
          
          <!-- 行事曆管理 -->
          <CalendarEventList
            v-else-if="activeMenu === '3-2'"
          />
          
          <!-- 定時任務日誌 -->
          <SysTaskLogList
            v-else-if="activeMenu === '3-3'"
          />
          
          <!-- 學生資訊管理 -->
          <StudentMatch
            v-else-if="activeMenu === '3-4'"
          />

          <BasicSettings
            v-else-if="activeMenu === '3-5'"
          />

          <ClassSectionList
            v-else-if="activeMenu === '3-6'"
          />

          <AttendanceRecordList
            v-else-if="activeMenu === '3-7'"
          />

          <UserRoleSettings
            v-else-if="activeMenu === '3-8' && hasSuperUserRole"
          />
        </transition>
      </div>
    </main>

    <!-- 移動端遮罩層 -->
    <div v-if="isMobile && isMobileMenuOpen" class="overlay" @click="toggleMobileMenu"></div>
  </div>
</template>

<script>
import { ElNotification } from 'element-plus'
import { Bell, Promotion, Edit, Message, Fold, Menu, Setting, Document, ArrowRight, ArrowDown, User, UserFilled, OfficeBuilding, Warning, Calendar, Tools, Collection, Clock } from '@element-plus/icons-vue'
import NotificationList from './NotificationList.vue'
import NotificationCardList from './NotificationCardList.vue'
import PublishNotification from './PublishNotification.vue'
import SchoolDepartment from './SchoolDepartment.vue'
import HomeSchoolContacts from './HomeSchoolContacts.vue'
import FailedNotificationList from './FailedNotificationList.vue'
import CalendarEventList from './CalendarEventList.vue'
import SysTaskLogList from './SysTaskLogList.vue'
import StudentMatch from './StudentMatch.vue'
import BasicSettings from './BasicSettings.vue'
import ClassSectionList from './ClassSectionList.vue'
import AttendanceRecordList from './AttendanceRecordList.vue'
import UserRoleSettings from './UserRoleSettings.vue'
import request from '@/utils/request'

export default {
  name: 'SchoolNotificationSystem',
  components: {
    NotificationList,
    NotificationCardList,
    PublishNotification,
    SchoolDepartment,
    HomeSchoolContacts,
    FailedNotificationList,
    CalendarEventList,
    SysTaskLogList,
    StudentMatch,
    BasicSettings,
    ClassSectionList,
    AttendanceRecordList,
    UserRoleSettings,
    UserFilled,
    Menu
  },
  data() {
    return {
      activeMenu: this.getInitialActiveMenu(),
      ccToMeNotifications: [],
      mySendNotifications: [],
      ccPagination: {
        currentPage: 1,
        pageSize: 10,
        total: 0
      },
      mySendPagination: {
        currentPage: 1,
        pageSize: 10,
        total: 0
      },
      isCollapsed: false,
      isMobileMenuOpen: false,
      isMobile: false,
      hasUserRole: false,
      hasSuperUserRole: false,
      expandedSections: this.getInitialExpandedSections(),
      menuItems: [
        { index: '1-1', title: '發佈通知', icon: 'Edit' },
        { index: '1-2', title: '抄送我的', icon: 'Message'},
        { index: '1-3', title: '我發送的', icon: 'Promotion' }
      ]
    }
  },
  mounted() {
    this.checkScreenSize()
    window.addEventListener('resize', this.handleResize)
    // 根據當前激活的菜單加載對應數據
    this.loadInitialData()
    this.checkUserRoleStatus()
    this.checkPendingNotice()
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize)
  },
  methods: {
    getInitialActiveMenu() {
      const savedMenu = sessionStorage.getItem('activeMenu')
      if (savedMenu === '3-5') {
        return '1-2'
      }
      return savedMenu || '1-2'
    },
    
    saveActiveMenu(menu) {
      // 保存當前菜單到 sessionStorage
      sessionStorage.setItem('activeMenu', menu)
    },
    
    getInitialExpandedSections() {
      const savedMenu = this.getInitialActiveMenu()
      const studentMenus = ['3-2', '3-4', '3-6', '3-7']
      const systemMenus = ['3-1', '3-3', '3-5', '3-8']
      return {
        homeSchool: savedMenu.startsWith('1-'),
        contact: savedMenu.startsWith('2-'),
        student: studentMenus.includes(savedMenu),
        system: systemMenus.includes(savedMenu)
      }
    },

    expandSectionForMenu(menu) {
      if (menu.startsWith('1-')) {
        this.expandedSections.homeSchool = true
      } else if (menu.startsWith('2-')) {
        this.expandedSections.contact = true
      } else if (['3-2', '3-4', '3-6', '3-7'].includes(menu)) {
        this.expandedSections.student = true
      } else if (menu.startsWith('3-')) {
        this.expandedSections.system = true
      }
    },
    
    checkPendingNotice() {
      const pendingNoticeId = sessionStorage.getItem('pendingNoticeId');
      if (pendingNoticeId) {
        sessionStorage.removeItem('pendingNoticeId');
        
        // 切換到抄送我的並加載背後列表數據
        this.handleMenuSelect('1-2');
        this.expandedSections.homeSchool = true;
        
        // 使用 nextTick 確保 NotificationList 元件已渲染
        this.$nextTick(() => {
          setTimeout(() => {
            if (this.$refs.ccList) {
              this.$refs.ccList.viewNotification({ notificationId: pendingNoticeId });
            }
          }, 300); // 稍微延遲確保元件完全掛載
        });
      }
    },
    
    loadInitialData() {
      // 根據當前激活的菜單加載對應的數據
      if (this.activeMenu === '1-2') {
        this.handleCcRefresh()
      } else if (this.activeMenu === '1-3') {
        this.handleMySendRefresh()
      }
      // 其他菜單無需加載數據
    },
    async checkUserRoleStatus() {
      try {
        const res = await request({ url: '/system/userRole/checkCurrentUser', method: 'get' })
        if (res.code === 200 || res.code === 0) {
          const data = res.data
          if (typeof data === 'boolean') {
            this.hasUserRole = data
            this.hasSuperUserRole = false
          } else if (data && typeof data === 'object') {
            this.hasUserRole = data.hasUserRole === true
            this.hasSuperUserRole = data.hasSuperUserRole === true
          } else {
            this.hasUserRole = false
            this.hasSuperUserRole = false
          }
          const userRoleMenus = ['3-1', '3-2', '3-3', '3-4', '3-5', '3-6', '3-7', '3-8']
          if (!this.hasUserRole && userRoleMenus.includes(this.activeMenu)) {
            this.handleMenuSelect('1-2')
          }
          if (this.activeMenu === '3-8' && !this.hasSuperUserRole) {
            this.handleMenuSelect('1-2')
          }
        }
      } catch (e) {
        console.error('用戶角色查詢失敗:', e)
      }
    },
    
    checkScreenSize() {
      this.isMobile = window.innerWidth <= 768
      if (!this.isMobile) {
        this.isMobileMenuOpen = false
      }
    },
    
    handleResize() {
      const wasMobile = this.isMobile
      this.checkScreenSize()
      if (wasMobile === this.isMobile) {
        return
      }
      // 桌面/手機切換時重置為第一頁，避免累加數據與表格分頁錯亂
      if (this.activeMenu === '1-2') {
        this.handleCcRefresh()
      } else if (this.activeMenu === '1-3') {
        this.handleMySendRefresh()
      }
    },
    
    toggleCollapse() {
      this.isCollapsed = !this.isCollapsed
    },
    
    toggleMobileMenu() {
      this.isMobileMenuOpen = !this.isMobileMenuOpen
    },
    
    toggleSection(sectionName) {
      if (this.isCollapsed) {
        this.isCollapsed = false
      }
      this.expandedSections[sectionName] = !this.expandedSections[sectionName]
    },
    
    handleMenuSelect(index) {
      this.activeMenu = index
      this.saveActiveMenu(index) // 保存菜單選擇
      this.expandSectionForMenu(index)
      if (this.isMobile) {
        this.isMobileMenuOpen = false
      }
      if (index === '1-1') {
        // 發佈通知，無需加載數據
      } else if (index === '1-2') {
        this.handleCcRefresh()
      } else if (index === '1-3') {
        this.handleMySendRefresh()
      } else if (index === '2-1') {
        // 老師通訊錄，無需加載數據
      } else if (index === '2-2') {
        // 家校通訊錄，無需加載數據
      } else if (index === '3-1') {
        // 查詢失敗通知，無需加載數據
      } else if (index === '3-2') {
        // 行事曆管理，無需加載數據
      } else if (index === '3-3') {
        // 定時任務日誌，無需加載數據
      }
    },

    handleCcRefresh() {
      this.ccPagination.currentPage = 1
      this.loadCcToMeNotifications({ pageNum: 1, pageSize: this.ccPagination.pageSize })
    },

    handleMySendRefresh() {
      this.mySendPagination.currentPage = 1
      this.loadMySendNotifications({ pageNum: 1, pageSize: this.mySendPagination.pageSize })
    },

    async loadCcToMeNotifications(params = {}) {
      const append = params.append === true
      try {
        const pageNum = params.pageNum || this.ccPagination.currentPage
        const pageSize = params.pageSize || this.ccPagination.pageSize
        const response = await request({
          url: '/system/notification/ccToMe',
          method: 'get',
          params: {
            pageNum,
            pageSize,
            publishDate: params.publishDate || ''
          }
        })
        
        if (response.code === 200 || response.code === 0) {
          const rows = response.rows || []
          this.ccToMeNotifications = append
              ? [...this.ccToMeNotifications, ...rows]
              : rows
          this.ccPagination.total = response.total || 0
          this.ccPagination.currentPage = pageNum
          this.ccPagination.pageSize = pageSize
        }
      } catch (error) {
        console.error('加載失敗:', error)
        ElNotification({ title: "操作失敗", message: '數據加載失敗', type: "error", duration: 4000 })
      }
    },

    async loadMySendNotifications(params = {}) {
      const append = params.append === true
      try {
        const pageNum = params.pageNum || this.mySendPagination.currentPage
        const pageSize = params.pageSize || this.mySendPagination.pageSize
        const response = await request({
          url: '/system/notification/mySend',
          method: 'get',
          params: {
            pageNum,
            pageSize,
            publishDate: params.publishDate || ''
          }
        })
        
        if (response.code === 200 || response.code === 0) {
          const rows = response.rows || []
          this.mySendNotifications = append
              ? [...this.mySendNotifications, ...rows]
              : rows
          this.mySendPagination.total = response.total || 0
          this.mySendPagination.currentPage = pageNum
          this.mySendPagination.pageSize = pageSize
        }
      } catch (error) {
        console.error('加載失敗:', error)
        ElNotification({ title: "操作失敗", message: '數據加載失敗', type: "error", duration: 4000 })
      }
    },

    handleCcPageChange({ pageNum, pageSize, publishDate }) {
      this.ccPagination.currentPage = pageNum
      this.ccPagination.pageSize = pageSize
      this.loadCcToMeNotifications({ pageNum, pageSize, publishDate, append: false })
    },

    handleMySendPageChange({ pageNum, pageSize, publishDate }) {
      this.mySendPagination.currentPage = pageNum
      this.mySendPagination.pageSize = pageSize
      this.loadMySendNotifications({ pageNum, pageSize, publishDate, append: false })
    },

    async handleCcLoadMore({ pageNum, pageSize, publishDate, done }) {
      try {
        await this.loadCcToMeNotifications({ pageNum, pageSize, publishDate, append: true })
      } finally {
        if (typeof done === 'function') {
          done()
        }
      }
    },

    async handleMySendLoadMore({ pageNum, pageSize, publishDate, done }) {
      try {
        await this.loadMySendNotifications({ pageNum, pageSize, publishDate, append: true })
      } finally {
        if (typeof done === 'function') {
          done()
        }
      }
    },

    handlePublishSuccess() {
      this.activeMenu = '1-3'
      this.handleMySendRefresh()
    }
  }
}
</script>

<style scoped>
.notification-system {
  display: flex;
  height: 100vh;
  background: linear-gradient(135deg, #f0f4ff 0%, #e8f0fe 100%);
  overflow: hidden;
}

/* 側邊欄樣式（對齊 LeaveManagementSystem） */
.sidebar {
  width: 220px;
  background-color: #3b82f6;
  color: #fff;
  display: flex;
  flex-direction: column;
  height: 100%;
  transition: width 0.3s ease;
  flex-shrink: 0;
  position: relative;
  z-index: 100;
  box-shadow: 2px 0 10px rgba(0, 0, 0, 0.15);
  overflow: hidden;
}

.sidebar.collapsed {
  width: 64px;
}

.sidebar-header {
  height: 68px;
  min-height: 68px;
  padding: 0 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.15);
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  overflow: hidden;
  background: transparent;
  gap: 0;
}

.sidebar.collapsed .sidebar-header {
  height: 64px;
  min-height: 64px;
  padding: 0;
  gap: 0;
}

.sidebar-footer {
  height: 72px;
  padding: 0;
  border-top: 1px solid rgba(255, 255, 255, 0.15);
  display: flex;
  justify-content: center;
  align-items: center;
  background: transparent;
  margin-top: auto;
  box-sizing: border-box;
  width: 100%;
}

.logo-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s ease;
  width: 100%;
}

.collapse-btn {
  color: rgba(255, 255, 255, 0.9) !important;
  font-size: 13px !important;
  font-weight: 500 !important;
  transition: all 0.2s ease;
  padding: 8px 24px !important;
  border-radius: 6px !important;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.1) !important;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  opacity: 1;
  height: auto !important;
}

.collapse-btn .el-icon {
  font-size: 16px;
  width: 16px;
  height: 16px;
  transition: none;
}

.collapse-btn:hover {
  color: #fff !important;
  background: rgba(255, 255, 255, 0.2) !important;
  border-color: rgba(255, 255, 255, 0.1) !important;
  opacity: 1;
  transform: none;
  box-shadow: none;
}

.sidebar.collapsed .collapse-btn {
  padding: 0 !important;
  width: 36px;
  height: 36px !important;
  margin-top: 0;
}

.sidebar.collapsed .collapse-btn:hover {
  background: rgba(255, 255, 255, 0.2) !important;
  transform: none;
}

.sidebar.collapsed .collapse-btn .collapse-text {
  display: none;
}

.logo-icon {
  width: 34px;
  height: 34px;
  object-fit: contain;
  flex-shrink: 0;
  filter: none;
  transition: all 0.3s ease;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.1);
  padding: 3px;
  box-sizing: border-box;
}

.sidebar.collapsed .logo-icon {
  margin: 0 auto;
  transform: none;
  filter: none;
}

.logo-text {
  font-size: 15px;
  font-weight: 650;
  white-space: nowrap;
  color: #fff;
  text-align: left;
  letter-spacing: 0.3px;
  text-shadow: none;
  transition: all 0.3s ease;
  opacity: 1;
  line-height: 1.2;
}

.sidebar.collapsed .logo-text {
  opacity: 0;
  width: 0;
  overflow: hidden;
  transform: none;
}

/* 導航欄樣式（對齊 LeaveManagementSystem） */
.sidebar-nav {
  flex: 1;
  padding: 15px 0;
  overflow-y: auto;
  overflow-x: hidden;
  background: transparent;
  display: flex;
  flex-direction: column;
  gap: 0;
  width: 100%;
  box-sizing: border-box;
}

.sidebar-nav::-webkit-scrollbar {
  width: 0px;
  display: none;
}

.sidebar-nav {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.nav-section {
  margin-bottom: 0;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.nav-section:last-child {
  margin-bottom: 0;
}

.nav-section-title {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  margin-bottom: 0;
  font-size: 14px;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.85);
  cursor: pointer;
  border-radius: 0;
  transition: all 0.2s ease;
  user-select: none;
  position: relative;
  letter-spacing: normal;
  text-transform: none;
  white-space: nowrap;
}

.nav-section-title:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

.nav-section-title .el-icon {
  font-size: 16px;
  flex-shrink: 0;
  width: 16px;
  height: 16px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.expand-icon {
  margin-left: auto;
  font-size: 12px;
  transition: none;
  opacity: 0.7;
}

.nav-section-title:hover .expand-icon {
  opacity: 0.7;
  transform: none;
}

.nav-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  margin: 2px 6px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: rgba(255, 255, 255, 0.85);
  position: relative;
  font-size: 13px;
  border: none;
  font-weight: 400;
  box-sizing: border-box;
  overflow: hidden;
  min-width: 0;
}

.nav-item::before {
  display: none;
}

.nav-item:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
  border-color: transparent;
  padding-left: 12px;
}

.nav-item:hover::before {
  display: none;
}

.nav-item.active {
  color: #fff;
  background: rgba(255, 255, 255, 0.15);
  box-shadow: none;
  font-weight: 600;
  border-color: transparent;
  padding-left: 12px;
}

.nav-item.active::before {
  display: none;
}

.nav-icon {
  font-size: 14px;
  flex-shrink: 0;
  width: 14px;
  height: 14px;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: none;
}

.nav-item:hover .nav-icon,
.nav-item.active .nav-icon {
  transform: none;
  filter: none;
}

.nav-text {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  letter-spacing: normal;
}

.nav-badge {
  position: absolute;
  right: 12px;
  transform: scale(0.9);
  z-index: 1;
}

.nav-sublist {
  margin: 6px 12px;
  padding: 6px 0;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  display: flex;
  flex-direction: column;
  gap: 0;
  max-width: calc(100% - 24px);
  box-sizing: border-box;
  overflow: hidden;
}

.nav-sublist .nav-item {
  margin: 2px 6px;
  padding: 8px 12px;
  font-size: 13px;
  border-radius: 6px;
}

.nav-sublist .nav-item::before {
  display: none;
}

.nav-sublist .nav-item:hover,
.nav-sublist .nav-item.active {
  padding-left: 12px;
}

.nav-item-placeholder {
  padding: 10px 14px;
  color: rgba(255, 255, 255, 0.4);
  font-size: 13px;
  font-style: italic;
  text-align: center;
  letter-spacing: 0.5px;
}

.sidebar.collapsed .nav-section-title {
  justify-content: center;
  padding: 14px 0;
}

.sidebar.collapsed .nav-section-title span,
.sidebar.collapsed .expand-icon {
  display: none;
}

/* 主內容區 */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.04);
}

.content-wrapper {
  flex: 1;
  padding: 28px 32px;
  overflow-y: auto;
  overflow-x: hidden;
  background: transparent;
}

.content-wrapper::-webkit-scrollbar {
  width: 8px;
}

.content-wrapper::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 6px;
}

.content-wrapper::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.15);
}



.mobile-topbar {
  display: none;
}

.overlay {
  display: none;
}

/* 響應式設計 */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 300;
    width: min(220px, 82vw);
    transform: translateX(-105%);
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.25, 1);
  }

  .sidebar.mobile-visible {
    transform: translateX(0);
  }

  .sidebar.collapsed {
    width: min(220px, 82vw);
  }

  .main-content {
    margin-left: 0;
  }

  .mobile-topbar {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-shrink: 0;
    height: 52px;
    padding: 0 14px;
    background: #3b82f6;
    color: #fff;
    box-shadow: 0 2px 8px rgba(59, 130, 246, 0.25);
    z-index: 50;
  }

  .mobile-menu-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    border: none;
    border-radius: 10px;
    background: rgba(255, 255, 255, 0.12);
    color: #fff;
    cursor: pointer;
  }

  .mobile-menu-btn:active {
    background: rgba(255, 255, 255, 0.22);
  }

  .mobile-topbar-logo {
    width: 32px;
    height: 32px;
    border-radius: 8px;
    object-fit: cover;
    flex-shrink: 0;
    background: #fff;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.12);
  }

  .mobile-topbar-title {
    font-size: 15px;
    font-weight: 600;
    letter-spacing: 0.02em;
  }

  .content-wrapper {
    padding: 16px;
  }

  .overlay {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(15, 23, 42, 0.45);
    z-index: 250;
  }
}

/* 動畫效果 */
.fade-enter-active,
.fade-leave-active {
  transition: all 0.35s cubic-bezier(0.4, 0, 0.25, 1);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(15px);
}
</style>
