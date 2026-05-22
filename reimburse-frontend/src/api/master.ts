import { request } from './http'
import type { BusinessType, City, Project, ReimCompany, ReimDepartment, Reimburser } from '@/types/reimburse'

export interface MasterDataBundle {
  companies: ReimCompany[]
  departments: ReimDepartment[]
  reimbursers: Reimburser[]
  businessTypes: BusinessType[]
  cities: City[]
  projects: Project[]
}

export function fetchMasterData() {
  return request<MasterDataBundle>('/master')
}
