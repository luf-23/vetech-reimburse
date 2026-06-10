<script setup lang="ts">
/**
 * 报销表单通用折叠面板容器。
 * 提供统一的区块标题栏、右侧操作插槽与收起/展开按钮，内容区通过默认插槽渲染。
 */
import { CaretBottom, CaretTop } from '@element-plus/icons-vue'
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    title?: string
    collapsed?: boolean
    showCollapse?: boolean
  }>(),
  {
    collapsed: false,
    showCollapse: true,
  },
)

const emit = defineEmits<{
  'update:collapsed': [value: boolean]
}>()

const isCollapsed = computed(() => props.collapsed)

function onToggleCollapse() {
  emit('update:collapsed', !props.collapsed)
}
</script>

<template>
  <div class="reim-section">
    <div class="reim-section-header">
      <div class="title-left">
        <slot name="title">{{ title }}</slot>
      </div>
      <div class="reim-section-actions">
        <slot name="actions" />
        <button
          v-if="showCollapse"
          type="button"
          class="reim-collapse-btn"
          :aria-expanded="!isCollapsed"
          :aria-label="isCollapsed ? '展开分区' : '收起分区'"
          @click.stop="onToggleCollapse"
        >
          <el-icon :size="16">
            <CaretTop v-if="!isCollapsed" />
            <CaretBottom v-else />
          </el-icon>
        </button>
      </div>
    </div>
    <div v-show="!isCollapsed" class="reim-section-body">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.reim-section-header {
  overflow: visible;
}

.reim-section-actions {
  flex-shrink: 0;
  overflow: visible;
}

.reim-collapse-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  margin: 0 0 0 4px;
  border: none;
  background: transparent;
  cursor: pointer;
  color: #666;
  border-radius: 2px;
  flex-shrink: 0;
  vertical-align: middle;
}

.reim-collapse-btn:hover {
  color: #333;
  background: rgba(0, 0, 0, 0.05);
}

.reim-collapse-btn :deep(svg) {
  width: 16px;
  height: 16px;
}
</style>
