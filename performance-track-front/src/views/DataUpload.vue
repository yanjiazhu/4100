<template>
  <div class="data-upload">
    <div class="upload-header">
      <h1>Performance Data Upload</h1>
    </div>
    
    <el-card class="upload-card">
      <template #header>
        <div class="card-header">
          <span>Upload Monthly Performance Data</span>
        </div>
      </template>
      
      <el-form :model="uploadForm">
        <el-form-item label="Performance Data File">
          <el-upload
            class="upload-area"
            drag
            action="#"
            :auto-upload="false"
            :on-change="handleFileChange"
            :limit="1"
            :file-list="fileList"
            ref="uploadRef"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              Drop file here or <em>click to upload</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                Please upload Excel files containing employee performance data
              </div>
            </template>
          </el-upload>
        </el-form-item>
        
        <el-button type="primary" @click="submitUpload" :disabled="!uploadForm.file" :loading="uploading">
          Preview Data
        </el-button>
      </el-form>
    </el-card>
    
    <el-card v-if="previewData.monthlyPerformances && previewData.monthlyPerformances.length > 0" class="result-card">
      <template #header>
        <div class="card-header">
          <span>Preview Results - {{ previewData.fileName }}</span>
          <el-tag type="info">{{ previewData.recordCount }} records</el-tag>
        </div>
      </template>
      
      <div class="processed-data">
        <h3>Data Preview (Not Yet Saved)</h3>
        
        <el-table :data="previewData.monthlyPerformances" style="width: 100%">
          <el-table-column prop="employeeID" label="Employee ID" sortable width="120" />
          <el-table-column prop="employeeName" label="Employee Name" sortable />
          <el-table-column prop="department" label="Department" sortable />
          <el-table-column prop="kpiCompletion" label="KPI Completion (%)" sortable>
            <template #default="scope">
              <el-progress 
                :percentage="scope.row.kpiCompletion" 
                :color="getKpiColor(scope.row.kpiCompletion)"
              />
            </template>
          </el-table-column>
          <el-table-column prop="attendanceRate" label="Attendance Rate (%)" sortable>
            <template #default="scope">
              <el-progress 
                :percentage="scope.row.attendanceRate" 
                :color="getAttendanceColor(scope.row.attendanceRate)"
              />
            </template>
          </el-table-column>
          <el-table-column prop="overtimeHours" label="Overtime Hours" sortable />
          <el-table-column prop="performanceRating" label="Performance Rating" sortable>
            <template #default="scope">
              <el-tag :type="getRatingTagType(scope.row.performanceRating)">
                {{ getRatingText(scope.row.performanceRating) }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        
        <div class="actions-row">
          <el-button type="success" @click="confirmData" :loading="saving">
            Confirm and Save to Database
          </el-button>
          <el-button @click="resetUpload">
            Cancel and Reset
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { previewPerformanceExcel, savePerformanceExcel } from '@/api/performance'

export default {
  name: 'DataUpload',
  components: {
    UploadFilled
  },
  setup() {
    const uploadForm = ref({
      file: null
    })
    
    const uploading = ref(false)
    const saving = ref(false)
    const fileList = ref([])
    const uploadRef = ref(null)
    const previewData = ref({
      monthlyPerformances: [],
      fileName: '',
      recordCount: 0
    })
    const originalFile = ref(null)
    
    const handleFileChange = (file, fileList) => {
      uploadForm.value.file = file
      originalFile.value = file.raw
      if (fileList.length > 1) {
        fileList.splice(0, fileList.length - 1)
      }
    }
    
    const submitUpload = async () => {
      if (!uploadForm.value.file) {
        ElMessage.warning('Please upload a file')
        return
      }
      
      try {
        uploading.value = true
        const response = await previewPerformanceExcel(uploadForm.value.file.raw)
        previewData.value = response
        ElMessage.success(`Successfully processed ${response.recordCount} records from ${response.fileName}`)
        
        if (uploadRef.value) {
          uploadRef.value.clearFiles()
        }
        fileList.value = []
      } catch (error) {
        if (error.response && error.response.status === 409) {
          const errorData = error.response.data
          if (errorData && errorData.alreadyExists) {
            ElMessage({
              type: 'warning',
              message: `${errorData.message}`,
              duration: 5000,
              showClose: true
            })
          } else {
            ElMessage.error('Failed to process file')
          }
        } else {
          ElMessage.error('Failed to process file')
        }
        resetUpload()
      } finally {
        uploading.value = false
      }
    }
    
    const getKpiColor = (value) => {
      if (value < 70) return '#F56C6C'
      if (value < 85) return '#E6A23C'
      return '#67C23A'
    }
    
    const getAttendanceColor = (value) => {
      if (value < 85) return '#F56C6C'
      if (value < 95) return '#E6A23C'
      return '#67C23A'
    }
    
    const getRatingTagType = (rating) => {
      const types = {
        3: 'success',
        2: 'warning',
        1: 'danger'
      }
      return types[rating] || 'info'
    }

    const getRatingText = (rating) => {
      const texts = {
        3: 'Excellent',
        2: 'Good',
        1: 'Poor'
      }
      return texts[rating] || 'Unknown'
    }
    
    const confirmData = async () => {
      if (!originalFile.value) {
        ElMessage.error('Original file reference lost, please upload again')
        return
      }
      
      try {
        ElMessageBox.confirm(
          `Are you sure you want to save these ${previewData.value.recordCount} records for ${previewData.value.fileName}?`,
          'Confirm Save',
          {
            confirmButtonText: 'Save',
            cancelButtonText: 'Cancel',
            type: 'warning'
          }
        ).then(async () => {
          try {
            saving.value = true
            await savePerformanceExcel(originalFile.value)
            ElMessage.success(`Data from ${previewData.value.fileName} has been saved to database`)
            resetUpload()
          } catch (error) {
            ElMessage.error('Failed to save data to database')
          } finally {
            saving.value = false
          }
        }).catch(() => {
          ElMessage.info('Save cancelled')
        })
      } catch (error) {
        ElMessage.error('An error occurred')
      }
    }
    
    const resetUpload = () => {
      uploadForm.value = {
        file: null
      }
      originalFile.value = null
      previewData.value = {
        monthlyPerformances: [],
        fileName: '',
        recordCount: 0
      }
      if (uploadRef.value) {
        uploadRef.value.clearFiles()
      }
      fileList.value = []
    }
    
    return {
      uploadForm,
      uploading,
      saving,
      fileList,
      uploadRef,
      previewData,
      handleFileChange,
      submitUpload,
      getKpiColor,
      getAttendanceColor,
      getRatingTagType,
      getRatingText,
      confirmData,
      resetUpload
    }
  }
}
</script>

<style scoped>
.data-upload {
  max-width: 100%;
}

.upload-header {
  margin-bottom: 20px;
}

.upload-header h1 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.upload-card, .result-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 500;
}

.upload-area {
  width: 100%;
}

.processed-data h3 {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 18px;
  color: #303133;
}

.actions-row {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.el-progress {
  margin-top: 15px;
}
</style> 