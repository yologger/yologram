import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import SearchBar from './SearchBar'

const mockNavigate = vi.fn()
vi.mock('react-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router')>()
  return { ...actual, useNavigate: () => mockNavigate }
})

let mockIsMobile = false
vi.mock('../../hooks/useIsMobile', () => ({ default: () => mockIsMobile }))

beforeEach(() => {
  mockNavigate.mockClear()
  mockIsMobile = false
})

function getInput() {
  return screen.getByPlaceholderText('검색어를 입력하세요')
}

describe('SearchBar (데스크탑)', () => {
  it('Enter 시 키워드 경로로 navigate한다', () => {
    render(<SearchBar basePath="/tech" />)
    fireEvent.change(getInput(), { target: { value: 'react' } })
    fireEvent.keyDown(getInput(), { key: 'Enter' })
    expect(mockNavigate).toHaveBeenCalledWith('/tech/keywords/react')
  })

  it('한글 키워드는 인코딩된 경로로 navigate한다', () => {
    render(<SearchBar basePath="/tech" />)
    fireEvent.change(getInput(), { target: { value: '제미나이' } })
    fireEvent.keyDown(getInput(), { key: 'Enter' })
    expect(mockNavigate).toHaveBeenCalledWith('/tech/keywords/%EC%A0%9C%EB%AF%B8%EB%82%98%EC%9D%B4')
  })

  it('앞뒤 공백은 trim 후 navigate한다', () => {
    render(<SearchBar basePath="/invest" />)
    fireEvent.change(getInput(), { target: { value: '  삼성전자  ' } })
    fireEvent.keyDown(getInput(), { key: 'Enter' })
    expect(mockNavigate).toHaveBeenCalledWith(`/invest/keywords/${encodeURIComponent('삼성전자')}`)
  })

  it('빈 입력으로 Enter 시 navigate하지 않는다', () => {
    render(<SearchBar basePath="/tech" />)
    fireEvent.keyDown(getInput(), { key: 'Enter' })
    expect(mockNavigate).not.toHaveBeenCalled()
  })

  it('공백만 입력 후 Enter 시 navigate하지 않는다', () => {
    render(<SearchBar basePath="/tech" />)
    fireEvent.change(getInput(), { target: { value: '   ' } })
    fireEvent.keyDown(getInput(), { key: 'Enter' })
    expect(mockNavigate).not.toHaveBeenCalled()
  })

  it('initialValue가 입력창에 표시된다', () => {
    render(<SearchBar basePath="/tech" initialValue="제미나이" />)
    expect(screen.getByDisplayValue('제미나이')).toBeInTheDocument()
  })
})

describe('SearchBar (모바일)', () => {
  beforeEach(() => {
    mockIsMobile = true
  })

  it('인라인 입력창 대신 돋보기 아이콘 버튼을 렌더한다', () => {
    render(<SearchBar basePath="/tech" />)
    expect(screen.queryByPlaceholderText('검색어를 입력하세요')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: '검색' })).toBeInTheDocument()
  })

  it('아이콘 클릭 시 오버레이 입력창이 표시되고 autofocus된다', () => {
    render(<SearchBar basePath="/tech" />)
    fireEvent.click(screen.getByRole('button', { name: '검색' }))
    const input = getInput()
    expect(input).toBeInTheDocument()
    expect(input).toHaveFocus()
  })

  it('뒤로 버튼 클릭 시 오버레이가 닫힌다', () => {
    render(<SearchBar basePath="/tech" />)
    fireEvent.click(screen.getByRole('button', { name: '검색' }))
    fireEvent.click(screen.getByRole('button', { name: '뒤로' }))
    expect(screen.queryByPlaceholderText('검색어를 입력하세요')).not.toBeInTheDocument()
  })

  it('ESC 키 입력 시 오버레이가 닫힌다', () => {
    render(<SearchBar basePath="/tech" />)
    fireEvent.click(screen.getByRole('button', { name: '검색' }))
    fireEvent.keyDown(getInput(), { key: 'Escape' })
    expect(screen.queryByPlaceholderText('검색어를 입력하세요')).not.toBeInTheDocument()
  })

  it('오버레이에서 Enter 시 navigate하고 오버레이가 닫힌다', () => {
    render(<SearchBar basePath="/tech" />)
    fireEvent.click(screen.getByRole('button', { name: '검색' }))
    fireEvent.change(getInput(), { target: { value: '제미나이' } })
    fireEvent.keyDown(getInput(), { key: 'Enter' })
    expect(mockNavigate).toHaveBeenCalledWith('/tech/keywords/%EC%A0%9C%EB%AF%B8%EB%82%98%EC%9D%B4')
    expect(screen.queryByPlaceholderText('검색어를 입력하세요')).not.toBeInTheDocument()
  })

  it('오버레이에서 빈 입력으로 Enter 시 navigate하지 않고 오버레이가 유지된다', () => {
    render(<SearchBar basePath="/tech" />)
    fireEvent.click(screen.getByRole('button', { name: '검색' }))
    fireEvent.keyDown(getInput(), { key: 'Enter' })
    expect(mockNavigate).not.toHaveBeenCalled()
    expect(getInput()).toBeInTheDocument()
  })

  it('initialValue가 있으면 오버레이 입력창에 표시된다', () => {
    render(<SearchBar basePath="/tech" initialValue="제미나이" />)
    fireEvent.click(screen.getByRole('button', { name: '검색' }))
    expect(screen.getByDisplayValue('제미나이')).toBeInTheDocument()
  })
})
