export const normalizeText = (value) => String(value ?? '')
  .normalize('NFD')
  .replace(/[\u0300-\u036f]/g, '')
  .replace(/đ/g, 'd')
  .replace(/Đ/g, 'D')
  .toLowerCase()
  .trim();

export const matchSearchText = (text, query) => {
  if (!query) return true;
  const normalizedQuery = normalizeText(query);
  if (!normalizedQuery) return true;
  return normalizeText(text).includes(normalizedQuery);
};
