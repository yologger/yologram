import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import FilterChips from './FilterChips'

describe('FilterChips', () => {
  it('string 배열을 칩으로 렌더하고 선택 시 값을 전달한다', () => {
    const onChange = vi.fn()
    render(<FilterChips items={['전체', '기술']} selected="전체" onChange={onChange} />)

    expect(screen.getByText('전체')).toBeInTheDocument()
    fireEvent.click(screen.getByText('기술'))
    expect(onChange).toHaveBeenCalledWith('기술')
  })

  it('label/value 객체를 렌더하고 value(숫자)를 전달한다', () => {
    const onChange = vi.fn()
    render(
      <FilterChips
        items={[
          { label: '전체', value: null },
          { label: 'Frontend', value: 1 },
        ]}
        selected={null}
        onChange={onChange}
      />,
    )

    fireEvent.click(screen.getByText('Frontend'))
    expect(onChange).toHaveBeenCalledWith(1)
  })
})
