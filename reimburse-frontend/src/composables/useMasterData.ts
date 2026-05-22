import { ref } from 'vue'
import { fetchMasterData, type MasterDataBundle } from '@/api/master'
import { MASTER_MOCK_BUNDLE } from '@/data/mockData'
import type { BusinessType, City, Project, ReimCompany, ReimDepartment, Reimburser } from '@/types/reimburse'
import { setCitiesCache } from '@/utils/reimburse'

const loading = ref(false)
const error = ref<string | null>(null)

/** 5.3 控件数据：默认使用前端 Mock，保证下拉框始终有选项 */
const companies = ref<ReimCompany[]>([...MASTER_MOCK_BUNDLE.companies])
const departments = ref<ReimDepartment[]>([...MASTER_MOCK_BUNDLE.departments])
const reimbursers = ref<Reimburser[]>([...MASTER_MOCK_BUNDLE.reimbursers])
const businessTypes = ref<BusinessType[]>([...MASTER_MOCK_BUNDLE.businessTypes])
const cities = ref<City[]>([...MASTER_MOCK_BUNDLE.cities])
const projects = ref<Project[]>([...MASTER_MOCK_BUNDLE.projects])

setCitiesCache(MASTER_MOCK_BUNDLE.cities)
const loaded = ref(true)

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
      .then((bundle) => {
        const hasData =
          bundle.companies.length > 0 &&
          bundle.departments.length > 0 &&
          bundle.reimbursers.length > 0
        if (hasData) {
          applyBundle(bundle)
        } else {
          applyBundle(MASTER_MOCK_BUNDLE)
        }
      })
      .catch((e: unknown) => {
        error.value = e instanceof Error ? e.message : '加载基础数据失败，已使用前端数据'
        applyBundle(MASTER_MOCK_BUNDLE)
      })
      .finally(() => {
        loading.value = false
      })
    await loadPromise
    if (error.value) {
      console.warn('[useMasterData]', error.value)
    }
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
