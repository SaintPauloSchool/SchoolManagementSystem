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
    // 提取可能存在的 notice ID (例如 /school-management-system/123)
    const pathParts = window.location.pathname.split('/').filter(Boolean);
    const lastPart = pathParts[pathParts.length - 1];
    let pendingNoticeId = null;

    // 如果最後一部分是純數字，則認為是 notice ID
    if (lastPart && /^\d+$/.test(lastPart)) {
      pendingNoticeId = lastPart;
      sessionStorage.setItem('pendingNoticeId', lastPart);
      // 清理 URL，移除結尾的 /123
      const cleanPath = window.location.pathname.replace(new RegExp(`/${lastPart}/?$`), '/');
      const newUrl = cleanPath + window.location.search + window.location.hash;
      window.history.replaceState({}, document.title, newUrl);
    }

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
        // 沒有 Token，如果是微信環境，直接跳轉去 StudentHandbook 重新授權
        const isWeChat = /MicroMessenger/i.test(navigator.userAgent);
        if (isWeChat) {
          let redirectUrl = import.meta.env.VITE_LOGIN_URL;
            const targetNoticeId = pendingNoticeId || sessionStorage.getItem('pendingNoticeId');
            if (targetNoticeId) {
                redirectUrl += '?redirect_to_campus=' + targetNoticeId + '&t=' + Date.now();
            } else {
                redirectUrl += '?redirect_to_campus=root&t=' + Date.now();
            }
            window.location.replace(redirectUrl);
            return;
        }

        this.hasToken = false;
      }
    }
  }
}
</script>

<style>
@import './styles/modern-theme.css';
@import './styles/selector-dialog.css';

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