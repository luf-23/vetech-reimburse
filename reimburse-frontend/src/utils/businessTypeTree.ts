import type { BusinessType } from '@/types/reimburse'
import { BUSINESS_TYPES } from '@/data/mockData'

export interface TreeNode {
  value: string
  label: string
  children?: TreeNode[]
  disabled?: boolean
}

export function buildBusinessTypeTree(types: BusinessType[] = BUSINESS_TYPES): TreeNode[] {
  const roots = types.filter((t) => t.superiorId === 'none')
  function buildChildren(parentId: string): TreeNode[] {
    return types
      .filter((t) => t.superiorId === parentId)
      .map((t) => ({
        value: t.businessTypeId,
        label: t.businessTypeName,
        disabled: t.thereSubordinateNode === '1',
        children: t.thereSubordinateNode === '1' ? buildChildren(t.businessTypeId) : undefined,
      }))
  }
  return roots.map((r) => ({
    value: r.businessTypeId,
    label: r.businessTypeName,
    disabled: r.thereSubordinateNode === '1',
    children: buildChildren(r.businessTypeId),
  }))
}
