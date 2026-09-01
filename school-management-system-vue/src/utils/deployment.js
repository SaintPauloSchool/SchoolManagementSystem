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

/** 圖片預覽用：走 Nginx /profile/ 靜態資源（img 標籤無法帶 JWT） */
export function toPublicProfileUrl(url) {
  if (!url || typeof url !== 'string') {
    return url
  }

  if (url.startsWith('/profile/')) {
    return url.replace(/([^:])\/\/+/g, '$1/')
  }

  const normalized = normalizeProfileUrl(url)
  if (normalized.startsWith(`${PROFILE_BASE_PATH}/`)) {
    return normalized.replace(PROFILE_BASE_PATH, '/profile').replace(/([^:])\/\/+/g, '$1/')
  }

  return normalized
}
