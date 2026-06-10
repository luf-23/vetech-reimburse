/**
 * 主数据组合式函数：向组件暴露公司、部门、人员等下拉选项数据。
 */

import { MASTER_DATA } from '@/data/masterData'

/** 返回全部主数据引用，供表单下拉与名称补全使用 */
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
