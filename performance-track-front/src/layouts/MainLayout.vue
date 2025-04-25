<template>
  <div class="main-layout">
    <div class="sidebar">
      <div class="logo">
        <h2>Performance Track</h2>
      </div>
      <div class="nav-menu">
        <router-link to="/performance" class="nav-item" active-class="active">
          <el-icon><DataLine /></el-icon>
          <span>Performance Dashboard</span>
        </router-link>
        <router-link to="/upload" class="nav-item" active-class="active">
          <el-icon><Upload /></el-icon>
          <span>Upload Data</span>
        </router-link>
      </div>
    </div>
    <div class="main-content">
      <div class="header">
        <div class="header-content">
          <h2>Employee Performance Management</h2>
          <el-button 
            type="text" 
            class="logout-btn"
            @click="handleLogout"
          >
            <el-icon><SwitchButton /></el-icon>
            <span>Logout</span>
          </el-button>
        </div>
      </div>
      <div class="content-container">
        <router-view></router-view>
      </div>
    </div>
  </div>
</template>

<script>
import { DataLine, Upload, SwitchButton } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

export default {
  name: 'MainLayout',
  components: {
    DataLine,
    Upload,
    SwitchButton
  },
  setup() {
    const router = useRouter()

    const handleLogout = () => {
      // Clear any stored user data
      localStorage.clear()
      // Redirect to login page
      router.push('/login')
    }

    return {
      handleLogout
    }
  }
}
</script>

<style scoped>
.main-layout {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: 250px;
  background-color: #304156;
  color: white;
  display: flex;
  flex-direction: column;
}

.logo {
  padding: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.nav-menu {
  padding: 20px 0;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  color: #bfcbd9;
  text-decoration: none;
  transition: all 0.3s;
}

.nav-item:hover, .nav-item.active {
  background-color: #263445;
  color: #409EFF;
}

.nav-item .el-icon {
  margin-right: 10px;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

.header {
  background-color: white;
  padding: 15px 30px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: normal;
  color: #606266;
}

.logout-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #606266;
  transition: color 0.3s;
}

.logout-btn:hover {
  color: #f56c6c;
}

.logout-btn .el-icon {
  font-size: 16px;
}

.content-container {
  padding: 25px;
  flex: 1;
  overflow: auto;
}
</style> 