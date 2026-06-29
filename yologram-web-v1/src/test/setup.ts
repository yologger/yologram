import '@testing-library/jest-dom/vitest'
import { afterEach } from 'vitest'
import { cleanup } from '@testing-library/react'

// antd Modal.confirm/message는 document.body의 portal로 렌더되어 RTL 자동 cleanup이
// 닿지 않는 경우가 있다. 테스트 간 누적을 막기 위해 매 테스트 후 잔여 portal을 제거한다.
afterEach(() => {
  cleanup()
  document.body
    .querySelectorAll('.ant-modal-root, .ant-message, .ant-notification, .ant-modal-wrap')
    .forEach((el) => el.parentElement?.removeChild(el))
})

const localStorageMock = (() => {
  let store: Record<string, string> = {}

  return {
    getItem: (key: string) => store[key] ?? null,
    setItem: (key: string, value: string) => {
      store[key] = value
    },
    removeItem: (key: string) => {
      delete store[key]
    },
    clear: () => {
      store = {}
    },
  }
})()

Object.defineProperty(window, 'localStorage', {
  value: localStorageMock,
})

Object.defineProperty(globalThis, 'localStorage', {
  value: localStorageMock,
})

Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  }),
})
