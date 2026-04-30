import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { API_BASE_PATH, withAppBase } from './deployment'

// Create an axios instance aligned with the separated frontend/backend deployment.
const service = axios.create({
  baseURL: API_BASE_PATH,
  timeout: 15000
})

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
      ElMessage({
        message: res.msg || res.message || '请求失败',
        type: 'error',
        duration: 3000
      })

      if (res.code === 401) {
        ElMessageBox.confirm(
          '登录状态已过期，是否重新登录？',
          '系统提示',
          {
            confirmButtonText: '重新登录',
            cancelButtonText: '取消',
            type: 'warning'
          }
        ).then(() => {
          localStorage.removeItem('token')
          sessionStorage.removeItem('token')
          window.location.href = withAppBase()
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

    ElMessage({
      message,
      type: 'error',
      duration: 3000
    })

    return Promise.reject(error)
  }
)

export default service
