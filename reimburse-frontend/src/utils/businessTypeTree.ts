/**
 * 业务类型树：判断末级节点、构建下拉树结构。
 */

import type { BusinessType } from '@/types/reimburse'

/** 业务类型树节点，供级联选择器使用 */
export interface TreeNode {
  value: string
  label: string
  children?: TreeNode[]
}

/** 判断给定业务类型 ID 是否为末级（无下级节点） */
export function isBusinessTypeLeaf(typeId: string, types: BusinessType[]): boolean {
  const t = types.find((x) => x.businessTypeId === typeId)
  return t?.thereSubordinateNode === '0'
}

/** 将扁平业务类型列表递归构建为树形结构 */
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
