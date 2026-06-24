export const APP_BASE_PATH = import.meta.env.VITE_APP_BASE_PATH || '/school-management-system/'
export const API_BASE_PATH = import.meta.env.VITE_API_BASE_PATH || '/sms-api'
export const PROFILE_BASE_PATH = `${API_BASE_PATH}/profile`

export function normalizeProfileUrl(url) {
  if (!url || typeof url !== 'string') {
    return url
  }

  let result = url

  if (result.startsWith(PROFILE_BASE_PATH)) {
    // 已經有 /sms-api/profile 前綴，直接進入清理
  } else if (result.startsWith('/profile/') || result.startsWith('/profile//')) {
    // 以 /profile/ 或 /profile// 開頭（舊格式或舊的雙斜線數據）
    result = result.replace(/^\/profile\/+/, `${PROFILE_BASE_PATH}/`)
  } else {
    return result
  }

  // 清除路徑中任何殘留的連續雙斜線（協議頭 // 除外）
  result = result.replace(/([^:])\/\/+/g, '$1/')

  return result
}
