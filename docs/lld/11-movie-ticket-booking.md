# 11. Movie Ticket Booking

[← LLD index](README.md) · [All docs](../README.md)

---

*(Similar to parking slot)*

- Users search movie
- Browse theatres, showtimes
- Select seat
- Reserve ticket

## Scoping questions / Requirements
1. User search movie by name (exact?) — can also browse theatres → return `List<Show>`
   *(strategy good here)*
2. List shows. `Show = theatre, screen, timings`. List by theatre?
3. User selects show → list available seats (no fancy seats)
4. User selects seats (block for 10 mins)
5. User confirms & books seat (pays). The seat is confirmed & ticket returned. (No cancellations)

## Edge cases
- Concurrent flow handling
- When 2 users book same seat

## Out of scope
- UI
- Payment
- Cancellation flow
- Fancy search
- Fancy cancellations
- Full-fledged DB

## Entities
- **Theatre**: `List<Show>`
- **Show**: startTime, endTime, `List<Seats>`, Movie
- **Movie**: name

- **SearchService**: searchByMovie, searchByTheatre → `List<Show>`
- **Theatre**: addShow
- **Show**: seats, start, end, movie (movie shared across)
- **BookingService**:
  - selectSeats(show, List\<Seat\>)
  - bookSeats(show, List\<Seat\>)

## Concurrency
1. Coarse-grain → single bookSeatsThread
2. Medium-grain → read lock on gets, write lock on book (check & set)
3. Lock for every seat, acquired in consistent order

- For every seat can also go for atomic reference: status, compareAndSet
  (But should be avoided in case we need to perform bookSeat op in multiple steps)

## Extensibility
1. Support dynamically adding shows, movies, theatres — expose methods
2. How to implement temporary seat holds

For Seat: have status {AVAILABLE, ON_HOLD, BOOKED}
- On successful payment confirmation: ON_HOLD → BOOKED
- Wait for payment flow to succeed (keep window, use refund)
