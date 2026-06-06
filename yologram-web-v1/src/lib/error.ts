import axios from 'axios'

export function getErrorMessage(error: unknown): string {
  if (!axios.isAxiosError(error)) {
    return '알 수 없는 오류가 발생했습니다.'
  }

  if (!error.response) {
    return '서버에 연결할 수 없습니다.'
  }

  if (error.response.data?.errorMessage) {
    return error.response.data.errorMessage
  }

  if (error.response.status >= 500) {
    return '서버 오류가 발생했습니다.'
  }

  return '알 수 없는 오류가 발생했습니다.'
}
