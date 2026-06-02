import { MASTER_DATA } from '@/data/masterData'

export function findCompany(id: string) {
  return MASTER_DATA.companies.find((c) => c.reimCompanyId === id)
}

export function findDepartment(id: string) {
  return MASTER_DATA.departments.find((d) => d.reimDepartmentId === id)
}

export function findReimburser(id: string) {
  return MASTER_DATA.reimbursers.find((r) => r.reimburserId === id)
}

export function findBusinessType(id: string) {
  return MASTER_DATA.businessTypes.find((b) => b.businessTypeId === id)
}

export function findCity(cityNo: string) {
  return MASTER_DATA.cities.find((c) => c.cityNo === cityNo)
}

export function findProject(projectId: string) {
  return MASTER_DATA.projects.find((p) => p.projectId === projectId)
}
