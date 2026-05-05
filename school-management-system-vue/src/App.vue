<template>
  <div id="app">
    <SchoolNotificationSystem v-if="hasToken" />
    <div v-else class="error-container">
      <el-result
        icon="error"
        title="未經授權 / 登入已過期"
        sub-title="系統未能獲取您的身份資訊，請從「學生手冊」系統重新點擊進入校園管理系統。"
      >
      </el-result>
    </div>
  </div>
</template>

<script>
import SchoolNotificationSystem from './components/SchoolNotificationSystem.vue'

export default {
  name: 'App',
  components: {
    SchoolNotificationSystem
  },
  data() {
    return {
      hasToken: false
    }
  },
  created() {
    // 檢查 URL 中是否有 token 參數 (SSO 跳轉過來的)
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token');
    
    if (token) {
      // 存入 localStorage 供後續 API 請求使用
      localStorage.setItem('token', token);
      
      // 使用 history.replaceState 移除 URL 中的 token 參數，保護安全
      const newUrl = window.location.pathname + window.location.hash;
      window.history.replaceState({}, document.title, newUrl);
      
      this.hasToken = true;
    } else {
      // 檢查 localStorage 是否已經有 token
      const localToken = localStorage.getItem('token');
      if (localToken) {
        this.hasToken = true;
      } else {
        this.hasToken = false;
      }
    }
  }
}
</script>

<style>
@import './styles/modern-theme.css';

#app {
  height: 100vh;
  width: 100%;
  overflow: hidden;
}

.error-container {
  height: 100vh;
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f7fa;
}
</style>