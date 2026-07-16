/** 根据总条数和每页条数计算页数，空列表仍保留第一页。 */
export function pageCount(totalItems: number, pageSize = 10): number {
  const safePageSize = Math.max(1, Math.floor(pageSize));
  return Math.max(1, Math.ceil(Math.max(0, totalItems) / safePageSize));
}

/** 把页码约束在当前列表的有效范围内。 */
export function clampPage(page: number, totalItems: number, pageSize = 10): number {
  return Math.min(Math.max(1, Math.floor(page)), pageCount(totalItems, pageSize));
}

/** 按统一页码规则截取一页数据。 */
export function paginate<T>(items: readonly T[], page: number, pageSize = 10): T[] {
  const safePageSize = Math.max(1, Math.floor(pageSize));
  const safePage = clampPage(page, items.length, safePageSize);
  const start = (safePage - 1) * safePageSize;
  return items.slice(start, start + safePageSize);
}
