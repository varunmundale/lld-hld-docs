# 14. Inventory Management

[← LLD index](README.md) · [All docs](../README.md)

---

*(numbered "8)")*

- Product stocks, multiple warehouses
- Inventory add to warehouse
- Order ships, deduct stock
- Transfer inventory between warehouses
- Alerting

## Scoping
1. How is order placed? AVAIL, RESERV
2. Different thresholds for low?
3. When order placed, nearest warehouse — what if not available?
4. Fixed warehouses at start?
5. Concurrency? Multiple requests, same warehouse
6. When inventory runs low, trigger automatic transfer?

## Assumptions
- No -ve inventory
- Fixed warehouse
- Order directed to ONLY nearest warehouse

## Requirements
1. Track warehouse inventories
2. Add stock
3. Remove stock (no OMS)
4. When inventory low:
   - Alert warehouse (client)
   - Request transfer between warehouse
5. Concurrency

## Entities
- **WarehouseService**: `Map<Warehouse>`, addStock, removeStock, transfer
- **Warehouse**: id, addStock, removeStock, `Map<PI>` (Product Inventory)
- **ProductInventory**: Product, quantity
- **AlertConfig**: (threshold, AlertListener)

AlertConfiguration is tricky!

## Extensions
1. How do you prevent overselling?
   - Locking
   - Order place, shipped — single operation
   - In real life, add status "reserve" to differentiate. Lock-only & reserve/available.
   - Pessimistic vs optimistic
2. How would you handle inventory that's being shipped between?
   - Currently atomic operation
   - Treat logistics also as warehouse (not visible to end users)
