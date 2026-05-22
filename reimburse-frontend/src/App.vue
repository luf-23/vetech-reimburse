<script setup lang="ts">
import { onMounted } from 'vue'
import { useMasterData } from '@/composables/useMasterData'

const { ensureLoaded } = useMasterData()
onMounted(() => {
  void ensureLoaded()
})
</script>

<template>
  <router-view />
</template>
