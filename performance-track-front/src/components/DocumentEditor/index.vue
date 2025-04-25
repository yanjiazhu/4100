<template>
  <div class="document-editor">
    <!-- 顶部标题和操作区 -->
    <div class="editor-header">
      <el-input v-model="document.title" :placeholder="titlePlaceholder" class="title-input" size="large"
        :readonly="readonly" @input="$emit('update:title', $event)" />
      <!-- 只在文档模式下显示操作按钮 -->
      <div class="action-buttons" v-if="mode === 'document'">
        <el-button-group>
          <el-button v-if="!readonly" type="primary" @click="handleSave" :loading="saving">
            <el-icon>
              <Save />
            </el-icon>
            保存
          </el-button>
          <el-button @click="handlePreview">
            <el-icon>
              <View />
            </el-icon>
            预览
          </el-button>
          <el-button @click="$emit('export')">
            <el-icon>
              <Download />
            </el-icon>
            导出文档
          </el-button>
        </el-button-group>
      </div>
    </div>

    <!-- 主体编辑区 -->
    <div class="editor-content">
      <el-row :gutter="20" class="full-height">
        <!-- 左侧大纲面板 -->
        <el-col :span="6" class="full-height">
          <div class="outline-panel">
            <div class="panel-header">
              <span class="panel-title">{{ outlineTitle }}</span>
              <div class="panel-actions">
                <el-button type="primary" link size="small" @click="handleSelectAll">
                  {{ isAllSelected ? '取消全选' : '全选' }}
                </el-button>
                <el-button 
                  type="primary" 
                  link 
                  size="small" 
                  @click="handleClearSelection"
                  v-if="selectedChapters.value?.length"
                >
                  取消选择
                </el-button>
                <el-button v-if="!readonly" type="primary" link @click="showAddChapterDialog()">
                  添加章节
                </el-button>
              </div>
            </div>

            <!-- 大纲树 -->
            <div class="outline-tree">
              <el-scrollbar>
                <el-tree ref="treeRef" :data="document.chapters" :props="{ children: 'subChapters', label: 'title' }"
                  draggable :draggable="!readonly" node-key="id" default-expand-all :allow-drop="allowDrop"
                  :allow-drag="allowDrag" @node-drop="handleDrop" :expand-on-click-node="false"
                  @node-click="handleNodeClick" show-checkbox @check="handleCheck"
                  @check-change="handleCheckChange" @current-change="handleCurrentChange">
                  <template #default="{ node, data }">
                    <div class="custom-tree-node">
                      <span class="chapter-title">
                        {{ getChapterNumber(data) }} {{ data.title }}
                      </span>
                      <!-- 只在可编辑模式下显示操作按钮 -->
                      <span v-if="!readonly" class="node-actions">
                        <el-button-group>
                          <el-button type="primary" link size="small" @click.stop="showAddChapterDialog(data)">
                            <el-icon>
                              <Plus />
                            </el-icon>
                          </el-button>
                          <el-button type="primary" link size="small" @click.stop="showEditChapterDialog(data)">
                            <el-icon>
                              <Edit />
                            </el-icon>
                          </el-button>
                          <el-button type="danger" link size="small" @click.stop="deleteChapter(node, data)">
                            <el-icon>
                              <Delete />
                            </el-icon>
                          </el-button>
                        </el-button-group>
                      </span>
                    </div>
                  </template>
                </el-tree>
              </el-scrollbar>
            </div>
          </div>
        </el-col>

        <!-- 右侧编辑区 -->
        <el-col :span="18" class="full-height">
          <div class="content-panel">
            <div class="editor-container">
              <div v-if="currentChapter" class="editor-panel">
                <!-- 编辑器顶部工具栏 -->
                <div class="editor-toolbar">
                  <div class="toolbar-left">
                    <h3>{{ getChapterNumber(currentChapter) }} {{ currentChapter.title }}</h3>
                  </div>
                </div>
                <!-- 富文本编辑器 -->
                <div class="panel-content">
                  <div class="editor-wrapper">
                    <QuillEditor theme="snow" :content="currentChapter.content" content-type="html" ref="editorRef"
                      @update:content="handleContentChange" @ready="handleCreated" :options="editorOptions"
                      :readonly="readonly" />
                  </div>
                </div>
              </div>
              <div v-else class="empty-state">
                <el-empty description="请选择或创建一个章节开始编辑" />
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 章节标题对话框 -->
    <el-dialog v-model="chapterDialogVisible" :title="editingChapter ? '编辑章节' : '添加章节'" width="30%" destroy-on-close>
      <el-form :model="chapterForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="chapterForm.title" placeholder="请输入章节标题" @keyup.enter="handleChapterConfirm"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="chapterDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleChapterConfirm">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 预览对话框 -->
    <el-dialog
      v-model="previewVisible"
      title="文档预览"
      width="800px"
      fullscreen
      :show-close="false"
      v-if="previewData"
    >
      <template #header="{ close }">
        <div class="preview-dialog-header">
          <span>文档预览</span>
          <el-button type="danger" @click="close" class="preview-close-btn">
            <el-icon class="close-icon">
              <Close />
            </el-icon>
            关闭预览
          </el-button>
        </div>
      </template>

      <div class="preview-content">
        <h1 class="preview-title">{{ previewData.title }}</h1>
        <div v-for="(chapter, index) in previewData.chapters" :key="chapter.id">
          <div class="preview-chapter">
            <!-- 一级章节标题居中 -->
            <h2 class="preview-chapter-title chapter-level-1">
              第{{ index + 1 }}章 {{ chapter.title }}
            </h2>
            <div class="preview-chapter-content" v-html="chapter.content"></div>

            <!-- 子章节 -->
            <template v-if="chapter.subChapters && chapter.subChapters.length">
              <div v-for="(subChapter, subIndex) in chapter.subChapters" :key="subChapter.id" class="preview-chapter">
                <h3 class="preview-chapter-title">
                  {{ index + 1 }}.{{ subChapter.sort }} {{ subChapter.title }}
                </h3>
                <div class="preview-chapter-content" v-html="subChapter.content"></div>

                <!-- 三级章节 -->
                <template v-if="subChapter.subChapters && subChapter.subChapters.length">
                  <div v-for="(thirdChapter, thirdIndex) in subChapter.subChapters" :key="thirdChapter.id"
                    class="preview-chapter">
                    <h4 class="preview-chapter-title">
                      {{ index + 1 }}.{{ subChapter.sort }}.{{ thirdChapter.sort }} {{ thirdChapter.title }}
                    </h4>
                    <div class="preview-chapter-content" v-html="thirdChapter.content"></div>

                    <!-- 四级章节 -->
                    <template v-if="thirdChapter.subChapters && thirdChapter.subChapters.length">
                      <div v-for="(fourthChapter, fourthIndex) in thirdChapter.subChapters" :key="fourthChapter.id"
                        class="preview-chapter">
                        <h5 class="preview-chapter-title">
                          {{ index + 1 }}.{{ subChapter.sort }}.{{ thirdChapter.sort }}.{{ fourthChapter.sort }} {{
                          fourthChapter.title }}
                        </h5>
                        <div class="preview-chapter-content" v-html="fourthChapter.content"></div>
                      </div>
                    </template>
                  </div>
                </template>
              </div>
            </template>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="previewVisible = false" size="large">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 添加常用片段选择对话框 -->
    <el-dialog v-model="snippetDialogVisible" title="插入常用片段" width="800px">
      <div class="snippet-list">
        <el-card v-for="item in snippets" :key="item.id" class="snippet-card" @click="insertSnippet(item.content)">
          <template #header>
            <div class="snippet-header">
              <span>{{ item.title }}</span>
              <el-tooltip :content="item.description" placement="top">
                <el-icon>
                  <InfoFilled />
                </el-icon>
              </el-tooltip>
            </div>
          </template>
          <div class="snippet-content">{{ item.content }}</div>
        </el-card>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, shallowRef, onMounted, onBeforeUnmount, watch, toRaw, nextTick, defineComponent, getCurrentInstance } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Close, InfoFilled, Save, View, Lock, Select, Download } from '@element-plus/icons-vue'
import { Document, Packer, Paragraph } from 'docx'
import { saveAs } from 'file-saver'
import { useRoute, useRouter } from 'vue-router'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css';
import { getDocumentById, deleteChapter as deleteChapterApi, exportSelectedToWord, exportToWord } from '@/api/documents'
import { getUserSnippets } from '@/api/snippets'
import ImageResize from 'quill-image-resize-vue'
import Quill from 'quill'

const props = defineProps({
  mode: {
    type: String,
    default: 'document',
    validator: (value) => ['document', 'template'].includes(value)
  },
  initialData: {
    type: Object,
    required: true
  },
  readonly: {
    type: Boolean,
    default: false
  }
})

// 创建一个空的初始文档数据
const document = ref({
  title: '',
  chapters: [],
  collaborators: [],
  reviewHistory: []
})

// 监听数据变化
watch(() => props.initialData, (newData) => {
  if (!newData) return;

  const processedData = JSON.parse(JSON.stringify(newData));

  if (processedData.chapters) {
    // 递归处理所有章节
    const processChapters = (chapters) => {
      chapters.forEach(chapter => {
        chapter.subChapters = chapter.subChapters || [];
        if (chapter.subChapters.length > 0) {
          processChapters(chapter.subChapters);
        }
      });
      // 对当前层级的章节进行排序
      chapters.sort((a, b) => (a.sort || 0) - (b.sort || 0));
    };

    processChapters(processedData.chapters);
  }

  document.value = processedData;
}, { deep: true });

const emit = defineEmits(['update:content', 'save'])
const treeRef = ref(null)
const saving = ref(false)
const currentChapter = ref(null)
const editorRef = ref(null)

// 章节锁定状态
const chapterLock = ref(null)
const isCurrentUserLock = computed(() => {
  return chapterLock.value?.username === localStorage.getItem('username')
})
const isLocked = computed(() => {
  return chapterLock.value && !isCurrentUserLock.value
})

// 锁定章节
const lockChapter = () => {
  // 模拟API调用
  chapterLock.value = {
    username: localStorage.getItem('username'),
    timestamp: Date.now()
  }
  // TODO: 发送WebSocket消息通知其他用户
}

// 解锁章节
const unlockChapter = () => {
  if (isCurrentUserLock.value) {
    chapterLock.value = null
    // TODO: 发送WebSocket消息通知其他用户
  }
}

// 自动解锁
watch(chapterLock, (newVal) => {
  if (newVal && isCurrentUserLock.value) {
    const timer = setTimeout(() => {
      unlockChapter()
      ElMessage.warning('章节锁定已超时，已自动解锁')
    }, 5 * 60 * 1000) // 5分钟后自动解锁

    // 组件卸载时清除定时器
    onBeforeUnmount(() => {
      clearTimeout(timer)
    })
  }
})

// 切换章节时自动解锁当前章节
watch(() => currentChapter.value?.id, (newId, oldId) => {
  if (oldId && isCurrentUserLock.value) {
    unlockChapter()
  }
})

// 片段相关的状态
const snippetDialogVisible = ref(false)
const activeSnippetTab = ref('all')
const snippets = ref([])

// 获取片段列表
const fetchSnippets = async () => {
  try {
    const userId = localStorage.getItem('userId')
    const data = await getUserSnippets(userId)
    snippets.value = data
  } catch (error) {
    console.error('获取片段失败:', error)
    ElMessage.error('获取片段列表失败')
  }
}

// 插入片段
const insertSnippet = (content) => {
  if (!editorRef.value || !currentChapter.value) return;

  // 直接更新章节内容
  let currentContent = currentChapter.value.content || '';

  // 拼接新内容
  currentChapter.value.content = currentContent + content;

  // 关闭对话框
  snippetDialogVisible.value = false;

  // 提示插入成功
  ElMessage.success('插入成功');
}

// 组件销毁时，也及时销毁编辑器
onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor == null) return
  editor.destroy()
})

// 编辑器回调函数
const handleCreated = (editor) => {
  console.log('Editor created:', editor);
  editorRef.value = editor;
};

// 处理内容变化
const handleContentChange = (content) => {
  if (!currentChapter.value) return;

  // 更新当前章节内容
  currentChapter.value.content = content;

  // 更新 document 中的章节内容
  const success = updateChapterContent(document.value.chapters, currentChapter.value.id, content);
  if (success) {
    // 触发更新事件
    emit('update:content', document.value);
  }
};

// 章节对话框相关
const chapterDialogVisible = ref(false)
const editingChapter = ref(null)
const chapterForm = ref({
  title: '',
  parentChapter: null
})

// 打开添加章节对话框
const showAddChapterDialog = (parentChapter = null) => {
  console.log('showAddChapterDialog called', { parentChapter })
  editingChapter.value = null
  chapterForm.value = {
    title: '',
    parentChapter
  }
  chapterDialogVisible.value = true
}

// 打开编辑章节对话框
const showEditChapterDialog = (chapter) => {
  console.log('showEditChapterDialog called', { chapter })
  editingChapter.value = chapter
  chapterForm.value = {
    title: chapter.title
  }
  chapterDialogVisible.value = true
}

// 处理章节对话框确认
const handleChapterConfirm = () => {
  if (!chapterForm.value.title.trim()) {
    ElMessage.warning('请输入章节标题')
    return
  }

  if (editingChapter.value) {
    // 编辑章节
    updateChapterTitle(editingChapter.value, chapterForm.value.title)
    ElMessage.success('章节已更新')
  } else {
    // 添加章节
    addChapter(chapterForm.value.parentChapter, chapterForm.value.title)
  }

  // 发送更新事件
  emit('update:content', {
    ...document.value,
    chapters: [...document.value.chapters]
  });

  chapterDialogVisible.value = false
}

// 添加章节
const addChapter = (parentChapter, title) => {
  const siblings = parentChapter 
    ? parentChapter.subChapters
    : document.value.chapters;

  const newChapter = {
    id: 'temp_' + Date.now(),
    title,
    content: '',
    subChapters: [],
    sort: siblings.length > 0 
      ? Math.max(...siblings.map(c => c.sort)) + 1  // 获取当前最大sort值+1
      : 1
  };

  if (parentChapter) {
    parentChapter.subChapters = parentChapter.subChapters || [];
    parentChapter.subChapters.push(newChapter);
  } else {
    document.value.chapters.push(newChapter);
  }

  emit('update:content', {
    ...document.value,
    chapters: [...document.value.chapters]
  });
}

// 更新章节标题
const updateChapterTitle = (chapter, newTitle) => {
  chapter.title = newTitle;

  // 发送更新事件
  emit('update:content', {
    ...document.value,
    chapters: [...document.value.chapters]
  });
}

// 删除章节
const deleteChapter = (node, data) => {
  ElMessageBox.confirm('确定要删除该章节吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      // 如果章节有 ID，说明是已保存到数据库的章节，需要调用 API
      if (data.id && !isNaN(data.id)) {  // 确保 ID 是有效的
        await deleteChapterApi(data.id);
      }

      // 无论是否调用 API，都在前端删除该章节
      const parent = node.parent;
      const children = parent.data.subChapters || document.value.chapters;
      const index = children.findIndex(item => item.id === data.id);
      children.splice(index, 1);

      // 发送更新事件
      emit('update:content', {
        ...document.value,
        chapters: [...document.value.chapters]
      });

      ElMessage.success('章节已删除');
    } catch (error) {
      console.error('删除章节失败:', error);
      ElMessage.error('删除章节失败');
      throw error; // 继续抛出错误，确保弹窗关闭
    }
  }).catch((err) => {
    // 只有当用户取消时，不显示错误信息
    if (err !== 'cancel' && err?.message !== 'cancel') {
      console.error('删除章节出错:', err);
    }
  });
};

// 获取章节路径
const getChapterPath = (chapters, targetId) => {
  if (!chapters || !Array.isArray(chapters)) return null; // 确保 chapters 是一个数组

  for (let i = 0; i < chapters.length; i++) {
    if (chapters[i].id === targetId) {
      return [i];
    }
    if (chapters[i].children) {
      const childPath = getChapterPath(chapters[i].children, targetId);
      if (childPath) {
        return [i, ...childPath];
      }
    }
  }
  return null;
}

// 获取章节编号
const getChapterNumber = (chapter) => {
  if (!chapter) return '';

  // 获取章节的完整路径
  const getChapterPath = (targetChapter) => {
    const path = [];
    let current = targetChapter;

    while (current) {
      path.unshift(current);
      current = findParentChapter(document.value.chapters, current.id);
    }

    return path;
  };

  const path = getChapterPath(chapter);

  // 如果是第一层章节，使用"第X章"的格式
  if (path.length === 1) {
    const index = document.value.chapters.findIndex(ch => ch.id === chapter.id);
    return `第${index + 1}章`;
  }

  // 其他层级使用数字格式，使用 sort 值
  return path.map(ch => ch.sort).join('.');
};

// 添加查找父章节的辅助函数
const findParentChapter = (chapters, childId) => {
  for (const chapter of chapters) {
    if (chapter.subChapters?.some(sub => sub.id === childId)) {
      return chapter;
    }
    if (chapter.subChapters) {
      const found = findParentChapter(chapter.subChapters, childId);
      if (found) return found;
    }
  }
  return null;
};

// 添加预览相关的响应式变量
const previewVisible = ref(false)
const previewData = ref(null)

// 修改预览处理函数
const handlePreview = () => {
  let chaptersToPreview = selectedChapters.value;
  
  // 如果没有选择任何章节，则预览全部
  if (!chaptersToPreview || chaptersToPreview.length === 0) {
    chaptersToPreview = getAllChapterIds(document.value.chapters);
  }

  // 构建预览文档结构
  previewData.value = {
    title: document.value.title,
    chapters: buildSelectedChapters(document.value.chapters, chaptersToPreview)
  };
  
  previewVisible.value = true;
};

// 添加子章节
const handleAddSubChapter = (parentChapter) => {
  showAddChapterDialog(parentChapter)
}

// 编辑章节
const handleEditChapter = (chapter) => {
  showEditChapterDialog(chapter)
}

// 获取编辑器数据的方法（供父组件调用）
const getData = () => ({
  selectedChapters: selectedChapters.value,
  allChapterIds: getAllChapterIds(document.value.chapters),
  title: document.value.title,
  chapters: document.value.chapters
});

// 暴露方法给父组件
defineExpose({
  getData
})

const route = useRoute()
const router = useRouter()

const generateChapterNumbers = (chapters, parentNumber = '', format = '1.') => {
  return chapters.map((chapter, index) => {
    const currentNumber = parentNumber ? `${parentNumber}${index + 1}` : `${index + 1}`
    const formattedNumber = format === '一、' ? convertToChineseNumber(currentNumber) : currentNumber
    return {
      ...chapter,
      number: formattedNumber,
      subChapters: chapter.subChapters ? generateChapterNumbers(chapter.subChapters, `${currentNumber}.`, format) : []
    }
  })
}

const convertToChineseNumber = (number) => {
  // 实现数字转换为中文格式
  // 例如：1 -> 一、 2 -> 二、
  const chineseNumbers = ['一', '二', '三', '四', '五', '六', '七', '八', '九', '十']
  return number.split('.').map(num => chineseNumbers[parseInt(num) - 1]).join('、') + '、'
}

const fetchDocument = async () => {
  if (!route.params.id) {
    document.value = {
      title: '',
      chapters: []
    }
    return
  }

  try {
    const fetchedDocument = await getDocumentById(route.params.id)
    document.value = {
      ...fetchedDocument,
      chapters: generateChapterNumbers(fetchedDocument.chapters)
    }
  } catch (error) {
    ElMessage.error('获取文档失败')
  }
}

// 初始化文档数据
const initDocumentData = (data) => {
  if (!data?.chapters?.length) return { ...data, chapters: [] }

  const chapters = JSON.parse(JSON.stringify(data.chapters))
  
  // 修改后的递归处理函数
  const processChapters = (chapters) => {
    chapters.forEach(chapter => {
      // 确保所有层级都有content字段
      chapter.content = chapter.content || ''
      chapter.subChapters = chapter.subChapters || []
      // 递归处理子章节
      if (chapter.subChapters.length > 0) {
        processChapters(chapter.subChapters)
      }
    })
  }
  
  processChapters(chapters)

  return {
    ...data,
    chapters
  }
}

// 处理节点点击
const handleNodeClick = async (data) => {
  try {
    // 查找并设置当前章节
    const foundChapter = findChapterById(document.value.chapters, data.id)
    if (foundChapter) {
      // 如果当前章节已经是选中的，不需要重新加载
      if (currentChapter.value?.id === foundChapter.id) {
        return
      }
      
      if (saving.value) {
        ElMessage.warning('正在保存，请稍后再试')
        return
      }

      // 保存当前选中的章节
      const currentCheckedKeys = selectedChapters.value

      currentChapter.value = foundChapter
      
      // 增加双重等待确保DOM更新
      await nextTick()
      await nextTick()

      if (editorRef.value) {
        // 使用深拷贝避免响应式数据问题
        const content = JSON.parse(JSON.stringify(foundChapter.content)) || ''
        editorRef.value.setContents(content)
      }

      // 恢复选中状态
      if (selectedChapters.value.length > 0) {
        selectedChapters.value.forEach(key => {
          treeRef.value.setChecked(key, true, false)
        })
      }
    }
  } catch (error) {
    console.error('切换章节失败:', error)
    ElMessage.error('切换章节失败')
  }
}

const findChapterById = (chapters, chapterId) => {
  for (const chapter of chapters) {
    if (chapter.id === chapterId) {
      return chapter;
    }
    if (chapter.subChapters) {
      const found = findChapterById(chapter.subChapters, chapterId);
      if (found) return found;
    }
  }
  return null;
};

// 监听章节变化
watch(() => currentChapter.value, (newChapter) => {
  if (newChapter && editorRef.value) {
    editorRef.value.setContents(newChapter.content || '');
  }
}, { immediate: true });

const updateChapterContent = (chapters, chapterId, content) => {
  for (const chapter of chapters) {
    if (chapter.id === chapterId) {
      chapter.content = content;
      return true;
    }
    if (chapter.subChapters && chapter.subChapters.length > 0) {
      if (updateChapterContent(chapter.subChapters, chapterId, content)) {
        return true;
      }
    }
  }
  return false;
};

const handleSave = () => {
  emit('save');
};

// 拖拽排序时
const handleDrop = (draggingNode, dropNode, dropType, ev) => {
  // 更新所有章节的排序
  const updateChapterSort = (chapters) => {
    if (!chapters || !Array.isArray(chapters)) return chapters;

    // 对当前层级进行排序
    chapters.sort((a, b) => (a.sort || 0) - (b.sort || 0));

    // 更新当前层级的 sort 值
    chapters.forEach((chapter, index) => {
      chapter.sort = index + 1;
      // 递归更新子章节的排序
      if (chapter.subChapters && Array.isArray(chapter.subChapters) && chapter.subChapters.length > 0) {
        updateChapterSort(chapter.subChapters);
      }
    });

    return chapters;
  };

  // 获取目标容器（只能是同级容器）
  const parent = dropNode.parent;
  const targetContainer = parent ? parent.data.subChapters : document.value.chapters;

  // 等待 DOM 更新完成后再更新排序
  nextTick(() => {
    // 更新整个文档的章节排序
    document.value.chapters = updateChapterSort([...document.value.chapters]);

    // 发送更新事件，包含完整的章节树
    emit('update:content', {
      ...document.value,
      chapters: JSON.parse(JSON.stringify(document.value.chapters))
    });

    // 强制更新树形控件
    if (treeRef.value) {
      treeRef.value.$forceUpdate();
      // 确保展开状态保持
      treeRef.value.setExpandedKeys(treeRef.value.getExpandedKeys());
    }
  });
};

// 允许拖拽的配置
const allowDrop = (draggingNode, dropNode, type) => {
  // 只允许同级移动
  if (type !== 'prev' && type !== 'next') {
    return false;
  }

  // 确保是同一层级
  const draggingParent = draggingNode.parent;
  const dropParent = dropNode.parent;

  // 如果两个节点的父节点不同，不允许拖拽
  if (draggingParent !== dropParent) {
    return false;
  }

  return true;
};

// 允许拖拽的节点
const allowDrag = (draggingNode) => {
  // 可以添加其他限制条件
  return true;
};

const formatContent = (content) => {
  if (!content) return '';

  // 如果内容是HTML格式，保持原有格式并添加默认样式
  if (content.trim().startsWith('<')) {
    // 为没有特定对齐方式的段落添加默认缩进
    return content.replace(/<p(?![^>]*class="ql-align)([^>]*)>/g, '<p$1 class="ql-indent-1">');
  }

  // 如果是纯文本，转换为HTML格式并添加默认缩进
  return `<div>${content.split('\n').map(line => `<p class="ql-indent-1">${line}</p>`).join('')}</div>`;
};

// 计算属性：扁平化章节列表
const flattenChapters = computed(() => {
  const flatten = (chapters) => {
    let result = [];
    for (const chapter of chapters) {
      result.push(chapter);
      if (chapter.subChapters && chapter.subChapters.length > 0) {
        result = result.concat(flatten(chapter.subChapters));
      }
    }
    return result;
  };

  return flatten(document.value.chapters);
});

// 定义支持的颜色
const SUPPORTED_COLORS = {
  // 基础黑白色（放在最前面）
  '#000000': '黑色',
  '#FFFFFF': '白色',

  // 基础颜色
  '#FFFF00': '黄色',
  '#00FF00': '绿色',
  '#00FFFF': '青色',
  '#FF00FF': '品红',
  '#0000FF': '蓝色',
  '#FF0000': '红色',
  '#000080': '深蓝',
  '#008080': '深青',
  '#008000': '深绿',
  '#800080': '深品红',
  '#800000': '深红',
  '#808000': '深黄',
  '#808080': '深灰',
  '#C0C0C0': '浅灰',

  // 黄色系
  '#FFD700': '金色',
  '#FFA500': '橙色',
  '#FFE4B5': '浅黄棕',
  '#FFEFD5': '粉黄',
  '#FAFAD2': '浅金黄',

  // 绿色系
  '#98FB98': '浅绿',
  '#90EE90': '淡绿',
  '#32CD32': '酸橙绿',
  '#3CB371': '中海绿',
  '#2E8B57': '海绿',
  '#72C040': '浅绿色',

  // 蓝色系
  '#87CEEB': '天蓝',
  '#87CEFA': '浅天蓝',
  '#4169E1': '皇家蓝',
  '#1E90FF': '道奇蓝',
  '#00BFFF': '深天蓝',
  '#597EF7': '浅蓝色',

  // 红色系
  '#FFA07A': '浅鲑鱼',
  '#FA8072': '鲑鱼色',
  '#E9967A': '深鲑鱼',
  '#F08080': '浅珊瑚',
  '#CD5C5C': '印第安红',

  // 紫色系
  '#DDA0DD': '梅红',
  '#DA70D6': '兰花紫',
  '#BA55D3': '中兰花紫',
  '#9370DB': '中紫色',
  '#8B008B': '深洋红',

  // 灰色系
  '#DCDCDC': '浅灰白',
  '#D3D3D3': '浅灰',
  '#A9A9A9': '深灰',
  '#696969': '暗灰',
  '#2F4F4F': '深石灰',

  // 其他常用颜色
  '#B2B200': '暗黄色'
};

// 注册图片调整大小插件
Quill.register('modules/imageResize', ImageResize)

// 修改编辑器配置
const editorOptions = {
  modules: {
    toolbar: [
    ['bold', 'italic', 'underline', 'strike'],
        [{ 'header': 1 }, { 'header': 2 }],
        [{ 'indent': '-1' }, { 'indent': '+1' }],
        [{ 'direction': 'rtl' }],
        [{ 'size': ['small', false, 'large', 'huge'] }],
        [{ 'header': [1, 2, 3, 4, 5, 6, false] }],
        [{ 'color': [] }, { 'background': [] }],
        [{ 'font': [] }],
        [{ 'align': [] }],
        ['image']
    ],
    imageResize: {
      displaySize: true,
      modules: ['Resize', 'DisplaySize', 'Toolbar']
    }
  },
  placeholder: '请输入内容...'
};

// 添加颜色选择器的提示
onMounted(() => {
  if (editorRef.value) {
    const toolbar = editorRef.value.getModule('toolbar');
    const colorPickers = document.querySelectorAll('.ql-color, .ql-background');

    colorPickers.forEach(picker => {
      const options = picker.querySelector('.ql-picker-options');
      if (options) {
        const items = options.querySelectorAll('.ql-picker-item');
        items.forEach(item => {
          const color = item.dataset.value || item.getAttribute('data-value');
          if (color && SUPPORTED_COLORS[color]) {
            item.setAttribute('title', SUPPORTED_COLORS[color]);
          }
        });
      }
    });
  }
});

// 处理章节排序
const sortChapters = (chapters) => {
  if (!chapters) return [];
  return chapters.sort((a, b) => (a.sort || 0) - (b.sort || 0));
};

// 添加一个辅助函数来重新计算所有章节的排序
const recalculateAllChapterSort = () => {
  const updateSort = (chapters, parentSort = '') => {
    chapters.forEach((chapter, index) => {
      const currentSort = parentSort ? `${parentSort}.${index + 1}` : `${index + 1}`;
      chapter.sort = index + 1;

      if (chapter.subChapters && chapter.subChapters.length > 0) {
        updateSort(chapter.subChapters, currentSort);
      }
    });
  };

  updateSort(document.value.chapters);
};

// 在组件挂载时初始化排序
onMounted(() => {
  recalculateAllChapterSort();
  fetchSnippets();
});

// 监听章节数据变化，确保排序正确
watch(() => document.value.chapters, () => {
  recalculateAllChapterSort();
}, { deep: true });

// 递归组件用于显示子章节
const RecursiveChapters = defineComponent({
  name: 'RecursiveChapters',
  props: {
    chapters: {
      type: Array,
      required: true
    },
    baseNumber: {
      type: [String, Number],
      required: true
    },
    level: {
      type: Number,
      required: true
    }
  },
  template: `    <template v-for="chapter in chapters" :key="chapter.id">
      <div class="preview-chapter">
        <div class="preview-chapter-title">
          {{ baseNumber }}.{{ chapter.sort }} {{ chapter.title }}
        </div>
        <div class="preview-chapter-content" v-html="chapter.content"></div>
        
        <recursive-chapters
          v-if="chapter.subChapters && chapter.subChapters.length"
          :chapters="chapter.subChapters"
          :base-number="baseNumber + '.' + chapter.sort"
          :level="level + 1"
        />
      </div>
    </template>
  `
})

// 注册递归组件
const app = getCurrentInstance()
app.appContext.app.component('RecursiveChapters', RecursiveChapters)

// 添加状态变量
const selectedChapters = ref([])

// 处理节点选中状态变化
const handleCheckChange = (data, checked) => {
  if (checked) {
    // 获取当前节点的所有父节点
    const parentKeys = []
    let currentNode = treeRef.value.getNode(data.id)
    
    while (currentNode.parent && currentNode.parent.key) {
      parentKeys.push(currentNode.parent.key)
      currentNode = currentNode.parent
    }

    // 选中所有父节点
    parentKeys.forEach(key => {
      treeRef.value.setChecked(key, true, false)
    })
  }
}

// 处理选中状态
const handleCheck = (node, checkedInfo) => {
  console.log('树节点选中状态变化:', {
    node,
    checkedInfo,
    checkedKeys: checkedInfo.checkedKeys
  });
  selectedChapters.value = checkedInfo.checkedKeys;
}

// 添加全选相关的计算属性和方法
const getAllChapterIds = (chapters) => {
  let ids = [];
  chapters.forEach(chapter => {
    ids.push(chapter.id);
    if (chapter.subChapters && chapter.subChapters.length > 0) {
      ids = ids.concat(getAllChapterIds(chapter.subChapters));
    }
  });
  return ids;
};

const isAllSelected = computed(() => {
  const allIds = getAllChapterIds(document.value.chapters);
  return allIds.length > 0 && allIds.every(id => selectedChapters.value.includes(id));
});

// 处理全选/取消全选
const handleSelectAll = () => {
  if (isAllSelected.value) {
    // 取消全选
    selectedChapters.value = [];
    treeRef.value.setCheckedKeys([]);
  } else {
    // 全选
    const allIds = getAllChapterIds(document.value.chapters);
    selectedChapters.value = allIds;
    treeRef.value.setCheckedKeys(allIds);
  }
};

// 构建选中的章节结构
const buildSelectedChapters = (chapters, selectedIds) => {
  if (!chapters) return []
  
  const result = []
  
  for (const chapter of chapters) {
    if (selectedIds.includes(chapter.id)) {
      // 如果当前章节被选中，添加该章节及其被选中的子章节
      const filteredSubChapters = chapter.subChapters ? buildSelectedChapters(chapter.subChapters, selectedIds) : []
      result.push({
        ...chapter,
        subChapters: filteredSubChapters
      })
    } else if (chapter.subChapters && chapter.subChapters.length > 0) {
      // 如果当前章节未被选中，但需要检查其子章节
      const filteredSubChapters = buildSelectedChapters(chapter.subChapters, selectedIds)
      if (filteredSubChapters.length > 0) {
        result.push({
          ...chapter,
          subChapters: filteredSubChapters
        })
      }
    }
  }
  
  return result
}

// 在组件卸载时清理预览数据
onBeforeUnmount(() => {
  previewData.value = null;
  previewVisible.value = false;
});

// 添加取消选择方法
const handleClearSelection = () => {
  selectedChapters.value = [];
  treeRef.value?.setCheckedKeys([]);
};
</script>

<style scoped>
.document-editor {
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
  background-color: #fff;
  border-radius: 4px;
  padding: 20px;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.title-input {
  width: 500px;
}

.editor-content {
  flex: 1;
  margin-top: 20px;
  overflow: hidden;
}

.full-height {
  height: 100%;
}

/* 左侧大纲面板 */
.outline-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background-color: #fff;
}

.panel-header {
  padding: 12px 16px;
  border-bottom: 1px solid #dcdfe6;
  background-color: #fafafa;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.outline-tree {
  flex: 1;
  overflow: hidden;
  border-bottom: 1px solid #dcdfe6;
}

/* 右侧编辑区 */
.content-panel {
  height: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background-color: #fff;
  display: flex;
  flex-direction: column;
}

.chapter-editor {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 20px;
}

.editor-toolbar {
  padding: 12px 20px;
  border-bottom: 1px solid #dcdfe6;
  background-color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.toolbar-left h3 {
  margin: 0;
  font-size: 18px;
  color: #1890ff;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.editor-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  margin-top: 16px;
}

.left-panel {
  width: 300px;
  border-right: 1px solid #dcdfe6;
  display: flex;
  flex-direction: column;
}

.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 20px;
  overflow: auto;
}

.editor-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.panel-header {
  padding: 12px 16px;
  border-bottom: 1px solid #dcdfe6;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-title {
  font-size: 16px;
  font-weight: 500;
}

.panel-content {
  flex: 1;
  overflow: auto;
  padding: 16px;
}

.editor-wrapper {
  min-height: 300px;
  display: flex;
  flex-direction: column;
  background-color: #fff;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

:deep(.el-tree-node__content) {
  height: 32px;
}

:deep(.el-button--small) {
  padding: 4px;
}

:deep(.ql-toolbar.ql-snow) {
  border: none;
  border-bottom: 1px solid #dcdfe6;
  padding: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

:deep(.ql-toolbar.ql-snow .ql-formats) {
  margin-right: 10px;
}

:deep(.ql-container.ql-snow) {
  flex: 1;
  border: none;
}

:deep(.ql-editor) {
  height: 100%;
  font-size: 14px;
  line-height: 1.6;
  padding: 16px;
  min-height: 300px;
}

:deep(.ql-picker-options) {
  z-index: 100;
}

.snippet-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  padding: 16px;
  max-height: 60vh;
  overflow-y: auto;
}

.snippet-card {
  cursor: pointer;
  transition: all 0.3s;
}

.snippet-card:hover {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, .1);
  transform: translateY(-2px);
}

.snippet-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.snippet-content {
  color: #666;
  font-size: 14px;
  line-height: 1.6;
  max-height: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.lock-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.lock-status .el-tag {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 添加工具栏按钮悬停效果 */
:deep(.ql-toolbar button:hover),
:deep(.ql-toolbar button.ql-active) {
  color: #409EFF;
}

:deep(.ql-toolbar button:hover .ql-stroke),
:deep(.ql-toolbar button.ql-active .ql-stroke) {
  stroke: #409EFF;
}

:deep(.ql-toolbar button:hover .ql-fill),
:deep(.ql-toolbar button.ql-active .ql-fill) {
  fill: #409EFF;
}

/* 预览样式 */
.preview-content {
  padding: 30px;
  max-width: 1000px;
  margin: 0 auto;
}

.preview-title {
  text-align: center;
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 40px;
  color: #333;
}

.preview-chapter {
  margin-bottom: 30px;
}

/* 一级章节标题居中 */
.chapter-level-1 {
  text-align: center;
  font-size: 24px;
  margin-bottom: 20px;
}

/* 二级章节缩进和样式 */
.chapter-level-2 {
  margin-left: 30px;
}

.chapter-level-2 .preview-chapter-title {
  font-size: 20px;
  text-align: left;
}

/* 三级章节缩进和样式 */
.chapter-level-3 {
  margin-left: 60px;
}

.chapter-level-3 .preview-chapter-title {
  font-size: 18px;
  text-align: left;
}

.preview-chapter-title {
  font-weight: bold;
  color: #333;
  margin-bottom: 15px;
}

.preview-chapter-content {
  color: #333;
  line-height: 1.8;
  margin: 15px 0;
}

/* Quill 编辑器样式覆盖 */
:deep(.preview-chapter-content) {

  /* 默认缩进 */
  .ql-indent-1 {
    padding-left: 2em;
  }

  /* 对齐方式 */
  .ql-align-center {
    text-align: center;
  }

  .ql-align-right {
    text-align: right;
  }

  .ql-align-justify {
    text-align: justify;
  }

  /* 列表样式 */
  .ql-indent-1:not([class*="ql-align"]) {
    text-indent: 2em;
  }

  ol,
  ul {
    padding-left: 2em;
  }

  /* 引用样式 */
  blockquote {
    border-left: 4px solid #ccc;
    margin: 1em 0;
    padding-left: 16px;
  }

  /* 代码块样式 */
  pre.ql-syntax {
    background-color: #f6f6f6;
    padding: 1em;
    border-radius: 4px;
    overflow-x: auto;
  }

  /* 图片居中 */
  p.ql-align-center img {
    margin: 0 auto;
    display: block;
  }

  /* 标题样式 */
  h1,
  h2,
  h3,
  h4,
  h5,
  h6 {
    margin: 1em 0 0.5em;
    line-height: 1.5;
  }

  /* 段落间距 */
  p {
    margin: 0.5em 0;
  }
}

/* 打印样式优化 */
@media print {
  .preview-content {
    padding: 0;
  }

  .preview-chapter-title {
    break-after: avoid;
  }

  .preview-chapter {
    break-inside: avoid;
  }
}

/* 预览对话框样式 */
.preview-dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 0 20px;
}

.preview-close-btn {
  font-size: 16px;
  padding: 10px 20px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.close-icon {
  font-size: 18px;
}

:deep(.el-dialog__header) {
  margin: 0;
  padding: 20px 0;
  border-bottom: 1px solid #dcdfe6;
}

:deep(.el-dialog__headerbtn) {
  font-size: 24px;
  top: 20px;
  right: 20px;
}

:deep(.el-dialog__footer) {
  border-top: 1px solid #dcdfe6;
  padding: 20px;
}

:deep(.el-dialog__footer .el-button) {
  padding: 12px 24px;
  font-size: 16px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #dcdfe6;
}

.panel-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.chapter-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 4px 0;
}

:deep(.el-tree-node__content) {
  height: auto;
  padding: 4px 0;
}

:deep(.el-checkbox) {
  margin-right: 8px;
}

.chapter-actions {
  opacity: 0;
  transition: opacity 0.2s;
}

.chapter-node:hover .chapter-actions {
  opacity: 1;
}

/* 添加按钮悬停效果 */
.panel-actions .el-button:hover {
  opacity: 0.8;
}
</style>
