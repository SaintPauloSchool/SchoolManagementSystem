export const CRON_TYPES = {
  EVERY_MINUTE: 'every_minute',
  EVERY_N_MINUTES: 'every_n_minutes',
  DAILY: 'daily',
  WEEKDAYS: 'weekdays',
  WEEKLY: 'weekly',
  HOURLY_RANGE: 'hourly_range'
}

export const CRON_TYPE_OPTIONS = [
  { value: CRON_TYPES.EVERY_MINUTE, label: '每分鐘' },
  { value: CRON_TYPES.EVERY_N_MINUTES, label: '每 N 分鐘' },
  { value: CRON_TYPES.DAILY, label: '每天固定時間' },
  { value: CRON_TYPES.WEEKDAYS, label: '週一至週五' },
  { value: CRON_TYPES.WEEKLY, label: '每週指定日' },
  { value: CRON_TYPES.HOURLY_RANGE, label: '每天時段內每小時' }
]

export const WEEKDAY_OPTIONS = [
  { value: 'MON', label: '週一' },
  { value: 'TUE', label: '週二' },
  { value: 'WED', label: '週三' },
  { value: 'THU', label: '週四' },
  { value: 'FRI', label: '週五' },
  { value: 'SAT', label: '週六' },
  { value: 'SUN', label: '週日' }
]

const WEEKDAY_LABEL_MAP = WEEKDAY_OPTIONS.reduce((map, item) => {
  map[item.value] = item.label
  return map
}, {})

function padTimePart(value) {
  return String(value).padStart(2, '0')
}

function formatTime(hour, minute) {
  return `${padTimePart(hour)}:${padTimePart(minute)}`
}

function parseTimeValue(time) {
  if (!time || typeof time !== 'string') {
    return { hour: 0, minute: 0 }
  }
  const [hourText, minuteText] = time.split(':')
  return {
    hour: Number.parseInt(hourText, 10) || 0,
    minute: Number.parseInt(minuteText, 10) || 0
  }
}

export function createDefaultCronForm() {
  return {
    type: CRON_TYPES.DAILY,
    intervalMinutes: 5,
    time: '08:00',
    weekDays: ['MON'],
    startHour: 9,
    endHour: 18
  }
}

export function parseCronExpression(cronExpression) {
  const cron = (cronExpression || '').trim()
  const defaults = createDefaultCronForm()

  if (!cron) {
    return { ...defaults }
  }

  if (/^0 \* \* \* \* \?$/.test(cron)) {
    return { ...defaults, type: CRON_TYPES.EVERY_MINUTE }
  }

  const everyNMinutesMatch = cron.match(/^0 \*\/(\d+) \* \* \* \?$/)
  if (everyNMinutesMatch) {
    return {
      ...defaults,
      type: CRON_TYPES.EVERY_N_MINUTES,
      intervalMinutes: Number.parseInt(everyNMinutesMatch[1], 10) || 5
    }
  }

  const dailyMatch = cron.match(/^0 (\d{1,2}) (\d{1,2}) \* \* \?$/)
  if (dailyMatch) {
    return {
      ...defaults,
      type: CRON_TYPES.DAILY,
      time: formatTime(Number.parseInt(dailyMatch[2], 10), Number.parseInt(dailyMatch[1], 10))
    }
  }

  const weekdaysMatch = cron.match(/^0 (\d{1,2}) (\d{1,2}) \? \* MON-FRI$/)
  if (weekdaysMatch) {
    return {
      ...defaults,
      type: CRON_TYPES.WEEKDAYS,
      time: formatTime(Number.parseInt(weekdaysMatch[2], 10), Number.parseInt(weekdaysMatch[1], 10))
    }
  }

  const weeklyMatch = cron.match(/^0 (\d{1,2}) (\d{1,2}) \? \* ([A-Z,-]+)$/)
  if (weeklyMatch && weeklyMatch[3] !== 'MON-FRI') {
    return {
      ...defaults,
      type: CRON_TYPES.WEEKLY,
      time: formatTime(Number.parseInt(weeklyMatch[2], 10), Number.parseInt(weeklyMatch[1], 10)),
      weekDays: weeklyMatch[3].split(',')
    }
  }

  const hourlyRangeMatch = cron.match(/^0 0 (\d{1,2})-(\d{1,2}) \* \* \?$/)
  if (hourlyRangeMatch) {
    return {
      ...defaults,
      type: CRON_TYPES.HOURLY_RANGE,
      startHour: Number.parseInt(hourlyRangeMatch[1], 10),
      endHour: Number.parseInt(hourlyRangeMatch[2], 10)
    }
  }

  return { ...defaults, type: CRON_TYPES.DAILY, time: '08:00' }
}

export function buildCronExpression(form) {
  if (!form || !form.type) {
    return ''
  }

  switch (form.type) {
    case CRON_TYPES.EVERY_MINUTE:
      return '0 * * * * ?'
    case CRON_TYPES.EVERY_N_MINUTES: {
      const interval = Math.min(59, Math.max(1, Number(form.intervalMinutes) || 1))
      return `0 */${interval} * * * ?`
    }
    case CRON_TYPES.DAILY: {
      const { hour, minute } = parseTimeValue(form.time)
      return `0 ${minute} ${hour} * * ?`
    }
    case CRON_TYPES.WEEKDAYS: {
      const { hour, minute } = parseTimeValue(form.time)
      return `0 ${minute} ${hour} ? * MON-FRI`
    }
    case CRON_TYPES.WEEKLY: {
      const days = Array.isArray(form.weekDays) ? form.weekDays.filter(Boolean) : []
      if (days.length === 0) {
        return ''
      }
      const { hour, minute } = parseTimeValue(form.time)
      return `0 ${minute} ${hour} ? * ${days.join(',')}`
    }
    case CRON_TYPES.HOURLY_RANGE: {
      const startHour = Math.min(23, Math.max(0, Number(form.startHour) || 0))
      const endHour = Math.min(23, Math.max(startHour, Number(form.endHour) || startHour))
      return `0 0 ${startHour}-${endHour} * * ?`
    }
    default:
      return ''
  }
}

export function describeCronForm(form) {
  if (!form || !form.type) {
    return ''
  }

  switch (form.type) {
    case CRON_TYPES.EVERY_MINUTE:
      return '每分鐘執行'
    case CRON_TYPES.EVERY_N_MINUTES:
      return `每 ${Math.max(1, Number(form.intervalMinutes) || 1)} 分鐘執行`
    case CRON_TYPES.DAILY:
      return `每天 ${form.time || '00:00'} 執行`
    case CRON_TYPES.WEEKDAYS:
      return `週一至週五 ${form.time || '00:00'} 執行`
    case CRON_TYPES.WEEKLY: {
      const labels = (form.weekDays || [])
        .map(day => WEEKDAY_LABEL_MAP[day])
        .filter(Boolean)
      return labels.length > 0
        ? `每${labels.join('、')} ${form.time || '00:00'} 執行`
        : '請選擇星期'
    }
    case CRON_TYPES.HOURLY_RANGE:
      return `每天 ${padTimePart(form.startHour)}:00-${padTimePart(form.endHour)}:00 每小時執行`
    default:
      return ''
  }
}

export function describeCronExpression(cronExpression) {
  const form = parseCronExpression(cronExpression)
  const generated = buildCronExpression(form)
  if (generated === (cronExpression || '').trim()) {
    return describeCronForm(form)
  }
  return cronExpression || '-'
}

export function validateCronForm(form) {
  if (!form || !form.type) {
    return '請選擇定時方式'
  }

  if (form.type === CRON_TYPES.EVERY_N_MINUTES) {
    const interval = Number(form.intervalMinutes)
    if (!Number.isInteger(interval) || interval < 1 || interval > 59) {
      return '分鐘間隔需為 1-59'
    }
  }

  if ([CRON_TYPES.DAILY, CRON_TYPES.WEEKDAYS, CRON_TYPES.WEEKLY].includes(form.type) && !form.time) {
    return '請選擇執行時間'
  }

  if (form.type === CRON_TYPES.WEEKLY && (!form.weekDays || form.weekDays.length === 0)) {
    return '請至少選擇一個星期'
  }

  if (form.type === CRON_TYPES.HOURLY_RANGE) {
    const startHour = Number(form.startHour)
    const endHour = Number(form.endHour)
    if (!Number.isInteger(startHour) || !Number.isInteger(endHour) || startHour < 0 || endHour > 23) {
      return '請選擇有效的時段'
    }
    if (startHour > endHour) {
      return '結束小時不能早於開始小時'
    }
  }

  const cronExpression = buildCronExpression(form)
  if (!cronExpression) {
    return '無法生成 Cron 表達式'
  }

  return ''
}
