/** 可滚动消息容器需要提供的最小属性集合。 */
export interface ScrollContainer {
  /** 当前纵向滚动位置。 */
  scrollTop: number;
  /** 容器全部内容的高度。 */
  readonly scrollHeight: number;
}

/**
 * 将消息容器定位到内容底部。
 *
 * @param container 已挂载的消息容器
 */
export function scrollToLatest(container: ScrollContainer | null | undefined) {
  if (!container) {
    return;
  }
  container.scrollTop = container.scrollHeight;
}
