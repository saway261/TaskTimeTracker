import { describe, expect, it } from 'vitest'
import { normalizeTagName } from './tagName'

describe('normalizeTagName', () => {
  it('前後空白・大文字小文字・全角半角を同じ名前として正規化する', () => {
    expect(normalizeTagName(' ＡＰＩ ')).toBe('api')
    expect(normalizeTagName('API')).toBe('api')
    expect(normalizeTagName('ﾁｮｳｻ')).toBe('チョウサ')
  })
})
