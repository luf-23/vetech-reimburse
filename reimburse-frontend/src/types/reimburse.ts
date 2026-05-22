/** 0草稿 1审批通过 2已作废 3审批中 */
export type DocStatus = 0 | 1 | 2 | 3

export interface ReimCompany {
  reimCompanyId: string
  reimCompanyNo: string
  reimCompanyName: string
}

export interface ReimDepartment {
  reimDepartmentId: string
  reimDepartmentNo: string
  reimDepartmentName: string
}

export interface Reimburser {
  reimburserId: string
  reimburserNo: string
  reimburserName: string
}

export interface BusinessType {
  businessTypeId: string
  businessTypeNo: string
  businessTypeName: string
  thereSubordinateNode: string
  superiorId: string
}

export interface City {
  cityNo: string
  cityName: string
  cityType: string
}

export interface Project {
  projectId: string
  projectNo: string
  projectName: string
}

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

export type SubsidyType = 'meal' | 'transport' | 'comm'

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

export interface AllocationItem {
  id: string
  costAttributionId: string
  costAttributionName: string
  projectId: string
  projectName: string
  ratio: number
  amount: number
}

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

export interface ListQuery {
  reimburseNo: string
  title: string
  reason: string
  companyId: string
  departmentId: string
  reimburserId: string
  businessTypeId: string
}
