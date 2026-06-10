<script setup lang="ts">
/**
 * 报销单表单页（新建 / 编辑）
 *
 * 纯视图层：组装各表单区块子组件与弹窗，状态与业务逻辑委托给 useReimburseForm。
 * 路由参数 id 存在时为编辑模式，否则为新建。
 */

import { computed } from 'vue'
import { useRoute } from 'vue-router'
import ItineraryDialog from '@/components/reimburse/ItineraryDialog.vue'
import SubsidyCalendarDialog from '@/components/reimburse/SubsidyCalendarDialog.vue'
import AllocationSection from '@/components/reimburse/form/AllocationSection.vue'
import BasicInfoSection from '@/components/reimburse/form/BasicInfoSection.vue'
import ExpenseTotalSection from '@/components/reimburse/form/ExpenseTotalSection.vue'
import FormFooter from '@/components/reimburse/form/FormFooter.vue'
import FormHeader from '@/components/reimburse/form/FormHeader.vue'
import ItinerarySection from '@/components/reimburse/form/ItinerarySection.vue'
import RemarkSection from '@/components/reimburse/form/RemarkSection.vue'
import SubsidySection from '@/components/reimburse/form/SubsidySection.vue'
import { useReimburseForm } from '@/composables/useReimburseForm'

const route = useRoute()
const editId = computed(() => route.params.id as string | undefined)

const {
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
} = useReimburseForm(editId)
</script>

<template>
  <div class="reim-form-page">
    <FormHeader :submit-date="form.submitDate" />

    <div class="reim-form-scroll">
      <div class="reim-form-content">
        <BasicInfoSection
          :form="form"
          :collapsed="collapsed.basic"
          :companies="companies"
          :departments="departments"
          :reimbursers="reimbursers"
          :business-type-tree="businessTypeTree"
          @update:collapsed="collapsed.basic = $event"
        />

        <ItinerarySection
          :itineraries="form.itineraries"
          :collapsed="collapsed.itinerary"
          @update:collapsed="collapsed.itinerary = $event"
          @add="openItineraryDialog(undefined, false)"
          @edit="openItineraryDialog($event, false)"
          @copy="copyItinerary"
          @delete="deleteItinerary"
        />

        <SubsidySection
          v-if="form.itineraries.length"
          :subsidies="form.subsidies"
          :collapsed="collapsed.subsidy"
          :summary-total="subsidySummary.total"
          :summary-parts="subsidySummary.parts"
          @update:collapsed="collapsed.subsidy = $event"
          @edit="openSubsidyCalendar"
        />

        <ExpenseTotalSection
          :collapsed="collapsed.total"
          :total="expenseTotal.total"
          :meal="expenseTotal.meal"
          :transport="expenseTotal.transport"
          :comm="expenseTotal.comm"
          @update:collapsed="collapsed.total = $event"
        />

        <AllocationSection
          :allocations="form.allocations"
          :collapsed="collapsed.allocation"
          :expense-total="expenseTotal.total"
          :companies="companies"
          :projects="projects"
          :total-ratio="allocationTotalRatio"
          :total-amount="allocationTotalAmount"
          :get-ratio-input-value="getRatioInputValue"
          @update:collapsed="collapsed.allocation = $event"
          @cost-change="onCostAttributionChange"
          @project-change="onProjectChange"
          @ratio-change="onRatioChange"
          @equal-split="equalSplit"
          @add-row="addAllocationRow"
          @delete-row="deleteAllocationRow"
        />

        <RemarkSection
          :form="form"
          :collapsed="collapsed.remark"
          @update:collapsed="collapsed.remark = $event"
          @delete-remark="deleteRemark"
        />
      </div>
    </div>

    <FormFooter :submitting="submitting" @close="handleClose" @submit="handleSubmit" />

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
