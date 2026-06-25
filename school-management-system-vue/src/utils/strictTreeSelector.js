export function collectDescendantIds(node) {
  const ids = []
  const walk = (treeNode) => {
    if (!treeNode?.childNodes?.length) return
    for (const child of treeNode.childNodes) {
      ids.push(child.data.id)
      walk(child)
    }
  }
  walk(node)
  return ids
}

export function collectAncestorIds(node) {
  const ids = []
  let parent = node?.parent
  while (parent && parent.level > 0) {
    ids.push(parent.data.id)
    parent = parent.parent
  }
  return ids
}

export function buildTreeCheckedKeys(treeRef, selectedIds) {
  if (!treeRef || !selectedIds?.length) return []
  const keys = new Set()
  for (const id of selectedIds) {
    keys.add(id)
    const node = treeRef.getNode(id)
    if (node?.childNodes?.length) {
      for (const descId of collectDescendantIds(node)) {
        keys.add(descId)
      }
    }
  }
  return Array.from(keys)
}

export function applyStrictTreeCheckSelection({
  sourceTree,
  data,
  isChecked,
  getSelectedIds,
  setSelectedIds,
  syncTreeCheckedKeys,
  onAncestorBlock
}) {
  const node = sourceTree.getNode(data.id)
  if (!node) return

  let selectedIds = [...getSelectedIds()]

  if (isChecked) {
    const hasChildren = node.childNodes?.length > 0
    if (hasChildren) {
      const descendantIds = new Set(collectDescendantIds(node))
      selectedIds = selectedIds.filter(id => id !== data.id && !descendantIds.has(id))
      if (!selectedIds.includes(data.id)) {
        selectedIds.push(data.id)
      }
    } else {
      const ancestorIds = new Set(collectAncestorIds(node))
      selectedIds = selectedIds.filter(id => !ancestorIds.has(id))
      if (!selectedIds.includes(data.id)) {
        selectedIds.push(data.id)
      }
    }
    setSelectedIds(selectedIds)
    syncTreeCheckedKeys()
    return
  }

  let parent = node.parent
  while (parent && parent.level > 0) {
    if (selectedIds.includes(parent.data.id)) {
      onAncestorBlock?.()
      syncTreeCheckedKeys()
      return
    }
    parent = parent.parent
  }

  setSelectedIds(selectedIds.filter(id => id !== data.id))
  syncTreeCheckedKeys()
}

export function findNodeInTree(id, tree) {
  for (const node of tree) {
    if (node.id === id) return node
    if (node.children) {
      const found = findNodeInTree(id, node.children)
      if (found) return found
    }
  }
  return null
}
