import { computed, reactive, ref, watch, type Ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { AllocationItem, ItineraryItem, ReimburseFormData, SubsidyInfoItem } from '@/types/reimburse'
import { useMasterData } from '@/composables/useMasterData'
import {
  createReimburse,
  fetchReimburseDetail,
  updateReimburse,
  validateReimburseOnServer,
} from '@/api/reimburse'
import { buildBusinessTypeTree, isBusinessTypeLeaf } from '@/utils/businessTypeTree'
import { distributeAllocationAmounts } from '@/utils/allocation'
import { getToday } from '@/utils/date'
import { genId } from '@/utils/id'
import {
  buildSubsidyFromItinerary,
  calcCalendarTotals,
  syncSubsidiesWithItineraries,
} from '@/utils/subsidy'
import { validateReimburseForm } from '@/utils/validateReimburse'

export function useReimburseForm(editId: Ref<string | undefined>) {
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

  const lastBusinessTypeId = ref(form.businessTypeId)
  const clearedRatioRows = ref(new Set<string>())
  const submitting = ref(false)

  const itineraryDialogVisible = ref(false)
  const itineraryEditData = ref<ItineraryItem | null>(null)
  const itineraryExcludeId = ref<string | undefined>()
  const itineraryIsCopy = ref(false)

  const subsidyDialogVisible = ref(false)
  const currentSubsidy = ref<SubsidyInfoItem | null>(null)

  const businessTypeTree = computed(() => buildBusinessTypeTree(businessTypes))
  const businessTypeName = computed(
    () =>
      businessTypes.find((b) => b.businessTypeId === form.businessTypeId)?.businessTypeName ?? '',
  )

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

  const allocationTotalRatio = computed(() => form.allocations.reduce((s, r) => s + r.ratio, 0))
  const allocationTotalAmount = computed(() => form.allocations.reduce((s, r) => s + r.amount, 0))

  function syncAllocationAmounts(total: number) {
    distributeAllocationAmounts(total, form.allocations)
  }

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

  function initSubmitDate() {
    form.submitDate = getToday()
  }

  async function loadFormData(id?: string) {
    if (!id) {
      initSubmitDate()
      return
    }
    try {
      const data = await fetchReimburseDetail(id)
      applyFormData(data)
    } catch (e) {
      ElMessage.error(e instanceof Error ? e.message : '加载单据详情失败')
      router.push('/')
    }
  }

  watch(editId, (val) => {
    void loadFormData(val)
  }, { immediate: true })

  watch(
    () => form.itineraries.map((it) => it.id).join('\n'),
    () => {
      form.subsidies = syncSubsidiesWithItineraries(form.itineraries, form.subsidies)
      recalcFirstAllocation()
    },
  )

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

  watch(
    () => expenseTotal.value.total,
    (total) => {
      syncAllocationAmounts(total)
    },
  )

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

  function openSubsidyCalendar(row: SubsidyInfoItem) {
    currentSubsidy.value = row
    subsidyDialogVisible.value = true
  }

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
        if (editId.value) {
          await updateReimburse(editId.value, form)
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

  return {
    form,
    collapsed,
    companies,
    departments,
    reimbursers,
    businessTypes,
    projects,
    businessTypeTree,
    businessTypeName,
    subsidySummary,
    expenseTotal,
    allocationTotalRatio,
    allocationTotalAmount,
    submitting,
    itineraryDialogVisible,
    itineraryEditData,
    itineraryExcludeId,
    itineraryIsCopy,
    subsidyDialogVisible,
    currentSubsidy,
    openItineraryDialog,
    onItinerarySave,
    deleteItinerary,
    copyItinerary,
    openSubsidyCalendar,
    onSubsidyConfirm,
    addAllocationRow,
    getRatioInputValue,
    deleteAllocationRow,
    onRatioChange,
    equalSplit,
    onCostAttributionChange,
    onProjectChange,
    deleteRemark,
    handleClose,
    handleSubmit,
  }
}
