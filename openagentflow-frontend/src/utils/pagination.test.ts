import { describe, expect, it } from 'vitest';
import { clampPage, pageCount, paginate } from './pagination';

describe('统一分页规则', () => {
  it('默认每页十条并保留空列表第一页', () => {
    expect(pageCount(0)).toBe(1);
    expect(pageCount(21)).toBe(3);
    expect(paginate(Array.from({ length: 25 }, (_, index) => index + 1), 2)).toEqual(
      Array.from({ length: 10 }, (_, index) => index + 11),
    );
  });

  it('列表缩短后把页码约束到最后一页', () => {
    expect(clampPage(8, 12)).toBe(2);
    expect(clampPage(-1, 12)).toBe(1);
  });
});
