import { atom } from 'jotai'
import { atomWithStorage } from 'jotai/utils'

export interface AuthState {
  uid: number
  email: string
  name: string
  nickname: string
  accessToken: string
}

export const authAtom = atomWithStorage<AuthState | null>('auth', null, undefined, {
  getOnInit: true,
})
export const isAuthenticatedAtom = atom((get) => get(authAtom) !== null)
