import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '',
  timeout: 30000 // 上传大文件需要更长超时时间
})

// 请求拦截器：自动带上 Token
request.interceptors.request.use(config => {
  const token = localStorage.getItem('disk-token')
  if (token) {
    config.headers['token'] = token
  }
  return config
}, error => Promise.reject(error))

// 响应拦截器：统一处理返回体
request.interceptors.response.use(response => {
  const res = response.data
  if (res.code === 200) {
    return res.data // 成功直接返回核心数据
  } else if (res.code === 401) {
    // Token 过期或无效 → 清除登录状态并跳转登录页
    ElMessage.error('登录已过期，请重新登录')
    localStorage.removeItem('disk-token')
    localStorage.removeItem('disk-user')
    window.location.reload()
    return Promise.reject(new Error(res.msg || '未授权'))
  } else {
    ElMessage.error(res.msg || '操作失败')
    return Promise.reject(new Error(res.msg || '操作失败'))
  }
}, error => {
  // 网络错误或 HTTP 错误
  if (error.response) {
    const status = error.response.status
    if (status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      localStorage.removeItem('disk-token')
      localStorage.removeItem('disk-user')
      window.location.reload()
    } else if (status === 413) {
      ElMessage.error('文件太大，超过系统限制')
    } else if (status >= 500) {
      ElMessage.error('服务器错误，请稍后再试')
    } else {
      ElMessage.error('网络请求失败')
    }
  } else {
    ElMessage.error('网络连接失败，请检查网络')
  }
  return Promise.reject(error)
})

export default request
