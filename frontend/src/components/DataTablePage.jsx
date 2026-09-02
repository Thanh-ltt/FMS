import { useCallback, useContext, useEffect, useMemo, useState } from 'react';
import toast from 'react-hot-toast';
import { Eye, ListFilter, Plus, RefreshCw, RotateCcw, Trash2 } from 'lucide-react';
import { AuthContext } from '../context/auth-context';
import api from '../services/api';
import DateField from './DateField';
import Modal from './Modal';

import SearchableSelect from './SearchableSelect';

const getRawValue = (row, key) => key
  .split('.')
  .reduce((current, part) => current?.[part], row);

const getValue = (row, key) => getRawValue(row, key) ?? '-';

const getRowKey = (row, index) => row.id || row.invoiceNumber || row.contractCode || `${index}`;

const normalizeText = (value) => String(value ?? '')
  .normalize('NFD')
  .replace(/[\u0300-\u036f]/g, '')
  .replace(/đ/g, 'd')
  .replace(/Đ/g, 'D')
  .toLowerCase()
  .trim();

const getFilterRowValue = (row, filter) => filter.getValue
  ? filter.getValue(row)
  : getRawValue(row, filter.field || filter.key);

const matchesFilter = (row, filter, selectedValue, filterValues) => {
  if (selectedValue === '' || selectedValue === null || selectedValue === undefined) return true;
  if (filter.match) return filter.match(row, selectedValue, filterValues);

  const rowValue = getFilterRowValue(row, filter);

  if (filter.type === 'text') {
    return normalizeText(rowValue).includes(normalizeText(selectedValue));
  }

  if (filter.type === 'date') {
    if (!rowValue) return false;
    const rowDate = String(rowValue).slice(0, 10);
    if (filter.operator === 'gte') return rowDate >= selectedValue;
    if (filter.operator === 'lte') return rowDate <= selectedValue;
    return rowDate === selectedValue;
  }

  if (filter.type === 'number') {
    const rowNumber = Number(rowValue);
    const filterNumber = Number(selectedValue);
    if (!Number.isFinite(rowNumber) || !Number.isFinite(filterNumber)) return false;
    if (filter.operator === 'gte') return rowNumber >= filterNumber;
    if (filter.operator === 'lte') return rowNumber <= filterNumber;
    return rowNumber === filterNumber;
  }

  if (Array.isArray(rowValue)) {
    return rowValue.some((value) => String(value) === String(selectedValue));
  }

  return String(rowValue ?? '') === String(selectedValue);
};

const normalizeOption = (option) => {
  if (option && typeof option === 'object') {
    return {
      value: String(option.value ?? ''),
      label: String(option.label ?? option.value ?? ''),
    };
  }

  return { value: String(option ?? ''), label: String(option ?? '') };
};

const getFilterOptions = (filter, rows) => {
  const configuredOptions = typeof filter.options === 'function'
    ? filter.options(rows)
    : filter.options;

  if (configuredOptions) return configuredOptions.map(normalizeOption);
  if (!filter.deriveOptions) return [];

  const options = new Map();
  rows.forEach((row) => {
    const rowValue = getFilterRowValue(row, filter);
    const values = Array.isArray(rowValue) ? rowValue : [rowValue];

    values.forEach((value) => {
      if (value === '' || value === null || value === undefined) return;
      const normalizedValue = String(value);
      const label = filter.getOptionLabel
        ? filter.getOptionLabel(row, value)
        : normalizedValue;
      if (!options.has(normalizedValue)) options.set(normalizedValue, String(label || normalizedValue));
    });
  });

  return Array.from(options, ([value, label]) => ({ value, label }))
    .sort((left, right) => left.label.localeCompare(right.label, 'vi'));
};

export default function DataTablePage({
  title,
  description,
  endpoint,
  columns,
  primaryColumns,
  emptyText,
  onCreate,
  rowActions,
  actionLabel = 'Thao tác',
  headerActions,
  showDetails = true,
  detailTitle,
  deleteEndpoint,
  deleteLabel,
  deleteSuccessMessage = 'Đã xóa dữ liệu',
  onDeleteSuccess,
  canDeleteRow = () => true,
  filters = [],
  filterGridClassName = 'grid-cols-[repeat(auto-fit,minmax(min(100%,200px),1fr))]',
}) {
  const { user } = useContext(AuthContext);
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filterValues, setFilterValues] = useState(() => Object.fromEntries(
    filters.map((filter) => [filter.key, filter.defaultValue ?? ''])
  ));
  const [selectedDetailRow, setSelectedDetailRow] = useState(null);
  const [deletingRowKey, setDeletingRowKey] = useState(null);
  const primaryColumnSet = useMemo(() => new Set(primaryColumns || []), [primaryColumns]);
  const visibleColumns = useMemo(() => {
    if (!primaryColumns?.length) return columns;

    const configuredColumns = columns.filter((column) => primaryColumnSet.has(column.key));
    return configuredColumns.length > 0 ? configuredColumns : columns;
  }, [columns, primaryColumnSet, primaryColumns]);
  const canDelete = Boolean(deleteEndpoint) && user?.role === 'ADMIN';
  const hasActionColumn = showDetails || Boolean(rowActions) || canDelete;
  const columnCount = visibleColumns.length + (hasActionColumn ? 1 : 0);

  const loadRows = useCallback(async () => {
    setLoading(true);
    try {
      const res = await api.get(endpoint);
      setRows(res.data.result || res.data || []);
    } catch (error) {
      toast.error(error.response?.data?.message || `Không thể tải dữ liệu ${title.toLowerCase()}`);
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [endpoint, title]);

  useEffect(() => {
    loadRows();
  }, [loadRows]);

  const filteredRows = useMemo(() => rows.filter((row) =>
    filters.every((filter) => matchesFilter(row, filter, filterValues[filter.key], filterValues))
  ), [filterValues, filters, rows]);

  const activeFilterCount = useMemo(() => Object.values(filterValues)
    .filter((value) => value !== '' && value !== null && value !== undefined).length, [filterValues]);

  const renderCell = (row, column) => column.render ? column.render(row) : getValue(row, column.key);
  const renderDetailCell = (row, column) => column.detailRender
    ? column.detailRender(row)
    : renderCell(row, column);

  const handleFilterChange = (event) => {
    const { name, value } = event.target;
    setFilterValues((current) => ({ ...current, [name]: value }));
  };

  const clearFilters = () => {
    setFilterValues(Object.fromEntries(filters.map((filter) => [filter.key, filter.defaultValue ?? ''])));
  };

  const renderFilter = (filter) => {
    const value = filterValues[filter.key] ?? '';

    if (filter.type === 'date') {
      return (
        <DateField
          compact
          label={filter.label}
          name={filter.key}
          value={value}
          min={filter.minFilterKey ? filterValues[filter.minFilterKey] || undefined : filter.min}
          max={filter.maxFilterKey ? filterValues[filter.maxFilterKey] || undefined : filter.max}
          popupAlign={filter.popupAlign || 'left'}
          onChange={handleFilterChange}
        />
      );
    }

    if (filter.type === 'searchable-select' || (filter.type === 'select' && filter.searchable)) {
      const options = getFilterOptions(filter, rows);
      return (
        <SearchableSelect
          label={filter.label}
          name={filter.key}
          value={value}
          options={options}
          placeholder={filter.placeholder}
          allLabel={filter.allLabel}
          popupAlign={filter.popupAlign || 'left'}
          onChange={handleFilterChange}
        />
      );
    }

    if (filter.type === 'select') {
      const options = getFilterOptions(filter, rows);
      return (
        <label className="block min-w-0">
          <span className="flex min-h-6 items-center text-xs font-medium text-slate-600">{filter.label}</span>
          <select
            name={filter.key}
            value={value}
            onChange={handleFilterChange}
            className="mt-1 block h-11 w-full min-w-0 rounded-md border border-slate-300 bg-white px-3 text-sm shadow-sm outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100"
          >
            <option value="">{filter.allLabel || `Tất cả ${filter.label.toLowerCase()}`}</option>
            {options.map((option) => (
              <option key={option.value} value={option.value}>{option.label}</option>
            ))}
          </select>
        </label>
      );
    }

    return (
      <label className="block min-w-0">
        <span className="flex min-h-6 items-center text-xs font-medium text-slate-600">{filter.label}</span>
        <input
          type={filter.type === 'number' ? 'number' : 'text'}
          name={filter.key}
          value={value}
          min={filter.min}
          max={filter.max}
          step={filter.step}
          placeholder={filter.placeholder || ''}
          onChange={handleFilterChange}
          className="mt-1 block h-11 w-full min-w-0 rounded-md border border-slate-300 bg-white px-3 text-sm shadow-sm outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100"
        />
      </label>
    );
  };

  const handleDelete = async (row, rowKey) => {
    const label = typeof deleteLabel === 'function'
      ? deleteLabel(row)
      : deleteLabel || `bản ghi "${row.name || row.id || rowKey}"`;
    if (!window.confirm(`Xóa ${label}? Thao tác này không thể hoàn tác.`)) {
      return;
    }

    const targetEndpoint = typeof deleteEndpoint === 'function'
      ? deleteEndpoint(row)
      : `${deleteEndpoint.replace(/\/$/, '')}/${row.id}`;

    setDeletingRowKey(rowKey);
    try {
      await api.delete(targetEndpoint);
      toast.success(deleteSuccessMessage);
      if (selectedDetailRow?.id === row.id) setSelectedDetailRow(null);
      await Promise.allSettled([
        loadRows(),
        onDeleteSuccess ? Promise.resolve(onDeleteSuccess(row)) : Promise.resolve(),
      ]);
    } catch (error) {
      const errorCode = error.response?.data?.code;
      const protectedMessage = errorCode === 1051
        ? 'Không thể xóa vì bản ghi đã có dữ liệu liên quan.'
        : errorCode === 1052
          ? 'Không thể xóa hóa đơn đã thanh toán.'
          : errorCode === 1059
            ? 'Không thể xóa phiếu cọc đã được cấn hoặc đã hoàn tiền.'
          : null;
      toast.error(protectedMessage || error.response?.data?.message || 'Không thể xóa dữ liệu');
    } finally {
      setDeletingRowKey(null);
    }
  };

  const resolvedDetailTitle = selectedDetailRow
    ? (typeof detailTitle === 'function'
        ? detailTitle(selectedDetailRow)
        : detailTitle || `Chi tiết ${title.replace(/^Quản lý\s+/i, '').toLowerCase()}`)
    : '';

  return (
    <>
      <section className="space-y-5">
        <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div>
            <h1 className="text-2xl font-semibold tracking-normal text-slate-950">{title}</h1>
            <p className="mt-1 max-w-2xl text-sm text-slate-500">{description}</p>
          </div>
          <div className="flex flex-wrap gap-2">
            {headerActions}
            <button
              type="button"
              onClick={loadRows}
              className="inline-flex h-10 items-center justify-center gap-2 rounded-lg border border-slate-300 bg-white px-3 text-sm font-medium text-slate-700 hover:bg-slate-100"
            >
              <RefreshCw size={16} />
              Tải lại
            </button>
            {onCreate && (
              <button
                type="button"
                onClick={onCreate}
                className="inline-flex h-10 items-center justify-center gap-2 rounded-lg bg-emerald-600 px-4 text-sm font-medium text-white hover:bg-emerald-700 focus:ring-4 focus:ring-emerald-100 transition-colors"
              >
                <Plus size={16} />
                Thêm mới
              </button>
            )}
          </div>
        </div>

        {filters.length > 0 && (
          <div className="rounded-lg border border-slate-200 bg-slate-50 p-3">
            <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
              <div className="flex flex-wrap items-center gap-2">
                <ListFilter size={16} className="text-emerald-700" aria-hidden="true" />
                <h2 className="text-sm font-semibold text-slate-900">Bộ lọc dữ liệu</h2>
                <span className="rounded-md bg-white px-2 py-0.5 text-xs font-medium text-slate-500 ring-1 ring-slate-200">
                  {filteredRows.length}/{rows.length} bản ghi
                </span>
              </div>
              <button
                type="button"
                onClick={clearFilters}
                disabled={activeFilterCount === 0}
                className="inline-flex h-8 items-center justify-center gap-1.5 self-start rounded-md border border-slate-300 bg-white px-2.5 text-xs font-medium text-slate-600 hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-40 sm:self-auto"
              >
                <RotateCcw size={15} aria-hidden="true" />
                Xóa bộ lọc{activeFilterCount > 0 ? ` (${activeFilterCount})` : ''}
              </button>
            </div>
            <div className={`mt-3 grid gap-2.5 ${filterGridClassName}`}>
              {filters.map((filter) => (
                <div key={filter.key} className={`min-w-0 ${filter.className || ''}`}>
                  {renderFilter(filter)}
                </div>
              ))}
            </div>
          </div>
        )}

        <div className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
          <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200 text-sm">
            <thead className="bg-slate-100">
              <tr>
                {visibleColumns.map((column) => (
                  <th key={column.key} className="px-4 py-3 text-left font-semibold text-slate-700">
                    {column.label}
                  </th>
                ))}
                {hasActionColumn && (
                  <th className="px-4 py-3 text-right font-semibold text-slate-700">
                    {actionLabel}
                  </th>
                )}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading ? (
                <tr>
                  <td colSpan={columnCount} className="px-4 py-10 text-center text-slate-500">
                    Đang tải dữ liệu...
                  </td>
                </tr>
              ) : filteredRows.length > 0 ? (
                filteredRows.map((row, index) => {
                  const rowKey = getRowKey(row, index);

                  return (
                    <tr key={rowKey} className="hover:bg-slate-50">
                      {visibleColumns.map((column) => (
                        <td key={column.key} className="max-w-64 whitespace-nowrap px-4 py-3 text-slate-700">
                          <div className="truncate">
                            {renderCell(row, column)}
                          </div>
                        </td>
                      ))}
                      {hasActionColumn && (
                        <td className="whitespace-nowrap px-4 py-3 text-right">
                          <div className="flex items-center justify-end gap-2">
                            {showDetails && (
                              <button
                                type="button"
                                onClick={() => setSelectedDetailRow(row)}
                                title="Xem chi tiết"
                                aria-label="Xem chi tiết"
                                className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-emerald-200 bg-emerald-50 text-emerald-700 transition hover:bg-emerald-100"
                              >
                                <Eye size={16} />
                              </button>
                            )}
                            {rowActions && rowActions(row)}
                            {canDelete && canDeleteRow(row) && (
                              <button
                                type="button"
                                onClick={() => handleDelete(row, rowKey)}
                                disabled={deletingRowKey === rowKey}
                                title="Xóa"
                                aria-label="Xóa"
                                className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border border-rose-200 bg-rose-50 text-rose-700 transition hover:bg-rose-100 disabled:cursor-not-allowed disabled:opacity-50"
                              >
                                <Trash2 size={16} />
                              </button>
                            )}
                          </div>
                        </td>
                      )}
                    </tr>
                  );
                })
              ) : (
                <tr>
                  <td colSpan={columnCount} className="px-4 py-10 text-center text-slate-500">
                    {rows.length > 0 && activeFilterCount > 0
                      ? 'Không có dữ liệu phù hợp với bộ lọc.'
                      : emptyText}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
          </div>
        </div>
      </section>

      <Modal isOpen={Boolean(selectedDetailRow)} onClose={() => setSelectedDetailRow(null)} title={resolvedDetailTitle} size="wide" variant="detail">
        {selectedDetailRow && (
          <dl className="grid gap-x-8 gap-y-5 rounded-lg border border-emerald-200 bg-emerald-50/40 p-5 sm:grid-cols-2 lg:grid-cols-3">
            {columns.map((column) => (
              <div key={column.key} className={`min-w-0 ${column.detailClassName || ''}`}>
                <dt className="text-xs font-medium uppercase text-emerald-700">{column.label}</dt>
                <dd className="mt-1 break-words text-sm font-medium text-slate-900">
                  {renderDetailCell(selectedDetailRow, column)}
                </dd>
              </div>
            ))}
          </dl>
        )}
      </Modal>
    </>
  );
}
