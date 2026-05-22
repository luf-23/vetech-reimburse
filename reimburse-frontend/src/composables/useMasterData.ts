import { ref } from 'vue'
import { fetchMasterData, type MasterDataBundle } from '@/api/master'
import type { BusinessType, City, Project, ReimCompany, ReimDepartment, Reimburser } from '@/types/reimburse'
import { setCitiesCache } from '@/utils/reimburse'

const loaded = ref(false)
const loading = ref(false)
const error = ref<string | null>(null)

const companies = ref<ReimCompany[]>([])
const departments = ref<ReimDepartment[]>([])
const reimbursers = ref<Reimburser[]>([])
const businessTypes = ref<BusinessType[]>([])
const cities = ref<City[]>([])
const projects = ref<Project[]>([])

let loadPromise: Promise<void> | null = null

function applyBundle(bundle: MasterDataBundle) {
  companies.value = bundle.companies
  departments.value = bundle.departments
  reimbursers.value = bundle.reimbursers
  businessTypes.value = bundle.businessTypes
  cities.value = bundle.cities
  projects.value = bundle.projects
  setCitiesCache(bundle.cities)
  loaded.value = true
}

export function useMasterData() {
  async function ensureLoaded() {
    if (loaded.value) return
    if (loadPromise) {
      await loadPromise
      return
    }
    loading.value = true
    error.value = null
    loadPromise = fetchMasterData()
      .then(applyBundle)
      .catch((e: unknown) => {
        error.value = e instanceof Error ? e.message : '加载基础数据失败'
        throw e
      })
      .finally(() => {
        loading.value = false
      })
    await loadPromise
  }

  return {
    loaded,
    loading,
    error,
    companies,
    departments,
    reimbursers,
    businessTypes,
    cities,
    projects,
    ensureLoaded,
  }
}
