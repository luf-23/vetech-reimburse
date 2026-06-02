<script setup lang="ts">
defineProps<{
  submitDate: string
}>()
</script>

<template>
  <div class="reim-form-header-fixed">
    <div class="reim-form-content">
      <div class="reim-form-header-row">
        <h1 class="reim-form-title">差旅费用报销单</h1>
        <div class="reim-form-date">提单日期 {{ submitDate }}</div>
      </div>
    </div>
  </div>
</template>
