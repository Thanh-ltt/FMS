const pad = (value) => String(value).padStart(2, '0');

const toDate = (value) => {
  if (!value) return null;
  const date = value instanceof Date ? value : new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
};

export const toDateInputValue = (value = new Date()) => {
  const date = toDate(value);
  if (!date) return '';

  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
};

export const toDateTimeLocalInputValue = (value = new Date()) => {
  const date = toDate(value);
  if (!date) return '';

  return [
    toDateInputValue(date),
    `${pad(date.getHours())}:${pad(date.getMinutes())}`,
  ].join('T');
};

export const formatDate = (value) => {
  if (!value) return '-';

  const match = String(value).match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (match) {
    return `${match[3]}/${match[2]}/${match[1]}`;
  }

  const date = toDate(value);
  return date ? date.toLocaleDateString('vi-VN') : String(value);
};

export const formatDateTime = (value) => {
  if (!value) return '-';

  const match = String(value).match(/^(\d{4})-(\d{2})-(\d{2})[T\s](\d{2}):(\d{2})/);
  if (match) {
    return `${match[3]}/${match[2]}/${match[1]} ${match[4]}:${match[5]}`;
  }

  const date = toDate(value);
  return date
    ? date.toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
    : String(value);
};
