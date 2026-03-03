<template>
  <div class="notification-system">
    <!-- 側邊欄 -->
    <aside class="sidebar" :class="{ 'collapsed': isCollapsed, 'mobile-visible': isMobileMenuOpen }">
      <div class="sidebar-header">
        <div class="logo">
          <div class="logo-icon">
            <svg viewBox="0 0 24 24" fill="none">
              <path d="M12 2L2 7L12 12L22 7L12 2Z" fill="currentColor"/>
              <path d="M2 17L12 22L22 17" stroke="currentColor" stroke-width="2"/>
              <path d="M2 12L12 17L22 12" stroke="currentColor" stroke-width="2"/>
            </svg>
          </div>
          <span v-show="!isCollapsed" class="logo-text">學生後台系統</span>
        </div>
        <el-button 
          v-if="!isMobile" 
          link 
          class="collapse-btn" 
          @click="toggleCollapse"
        >
          <el-icon><Fold /></el-icon>
        </el-button>
      </div>

      <nav class="sidebar-nav">
        <div class="nav-section">
          <div class="nav-section-title">
            <el-icon><Bell /></el-icon>
            <span v-show="!isCollapsed">家校通知</span>
          </div>
          
          <ul class="nav-list">
              <li 
                v-for="item in menuItems" 
                :key="item.index"
                class="nav-item"
                :class="{ active: activeMenu === item.index }"
                @click="handleMenuSelect(item.index)"
              >
                <el-icon class="nav-icon"><component :is="item.icon" /></el-icon>
                <span v-show="!isCollapsed" class="nav-text">{{ item.title }}</span>
              <el-badge 
                v-if="item.badge" 
                :value="item.badge" 
                :hidden="!badgeVisible"
                class="nav-badge"
              />
            </li>
          </ul>
        </div>
      </nav>
    </aside>

    <!-- 主內容區 -->
    <main class="main-content">
      <!-- 頂部導航 -->
      <header class="top-header">
        <div class="header-left">
          <el-button 
            v-if="isMobile" 
            link 
            class="mobile-menu-btn" 
            @click="toggleMobileMenu"
          >
            <el-icon><Menu /></el-icon>
          </el-button>
          <div class="breadcrumb-area">
            <h1 class="page-title">{{ pageTitle }}</h1>
            <el-breadcrumb separator="/">
              <el-breadcrumb-item>{{ currentPage }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
        </div>
        <div class="header-right">
          <el-button type="primary" plain @click="refreshData">
            <el-icon><Refresh /></el-icon>
            <span v-if="!isMobile">刷新數據</span>
          </el-button>
        </div>
      </header>

      <!-- 內容區域 -->
      <div class="content-wrapper">
        <transition name="fade" mode="out-in">
          <!-- 抄送我的 -->
          <NotificationList 
            v-if="activeMenu === '1-1'"
            :notifications="ccToMeNotifications" 
            @refresh="loadCcToMeNotifications"
            type="ccToMe"
          />
          
          <!-- 我發送的 -->
          <NotificationList 
            v-else-if="activeMenu === '1-2'"
            :notifications="mySendNotifications" 
            @refresh="loadMySendNotifications"
            type="mySend"
          />
          
          <!-- 發布通知 -->
          <PublishNotification 
            v-else-if="activeMenu === '1-3'"
            @publish-success="handlePublishSuccess" 
          />
        </transition>
      </div>
    </main>

    <!-- 移動端遮罩層 -->
    <div v-if="isMobile && isMobileMenuOpen" class="overlay" @click="toggleMobileMenu"></div>
  </div>
</template>

<script>
import { Bell, Promotion, Edit, Refresh, Message, Menu, Fold } from '@element-plus/icons-vue'
import NotificationList from './NotificationList.vue'
import PublishNotification from './PublishNotification.vue'

export default {
  name: 'SchoolNotificationSystem',
  components: {
    NotificationList,
    PublishNotification
  },
  data() {
    return {
      activeMenu: '1-1',
      ccToMeNotifications: [],
      mySendNotifications: [],
      isCollapsed: false,
      isMobileMenuOpen: false,
      isMobile: false,
      badgeVisible: true,
      menuItems: [
        { index: '1-1', title: '抄送我的', icon: 'Message', badge: 2 },
        { index: '1-2', title: '我發送的', icon: 'Promotion' },
        { index: '1-3', title: '發布通知', icon: 'Edit' }
      ]
    }
  },
  computed: {
    pageTitle() {
      const titles = {
        '1-1': '抄送給我的通知',
        '1-2': '我發送的通知',
        '1-3': '發布新通知'
      }
      return titles[this.activeMenu] || '學生後台系統'
    },
    currentPage() {
      const pages = {
        '1-1': '抄送我的',
        '1-2': '我發送的',
        '1-3': '發布通知'
      }
      return pages[this.activeMenu] || '家校通知'
    }
  },
  mounted() {
    this.checkScreenSize()
    window.addEventListener('resize', this.handleResize)
    this.loadCcToMeNotifications()
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize)
  },
  methods: {
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
    
    handleMenuSelect(index) {
      this.activeMenu = index
      if (this.isMobile) {
        this.isMobileMenuOpen = false
      }
      if (index === '1-1') {
        this.loadCcToMeNotifications()
      } else if (index === '1-2') {
        this.loadMySendNotifications()
      }
    },

    async loadCcToMeNotifications() {
      try {
        // 模擬數據
        this.ccToMeNotifications = [
          {
            notificationId: 1,
            title: '學校家長會通知',
            senderName: '張老師',
            status: '1',
            createTime: '2024-01-15 14:30:00'
          },
          {
            notificationId: 2,
            title: '寒假作業安排',
            senderName: '李主任',
            status: '1',
            createTime: '2024-01-14 09:15:00'
          }
        ]
      } catch (error) {
        console.error('加載失敗:', error)
        this.$message.error('數據加載失敗')
      }
    },

    async loadMySendNotifications() {
      try {
        this.mySendNotifications = [
          {
            notificationId: 3,
            title: '班級活動通知',
            senderName: '王老師',
            status: '1',
            createTime: '2024-01-16 10:00:00'
          }
        ]
      } catch (error) {
        console.error('加載失敗:', error)
        this.$message.error('數據加載失敗')
      }
    },

    handlePublishSuccess() {
      this.$message.success('通知發布成功')
      this.activeMenu = '1-2'
      this.loadMySendNotifications()
    },

    refreshData() {
      if (this.activeMenu === '1-1') {
        this.loadCcToMeNotifications()
      } else if (this.activeMenu === '1-2') {
        this.loadMySendNotifications()
      }
    }
  }
}
</script>

<style scoped>
.notification-system {
  display: flex;
  height: 100vh;
  background-color: #f9fafb;
  overflow: hidden;
}

/* 側邊欄樣式 */
.sidebar {
  width: 260px;
  background: linear-gradient(180deg, #1e293b 0%, #0f172a 100%);
  color: #ffffff;
  display: flex;
  flex-direction: column;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
  position: relative;
  z-index: 100;
}

.sidebar.collapsed {
  width: 70px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  flex-shrink: 0;
}

.logo-icon svg {
  width: 22px;
  height: 22px;
}

.logo-text {
  font-size: 16px;
  font-weight: 700;
  white-space: nowrap;
  background: linear-gradient(135deg, #60a5fa, #3b82f6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.collapse-btn {
  color: #94a3b8;
  font-size: 18px;
  transition: all 0.2s ease;
}

.collapse-btn:hover {
  color: #ffffff;
  transform: scale(1.1);
}

/* 導航欄樣式 */
.sidebar-nav {
  flex: 1;
  padding: 16px 12px;
  overflow-y: auto;
}

.nav-section {
  margin-bottom: 24px;
}

.nav-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  margin-bottom: 12px;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.nav-section-title .el-icon {
  font-size: 14px;
}

.nav-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  margin-bottom: 4px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  color: #cbd5e1;
  position: relative;
}

.nav-item:hover {
  background-color: rgba(255, 255, 255, 0.08);
  color: #ffffff;
}

.nav-item.active {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.nav-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.nav-text {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
}

.nav-badge {
  position: absolute;
  right: 12px;
}

/* 主內容區 */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: #f9fafb;
}

.top-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 32px;
  background-color: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
  flex: 1;
}

.mobile-menu-btn {
  font-size: 20px;
  color: #6b7280;
}

.breadcrumb-area {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #111827;
  letter-spacing: -0.5px;
}

.content-wrapper {
  flex: 1;
  padding: 24px 32px;
  overflow-y: auto;
  overflow-x: hidden;
}

.overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 99;
  backdrop-filter: blur(2px);
}

/* 響應式設計 */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: -260px;
    top: 0;
    bottom: 0;
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  }
  
  .sidebar.mobile-visible {
    transform: translateX(260px);
  }
  
  .main-content {
    margin-left: 0;
  }
  
  .top-header {
    padding: 16px 20px;
  }
  
  .content-wrapper {
    padding: 16px 20px;
  }
  
  .page-title {
    font-size: 18px;
  }
}

@media (max-width: 576px) {
  .top-header {
    padding: 12px 16px;
  }
  
  .content-wrapper {
    padding: 12px 16px;
  }
  
  .page-title {
    font-size: 16px;
  }
}

/* 動畫效果 */
.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
