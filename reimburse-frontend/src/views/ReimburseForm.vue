<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CirclePlus,
  CopyDocument,
  Delete,
  Edit,
  Refresh,
  WarningFilled,
} from '@element-plus/icons-vue'
import SectionPanel from '@/components/reimburse/SectionPanel.vue'
import ItineraryDialog from '@/components/reimburse/ItineraryDialog.vue'
import SubsidyCalendarDialog from '@/components/reimburse/SubsidyCalendarDialog.vue'
import type { AllocationItem, ItineraryItem, ReimburseFormData, SubsidyInfoItem } from '@/types/reimburse'
import { useMasterData } from '@/composables/useMasterData'
import {
  buildSubsidyFromItinerary,
  calcCalendarTotals,
  formatMoney,
  formatPercent,
  genId,
  getToday,
  syncSubsidiesWithItineraries,
} from '@/utils/reimburse'
import { buildBusinessTypeTree, isBusinessTypeLeaf } from '@/utils/businessTypeTree'
import {
  createReimburse,
  fetchReimburseDetail,
  updateReimburse,
  validateReimburseOnServer,
} from '@/api/reimburse'
import { validateReimburseForm } from '@/utils/validateReimburse'

const route = useRoute()
const router = useRouter()
const { companies, departments, reimbursers, businessTypes, projects } = useMasterData()

const collapsed = reactive({
  basic: false,
  itinerary: false,
  subsidy: false,
  total: false,
  allocation: false,
  remark: false,
})

const form = reactive<ReimburseFormData>({
  status: 0,
  submitDate: getToday(),
  title: '',
  reason: '',
  reimburserId: '',
  departmentId: '',
  companyId: '',
  businessTypeId: '',
  itineraries: [],
  subsidies: [],
  allocations: [
    {
      id: genId(),
      costAttributionId: '',
      costAttributionName: '',
      projectId: '',
      projectName: '',
      ratio: 1,
      amount: 0,
    },
  ],
  remark: '',
})

const id = computed(() => route.params.id as string | undefined)

function applyFormData(data: ReimburseFormData) {
  form.status = data.status ?? 0
  form.submitDate = data.submitDate ?? getToday()
  form.title = data.title ?? ''
  form.reason = data.reason ?? ''
  form.reimburserId = data.reimburserId ?? ''
  form.departmentId = data.departmentId ?? ''
  form.companyId = data.companyId ?? ''
  form.businessTypeId = data.businessTypeId ?? ''
  form.reimburseNo = data.reimburseNo
  form.id = data.id
  form.itineraries = data.itineraries ? [...data.itineraries] : []
  form.subsidies = syncSubsidiesWithItineraries(
    form.itineraries,
    data.subsidies ? JSON.parse(JSON.stringify(data.subsidies)) : [],
  )
  form.allocations = data.allocations?.length
    ? JSON.parse(JSON.stringify(data.allocations))
    : [
        {
          id: genId(),
          costAttributionId: '',
          costAttributionName: '',
          projectId: '',
          projectName: '',
          ratio: 1,
          amount: 0,
        },
      ]
  form.remark = data.remark ?? ''
  lastBusinessTypeId.value = form.businessTypeId
  syncAllocationAmounts(expenseTotal.value.total)
}

async function loadFormData(editId?: string) {
  if (!editId) {
    initSubmitDate()
    return
  }
  try {
    const data = await fetchReimburseDetail(editId)
    applyFormData(data)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载单据详情失败')
    router.push('/')
  }
}

watch(id, (val) => {
  void loadFormData(val)
}, { immediate: true })

watch(
  () => form.itineraries.map((it) => it.id).join('\n'),
  () => {
    form.subsidies = syncSubsidiesWithItineraries(form.itineraries, form.subsidies)
    recalcFirstAllocation()
  },
)

const businessTypeName = computed(
  () =>
    businessTypes.find((b) => b.businessTypeId === form.businessTypeId)?.businessTypeName ??
    '',
)

const businessTypeTree = computed(() => buildBusinessTypeTree(businessTypes))

const lastBusinessTypeId = ref(form.businessTypeId)

watch(
  () => form.businessTypeId,
  (id) => {
    if (!id) return
    if (!isBusinessTypeLeaf(id, businessTypes)) {
      form.businessTypeId = lastBusinessTypeId.value
      ElMessage.warning('请选择末级业务类型')
      return
    }
    lastBusinessTypeId.value = id
  },
)

const SUBSIDY_TIP =
  '1、请根据实际出差日期选择补助2、出差期间当日用餐安排的请自行核减当日餐补3、出差期间当日有用车的，请自行核减当日交补'

const subsidySummary = computed(() => {
  const parts = form.subsidies.map((s) => `${s.travelerName}:${s.days}天`)
  const total = form.subsidies.reduce((sum, s) => sum + s.subsidyAmount, 0)
  return { total, parts: parts.join('、') }
})

const expenseTotal = computed(() => ({
  total: form.subsidies.reduce((s, i) => s + i.subsidyAmount, 0),
  meal: form.subsidies.reduce((s, i) => s + i.mealTotal, 0),
  transport: form.subsidies.reduce((s, i) => s + i.transportTotal, 0),
  comm: form.subsidies.reduce((s, i) => s + i.commTotal, 0),
}))

watch(
  () => expenseTotal.value.total,
  (total) => {
    syncAllocationAmounts(total)
  },
)

/** 除不尽时差额放在首行，保证合计等于补助总金额 */
function syncAllocationAmounts(total: number) {
  const rows = form.allocations
  if (rows.length === 0) return
  if (rows.length === 1) {
    rows[0]!.amount = +total.toFixed(2)
    return
  }
  let othersAmount = 0
  for (let i = 1; i < rows.length; i++) {
    const row = rows[i]!
    row.amount = +(total * row.ratio).toFixed(2)
    othersAmount += row.amount
  }
  rows[0]!.amount = +(total - othersAmount).toFixed(2)
}

const itineraryDialogVisible = ref(false)
const itineraryEditData = ref<ItineraryItem | null>(null)
const itineraryExcludeId = ref<string | undefined>()
const itineraryIsCopy = ref(false)

/** 5.2.2.1 新增取当前日期，编辑取已保存数据 */
function initSubmitDate() {
  form.submitDate = getToday()
}

function openItineraryDialog(data?: ItineraryItem, copy = false) {
  itineraryIsCopy.value = copy
  itineraryEditData.value = data ?? null
  itineraryExcludeId.value = copy ? undefined : data?.id
  itineraryDialogVisible.value = true
}

function onItinerarySave(data: Omit<ItineraryItem, 'id'> & { id?: string }) {
  if (data.id && itineraryEditData.value?.id && !itineraryIsCopy.value) {
    const idx = form.itineraries.findIndex((it) => it.id === data.id)
    if (idx >= 0) {
      const updated = { ...data, id: data.id } as ItineraryItem
      form.itineraries[idx] = updated
      const subIdx = form.subsidies.findIndex((s) => s.itineraryId === data.id)
      if (subIdx >= 0) form.subsidies[subIdx] = buildSubsidyFromItinerary(updated)
    }
  } else {
    const newIt: ItineraryItem = { ...data, id: genId() } as ItineraryItem
    form.itineraries.push(newIt)
    form.subsidies.push(buildSubsidyFromItinerary(newIt))
  }
  recalcFirstAllocation()
}

async function deleteItinerary(row: ItineraryItem) {
  try {
    await ElMessageBox.confirm('确认删除?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    form.itineraries = form.itineraries.filter((it) => it.id !== row.id)
    form.subsidies = form.subsidies.filter((s) => s.itineraryId !== row.id)
    recalcFirstAllocation()
  } catch {
    /* cancelled */
  }
}

function copyItinerary(row: ItineraryItem) {
  const { id: _id, ...rest } = row
  openItineraryDialog({ ...rest, id: '' } as ItineraryItem, true)
}

const subsidyDialogVisible = ref(false)
const currentSubsidy = ref<SubsidyInfoItem | null>(null)

function openSubsidyCalendar(row: SubsidyInfoItem) {
  currentSubsidy.value = row
  subsidyDialogVisible.value = true
}

/** 5.2.2.4 确认后：申请金额=勾选标准合计，补助金额=勾选金额合计 */
function onSubsidyConfirm(calendar: SubsidyInfoItem['calendar']) {
  if (!currentSubsidy.value) return
  const idx = form.subsidies.findIndex((s) => s.id === currentSubsidy.value!.id)
  if (idx < 0) return
  const totals = calcCalendarTotals(calendar)
  const row = form.subsidies[idx]!
  form.subsidies[idx] = {
    ...row,
    calendar,
    applyAmount: totals.standardTotal,
    subsidyAmount: totals.subsidyAmount,
    mealTotal: totals.mealTotal,
    transportTotal: totals.transportTotal,
    commTotal: totals.commTotal,
  }
  recalcFirstAllocation()
}

function addAllocationRow() {
  form.allocations.push({
    id: genId(),
    costAttributionId: '',
    costAttributionName: '',
    projectId: '',
    projectName: '',
    ratio: 0,
    amount: 0,
  })
  recalcFirstAllocation()
}

const clearedRatioRows = ref(new Set<string>())

function getRatioInputValue(index: number): number | undefined {
  const row = form.allocations[index]
  if (!row || clearedRatioRows.value.has(row.id)) return undefined
  return +(row.ratio * 100).toFixed(2)
}

async function deleteAllocationRow(row: AllocationItem, index: number) {
  if (form.allocations.length === 1) {
    ElMessage.warning('至少保留一条分摊信息')
    return
  }
  try {
    await ElMessageBox.confirm('确定删除?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    form.allocations.splice(index, 1)
    clearedRatioRows.value.clear()
    recalcFirstAllocation()
  } catch {
    /* cancelled */
  }
}

function recalcFirstAllocation() {
  const total = expenseTotal.value.total
  const first = form.allocations[0]
  if (!first) return
  if (form.allocations.length === 1) {
    first.ratio = 1
    first.amount = total
    return
  }
  const othersSum = form.allocations.slice(1).reduce((s, r) => s + r.ratio, 0)
  if (othersSum > 1) return
  const firstRatio = +(1 - othersSum).toFixed(4)
  first.ratio = Math.min(1, Math.max(0, firstRatio))
  syncAllocationAmounts(total)
}

function onRatioChange(index: number, val: number | null | undefined) {
  if (index === 0) return
  const row = form.allocations[index]
  if (!row) return
  clearedRatioRows.value.delete(row.id)

  if (val === null || val === undefined) {
    row.ratio = 0
    recalcFirstAllocation()
    return
  }

  const ratio = val / 100
  if (ratio < 0 || ratio > 1) {
    clearedRatioRows.value.add(row.id)
    return
  }

  const othersSum = form.allocations.slice(1).reduce((s, r, i) => {
    const rowIndex = i + 1
    return s + (rowIndex === index ? ratio : r.ratio)
  }, 0)

  if (othersSum > 1) {
    clearedRatioRows.value.add(row.id)
    ElMessage.warning('分摊比例合计不能超过100%')
    recalcFirstAllocation()
    return
  }

  row.ratio = ratio
  recalcFirstAllocation()
}

/** 均摊：除不尽时差值放在首行 */
function equalSplit() {
  const n = form.allocations.length
  if (n === 0) return
  if (n === 1) {
    const only = form.allocations[0]
    if (only) only.ratio = 1
    syncAllocationAmounts(expenseTotal.value.total)
    return
  }
  clearedRatioRows.value.clear()
  const base = Math.floor((10000 / n)) / 100
  const remainder = 100 - base * n
  form.allocations.forEach((row, i) => {
    row.ratio = (base + (i === 0 ? remainder : 0)) / 100
  })
  syncAllocationAmounts(expenseTotal.value.total)
}

function onCostAttributionChange(row: AllocationItem, id: string | undefined) {
  if (!id) {
    row.costAttributionId = ''
    row.costAttributionName = ''
    return
  }
  const c = companies.find((x) => x.reimCompanyId === id)
  row.costAttributionId = id
  row.costAttributionName = c?.reimCompanyName ?? ''
}

function onProjectChange(row: AllocationItem, id: string | undefined) {
  if (!id) {
    row.projectId = ''
    row.projectName = ''
    return
  }
  const p = projects.find((x) => x.projectId === id)
  row.projectId = id
  row.projectName = p?.projectName ?? ''
}

/** 5.2.2.7 删除备注：确认后清空 */
async function deleteRemark() {
  if (!form.remark.trim()) {
    form.remark = ''
    return
  }
  try {
    await ElMessageBox.confirm('确认删除?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    form.remark = ''
  } catch {
    /* cancelled */
  }
}

/** 5.2.2.8 关闭：确认后返回列表 */
async function handleClose() {
  try {
    await ElMessageBox.confirm('确认关闭当前页面?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    router.push('/')
  } catch {
    /* cancelled */
  }
}

const submitting = ref(false)

/** 5.2.2.8 / 5.2.2.9 提交：前端 + 后台双重校验 */
async function handleSubmit() {
  if (submitting.value) return

  const local = validateReimburseForm(form, expenseTotal.value.total, businessTypes)
  if (!local.valid) {
    ElMessage.warning(local.message)
    return
  }

  submitting.value = true
  try {
    try {
      const remote = await validateReimburseOnServer(form, expenseTotal.value.total)
      if (!remote.valid) {
        ElMessage.warning(remote.message || '提交校验未通过')
        return
      }
    } catch {
      /* 后台校验不可用：已通过前端校验则继续 */
    }

    try {
      if (id.value) {
        await updateReimburse(id.value, form)
      } else {
        await createReimburse(form)
      }
    } catch (e) {
      ElMessage.error(e instanceof Error ? e.message : '保存失败，请确认后端已启动')
      return
    }

    await ElMessageBox.confirm('提交成功', '提示', {
      confirmButtonText: '确定',
      showCancelButton: false,
      type: 'success',
    })
    router.push('/')
  } catch {
    /* cancelled */
  } finally {
    submitting.value = false
  }
}

const allocationTotalRatio = computed(() =>
  form.allocations.reduce((s, r) => s + r.ratio, 0),
)
const allocationTotalAmount = computed(() =>
  form.allocations.reduce((s, r) => s + r.amount, 0),
)
</script>

<template>
  <div class="reim-form-page">
    <!-- 5.2.2.1 固定表头 -->
    <div class="reim-form-header-fixed">
      <div class="reim-form-content">
        <h1 class="reim-form-title">差旅费用报销单</h1>
        <div class="reim-form-date">提单日期 {{ form.submitDate }}</div>
      </div>
    </div>

    <div class="reim-form-scroll">
      <div class="reim-form-content">
        <!-- 5.2.2.2 基础信息 -->
        <SectionPanel
          :collapsed="collapsed.basic"
          title="基础信息"
          @update:collapsed="collapsed.basic = $event"
        >
          <el-form label-width="110px" label-position="right">
            <el-form-item label="报销标题" required>
              <el-input v-model="form.title" placeholder="请输入" maxlength="500" show-word-limit />
            </el-form-item>
            <el-row :gutter="24">
              <el-col :span="8">
                <el-form-item label="报销人" required>
                  <el-select v-model="form.reimburserId" placeholder="请选择" style="width: 100%">
                    <el-option
                      v-for="r in reimbursers"
                      :key="r.reimburserId"
                      :label="r.reimburserName"
                      :value="r.reimburserId"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="报销部门" required>
                  <el-select v-model="form.departmentId" placeholder="请选择" style="width: 100%">
                    <el-option
                      v-for="d in departments"
                      :key="d.reimDepartmentId"
                      :label="d.reimDepartmentName"
                      :value="d.reimDepartmentId"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="费用归属公司" required>
                  <el-select v-model="form.companyId" placeholder="请选择" style="width: 100%">
                    <el-option
                      v-for="c in companies"
                      :key="c.reimCompanyId"
                      :label="c.reimCompanyName"
                      :value="c.reimCompanyId"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="24">
              <el-col :span="8">
                <el-form-item label="业务类型" required>
                  <el-tree-select
                    v-model="form.businessTypeId"
                    :data="businessTypeTree"
                    check-strictly
                    default-expand-all
                    expand-on-click-node
                    :render-after-expand="false"
                    placeholder="请选择"
                    class="business-type-select"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="出差事由" required>
              <el-input
                v-model="form.reason"
                type="textarea"
                :rows="3"
                placeholder="请输入"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
          </el-form>
        </SectionPanel>

        <!-- 补录行程 -->
        <SectionPanel
          :collapsed="collapsed.itinerary"
          @update:collapsed="collapsed.itinerary = $event"
        >
          <template #title>补录行程</template>
          <template #actions>
            <span class="reim-section-action-btn" @click.stop="openItineraryDialog(undefined, false)">
              <el-icon><CirclePlus /></el-icon> 补录行程
            </span>
          </template>
          <el-table :data="form.itineraries" border style="width: 100%">
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
                  <el-icon class="reim-icon-btn" @click="deleteItinerary(row)"><Delete /></el-icon>
                  <el-icon class="reim-icon-btn" @click="openItineraryDialog(row, false)"><Edit /></el-icon>
                  <el-icon class="reim-icon-btn" @click="copyItinerary(row)"><CopyDocument /></el-icon>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </SectionPanel>

        <!-- 5.2.2.4 补助信息（随补录行程生成，无行程不展示） -->
        <SectionPanel
          v-if="form.itineraries.length"
          :collapsed="collapsed.subsidy"
          @update:collapsed="collapsed.subsidy = $event"
        >
          <template #title>
            <span class="section-title-main">补助信息</span>
            <span v-if="form.subsidies.length" class="reim-subsidy-header-extra">
              {{ formatMoney(subsidySummary.total) }}
              ({{ subsidySummary.parts }})
            </span>
          </template>
          <el-tooltip :content="SUBSIDY_TIP" placement="top" :show-after="300">
            <div class="subsidy-tip-bar">
              <el-icon class="subsidy-tip-icon"><WarningFilled /></el-icon>
              <span class="subsidy-tip-text">{{ SUBSIDY_TIP }}</span>
            </div>
          </el-tooltip>
          <el-table :data="form.subsidies" border class="subsidy-info-table" style="width: 100%">
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
                <el-icon class="subsidy-edit-icon" @click="openSubsidyCalendar(row)"><Edit /></el-icon>
              </template>
            </el-table-column>
          </el-table>
        </SectionPanel>

        <!-- 费用合计 -->
        <SectionPanel
          :collapsed="collapsed.total"
          title="费用合计"
          @update:collapsed="collapsed.total = $event"
        >
          <el-row class="expense-total-row">
            <el-col :span="6">
              <span class="total-label">补助总金额：</span>
              <span>{{ formatMoney(expenseTotal.total) }}</span>
            </el-col>
            <el-col :span="6">
              <span class="total-label">餐费补助：</span>
              <span>{{ formatMoney(expenseTotal.meal) }}</span>
            </el-col>
            <el-col :span="6">
              <span class="total-label">交通补助：</span>
              <span>{{ formatMoney(expenseTotal.transport) }}</span>
            </el-col>
            <el-col :span="6">
              <span class="total-label">通讯补助：</span>
              <span>{{ formatMoney(expenseTotal.comm) }}</span>
            </el-col>
          </el-row>
        </SectionPanel>

        <!-- 费用归属及分摊 -->
        <SectionPanel
          :collapsed="collapsed.allocation"
          @update:collapsed="collapsed.allocation = $event"
        >
          <template #title>
            <span class="section-title-main">费用归属及分摊</span>
            <span class="reim-subsidy-header-extra">(分摊金额: {{ formatMoney(expenseTotal.total) }})</span>
          </template>
          <el-table :data="form.allocations" border class="allocation-table" style="width: 100%">
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column label="费用归属" min-width="180">
              <template #header><span class="allocation-required">*</span> 费用归属</template>
              <template #default="{ row }">
                <el-select
                  :model-value="row.costAttributionId"
                  placeholder="请选择"
                  clearable
                  style="width: 100%"
                  @update:model-value="onCostAttributionChange(row, $event)"
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
                  @update:model-value="onProjectChange(row, $event)"
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
                <div class="allocation-ratio-header">
                  <el-button
                    type="primary"
                    link
                    class="allocation-equal-btn"
                    @click.stop="equalSplit"
                  >
                    均摊
                  </el-button>
                  <div class="allocation-ratio-header-row">
                    <span><span class="allocation-required">*</span> 分摊比例</span>
                    <el-icon class="allocation-refresh-icon" title="均摊" @click.stop="equalSplit">
                      <Refresh />
                    </el-icon>
                  </div>
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
                  @update:model-value="onRatioChange($index, $event)"
                />
              </template>
            </el-table-column>
            <el-table-column label="分摊金额" width="120" align="right" header-align="right">
              <template #header><span class="allocation-required">*</span> 分摊金额</template>
              <template #default="{ row, $index }">
                <div
                  :class="$index === 0 ? 'allocation-readonly' : 'allocation-amount-cell'"
                >
                  {{ formatMoney(row.amount) }}
                </div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="{ row, $index }">
                <el-icon class="reim-icon-btn" @click="deleteAllocationRow(row, $index)"><Delete /></el-icon>
              </template>
            </el-table-column>
          </el-table>
          <div class="allocation-table-extra">
            <div class="allocation-add-row" @click="addAllocationRow">
              <el-icon class="allocation-add-icon"><CirclePlus /></el-icon>
              <span>添加一行</span>
            </div>
            <div class="allocation-summary-row">
              <span class="allocation-summary-label">合计</span>
              <span class="allocation-summary-ratio">{{ formatPercent(allocationTotalRatio) }}</span>
              <span class="allocation-summary-amount">CNY {{ formatMoney(allocationTotalAmount) }}</span>
            </div>
          </div>
        </SectionPanel>

        <!-- 备注信息 -->
        <SectionPanel
          :collapsed="collapsed.remark"
          @update:collapsed="collapsed.remark = $event"
        >
          <template #title>备注信息</template>
          <template #actions>
            <span class="reim-section-action-btn" @click.stop="deleteRemark">
              <el-icon><Delete /></el-icon> 删除备注
            </span>
          </template>
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="4"
            placeholder="请输入"
            maxlength="1000"
            show-word-limit
          />
        </SectionPanel>
      </div>
    </div>

    <div class="reim-form-footer">
      <div class="reim-form-footer-inner">
        <el-button @click="handleClose">关闭</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交</el-button>
      </div>
    </div>

    <ItineraryDialog
      v-model:visible="itineraryDialogVisible"
      :edit-data="itineraryEditData"
      :existing-list="form.itineraries"
      :exclude-id="itineraryExcludeId"
      :is-copy="itineraryIsCopy"
      @save="onItinerarySave"
    />

    <SubsidyCalendarDialog
      v-model:visible="subsidyDialogVisible"
      :subsidy="currentSubsidy"
      :business-type-name="businessTypeName"
      @confirm="onSubsidyConfirm"
    />
  </div>
</template>

<style scoped>
.section-title-main {
  font-size: 16px;
  font-weight: 500;
}

.business-type-select {
  width: 100%;
}

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
.allocation-required {
  color: #f56c6c;
}

.allocation-table :deep(.el-table__header th) {
  background: #fafafa !important;
  vertical-align: middle;
}

.allocation-ratio-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  width: 100%;
}

.allocation-equal-btn {
  font-size: 14px;
  padding: 0;
  height: auto;
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
/* 5.2.2.4 补助提示条 */
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
