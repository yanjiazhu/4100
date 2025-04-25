import request from '@/utils/request'

// 搜索员工绩效数据
export function searchPerformance(data) {
  return request({
    url: '/employee-performance/search',
    method: 'post',
    data
  })
}

// 获取过去六个月的优秀员工
export function getTopPerformer() {
  return request({
    url: '/employee-performance/top-performer',
    method: 'get'
  })
}

// 预览Excel文件（不保存）
export function previewPerformanceExcel(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/employee-performance/preview',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 上传并保存Excel文件
export function savePerformanceExcel(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/employee-performance/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
} 