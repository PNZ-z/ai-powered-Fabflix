// Money helpers keep every total in integer cents so the JSON number prices
// from the server never accumulate floating-point drift. The server is the
// only source of prices; these helpers format what it already returned.
function toCents(amount) {
  return Math.round(Number(amount) * 100)
}

function formatCents(cents) {
  return `$${(cents / 100).toFixed(2)}`
}

export { formatCents, toCents }
