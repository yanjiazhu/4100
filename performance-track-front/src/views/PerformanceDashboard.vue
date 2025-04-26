<template>
  <div class="performance-dashboard">
    <div class="dashboard-header">
      <h1>Performance Dashboard</h1>
      <div class="date-selector">
        <el-date-picker
          v-model="searchForm.recordDate"
          type="month"
          placeholder="Select Month"
          format="YYYY/MM"
          value-format="YYYY-MM"
          @change="handleSearch"
        />
      </div>
    </div>

    <!-- 员工表彰部分 -->
    <el-card class="highlight-card" v-if="topPerformer" v-loading="topPerformerLoading">
      <template #header>
        <div class="card-header">
          <span>Employee Highlights</span>
        </div>
      </template>
      <div class="employee-highlight">
        <div class="highlight-header">
          <div class="highlight-title">
            <h3>{{ topPerformer.employeeName }}</h3>
            <p>{{ topPerformer.department }} | Employee ID: {{ topPerformer.employeeId }}</p>
          </div>
          <el-tag type="success" effect="dark" size="large" class="highlight-tag">TOP PERFORMER</el-tag>
        </div>
        <div class="highlight-content">
          <p>Over the past six months, {{ topPerformer.employeeName }} has consistently stood out as a top performer in the {{ topPerformer.department.toLowerCase() }} team. According to our K-Nearest Neighbors (KNN) model, which evaluates employees based on key metrics such as task completion time, accuracy, collaboration score, and customer feedback ratings, {{ topPerformer.employeeName }} ranked in the top 5% of their peer group.</p>
          <p>{{ topPerformer.employeeName }}'s ability to balance speed and quality has made a measurable impact on delivery efficiency and client satisfaction scores. Their performance profile was closely matched with other high-efficiency employees, reinforcing the model's accuracy and highlighting their strong alignment with organizational excellence benchmarks.</p>
          <p>Congratulations to {{ topPerformer.employeeName }} for exemplifying dedication, efficiency, and teamwork. Your consistent performance is inspiring, and we look forward to your continued success!</p>
        </div>
      </div>
    </el-card>

    <el-empty 
      v-if="topPerformerLoading === false && !topPerformer" 
      description="No top performer data available for the past six months"
    >
      <template #image>
        <el-icon style="font-size: 60px"><Trophy /></el-icon>
      </template>
    </el-empty>

    <el-card class="dashboard-card">
      <template #header>
        <div class="card-header">
          <span>Employee Performance Overview - {{ formattedMonth }}</span>
        </div>
      </template>

      <!-- 搜索和筛选区域 -->
      <div class="filter-section">
        <el-form :model="searchForm" inline>
          <el-form-item label="Employee ID">
            <el-input
              v-model="searchForm.employeeID"
              placeholder="Enter Employee ID"
              clearable
            />
          </el-form-item>
          
          <el-form-item label="Employee Name">
            <el-input
              v-model="searchForm.employeeName"
              placeholder="Enter Employee Name"
              clearable
            />
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" @click="handleSearch">
              Search
            </el-button>
            <el-button @click="resetSearch">
              Reset
            </el-button>
          </el-form-item>
        </el-form>
      </div>
      
      <el-table 
        :data="employeeData" 
        style="width: 100%" 
        :default-sort="{ prop: searchForm.sortBy, order: 'descending' }"
        @sort-change="handleSortChange"
        v-loading="loading"
      >
        <el-table-column prop="employeeID" label="Employee ID" sortable="custom" width="120" />
        <el-table-column prop="employeeName" label="Employee Name" sortable="custom" />
        <el-table-column prop="department" label="Department" sortable="custom" />
        <el-table-column prop="kpiCompletion" label="KPI Completion (%)" sortable="custom">
          <template #default="scope">
            <el-progress 
              :percentage="scope.row.kpiCompletion" 
              :color="getKpiColor(scope.row.kpiCompletion)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="attendanceRate" label="Attendance Rate (%)" sortable="custom">
          <template #default="scope">
            <el-progress 
              :percentage="scope.row.attendanceRate" 
              :color="getAttendanceColor(scope.row.attendanceRate)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="overtimeHours" label="Overtime Hours" sortable="custom" />
        <el-table-column prop="performanceRating" label="Performance Rating" sortable="custom">
          <template #default="scope">
            <el-tag :type="getRatingTagType(scope.row.performanceRating)">
              {{ getRatingText(scope.row.performanceRating) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页器 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="searchForm.page"
          v-model:page-size="searchForm.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="detailsVisible"
      title="Performance History"
      width="70%"
    >
      <template v-if="selectedEmployee">
        <div class="employee-details-header">
          <h2>{{ selectedEmployee.name }} - Performance History</h2>
          <p>Employee ID: {{ selectedEmployee.employeeId }} | Department: {{ selectedEmployee.department }}</p>
        </div>
        
        <el-tabs v-model="activeTab">
          <el-tab-pane label="Performance Metrics" name="metrics">
            <el-table :data="performanceHistory" style="width: 100%">
              <el-table-column prop="month" label="Month" sortable />
              <el-table-column prop="kpiCompletion" label="KPI Completion (%)" sortable />
              <el-table-column prop="attendance" label="Attendance Rate (%)" sortable />
              <el-table-column prop="overtimeHours" label="Overtime Hours" sortable />
              <el-table-column prop="performanceRating" label="Rating" sortable>
                <template #default="scope">
                  <el-tag :type="getRatingTagType(scope.row.performanceRating)">
                    {{ scope.row.performanceRating }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          
          <el-tab-pane label="Performance Trend" name="trend">
            <div class="chart-container">
              <!-- Charts would be implemented here using a charting library like ECharts -->
              <p>Performance trend visualization will be implemented here.</p>
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { format, subMonths } from 'date-fns'
import { ElMessage } from 'element-plus'
import { Trophy } from '@element-plus/icons-vue'
import { searchPerformance, getTopPerformer } from '@/api/performance'

export default {
  name: 'PerformanceDashboard',
  components: {
    Trophy
  },
  setup() {
    const loading = ref(false)
    const total = ref(0)
    const employeeData = ref([])
    const detailsVisible = ref(false)
    const selectedEmployee = ref(null)
    const performanceHistory = ref([])
    const activeTab = ref('metrics')
    const topPerformer = ref(null)
    const topPerformerLoading = ref(false)

    // 获取上一个月的日期
    const getLastMonth = () => {
      // 使用 date-fns 的 subMonths 函数获取上一个月
      const lastMonth = subMonths(new Date(), 1)
      return format(lastMonth, 'yyyy-MM')
    }

    // 搜索表单，默认选择上一个月
    const searchForm = ref({
      employeeID: '',
      employeeName: '',
      recordDate: getLastMonth(),
      page: 1,
      size: 10,
      sortBy: 'performanceRating'
    })

    const formattedMonth = computed(() => {
      if (!searchForm.value.recordDate) return ''
      const [year, month] = searchForm.value.recordDate.split('-')
      return `${year}/${month}`
    })

    // 获取优秀员工数据
    const fetchTopPerformer = async () => {
      try {
        topPerformerLoading.value = true
        const response = await getTopPerformer()
        if (response && response.data) {
          topPerformer.value = response.data
          console.log('Top performer data:', topPerformer.value)
        } else {
          console.log('No top performer data available')
        }
      } catch (error) {
        console.error('Failed to fetch top performer:', error)
      } finally {
        topPerformerLoading.value = false
      }
    }

    // 获取数据的方法
    const fetchData = async () => {
      try {
        loading.value = true
        const response = await searchPerformance({
          ...searchForm.value,
          page: searchForm.value.page - 1 // 后端从0开始计数
        })
        employeeData.value = response.content
        total.value = response.totalElements
      } catch (error) {
        ElMessage.error('Failed to fetch performance data')
      } finally {
        loading.value = false
      }
    }

    // 搜索处理
    const handleSearch = () => {
      searchForm.value.page = 1
      fetchData()
    }

    // 重置搜索
    const resetSearch = () => {
      // 保存当前选择的月份
      const currentRecordDate = searchForm.value.recordDate
      
      searchForm.value = {
        employeeID: '',
        employeeName: '',
        recordDate: currentRecordDate, // 保留用户选择的月份值
        page: 1,
        size: 10,
        sortBy: 'performanceRating'
      }
      fetchData()
    }

    // 分页处理
    const handleSizeChange = (val) => {
      searchForm.value.size = val
      fetchData()
    }

    const handleCurrentChange = (val) => {
      searchForm.value.page = val
      fetchData()
    }

    // 排序处理
    const handleSortChange = ({ prop, order }) => {
      searchForm.value.sortBy = prop
      fetchData()
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

    const showEmployeeDetails = (row) => {
      selectedEmployee.value = row
      // In a real app, this would fetch historical data from an API
      // Simulating data for demonstration
      performanceHistory.value = [
        {
          month: '2023/11',
          kpiCompletion: 88,
          attendance: 96,
          overtimeHours: 10,
          performanceRating: 'Good'
        },
        {
          month: '2023/12',
          kpiCompletion: 90,
          attendance: 97,
          overtimeHours: 12,
          performanceRating: 'Good'
        },
        {
          month: formattedMonth.value,
          kpiCompletion: row.kpiCompletion,
          attendance: row.attendance,
          overtimeHours: row.overtimeHours,
          performanceRating: row.performanceRating
        }
      ]
      detailsVisible.value = true
    }

    onMounted(() => {
      fetchData()
      fetchTopPerformer() // 获取优秀员工数据
    })

    return {
      formattedMonth,
      loading,
      topPerformerLoading,
      total,
      searchForm,
      employeeData,
      detailsVisible,
      selectedEmployee,
      performanceHistory,
      activeTab,
      topPerformer,
      handleSearch,
      resetSearch,
      handleSizeChange,
      handleCurrentChange,
      handleSortChange,
      getKpiColor,
      getAttendanceColor,
      getRatingTagType,
      getRatingText,
      showEmployeeDetails
    }
  }
}
</script>

<style scoped>
.performance-dashboard {
  max-width: 100%;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.dashboard-header h1 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.date-selector {
  min-width: 200px;
}

.date-selector :deep(.el-input__wrapper) {
  padding: 0 15px;
  height: 42px;
  font-size: 16px;
  font-weight: 500;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
  transition: all 0.3s;
}

.date-selector :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #409eff inset;
}

.date-selector :deep(.el-input__inner) {
  font-weight: 500;
  color: #303133;
}

.date-selector :deep(.el-input__prefix) {
  font-size: 18px;
}

.dashboard-card, .highlight-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 500;
}

.employee-details-header {
  margin-bottom: 20px;
}

.employee-details-header h2 {
  margin: 0 0 10px 0;
  font-size: 20px;
}

.chart-container {
  height: 400px;
  margin-top: 20px;
}

.el-table {
  --el-table-header-bg-color: #f5f7fa;
  cursor: pointer;
}

.el-table .el-table__row:hover {
  background-color: #f0f7ff;
}

/* 员工表彰样式 */
.employee-highlight {
  padding: 10px 0;
}

.highlight-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.highlight-title {
  margin-left: 15px;
  flex: 1;
}

.highlight-title h3 {
  margin: 0 0 5px 0;
  font-size: 18px;
}

.highlight-title p {
  margin: 0;
  color: #606266;
}

.highlight-tag {
  font-weight: bold;
  letter-spacing: 1px;
}

.highlight-content {
  line-height: 1.6;
  color: #303133;
  padding: 0 10px;
}

.highlight-content p {
  margin-bottom: 15px;
}

.filter-section {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style> 