# 8. Elevator

[← LLD index](README.md) · [All docs](../README.md)

---

- Multiple elevators serving different floors
- On request, match a lift ↑↓
- Passenger select destination floors
- Multiple requests, # floors

User, `reqLift(floor, direction)`, Lift
Lift contains: Buttons, Floor, User

## Functional
- Design for single system?
- Lift capacity handling?

## Flow
1. User requests ↑↓ from a floor
2. Appropriate lift returned via match strategy — first available
3. Lift state (which floor?)
   - (a) Do we need to simulate real-time?
   - (b) Keep time tracking out of system
   - (c) System responds to time unit trigger
4. User presses lift destination floor (optional)
5. Do we need to track user?

## Edge cases
1. Multiple destinations?
2. Pressed ↑ but down floor? Cancellations?
3. Same floor — reject!

## Base case
3 elevators, 10 floors

## Requirements
1. System manages 3 elevators, 10 floors
2. User can request from any floor ↑↓, returns a lift
3. User selects destination floor
4. `step()` function
5. Concurrent pickup requests (extend critical section locking)
6. Invalid floors (not possible)

**Note:** Destination request can happen async
⭐ Idle state is important (don't move lift if no requests)

## Out of scope
- Passenger limits
- Lift stop delay
- UI
- Configurability (but can be extended easily to configure if required)
- Lift door mechanics

## LiftService
`// This is responsible for handling requests`
`Map<Lift>`
- `requestLift(fromFloor, direction)`
- `assignLift(strategy)`
- `step()`
- `validateBounds()`
- `requestDestination`

## Lift
- # of floors
- currentFloor
- currentDirection
- upQueue, downQueue — from `requestLift`
- destinationQueue

`step()`
- `onFloorReached` — // process
- `changeDirection()`

⭐ To simplify, keep idle state at TOP or BOTTOM floor only

## Extensions
1. How to add priority floors / express elevator
   - Add LiftType, based on this reject requests & stops
2. How to cancel floor request
   - Add/remove request API
   - `removeLiftCall`
   - `removeDestCall`
3. Multiple lift calls
   - Make all functions critical section using **lock**
   - This will allow only single operation at a time
