export default function CheckboxGroup({ label, name, options, values, onChange }) {
  const selectedValues = Array.isArray(values) ? values : [];
  const selectedSet = new Set(selectedValues);

  const toggleValue = (value) => {
    const nextSet = new Set(selectedValues);
    if (nextSet.has(value)) {
      nextSet.delete(value);
    } else {
      nextSet.add(value);
    }

    const nextValues = options
      .filter((option) => nextSet.has(option.value))
      .map((option) => option.value);
    onChange({ target: { name, value: nextValues } });
  };

  return (
    <fieldset className="min-w-0" aria-required="true">
      <legend className="text-sm font-medium text-slate-700">{label}</legend>
      <div className="mt-1.5 grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
        {options.map((option) => {
          const checked = selectedSet.has(option.value);
          return (
            <label
              key={option.value}
              className={`flex min-h-11 min-w-0 cursor-pointer items-center gap-2 rounded-md border px-3 py-2 text-sm transition ${
                checked
                  ? 'border-emerald-400 bg-emerald-50 text-emerald-900'
                  : 'border-slate-200 bg-white text-slate-700 hover:border-emerald-200 hover:bg-emerald-50/50'
              }`}
            >
              <input
                type="checkbox"
                checked={checked}
                onChange={() => toggleValue(option.value)}
                className="h-4 w-4 shrink-0 accent-emerald-600"
              />
              <span className="min-w-0 leading-5">{option.label}</span>
            </label>
          );
        })}
      </div>
    </fieldset>
  );
}
