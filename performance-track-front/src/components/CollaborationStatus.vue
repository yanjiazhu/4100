<template>
  <div class="collaboration-status">
    <!-- 在线协作者列表 -->
    <div class="collaborators-list">
      <el-tooltip 
        v-for="user in onlineUsers" 
        :key="user.id"
        :content="`${user.name}${user.editing ? ' - 正在编辑' + user.editing : ''}`"
        placement="top"
      >
        <el-avatar 
          :size="28" 
          :class="{ 'is-editing': user.editing }"
          :style="{ backgroundColor: user.color }">
          {{ user.name.substring(0, 1) }}
        </el-avatar>
      </el-tooltip>
    </div>

    <!-- 章节锁定状态 -->
    <div v-if="currentChapter" class="chapter-status">
      <el-tag v-if="chapterLock" type="warning" size="small">
        <el-icon><Lock /></el-icon>
        {{ chapterLock.user.name }} 正在编辑
      </el-tag>
      <el-button 
        v-else-if="canLock" 
        type="primary" 
        link 
        size="small"
        @click="lockChapter">
        锁定章节
      </el-button>
    </div>

    <!-- 历史版本抽屉 -->
    <el-drawer
      v-model="historyVisible"
      title="修订历史"
      size="60%"
    >
      <div class="history-content">
        <div class="history-list">
          <el-timeline>
            <el-timeline-item
              v-for="version in versionHistory"
              :key="version.id"
              :timestamp="version.time"
              :type="version.type"
            >
              <div class="version-item">
                <div class="version-header">
                  <span class="version-user">{{ version.user.name }}</span>
                  <el-tag size="small">{{ version.type === 'primary' ? '保存' : '自动保存' }}</el-tag>
                </div>
                <div class="version-diff" v-html="version.diff"></div>
                <div class="version-actions">
                  <el-button type="primary" link size="small" @click="previewVersion(version)">
                    预览
                  </el-button>
                  <el-button type="primary" link size="small" @click="restoreVersion(version)">
                    恢复此版本
                  </el-button>
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
    </el-drawer>

    <!-- 评论抽屉 -->
    <el-drawer
      v-model="commentsVisible"
      title="评论与讨论"
      size="40%"
    >
      <div class="comments-content">
        <div class="comments-list">
          <div v-for="comment in comments" :key="comment.id" class="comment-item">
            <div class="comment-header">
              <el-avatar :size="24">{{ comment.user.name.substring(0, 1) }}</el-avatar>
              <span class="comment-user">{{ comment.user.name }}</span>
              <span class="comment-time">{{ comment.time }}</span>
            </div>
            <div class="comment-body">{{ comment.content }}</div>
            <div class="comment-actions">
              <el-button type="primary" link size="small" @click="replyComment(comment)">
                回复
              </el-button>
            </div>
            <!-- 回复列表 -->
            <div v-if="comment.replies?.length" class="reply-list">
              <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                <div class="reply-header">
                  <el-avatar :size="20">{{ reply.user.name.substring(0, 1) }}</el-avatar>
                  <span class="reply-user">{{ reply.user.name }}</span>
                  <span class="reply-time">{{ reply.time }}</span>
                </div>
                <div class="reply-body">{{ reply.content }}</div>
              </div>
            </div>
          </div>
        </div>
        <!-- 添加评论 -->
        <div class="add-comment">
          <el-input
            v-model="newComment"
            type="textarea"
            rows="3"
            placeholder="添加评论..."
          />
          <el-button type="primary" @click="addComment">发表评论</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { Lock } from '@element-plus/icons-vue'

const props = defineProps({
  currentChapter: Object,
  documentId: String
})

// 在线用户
const onlineUsers = ref([
  { id: 1, name: '张三', color: '#1890ff', editing: '第一章' },
  { id: 2, name: '李四', color: '#52c41a' },
  { id: 3, name: '王五', color: '#722ed1', editing: '第三章' }
])

// 章节锁定状态
const chapterLock = ref(null)
const canLock = ref(true)

// 版本历史
const historyVisible = ref(false)
const versionHistory = ref([
  {
    id: 1,
    time: '2024-03-21 10:00:00',
    user: { name: '张三' },
    type: 'primary',
    diff: '<span class="diff-add">添加了新内容</span>'
  }
])

// 评论功能
const commentsVisible = ref(false)
const comments = ref([
  {
    id: 1,
    user: { name: '张三' },
    time: '2024-03-21 10:00:00',
    content: '这部分内容需要补充更多细节',
    replies: [
      {
        id: 2,
        user: { name: '李四' },
        time: '2024-03-21 10:05:00',
        content: '好的，我来补充'
      }
    ]
  }
])
const newComment = ref('')

// 锁定章节
const lockChapter = () => {
  // TODO: 调用API锁定章节
}

// 添加评论
const addComment = () => {
  if (!newComment.value) return
  
  comments.value.unshift({
    id: Date.now(),
    user: { name: '我' },
    time: new Date().toLocaleString(),
    content: newComment.value,
    replies: []
  })
  newComment.value = ''
}

// 回复评论
const replyComment = (comment) => {
  // TODO: 实现回复功能
}

// 预览历史版本
const previewVersion = (version) => {
  // TODO: 实现预览功能
}

// 恢复历史版本
const restoreVersion = (version) => {
  // TODO: 实现恢复功能
}

// 生命周期钩子
onMounted(() => {
  // TODO: 建立WebSocket连接，监听协作状态
})

onUnmounted(() => {
  // TODO: 关闭WebSocket连接
})
</script>

<style scoped>
.collaboration-status {
  margin-bottom: 16px;
}

.collaborators-list {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.is-editing {
  border: 2px solid #52c41a;
}

.chapter-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.history-content,
.comments-content {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.version-item {
  margin-bottom: 16px;
}

.version-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.version-diff {
  background-color: #f8f9fa;
  padding: 8px;
  border-radius: 4px;
  margin-bottom: 8px;
}

.diff-add {
  background-color: #d4f7d4;
}

.diff-remove {
  background-color: #ffd4d4;
  text-decoration: line-through;
}

.comment-item {
  margin-bottom: 24px;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.comment-time {
  color: #999;
  font-size: 12px;
}

.reply-list {
  margin-left: 32px;
  margin-top: 8px;
}

.reply-item {
  margin-bottom: 8px;
  background-color: #f8f9fa;
  padding: 8px;
  border-radius: 4px;
}

.add-comment {
  margin-top: auto;
  padding: 16px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style> 