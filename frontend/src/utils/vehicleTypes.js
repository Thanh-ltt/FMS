export const vehicleTypeGroups = [
  {
    label: 'Xe chở hàng cỡ nhỏ',
    options: [
      { value: 'CARGO_TRUCK', label: 'Xe tải chở hàng', capacityRange: { min: 0.5, max: 30 } },
      { value: 'CARGO_VAN', label: 'Xe van chở hàng', capacityRange: { min: 0.3, max: 1.5 } },
      { value: 'PICKUP_TRUCK', label: 'Xe bán tải chở hàng', capacityRange: { min: 0.5, max: 1.5 } },
      { value: 'LIGHT_DUTY_TRUCK', label: 'Xe tải nhẹ', capacityRange: { min: 0.5, max: 3.5 } },
    ],
  },
  {
    label: 'Xe tải thùng',
    options: [
      { value: 'BOX_TRUCK', label: 'Xe tải thùng kín', capacityRange: { min: 0.5, max: 15 } },
      { value: 'TARPAULIN_TRUCK', label: 'Xe tải mui bạt', capacityRange: { min: 1, max: 18 } },
      { value: 'FLATBED_TRUCK', label: 'Xe tải thùng lửng / sàn', capacityRange: { min: 1, max: 25 } },
      { value: 'REFRIGERATED_TRUCK', label: 'Xe tải đông lạnh', capacityRange: { min: 1, max: 15 } },
      { value: 'HEAVY_DUTY_TRUCK', label: 'Xe tải nặng', capacityRange: { min: 8, max: 30 } },
    ],
  },
  {
    label: 'Xe vận tải chuyên dụng',
    options: [
      { value: 'DUMP_TRUCK', label: 'Xe ben', capacityRange: { min: 5, max: 30 } },
      { value: 'TANKER_TRUCK', label: 'Xe bồn', capacityRange: { min: 5, max: 30 } },
      { value: 'BULK_CARRIER_TRUCK', label: 'Xe chở hàng rời', capacityRange: { min: 5, max: 30 } },
      { value: 'CONTAINER_TRACTOR', label: 'Xe đầu kéo container', capacityRange: { min: 20, max: 40 } },
      { value: 'SPECIAL_PURPOSE_TRUCK', label: 'Xe vận tải chuyên dụng', capacityRange: { min: 1, max: 40 } },
      { value: 'OTHER', label: 'Khác' },
    ],
  },
];

export const vehicleTypes = vehicleTypeGroups.flatMap((group) => group.options);

const vehicleTypeLabels = new Map(vehicleTypes.map((type) => [type.value, type.label]));
const vehicleTypeDetails = new Map(vehicleTypes.map((type) => [type.value, type]));
const capacityFormatter = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 2 });

export const isKnownVehicleType = (value) => vehicleTypeLabels.has(value);

export const vehicleTypeLabel = (value) => vehicleTypeLabels.get(value) || value || '-';

export const vehicleTypeCapacityRange = (value) => {
  const capacityRange = vehicleTypeDetails.get(value)?.capacityRange;
  if (!capacityRange) return '';

  return `${capacityFormatter.format(capacityRange.min)}–${capacityFormatter.format(capacityRange.max)} tấn`;
};

export const vehicleTypeLabelWithRange = (value) => {
  const label = vehicleTypeLabel(value);
  const capacityRange = vehicleTypeCapacityRange(value);
  return capacityRange ? `${label} (${capacityRange})` : label;
};

export const isVehicleCapacityOutsideRange = (value, capacity) => {
  const capacityRange = vehicleTypeDetails.get(value)?.capacityRange;
  const numericCapacity = Number(capacity);

  if (!capacityRange || capacity === '' || !Number.isFinite(numericCapacity)) return false;
  return numericCapacity < capacityRange.min || numericCapacity > capacityRange.max;
};

export const vehicleTypeWithCapacity = (value, capacity) => {
  const label = vehicleTypeLabel(value);
  if (capacity === '' || capacity === null || capacity === undefined) return label;

  const numericCapacity = Number(capacity);
  if (!Number.isFinite(numericCapacity) || numericCapacity <= 0) return label;

  return `${label} · ${capacityFormatter.format(numericCapacity)} tấn`;
};
