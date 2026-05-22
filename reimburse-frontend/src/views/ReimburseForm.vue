<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CirclePlus,
  CopyDocument,
  Delete,
  Edit,
  InfoFilled,
  WarningFilled,
} from '@element-plus/icons-vue'
import SectionPanel from '@/components/reimburse/SectionPanel.vue'
import ItineraryDialog from '@/components/reimburse/ItineraryDialog.vue'
import SubsidyCalendarDialog from '@/components/reimburse/SubsidyCalendarDialog.vue'
import type { AllocationItem, ItineraryItem, ReimburseFormData, SubsidyInfoItem } from '@/types/reimburse'
import {
  BUSINESS_TYPES,
  MOCK_LIST_DATA,
  PROJECTS,
  REIM_COMPANIES,
  REIM_DEPARTMENTS,
  REIMBURSERS,
} from '@/data/mockData'
import {
  buildSubsidyFromItinerary,
  calcCalendarTotals,
  formatMoney,
  formatPercent,
  genId,
  getToday,
} from '@/utils/reimburse'
import { buildBusinessTypeTree } from '@/utils/businessTypeTree'

const route = useRoute()
const router = useRouter()

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
  reimburserId: '13AB3A3F72409002',
  departmentId: '14515BB4BFB92003',
  companyId: '1C61686865DA8000',
  businessTypeId: '1B5FEB7DD4396000',
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

function loadFormData(editId?: string) {
  if (!editId) {
    initSubmitDate()
    return
  }
  const item = MOCK_LIST_DATA.find((r) => r.id === editId)
  if (!item) {
    initSubmitDate()
    return
  }
  form.title = item.title
  form.reason = item.reason
  form.reimburserId = item.reimburserId
  form.departmentId = item.departmentId
  form.companyId = item.companyId
  form.businessTypeId = item.businessTypeId
  form.reimburseNo = item.reimburseNo
  initSubmitDate(editId)
  if (editId === '1') {
    form.itineraries = [
      {
        id: 'it-1',
        travelerId: '13AB3A3F72409002',
        travelerName: '徐年年',
        travelerNo: '74541',
        departCityNo: '10458',
        departCityName: '武汉',
        arriveCityNo: '10119',
        arriveCityName: '北京',
        startDate: '2026-04-13',
        endDate: '2026-04-17',
        description: '行程说明',
      },
    ]
    form.subsidies = form.itineraries.map(buildSubsidyFromItinerary)
    syncAllocationAmounts(expenseTotal.value.total)
  }
}

watch(id, (val) => loadFormData(val), { immediate: true })

const businessTypeName = computed(
  () => BUSINESS_TYPES.find((b) => b.businessTypeId === form.businessTypeId)?.businessTypeName ?? '',
)

const businessTypeTree = buildBusinessTypeTree()

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

function syncAllocationAmounts(total: number) {
  form.allocations.forEach((row, idx) => {
    if (idx === 0) {
      row.amount = +(total * row.ratio).toFixed(2)
    } else {
      row.amount = +(total * row.ratio).toFixed(2)
    }
  })
}

const itineraryDialogVisible = ref(false)
const itineraryEditData = ref<ItineraryItem | null>(null)
const itineraryExcludeId = ref<string | undefined>()
const itineraryIsCopy = ref(false)

/** 5.2.2.1 新增取当前日期，编辑取已保存数据 */
function initSubmitDate(editId?: string) {
  if (editId) {
    const item = MOCK_LIST_DATA.find((r) => r.id === editId)
    form.submitDate = item?.createTime?.slice(0, 10) ?? getToday()
  } else {
    form.submitDate = getToday()
  }
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
  form.subsidies[idx] = {
    ...form.subsidies[idx],
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

async function deleteAllocationRow(row: AllocationItem, index: number) {
  if (form.allocations.length === 1) {
    ElMessage.warning('至少保留一条分摊信息')
    return
  }
  try {
    await ElMessageBox.confirm('确认删除?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    form.allocations.splice(index, 1)
    recalcFirstAllocation()
  } catch {
    /* cancelled */
  }
}

function recalcFirstAllocation() {
  const total = expenseTotal.value.total
  if (form.allocations.length === 1) {
    form.allocations[0].ratio = 1
    form.allocations[0].amount = total
    return
  }
  const othersSum = form.allocations.slice(1).reduce((s, r) => s + r.ratio, 0)
  if (othersSum > 1) return
  form.allocations[0].ratio = +(1 - othersSum).toFixed(4)
  syncAllocationAmounts(total)
}

function onRatioChange(index: number, val: number | null) {
  if (index === 0) return
  const ratio = (val ?? 0) / 100
  if (ratio < 0 || ratio > 1) {
    form.allocations[index].ratio = 0
    return
  }
  const othersSum = form.allocations.slice(1).reduce((s, r, i) => s + (i + 1 === index ? ratio : r.ratio), 0)
  if (othersSum > 1) {
    form.allocations[index].ratio = 0
    ElMessage.warning('分摊比例合计不能超过100%')
    return
  }
  form.allocations[index].ratio = ratio
  recalcFirstAllocation()
}

function equalSplit() {
  const n = form.allocations.length
  if (n === 0) return
  const base = Math.floor((10000 / n)) / 100
  let remainder = 100 - base * n
  form.allocations.forEach((row, i) => {
    row.ratio = (base + (i === 0 ? remainder : 0)) / 100
  })
  recalcFirstAllocation()
}

function onCostAttributionChange(row: AllocationItem, id: string) {
  const c = REIM_COMPANIES.find((x) => x.reimCompanyId === id)
  row.costAttributionId = id
  row.costAttributionName = c?.reimCompanyName ?? ''
}

function onProjectChange(row: AllocationItem, id: string) {
  const p = PROJECTS.find((x) => x.projectId === id)
  row.projectId = id
  row.projectName = p?.projectName ?? ''
}

async function deleteRemark() {
  try {
    await ElMessageBox.confirm('确认删除备注?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    form.remark = ''
  } catch {
    /* cancelled */
  }
}

async function handleClose() {
  try {
    await ElMessageBox.confirm('确认关闭当前页面?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    router.push('/')
  } catch {
    /* cancelled */
  }
}

async function handleSubmit() {
  if (!form.title.trim()) {
    ElMessage.warning('请输入报销标题')
    return
  }
  if (!form.reimburserId) {
    ElMessage.warning('请选择报销人')
    return
  }
  if (!form.departmentId) {
    ElMessage.warning('请选择报销部门')
    return
  }
  if (!form.companyId) {
    ElMessage.warning('请选择费用归属公司')
    return
  }
  if (!form.businessTypeId) {
    ElMessage.warning('请选择业务类型')
    return
  }
  if (form.itineraries.length === 0) {
    ElMessage.warning('请补录行程')
    return
  }

  const ratioSum = form.allocations.reduce((s, r) => s + r.ratio, 0)
  if (Math.abs(ratioSum - 1) > 0.001) {
    ElMessage.warning('分摊比例合计必须为100%')
    return
  }

  const amountSum = form.allocations.reduce((s, r) => s + r.amount, 0)
  if (Math.abs(amountSum - expenseTotal.value.total) > 0.01) {
    ElMessage.warning('分摊金额合计必须等于补助总金额')
    return
  }

  try {
    await ElMessageBox.confirm('提交成功', '提示', {
      confirmButtonText: '确定',
      showCancelButton: false,
    })
    router.push('/')
  } catch {
    /* cancelled */
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
        <SectionPanel title="基础信息" :collapsed="collapsed.basic" @toggle="collapsed.basic = !collapsed.basic">
          <el-form label-width="110px" label-position="right">
            <el-form-item label="报销标题" required>
              <el-input v-model="form.title" placeholder="请输入" maxlength="500" show-word-limit />
            </el-form-item>
            <el-row :gutter="24">
              <el-col :span="8">
                <el-form-item label="报销人" required>
                  <el-select v-model="form.reimburserId" style="width: 100%">
                    <el-option
                      v-for="r in REIMBURSERS"
                      :key="r.reimburserId"
                      :label="r.reimburserName"
                      :value="r.reimburserId"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="报销部门" required>
                  <el-select v-model="form.departmentId" style="width: 100%">
                    <el-option
                      v-for="d in REIM_DEPARTMENTS"
                      :key="d.reimDepartmentId"
                      :label="d.reimDepartmentName"
                      :value="d.reimDepartmentId"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="费用归属公司" required>
                  <el-select v-model="form.companyId" style="width: 100%">
                    <el-option
                      v-for="c in REIM_COMPANIES"
                      :key="c.reimCompanyId"
                      :label="c.reimCompanyName"
                      :value="c.reimCompanyId"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="业务类型" required>
              <el-tree-select
                v-model="form.businessTypeId"
                :data="businessTypeTree"
                check-strictly
                :render-after-expand="false"
                placeholder="请选择"
                style="width: calc(100% - 28px)"
              />
              <el-icon style="margin-left: 8px; color: #999; vertical-align: middle"><InfoFilled /></el-icon>
            </el-form-item>
            <el-form-item label="出差事由">
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
          @toggle="collapsed.itinerary = !collapsed.itinerary"
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

        <!-- 5.2.2.4 补助信息 -->
        <SectionPanel
          :collapsed="collapsed.subsidy"
          @toggle="collapsed.subsidy = !collapsed.subsidy"
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
          <el-table :data="form.subsidies" class="subsidy-info-table" style="width: 100%">
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
        <SectionPanel title="费用合计" :collapsed="collapsed.total" @toggle="collapsed.total = !collapsed.total">
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
          @toggle="collapsed.allocation = !collapsed.allocation"
        >
          <template #title>
            <span class="section-title-main">费用归属及分摊</span>
            <span class="reim-subsidy-header-extra">(分摊金额: {{ formatMoney(expenseTotal.total) }})</span>
          </template>
          <template #actions>
            <span class="reim-section-action-btn" @click.stop="equalSplit">均摊</span>
          </template>
          <el-table :data="form.allocations" border style="width: 100%">
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column label="费用归属" min-width="180">
              <template #header><span style="color: #f56c6c">*</span> 费用归属</template>
              <template #default="{ row, $index }">
                <el-select
                  :model-value="row.costAttributionId"
                  placeholder="请选择"
                  :disabled="$index === 0 && form.allocations.length > 1"
                  style="width: 100%"
                  @update:model-value="onCostAttributionChange(row, $event)"
                >
                  <el-option
                    v-for="c in REIM_COMPANIES"
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
                  style="width: 100%"
                  @update:model-value="onProjectChange(row, $event)"
                >
                  <el-option
                    v-for="p in PROJECTS"
                    :key="p.projectId"
                    :label="p.projectName"
                    :value="p.projectId"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="分摊比例" width="140" align="right">
              <template #header><span style="color: #f56c6c">*</span> 分摊比例</template>
              <template #default="{ row, $index }">
                <el-input-number
                  v-if="$index > 0"
                  :model-value="row.ratio * 100"
                  :min="0"
                  :max="100"
                  :precision="2"
                  controls-position="right"
                  style="width: 100%"
                  @update:model-value="onRatioChange($index, $event)"
                />
                <span v-else>{{ formatPercent(row.ratio) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="分摊金额" width="120" align="right">
              <template #header><span style="color: #f56c6c">*</span> 分摊金额</template>
              <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
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
          @toggle="collapsed.remark = !collapsed.remark"
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
        <el-button type="primary" @click="handleSubmit">提交</el-button>
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
