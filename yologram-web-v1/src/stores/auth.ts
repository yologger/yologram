import { atom } from 'jotai'
import { atomWithStorage } from 'jotai/utils'

export interface AuthState {
  uid: number
  email: string
  nickname: string
  accessToken: string
}

export const authAtom = atomWithStorage<AuthState | null>('auth', null)
export const isAuthenticatedAtom = atom((get) => get(authAtom) !== null)
