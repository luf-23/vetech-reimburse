<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Location } from '@element-plus/icons-vue'
import type { SubsidyDayItem, SubsidyInfoItem } from '@/types/reimburse'
import { calcCalendarTotals, formatMoney, normalizeSubsidyCalendar } from '@/utils/reimburse'

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

const SUB_LABELS: Record<SubKey, string> = {
  meal: '餐费补助',
  transport: '交通补助',
  comm: '通讯补助',
}

watch(
  () => props.visible,
  (v) => {
    if (v && props.subsidy) {
      calendar.value = normalizeSubsidyCalendar(
        JSON.parse(JSON.stringify(props.subsidy.calendar)),
      )
    }
  },
)

/** 5.2.2.4-4 / 5.2.2.4-5 仅统计已勾选项 */
const totals = computed(() => calcCalendarTotals(calendar.value))

/** 5.2.2.4-3 行程中间行：城市与天数 */
const routeParts = computed(() => {
  if (!props.subsidy) return { middle: '', days: '' }
  const parts = props.subsidy.route.split('-')
  const from = parts[0]?.trim() ?? ''
  const to = parts[1]?.trim() ?? props.subsidy.subsidyCityName
  return { middle: `${from} - ${to}`, days: `${props.subsidy.days}天` }
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

/** 5.2.2.4-7 当前行全部选中时，出差日期复选框才为选中 */
function isDayAllChecked(day: SubsidyDayItem): boolean {
  return SUB_KEYS.every((key) => day[key].checked)
}

function isDayIndeterminate(day: SubsidyDayItem): boolean {
  const checkedCount = SUB_KEYS.filter((key) => day[key].checked).length
  return checkedCount > 0 && checkedCount < SUB_KEYS.length
}

/** 5.2.2.4-6 当前列全部选中时，表头复选框才为选中 */
function isColAllChecked(key: SubKey): boolean {
  return calendar.value.length > 0 && calendar.value.every((d) => d[key].checked)
}

function isColIndeterminate(key: SubKey): boolean {
  const n = calendar.value.filter((d) => d[key].checked).length
  return n > 0 && n < calendar.value.length
}

/** 5.2.2.4-9 全选：仅当日历内每一个复选框都选中时才为选中态 */
const selectAllChecked = computed(() => {
  const { total, checked } = countAllChecks()
  return total > 0 && checked === total
})

/** 仅选中部分列/行时，全选保持未勾选（不显示半选） */
const selectAllIndeterminate = computed(() => false)

function onSelectAllChange(checked: boolean) {
  calendar.value.forEach((day) => {
    for (const key of SUB_KEYS) {
      setSubsidyChecked(day, key, checked)
    }
  })
}

/** 5.2.2.4-6 横向：出差日期列选中整行 */
function toggleDay(day: SubsidyDayItem, checked: boolean) {
  for (const key of SUB_KEYS) {
    setSubsidyChecked(day, key, checked)
  }
}

/** 5.2.2.4-7 纵向：表头选中整列 */
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

/** 5.2.2.4-9 未选中不可编辑；仅正数且不大于标准额 */
function onAmountChange(day: SubsidyDayItem, key: SubKey, val: number | null | undefined) {
  if (!day[key].checked) return
  let num = val ?? 0
  if (num < 0) num = 0
  if (num > day[key].standard) num = day[key].standard
  day[key].amount = +num.toFixed(2)
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
        <div class="trip-detail-box">
          <div class="trip-axis-line" aria-hidden="true" />
          <span class="trip-label trip-label-start">开始日期</span>
          <span class="trip-dot trip-dot-top" />
          <span class="trip-value">{{ subsidy.startDate }}</span>
          <div class="trip-row-route">
            <span class="trip-route-label">行程天数</span>
            <span class="trip-route-mid">{{ routeParts.middle }}</span>
            <span class="trip-route-days">{{ routeParts.days }}</span>
          </div>
          <span class="trip-label trip-label-end">结束日期</span>
          <span class="trip-dot trip-dot-bottom" />
          <span class="trip-value trip-value-end">{{ subsidy.endDate }}</span>
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
        </div>
      </div>

      <!-- 右侧：补助日历表 -->
      <div class="calendar-main">
        <div class="calendar-main-header">
          <span class="calendar-main-title">出差补助</span>
          <el-checkbox
            :model-value="selectAllChecked"
            :indeterminate="selectAllIndeterminate"
            @update:model-value="onSelectAllChange"
          >
            全选
          </el-checkbox>
        </div>
        <div class="calendar-table-wrap">
          <table class="calendar-table">
            <colgroup>
              <col class="col-date" />
              <col class="col-city" />
              <col class="col-subsidy" />
              <col class="col-subsidy" />
              <col class="col-subsidy" />
            </colgroup>
            <thead>
              <tr>
                <th class="col-date">出差日期</th>
                <th class="col-city">补助城市</th>
                <th v-for="key in SUB_KEYS" :key="key" class="col-subsidy">
                  <div class="th-inner">
                    <el-checkbox
                      :model-value="isColAllChecked(key)"
                      :indeterminate="isColIndeterminate(key)"
                      @update:model-value="(v: boolean) => toggleCol(key, v)"
                    />
                    <span>{{ SUB_LABELS[key] }}</span>
                  </div>
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="day in calendar" :key="day.date">
                <td class="date-cell">
                  <div class="date-cell-inner">
                    <el-checkbox
                      :model-value="isDayAllChecked(day)"
                      :indeterminate="isDayIndeterminate(day)"
                      @update:model-value="(v: boolean) => toggleDay(day, v)"
                    />
                    <div class="date-text">
                      <div class="date-line">{{ day.date }}</div>
                      <div class="weekday">{{ day.weekday }}</div>
                    </div>
                  </div>
                </td>
                <td class="city-cell">
                  <div class="city-cell-inner">
                    <el-icon class="loc-icon"><Location /></el-icon>
                    <span>{{ day.cityName }}</span>
                  </div>
                </td>
                <td v-for="key in SUB_KEYS" :key="key" class="subsidy-td">
                  <div class="subsidy-cell">
                    <div class="std-label">
                      CNY {{ formatMoney(day[key].standard) }} / 天
                    </div>
                    <div class="subsidy-input-row">
                      <el-checkbox
                        :model-value="day[key].checked"
                        @update:model-value="(v: boolean) => setSubsidyChecked(day, key, v)"
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
  overflow: hidden;
}

.subsidy-calendar-dialog :deep(.el-dialog__footer) {
  padding: 12px 20px 16px;
  border-top: 1px solid #f0f0f0;
}

.calendar-layout {
  display: flex;
  gap: 16px;
  height:70vh;
}

.calendar-sidebar {
  width: 220px;
  flex-shrink: 0;
  padding: 0;
  background: transparent;
  border: none;
  align-self: flex-start;
}

.sidebar-type {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
  font-size: 14px;
  line-height: 22px;
}

.sidebar-type-label {
  color: #333;
  font-weight: 500;
  flex-shrink: 0;
}

.sidebar-type-value {
  color: #fa8c16;
  font-size: 14px;
}

/* 行程明细框：三列网格，竖线与圆点同列居中 */
.trip-detail-box {
  display: grid;
  grid-template-columns: 62px 24px 1fr;
  grid-template-rows: auto auto auto;
  align-items: center;
  column-gap: 0;
  row-gap: 0;
  border: 1px solid #e8e8e8;
  background: #fff;
  padding: 10px 8px;
  margin-bottom: 16px;
}

.trip-axis-line {
  grid-column: 2;
  grid-row: 1 / 4;
  justify-self: center;
  align-self: stretch;
  width: 2px;
  background: #1890ff;
  margin: 11px 0;
  z-index: 0;
}

.trip-label {
  font-size: 12px;
  color: #999;
}

.trip-label-start {
  grid-column: 1;
  grid-row: 1;
}

.trip-label-end {
  grid-column: 1;
  grid-row: 3;
}

.trip-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #fff;
  border: 2px solid #1890ff;
  box-sizing: border-box;
  justify-self: center;
  align-self: center;
  z-index: 1;
}

.trip-dot-top {
  grid-column: 2;
  grid-row: 1;
}

.trip-dot-bottom {
  grid-column: 2;
  grid-row: 3;
}

.trip-value {
  grid-column: 3;
  grid-row: 1;
  font-size: 14px;
  color: #333;
  text-align: right;
  padding-right: 2px;
}

.trip-value-end {
  grid-row: 3;
}

.trip-row-route {
  grid-column: 1 / -1;
  grid-row: 2;
  display: grid;
  grid-template-columns: 62px 1fr auto;
  align-items: center;
  gap: 4px;
  background: #1890ff;
  color: #fff;
  font-size: 12px;
  padding: 8px;
  margin: 6px 0;
  line-height: 1.4;
  z-index: 1;
}

.trip-route-label {
  white-space: nowrap;
}

.trip-route-mid {
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.trip-route-days {
  white-space: nowrap;
  text-align: right;
}

.sidebar-summary .summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-size: 14px;
  color: #333;
}

.sidebar-summary .summary-row:last-child {
  margin-bottom: 0;
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
  min-height: 0;
  overflow: hidden;
}

.calendar-main-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.calendar-main-title {
  font-size: 14px;
  font-weight: 500;
}

.calendar-table-wrap {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: thin;
  scrollbar-color: #c1c1c1 transparent;
}

.calendar-table-wrap::-webkit-scrollbar {
  width: 4px;
}

.calendar-table-wrap::-webkit-scrollbar-track {
  background: transparent;
}

.calendar-table-wrap::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 2px;
}

.calendar-table-wrap::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

.calendar-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  font-size: 14px;
}

.calendar-table th,
.calendar-table td {
  border: 1px solid #e8e8e8;
  padding: 10px 8px;
  text-align: center;
  vertical-align: middle;
}

.calendar-table thead th {
  border-top: none;
}

.calendar-table thead th:first-child,
.calendar-table tbody td:first-child {
  border-left: none;
}

.calendar-table thead th:last-child,
.calendar-table tbody td:last-child {
  border-right: none;
}

.calendar-table tbody tr:last-child td {
  border-bottom: none;
}

.calendar-table th {
  background: #fafafa;
  font-weight: normal;
  position: sticky;
  top: 0;
  z-index: 1;
}

.col-date {
  width: 168px;
}

.col-city {
  width: 88px;
}

.col-subsidy {
  width: auto;
}

.th-inner {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  justify-content: center;
}

.date-cell {
  text-align: left;
}

.date-cell-inner {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-text {
  min-width: 0;
}

.date-line {
  font-size: 14px;
  color: #333;
  line-height: 20px;
}

.weekday {
  font-size: 12px;
  color: #999;
  line-height: 18px;
}

.city-cell-inner {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  justify-content: center;
}

.loc-icon {
  color: #bfbfbf;
  font-size: 14px;
  flex-shrink: 0;
}

.city-cell {
  color: #333;
}

.subsidy-td {
  padding: 8px 6px !important;
}

.subsidy-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.subsidy-cell .std-label {
  font-size: 12px;
  color: #fa8c16;
  line-height: 18px;
  white-space: nowrap;
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
