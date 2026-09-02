const OTHER_BANK = '__OTHER_BANK__';

const bankGroups = [
  {
    label: 'Ngân hàng phổ biến',
    banks: [
      { value: 'ACB', label: 'ACB - Ngân hàng Á Châu' },
      { value: 'Agribank', label: 'Agribank - Ngân hàng Nông nghiệp và Phát triển Nông thôn' },
      { value: 'BIDV', label: 'BIDV - Ngân hàng Đầu tư và Phát triển Việt Nam' },
      { value: 'HDBank', label: 'HDBank - Ngân hàng Phát triển TP.HCM' },
      { value: 'MB', label: 'MB - Ngân hàng Quân đội' },
      { value: 'Sacombank', label: 'Sacombank - Ngân hàng Sài Gòn Thương Tín' },
      { value: 'SHB', label: 'SHB - Ngân hàng Sài Gòn - Hà Nội' },
      { value: 'Techcombank', label: 'Techcombank - Ngân hàng Kỹ Thương Việt Nam' },
      { value: 'TPBank', label: 'TPBank - Ngân hàng Tiên Phong' },
      { value: 'VIB', label: 'VIB - Ngân hàng Quốc tế Việt Nam' },
      { value: 'Vietcombank', label: 'Vietcombank - Ngân hàng Ngoại thương Việt Nam' },
      { value: 'VietinBank', label: 'VietinBank - Ngân hàng Công Thương Việt Nam' },
      { value: 'VPBank', label: 'VPBank - Ngân hàng Việt Nam Thịnh Vượng' },
    ],
  },
  {
    label: 'Ngân hàng khác tại Việt Nam',
    banks: [
      { value: 'ABBank', label: 'ABBank - Ngân hàng An Bình' },
      { value: 'Bac A Bank', label: 'Bac A Bank - Ngân hàng Bắc Á' },
      { value: 'BaoViet Bank', label: 'BaoViet Bank - Ngân hàng Bảo Việt' },
      { value: 'BVBank', label: 'BVBank - Ngân hàng Bản Việt' },
      { value: 'Co-opBank', label: 'Co-opBank - Ngân hàng Hợp tác xã Việt Nam' },
      { value: 'Eximbank', label: 'Eximbank - Ngân hàng Xuất Nhập Khẩu Việt Nam' },
      { value: 'GPBank', label: 'GPBank - Ngân hàng Dầu Khí Toàn Cầu' },
      { value: 'KienlongBank', label: 'KienlongBank - Ngân hàng Kiên Long' },
      { value: 'LPBank', label: 'LPBank - Ngân hàng Lộc Phát Việt Nam' },
      { value: 'MBV', label: 'MBV - Ngân hàng TNHH MTV Việt Nam Hiện Đại' },
      { value: 'MSB', label: 'MSB - Ngân hàng Hàng Hải Việt Nam' },
      { value: 'Nam A Bank', label: 'Nam A Bank - Ngân hàng Nam Á' },
      { value: 'NCB', label: 'NCB - Ngân hàng Quốc Dân' },
      { value: 'OCB', label: 'OCB - Ngân hàng Phương Đông' },
      { value: 'PGBank', label: 'PGBank - Ngân hàng Thịnh Vượng và Phát triển' },
      { value: 'PVcomBank', label: 'PVcomBank - Ngân hàng Đại Chúng Việt Nam' },
      { value: 'Saigonbank', label: 'Saigonbank - Ngân hàng Sài Gòn Công Thương' },
      { value: 'SeABank', label: 'SeABank - Ngân hàng Đông Nam Á' },
      { value: 'VCBNeo', label: 'VCBNeo - Ngân hàng Ngoại thương Công nghệ số' },
      { value: 'VietABank', label: 'VietABank - Ngân hàng Việt Á' },
      { value: 'VietBank', label: 'VietBank - Ngân hàng Việt Nam Thương Tín' },
      { value: 'Vikki Bank', label: 'Vikki Bank - Ngân hàng số Vikki' },
    ],
  },
  {
    label: 'Ngân hàng nước ngoài và liên doanh',
    banks: [
      { value: 'CIMB Vietnam', label: 'CIMB Vietnam' },
      { value: 'Hong Leong Bank Vietnam', label: 'Hong Leong Bank Vietnam' },
      { value: 'HSBC Vietnam', label: 'HSBC Vietnam' },
      { value: 'Indovina Bank', label: 'Indovina Bank' },
      { value: 'KBank', label: 'KBank - Kasikornbank' },
      { value: 'Public Bank Vietnam', label: 'Public Bank Vietnam' },
      { value: 'Shinhan Bank Vietnam', label: 'Shinhan Bank Vietnam' },
      { value: 'Standard Chartered Vietnam', label: 'Standard Chartered Vietnam' },
      { value: 'UOB Vietnam', label: 'UOB Vietnam' },
      { value: 'VRB', label: 'VRB - Ngân hàng Liên doanh Việt - Nga' },
      { value: 'Woori Bank Vietnam', label: 'Woori Bank Vietnam' },
    ],
  },
];

const knownBankNames = new Set(bankGroups.flatMap((group) => group.banks.map((bank) => bank.value)));

export default function BankSelect({ value = '', onChange, required = false, label = 'Ngân hàng' }) {
  const isOtherBank = value === OTHER_BANK || Boolean(value && !knownBankNames.has(value));
  const selectedValue = isOtherBank ? OTHER_BANK : value;

  return (
    <div>
      <label className="block text-sm font-medium text-slate-700">{label}</label>
      <select
        required={required}
        value={selectedValue}
        onChange={(event) => onChange(event.target.value)}
        className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500"
      >
        <option value="">Chọn ngân hàng</option>
        {bankGroups.map((group) => (
          <optgroup key={group.label} label={group.label}>
            {group.banks.map((bank) => (
              <option key={bank.value} value={bank.value}>{bank.label}</option>
            ))}
          </optgroup>
        ))}
        <option value={OTHER_BANK}>Ngân hàng khác</option>
      </select>

      {isOtherBank && (
        <input
          required={required}
          autoFocus
          maxLength={100}
          value={value === OTHER_BANK ? '' : value}
          onChange={(event) => onChange(event.target.value || OTHER_BANK)}
          placeholder="Nhập tên ngân hàng khác"
          className="mt-2 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500"
        />
      )}
    </div>
  );
}
