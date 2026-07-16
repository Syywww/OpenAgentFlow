import { describe, expect, it } from 'vitest';
import { scrollToLatest } from './chatScroll';

describe('scrollToLatest', () => {
  it('将消息容器定位到内容底部', () => {
    const container = { scrollTop: 0, scrollHeight: 1280 };

    scrollToLatest(container);

    expect(container.scrollTop).toBe(1280);
  });

  it('容器尚未挂载时保持静默', () => {
    expect(() => scrollToLatest(null)).not.toThrow();
  });
});
