<script setup lang="ts">
/**
 * 报销表单底部操作栏（固定底部）。
 * 提供「关闭」与「提交」按钮，提交时展示 loading 状态。
 */
defineProps<{
  submitting: boolean
}>()

const emit = defineEmits<{
  close: []
  submit: []
}>()
</script>

<template>
  <div class="reim-form-footer">
    <div class="reim-form-footer-inner">
      <el-button @click="emit('close')">关闭</el-button>
      <el-button type="primary" :loading="submitting" @click="emit('submit')">提交</el-button>
    </div>
  </div>
</template>
