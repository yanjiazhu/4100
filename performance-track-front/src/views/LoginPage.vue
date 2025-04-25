<template>
  <div class="login-container">
    <div class="login-content">
      <!-- 左侧公司信息区域 -->
      <div class="company-section">
        <div class="logo-container">
          <img src="../assets/logo.png" alt="Kleinfelder Water Technologies Logo" class="company-logo">
        </div>
        
        <div class="company-intro">
          <h2 class="welcome-text">Welcome to <span class="highlight-text">Performance Track</span></h2>
          
          <div class="company-description">
            <p>Founded in 1961, <span class="highlight-text-alt">Kleinfelder</span> is an integrated, cross-disciplinary team that embraces curiosity, innovation, and unwavering commitment to doing the right thing. Our offices span the U.S., Canada, and Australia, where we tackle diverse projects with passion and precision.</p>
            <p>As a part of this passion for growth, Kleinfelder acquired <span class="highlight-text-alt">Industrial Fluid Management</span> in 2020. Established in 1992, IFM was a water cycle company that specialized in the design and installation of ultra-pure water equipment, ultra filtration, and wastewater systems.</p>
            <p><span class="highlight-text">Kleinfelder Water Technologies</span> emerges from the fusion of IFM and Kleinfelder. Our legacy of excellence continues as we combine expertise, creativity, and dedication. From water treatment to environmental solutions, we're here to make a lasting impact.</p>
          </div>
        </div>
        
        <div class="login-footer desktop-only">
          <p>© {{ currentYear }} Kleinfelder Water Technologies</p>
        </div>
      </div>
      
      <!-- 右侧登录区域 -->
      <div class="login-section">
        <div class="login-box">
          <h1 class="login-title">Login to System</h1>
          
          <el-form class="login-form" :model="loginForm" ref="loginFormRef">
            <el-form-item prop="username">
              <el-input 
                v-model="loginForm.username" 
                placeholder="Username"
                prefix-icon="User"
                size="large"
              />
            </el-form-item>
            
            <el-form-item prop="password">
              <el-input 
                v-model="loginForm.password" 
                placeholder="Password" 
                type="password"
                prefix-icon="Lock" 
                show-password
                size="large"
              />
            </el-form-item>
            
            <el-form-item>
              <el-button 
                type="primary" 
                class="login-button" 
                @click="handleLogin" 
                size="large"
                :loading="loading"
              >
                Login
              </el-button>
            </el-form-item>
          </el-form>
          
          <div class="login-footer mobile-only">
            <p>© {{ currentYear }} Kleinfelder Water Technologies</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { login } from '@/api/user'

export default {
  name: 'LoginPage',
  components: {
    User,
    Lock
  },
  setup() {
    const router = useRouter()
    const loginFormRef = ref(null)
    const loading = ref(false)
    
    const loginForm = ref({
      username: '',
      password: ''
    })
    
    const currentYear = computed(() => {
      return new Date().getFullYear()
    })
    
    const handleLogin = async () => {
      if (!loginFormRef.value) return
      
      try {
        loading.value = true
        const response = await login(loginForm.value)
        
        if (response === "Login successful") {
          ElMessage.success('Login successful')
          router.push('/performance')
        } else {
          ElMessage.error('Invalid username or password')
        }
      } catch (error) {
        if (error.response?.data) {
          ElMessage.error(error.response.data)
        } else {
          ElMessage.error('Login failed')
        }
      } finally {
        loading.value = false
      }
    }
    
    return {
      loginForm,
      loginFormRef,
      currentYear,
      handleLogin,
      loading
    }
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f0f2f5;
  background-image: linear-gradient(135deg, #f5f7fa 0%, #d5e5ec 100%);
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.login-container::before {
  content: '';
  position: absolute;
  top: -5%;
  right: -5%;
  width: 500px;
  height: 500px;
  border-radius: 50%;
  background-color: rgba(7, 93, 86, 0.05);
  z-index: 0;
}

.login-container::after {
  content: '';
  position: absolute;
  bottom: -5%;
  left: -5%;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background-color: rgba(164, 197, 83, 0.05);
  z-index: 0;
}

.login-content {
  display: flex;
  max-width: 1200px;
  width: 100%;
  min-height: 600px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  position: relative;
  z-index: 1;
}

/* 左侧公司信息区域 */
.company-section {
  flex: 1.4;
  background: linear-gradient(135deg, #075d56, #0c8f7c);
  color: white;
  padding: 40px;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.company-section::before {
  content: '';
  position: absolute;
  top: -100px;
  right: -100px;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(164, 197, 83, 0.2) 0%, rgba(164, 197, 83, 0) 70%);
}

.company-section::after {
  content: '';
  position: absolute;
  bottom: -50px;
  left: -50px;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0) 70%);
}

.logo-container {
  text-align: center;
  margin-bottom: 40px;
  position: relative;
  z-index: 2;
}

.company-logo {
  max-width: 220px;
  height: auto;
  filter: drop-shadow(0 4px 6px rgba(0, 0, 0, 0.1));
}

.company-intro {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  z-index: 2;
}

.welcome-text {
  font-size: 32px;
  margin-bottom: 30px;
  font-weight: 500;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  position: relative;
}

.welcome-text::after {
  content: '';
  position: absolute;
  bottom: -10px;
  left: 0;
  width: 60px;
  height: 3px;
  background-color: #a4c553;
  border-radius: 3px;
}

.highlight-text {
  color: #a4c553;
  font-weight: 600;
}

.highlight-text-alt {
  color: #d9e9a0;
  font-weight: 600;
}

.company-description {
  font-size: 15px;
  line-height: 1.7;
  overflow-y: auto;
  max-height: 300px;
  padding-right: 15px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.company-description p {
  margin-bottom: 16px;
  position: relative;
  padding-left: 12px;
}

.company-description p::before {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background-color: #a4c553;
}

/* 右侧登录区域 */
.login-section {
  flex: 1;
  background: white;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
}

.login-section::before {
  content: '';
  position: absolute;
  top: 20px;
  left: 20px;
  right: 20px;
  bottom: 20px;
  border: 1px solid rgba(7, 93, 86, 0.05);
  border-radius: 8px;
  pointer-events: none;
}

.login-box {
  width: 100%;
  max-width: 360px;
  padding: 40px 20px;
  position: relative;
  z-index: 2;
}

.login-title {
  font-size: 26px;
  margin-bottom: 40px;
  text-align: center;
  color: #303133;
  font-weight: 500;
  position: relative;
}

.login-title::after {
  content: '';
  position: absolute;
  bottom: -12px;
  left: 50%;
  transform: translateX(-50%);
  width: 40px;
  height: 3px;
  background-color: #0c8f7c;
  border-radius: 3px;
}

.login-form {
  width: 100%;
}

:deep(.el-input__wrapper) {
  padding: 0 15px;
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.1) inset;
  transition: all 0.3s;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(12, 143, 124, 0.3) inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #0c8f7c inset !important;
}

:deep(.el-input__inner) {
  height: 44px;
}

.login-button {
  width: 100%;
  height: 44px;
  border-radius: 4px;
  font-size: 16px;
  font-weight: 500;
  background-color: #0c8f7c;
  border-color: #0c8f7c;
  margin-top: 20px;
  box-shadow: 0 4px 12px rgba(12, 143, 124, 0.2);
  transition: all 0.3s;
}

.login-button:hover {
  background-color: #075d56;
  border-color: #075d56;
  transform: translateY(-2px);
  box-shadow: 0 6px 15px rgba(12, 143, 124, 0.3);
}

.login-footer {
  text-align: center;
  margin-top: 30px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.login-section .login-footer {
  color: #909399;
}

.desktop-only {
  display: block;
}

.mobile-only {
  display: none;
}

/* 响应式布局 */
@media (max-width: 992px) {
  .login-content {
    flex-direction: column;
    height: auto;
    max-width: 450px;
  }
  
  .company-section {
    padding: 30px;
  }
  
  .company-description {
    max-height: 200px;
  }
  
  .login-box {
    padding: 30px 20px;
  }
  
  .desktop-only {
    display: none;
  }
  
  .mobile-only {
    display: block;
  }
}

/* 美化滚动条 */
.company-description::-webkit-scrollbar {
  width: 6px;
}

.company-description::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
}

.company-description::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 3px;
}

.company-description::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.5);
}
</style> 