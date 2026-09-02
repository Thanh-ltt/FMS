# Hai bộ dữ liệu kiểm thử FMS

Hai bộ dữ liệu này dành cho cơ sở dữ liệu phát triển `fms_db`. Không chạy trên
cơ sở dữ liệu thật. Mỗi lần chạy lại một tệp, script sẽ xóa và tạo lại riêng
dữ liệu có tiền tố `test-a-` hoặc `test-b-`, kể cả dữ liệu phát sinh khi bạn thao
tác trên các khách hàng và hợp đồng test đó. Dữ liệu khác không bị xóa.

## Chuẩn bị

1. Chạy backend ít nhất một lần để Hibernate và Flyway tạo đủ bảng.
2. Dừng backend trong lúc nạp dữ liệu để tránh thao tác đồng thời.
3. Mở ứng dụng **Terminal** trên macOS. Không nhập lệnh `psql` trong trình duyệt,
   màn hình đăng nhập hoặc ô Run của frontend.
4. Di chuyển vào project:

```bash
cd /Users/thanhlt/Downloads/FMS
```

## Nạp dữ liệu

Chỉ nạp bộ A:

```bash
/Library/PostgreSQL/17/bin/psql -h localhost -U postgres -d fms_db -v ON_ERROR_STOP=1 -f test-data/dataset-01-complete-flow.sql
```

Chỉ nạp bộ B:

```bash
/Library/PostgreSQL/17/bin/psql -h localhost -U postgres -d fms_db -v ON_ERROR_STOP=1 -f test-data/dataset-02-status-and-edge-cases.sql
```

Có thể chạy cả hai lệnh để hai bộ cùng tồn tại. Khi Terminal hỏi `Password for
user postgres`, nhập mật khẩu PostgreSQL đang cấu hình trong
`api-web/src/main/resources/application.yaml`. Sau khi thấy dòng `COMMIT`, khởi
động lại backend và tải lại frontend.

## Tài khoản

Tất cả tài khoản đang hoạt động đều dùng mật khẩu:

```text
Test@12345
```

### Bộ A

| Tên đăng nhập | Role | Mục đích |
|---|---|---|
| `testa_admin` | ADMIN | Toàn quyền, kiểm tra xóa dữ liệu |
| `testa_manager` | MANAGER | Quản lý hợp đồng, chuyến và bảo dưỡng nhưng không được xóa |
| `testa_account` | ACCOUNTANT | Hóa đơn, thanh toán và báo cáo |
| `testa_driver` | DRIVER | Cổng tài xế và chuyến được phân công |
| `testa_customer` | CUSTOMER | Cổng khách hàng An Phát |

### Bộ B

| Tên đăng nhập | Role | Mục đích |
|---|---|---|
| `testb_admin` | ADMIN | Toàn quyền với dữ liệu trạng thái biên |
| `testb_manager` | MANAGER | Điều phối và chuyển trạng thái |
| `testb_account` | ACCOUNTANT | Công nợ, hóa đơn quá hạn và báo cáo |
| `testb_driver1` | DRIVER | Tài xế của chuyến đang vận chuyển |
| `testb_driver2` | DRIVER | Tài xế của chuyến mới/chuyến hoàn tất |
| `testb_customer` | CUSTOMER | Cổng khách hàng Minh Long |
| `testb_inactive` | MANAGER, đã khóa | Đăng nhập phải bị từ chối |

## Bộ A: luồng hoàn chỉnh

### Dữ liệu chính

| Phân hệ | Dữ liệu |
|---|---|
| Khách hàng | Công ty An Phát, đã liên kết `testa_customer` |
| Hợp đồng | `HD-TEST-A-001`, đang hiệu lực, giá trị thỏa thuận 50.000.000đ, hàng khô, 20.000đ/tấn/km |
| Tiền cọc | 10.000.000đ theo hợp đồng; đã cấn 1.000.000đ; còn 9.000.000đ |
| Phương tiện | `51C-123.45` và `51D-234.56`, đều sẵn sàng |
| Chuyến hoàn tất | 42,5 km × 5 tấn × 20.000đ = 4.250.000đ |
| Chuyến mới | 60 km × 4 tấn × 20.000đ = 4.800.000đ |
| Chi phí chuyến hoàn tất | 1.100.000đ + 250.000đ = 1.350.000đ |
| Hóa đơn | `HDON-TEST-A-001`, đã thanh toán |
| Thanh toán | Cấn cọc 1.000.000đ + chuyển khoản 3.250.000đ |
| Bảo dưỡng | Xe `51D-234.56`, phiếu 3.200.000đ đã hoàn tất |

### Checklist bộ A

1. **Đăng nhập và phân quyền**: đăng nhập lần lượt bằng ADMIN, MANAGER,
   ACCOUNTANT, DRIVER và CUSTOMER; menu phải thay đổi theo role. MANAGER không
   được thấy nút xóa trong các bảng.
2. **Khách hàng**: Công ty An Phát phải hiển thị mã tài khoản
   `testa_customer`; cổng khách hàng phải thấy đúng hợp đồng, chuyến, hóa đơn và
   tiền cọc của mình.
3. **Hợp đồng**: `HD-TEST-A-001` phải nằm trong bộ lọc `Đang trong thời hạn`.
   Sổ cọc phải hiển thị nhận 10.000.000đ, đã cấn 1.000.000đ và còn 9.000.000đ.
4. **Chuyến đi**: chuyến `test-a-trip-completed` phải hoàn tất và có cước
   4.250.000đ. Chuyến `test-a-trip-upcoming` phải ở trạng thái mới tạo và có
   cước 4.800.000đ.
5. **Hóa đơn**: hóa đơn `HDON-TEST-A-001` phải là `Đã thanh toán`; hệ thống
   không được cho thanh toán, xóa hoặc tạo thêm hóa đơn đang hoạt động cho cùng
   chuyến.
6. **Báo cáo chuyến hoàn tất**: doanh thu 4.250.000đ, chi phí chuyến
   1.350.000đ, lợi nhuận gộp 2.900.000đ.
7. **Báo cáo tài chính khi chỉ có bộ A**: đã thu 4.250.000đ, cọc đã cấn
   1.000.000đ, cọc còn 9.000.000đ, chi phí chuyến 1.350.000đ, chi phí bảo dưỡng
   3.200.000đ và lợi nhuận ròng -300.000đ. Khi lọc riêng khách hàng, chi phí bảo
   dưỡng dùng chung không được cộng vào báo cáo khách hàng.
8. **Bảo dưỡng**: phiếu đã hoàn tất phải có ba loại bảo dưỡng và xe vẫn ở trạng
   thái `Sẵn sàng`.

## Bộ B: trạng thái và trường hợp biên

### Dữ liệu chính

| Phân hệ | Dữ liệu |
|---|---|
| Hợp đồng tương lai | `HD-TEST-B-FUTURE`, bản nháp, chưa tới ngày bắt đầu |
| Hợp đồng hoạt động | `HD-TEST-B-ACTIVE`, chưa biết tổng tiền, tính cước và cọc 25% theo từng chuyến |
| Hợp đồng hết hạn | `HD-TEST-B-EXPIRED`, đã hoàn tất và đã qua ngày kết thúc |
| Hợp đồng đã hủy | `HD-TEST-B-CANCELLED`, không có dữ liệu liên quan để test xóa |
| Xe đang chạy | `51C-987.65`, gắn với chuyến `test-b-trip-in-progress` |
| Xe bảo dưỡng | `50H-345.67`, có phiếu đang bảo dưỡng |
| Xe ngưng hoạt động | `51D-876.54` |
| Xe sẵn sàng | `51C-456.78`, có một chuyến mới đã đặt lịch |
| Chuyến thiếu cọc | Cước 14.400.000đ, cọc bắt buộc 3.600.000đ nhưng chưa nhận |
| Hóa đơn quá hạn | `HDON-TEST-B-OVERDUE`, còn nợ 459.200đ |
| Hóa đơn đã hủy | `HDON-TEST-B-CANCELLED`, chuyến được phép tạo hóa đơn mới |
| Cọc hoàn một phần | 1.500.000đ, đã hoàn 500.000đ, còn 1.000.000đ |

### Checklist bộ B

1. **Nhân viên**: `testb_inactive` phải xuất hiện là ngưng hoạt động và không
   đăng nhập được. ADMIN có thể mở lại tài khoản; MANAGER không được xóa nhân
   viên.
2. **Bộ lọc hợp đồng**: `HD-TEST-B-FUTURE` phải vào `Chưa tới ngày bắt đầu`,
   `HD-TEST-B-ACTIVE` vào `Đang trong thời hạn`, còn
   `HD-TEST-B-EXPIRED` vào `Đã qua ngày kết thúc`.
3. **Xóa hợp đồng**: chỉ ADMIN thấy nút xóa `HD-TEST-B-CANCELLED`. Hợp đồng có
   chuyến hoặc phiếu cọc liên quan phải bị chặn xóa.
4. **Đồng bộ phương tiện**: xe `51C-987.65` không được chọn cho chuyến khác vì
   đang chạy; xe `50H-345.67` không được chọn vì đang bảo dưỡng; xe
   `51D-876.54` không được chọn vì đã ngưng hoạt động.
5. **Hoàn tất chuyến đang chạy**: hoàn tất `test-b-trip-in-progress` phải đưa xe
   `51C-987.65` về `Sẵn sàng`. Trước khi hoàn tất, thử hoàn tất hợp đồng đang
   hoạt động phải bị chặn vì còn chuyến mở.
6. **Thiếu cọc**: bắt đầu `test-b-trip-missing-deposit` phải bị chặn với mức
   thiếu 3.600.000đ. Ghi nhận đủ cọc cho chính chuyến rồi bắt đầu lại; xe
   `51C-456.78` phải chuyển sang `Đang chạy`.
7. **Bảo dưỡng**: hoàn tất phiếu của xe `50H-345.67` phải đưa xe về `Sẵn sàng`.
   Phiếu chờ của xe `51C-456.78` chưa tới ngày nên thao tác bắt đầu phải bị chặn.
8. **Hóa đơn**: `HDON-TEST-B-OVERDUE` phải hiển thị `Quá hạn` và công nợ
   459.200đ. Chuyến đang vận chuyển không được tạo hóa đơn.
9. **Tạo lại hóa đơn**: chọn chuyến `test-b-trip-reinvoice`; dù đã có hóa đơn
   `HDON-TEST-B-CANCELLED`, hệ thống vẫn phải cho tạo một hóa đơn mới. Sau khi
   tạo, chuyến này chỉ được có một hóa đơn không bị hủy.
10. **Hoàn cọc**: phiếu `PC-TEST-B-REFUND` phải có lịch sử hoàn 500.000đ và
    không được xóa vì đã phát sinh hoàn tiền.
11. **Báo cáo khi chỉ có bộ B**: doanh thu ghi nhận 459.200đ, đã thu 0đ, công nợ
    459.200đ, chi phí chuyến 2.930.000đ và lợi nhuận ròng -2.470.800đ. Phiếu bảo
    dưỡng đang thực hiện chưa được tính vào chi phí bảo dưỡng hoàn tất.
12. **Tuyến đường có số nhà**: thử tính lại tuyến
    `4418 Nguyễn Cửu Phú, Tân Tạo, Bình Tân, TP. Hồ Chí Minh` đến
    `742 Hương lộ 2, Bình Trị Đông, Bình Tân, TP. Hồ Chí Minh`; kết quả phải lớn
    hơn 0 km và phần mô tả phải cho biết địa chỉ đã được định vị thành gì.

## Số bản ghi ban đầu

| Bảng nghiệp vụ | Bộ A | Bộ B |
|---|---:|---:|
| Tài khoản | 5 | 7 |
| Khách hàng | 1 | 1 |
| Tài xế | 1 | 2 |
| Phương tiện | 2 | 4 |
| Hợp đồng | 1 | 4 |
| Chuyến đi | 2 | 5 |
| Bảo dưỡng | 1 | 3 |
| Chi phí | 2 | 3 |
| Phiếu cọc | 1 | 2 |
| Hóa đơn | 1 | 2 |
