export function cleanResponseData(data) {
  if (Array.isArray(data)) {
    return data.map(item => cleanResponseData(item))
  }
  
  if (data && typeof data === 'object' && !(data instanceof Date)) {
    const rawData = data.$raw ? data.$raw : { ...data }
    return Object.keys(rawData).reduce((cleaned, key) => {
      if (key.startsWith('_')) return cleaned
      cleaned[key] = cleanResponseData(rawData[key])
      return cleaned
    }, {})
  }
  
  return data
} 