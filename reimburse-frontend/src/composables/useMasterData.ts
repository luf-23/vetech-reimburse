import { ref } from 'vue'
import { fetchMasterData, type MasterDataBundle } from '@/api/master'
import { MASTER_DATA } from '@/data/masterData'
import type { BusinessType, City, Project, ReimCompany, ReimDepartment, Reimburser } from '@/types/reimburse'
import { setCitiesCache } from '@/utils/reimburse'

const companies = ref<ReimCompany[]>([...MASTER_DATA.companies])
const departments = ref<ReimDepartment[]>([...MASTER_DATA.departments])
const reimbursers = ref<Reimburser[]>([...MASTER_DATA.reimbursers])
const businessTypes = ref<BusinessType[]>([...MASTER_DATA.businessTypes])
const cities = ref<City[]>([...MASTER_DATA.cities])
const projects = ref<Project[]>([...MASTER_DATA.projects])

setCitiesCache(MASTER_DATA.cities)

const loaded = ref(false)
const loading = ref(false)
const error = ref<string | null>(null)
const fromApi = ref(false)

let loadPromise: Promise<void> | null = null

function applyBundle(bundle: MasterDataBundle) {
  companies.value = bundle.companies ?? []
  departments.value = bundle.departments ?? []
  reimbursers.value = bundle.reimbursers ?? []
  businessTypes.value = bundle.businessTypes ?? []
  cities.value = bundle.cities ?? []
  projects.value = bundle.projects ?? []
  setCitiesCache(cities.value)
  loaded.value = true
}

function applyStaticFallback() {
  applyBundle(MASTER_DATA)
  fromApi.value = false
}

export function useMasterData() {
  async function ensureLoaded() {
    if (loaded.value && fromApi.value) return
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
          fromApi.value = true
        } else {
          applyStaticFallback()
        }
      })
      .catch((e: unknown) => {
        error.value = e instanceof Error ? e.message : '加载主数据失败'
        applyStaticFallback()
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
    fromApi,
    companies,
    departments,
    reimbursers,
    businessTypes,
    cities,
    projects,
    ensureLoaded,
  }
}
