import type {
  ItineraryItem,
  ReimburseFormData,
  ReimburseListItem,
  SubsidyInfoItem,
} from '@/types/reimburse'
import {
  findBusinessType,
  findCity,
  findCompany,
  findDepartment,
  findProject,
  findReimburser,
} from '@/utils/masterDataLookup'

/** 接口只带 ID 时，用本地主数据补全列表展示字段 */
export function enrichListItem(row: ReimburseListItem): ReimburseListItem {
  const r = findReimburser(row.reimburserId)
  const d = findDepartment(row.departmentId)
  const c = findCompany(row.companyId)
  const b = findBusinessType(row.businessTypeId)
  return {
    ...row,
    reimburserName: r?.reimburserName ?? row.reimburserName ?? '',
    reimburserNo: r?.reimburserNo ?? row.reimburserNo ?? '',
    departmentName: d?.reimDepartmentName ?? row.departmentName ?? '',
    departmentNo: d?.reimDepartmentNo ?? row.departmentNo ?? '',
    companyName: c?.reimCompanyName ?? row.companyName ?? '',
    businessTypeName: b?.businessTypeName ?? row.businessTypeName ?? '',
  }
}

function enrichItinerary(it: ItineraryItem): ItineraryItem {
  const traveler = findReimburser(it.travelerId)
  const depart = findCity(it.departCityNo)
  const arrive = findCity(it.arriveCityNo)
  return {
    ...it,
    travelerName: traveler?.reimburserName ?? it.travelerName ?? '',
    travelerNo: traveler?.reimburserNo ?? it.travelerNo ?? '',
    departCityName: depart?.cityName ?? it.departCityName ?? '',
    arriveCityName: arrive?.cityName ?? it.arriveCityName ?? '',
  }
}

function enrichSubsidy(sub: SubsidyInfoItem): SubsidyInfoItem {
  const traveler = findReimburser(sub.travelerId)
  const city = findCity(sub.subsidyCityNo)
  return {
    ...sub,
    travelerName: traveler?.reimburserName ?? sub.travelerName ?? '',
    subsidyCityName: city?.cityName ?? sub.subsidyCityName ?? '',
  }
}

/** 接口只带 ID 时，用本地主数据补全详情/表单展示字段 */
export function enrichReimburseForm(data: ReimburseFormData): ReimburseFormData {
  return {
    ...data,
    itineraries: (data.itineraries ?? []).map(enrichItinerary),
    subsidies: (data.subsidies ?? []).map(enrichSubsidy),
    allocations: (data.allocations ?? []).map((a) => {
      const company = findCompany(a.costAttributionId)
      const project = findProject(a.projectId)
      return {
        ...a,
        costAttributionName: company?.reimCompanyName ?? a.costAttributionName ?? '',
        projectName: project?.projectName ?? a.projectName ?? '',
      }
    }),
  }
}
