<template>
  <el-form
    ref="formRef"
    :model="formData"
    :rules="rules"
    label-position="top"
    size="large"
  >
    <el-form-item label="用户名" prop="username">
      <el-input 
        v-model="formData.username"
        :prefix-icon="User"
        placeholder="请输入用户名"
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
import { ref, reactive, toRefs } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { loginUser } from '@/api/user'

const router = useRouter()
const emit = defineEmits(['success'])
const formRef = ref(null)
const loading = ref(false)

const formData = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
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
      const data = await loginUser(formData.username, formData.password)
      if (data === "User account is inactive") {
        ElMessage.error('登录失败, 用户被停用。')
      } else if (data === "Invalid username or password") {
        ElMessage.error('用户名或密码错误')
      } else {
        localStorage.setItem('username', data.username)
        localStorage.setItem('name', data.name)
        localStorage.setItem('userId', data.id)
        ElMessage.success('登录成功')
        router.push('/home')
        emit('success')
      }
    } catch (error) {
      ElMessage.error('登录失败')
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