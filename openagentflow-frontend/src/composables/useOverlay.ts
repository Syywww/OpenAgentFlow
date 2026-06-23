import { reactive } from 'vue';

export type ModalKey =
  | 'new-agent'
  | 'prompt'
  | 'upload'
  | 'schema'
  | 'risk'
  | 'mcp-test'
  | 'node-debug'
  | 'publish'
  | 'eval-task'
  | 'audit'
  | 'toast';

export type DrawerKey = 'sources' | 'step' | 'notices';

interface OverlayState {
  modal: ModalKey | null;
  drawer: DrawerKey | null;
  message: string;
}

const state = reactive<OverlayState>({
  modal: null,
  drawer: null,
  message: '',
});

export function useOverlay() {
  function showModal(modal: ModalKey, message = '') {
    state.modal = modal;
    state.message = message;
  }

  function showDrawer(drawer: DrawerKey) {
    state.drawer = drawer;
  }

  function closeModal() {
    state.modal = null;
    state.message = '';
  }

  function closeDrawer() {
    state.drawer = null;
  }

  function toast(message: string) {
    showModal('toast', message);
  }

  return {
    overlay: state,
    showModal,
    showDrawer,
    closeModal,
    closeDrawer,
    toast,
  };
}
