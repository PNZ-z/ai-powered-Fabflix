function movieListUrl(filters) {
  const params = new URLSearchParams()
  Object.entries(filters).forEach(([name, value]) => {
    const trimmed = value.trim()
    if (trimmed) params.set(name, trimmed)
  })
  params.set('pageNumber', '0')
  params.set('pageSize', '10')
  return `/movies?${params}`
}

function fullTextSearchUrl(query) {
  const params = new URLSearchParams({
    query: query.trim(),
    mode: 'fulltext',
    pageNumber: '0',
    pageSize: '10',
  })
  return `/movies?${params}`
}

export { fullTextSearchUrl, movieListUrl }
