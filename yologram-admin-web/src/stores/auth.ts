import { atom } from 'jotai'
import { atomWithStorage } from 'jotai/utils'

export type AdminRole = 'OWNER' | 'ADMIN'

export interface AuthState {
  uid: number
  email: string
  name: string
  role: AdminRole
  accessToken: string
}

export const authAtom = atomWithStorage<AuthState | null>('auth', null, undefined, {
  getOnInit: true,
})
export const isAuthenticatedAtom = atom((get) => get(authAtom) !== null)
