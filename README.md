# SRM Portal Backend (Spring Boot + MySQL)

## Run

Set environment variables (do NOT commit secrets):

- `SRM_DB_URL` (e.g. `jdbc:mysql://host:3306/db?useSSL=false&serverTimezone=GMT%2B8`)
- `SRM_DB_USER`
- `SRM_DB_PASSWORD`

Then run:

```bash
mvn spring-boot:run -f pom.xml
```

### 从 `sqltxt.md` 填充（注意 `&amp;`）

如果你的 `sqltxt.md` 里是 `&amp;` 这种 HTML 转义，请在填 `SRM_DB_URL` 时把它改回 `&`。

例如 `sqltxt.md` 的 URL：

`jdbc:mysql://10.30.254.73:3306/license?...&amp;...`

填到 `SRM_DB_URL` 时变为：

`jdbc:mysql://10.30.254.73:3306/license?...&...`

## API (table lookups)

All responses are JSON objects mapped from `SELECT * FROM ...`.

- `GET /api/suppliers/{supplierId}` -> `tb_contract_supplier`
- `GET /api/users/{userId}` -> `blade_user`
- `GET /api/users/{userId}/detail` -> `tb_user_detail` (filter by `srm.db.user-detail-filter-column`, default `user_id`)
- `GET /api/orders/{orderId}` -> `tb_purchase_parts_order`
- `GET /api/invoices/{invoiceId}` -> `tb_invoice`
- `GET /api/users/{userId}/supplier-profile` -> user + detail + supplier
- `GET /api/suppliers/{supplierId}/orders` -> orders by supplier
- `GET /api/orders/{orderId}/invoices` -> invoices by order

## Order collaboration (供应商隔离)

- `GET /api/?userId={blade_user.id}&limit=20&offset=0`  
  根据 `tb_user_detail.USER_ID` + `SUPPLIER_COMPANY_ID` 解析供应商，仅返回该供应商的订单。
- `POST /api/collaboration/orders/{orderId}/confirm` body: `{ "operatorUserId": 123 }`  
  仅当订单 `supplier_id` 与当前用户绑定供应商一致时允许更新。
- `POST /api/collaboration/orders/{orderId}/reject` body: `{ "operatorUserId": 123, "reason": "必填" }`

## ASN（MySQL 持久化）

在 `license` 库执行建表脚本：

- `sql/create_tb_srm_asn.sql`

接口：

- `POST /api/collaboration/asn` body 示例：

```json
{
  "operatorUserId": 123,
  "orderIds": [1, 2],
  "logisticsCompany": "SF",
  "trackingNo": "SF123",
  "etaDate": "2026-04-10"
}
```

- `GET /api/collaboration/asn/{asnId}`
- `GET /api/collaboration/asn/{asnId}/timeline`

## Invoice-Order relation field

To add a direct relation between invoice and purchase order, execute:

- `sql/alter_tb_invoice_add_order_id.sql`

After adding `tb_invoice.PUCHASE_ORDER_ID`, API `GET /api/orders/{orderId}/invoices` will query by `PUCHASE_ORDER_ID` first.
If the new column is not available yet, backend falls back to:

- `tb_purchase_parts_order.order_no` -> `tb_invoice.SALES_ORDER_NO`

## Important config

`src/main/resources/application.yml` contains `srm.db.pk-columns.*`.
You must adjust these values to match your real primary-key column names (defaults assume `id`).

