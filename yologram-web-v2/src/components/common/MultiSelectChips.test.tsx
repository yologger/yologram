import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import MultiSelectChips from './MultiSelectChips'

describe('MultiSelectChips', () => {
  it('label/value 객체를 렌더하고 토글 시 value(숫자)를 전달한다', () => {
    const onToggle = vi.fn()
    render(
      <MultiSelectChips
        items={[
          { label: 'Frontend', value: 1 },
          { label: 'Backend', value: 2 },
        ]}
        selected={[1]}
        onToggle={onToggle}
      />,
    )

    fireEvent.click(screen.getByText('Backend'))
    expect(onToggle).toHaveBeenCalledWith(2)
  })

  it('selected에 포함된 칩에 active 클래스를 적용한다', () => {
    render(
      <MultiSelectChips
        items={[{ label: 'Frontend', value: 1 }]}
        selected={[1]}
        onToggle={vi.fn()}
      />,
    )

    expect(screen.getByText('Frontend').className).toMatch(/active/)
  })
})
