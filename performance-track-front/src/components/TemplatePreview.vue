<template>
  <div class="template-preview">
    <h1>{{ data.title }}</h1>
    <div v-for="chapter in data.chapters" :key="chapter.id">
      <h2>{{ chapter.title }}</h2>
      <div class="chapter-content" v-html="chapter.content" />
      <div v-if="chapter.children?.length" class="sub-chapters">
        <div v-for="subChapter in chapter.children" :key="subChapter.id">
          <h3>{{ subChapter.title }}</h3>
          <div class="chapter-content" v-html="subChapter.content" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  data: {
    type: Object,
    required: true
  }
})
</script>

<style scoped>
.template-preview {
  padding: 40px;
  max-width: 800px;
  margin: 0 auto;
}

h1 {
  text-align: center;
  margin-bottom: 40px;
  color: #1890ff;
  font-size: 28px;
}

h2 {
  margin: 24px 0 16px;
  color: #1890ff;
  font-size: 20px;
}

h3 {
  margin: 16px 0 12px;
  color: #333;
  font-size: 18px;
}

.chapter-content {
  line-height: 1.8;
  text-align: justify;
  color: #666;
}

.sub-chapters {
  margin-left: 24px;
}
</style> 