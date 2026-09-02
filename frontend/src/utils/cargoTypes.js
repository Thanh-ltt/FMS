export const cargoTypes = [
  { value: 'DRY', label: 'Hàng khô', defaultRate: 20000 },
  { value: 'COLD', label: 'Hàng lạnh/đông lạnh', defaultRate: 32000 },
  { value: 'FRAGILE', label: 'Hàng dễ vỡ', defaultRate: 28000 },
  { value: 'DANGEROUS', label: 'Hàng nguy hiểm', defaultRate: 45000 },
  { value: 'CONSTRUCTION', label: 'Vật liệu xây dựng', defaultRate: 18000 },
  { value: 'MACHINERY', label: 'Máy móc/thiết bị', defaultRate: 30000 },
  { value: 'AGRICULTURE', label: 'Nông sản/thực phẩm', defaultRate: 22000 },
  { value: 'OVERSIZED', label: 'Hàng quá khổ/quá tải', defaultRate: 50000 },
  { value: 'OTHER', label: 'Khác', defaultRate: 20000 },
];

const cargoTypeLabels = cargoTypes.reduce((labels, type) => {
  labels[type.value] = type.label;
  return labels;
}, {});

export const cargoTypeLabel = (value) => cargoTypeLabels[value] || value || '-';
