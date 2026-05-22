import type { BusinessType } from '@/types/reimburse'

export interface TreeNode {
  value: string
  label: string
  children?: TreeNode[]
}

/** 仅末级（thereSubordinateNode === '0'）可选 */
export function isBusinessTypeLeaf(typeId: string, types: BusinessType[]): boolean {
  const t = types.find((x) => x.businessTypeId === typeId)
  return t?.thereSubordinateNode === '0'
}

export function buildBusinessTypeTree(types: BusinessType[]): TreeNode[] {
  const roots = types.filter((t) => t.superiorId === 'none')
  function buildChildren(parentId: string): TreeNode[] {
    return types
      .filter((t) => t.superiorId === parentId)
      .map((t) => ({
        value: t.businessTypeId,
        label: t.businessTypeName,
        children: t.thereSubordinateNode === '1' ? buildChildren(t.businessTypeId) : undefined,
      }))
  }
  return roots.map((r) => ({
    value: r.businessTypeId,
    label: r.businessTypeName,
    children: buildChildren(r.businessTypeId),
  }))
}
