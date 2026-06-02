<script setup lang="ts">
import { CirclePlus, Delete, Refresh } from '@element-plus/icons-vue'
import SectionPanel from '@/components/reimburse/SectionPanel.vue'
import type { AllocationItem, Project, ReimCompany } from '@/types/reimburse'
import { formatMoney, formatPercent } from '@/utils/format'

defineProps<{
  allocations: AllocationItem[]
  collapsed: boolean
  expenseTotal: number
  companies: ReimCompany[]
  projects: Project[]
  totalRatio: number
  totalAmount: number
  getRatioInputValue: (index: number) => number | undefined
}>()

const emit = defineEmits<{
  'update:collapsed': [v: boolean]
  'cost-change': [row: AllocationItem, id: string | undefined]
  'project-change': [row: AllocationItem, id: string | undefined]
  'ratio-change': [index: number, val: number | null | undefined]
  'equal-split': []
  'add-row': []
  'delete-row': [row: AllocationItem, index: number]
}>()
</script>

<template>
  <SectionPanel :collapsed="collapsed" @update:collapsed="emit('update:collapsed', $event)">
    <template #title>
      <span class="section-title-main">费用归属及分摊</span>
      <span class="reim-subsidy-header-extra">(分摊金额: {{ formatMoney(expenseTotal) }})</span>
    </template>
    <el-table :data="allocations" border class="allocation-table" style="width: 100%">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column label="费用归属" min-width="180">
        <template #header><span class="allocation-required">*</span> 费用归属</template>
        <template #default="{ row }">
          <el-select
            :model-value="row.costAttributionId"
            placeholder="请选择"
            clearable
            style="width: 100%"
            @update:model-value="emit('cost-change', row, $event)"
          >
            <el-option
              v-for="c in companies"
              :key="c.reimCompanyId"
              :label="c.reimCompanyName"
              :value="c.reimCompanyId"
            />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="项目" min-width="180">
        <template #default="{ row }">
          <el-select
            :model-value="row.projectId"
            placeholder="请选择"
            clearable
            style="width: 100%"
            @update:model-value="emit('project-change', row, $event)"
          >
            <el-option
              v-for="p in projects"
              :key="p.projectId"
              :label="p.projectName"
              :value="p.projectId"
            />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column width="140" align="right" header-align="right">
        <template #header>
          <div class="allocation-ratio-header-row">
            <span><span class="allocation-required">*</span> 分摊比例</span>
            <el-icon class="allocation-refresh-icon" title="均摊" @click.stop="emit('equal-split')">
              <Refresh />
            </el-icon>
          </div>
        </template>
        <template #default="{ row, $index }">
          <div v-if="$index === 0" class="allocation-readonly">{{ formatPercent(row.ratio) }}</div>
          <el-input-number
            v-else
            :model-value="getRatioInputValue($index)"
            :min="0"
            :max="100"
            :precision="2"
            :controls="true"
            controls-position="right"
            class="allocation-ratio-input"
            @update:model-value="emit('ratio-change', $index, $event)"
          />
        </template>
      </el-table-column>
      <el-table-column label="分摊金额" width="120" align="right" header-align="right">
        <template #header><span class="allocation-required">*</span> 分摊金额</template>
        <template #default="{ row, $index }">
          <div :class="$index === 0 ? 'allocation-readonly' : 'allocation-amount-cell'">
            {{ formatMoney(row.amount) }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80">
        <template #default="{ row, $index }">
          <el-icon class="reim-icon-btn" @click="emit('delete-row', row, $index)"><Delete /></el-icon>
        </template>
      </el-table-column>
    </el-table>
    <div class="allocation-table-extra">
      <div class="allocation-add-row" @click="emit('add-row')">
        <el-icon class="allocation-add-icon"><CirclePlus /></el-icon>
        <span>添加一行</span>
      </div>
      <div class="allocation-summary-row">
        <span class="allocation-summary-label">合计</span>
        <span class="allocation-summary-ratio">{{ formatPercent(totalRatio) }}</span>
        <span class="allocation-summary-amount">CNY {{ formatMoney(totalAmount) }}</span>
      </div>
    </div>
  </SectionPanel>
</template>

<style scoped>
.section-title-main {
  font-size: 16px;
  font-weight: 500;
}

.allocation-required {
  color: #f56c6c;
}

.allocation-table :deep(.el-table__header th) {
  background: #fafafa !important;
  vertical-align: middle;
}

.allocation-ratio-header-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
  width: 100%;
}

.allocation-refresh-icon {
  color: #1890ff;
  font-size: 16px;
  cursor: pointer;
}

.allocation-refresh-icon:hover {
  opacity: 0.75;
}

.allocation-readonly {
  text-align: right;
  padding: 0 11px;
  height: 32px;
  line-height: 32px;
  background: #f5f5f5;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  color: #333;
  box-sizing: border-box;
}

.allocation-amount-cell {
  text-align: right;
  padding-right: 4px;
}

.allocation-ratio-input {
  width: 100%;
}

.allocation-ratio-input :deep(.el-input__inner) {
  text-align: right;
}

.allocation-table-extra {
  border: 1px solid #e8e8e8;
  border-top: none;
}

.allocation-add-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px 0;
  cursor: pointer;
  font-size: 14px;
  color: #1890ff;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
}

.allocation-add-row:hover {
  opacity: 0.85;
}

.allocation-add-icon {
  font-size: 16px;
}

.allocation-summary-row {
  display: grid;
  grid-template-columns: 60px minmax(180px, 1fr) minmax(180px, 1fr) 140px 120px 80px;
  align-items: center;
  min-height: 40px;
  padding: 0 12px;
  background: #fffbe6;
  font-size: 14px;
}

.allocation-summary-label {
  color: #333;
  font-weight: 500;
}

.allocation-summary-ratio {
  grid-column: 4;
  text-align: right;
  color: #fa8c16;
  font-weight: 500;
}

.allocation-summary-amount {
  grid-column: 5;
  text-align: right;
  color: #fa8c16;
  font-weight: 500;
}
</style>
