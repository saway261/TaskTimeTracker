export function normalizeTagName(name: string) {
  return name.trim().normalize('NFKC').toLowerCase()
}
