<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Edit, MoreFilled, Operation } from '@element-plus/icons-vue'
import type { ListQuery, ReimburseListItem } from '@/types/reimburse'
import { DOC_STATUS_MAP } from '@/data/constants'
import { useMasterData } from '@/composables/useMasterData'
import { copyReimburse, deleteReimburse, fetchReimburseList } from '@/api/reimburse'
import { formatMoney } from '@/utils/format'
import { buildBusinessTypeTree, isBusinessTypeLeaf } from '@/utils/businessTypeTree'

const router = useRouter()
const { companies, departments, reimbursers, businessTypes } = useMasterData()

const query = reactive<ListQuery>({
  reimburseNo: '',
  title: '',
  reason: '',
  companyId: '',
  departmentId: '',
  reimburserId: '',
  businessTypeId: '',
})

const tableData = ref<ReimburseListItem[]>([])
const total = ref(0)
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)

const businessTypeTree = computed(() => buildBusinessTypeTree(businessTypes))

const lastQueryBusinessTypeId = ref('')

watch(
  () => query.businessTypeId,
  (id) => {
    if (!id) {
      lastQueryBusinessTypeId.value = ''
      return
    }
    if (!isBusinessTypeLeaf(id, businessTypes)) {
      query.businessTypeId = lastQueryBusinessTypeId.value
      ElMessage.warning('请选择末级业务类型')
    } else {
      lastQueryBusinessTypeId.value = id
    }
  },
)

async function loadList() {
  loading.value = true
  try {
    const data = await fetchReimburseList({
      ...query,
      page: currentPage.value,
      size: pageSize.value,
    })
    tableData.value = data.records
    total.value = data.total
  } catch (e) {
    tableData.value = []
    total.value = 0
    ElMessage.error(e instanceof Error ? e.message : '加载列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadList()
})

function handleSearch() {
  currentPage.value = 1
  loadList()
}

function handleClear() {
  query.reimburseNo = ''
  query.title = ''
  query.reason = ''
  query.companyId = ''
  query.departmentId = ''
  query.reimburserId = ''
  query.businessTypeId = ''
  lastQueryBusinessTypeId.value = ''
  currentPage.value = 1
  loadList()
}

function goNew() {
  router.push('/reimburse/form')
}

function goDetail(row: ReimburseListItem) {
  router.push(`/reimburse/form/${row.id}`)
}

function formatClaimant(row: ReimburseListItem) {
  return `${row.reimburserName}[${row.reimburserNo}]`
}

function formatDepartment(row: ReimburseListItem) {
  return `[${row.departmentNo}]${row.departmentName}`
}

function deptLabel(d: (typeof departments)[0]) {
  return `[${d.reimDepartmentNo}]${d.reimDepartmentName}`
}

function reimburserLabel(r: (typeof reimbursers)[0]) {
  return `${r.reimburserName}[${r.reimburserNo}]`
}

async function handleDelete(row: ReimburseListItem) {
  try {
    await ElMessageBox.confirm('确认删除该报销单?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteReimburse(row.id)
    ElMessage.success('删除成功')
    await loadList()
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e instanceof Error ? e.message : '删除失败')
  }
}

async function handleCopy(row: ReimburseListItem) {
  try {
    await copyReimburse(row.id)
    ElMessage.success('复制成功')
    currentPage.value = 1
    await loadList()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '复制失败')
  }
}

function handlePageChange(page: number) {
  currentPage.value = page
  loadList()
}

function handleSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  loadList()
}
</script>

<template>
  <div class="list-page">
    <div class="list-card">
      <!-- 5.1.2.1 列表查询条件 -->
      <el-form :model="query" label-width="100px" class="list-query-form">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-form-item label="报销单号">
              <el-input v-model="query.reimburseNo" placeholder="请输入" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="标题">
              <el-input v-model="query.title" placeholder="请输入" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="事由">
              <el-input v-model="query.reason" placeholder="请输入" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="费用归属公司">
              <el-select v-model="query.companyId" placeholder="请选择" clearable style="width: 100%">
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
        <el-row :gutter="16" class="query-row-second">
          <el-col :span="6">
            <el-form-item label="报销部门">
              <el-select v-model="query.departmentId" placeholder="请选择" clearable style="width: 100%">
                <el-option
                  v-for="d in departments"
                  :key="d.reimDepartmentId"
                  :label="deptLabel(d)"
                  :value="d.reimDepartmentId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="报销人">
              <el-select v-model="query.reimburserId" placeholder="请选择" clearable style="width: 100%">
                <el-option
                  v-for="r in reimbursers"
                  :key="r.reimburserId"
                  :label="reimburserLabel(r)"
                  :value="r.reimburserId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="业务类型">
              <el-tree-select
                v-model="query.businessTypeId"
                :data="businessTypeTree"
                check-strictly
                default-expand-all
                expand-on-click-node
                clearable
                placeholder="请选择"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="6" class="query-btns-col">
            <div class="list-query-btns">
              <el-button class="btn-ghost" @click="goNew">新增</el-button>
              <el-button class="btn-ghost" @click="handleClear">清除</el-button>
              <el-button type="primary" @click="handleSearch">搜索</el-button>
            </div>
          </el-col>
        </el-row>
      </el-form>

      <div class="list-table-wrap">
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        fit
        class="list-table"
        style="width: 100%"
      >
        <el-table-column width="50" align="center" class-name="col-index">
          <template #header>
            <el-icon class="col-list-icon"><Operation /></el-icon>
          </template>
          <template #default="{ $index }">
            <span class="col-index-num">{{ (currentPage - 1) * pageSize + $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" class-name="col-action">
          <template #default="{ row }">
            <div class="list-table-actions">
              <el-tooltip content="查看" placement="top">
                <el-icon class="action-icon action-icon-muted" @click="goDetail(row)"><Document /></el-icon>
              </el-tooltip>
              <el-tooltip content="编辑" placement="top">
                <el-icon
                  class="action-icon"
                  :class="{ 'action-icon-muted': row.status !== 0 }"
                  @click="goDetail(row)"
                ><Edit /></el-icon>
              </el-tooltip>
              <el-dropdown trigger="hover" placement="bottom">
                <el-icon class="action-icon more-icon"><MoreFilled /></el-icon>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="handleDelete(row)">删除</el-dropdown-item>
                    <el-dropdown-item disabled>手工推送</el-dropdown-item>
                    <el-dropdown-item @click="handleCopy(row)">复制</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="报销单号" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="list-link" @click="goDetail(row)">{{ row.reimburseNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="单据状态" width="78" align="center">
          <template #default="{ row }">
            <span class="status-text">{{ DOC_STATUS_MAP[row.status] }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="docType" label="单据类型" min-width="100" show-overflow-tooltip />
        <el-table-column label="报销人" min-width="115" show-overflow-tooltip>
          <template #default="{ row }">{{ formatClaimant(row) }}</template>
        </el-table-column>
        <el-table-column label="报销部门" min-width="125" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDepartment(row) }}</template>
        </el-table-column>
        <el-table-column prop="companyName" label="费用归属公司" min-width="125" show-overflow-tooltip />
        <el-table-column prop="businessTypeName" label="业务类型" min-width="90" show-overflow-tooltip />
        <el-table-column label="报销标题" min-width="125" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="list-link" @click="goDetail(row)">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="报销事由" min-width="90" show-overflow-tooltip />
        <el-table-column label="补助金额" width="86" align="right" header-align="right">
          <template #default="{ row }">{{ formatMoney(row.subsidyAmount) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="100" show-overflow-tooltip />
      </el-table>
      </div>

      <div class="list-pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.list-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-height: 100vh;
  padding: 16px;
  background: #f0f2f5;
  overflow: hidden;
  box-sizing: border-box;
}

.list-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  padding: 16px 16px 0;
}

.list-query-form {
  flex-shrink: 0;
}

.list-table-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
  -webkit-overflow-scrolling: touch;
}

.list-query-form :deep(.el-form-item) {
  margin-bottom: 12px;
}

.list-query-form :deep(.el-form-item__label) {
  font-size: 14px;
  color: #333;
}

.query-row-second {
  align-items: flex-end;
}

.query-btns-col {
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
  padding-bottom: 12px;
}

.list-query-btns {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  width: 100%;
}

.btn-ghost {
  color: #1890ff;
  border-color: #1890ff;
  background: #fff;
}

.btn-ghost:hover,
.btn-ghost:focus {
  color: #40a9ff;
  border-color: #40a9ff;
  background: #fff;
}

.list-table {
  width: 100%;
}

.list-table :deep(.el-table__inner-wrapper),
.list-table :deep(.el-table__header),
.list-table :deep(.el-table__body) {
  width: 100% !important;
}

.list-table :deep(table) {
  table-layout: fixed;
  width: 100% !important;
}

.list-table :deep(.el-table__header th) {
  background: #fafafa !important;
  color: #333;
  font-weight: 500;
  font-size: 14px;
}

.list-table :deep(.el-table__body td) {
  font-size: 14px;
  color: #333;
}

.list-table :deep(.el-table__cell) {
  padding: 8px 0;
}

.list-table :deep(.cell) {
  padding-left: 8px;
  padding-right: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.list-table :deep(.el-table--border .el-table__cell) {
  border-color: #e8e8e8;
}

.list-table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.action-icon {
  color: #1890ff;
  font-size: 17px;
  cursor: pointer;
  outline: none;
}

.action-icon:hover {
  opacity: 0.75;
}

.more-icon {
  border: 1px solid #1890ff;
  border-radius: 50%;
  padding: 2px;
  width: 20px;
  height: 20px;
}

.list-link {
  color: #1890ff;
  cursor: pointer;
}

.list-link:hover {
  text-decoration: underline;
}

.col-list-icon {
  color: #1890ff;
  font-size: 18px;
  vertical-align: middle;
}

.col-index-num {
  font-size: 14px;
  color: #333;
}

.list-table :deep(.col-index .cell),
.list-table :deep(.col-action .cell) {
  padding-left: 8px;
  padding-right: 8px;
}

.status-text {
  font-size: 14px;
  color: #1890ff;
}

.action-icon-muted {
  color: #c0c4cc !important;
}

.list-pagination {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  padding: 12px 0;
  margin-top: 0;
  border-top: 1px solid #e8e8e8;
  background: #fff;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);
}

.list-pagination :deep(.el-pagination.is-background .el-pager li.is-active) {
  background: #fff;
  color: #1890ff;
  border: 1px solid #1890ff;
}

.list-pagination :deep(.el-pagination.is-background .el-pager li) {
  background: #fff;
  border: 1px solid #d9d9d9;
  margin: 0 4px;
  min-width: 32px;
}

.list-pagination :deep(.el-pagination__total),
.list-pagination :deep(.el-pagination__jump) {
  color: #666;
  font-size: 14px;
}
</style>
