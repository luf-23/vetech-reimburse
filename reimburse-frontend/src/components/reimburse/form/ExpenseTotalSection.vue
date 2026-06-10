<script setup lang="ts">
/**
 * 费用合计区块。
 * 只读展示补助总金额及餐费、交通、通讯三类分项合计，数据由父组件汇总传入。
 */
import SectionPanel from '@/components/reimburse/SectionPanel.vue'
import { formatMoney } from '@/utils/format'

defineProps<{
  collapsed: boolean
  total: number
  meal: number
  transport: number
  comm: number
}>()

const emit = defineEmits<{
  'update:collapsed': [v: boolean]
}>()
</script>

<template>
  <SectionPanel
    :collapsed="collapsed"
    title="费用合计"
    @update:collapsed="emit('update:collapsed', $event)"
  >
    <el-row class="expense-total-row">
      <el-col :span="6">
        <span class="total-label">补助总金额：</span>
        <span>{{ formatMoney(total) }}</span>
      </el-col>
      <el-col :span="6">
        <span class="total-label">餐费补助：</span>
        <span>{{ formatMoney(meal) }}</span>
      </el-col>
      <el-col :span="6">
        <span class="total-label">交通补助：</span>
        <span>{{ formatMoney(transport) }}</span>
      </el-col>
      <el-col :span="6">
        <span class="total-label">通讯补助：</span>
        <span>{{ formatMoney(comm) }}</span>
      </el-col>
    </el-row>
  </SectionPanel>
</template>

<style scoped>
.expense-total-row {
  padding: 8px 0;
  font-size: 14px;
}

.total-label {
  color: var(--reim-text-secondary);
  font-size: 14px;
}

.expense-total-row span:not(.total-label) {
  font-size: 14px;
}
</style>
