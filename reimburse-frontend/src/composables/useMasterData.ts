import { MASTER_DATA } from '@/data/masterData'

/** 5.3 主数据（只读，与 masterData.ts 同源） */
export function useMasterData() {
  return {
    companies: MASTER_DATA.companies,
    departments: MASTER_DATA.departments,
    reimbursers: MASTER_DATA.reimbursers,
    businessTypes: MASTER_DATA.businessTypes,
    cities: MASTER_DATA.cities,
    projects: MASTER_DATA.projects,
  }
}
