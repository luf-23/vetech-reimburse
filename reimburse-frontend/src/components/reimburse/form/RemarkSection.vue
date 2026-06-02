<script setup lang="ts">
import { Delete } from '@element-plus/icons-vue'
import SectionPanel from '@/components/reimburse/SectionPanel.vue'
import type { ReimburseFormData } from '@/types/reimburse'

defineProps<{
  form: ReimburseFormData
  collapsed: boolean
}>()

const emit = defineEmits<{
  'update:collapsed': [v: boolean]
  'delete-remark': []
}>()
</script>

<template>
  <SectionPanel :collapsed="collapsed" @update:collapsed="emit('update:collapsed', $event)">
    <template #title>备注信息</template>
    <template #actions>
      <span class="reim-section-action-btn" @click.stop="emit('delete-remark')">
        <el-icon><Delete /></el-icon> 删除备注
      </span>
    </template>
    <el-input
      v-model="form.remark"
      type="textarea"
      :rows="4"
      placeholder="请输入"
      maxlength="1000"
      show-word-limit
    />
  </SectionPanel>
</template>
