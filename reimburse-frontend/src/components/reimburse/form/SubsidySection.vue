<script setup lang="ts">
/**
 * 补助信息区块。
 * 展示按出行人/行程汇总的补助列表（天数、路线、城市、申请金额与补助金额）。
 * 标题栏显示 summaryTotal 及 summaryParts 汇总文案；顶部黄色提示条说明核减规则。
 * 点击编辑图标打开 SubsidyCalendarDialog，由父组件传入 calendar 数据并回写确认结果。
 */
import { Edit, WarningFilled } from '@element-plus/icons-vue'
import SectionPanel from '@/components/reimburse/SectionPanel.vue'
import type { SubsidyInfoItem } from '@/types/reimburse'
import { formatMoney } from '@/utils/format'

const SUBSIDY_TIP =
  '1、请根据实际出差日期选择补助2、出差期间当日用餐安排的请自行核减当日餐补3、出差期间当日有用车的，请自行核减当日交补'

defineProps<{
  subsidies: SubsidyInfoItem[]
  collapsed: boolean
  summaryTotal: number
  summaryParts: string
}>()

const emit = defineEmits<{
  'update:collapsed': [v: boolean]
  edit: [row: SubsidyInfoItem]
}>()
</script>

<template>
  <SectionPanel :collapsed="collapsed" @update:collapsed="emit('update:collapsed', $event)">
    <template #title>
      <span class="section-title-main">补助信息</span>
      <span v-if="subsidies.length" class="reim-subsidy-header-extra">
        {{ formatMoney(summaryTotal) }}
        ({{ summaryParts }})
      </span>
    </template>
    <el-tooltip :content="SUBSIDY_TIP" placement="top" :show-after="300">
      <div class="subsidy-tip-bar">
        <el-icon class="subsidy-tip-icon"><WarningFilled /></el-icon>
        <span class="subsidy-tip-text">{{ SUBSIDY_TIP }}</span>
      </div>
    </el-tooltip>
    <el-table :data="subsidies" border class="subsidy-info-table" style="width: 100%">
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="travelerName" label="出行人" width="100" />
      <el-table-column label="出差日期" min-width="200">
        <template #default="{ row }">{{ row.startDate }} 至 {{ row.endDate }}</template>
      </el-table-column>
      <el-table-column prop="days" label="补助天数" width="90" align="center" />
      <el-table-column prop="route" label="行程" width="110" />
      <el-table-column prop="subsidyCityName" label="补助城市" width="100" />
      <el-table-column label="申请金额" width="100" align="right" header-align="right">
        <template #default="{ row }">{{ formatMoney(row.applyAmount) }}</template>
      </el-table-column>
      <el-table-column label="补助金额" width="100" align="right" header-align="right">
        <template #default="{ row }">{{ formatMoney(row.subsidyAmount) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="72" align="center">
        <template #default="{ row }">
          <el-icon class="subsidy-edit-icon" @click="emit('edit', row)"><Edit /></el-icon>
        </template>
      </el-table-column>
    </el-table>
  </SectionPanel>
</template>

<style scoped>
.section-title-main {
  font-size: 16px;
  font-weight: 500;
}

.subsidy-tip-bar {
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 2px;
  padding: 8px 12px;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: default;
}

.subsidy-tip-icon {
  color: #faad14;
  font-size: 16px;
  flex-shrink: 0;
}

.subsidy-tip-text {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.subsidy-info-table :deep(.el-table__header th) {
  background: #fafafa !important;
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.subsidy-info-table :deep(.el-table__body td) {
  font-size: 14px;
  color: #333;
}

.subsidy-edit-icon {
  color: #1890ff;
  font-size: 16px;
  cursor: pointer;
}

.subsidy-edit-icon:hover {
  opacity: 0.75;
}
</style>
