import { toDateInputValue } from './dates';

export const PHONE_INPUT_PATTERN = '(?:\\+84|0)(?:[ .-]?[0-9]){9,10}';
export const ID_NUMBER_INPUT_PATTERN = '(?:[0-9]{9}|[0-9]{10}|[0-9]{12}|[0-9]{10}-[0-9]{3})';
export const DRIVER_LICENSE_INPUT_PATTERN = '[A-Za-z0-9./-]{5,20}';
export const VEHICLE_LICENSE_PLATE_INPUT_PATTERN = '[0-9]{2}[A-Za-z][0-9]?-?[0-9]{3}(?:\\.?[0-9]{2}|[0-9])';
export const EMPLOYEE_CODE_INPUT_PATTERN = '[A-Za-z0-9._-]{2,30}';

export const latestAdultBirthDate = () => {
  const date = new Date();
  date.setFullYear(date.getFullYear() - 18);
  return toDateInputValue(date);
};

export const latestPastDate = () => {
  const date = new Date();
  date.setDate(date.getDate() - 1);
  return toDateInputValue(date);
};

export const isAdultBirthDate = (value) => Boolean(value && value <= latestAdultBirthDate());
