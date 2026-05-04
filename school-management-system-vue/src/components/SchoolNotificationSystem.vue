<template>
  <div class="notification-system">
    <!-- 側邊欄 -->
    <aside class="sidebar" :class="{ 'collapsed': isCollapsed, 'mobile-visible': isMobileMenuOpen }">
      <div class="sidebar-header">
        <div class="logo-wrapper">
          <img src="@/logo/sp.jpg" alt="School Logo" class="logo-icon" />
          <span class="logo-text" v-show="!isCollapsed">校園管理系統</span>
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
        
        <!-- 占位大類 1 -->
        <div class="nav-section">
          <div class="nav-section-title" @click="toggleSection('system')">
            <el-icon><Setting /></el-icon>
            <span v-show="!isCollapsed">系統管理</span>
            <el-icon v-show="!isCollapsed" class="expand-icon">
              <ArrowRight v-if="!expandedSections.system" />
              <ArrowDown v-else />
            </el-icon>
          </div>
          
          <ul v-show="!isCollapsed && expandedSections.system" class="nav-list nav-sublist">
            <li class="nav-item-placeholder">
              <span class="placeholder-text">功能開發中...</span>
            </li>
          </ul>
        </div>
        
        <!-- 占位大類 2 -->
        <div class="nav-section">
          <div class="nav-section-title" @click="toggleSection('report')">
            <el-icon><Document /></el-icon>
            <span v-show="!isCollapsed">數據報表</span>
            <el-icon v-show="!isCollapsed" class="expand-icon">
              <ArrowRight v-if="!expandedSections.report" />
              <ArrowDown v-else />
            </el-icon>
          </div>
          
          <ul v-show="!isCollapsed && expandedSections.report" class="nav-list nav-sublist">
            <li class="nav-item-placeholder">
              <span class="placeholder-text">功能開發中...</span>
            </li>
          </ul>
        </div>
      </nav>

      <!-- 收起按鈕 - 放在底部 -->
      <div class="sidebar-footer">
        <el-button 
          v-if="!isMobile" 
          link 
          class="collapse-btn" 
          @click="toggleCollapse"
        >
          <el-icon><Fold /></el-icon>
          <span class="collapse-text" v-show="!isCollapsed">收起</span>
        </el-button>
      </div>
    </aside>

    <!-- 主內容區 -->
    <main class="main-content">
      <!-- 內容區域 -->
      <div class="content-wrapper">
        <transition name="fade" mode="out-in">
          <!-- 發布通知 -->
          <PublishNotification 
            v-if="activeMenu === '1-1'"
            @publish-success="handlePublishSuccess" 
          />
          
          <!-- 抄送我的 -->
          <NotificationList 
            v-else-if="activeMenu === '1-2'"
            :notifications="ccToMeNotifications"
            :pagination="ccPagination"
            @refresh="loadCcToMeNotifications"
            @page-change="handleCcPageChange"
            type="ccToMe"
          />
          
          <!-- 我發送的 -->
          <NotificationList 
            v-else-if="activeMenu === '1-3'"
            :notifications="mySendNotifications"
            :pagination="mySendPagination"
            @refresh="loadMySendNotifications"
            @page-change="handleMySendPageChange"
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
        </transition>
      </div>
    </main>

    <!-- 移動端遮罩層 -->
    <div v-if="isMobile && isMobileMenuOpen" class="overlay" @click="toggleMobileMenu"></div>
  </div>
</template>

<script>
import { Bell, Promotion, Edit, Message, Fold, Setting, Document, ArrowRight, ArrowDown, User, OfficeBuilding } from '@element-plus/icons-vue'
import NotificationList from './NotificationList.vue'
import PublishNotification from './PublishNotification.vue'
import SchoolDepartment from './SchoolDepartment.vue'
import HomeSchoolContacts from './HomeSchoolContacts.vue'
import request from '@/utils/request'

export default {
  name: 'SchoolNotificationSystem',
  components: {
    NotificationList,
    PublishNotification,
    SchoolDepartment,
    HomeSchoolContacts
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
      expandedSections: this.getInitialExpandedSections(),
      menuItems: [
        { index: '1-1', title: '發布通知', icon: 'Edit' },
        { index: '1-2', title: '抄送我的', icon: 'Message'},
        { index: '1-3', title: '我發送的', icon: 'Promotion' }
      ]
    }
  },
  mounted() {
    this.checkScreenSize()
    window.addEventListener('resize', this.handleResize)
    // 根据当前激活的菜单加载对应数据
    this.loadInitialData()
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize)
  },
  methods: {
    getInitialActiveMenu() {
      // 从 sessionStorage 获取上次访问的菜单，如果没有则默认为 '1-1'
      const savedMenu = sessionStorage.getItem('activeMenu')
      return savedMenu || '1-1'
    },
    
    saveActiveMenu(menu) {
      // 保存当前菜单到 sessionStorage
      sessionStorage.setItem('activeMenu', menu)
    },
    
    getInitialExpandedSections() {
      // 从 sessionStorage 获取上次菜单展开状态
      const savedSections = sessionStorage.getItem('expandedSections')
      if (savedSections) {
        try {
          return JSON.parse(savedSections)
        } catch (e) {
          console.error('解析菜单展开状态失败:', e)
        }
      }
      // 默认状态：家校通知展开，其他折叠
      return {
        homeSchool: true,
        contact: false,
        system: false,
        report: false
      }
    },
    
    saveExpandedSections() {
      // 保存菜单展开状态到 sessionStorage
      sessionStorage.setItem('expandedSections', JSON.stringify(this.expandedSections))
    },
    loadInitialData() {
      // 根据当前激活的菜单加载对应的数据
      if (this.activeMenu === '1-2') {
        this.loadCcToMeNotifications()
      } else if (this.activeMenu === '1-3') {
        this.loadMySendNotifications()
      }
      // 其他菜单无需加载数据
    },
    
    checkScreenSize() {
      this.isMobile = window.innerWidth <= 768
      if (!this.isMobile) {
        this.isMobileMenuOpen = false
      }
    },
    
    handleResize() {
      this.checkScreenSize()
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
      this.saveExpandedSections() // 保存展开状态
    },
    
    handleMenuSelect(index) {
      this.activeMenu = index
      this.saveActiveMenu(index) // 保存菜单选择
      if (this.isMobile) {
        this.isMobileMenuOpen = false
      }
      if (index === '1-1') {
        // 發布通知，无需加载数据
      } else if (index === '1-2') {
        this.loadCcToMeNotifications()
      } else if (index === '1-3') {
        this.mySendPagination.currentPage = 1
        this.loadMySendNotifications()
      } else if (index === '2-1') {
        // 老師通訊錄，无需加载数据
      } else if (index === '2-2') {
        // 家校通訊錄，无需加载数据
      }
    },

    async loadCcToMeNotifications(params = {}) {
      /*try {
        const response = await request({
          url: '/system/notification/ccToMe',
          method: 'get',
          params: {
            pageNum: params.pageNum || this.ccPagination.currentPage,
            pageSize: params.pageSize || this.ccPagination.pageSize
          }
        })
        
        if (response.code === 200 || response.code === 0) {
          this.ccToMeNotifications = response.rows || []
          this.ccPagination.total = response.total || 0
        }
      } catch (error) {
        console.error('加载失败:', error)
        this.$message.error('数据加载失败')
      }*/
    },

    async loadMySendNotifications(params = {}) {
      try {
        const response = await request({
          url: '/system/notification/mySend',
          method: 'get',
          params: {
            pageNum: params.pageNum || this.mySendPagination.currentPage,
            pageSize: params.pageSize || this.mySendPagination.pageSize,
            publishDate: params.publishDate || ''
          }
        })
        
        if (response.code === 200 || response.code === 0) {
          this.mySendNotifications = response.rows || []
          this.mySendPagination.total = response.total || 0
        }
      } catch (error) {
        console.error('加载失败:', error)
        this.$message.error('数据加载失败')
      }
    },

    handleCcPageChange({ pageNum, pageSize }) {
      this.ccPagination.currentPage = pageNum
      this.ccPagination.pageSize = pageSize
      this.loadCcToMeNotifications({ pageNum, pageSize })
    },

    handleMySendPageChange({ pageNum, pageSize, publishDate }) {
      this.mySendPagination.currentPage = pageNum
      this.mySendPagination.pageSize = pageSize
      this.loadMySendNotifications({ pageNum, pageSize, publishDate })
    },

    handlePublishSuccess() {
      this.activeMenu = '1-3'
      this.loadMySendNotifications()
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

/* 側邊欄樣式 */
.sidebar {
  width: 260px;
  background: linear-gradient(180deg, #1e40af 0%, #3b82f6 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.25, 1);
  flex-shrink: 0;
  position: relative;
  z-index: 100;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1), 4px 0 16px rgba(59, 130, 246, 0.15);
}

.sidebar.collapsed {
  width: 72px;
}

.sidebar-header {
  padding: 24px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  background: rgba(30, 64, 175, 0.3);
  backdrop-filter: blur(12px);
  min-height: 88px;
}

.sidebar.collapsed .sidebar-header {
  gap: 12px;
  padding: 20px 16px;
  min-height: 80px;
}

/* 側邊欄底部 */
.sidebar-footer {
  padding: 20px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  justify-content: center;
  align-items: center;
  background: rgba(30, 64, 175, 0.2);
  backdrop-filter: blur(8px);
  margin-top: auto;
}

.logo-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.25, 1);
  width: 100%;
}

.collapse-btn {
  color: rgba(255, 255, 255, 0.75) !important;
  font-size: 13px !important;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.25, 1);
  padding: 8px 12px !important;
  border-radius: 8px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.05) !important;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  backdrop-filter: blur(8px);
  opacity: 0.9;
}

.collapse-btn .el-icon {
  font-size: 18px;
  width: 20px;
  height: 20px;
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.25, 1);
}

.collapse-btn:hover {
  color: #fff !important;
  background: rgba(255, 255, 255, 0.12) !important;
  border-color: rgba(255, 255, 255, 0.15) !important;
  opacity: 1;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
}

.sidebar.collapsed .collapse-btn {
  padding: 10px !important;
  margin-top: 4px;
}

.sidebar.collapsed .collapse-btn:hover {
  background: rgba(255, 255, 255, 0.15) !important;
  transform: translateY(-1px) scale(1.05);
}

.sidebar.collapsed .collapse-btn .collapse-text {
  display: none;
}

.logo-icon {
  width: 40px;
  height: 40px;
  object-fit: contain;
  flex-shrink: 0;
  filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.2));
  transition: all 0.3s cubic-bezier(0.4, 0, 0.25, 1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  padding: 4px;
}

.sidebar.collapsed .logo-icon {
  margin: 0 auto;
  transform: scale(1.05);
  filter: drop-shadow(0 4px 16px rgba(255, 255, 255, 0.3));
}

.logo-text {
  font-size: 17px;
  font-weight: 600;
  white-space: nowrap;
  color: #fff;
  text-align: center;
  letter-spacing: 1px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.25, 1);
  opacity: 1;
}

.sidebar.collapsed .logo-text {
  opacity: 0;
  width: 0;
  overflow: hidden;
  transform: translateX(-10px);
}

/* 導航欄樣式 */
.sidebar-nav {
  flex: 1;
  padding: 16px 12px;
  overflow-y: auto;
  background: transparent;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sidebar-nav::-webkit-scrollbar {
  width: 6px;
}

.sidebar-nav::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 6px;
  transition: background 0.25s ease;
}

.sidebar-nav::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.25);
}

.sidebar-nav::-webkit-scrollbar-track {
  background: transparent;
  margin: 4px 0;
}

.nav-section {
  margin-bottom: 16px;
}

.nav-section:last-child {
  margin-bottom: 0;
}

.nav-section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.25, 1);
  user-select: none;
  position: relative;
  letter-spacing: 0.8px;
  text-transform: uppercase;
}

.nav-section-title:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

.nav-section-title .el-icon {
  font-size: 16px;
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.expand-icon {
  margin-left: auto;
  font-size: 12px;
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.25, 1);
  opacity: 0.6;
}

.nav-section-title:hover .expand-icon {
  opacity: 1;
  transform: translateX(2px);
}

.nav-list {
  list-style: none;
  padding: 0 6px;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  margin-bottom: 0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.25, 1);
  color: rgba(255, 255, 255, 0.75);
  position: relative;
  font-size: 14px;
  border: 1px solid transparent;
  font-weight: 400;
}

.nav-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%) scaleY(0);
  width: 4px;
  height: 60%;
  background: linear-gradient(180deg, #60a5fa 0%, #3b82f6 100%);
  border-radius: 0 4px 4px 0;
  opacity: 0;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.25, 1);
}

.nav-item:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.12);
  padding-left: 16px;
}

.nav-item:hover::before {
  opacity: 1;
  transform: translateY(-50%) scaleY(1);
  height: 70%;
}

.nav-item.active {
  color: #fff;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.85) 0%, rgba(37, 99, 235, 0.75) 100%);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.2);
  font-weight: 500;
  border-color: rgba(255, 255, 255, 0.25);
  padding-left: 16px;
}

.nav-item.active::before {
  opacity: 1;
  transform: translateY(-50%) scaleY(1);
  height: 70%;
  background: linear-gradient(180deg, #93c5fd 0%, #60a5fa 100%);
}

.nav-icon {
  font-size: 18px;
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.25, 1);
}

.nav-item:hover .nav-icon {
  transform: scale(1.1);
  filter: drop-shadow(0 2px 6px rgba(255, 255, 255, 0.2));
}

.nav-item.active .nav-icon {
  transform: scale(1.15);
  filter: drop-shadow(0 2px 8px rgba(255, 255, 255, 0.35));
}

.nav-text {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  letter-spacing: 0.3px;
}

.nav-badge {
  position: absolute;
  right: 12px;
  transform: scale(0.9);
  z-index: 1;
}

.nav-sublist {
  margin-top: 6px;
  padding: 8px 8px 8px 10px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.nav-sublist .nav-item {
  margin-bottom: 0;
  padding: 9px 13px;
  font-size: 13.5px;
  border-radius: 6px;
}

.nav-sublist .nav-item::before {
  width: 3px;
  height: 40%;
}

.nav-sublist .nav-item:hover {
  padding-left: 15px;
}

.nav-sublist .nav-item.active {
  padding-left: 15px;
}

.nav-item-placeholder {
  padding: 10px 14px;
  color: rgba(255, 255, 255, 0.4);
  font-size: 13px;
  font-style: italic;
  text-align: center;
  letter-spacing: 0.5px;
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



/* 響應式設計 */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: -272px;
    top: 0;
    bottom: 0;
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.25, 1);
  }
  
  .sidebar.mobile-visible {
    transform: translateX(272px);
  }
  
  .sidebar.collapsed {
    width: 272px;
  }
  
  .main-content {
    margin-left: 0;
  }
  
  .content-wrapper {
    padding: 20px 24px;
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
