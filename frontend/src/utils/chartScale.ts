export type ChartScaleMode = 'linear' | 'log'

export interface ChartScale {
  mode: ChartScaleMode
  domainMin: number
  domainMax: number
  rangeStart: number
  rangeEnd: number
  ticks: number[]
}

export interface SvgPoint {
  x: number
  y: number
}

export interface SvgLineSegment {
  start: SvgPoint
  end: SvgPoint
}

function niceStep(maxValue: number, targetTickCount: number): number {
  const roughStep = maxValue / Math.max(1, targetTickCount)
  const magnitude = 10 ** Math.floor(Math.log10(roughStep || 1))
  const normalized = roughStep / magnitude
  const multiplier = normalized <= 1 ? 1 : normalized <= 2 ? 2 : normalized <= 5 ? 5 : 10
  return multiplier * magnitude
}

export function linearTicks(maxValue: number, targetTickCount = 5): number[] {
  const safeMax = Math.max(1, maxValue)
  const step = niceStep(safeMax, targetTickCount)
  const domainMax = Math.ceil(safeMax / step) * step
  const tickCount = Math.round(domainMax / step)
  return Array.from({ length: tickCount + 1 }, (_, index) => index * step)
}

export function logarithmicTicks(maxValue: number): number[] {
  const domainMax = 10 ** Math.ceil(Math.log10(Math.max(10, maxValue)))
  const ticks: number[] = []

  for (let power = 0; 10 ** power <= domainMax; power += 1) {
    for (const multiplier of [1, 2, 5]) {
      const value = multiplier * 10 ** power
      if (value <= domainMax) ticks.push(value)
    }
  }

  if (ticks[ticks.length - 1] !== domainMax) ticks.push(domainMax)
  return ticks
}

export function createChartScale(
  maxValue: number,
  rangeStart: number,
  rangeEnd: number,
  mode: ChartScaleMode,
): ChartScale {
  const ticks = mode === 'linear' ? linearTicks(maxValue) : logarithmicTicks(maxValue)
  return {
    mode,
    domainMin: mode === 'linear' ? 0 : 1,
    domainMax: ticks[ticks.length - 1] ?? 1,
    rangeStart,
    rangeEnd,
    ticks,
  }
}

export function createLinearDomainScale(
  minValue: number,
  maxValue: number,
  rangeStart: number,
  rangeEnd: number,
  targetTickCount = 5,
): ChartScale {
  const safeMin = Number.isFinite(minValue) ? minValue : 0
  const safeMax = Number.isFinite(maxValue) ? maxValue : safeMin + 1
  const domainRange = Math.max(Math.abs(safeMax - safeMin), Math.abs(safeMax) * 0.1, 0.1)
  const step = niceStep(domainRange, targetTickCount)
  const domainMin = Math.floor(safeMin / step) * step
  let domainMax = Math.ceil(safeMax / step) * step
  if (domainMax <= domainMin) domainMax = domainMin + step
  const tickCount = Math.round((domainMax - domainMin) / step)
  const ticks = Array.from({ length: tickCount + 1 }, (_, index) =>
    Number((domainMin + index * step).toPrecision(12)),
  )

  return {
    mode: 'linear',
    domainMin,
    domainMax,
    rangeStart,
    rangeEnd,
    ticks,
  }
}

export function mapScaleValue(value: number, scale: ChartScale): number {
  const clamped = Math.min(scale.domainMax, Math.max(scale.domainMin, value))
  const ratio =
    scale.mode === 'linear'
      ? (clamped - scale.domainMin) / (scale.domainMax - scale.domainMin || 1)
      : (Math.log10(clamped) - Math.log10(scale.domainMin)) /
        (Math.log10(scale.domainMax) - Math.log10(scale.domainMin) || 1)
  return scale.rangeStart + ratio * (scale.rangeEnd - scale.rangeStart)
}

export function mapSvgPoint(
  xValue: number,
  yValue: number,
  xScale: ChartScale,
  yScale: ChartScale,
): SvgPoint {
  return {
    x: mapScaleValue(xValue, xScale),
    y: mapScaleValue(yValue, yScale),
  }
}

export function factorLineSegment(
  factor: number,
  xScale: ChartScale,
  yScale: ChartScale,
): SvgLineSegment | null {
  if (!Number.isFinite(factor) || factor < 0) return null
  if (factor === 0) {
    if (yScale.mode === 'log') return null
    return {
      start: mapSvgPoint(xScale.domainMin, 0, xScale, yScale),
      end: mapSvgPoint(xScale.domainMax, 0, xScale, yScale),
    }
  }

  const xStart = Math.max(xScale.domainMin, yScale.domainMin / factor)
  const xEnd = Math.min(xScale.domainMax, yScale.domainMax / factor)
  if (xStart > xEnd) return null

  return {
    start: mapSvgPoint(xStart, factor * xStart, xScale, yScale),
    end: mapSvgPoint(xEnd, factor * xEnd, xScale, yScale),
  }
}

export function factorBandPolygon(
  lowerFactor: number,
  upperFactor: number,
  xScale: ChartScale,
  yScale: ChartScale,
): SvgPoint[] {
  const lower =
    lowerFactor <= 0 && yScale.mode === 'log'
      ? {
          start: mapSvgPoint(xScale.domainMin, yScale.domainMin, xScale, yScale),
          end: mapSvgPoint(xScale.domainMax, yScale.domainMin, xScale, yScale),
        }
      : factorLineSegment(lowerFactor, xScale, yScale)
  const upper = factorLineSegment(upperFactor, xScale, yScale)
  if (!lower || !upper) return []
  return [lower.start, lower.end, upper.end, upper.start]
}

export function svgPoints(points: SvgPoint[]): string {
  return points.map(({ x, y }) => `${x},${y}`).join(' ')
}

export function evenlySpacedValues(count: number, start: number, end: number): number[] {
  if (count <= 0) return []
  if (count === 1) return [(start + end) / 2]
  const step = (end - start) / (count - 1)
  return Array.from({ length: count }, (_, index) => start + index * step)
}
