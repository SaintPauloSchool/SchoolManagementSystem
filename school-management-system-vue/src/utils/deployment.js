export const APP_BASE_PATH = '/school-management-system/'
export const API_BASE_PATH = '/sms-api'
export const PROFILE_BASE_PATH = `${API_BASE_PATH}/profile`

export function normalizeProfileUrl(url) {
  if (!url || typeof url !== 'string') {
    return url
  }

  if (url.startsWith(PROFILE_BASE_PATH)) {
    return url
  }

  if (url.startsWith('/profile/')) {
    return url.replace(/^\/profile\//, `${PROFILE_BASE_PATH}/`)
  }

  return url
}

export function withAppBase(path = '') {
  const normalizedPath = path.replace(/^\/+/, '')
  return `${APP_BASE_PATH}${normalizedPath}`
}
