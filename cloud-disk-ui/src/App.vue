<template>
  <div class="app-container">
    <!-- ==================== 登录界面 ==================== -->
    <div v-if="!isLogin" class="login-box">
      <h2>☁️ 迷你云盘</h2>
      <el-form :model="loginForm" label-width="0">
        <el-form-item>
          <el-input v-model="loginForm.username" placeholder="请输入账号 (如: zhangsan)" prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码 (如: 123456)" prefix-icon="Lock" @keyup.enter="handleLogin" />
        </el-form-item>
        <el-button type="primary" class="login-btn" @click="handleLogin">登 录</el-button>
      </el-form>
    </div>

    <!-- ==================== 网盘主界面 ==================== -->
    <div v-else class="disk-container">
      <header class="header">
        <div class="logo">☁️ 迷你云盘</div>
        <div class="user-info">
          欢迎，{{ currentUser.username }}
          <el-button link type="danger" @click="handleLogout">退出登录</el-button>
        </div>
      </header>

      <div class="main-content">
        <!-- 操作栏 -->
        <div class="action-bar">
          <!-- 拖拽上传区域 -->
          <div
            class="drop-zone"
            :class="{ 'drop-active': isDragging }"
            @dragenter.prevent="onDragEnter"
            @dragover.prevent="onDragOver"
            @dragleave.prevent="onDragLeave"
            @drop.prevent="onDrop"
          >
            <div class="drop-content">
              <el-icon :size="28"><UploadFilled /></el-icon>
              <span>拖拽文件或文件夹到此处上传</span>
              <span class="drop-hint">支持单文件、多文件、整个文件夹</span>
            </div>
          </div>
          <div class="action-buttons">
            <el-upload
              ref="uploadRef"
              class="upload-demo"
              action="/api/file/upload"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
              multiple
            >
              <el-button type="primary" icon="Upload">上传文件</el-button>
            </el-upload>
            <el-button icon="FolderAdd" @click="handleCreateFolder" style="margin-left: 10px;">新建文件夹</el-button>
            <el-button icon="Refresh" @click="loadFileList" style="margin-left: 10px;">刷新列表</el-button>
          </div>
        </div>

        <!-- 文件列表 -->
        <el-table :data="fileList" style="width: 100%" v-loading="loading">
          <el-table-column label="文件名" min-width="250">
            <template #default="scope">
              <el-icon v-if="scope.row.isDirectory === 1" color="#E6A23C" size="20" style="vertical-align: middle; margin-right: 8px;"><Folder /></el-icon>
              <el-icon v-else color="#409EFF" size="20" style="vertical-align: middle; margin-right: 8px;"><Document /></el-icon>
              <span>{{ scope.row.fileName }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="fileSize" label="大小" width="120">
            <template #default="scope">
              {{ scope.row.isDirectory === 1 ? '-' : formatSize(scope.row.fileSize) }}
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="上传时间" width="180" />
          <el-table-column label="操作" width="480" fixed="right">
            <template #default="scope">
              <el-button link type="success" @click="handleShare(scope.row)">分享</el-button>
              <el-button link type="warning" @click="handleRename(scope.row)">重命名</el-button>
              <el-button link type="info" @click="handleMove(scope.row)">移动</el-button>
              <el-button v-if="scope.row.isDirectory === 0" link type="primary" @click="handlePreview(scope.row)">预览</el-button>
              <el-button v-if="scope.row.isDirectory === 0" link type="primary" @click="handleDownload(scope.row)">下载</el-button>
              <el-button link type="danger" @click="handleDelete(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- ==================== 文件预览对话框 ==================== -->
    <el-dialog
      v-model="previewVisible"
      :title="previewFile?.fileName || '文件预览'"
      width="80%"
      top="5vh"
      destroy-on-close
      @closed="closePreview"
    >
      <div class="preview-container">
        <!-- 图片预览 -->
        <div v-if="previewCategory === 'image'" class="preview-image-wrapper">
          <img :src="previewUrl" :alt="previewFile?.fileName" class="preview-image" />
        </div>
        <!-- 文本/代码预览 -->
        <div v-else-if="previewCategory === 'text'" class="preview-text-wrapper">
          <pre class="preview-text">{{ previewText }}</pre>
        </div>
        <!-- PDF 预览 -->
        <div v-else-if="previewCategory === 'pdf'" class="preview-pdf-wrapper">
          <iframe :src="previewUrl" class="preview-pdf" frameborder="0"></iframe>
        </div>
        <!-- 视频预览 -->
        <div v-else-if="previewCategory === 'video'" class="preview-video-wrapper">
          <video :src="previewUrl" controls class="preview-video">
            您的浏览器不支持视频播放
          </video>
        </div>
        <!-- 音频预览 -->
        <div v-else-if="previewCategory === 'audio'" class="preview-audio-wrapper">
          <audio :src="previewUrl" controls class="preview-audio">
            您的浏览器不支持音频播放
          </audio>
        </div>
        <!-- 不支持预览 -->
        <div v-else class="preview-unsupported">
          <el-icon :size="64" color="#909399"><WarningFilled /></el-icon>
          <p>该文件类型暂不支持在线预览</p>
          <el-button type="primary" @click="handleDownload(previewFile)">下载文件</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import request from './utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Folder, Document, UploadFilled, FolderAdd, WarningFilled } from '@element-plus/icons-vue'

// ==================== 状态数据 ====================
const isLogin = ref(false)
const currentUser = ref({})
const fileList = ref([])
const loading = ref(false)
const uploadRef = ref(null)
const isDragging = ref(false)
let dragCounter = 0

// 预览相关
const previewVisible = ref(false)
const previewFile = ref(null)
const previewUrl = ref('')
const previewText = ref('')
const previewCategory = ref('') // image | text | pdf | video | audio | unsupported

const loginForm = reactive({
  username: '',
  password: ''
})

// Token 响应式管理 — 不再需要手动同步 uploadHeaders
const token = computed(() => localStorage.getItem('disk-token') || '')
const uploadHeaders = computed(() => {
  const t = token.value
  return t ? { token: t } : {}
})

// ==================== 初始化 ====================
onMounted(() => {
  const savedToken = localStorage.getItem('disk-token')
  const user = localStorage.getItem('disk-user')
  if (savedToken && user) {
    try {
      isLogin.value = true
      currentUser.value = JSON.parse(user)
      loadFileList()
    } catch {
      // 数据损坏，清除重新登录
      localStorage.removeItem('disk-token')
      localStorage.removeItem('disk-user')
    }
  }
})

// ==================== 登录 / 登出 ====================
const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) return ElMessage.warning('请输入账号和密码')
  try {
    const res = await request.post('/api/user/login', null, { params: loginForm })
    localStorage.setItem('disk-token', res.token)
    localStorage.setItem('disk-user', JSON.stringify(res.userInfo))
    isLogin.value = true
    currentUser.value = res.userInfo
    ElMessage.success('登录成功')
    loadFileList()
  } catch (error) {
    // 错误已由拦截器处理
  }
}

const handleLogout = () => {
  localStorage.removeItem('disk-token')
  localStorage.removeItem('disk-user')
  isLogin.value = false
  fileList.value = []
}

// ==================== 文件列表 ====================
const loadFileList = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/file/list', {
      params: { pageNo: 1, pageSize: 100, parentId: 0 }
    })
    fileList.value = res.records
  } finally {
    loading.value = false
  }
}

// ==================== 上传 ====================
const handleUploadSuccess = (response) => {
  if (response.code === 200) {
    ElMessage.success('上传成功')
    loadFileList()
  } else {
    ElMessage.error(response.msg || '上传失败')
  }
}

const handleUploadError = (error) => {
  ElMessage.error('上传请求失败，请检查网络或后端服务')
}

// ==================== 拖拽上传（支持文件夹） ====================
const onDragEnter = () => {
  dragCounter++
  isDragging.value = true
}

const onDragOver = () => {
  // 保持 dragover 以允许 drop
}

const onDragLeave = () => {
  dragCounter--
  if (dragCounter <= 0) {
    dragCounter = 0
    isDragging.value = false
  }
}

const onDrop = async (e) => {
  dragCounter = 0
  isDragging.value = false

  const items = e.dataTransfer?.items
  if (!items || items.length === 0) return

  const filesToUpload = []

  // 递归遍历文件夹
  const traverseFileTree = (entry) => {
    return new Promise((resolve) => {
      if (entry.isFile) {
        entry.file((file) => {
          filesToUpload.push(file)
          resolve()
        })
      } else if (entry.isDirectory) {
        const dirReader = entry.createReader()
        dirReader.readEntries(async (entries) => {
          for (const childEntry of entries) {
            await traverseFileTree(childEntry)
          }
          resolve()
        })
      }
    })
  }

  // 处理所有拖拽项
  const tasks = []
  for (let i = 0; i < items.length; i++) {
    const entry = items[i].webkitGetAsEntry?.()
    if (entry) {
      tasks.push(traverseFileTree(entry))
    } else {
      // 普通文件拖拽（非文件夹）
      const file = items[i].getAsFile()
      if (file) filesToUpload.push(file)
    }
  }

  await Promise.all(tasks)

  if (filesToUpload.length === 0) {
    ElMessage.warning('未识别到有效文件')
    return
  }

  // 判断是否来自文件夹（保留目录路径信息）
  const isFromFolder = filesToUpload.some(f => f.webkitRelativePath && f.webkitRelativePath.includes('/'))

  if (isFromFolder && filesToUpload.length > 1) {
    // 文件夹上传：使用批量接口
    await uploadFolderFiles(filesToUpload)
  } else {
    // 单文件或多文件：逐个上传
    await uploadMultipleFiles(filesToUpload)
  }
}

// 批量上传文件夹文件
const uploadFolderFiles = async (files) => {
  const formData = new FormData()
  files.forEach((file) => {
    formData.append('files', file)  // 保持相对路径信息
  })

  loading.value = true
  try {
    const res = await request.post('/api/file/upload-folder', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res && Array.isArray(res)) {
      ElMessage.success(`文件夹上传成功，共 ${res.length} 个文件`)
    } else {
      ElMessage.success('文件夹上传成功')
    }
    loadFileList()
  } catch (error) {
    // 分批上传降级方案
    ElMessage.info('批量上传失败，正在逐个上传...')
    await uploadMultipleFiles(files)
  } finally {
    loading.value = false
  }
}

// 逐个上传多个文件
const uploadMultipleFiles = async (files) => {
  let successCount = 0
  let failCount = 0

  for (const file of files) {
    const formData = new FormData()
    formData.append('file', file)

    try {
      await request.post('/api/file/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      successCount++
    } catch {
      failCount++
    }
  }

  if (failCount === 0) {
    ElMessage.success(`全部 ${successCount} 个文件上传成功`)
  } else {
    ElMessage.warning(`上传完成：${successCount} 个成功，${failCount} 个失败`)
  }
  loadFileList()
}

// ==================== 下载 ====================
const handleDownload = (row) => {
  // 使用相对路径，避免硬编码 localhost
  const t = token.value
  const url = `/api/file/download/${row.id}`
  // 通过动态创建 a 标签下载，可以带上 token
  const link = document.createElement('a')
  link.href = url
  link.download = row.fileName
  // 通过请求头传递 token（对于直接下载链接，改用 iframe + token 参数方案）
  // 更可靠的方式：使用 fetch 获取 blob 再下载
  fetch(url, {
    headers: t ? { token: t } : {}
  })
    .then(response => {
      if (!response.ok) throw new Error('下载失败')
      return response.blob()
    })
    .then(blob => {
      const blobUrl = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = blobUrl
      a.download = row.fileName
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(blobUrl)
    })
    .catch(() => {
      ElMessage.error('下载失败，请重试')
    })
}

// ==================== 删除 ====================
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除 [${row.fileName}] 吗？`, '警告', {
    confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
  }).then(async () => {
    await request.delete(`/api/file/delete/${row.id}`)
    ElMessage.success('删除成功')
    loadFileList()
  }).catch(() => {})
}

// ==================== 重命名 ====================
const handleRename = (row) => {
  ElMessageBox.prompt('请输入新的文件名', '重命名', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputValue: row.fileName
  }).then(async ({ value }) => {
    if (!value || value === row.fileName) return
    await request.put('/api/file/rename', null, { params: { id: row.id, newName: value } })
    ElMessage.success('重命名成功')
    loadFileList()
  }).catch(() => {})
}

// ==================== 分享 ====================
const handleShare = async (row) => {
  try {
    const shareUrl = await request.get('/api/file/share', { params: { id: row.id } })
    // 优先使用 Clipboard API，降级使用弹窗
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(shareUrl)
      ElMessage.success('分享链接已复制到剪贴板')
    } else {
      ElMessageBox.alert(shareUrl, '分享链接', { confirmButtonText: '知道了' })
    }
  } catch (e) {
    // 错误已由拦截器处理
  }
}

// ==================== 移动 ====================
const handleMove = (row) => {
  ElMessageBox.prompt('请输入目标文件夹 ID（0 表示根目录）', '移动文件', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputValue: '0'
  }).then(async ({ value }) => {
    const targetParentId = parseInt(value, 10)
    if (isNaN(targetParentId)) {
      ElMessage.warning('请输入有效的文件夹 ID')
      return
    }
    await request.put('/api/file/move', null, { params: { id: row.id, targetParentId } })
    ElMessage.success('移动成功')
    loadFileList()
  }).catch(() => {})
}

// ==================== 新建文件夹 ====================
const handleCreateFolder = () => {
  ElMessageBox.prompt('请输入文件夹名称', '新建文件夹', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputValue: '新建文件夹'
  }).then(async ({ value }) => {
    if (!value || !value.trim()) {
      ElMessage.warning('文件夹名称不能为空')
      return
    }
    await request.post('/api/directory/create', null, { params: { folderName: value.trim(), parentId: 0 } })
    ElMessage.success('文件夹创建成功')
    loadFileList()
  }).catch(() => {})
}

// ==================== 文件预览 ====================
const handlePreview = async (row) => {
  previewFile.value = row
  const ext = (row.fileType || '').toLowerCase()

  // 判断预览类型
  if (['.jpg', '.jpeg', '.png', '.gif', '.webp', '.svg', '.bmp', '.ico'].includes(ext)) {
    previewCategory.value = 'image'
  } else if (['.txt', '.log', '.java', '.py', '.js', '.ts', '.vue', '.css', '.html', '.json', '.xml', '.md', '.yml', '.yaml', '.sh', '.bat'].includes(ext)) {
    previewCategory.value = 'text'
  } else if (ext === '.pdf') {
    previewCategory.value = 'pdf'
  } else if (['.mp4', '.webm', '.ogg'].includes(ext)) {
    previewCategory.value = 'video'
  } else if (['.mp3', '.wav', '.flac', '.aac'].includes(ext)) {
    previewCategory.value = 'audio'
  } else {
    previewCategory.value = 'unsupported'
  }

  // 构造预览 URL（带上 token）
  const t = token.value
  const previewPath = `/api/file/preview/${row.id}`

  if (['image', 'pdf', 'video', 'audio'].includes(previewCategory.value)) {
    // 对于二进制文件类型，通过 fetch 获取 blob URL（可以带 token）
    try {
      const response = await fetch(previewPath, {
        headers: t ? { token: t } : {}
      })
      if (!response.ok) throw new Error('加载失败')
      const blob = await response.blob()
      previewUrl.value = URL.createObjectURL(blob)
    } catch {
      ElMessage.error('加载预览失败')
      previewVisible.value = false
      return
    }
  } else if (previewCategory.value === 'text') {
    // 文本文件：fetch 内容并显示
    try {
      const response = await fetch(previewPath, {
        headers: t ? { token: t } : {}
      })
      if (!response.ok) throw new Error('加载失败')
      previewText.value = await response.text()
    } catch {
      ElMessage.error('加载文本预览失败')
      previewVisible.value = false
      return
    }
  }

  previewVisible.value = true
}

const closePreview = () => {
  // 释放 blob URL
  if (previewUrl.value && previewUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(previewUrl.value)
  }
  previewUrl.value = ''
  previewText.value = ''
  previewFile.value = null
  previewCategory.value = ''
}

// ==================== 工具函数 ====================
const formatSize = (size) => {
  if (!size) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let index = 0
  let displaySize = size
  while (displaySize >= 1024 && index < units.length - 1) {
    displaySize /= 1024
    index++
  }
  // 整数不显示小数位
  return Number.isInteger(displaySize)
    ? displaySize + ' ' + units[index]
    : displaySize.toFixed(2) + ' ' + units[index]
}
</script>

<style scoped>
/* ==================== 基础布局 ==================== */
.app-container {
  height: 100vh;
  background-color: #f5f7fa;
  display: flex;
  justify-content: center;
  align-items: center;
}
.login-box {
  width: 350px;
  padding: 40px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  text-align: center;
}
.login-box h2 {
  margin-bottom: 30px;
  color: #409EFF;
}
.login-btn {
  width: 100%;
  margin-top: 10px;
}
.disk-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}
.header {
  height: 60px;
  background: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 30px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}
.logo {
  font-size: 20px;
  font-weight: bold;
  color: #409EFF;
}
.main-content {
  flex: 1;
  padding: 20px 40px;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
}

/* ==================== 操作栏 ==================== */
.action-bar {
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.action-buttons {
  display: flex;
  align-items: center;
}

/* ==================== 拖拽上传区域 ==================== */
.drop-zone {
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  transition: all 0.3s;
  background: #fafafa;
  cursor: pointer;
}
.drop-zone:hover,
.drop-zone.drop-active {
  border-color: #409EFF;
  background: #ecf5ff;
}
.drop-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: #606266;
}
.drop-hint {
  font-size: 12px;
  color: #909399;
}

/* ==================== 预览对话框 ==================== */
.preview-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
  max-height: 70vh;
  overflow: auto;
}
.preview-image-wrapper {
  text-align: center;
}
.preview-image {
  max-width: 100%;
  max-height: 65vh;
  object-fit: contain;
}
.preview-text-wrapper {
  width: 100%;
}
.preview-text {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 20px;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.6;
  overflow: auto;
  max-height: 60vh;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}
.preview-pdf-wrapper {
  width: 100%;
}
.preview-pdf {
  width: 100%;
  height: 65vh;
}
.preview-video-wrapper {
  width: 100%;
  text-align: center;
}
.preview-video {
  max-width: 100%;
  max-height: 60vh;
}
.preview-audio-wrapper {
  width: 100%;
  text-align: center;
  padding: 40px 0;
}
.preview-audio {
  width: 80%;
}
.preview-unsupported {
  text-align: center;
  padding: 40px;
  color: #909399;
}
.preview-unsupported p {
  margin: 16px 0;
  font-size: 16px;
}
</style>
