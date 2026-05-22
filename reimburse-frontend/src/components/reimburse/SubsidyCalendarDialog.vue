<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Location } from '@element-plus/icons-vue'
import type { SubsidyDayItem, SubsidyInfoItem } from '@/types/reimburse'
import { calcCalendarTotals, formatMoney } from '@/utils/reimburse'

const props = defineProps<{
  visible: boolean
  subsidy: SubsidyInfoItem | null
  /** 5.2.2.4-1 与主单业务类型一致 */
  businessTypeName: string
}>()

const emit = defineEmits<{
  'update:visible': [v: boolean]
  confirm: [calendar: SubsidyDayItem[]]
}>()

const calendar = ref<SubsidyDayItem[]>([])

const SUB_KEYS = ['meal', 'transport', 'comm'] as const
type SubKey = (typeof SUB_KEYS)[number]

watch(
  () => props.visible,
  (v) => {
    if (v && props.subsidy) {
      calendar.value = JSON.parse(JSON.stringify(props.subsidy.calendar))
    }
  },
)

/** 5.2.2.4-4 / 5.2.2.4-5 仅统计已勾选项 */
const totals = computed(() => calcCalendarTotals(calendar.value))

/** 5.2.2.4-3 行程天数、出发-到达城市 */
const routeLabel = computed(() => {
  if (!props.subsidy) return ''
  const parts = props.subsidy.route.split('-')
  const from = parts[0]?.trim() ?? ''
  const to = parts[1]?.trim() ?? props.subsidy.subsidyCityName
  return `行程天数 ${from} - ${to} ${props.subsidy.days}天`
})

function countAllChecks() {
  let total = 0
  let checked = 0
  for (const day of calendar.value) {
    for (const key of SUB_KEYS) {
      total++
      if (day[key].checked) checked++
    }
  }
  return { total, checked }
}

function isDayAllChecked(day: SubsidyDayItem): boolean {
  return day.meal.checked && day.transport.checked && day.comm.checked
}

function isDayIndeterminate(day: SubsidyDayItem): boolean {
  const c = [day.meal.checked, day.transport.checked, day.comm.checked]
  return c.some(Boolean) && !c.every(Boolean)
}

function isColAllChecked(key: SubKey): boolean {
  return calendar.value.length > 0 && calendar.value.every((d) => d[key].checked)
}

function isColIndeterminate(key: SubKey): boolean {
  const n = calendar.value.filter((d) => d[key].checked).length
  return n > 0 && n < calendar.value.length
}

/** 5.2.2.4-9 全选 / 取消全选所有复选框 */
const selectAll = computed({
  get() {
    const { total, checked } = countAllChecks()
    return total > 0 && checked === total
  },
  set(v: boolean) {
    calendar.value.forEach((day) => {
      for (const key of SUB_KEYS) {
        day[key].checked = v
      }
    })
  },
})

const selectAllIndeterminate = computed(() => {
  const { total, checked } = countAllChecks()
  return checked > 0 && checked < total
})

/** 5.2.2.4-6 横向：选中整行所有补助项 */
function toggleDay(day: SubsidyDayItem, checked: boolean) {
  for (const key of SUB_KEYS) {
    setSubsidyChecked(day, key, checked)
  }
}

/** 5.2.2.4-7 纵向：选中整列补助项 */
function toggleCol(key: SubKey, checked: boolean) {
  calendar.value.forEach((day) => {
    setSubsidyChecked(day, key, checked)
  })
}

function setSubsidyChecked(day: SubsidyDayItem, key: SubKey, checked: boolean) {
  day[key].checked = checked
  if (!checked) {
    day[key].amount = day[key].standard
  }
}

function onAmountChange(day: SubsidyDayItem, key: SubKey, val: number | undefined) {
  if (!day[key].checked) return
  let num = val ?? 0
  if (num < 0) num = 0
  if (num > day[key].standard) num = day[key].standard
  day[key].amount = num
}

function handleConfirm() {
  emit('confirm', JSON.parse(JSON.stringify(calendar.value)))
  emit('update:visible', false)
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="补助日历"
    width="1100px"
    destroy-on-close
    class="subsidy-calendar-dialog"
    align-center
    @update:model-value="emit('update:visible', $event)"
  >
    <div v-if="subsidy" class="calendar-layout">
      <!-- 左侧：出差类型、日期轴、金额汇总 -->
      <div class="calendar-sidebar">
        <div class="sidebar-type">
          <span class="sidebar-type-label">出差类型</span>
          <span class="sidebar-type-value">{{ businessTypeName }}</span>
        </div>
        <div class="sidebar-timeline">
          <div class="timeline-track">
            <div class="timeline-node">
              <span class="node-dot" />
              <div class="node-content">
                <span class="node-label">开始日期</span>
                <span class="node-date">{{ subsidy.startDate }}</span>
              </div>
            </div>
            <div class="timeline-bar">{{ routeLabel }}</div>
            <div class="timeline-node">
              <span class="node-dot" />
              <div class="node-content">
                <span class="node-label">结束日期</span>
                <span class="node-date">{{ subsidy.endDate }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="sidebar-summary">
          <div class="summary-row">
            <span>补助金额</span>
            <span class="summary-value">CNY {{ formatMoney(totals.subsidyAmount) }}</span>
          </div>
          <div class="summary-row">
            <span>标准总额</span>
            <span class="summary-value">CNY {{ formatMoney(totals.standardTotal) }}</span>
          </div>
          <div class="summary-row">
            <span>补助金额</span>
            <span class="summary-value">CNY {{ formatMoney(totals.subsidyAmount) }}</span>
          </div>
        </div>
      </div>

      <!-- 右侧：补助日历表 -->
      <div class="calendar-main">
        <div class="calendar-main-header">
          <span class="calendar-main-title">出差补助</span>
          <el-checkbox
            v-model="selectAll"
            :indeterminate="selectAllIndeterminate"
            label="全选"
          />
        </div>
        <div class="calendar-table-wrap">
          <table class="calendar-table">
            <thead>
              <tr>
                <th class="col-date">出差日期</th>
                <th class="col-city">补助城市</th>
                <th v-for="key in SUB_KEYS" :key="key" class="col-subsidy">
                  <div class="th-inner">
                    <el-checkbox
                      :model-value="isColAllChecked(key)"
                      :indeterminate="isColIndeterminate(key)"
                      @change="(v: boolean) => toggleCol(key, v)"
                    />
                    <span>{{ key === 'meal' ? '餐费补助' : key === 'transport' ? '交通补助' : '通讯补助' }}</span>
                  </div>
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="day in calendar" :key="day.date">
                <td class="date-cell">
                  <el-checkbox
                    :model-value="isDayAllChecked(day)"
                    :indeterminate="isDayIndeterminate(day)"
                    @change="(v: boolean) => toggleDay(day, v)"
                  />
                  <div class="date-text">
                    <div class="date-line">{{ day.date }}</div>
                    <div class="weekday">{{ day.weekday }}</div>
                  </div>
                  <el-icon class="loc-icon"><Location /></el-icon>
                </td>
                <td class="city-cell">{{ day.cityName }}</td>
                <td v-for="key in SUB_KEYS" :key="key" class="subsidy-td">
                  <div class="subsidy-cell">
                    <div class="std-label">
                      CNY {{ formatMoney(day[key].standard) }} / 天
                    </div>
                    <div class="subsidy-input-row">
                      <el-checkbox
                        :model-value="day[key].checked"
                        @change="(v: boolean) => setSubsidyChecked(day, key, v)"
                      />
                      <el-input-number
                        :model-value="day[key].amount"
                        :disabled="!day[key].checked"
                        :min="0"
                        :max="day[key].standard"
                        :precision="2"
                        :controls="false"
                        size="small"
                        class="amount-input"
                        @update:model-value="onAmountChange(day, key, $event)"
                      />
                    </div>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="calendar-dialog-footer">
        <el-button class="btn-cancel" @click="emit('update:visible', false)">取消</el-button>
        <el-button type="primary" @click="handleConfirm">确认</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.subsidy-calendar-dialog :deep(.el-dialog__header) {
  padding: 16px 20px 12px;
  margin-right: 0;
  border-bottom: 1px solid #f0f0f0;
}

.subsidy-calendar-dialog :deep(.el-dialog__title) {
  font-size: 16px;
  font-weight: 500;
}

.subsidy-calendar-dialog :deep(.el-dialog__body) {
  padding: 16px 20px;
}

.subsidy-calendar-dialog :deep(.el-dialog__footer) {
  padding: 12px 20px 16px;
  border-top: 1px solid #f0f0f0;
}

.calendar-layout {
  display: flex;
  gap: 16px;
  min-height: 400px;
}

.calendar-sidebar {
  width: 210px;
  flex-shrink: 0;
  border: 1px solid #e8e8e8;
  padding: 14px 12px;
  background: #fff;
}

.sidebar-type {
  margin-bottom: 14px;
  font-size: 14px;
}

.sidebar-type-label {
  color: #333;
}

.sidebar-type-value {
  margin-left: 8px;
  color: #fa8c16;
}

.sidebar-timeline {
  margin-bottom: 18px;
}

.timeline-track {
  position: relative;
  padding-left: 2px;
}

.timeline-track::before {
  content: '';
  position: absolute;
  left: 5px;
  top: 14px;
  bottom: 14px;
  width: 2px;
  background: #1890ff;
}

.timeline-node {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  position: relative;
  z-index: 1;
}

.node-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #1890ff;
  border: 2px solid #fff;
  box-shadow: 0 0 0 1px #1890ff;
  flex-shrink: 0;
  margin-top: 4px;
}

.node-label {
  font-size: 12px;
  color: #999;
  display: block;
}

.node-date {
  font-size: 14px;
  color: #333;
}

.timeline-bar {
  background: #1890ff;
  color: #fff;
  padding: 8px 10px;
  font-size: 12px;
  margin: 8px 0 8px 22px;
  border-radius: 2px;
  line-height: 1.5;
  word-break: break-all;
}

.sidebar-summary .summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-size: 14px;
  color: #333;
}

.summary-value {
  color: #fa8c16;
  font-weight: 500;
  white-space: nowrap;
}

.calendar-main {
  flex: 1;
  border: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.calendar-main-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  border-bottom: 1px solid #e8e8e8;
}

.calendar-main-title {
  font-size: 14px;
  font-weight: 500;
}

.calendar-table-wrap {
  overflow: auto;
  flex: 1;
}

.calendar-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.calendar-table th,
.calendar-table td {
  border-bottom: 1px solid #e8e8e8;
  padding: 10px 8px;
  text-align: center;
  vertical-align: middle;
}

.calendar-table th {
  background: #fafafa;
  font-weight: normal;
}

.th-inner {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  justify-content: center;
}

.date-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  text-align: left;
  padding-left: 10px !important;
  min-width: 150px;
}

.date-text {
  flex: 1;
}

.date-line {
  font-size: 14px;
  color: #333;
}

.weekday {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

.loc-icon {
  color: #bfbfbf;
  font-size: 14px;
  flex-shrink: 0;
}

.city-cell {
  color: #333;
}

.subsidy-cell .std-label {
  font-size: 12px;
  color: #fa8c16;
  margin-bottom: 6px;
}

.subsidy-input-row {
  display: flex;
  align-items: center;
  gap: 6px;
  justify-content: center;
}

.amount-input {
  width: 76px;
}

.amount-input :deep(.el-input__inner) {
  text-align: center;
}

.amount-input.is-disabled :deep(.el-input__wrapper) {
  background: #f5f5f5;
}

.calendar-dialog-footer {
  display: flex;
  justify-content: center;
  gap: 12px;
  width: 100%;
}

.btn-cancel {
  min-width: 80px;
  color: #1890ff;
  border-color: #1890ff;
  background: #fff;
}

.btn-cancel:hover {
  color: #40a9ff;
  border-color: #40a9ff;
  background: #fff;
}
</style>
