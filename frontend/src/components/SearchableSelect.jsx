import { useEffect, useId, useMemo, useRef, useState } from 'react';
import { Check, ChevronDown, Search, X } from 'lucide-react';
import { matchSearchText } from '../utils/text';

export default function SearchableSelect({
  label,
  name,
  value = '',
  options = [],
  placeholder = 'Gõ để tìm kiếm...',
  allLabel,
  onChange,
  disabled = false,
  required = false,
  className = '',
  popupAlign = 'left',
}) {
  const containerRef = useRef(null);
  const inputRef = useRef(null);
  const listRef = useRef(null);
  const listboxId = useId();

  const [isOpen, setIsOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [highlightedIndex, setHighlightedIndex] = useState(-1);

  const normalizedValue = String(value ?? '');
  const selectedOption = useMemo(
    () => options.find((opt) => String(opt.value) === normalizedValue),
    [options, normalizedValue]
  );

  const defaultAllText = allLabel || (label ? `Tất cả ${label.toLowerCase()}` : 'Tất cả');

  // Filter options based on user search query
  const filteredOptions = useMemo(() => {
    if (!searchQuery.trim()) return options;
    return options.filter((opt) => (
      matchSearchText(opt.label, searchQuery)
      || matchSearchText(opt.value, searchQuery)
      || (opt.keywords && matchSearchText(opt.keywords, searchQuery))
    ));
  }, [options, searchQuery]);

  // Click outside listener
  useEffect(() => {
    const handlePointerDownOutside = (event) => {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        setIsOpen(false);
        setSearchQuery('');
        setHighlightedIndex(-1);
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handlePointerDownOutside);
      document.addEventListener('touchstart', handlePointerDownOutside);
    }

    return () => {
      document.removeEventListener('mousedown', handlePointerDownOutside);
      document.removeEventListener('touchstart', handlePointerDownOutside);
    };
  }, [isOpen]);

  // Reset highlight index when filtered options change
  useEffect(() => {
    setHighlightedIndex(-1);
  }, [filteredOptions.length, searchQuery]);

  // Scroll highlighted item into view
  useEffect(() => {
    if (isOpen && highlightedIndex >= 0 && listRef.current) {
      const items = listRef.current.querySelectorAll('[role="option"]');
      if (items[highlightedIndex]) {
        items[highlightedIndex].scrollIntoView({ block: 'nearest' });
      }
    }
  }, [highlightedIndex, isOpen]);

  const handleSelect = (optionValue) => {
    if (onChange) {
      onChange({
        target: {
          name,
          value: optionValue,
        },
      });
    }
    setIsOpen(false);
    setSearchQuery('');
    setHighlightedIndex(-1);
    if (inputRef.current) {
      inputRef.current.blur();
    }
  };

  const handleClear = (event) => {
    event.stopPropagation();
    if (onChange) {
      onChange({
        target: {
          name,
          value: '',
        },
      });
    }
    setSearchQuery('');
    setHighlightedIndex(-1);
    if (inputRef.current) {
      inputRef.current.focus();
    }
  };

  const handleKeyDown = (event) => {
    if (disabled) return;

    if (!isOpen) {
      if (event.key === 'ArrowDown' || event.key === 'ArrowUp' || event.key === 'Enter') {
        event.preventDefault();
        setIsOpen(true);
        return;
      }
    }

    if (event.key === 'ArrowDown') {
      event.preventDefault();
      setHighlightedIndex((prev) => {
        const next = prev + 1;
        return next >= filteredOptions.length ? 0 : next;
      });
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      setHighlightedIndex((prev) => {
        const next = prev - 1;
        return next < 0 ? filteredOptions.length - 1 : next;
      });
    } else if (event.key === 'Enter') {
      event.preventDefault();
      if (highlightedIndex >= 0 && filteredOptions[highlightedIndex]) {
        handleSelect(filteredOptions[highlightedIndex].value);
      } else if (filteredOptions.length === 1) {
        handleSelect(filteredOptions[0].value);
      }
    } else if (event.key === 'Escape') {
      event.preventDefault();
      setIsOpen(false);
      setSearchQuery('');
      setHighlightedIndex(-1);
      if (inputRef.current) {
        inputRef.current.blur();
      }
    } else if (event.key === 'Tab') {
      setIsOpen(false);
      setSearchQuery('');
      setHighlightedIndex(-1);
    }
  };

  const displayInputValue = isOpen
    ? searchQuery
    : selectedOption
      ? selectedOption.label
      : '';

  return (
    <div ref={containerRef} className={`relative min-w-0 ${className}`}>
      {label && (
        <label
          htmlFor={`${listboxId}-input`}
          className="flex min-h-6 items-center text-xs font-medium text-slate-600"
        >
          {label}
          {required && <span className="ml-1 text-rose-500">*</span>}
        </label>
      )}

      <div
        className={`group relative mt-1 flex h-11 w-full items-center rounded-md border bg-white shadow-sm transition-all ${
          isOpen
            ? 'border-emerald-500 ring-2 ring-emerald-100'
            : 'border-slate-300 hover:border-slate-400'
        } ${disabled ? 'cursor-not-allowed bg-slate-100 opacity-60' : 'cursor-text'}`}
        onClick={() => {
          if (!disabled) {
            setIsOpen(true);
            if (inputRef.current) {
              inputRef.current.focus();
            }
          }
        }}
      >
        <div className="pointer-events-none pl-3 text-slate-400">
          <Search size={16} aria-hidden="true" />
        </div>

        <input
          ref={inputRef}
          id={`${listboxId}-input`}
          name={name}
          type="text"
          role="combobox"
          aria-expanded={isOpen}
          aria-controls={listboxId}
          aria-autocomplete="list"
          autoComplete="off"
          disabled={disabled}
          value={displayInputValue}
          placeholder={selectedOption ? selectedOption.label : (placeholder || defaultAllText)}
          onChange={(e) => {
            setSearchQuery(e.target.value);
            if (!isOpen) setIsOpen(true);
          }}
          onFocus={() => {
            if (!disabled) setIsOpen(true);
          }}
          onKeyDown={handleKeyDown}
          className="h-full min-w-0 flex-1 bg-transparent px-2.5 text-sm text-slate-900 placeholder:text-slate-400 outline-none"
        />

        <div className="flex items-center gap-1 pr-2">
          {(normalizedValue !== '' || searchQuery !== '') && !disabled && (
            <button
              type="button"
              tabIndex={-1}
              onClick={handleClear}
              title="Xóa lựa chọn"
              aria-label="Xóa lựa chọn"
              className="inline-flex size-6 items-center justify-center rounded text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
            >
              <X size={15} />
            </button>
          )}

          <button
            type="button"
            tabIndex={-1}
            disabled={disabled}
            aria-label={isOpen ? 'Đóng danh sách' : 'Mở danh sách'}
            className="inline-flex size-6 items-center justify-center text-slate-400 transition group-hover:text-slate-600"
          >
            <ChevronDown
              size={16}
              className={`transition-transform duration-200 ${isOpen ? 'rotate-180 text-emerald-600' : ''}`}
            />
          </button>
        </div>
      </div>

      {isOpen && !disabled && (
        <div
          id={listboxId}
          role="listbox"
          ref={listRef}
          className={`absolute ${popupAlign === 'right' ? 'right-0' : 'left-0'} top-full z-50 mt-1 max-h-64 w-full min-w-[240px] overflow-auto rounded-lg border border-slate-200 bg-white p-1 shadow-xl ring-1 ring-black/5`}
        >
          {/* Default "All" option */}
          <div
            role="option"
            aria-selected={normalizedValue === ''}
            onClick={() => handleSelect('')}
            className={`flex cursor-pointer items-center justify-between rounded-md px-3 py-2 text-sm transition ${
              normalizedValue === ''
                ? 'bg-emerald-50 font-semibold text-emerald-800'
                : 'text-slate-700 hover:bg-slate-100'
            }`}
          >
            <span className="truncate">{defaultAllText}</span>
            {normalizedValue === '' && <Check size={16} className="shrink-0 text-emerald-600" />}
          </div>

          {searchQuery.trim() && (
            <div className="border-t border-slate-100 px-3 py-1.5 text-[11px] font-medium uppercase tracking-wider text-slate-400">
              {filteredOptions.length > 0 ? `${filteredOptions.length} kết quả phù hợp` : 'Không có kết quả'}
            </div>
          )}

          {filteredOptions.length > 0 ? (
            filteredOptions.map((option, index) => {
              const isSelected = String(option.value) === normalizedValue;
              const isHighlighted = index === highlightedIndex;

              return (
                <div
                  key={option.value}
                  role="option"
                  aria-selected={isSelected}
                  onClick={() => handleSelect(option.value)}
                  className={`flex cursor-pointer items-center justify-between rounded-md px-3 py-2 text-sm transition ${
                    isSelected
                      ? 'bg-emerald-50 font-semibold text-emerald-800'
                      : isHighlighted
                        ? 'bg-slate-100 text-slate-900'
                        : 'text-slate-700 hover:bg-slate-100'
                  }`}
                >
                  <span className="truncate" title={option.label}>
                    {option.label}
                  </span>
                  {isSelected && <Check size={16} className="shrink-0 text-emerald-600" />}
                </div>
              );
            })
          ) : (
            <div className="px-3 py-4 text-center text-xs text-slate-500">
              Không tìm thấy kết quả phù hợp với &ldquo;<span className="font-semibold text-slate-700">{searchQuery}</span>&rdquo;
            </div>
          )}
        </div>
      )}
    </div>
  );
}
