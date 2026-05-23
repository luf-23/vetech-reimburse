<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { WarningFilled } from '@element-plus/icons-vue'
import type { ItineraryItem } from '@/types/reimburse'
import { useMasterData } from '@/composables/useMasterData'
import { formatDate, getToday, isItineraryDuplicate } from '@/utils/reimburse'
import dayjs from 'dayjs'

const props = defineProps<{
  visible: boolean
  editData?: ItineraryItem | null
  existingList: ItineraryItem[]
  excludeId?: string
  isCopy?: boolean
}>()

const emit = defineEmits<{
  'update:visible': [v: boolean]
  save: [data: Omit<ItineraryItem, 'id'> & { id?: string }]
}>()

const { cities, reimbursers } = useMasterData()

const travelerId = ref('')
const departCityNo = ref('')
const arriveCityNo = ref('')
const dateRange = ref<[string, string] | null>(null)
const description = ref('')

/** 正在选择区间时暂存的出发日期（用于禁用早于出发的到达日） */
const pickingStartDate = ref<string | null>(null)

const isEdit = computed(() => !!props.editData?.id && !props.isCopy)

const dialogTitle = computed(() => (isEdit.value ? '编辑行程' : '补录行程'))

const today = getToday()
const todayEnd = dayjs(today).endOf('day')

const defaultTime: [Date, Date] = [
  new Date(2000, 0, 1, 0, 0, 0),
  new Date(2000, 0, 1, 0, 0, 0),
]

watch(dateRange, (val) => {
  if (!val?.[0] || val[1]) {
    if (!val?.[0]) pickingStartDate.value = null
  }
})

watch(
  () => props.visible,
  (v) => {
    if (v) {
      pickingStartDate.value = null
      if (props.editData) {
        travelerId.value = props.editData.travelerId
        departCityNo.value = props.editData.departCityNo
        arriveCityNo.value = props.editData.arriveCityNo
        dateRange.value = [
          `${props.editData.startDate} 00:00:00`,
          `${props.editData.endDate} 00:00:00`,
        ]
        description.value = props.editData.description
      } else {
        resetForm()
      }
    }
  },
)

function resetForm() {
  travelerId.value = ''
  departCityNo.value = ''
  arriveCityNo.value = ''
  dateRange.value = null
  description.value = ''
  pickingStartDate.value = null
}

/**
 * 禁用规则：
 * 1. 不可晚于当前日期
 * 2. 选择到达日时，不可早于已选出发日（允许跨月，只要 <= 今天）
 */
function disabledDate(time: Date) {
  const cell = dayjs(time).startOf('day')
  const maxDay = dayjs(today).startOf('day')

  if (cell.isAfter(maxDay)) {
    return true
  }

  if (pickingStartDate.value) {
    const startDay = dayjs(pickingStartDate.value).startOf('day')
    if (cell.isBefore(startDay)) {
      return true
    }
  }

  return false
}

function onCalendarChange(val: [Date, Date] | [string, string] | null) {
  if (!val || !val[0]) {
    pickingStartDate.value = null
    return
  }
  const start = formatDate(val[0] as Date | string)
  const end = val[1] ? formatDate(val[1] as Date | string) : null
  if (end) {
    pickingStartDate.value = null
  } else {
    pickingStartDate.value = start
  }
}

function handleSave() {
  if (!travelerId.value) {
    ElMessage.warning('请选择出行人')
    return
  }
  if (!departCityNo.value) {
    ElMessage.warning('请选择出发城市')
    return
  }
  if (!arriveCityNo.value) {
    ElMessage.warning('请选择到达城市')
    return
  }
  if (!dateRange.value?.[0] || !dateRange.value?.[1]) {
    ElMessage.warning('请选择出发到达日期')
    return
  }
  if (!description.value.trim()) {
    ElMessage.warning('请输入行程说明')
    return
  }
  if (description.value.length > 500) {
    ElMessage.warning('行程说明不可超过500字')
    return
  }

  const start = formatDate(dateRange.value[0])
  const end = formatDate(dateRange.value[1])

  if (dayjs(end).isBefore(dayjs(start), 'day')) {
    ElMessage.warning('到达日期不可早于出发日期')
    return
  }
  if (dayjs(start).isAfter(todayEnd) || dayjs(end).isAfter(todayEnd)) {
    ElMessage.warning('出发到达日期不可晚于当前日期')
    return
  }

  const excludeId = isEdit.value ? props.editData?.id : props.excludeId
  if (isItineraryDuplicate(props.existingList, travelerId.value, start, end, excludeId)) {
    ElMessage.warning('该人员在此日期范围内已有行程，不可重复')
    return
  }

  const traveler = reimbursers.find((r) => r.reimburserId === travelerId.value)!
  const depart = cities.find((c) => c.cityNo === departCityNo.value)!
  const arrive = cities.find((c) => c.cityNo === arriveCityNo.value)!

  emit('save', {
    id: isEdit.value ? props.editData?.id : undefined,
    travelerId: traveler.reimburserId,
    travelerName: traveler.reimburserName,
    travelerNo: traveler.reimburserNo,
    departCityNo: depart.cityNo,
    departCityName: depart.cityName,
    arriveCityNo: arrive.cityNo,
    arriveCityName: arrive.cityName,
    startDate: start,
    endDate: end,
    description: description.value.trim(),
  })
  emit('update:visible', false)
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="dialogTitle"
    width="640px"
    destroy-on-close
    class="itinerary-dialog"
    align-center
    @update:model-value="emit('update:visible', $event)"
    @closed="pickingStartDate = null"
  >
    <div class="itinerary-warning-bar">
      <el-icon class="warning-icon"><WarningFilled /></el-icon>
      <div class="warning-text">
        <p>仅可补录未从申请单带入或未产生费用的行程信息</p>
        <p>
          跨天跨城行程填写说明： 出发城市-到达城市：武汉-北京; 出发日期-到达日期：1号-5号;
          1号~5号补助按北京匹配;
        </p>
      </div>
    </div>

    <el-form label-width="120px" label-position="right" class="itinerary-form">
      <el-form-item label="出行人" required>
        <el-select v-model="travelerId" placeholder="请选择" style="width: 100%">
          <el-option
            v-for="r in reimbursers"
            :key="r.reimburserId"
            :label="r.reimburserName"
            :value="r.reimburserId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="出发城市" required>
        <el-select
          v-model="departCityNo"
          placeholder="请选择"
          filterable
          clearable
          style="width: 100%"
        >
          <el-option v-for="c in cities" :key="c.cityNo" :label="c.cityName" :value="c.cityNo" />
        </el-select>
      </el-form-item>
      <el-form-item label="到达城市" required>
        <el-select
          v-model="arriveCityNo"
          placeholder="请选择"
          filterable
          clearable
          style="width: 100%"
        >
          <el-option v-for="c in cities" :key="c.cityNo" :label="c.cityName" :value="c.cityNo" />
        </el-select>
      </el-form-item>
      <el-form-item label="出发到达日期" required>
        <el-date-picker
          v-model="dateRange"
          type="datetimerange"
          unlink-panels
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD HH:mm:ss"
          value-format="YYYY-MM-DD HH:mm:ss"
          :default-time="defaultTime"
          :disabled-date="disabledDate"
          clearable
          style="width: 100%"
          @calendar-change="onCalendarChange"
        />
      </el-form-item>
      <el-form-item label="行程说明" required>
        <el-input
          v-model="description"
          type="textarea"
          :rows="3"
          placeholder="行程说明"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="itinerary-dialog-footer">
        <el-button class="btn-cancel" @click="emit('update:visible', false)">取消</el-button>
        <el-button type="primary" class="btn-save" @click="handleSave">保存</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.itinerary-dialog :deep(.el-dialog__header) {
  padding: 16px 20px 12px;
  margin-right: 0;
  border-bottom: 1px solid #f0f0f0;
}

.itinerary-dialog :deep(.el-dialog__title) {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.itinerary-dialog :deep(.el-dialog__body) {
  padding: 16px 20px 8px;
}

.itinerary-dialog :deep(.el-dialog__footer) {
  padding: 12px 20px 16px;
  border-top: 1px solid #f0f0f0;
}

.itinerary-warning-bar {
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 2px;
  padding: 10px 12px;
  margin-bottom: 20px;
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.warning-icon {
  color: #faad14;
  font-size: 16px;
  margin-top: 2px;
  flex-shrink: 0;
}

.warning-text {
  flex: 1;
  font-size: 14px;
  line-height: 1.6;
  color: #333;
}

.warning-text p {
  margin: 0;
}

.warning-text p + p {
  margin-top: 4px;
}

.itinerary-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.itinerary-form :deep(.el-form-item__label) {
  font-size: 14px;
  color: #333;
  padding-right: 12px;
}

.itinerary-form :deep(.el-form-item__label)::before {
  display: none !important;
}

.itinerary-form :deep(.el-form-item.is-required:not(.is-no-asterisk) > .el-form-item__label::after) {
  content: '*';
  color: #ff4d4f;
  margin-left: 4px;
}

.itinerary-form :deep(.el-input__inner),
.itinerary-form :deep(.el-textarea__inner),
.itinerary-form :deep(.el-select__wrapper),
.itinerary-form :deep(.el-range-input) {
  font-size: 14px;
}

.itinerary-form :deep(.el-input__wrapper),
.itinerary-form :deep(.el-textarea__inner) {
  border-radius: 2px;
}

.itinerary-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.btn-cancel {
  min-width: 72px;
  color: #1890ff;
  border-color: #1890ff;
  background: #fff;
}

.btn-cancel:hover {
  color: #40a9ff;
  border-color: #40a9ff;
  background: #fff;
}

.btn-save {
  min-width: 72px;
}
</style>
