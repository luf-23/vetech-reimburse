/**
 * 差旅报销领域类型定义：主数据、列表、表单、行程、补助、分摊等。
 */

/** 单据状态：0 草稿 1 审批通过 2 已作废 3 审批中 */
export type DocStatus = 0 | 1 | 2 | 3

/** 费用归属公司 */
export interface ReimCompany {
  reimCompanyId: string
  reimCompanyNo: string
  reimCompanyName: string
}

/** 报销部门 */
export interface ReimDepartment {
  reimDepartmentId: string
  reimDepartmentNo: string
  reimDepartmentName: string
}

/** 报销人 / 出行人员 */
export interface Reimburser {
  reimburserId: string
  reimburserNo: string
  reimburserName: string
}

/** 业务类型（树形，含是否有下级） */
export interface BusinessType {
  businessTypeId: string
  businessTypeNo: string
  businessTypeName: string
  thereSubordinateNode: string
  superiorId: string
}

/** 城市（含类型，用于补助标准） */
export interface City {
  cityNo: string
  cityName: string
  cityType: string
}

/** 项目 */
export interface Project {
  projectId: string
  projectNo: string
  projectName: string
}

/** 报销单列表行 */
export interface ReimburseListItem {
  id: string
  reimburseNo: string
  docType: string
  status: DocStatus
  reimburserId: string
  reimburserName: string
  reimburserNo: string
  departmentId: string
  departmentName: string
  departmentNo: string
  companyId: string
  companyName: string
  businessTypeId: string
  businessTypeName: string
  title: string
  reason: string
  subsidyAmount: number
  createTime: string
}

/** 补录行程单行 */
export interface ItineraryItem {
  id: string
  travelerId: string
  travelerName: string
  travelerNo: string
  departCityNo: string
  departCityName: string
  arriveCityNo: string
  arriveCityName: string
  startDate: string
  endDate: string
  description: string
}

/** 补助类型：餐补、交通、通讯 */
export type SubsidyType = 'meal' | 'transport' | 'comm'

/** 补助日历中单日明细 */
export interface SubsidyDayItem {
  date: string
  weekday: string
  cityNo: string
  cityName: string
  cityType: string
  meal: { checked: boolean; standard: number; amount: number }
  transport: { checked: boolean; standard: number; amount: number }
  comm: { checked: boolean; standard: number; amount: number }
  isPlanned?: boolean
}

/** 与一条行程对应的补助汇总（含日历） */
export interface SubsidyInfoItem {
  id: string
  itineraryId: string
  travelerId: string
  travelerName: string
  startDate: string
  endDate: string
  days: number
  route: string
  subsidyCityNo: string
  subsidyCityName: string
  applyAmount: number
  subsidyAmount: number
  calendar: SubsidyDayItem[]
  mealTotal: number
  transportTotal: number
  commTotal: number
}

/** 费用归属及分摊单行 */
export interface AllocationItem {
  id: string
  costAttributionId: string
  costAttributionName: string
  projectId: string
  projectName: string
  ratio: number
  amount: number
}

/** 报销单完整表单数据 */
export interface ReimburseFormData {
  id?: string
  reimburseNo?: string
  status: DocStatus
  submitDate: string
  title: string
  reason: string
  reimburserId: string
  departmentId: string
  companyId: string
  businessTypeId: string
  itineraries: ItineraryItem[]
  subsidies: SubsidyInfoItem[]
  allocations: AllocationItem[]
  remark: string
}

/** 列表页查询条件 */
export interface ListQuery {
  reimburseNo: string
  title: string
  reason: string
  companyId: string
  departmentId: string
  reimburserId: string
  businessTypeId: string
}
