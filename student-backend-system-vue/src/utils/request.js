import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

// 创建 axios 实例
const service = axios.create({
  baseURL: '/api', // API 基础路径（与 vite.config.js 中的代理配置对应）
  timeout: 15000 // 请求超时时间
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 在发送请求之前做些什么
    const token = localStorage.getItem('token') || sessionStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    
    // 防止缓存，添加时间戳
    if (config.method === 'get') {
      config.params = {
        ...config.params,
        _t: new Date().getTime()
      }
    }
    
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    
    // 如果返回的状态码不是 200，则认为是错误
    if (res.code !== 200 && res.code !== 0) {
      // 业务错误处理
      ElMessage({
        message: res.msg || res.message || '请求失败',
        type: 'error',
        duration: 3000
      })
      
      // 特殊错误码处理（如 401 未授权、403 无权限等）
      if (res.code === 401) {
        ElMessageBox.confirm(
          '登录状态已过期，请重新登录',
          '系统提示',
          {
            confirmButtonText: '重新登录',
            cancelButtonText: '取消',
            type: 'warning'
          }
        ).then(() => {
          // 清除 token 并跳转登录页
          localStorage.removeItem('token')
          sessionStorage.removeItem('token')
          window.location.href = '/login'
        })
      }
      
      return Promise.reject(new Error(res.msg || res.message || '请求失败'))
    } else {
      return res
    }
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
          message = `连接错误${error.response.status}`
      }
    } else if (error.message.includes('timeout')) {
      message = '请求超时'
    } else if (error.message.includes('Network Error')) {
      message = '网络连接失败'
    }
    
    ElMessage({
      message,
      type: 'error',
      duration: 3000
    })
    
    return Promise.reject(error)
  }
)

export default service
