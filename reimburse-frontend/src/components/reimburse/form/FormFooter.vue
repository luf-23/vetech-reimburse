<script setup lang="ts">
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
