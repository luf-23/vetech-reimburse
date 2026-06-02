<script setup lang="ts">
import { CirclePlus, CopyDocument, Delete, Edit } from '@element-plus/icons-vue'
import SectionPanel from '@/components/reimburse/SectionPanel.vue'
import type { ItineraryItem } from '@/types/reimburse'

defineProps<{
  itineraries: ItineraryItem[]
  collapsed: boolean
}>()

const emit = defineEmits<{
  'update:collapsed': [v: boolean]
  add: []
  edit: [row: ItineraryItem]
  copy: [row: ItineraryItem]
  delete: [row: ItineraryItem]
}>()
</script>

<template>
  <SectionPanel :collapsed="collapsed" @update:collapsed="emit('update:collapsed', $event)">
    <template #title>补录行程</template>
    <template #actions>
      <span class="reim-section-action-btn" @click.stop="emit('add')">
        <el-icon><CirclePlus /></el-icon> 补录行程
      </span>
    </template>
    <el-table :data="itineraries" border style="width: 100%">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column label="出行人员" min-width="140">
        <template #default="{ row }">{{ row.travelerName }}/{{ row.travelerNo }}</template>
      </el-table-column>
      <el-table-column label="出差日期" min-width="200">
        <template #default="{ row }">{{ row.startDate }} 至 {{ row.endDate }}</template>
      </el-table-column>
      <el-table-column label="行程" width="120">
        <template #default="{ row }">{{ row.departCityName }}-{{ row.arriveCityName }}</template>
      </el-table-column>
      <el-table-column prop="description" label="行程说明" min-width="120" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <div class="reim-table-actions">
            <el-icon class="reim-icon-btn" @click="emit('delete', row)"><Delete /></el-icon>
            <el-icon class="reim-icon-btn" @click="emit('edit', row)"><Edit /></el-icon>
            <el-icon class="reim-icon-btn" @click="emit('copy', row)"><CopyDocument /></el-icon>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </SectionPanel>
</template>
