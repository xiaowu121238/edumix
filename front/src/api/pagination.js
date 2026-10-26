/**
 * 将后端分页结构适配成原有的接口结果形状。
 * data 继续表示当前页数组，page 保存 total、pages 等分页信息，旧页面无需整体重写。
 */
export function unwrapPage(response) {
  const payload = response?.data
  if (!payload || Array.isArray(payload) || !Array.isArray(payload.records)) return response
  return { ...response, data: payload.records, page: payload }
}

// 仅用于确实需要全部收藏上下文的调用；普通列表应直接使用服务端分页。
export async function collectPages(fetchPage, params = {}) {
  const records = new Map()
  let page = 1,
    result
  do {
    result = await fetchPage({ ...params, page, size: 100 })
    for (const item of result.data || []) records.set(item.id, item)
    if (!result.page || page >= result.page.pages) break
    if (!result.data.length) throw new Error('分页数据已变化，请刷新重试')
    page++
  } while (true)
  return { ...result, data: [...records.values()] }
}
