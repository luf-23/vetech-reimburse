/**
 * 报销单表单核心 Composable（表单的「大脑」）
 *
 * 集中管理报销单新建/编辑页的全部状态与业务逻辑：
 * - 表单数据（基本信息、行程、补助、分摊、备注）
 * - 各区块折叠状态、弹窗状态
 * - 行程 → 补助 → 费用合计 → 分摊金额 的联动计算链
 * - 数据加载、校验、提交、关闭等操作
 *
 * 视图层（ReimburseForm.vue）仅负责布局与事件绑定，业务逻辑均在此处理。
 */

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

  /** 各表单区块的折叠/展开状态 */
  const collapsed = reactive({
    basic: false,
    itinerary: false,
    subsidy: false,
    total: false,
    allocation: false,
    remark: false,
  })

  /** 报销单主表单数据 */
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

  /** 上一次合法的业务类型 ID，用于拦截非末级选择时回滚 */
  const lastBusinessTypeId = ref(form.businessTypeId)
  /** 分摊比例输入框被清空或校验失败时，记录对应行 ID，控制显示为空而非 0 */
  const clearedRatioRows = ref(new Set<string>())
  /** 提交中防重复点击 */
  const submitting = ref(false)

  /** 行程编辑弹窗相关状态 */
  const itineraryDialogVisible = ref(false)
  const itineraryEditData = ref<ItineraryItem | null>(null)
  const itineraryExcludeId = ref<string | undefined>()
  const itineraryIsCopy = ref(false)

  /** 补助日历弹窗相关状态 */
  const subsidyDialogVisible = ref(false)
  const currentSubsidy = ref<SubsidyInfoItem | null>(null)

  /** 业务类型树形数据，供树形选择器使用 */
  const businessTypeTree = computed(() => buildBusinessTypeTree(businessTypes))
  /** 当前选中业务类型的显示名称 */
  const businessTypeName = computed(
    () =>
      businessTypes.find((b) => b.businessTypeId === form.businessTypeId)?.businessTypeName ?? '',
  )

  /** 补助汇总：各出差人天数文案 + 补助总额 */
  const subsidySummary = computed(() => {
    const parts = form.subsidies.map((s) => `${s.travelerName}:${s.days}天`)
    const total = form.subsidies.reduce((sum, s) => sum + s.subsidyAmount, 0)
    return { total, parts: parts.join('、') }
  })

  /** 费用合计：补助总额及伙食、交通、通讯分项合计 */
  const expenseTotal = computed(() => ({
    total: form.subsidies.reduce((s, i) => s + i.subsidyAmount, 0),
    meal: form.subsidies.reduce((s, i) => s + i.mealTotal, 0),
    transport: form.subsidies.reduce((s, i) => s + i.transportTotal, 0),
    comm: form.subsidies.reduce((s, i) => s + i.commTotal, 0),
  }))

  /** 分摊比例合计（0~1） */
  const allocationTotalRatio = computed(() => form.allocations.reduce((s, r) => s + r.ratio, 0))
  /** 分摊金额合计 */
  const allocationTotalAmount = computed(() => form.allocations.reduce((s, r) => s + r.amount, 0))

  /** 按费用总额与各行比例，重新分配分摊金额 */
  function syncAllocationAmounts(total: number) {
    distributeAllocationAmounts(total, form.allocations)
  }

  /** 将接口/详情数据写入表单，并同步补助与分摊 */
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

  /** 新建单据时初始化提交日期为今天 */
  function initSubmitDate() {
    form.submitDate = getToday()
  }

  /** 根据 editId 加载详情；无 id 时为新建模式，仅初始化日期 */
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

  /** 路由参数变化时重新加载（新建 ↔ 编辑切换） */
  watch(editId, (val) => {
    void loadFormData(val)
  }, { immediate: true })

  /**
   * 联动链 ①：行程变化 → 同步补助行 → 重算首行分摊
   * 监听行程 ID 列表变化（增删改行程），自动对齐补助数据并更新分摊
   */
  watch(
    () => form.itineraries.map((it) => it.id).join('\n'),
    () => {
      form.subsidies = syncSubsidiesWithItineraries(form.itineraries, form.subsidies)
      recalcFirstAllocation()
    },
  )

  /**
   * 联动链 ②（校验）：业务类型必须选末级节点
   * 非末级时回滚到上次合法值并提示
   */
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

  /**
   * 联动链 ③：费用总额变化 → 按比例重新分配各行分摊金额
   * 补助金额变动会触发 expenseTotal，进而同步 allocation 的 amount
   */
  watch(
    () => expenseTotal.value.total,
    (total) => {
      syncAllocationAmounts(total)
    },
  )

  /** 打开行程编辑弹窗；copy 为 true 时表示复制新增 */
  function openItineraryDialog(data?: ItineraryItem, copy = false) {
    itineraryIsCopy.value = copy
    itineraryEditData.value = data ?? null
    itineraryExcludeId.value = copy ? undefined : data?.id
    itineraryDialogVisible.value = true
  }

  /** 行程弹窗保存：编辑则更新对应行程与补助，新增则追加并生成补助行 */
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

  /** 删除行程及关联补助行，确认后重算分摊 */
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

    }
  }

  /** 复制行程：去掉原 id，以复制模式打开弹窗 */
  function copyItinerary(row: ItineraryItem) {
    const { id: _id, ...rest } = row
    openItineraryDialog({ ...rest, id: '' } as ItineraryItem, true)
  }

  /** 打开补助日历弹窗，编辑指定补助行的日历明细 */
  function openSubsidyCalendar(row: SubsidyInfoItem) {
    currentSubsidy.value = row
    subsidyDialogVisible.value = true
  }

  /** 补助日历确认：根据日历重算各项金额并更新补助行，再重算分摊 */
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

  /** 新增一条空分摊行，并重算首行比例 */
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

  /** 分摊比例输入框显示值（百分比）；清空态返回 undefined */
  function getRatioInputValue(index: number): number | undefined {
    const row = form.allocations[index]
    if (!row || clearedRatioRows.value.has(row.id)) return undefined
    return +(row.ratio * 100).toFixed(2)
  }

  /** 删除分摊行（至少保留一行），确认后清空比例清空标记并重算 */
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

    }
  }

  /**
   * 重算首行分摊比例与金额
   * 仅一行时首行 100%；多行时首行 = 1 - 其余行比例之和，再按总额分配金额
   */
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

  /**
   * 非首行分摊比例变更（首行比例由 recalcFirstAllocation 自动计算）
   * 校验范围与合计不超过 100%，非法时标记清空态
   */
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

  /** 均分分摊比例（多行时尽量整数百分比，余数给首行） */
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

  /** 费用归属公司变更：同步 ID 与名称 */
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

  /** 项目变更：同步 ID 与名称 */
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

  /** 清空备注（有内容时需确认） */
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

    }
  }

  /** 关闭页面：确认后返回列表 */
  async function handleClose() {
    try {
      await ElMessageBox.confirm('确认关闭当前页面?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
      router.push('/')
    } catch {

    }
  }

  /** 提交：本地校验 → 服务端校验 → 创建/更新 → 成功后返回列表 */
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
