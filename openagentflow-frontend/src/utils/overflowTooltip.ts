const tooltipSelector = [
  '.data-table td',
  '.list-row b',
  '.list-row span',
  '.checkbox-row b',
  '.mono',
  '.template-card h2',
  '.template-card p',
  '.provider-card b',
  '.governance-tab-card small',
].join(',');

const ignoredSelector = [
  'button',
  'input',
  'select',
  'textarea',
  'a',
  '.status-badge',
  '.pagination-bar',
  '.table-actions',
  '.icon-button',
].join(',');

let tooltipElement: HTMLDivElement | null = null;
let activeTarget: HTMLElement | null = null;

function getTooltipElement() {
  if (tooltipElement) {
    return tooltipElement;
  }
  tooltipElement = document.createElement('div');
  tooltipElement.className = 'oaf-overflow-tooltip';
  document.body.appendChild(tooltipElement);
  return tooltipElement;
}

function normalizeText(element: HTMLElement) {
  return (element.innerText || element.textContent || '').replace(/\s+/g, ' ').trim();
}

function shouldShowTooltip(element: HTMLElement, text: string) {
  if (text.length <= 12) {
    return false;
  }
  const hasOverflow = element.scrollWidth > element.clientWidth + 2 || element.scrollHeight > element.clientHeight + 2;
  const isLikelyLongField = text.length > 24 && element.matches('.data-table td, .mono, .list-row b, .list-row span, .checkbox-row b');
  return hasOverflow || isLikelyLongField;
}

function findTarget(rawTarget: EventTarget | null) {
  if (!(rawTarget instanceof Element)) {
    return null;
  }
  if (rawTarget.closest(ignoredSelector)) {
    return null;
  }
  const element = rawTarget.closest<HTMLElement>(tooltipSelector);
  if (!element || element.closest(ignoredSelector)) {
    return null;
  }
  if (element.matches('.data-table td') && element.querySelector(ignoredSelector)) {
    return null;
  }
  return element;
}

function placeTooltip(event: MouseEvent) {
  if (!tooltipElement || !activeTarget) {
    return;
  }
  const offset = 14;
  const rect = tooltipElement.getBoundingClientRect();
  let left = event.clientX + offset;
  let top = event.clientY + offset;
  if (left + rect.width > window.innerWidth - 12) {
    left = Math.max(12, event.clientX - rect.width - offset);
  }
  if (top + rect.height > window.innerHeight - 12) {
    top = Math.max(12, event.clientY - rect.height - offset);
  }
  tooltipElement.style.left = `${left}px`;
  tooltipElement.style.top = `${top}px`;
}

function showTooltip(target: HTMLElement, event: MouseEvent) {
  const text = normalizeText(target);
  if (!shouldShowTooltip(target, text)) {
    hideTooltip();
    return;
  }
  activeTarget = target;
  const tooltip = getTooltipElement();
  tooltip.textContent = text;
  tooltip.classList.add('visible');
  placeTooltip(event);
}

function hideTooltip() {
  activeTarget = null;
  if (tooltipElement) {
    tooltipElement.classList.remove('visible');
  }
}

export function installOverflowTooltip() {
  document.addEventListener('mouseover', (event) => {
    const target = findTarget(event.target);
    if (!target) {
      hideTooltip();
      return;
    }
    showTooltip(target, event as MouseEvent);
  });
  document.addEventListener('mousemove', (event) => {
    placeTooltip(event);
  });
  document.addEventListener('mouseout', (event) => {
    if (activeTarget && event.target instanceof Node && activeTarget.contains(event.target)) {
      const nextTarget = event.relatedTarget instanceof Node ? event.relatedTarget : null;
      if (!nextTarget || !activeTarget.contains(nextTarget)) {
        hideTooltip();
      }
    }
  });
  window.addEventListener('scroll', hideTooltip, true);
  window.addEventListener('resize', hideTooltip);
}
