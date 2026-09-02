import { useEffect, useRef, useState } from 'react';
import { CalendarDays, ChevronLeft, ChevronRight, X } from 'lucide-react';

const monthLabels = [
  'Tháng 1',
  'Tháng 2',
  'Tháng 3',
  'Tháng 4',
  'Tháng 5',
  'Tháng 6',
  'Tháng 7',
  'Tháng 8',
  'Tháng 9',
  'Tháng 10',
  'Tháng 11',
  'Tháng 12',
];

const weekdayLabels = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];

const pad = (value) => String(value).padStart(2, '0');

const buildChangeEvent = (name, value) => ({
  target: { name, value },
});

const daysInMonth = (year, month) => new Date(year, month, 0).getDate();

const getCurrentParts = () => {
  const now = new Date();
  return {
    year: now.getFullYear(),
    month: now.getMonth() + 1,
    day: now.getDate(),
    hour: now.getHours(),
    minute: now.getMinutes(),
  };
};

const normalizeParts = (parts) => ({
  ...parts,
  day: Math.min(parts.day, daysInMonth(parts.year, parts.month)),
});

const parseParts = (value) => {
  const match = String(value || '').match(/^(\d{4})-(\d{2})-(\d{2})(?:T(\d{2}):(\d{2}))?/);
  if (!match) return null;

  const parts = {
    year: Number(match[1]),
    month: Number(match[2]),
    day: Number(match[3]),
    hour: Number(match[4] || 0),
    minute: Number(match[5] || 0),
  };

  if (
    parts.month < 1
    || parts.month > 12
    || parts.day < 1
    || parts.day > daysInMonth(parts.year, parts.month)
  ) {
    return null;
  }

  return parts;
};

const partsToDate = (parts) =>
  new Date(parts.year, parts.month - 1, parts.day, parts.hour || 0, parts.minute || 0);

const partsToValue = (parts, isDateTime) => {
  const normalized = normalizeParts(parts);
  const date = `${normalized.year}-${pad(normalized.month)}-${pad(normalized.day)}`;

  if (!isDateTime) return date;

  return `${date}T${pad(normalized.hour || 0)}:${pad(normalized.minute || 0)}`;
};

const parseLimitParts = (value, isMax) => {
  const parts = parseParts(value);
  if (!parts) return null;

  const hasTime = String(value || '').includes('T');
  if (isMax && !hasTime) {
    return { ...parts, hour: 23, minute: 59 };
  }

  return parts;
};

const yearRangeStart = (year) => year - (year % 12);

const segmentClassName = (isActive, isInvalid) => [
  'min-h-[68px] min-w-0 overflow-hidden rounded-lg border px-2.5 py-2 text-left transition focus:outline-none focus:ring-4',
  isActive
    ? 'border-emerald-500 bg-emerald-50 text-emerald-900 shadow-sm focus:ring-emerald-100'
    : 'border-slate-200 bg-white text-slate-900 hover:border-emerald-300 hover:bg-emerald-50/60 focus:ring-emerald-100',
  isInvalid ? 'border-rose-300 bg-rose-50 text-rose-800' : '',
].join(' ');

const pickerButtonClassName = (isSelected, isDisabled) => [
  'h-10 min-w-0 whitespace-nowrap rounded-md px-1 text-[13px] font-medium transition focus:outline-none focus:ring-4 focus:ring-emerald-100',
  isSelected ? 'bg-emerald-600 text-white shadow-sm' : 'bg-white text-slate-700 hover:bg-emerald-50 hover:text-emerald-800',
  isDisabled ? 'cursor-not-allowed bg-slate-50 text-slate-300 hover:bg-slate-50 hover:text-slate-300' : '',
].join(' ');

const compactSegmentClassName = (isActive) => [
  'flex min-w-0 overflow-hidden flex-col justify-center px-1.5 text-left transition focus:outline-none focus:ring-2 focus:ring-inset focus:ring-emerald-200',
  isActive
    ? 'bg-emerald-50 text-emerald-900'
    : 'bg-white text-slate-900 hover:bg-slate-50',
].join(' ');

export default function DateField({
  label,
  name,
  value,
  onChange,
  type = 'date',
  required = false,
  disabled = false,
  min,
  max,
  compact = false,
  popupAlign = 'left',
  className = '',
}) {
  const rootRef = useRef(null);
  const isDateTime = type === 'datetime-local';
  const selectedParts = parseParts(value);
  const displayParts = selectedParts || getCurrentParts();
  const minParts = parseLimitParts(min, false);
  const maxParts = parseLimitParts(max, true);
  const minDate = minParts ? partsToDate(minParts) : null;
  const maxDate = maxParts ? partsToDate(maxParts) : null;
  const [activePanel, setActivePanel] = useState(null);
  const [viewParts, setViewParts] = useState(displayParts);
  const [yearStart, setYearStart] = useState(yearRangeStart(displayParts.year));
  const [showRequiredError, setShowRequiredError] = useState(false);
  const isInvalid = required && showRequiredError && !value;

  useEffect(() => {
    if (value) setShowRequiredError(false);
  }, [value]);

  useEffect(() => {
    const handlePointerDown = (event) => {
      if (!rootRef.current?.contains(event.target)) {
        setActivePanel(null);
      }
    };

    document.addEventListener('mousedown', handlePointerDown);
    return () => document.removeEventListener('mousedown', handlePointerDown);
  }, []);

  const isDayDisabled = (parts) => {
    const firstDate = new Date(parts.year, parts.month - 1, parts.day, 0, 0);
    const lastDate = new Date(parts.year, parts.month - 1, parts.day, 23, 59);
    return Boolean((minDate && lastDate < minDate) || (maxDate && firstDate > maxDate));
  };

  const clampToRange = (parts) => {
    const normalized = normalizeParts(parts);
    const date = partsToDate(normalized);

    if (minParts && date < minDate) return minParts;
    if (maxParts && date > maxDate) return maxParts;

    return normalized;
  };

  const setValue = (nextValue) => {
    setShowRequiredError(false);
    onChange(buildChangeEvent(name, nextValue));
  };

  const applyParts = (parts, closePanel = true) => {
    const nextParts = clampToRange(parts);
    setValue(partsToValue(nextParts, isDateTime));
    setViewParts(nextParts);
    if (closePanel) setActivePanel(null);
  };

  const openPanel = (panel) => {
    if (disabled) return;

    const nextParts = selectedParts || displayParts;
    setViewParts(nextParts);
    if (panel === 'year') setYearStart(yearRangeStart(nextParts.year));
    setActivePanel((current) => current === panel ? null : panel);
  };

  const clearValue = () => {
    setValue('');
    setActivePanel(null);
  };

  const moveViewMonth = (offset) => {
    const nextDate = new Date(viewParts.year, viewParts.month - 1 + offset, 1);
    setViewParts((current) => ({
      ...current,
      year: nextDate.getFullYear(),
      month: nextDate.getMonth() + 1,
      day: Math.min(current.day, daysInMonth(nextDate.getFullYear(), nextDate.getMonth() + 1)),
    }));
  };

  const selectMonth = (month) => {
    applyParts({
      ...displayParts,
      month,
      day: Math.min(displayParts.day, daysInMonth(displayParts.year, month)),
    });
  };

  const selectYear = (year) => {
    applyParts({
      ...displayParts,
      year,
      day: Math.min(displayParts.day, daysInMonth(year, displayParts.month)),
    });
  };

  const updateTime = (key, nextValue) => {
    applyParts({ ...displayParts, [key]: Number(nextValue) }, false);
  };

  const firstWeekday = (new Date(viewParts.year, viewParts.month - 1, 1).getDay() + 6) % 7;
  const dayCells = [
    ...Array.from({ length: firstWeekday }, (_, index) => ({ key: `empty-${index}`, day: null })),
    ...Array.from({ length: daysInMonth(viewParts.year, viewParts.month) }, (_, index) => ({
      key: `day-${index + 1}`,
      day: index + 1,
    })),
  ];

  const isMonthDisabled = (year, month) => {
    const firstDate = new Date(year, month - 1, 1, 0, 0);
    const lastDate = new Date(year, month, 0, 23, 59);
    return Boolean((minDate && lastDate < minDate) || (maxDate && firstDate > maxDate));
  };

  const isYearDisabled = (year) => {
    const firstDate = new Date(year, 0, 1, 0, 0);
    const lastDate = new Date(year, 11, 31, 23, 59);
    return Boolean((minDate && lastDate < minDate) || (maxDate && firstDate > maxDate));
  };

  const renderDayPanel = () => (
    <>
      <div className="mb-3 flex items-center justify-between">
        <button type="button" onClick={() => moveViewMonth(-1)} className="rounded-lg p-2 text-slate-500 hover:bg-slate-100">
          <ChevronLeft size={17} />
        </button>
        <p className="text-sm font-semibold text-slate-900">
          {monthLabels[viewParts.month - 1]} / {viewParts.year}
        </p>
        <button type="button" onClick={() => moveViewMonth(1)} className="rounded-lg p-2 text-slate-500 hover:bg-slate-100">
          <ChevronRight size={17} />
        </button>
      </div>
      <div className="grid grid-cols-7 gap-1 text-center text-[11px] font-semibold text-slate-400">
        {weekdayLabels.map((weekday) => (
          <span key={weekday}>{weekday}</span>
        ))}
      </div>
      <div className="mt-1 grid grid-cols-7 gap-1">
        {dayCells.map((cell) => {
          if (!cell.day) return <span key={cell.key} />;

          const parts = { ...displayParts, year: viewParts.year, month: viewParts.month, day: cell.day };
          const isSelected = selectedParts
            && selectedParts.year === viewParts.year
            && selectedParts.month === viewParts.month
            && selectedParts.day === cell.day;
          const isDisabled = isDayDisabled(parts);

          return (
            <button
              key={cell.key}
              type="button"
              disabled={isDisabled}
              onClick={() => applyParts(parts)}
              className={pickerButtonClassName(isSelected, isDisabled)}
            >
              {cell.day}
            </button>
          );
        })}
      </div>
    </>
  );

  const renderMonthPanel = () => (
    <div className="grid grid-cols-3 gap-2">
      {monthLabels.map((month, index) => {
        const monthNumber = index + 1;
        const isSelected = selectedParts?.month === monthNumber && selectedParts?.year === displayParts.year;
        const isDisabled = isMonthDisabled(displayParts.year, monthNumber);

        return (
          <button
            key={month}
            type="button"
            disabled={isDisabled}
            onClick={() => selectMonth(monthNumber)}
            className={pickerButtonClassName(isSelected, isDisabled)}
          >
            {month}
          </button>
        );
      })}
    </div>
  );

  const renderYearPanel = () => (
    <>
      <div className="mb-3 flex items-center justify-between">
        <button type="button" onClick={() => setYearStart((current) => current - 12)} className="rounded-lg p-2 text-slate-500 hover:bg-slate-100">
          <ChevronLeft size={17} />
        </button>
        <p className="text-sm font-semibold text-slate-900">
          {yearStart} - {yearStart + 11}
        </p>
        <button type="button" onClick={() => setYearStart((current) => current + 12)} className="rounded-lg p-2 text-slate-500 hover:bg-slate-100">
          <ChevronRight size={17} />
        </button>
      </div>
      <div className="grid grid-cols-4 gap-2">
        {Array.from({ length: 12 }, (_, index) => yearStart + index).map((year) => {
          const isSelected = selectedParts?.year === year;
          const isDisabled = isYearDisabled(year);

          return (
            <button
              key={year}
              type="button"
              disabled={isDisabled}
              onClick={() => selectYear(year)}
              className={pickerButtonClassName(isSelected, isDisabled)}
            >
              {year}
            </button>
          );
        })}
      </div>
    </>
  );

  return (
    <div ref={rootRef} className={`relative ${compact ? 'space-y-1' : 'space-y-1.5'} ${className}`}>
      <div className={`flex items-center justify-between gap-2 ${compact ? 'min-h-6' : 'min-h-8'}`}>
        <label title={label} className={`block min-w-0 truncate font-medium ${compact ? 'text-xs text-slate-600' : 'text-sm text-slate-700'}`}>
          {label}
        </label>
        <div className="flex items-center gap-1">
          {!required && value && (
            <button
              type="button"
              onClick={clearValue}
              disabled={disabled}
              title={`Xóa ${label.toLowerCase()}`}
              aria-label={`Xóa ${label.toLowerCase()}`}
              className={`inline-flex shrink-0 items-center justify-center border border-slate-200 bg-white text-slate-500 transition hover:border-rose-200 hover:bg-rose-50 hover:text-rose-600 disabled:cursor-not-allowed disabled:opacity-50 ${compact ? 'h-6 w-6 rounded-md' : 'h-8 w-8 rounded-lg'}`}
            >
              <X size={compact ? 13 : 15} />
            </button>
          )}
        </div>
      </div>

      <input
        required={required}
        tabIndex={-1}
        aria-hidden="true"
        name={name}
        value={value || ''}
        autoComplete="off"
        onChange={() => {}}
        onInvalid={(event) => {
          event.preventDefault();
          setShowRequiredError(true);
          setViewParts(selectedParts || displayParts);
          setActivePanel('day');
        }}
        className="pointer-events-none absolute h-px w-px opacity-0"
      />

      {compact ? (
        <div className={`grid h-11 grid-cols-[28px_minmax(40px,0.8fr)_1px_minmax(48px,1fr)_1px_minmax(64px,1.2fr)] overflow-hidden rounded-lg border bg-white shadow-sm transition focus-within:ring-2 focus-within:ring-emerald-100 ${
          isInvalid ? 'border-rose-300' : activePanel ? 'border-emerald-400' : 'border-slate-300'
        } ${disabled ? 'opacity-60' : ''}`}>
          <span className="flex min-w-0 items-center justify-center bg-slate-50 text-slate-500" aria-hidden="true">
            <CalendarDays size={15} />
          </span>
          <button
            type="button"
            disabled={disabled}
            onClick={() => openPanel('day')}
            title={`Chọn ngày cho ${label.toLowerCase()}`}
            className={compactSegmentClassName(activePanel === 'day')}
          >
            <span className="block w-full truncate text-[9px] font-medium uppercase leading-none text-slate-500">Ngày</span>
            <span className="block w-full truncate text-[13px] font-semibold leading-5 tabular-nums">{selectedParts ? pad(selectedParts.day) : '--'}</span>
          </button>
          <span className="my-1.5 w-full bg-slate-200" aria-hidden="true" />
          <button
            type="button"
            disabled={disabled}
            onClick={() => openPanel('month')}
            title={`Chọn tháng cho ${label.toLowerCase()}`}
            className={compactSegmentClassName(activePanel === 'month')}
          >
            <span className="block w-full truncate text-[9px] font-medium uppercase leading-none text-slate-500">Tháng</span>
            <span className="block w-full truncate text-[13px] font-semibold leading-5 tabular-nums">{selectedParts ? pad(selectedParts.month) : '--'}</span>
          </button>
          <span className="my-1.5 w-full bg-slate-200" aria-hidden="true" />
          <button
            type="button"
            disabled={disabled}
            onClick={() => openPanel('year')}
            title={`Chọn năm cho ${label.toLowerCase()}`}
            className={compactSegmentClassName(activePanel === 'year')}
          >
            <span className="block w-full truncate text-[9px] font-medium uppercase leading-none text-slate-500">Năm</span>
            <span className="block w-full truncate text-[13px] font-semibold leading-5 tabular-nums">{selectedParts ? selectedParts.year : '----'}</span>
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-[minmax(0,0.85fr)_minmax(0,1fr)_minmax(84px,1.25fr)] gap-2">
          <button
            type="button"
            disabled={disabled}
            onClick={() => openPanel('day')}
            className={segmentClassName(activePanel === 'day', isInvalid)}
          >
            <span className="block truncate text-[11px] font-medium uppercase text-slate-500">Ngày</span>
            <span className="mt-1 block truncate text-xl font-semibold tracking-normal">
              {selectedParts ? pad(selectedParts.day) : '--'}
            </span>
          </button>
          <button
            type="button"
            disabled={disabled}
            onClick={() => openPanel('month')}
            className={segmentClassName(activePanel === 'month', isInvalid)}
          >
            <span className="block truncate text-[11px] font-medium uppercase text-slate-500">Tháng</span>
            <span className="mt-1 block truncate text-xl font-semibold tracking-normal">
              {selectedParts ? pad(selectedParts.month) : '--'}
            </span>
          </button>
          <button
            type="button"
            disabled={disabled}
            onClick={() => openPanel('year')}
            className={segmentClassName(activePanel === 'year', isInvalid)}
          >
            <span className="block truncate text-[11px] font-medium uppercase text-slate-500">Năm</span>
            <span className="mt-1 block truncate text-xl font-semibold tracking-normal">
              {selectedParts ? selectedParts.year : '----'}
            </span>
          </button>
        </div>
      )}

      {isDateTime && (
        <div className="grid grid-cols-2 gap-2">
          <label className="min-w-0 rounded-lg border border-slate-200 bg-white px-3 py-2">
            <span className="text-xs font-medium uppercase text-slate-500">Giờ</span>
            <select
              value={pad(displayParts.hour || 0)}
              onChange={(event) => updateTime('hour', event.target.value)}
              disabled={disabled}
              className="mt-1 block w-full min-w-0 bg-transparent text-lg font-semibold text-slate-900 outline-none"
            >
              {Array.from({ length: 24 }, (_, hour) => (
                <option key={hour} value={pad(hour)}>{pad(hour)}</option>
              ))}
            </select>
          </label>
          <label className="min-w-0 rounded-lg border border-slate-200 bg-white px-3 py-2">
            <span className="text-xs font-medium uppercase text-slate-500">Phút</span>
            <select
              value={pad(displayParts.minute || 0)}
              onChange={(event) => updateTime('minute', event.target.value)}
              disabled={disabled}
              className="mt-1 block w-full min-w-0 bg-transparent text-lg font-semibold text-slate-900 outline-none"
            >
              {Array.from({ length: 60 }, (_, minute) => (
                <option key={minute} value={pad(minute)}>{pad(minute)}</option>
              ))}
            </select>
          </label>
        </div>
      )}

      {isInvalid && (
        <p className="text-xs font-medium text-rose-600">Vui lòng chọn {label.toLowerCase()}.</p>
      )}

      {activePanel && (
        <div className={`absolute top-full z-50 mt-2 rounded-lg border border-slate-200 bg-white p-3 shadow-xl ${
          compact
            ? `${popupAlign === 'right' ? 'right-0' : 'left-0'} w-[min(320px,calc(100vw-2rem))]`
            : 'left-0 right-0'
        }`}>
          {activePanel === 'day' && renderDayPanel()}
          {activePanel === 'month' && renderMonthPanel()}
          {activePanel === 'year' && renderYearPanel()}
        </div>
      )}
    </div>
  );
}
