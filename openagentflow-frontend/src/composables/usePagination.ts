import { computed, ref, watch, type ComputedRef, type Ref } from 'vue';
import { clampPage, pageCount, paginate } from '../utils/pagination';

/**
 * 前端列表统一分页，每页默认10条。
 * 适用于后端一次返回数组的列表，后端分页列表仍可直接使用同一个分页组件展示页码。
 */
export function usePagination<T>(items: Ref<T[]> | ComputedRef<T[]>, pageSize = 10) {
  /** 当前页码，从1开始。 */
  const currentPage = ref(1);

  /** 总条数。 */
  const totalItems = computed(() => items.value.length);

  /** 总页数，空列表也保持为1，避免分页控件出现0页。 */
  const totalPages = computed(() => pageCount(totalItems.value, pageSize));

  /** 当前页需要展示的数据。 */
  const pagedItems = computed(() => {
    return paginate(items.value, currentPage.value, pageSize);
  });

  /** 筛选条件变化导致总页数变少时，自动回到最后一个可用页。 */
  watch(totalPages, (value) => {
    if (currentPage.value > value) {
      currentPage.value = clampPage(currentPage.value, totalItems.value, pageSize);
    }
  });

  /** 列表内容变化时，如果当前页已经无数据，则回到第一页。 */
  watch(totalItems, () => {
    if (currentPage.value > totalPages.value) {
      currentPage.value = clampPage(currentPage.value, totalItems.value, pageSize);
    }
  });

  /** 手动重置页码，通常用于筛选或搜索后回到第一页。 */
  function resetPage() {
    currentPage.value = 1;
  }

  return {
    currentPage,
    pageSize,
    totalItems,
    totalPages,
    pagedItems,
    resetPage,
  };
}
