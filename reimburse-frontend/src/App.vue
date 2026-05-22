<script setup lang="ts">
import { onMounted } from 'vue'
import { useMasterData } from '@/composables/useMasterData'

const { ensureLoaded } = useMasterData()

onMounted(() => {
  ensureLoaded().catch(() => {
    /* 各页面可再次提示加载失败 */
  })
})
</script>

<template>
  <router-view />
</template>
