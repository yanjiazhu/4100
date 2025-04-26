<template>
  <el-form
    ref="formRef"
    :model="formData"
    :rules="rules"
    label-position="top"
    size="large"
  >
    <el-form-item label="管理员账号" prop="username">
      <el-input 
        v-model="formData.username"
        :prefix-icon="User"
        placeholder="请输入管理员账号"
      />
    </el-form-item>
    
    <el-form-item label="密码" prop="password">
      <el-input
        v-model="formData.password"
        type="password"
        show-password
        :prefix-icon="Lock"
        placeholder="请输入密码"
      />
    </el-form-item>
    
    <el-form-item>
      <el-button 
        type="primary" 
        class="submit-btn"
        @click="handleSubmit" 
        :loading="loading"
      >
        登录
      </el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { loginAdmin } from '@/api/user'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const formData = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入管理员账号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const data = await loginAdmin(formData.username, formData.password)
      if (data === "User account is inactive") {
        ElMessage.error('登录失败, 用户被停用。')
      } else if (data === "Invalid username or password") {
        ElMessage.error('用户名或密码错误')
      } else {
        localStorage.setItem('username', data.username)
        localStorage.setItem('name', data.name)
        localStorage.setItem('userId', data.id)
        ElMessage.success('登录成功')
        router.push('/admin')
        emit('success')
        return
      }
    } catch (error) {
      // ElMessage.error('登录失败')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.submit-btn {
  width: 100%;
  padding: 12px;
  font-size: 16px;
}
</style> 