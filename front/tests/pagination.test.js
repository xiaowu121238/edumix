import test from 'node:test'
import assert from 'node:assert/strict'
import { collectPages, unwrapPage } from '../src/api/pagination.js'

test('collects favorites beyond the first 100 and retains query parameters', async () => {
  const data = Array.from({ length: 121 }, (_, i) => ({ id: i + 1 }))
  const calls = []
  const result = await collectPages(async params => {
    calls.push(params)
    return unwrapPage({ data: { records: data.slice((params.page - 1) * 100, params.page * 100), total: 121, pages: 2 } })
  }, { favoriteOnly: true })
  assert.equal(result.data.length, 121)
  assert.equal(result.data[120].id, 121)
  assert.deepEqual(calls.map(p => [p.page, p.size, p.favoriteOnly]), [[1, 100, true], [2, 100, true]])
})
test('reports a disappearing intermediate page instead of silently truncating', async () => {
  await assert.rejects(collectPages(async () => ({ data: [], page: { pages: 3 } })), /分页数据已变化/)
})
test('empty results terminate and duplicate records are deduplicated', async () => {
  assert.deepEqual((await collectPages(async () => ({ data: [], page: { pages: 0 } }))).data, [])
  const result = await collectPages(async ({page}) => ({ data: [{id: 1}, {id: page + 1}], page: { pages: 2 } }))
  assert.equal(result.data.length, 3)
})
