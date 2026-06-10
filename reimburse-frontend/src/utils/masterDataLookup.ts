/**
 * 主数据查询：按 ID 从本地主数据中查找公司、部门、人员等。
 */

import { MASTER_DATA } from '@/data/masterData'

/** 按公司 ID 查找费用归属公司 */
export function findCompany(id: string) {
  return MASTER_DATA.companies.find((c) => c.reimCompanyId === id)
}

/** 按部门 ID 查找报销部门 */
export function findDepartment(id: string) {
  return MASTER_DATA.departments.find((d) => d.reimDepartmentId === id)
}

/** 按人员 ID 查找报销人/出行人 */
export function findReimburser(id: string) {
  return MASTER_DATA.reimbursers.find((r) => r.reimburserId === id)
}

/** 按业务类型 ID 查找业务类型 */
export function findBusinessType(id: string) {
  return MASTER_DATA.businessTypes.find((b) => b.businessTypeId === id)
}

/** 按城市编号查找城市（含城市类型，用于补助标准） */
export function findCity(cityNo: string) {
  return MASTER_DATA.cities.find((c) => c.cityNo === cityNo)
}

/** 按项目 ID 查找项目 */
export function findProject(projectId: string) {
  return MASTER_DATA.projects.find((p) => p.projectId === projectId)
}
