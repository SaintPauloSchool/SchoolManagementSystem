import axios from 'axios'
import { ElMessageBox, ElNotification } from 'element-plus'
import { API_BASE_PATH, withAppBase } from './deployment'
import settings from '../config/settings'

// Create an axios instance aligned with the separated frontend/backend deployment.
const service = axios.create({
  baseURL: API_BASE_PATH,
  timeout: 15000
})

import MD5 from 'crypto-js/md5' // 导入 MD5 用于计算签名

// 生成唯一标识符(UUID的简易实现)
const generateNonce = () => {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID().replace(/-/g, '');
  }
  return 'xxxxxxxxxxxx4xxxyxxxxxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
};

service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    if (config.method === 'get') {
      config.params = {
        ...config.params,
        _t: new Date().getTime()
      }
    }

    // API 安全校驗拦截器
    const timestamp = Date.now().toString();
    const nonce = generateNonce();
    const appSecret = settings.appSecret;
    const signature = MD5(appSecret + timestamp + nonce).toString();

    config.headers['x-timestamp'] = timestamp;
    config.headers['x-nonces'] = nonce;
    config.headers['x-signature'] = signature;

    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  response => {
    // 如果是 blob 类型（文件下载），直接返回原始 response
    if (response.config.responseType === 'blob') {
      return response.data
    }

    const res = response.data

    // 402 业务状态码由组件自行处理，拦截器不显示错误提示
    if (res.code === 402) {
      return res
    }

    if (res.code !== 200 && res.code !== 0) {
      ElNotification({
        title: '請求失敗',
        message: res.msg || res.message || '請求失敗',
        type: 'error',
        duration: 4000
      })

      if (res.code === 401) {
        localStorage.removeItem('token')
        sessionStorage.removeItem('token')
        
        // 如果在微信環境中，嘗試自動靜默重新授權，避免彈窗中斷體驗
        const isWeChat = /MicroMessenger/i.test(navigator.userAgent);
        if (isWeChat) {
            const pendingNoticeId = sessionStorage.getItem('pendingNoticeId');
            const studentHandbookUrl = import.meta.env.MODE === 'production'
                ? 'http://tals-wcapp.esp.edu.mo/login'
                : 'http://10.32.96.55:8082/login';
            
            let redirectUrl = studentHandbookUrl;
            if (pendingNoticeId) {
                redirectUrl += '?redirect_to_campus=' + pendingNoticeId;
            }
            window.location.replace(redirectUrl);
            return Promise.reject(new Error('登錄已過期，正在重新授權'));
        }

        ElMessageBox.alert(
          '登錄狀態已過期或失效，請關閉此視窗，並重新從「學生手冊」系統點擊進入。',
          '系統提示',
          {
            confirmButtonText: '確定',
            type: 'warning'
          }
        ).then(() => {
          window.location.href = window.location.pathname
        })
      }

      return Promise.reject(new Error(res.msg || res.message || '请求失败'))
    }

    return res
  },
  error => {
    console.error('响应错误:', error)

    let message = '网络异常，请稍后重试'

    if (error.response) {
      switch (error.response.status) {
        case 400:
          message = '请求参数错误'
          break
        case 401:
          message = '未授权，请重新登录'
          break
        case 403:
          message = '拒绝访问'
          break
        case 404:
          message = '请求地址不存在'
          break
        case 500:
          message = '服务器内部错误'
          break
        case 502:
          message = '网关错误'
          break
        case 503:
          message = '服务不可用'
          break
        case 504:
          message = '网关超时'
          break
        default:
          message = `请求失败(${error.response.status})`
      }
    } else if (error.message.includes('timeout')) {
      message = '请求超时'
    } else if (error.message.includes('Network Error')) {
      message = '网络连接失败'
    }

    ElNotification({
      title: '網路錯誤',
      message,
      type: 'error',
      duration: 4000
    })

    return Promise.reject(error)
  }
)

export default service
