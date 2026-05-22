<script setup lang="ts">
import { ArrowDown, ArrowUp } from '@element-plus/icons-vue'

defineProps<{
  title: string
  collapsed?: boolean
  showCollapse?: boolean
}>()

const emit = defineEmits<{
  'update:collapsed': [value: boolean]
  toggle: []
}>()
</script>

<template>
  <div class="reim-section">
    <div class="reim-section-header">
      <div class="title-left">
        <slot name="title">{{ title }}</slot>
      </div>
      <div class="reim-section-actions">
        <slot name="actions" />
        <el-icon
          v-if="showCollapse !== false"
          class="reim-collapse-icon"
          :size="14"
          @click.stop="emit('toggle')"
        >
          <ArrowUp v-if="!collapsed" />
          <ArrowDown v-else />
        </el-icon>
      </div>
    </div>
    <div v-show="!collapsed" class="reim-section-body">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.reim-section-header {
  cursor: default;
}

.reim-collapse-icon {
  cursor: pointer;
  color: #999;
  flex-shrink: 0;
}

.reim-collapse-icon:hover {
  color: #666;
}
</style>
