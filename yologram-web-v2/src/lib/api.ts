import axios from 'axios'
import { getDefaultStore } from 'jotai'
import { authAtom } from '../stores/auth'

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL,
})

api.interceptors.request.use((config) => {
  const store = getDefaultStore()
  const auth = store.get(authAtom)
  if (auth?.accessToken) {
    config.headers['Authorization'] = `Bearer ${auth.accessToken}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const url = error.config?.url || ''
    if (error.response?.status === 401 && !url.includes('/ums/auth/')) {
      const store = getDefaultStore()
      store.set(authAtom, null)
    }
    return Promise.reject(error)
  }
)

export default api
