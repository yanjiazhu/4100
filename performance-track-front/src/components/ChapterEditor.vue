<template>
  <div class="template-editor">
    <div class="editor-header">
      <h2>{{ templateData.title || '新建模板' }}</h2>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </div>
    <div class="editor-content">
      <div class="sidebar">
        <el-button @click="addChapter">添加章节</el-button>
        <ul>
          <li v-for="chapter in templateData.chapters" :key="chapter.id" @click="selectChapter(chapter)">
            {{ chapter.title }}
          </li>
        </ul>
      </div>
      <div class="main-content">
        <chapter-editor v-if="selectedChapter" :chapter="selectedChapter" @update="updateChapter" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ChapterEditor from '@/components/ChapterEditor'
import { getTemplateById, saveTemplate } from '@/api/templates'

const route = useRoute()
const router = useRouter()
const templateData = ref({
  title: '',
  chapters: []
})
const selectedChapter = ref(null)

const fetchTemplate = async () => {
  if (!route.params.id) {
    templateData.value = {
      title: '',
      chapters: []
    }
    return
  }

  try {
    templateData.value = await getTemplateById(route.params.id)
  } catch (error) {
    ElMessage.error('获取模板失败')
  }
}

const handleSave = async () => {
  try {
    await saveTemplate(templateData.value)
    ElMessage.success('模板保存成功')
    router.push('/admin/templates')
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const addChapter = () => {
  const newChapter = {
    id: Date.now(),
    title: `章节 ${templateData.value.chapters.length + 1}`,
    content: '<p>新章节内容</p>'
  }
  templateData.value.chapters.push(newChapter)
  selectedChapter.value = newChapter
}

const selectChapter = (chapter) => {
  selectedChapter.value = chapter
}

const updateChapter = (updatedChapter) => {
  const index = templateData.value.chapters.findIndex(ch => ch.id === updatedChapter.id)
  if (index !== -1) {
    templateData.value.chapters[index] = updatedChapter
  }
}

onMounted(() => {
  fetchTemplate()
})
</script>

<style scoped>
.template-editor {
  padding: 20px;
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
}

.editor-content {
  display: flex;
}

.sidebar {
  width: 200px;
  border-right: 1px solid #ddd;
  padding-right: 20px;
}

.main-content {
  flex: 1;
  padding-left: 20px;
}
</style> 