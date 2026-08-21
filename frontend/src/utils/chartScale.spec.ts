import { describe, expect, it } from 'vitest'
import {
  createChartScale,
  createLinearDomainScale,
  evenlySpacedValues,
  factorBandPolygon,
  factorLineSegment,
  linearTicks,
  logarithmicTicks,
  mapScaleValue,
} from './chartScale'

describe('chartScale', () => {
  it('線形軸の見やすい目盛りと座標を算出する', () => {
    expect(linearTicks(83)).toEqual([0, 20, 40, 60, 80, 100])
    const scale = createChartScale(83, 10, 210, 'linear')

    expect(scale.domainMax).toBe(100)
    expect(mapScaleValue(0, scale)).toBe(10)
    expect(mapScaleValue(50, scale)).toBe(110)
    expect(mapScaleValue(100, scale)).toBe(210)
  })

  it('任意の最小値と最大値を持つ線形軸を算出する', () => {
    const scale = createLinearDomainScale(0.82, 1.18, 100, 0)

    expect(scale.domainMin).toBeLessThanOrEqual(0.82)
    expect(scale.domainMax).toBeGreaterThanOrEqual(1.18)
    expect(scale.ticks).toContain(1)
    expect(mapScaleValue(scale.domainMin, scale)).toBe(100)
    expect(mapScaleValue(scale.domainMax, scale)).toBe(0)
  })

  it('対数軸を10の累乗間で写像する', () => {
    expect(logarithmicTicks(100)).toEqual([1, 2, 5, 10, 20, 50, 100])
    const scale = createChartScale(100, 0, 200, 'log')

    expect(mapScaleValue(1, scale)).toBe(0)
    expect(mapScaleValue(10, scale)).toBe(100)
    expect(mapScaleValue(100, scale)).toBe(200)
  })

  it('線形軸で係数線としきい値帯を描画範囲内へ切り詰める', () => {
    const xScale = createChartScale(100, 0, 100, 'linear')
    const yScale = createChartScale(100, 100, 0, 'linear')
    const reference = factorLineSegment(1, xScale, yScale)
    const band = factorBandPolygon(0.9, 1.1, xScale, yScale)

    expect(reference?.start).toEqual({ x: 0, y: 100 })
    expect(reference?.end).toEqual({ x: 100, y: 0 })
    expect(band).toHaveLength(4)
    expect(band.every(({ x, y }) => x >= 0 && x <= 100 && y >= 0 && y <= 100)).toBe(true)
  })

  it('対数軸でも比例線を平行な直線として配置する', () => {
    const xScale = createChartScale(100, 0, 200, 'log')
    const yScale = createChartScale(100, 200, 0, 'log')
    const lower = factorLineSegment(0.9, xScale, yScale)
    const upper = factorLineSegment(1.1, xScale, yScale)

    expect(lower).not.toBeNull()
    expect(upper).not.toBeNull()
    const lowerSlope = (lower!.end.y - lower!.start.y) / (lower!.end.x - lower!.start.x)
    const upperSlope = (upper!.end.y - upper!.start.y) / (upper!.end.x - upper!.start.x)
    expect(lowerSlope).toBeCloseTo(upperSlope)
  })

  it('行位置を指定範囲へ均等配置する', () => {
    expect(evenlySpacedValues(3, 10, 30)).toEqual([10, 20, 30])
  })
})
