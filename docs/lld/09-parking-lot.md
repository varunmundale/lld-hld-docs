# 9. Parking Lot

[← LLD index](README.md) · [All docs](../README.md)

---

*(numbered "4)")*

- Multiple spots

## Flow
- Vehicle enters system
- System assigns available slot
- Vehicle parked in the spot
- Vehicle exits, system calculates fee, frees up spot (by showing ticket)

## Scoping questions — Solution optimized?
1. Only 1 parking slot?
2. Concurrency?
3. Spots of different sizes / cars? Fallback on size? Separate slot types
4. Only single trigger, assume entry & parking as same operation
5. Simulation `step()`?
6. Multiple floor?
7. Fee calculated (rounded to hour), base price, start with simple

## Edge cases
8. Parking is full; ticket is invalid

## Out of scope / Assumptions
- UI
- Third party (payment, physical h/w)
- User service
- The ticket is not lost

## Requirements

**Flow 1**
1. Vehicle enters parking spot (car, truck)
2. System returns valid parking spot (assign)
3. The car is parked in this slot
4. System returns ticket (start-time, vehicle, slot)

**Flow 2**
1. Vehicle exits parking spot, show valid ticket
2. End-time captured, fee calculated
3. Stamped on ticket
4. Slot freed

## System
`step` function for simulation

- **Parking**: slots, addSlots
- **ParkingService**: park, unpark
- **Slot**: slotType, slotId
- **Ticket**: st-time, e-time, fee, slot, vehicle
- **Vehicle**: vehicleId, type

## Extensibility
1. Multifloor parking
   - ParkingLot → has floors → has slots
   - Change findSlot logic
2. How to add different pricing for different types
   - Create a Factory which returns PricingStrategy object based on vehicleType
3. Concurrency, multiple entry, exits
   1. **Simple approach**: keep a single lock for park, unpark operations. Every entrance, exit calls this. (coarse-grained lock)
   2. **Better approach** (medium concurrency):
      - Have read lock on findAvailableSlots
      - Have write lock on park()/unpark() (some operations might be unsuccessful — 3 retry)
      - This is low contention scenario (OCC-like)
