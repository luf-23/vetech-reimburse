<script setup lang="ts">
import type { TreeNode } from '@/utils/businessTypeTree'
import SectionPanel from '@/components/reimburse/SectionPanel.vue'
import type { ReimCompany, ReimDepartment, ReimburseFormData, Reimburser } from '@/types/reimburse'

defineProps<{
  form: ReimburseFormData
  collapsed: boolean
  companies: ReimCompany[]
  departments: ReimDepartment[]
  reimbursers: Reimburser[]
  businessTypeTree: TreeNode[]
}>()

const emit = defineEmits<{
  'update:collapsed': [v: boolean]
}>()
</script>

<template>
  <SectionPanel
    :collapsed="collapsed"
    title="基础信息"
    @update:collapsed="emit('update:collapsed', $event)"
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
</template>

<style scoped>
.business-type-select {
  width: 100%;
}
</style>
