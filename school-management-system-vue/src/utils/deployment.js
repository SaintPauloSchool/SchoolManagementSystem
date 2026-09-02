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

/** 將路徑各段編碼，避免中文檔名導致 img 載入失敗 */
export function encodeProfileUrlPath(url) {
  if (!url || typeof url !== 'string') {
    return url
  }

  const queryIndex = url.indexOf('?')
  const path = queryIndex >= 0 ? url.slice(0, queryIndex) : url
  const query = queryIndex >= 0 ? url.slice(queryIndex) : ''

  const encodedPath = path.split('/').map((segment) => {
    if (!segment) {
      return segment
    }
    try {
      return encodeURIComponent(decodeURIComponent(segment))
    } catch {
      return encodeURIComponent(segment)
    }
  }).join('/')

  return encodedPath + query
}

function resolveApiProfilePath(url) {
  if (!url || typeof url !== 'string') {
    return url
  }

  if (/^https?:\/\//i.test(url)) {
    try {
      const { pathname } = new URL(url)
      const profileIndex = pathname.indexOf('/profile/')
      if (profileIndex >= 0) {
        return `${API_BASE_PATH}${pathname.slice(profileIndex)}`.replace(/([^:])\/\/+/g, '$1/')
      }
    } catch {
      // ignore invalid URL
    }
    return url
  }

  if (url.startsWith(`${PROFILE_BASE_PATH}/`)) {
    return url.replace(/([^:])\/\/+/g, '$1/')
  }

  if (url.startsWith('/profile/')) {
    return `${API_BASE_PATH}${url}`.replace(/([^:])\/\/+/g, '$1/')
  }

  const normalized = normalizeProfileUrl(url)
  if (normalized.startsWith(`${PROFILE_BASE_PATH}/`)) {
    return normalized
  }

  return normalized
}

/**
 * 圖片預覽用：走 /sms-api/profile（後端 SecurityConfig 已放行，無需 JWT）。
 * 不依賴 Nginx 根路徑 /profile/ 靜態配置。
 */
export function toPublicProfileUrl(url) {
  return encodeProfileUrlPath(resolveApiProfilePath(url))
}
