<template>
  <div class="student-photo" :style="wrapperStyle">
    <el-image
      v-if="photoSrc"
      :src="photoSrc"
      :preview-src-list="previewList"
      :initial-index="0"
      fit="cover"
      preview-teleported
      hide-on-click-modal
      class="student-photo__image student-photo__image--zoomable"
      :style="imageStyle"
    >
      <template #error>
        <div class="student-photo__placeholder" :style="imageStyle">
          <el-icon><User /></el-icon>
        </div>
      </template>
    </el-image>
    <div v-else-if="loading" class="student-photo__placeholder student-photo__loading" :style="imageStyle">
      <el-icon class="is-loading"><Loading /></el-icon>
    </div>
    <div v-else class="student-photo__placeholder" :style="imageStyle">
      <el-icon><User /></el-icon>
    </div>
  </div>
</template>

<script>
import { User, Loading } from '@element-plus/icons-vue'
import request from '@/utils/request'

const photoCache = new Map()

export default {
  name: 'StudentPhoto',
  components: { User, Loading },
  props: {
    profileNumber: {
      type: [String, Number],
      default: ''
    },
    size: {
      type: Number,
      default: 40
    }
  },
  data() {
    return {
      photoSrc: '',
      loading: false,
      objectUrl: ''
    }
  },
  computed: {
    normalizedProfileNumber() {
      if (this.profileNumber === null || this.profileNumber === undefined || this.profileNumber === '') {
        return ''
      }
      return String(this.profileNumber).trim()
    },
    wrapperStyle() {
      return {
        width: `${this.size}px`,
        height: `${this.size}px`
      }
    },
    imageStyle() {
      return {
        width: `${this.size}px`,
        height: `${this.size}px`,
        borderRadius: this.size >= 80 ? '8px' : '4px'
      }
    },
    previewList() {
      return this.photoSrc ? [this.photoSrc] : []
    }
  },
  watch: {
    normalizedProfileNumber: {
      immediate: true,
      handler() {
        this.loadPhoto()
      }
    }
  },
  beforeUnmount() {
    this.revokeObjectUrl()
  },
  methods: {
    revokeObjectUrl() {
      if (this.objectUrl) {
        URL.revokeObjectURL(this.objectUrl)
        this.objectUrl = ''
      }
    },
    async loadPhoto() {
      this.revokeObjectUrl()
      this.photoSrc = ''

      const profileNumber = this.normalizedProfileNumber
      if (!/^[0-9]{1,20}$/.test(profileNumber)) {
        this.loading = false
        return
      }

      if (photoCache.has(profileNumber)) {
        this.photoSrc = photoCache.get(profileNumber)
        return
      }

      this.loading = true
      try {
        const blob = await request({
          url: `/system/student/match/photo/${profileNumber}`,
          method: 'get',
          responseType: 'blob',
          silentError: true
        })
        if (blob && blob.size > 0 && blob.type && blob.type.startsWith('image/')) {
          const objectUrl = URL.createObjectURL(blob)
          photoCache.set(profileNumber, objectUrl)
          this.objectUrl = objectUrl
          this.photoSrc = objectUrl
        }
      } catch (e) {
        // 無照片或無權限時顯示佔位
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.student-photo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.student-photo__image {
  display: block;
  border: 1px solid #e4e7ed;
  background: #fff;
}

.student-photo__image--zoomable {
  cursor: zoom-in;
}

.student-photo__image--zoomable:hover {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.15);
}

.student-photo__placeholder {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  color: #c0c4cc;
  font-size: 16px;
}

.student-photo__loading {
  color: #909399;
}
</style>
