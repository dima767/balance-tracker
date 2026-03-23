# Multi-User Data Separation

## Overview
- Partition all data (Payees, PaymentPeriods, PaymentItems) by authenticated user
- Add `user_id` foreign key to `Payee` and `PaymentPeriod` tables (PaymentItems inherit via PaymentPeriod)
- Filter all queries by the currently authenticated user — users only see their own data
- Assign all existing data to the first user who logs in (preserves current single-user data)
- Secure the service and controller layers to enforce user boundaries
- Prepares the backend for Chrome extension API (users access only their data via API)

## Context (from discovery)
- **Depends on Plan 1** (Google OAuth/OIDC Login) — `User` entity and auth must be in place
- **Existing entities**: `Payee` (has `name` unique constraint — must become unique per user, not globally), `PaymentPeriod` (has `periodDate` unique constraint — must become unique per user), `PaymentItem` (belongs to PaymentPeriod)
- **Hibernate `ddl-auto=update`** — adding `user_id` columns will be auto-applied, but existing rows need a migration script for null backfill
- **Service layer**: `DefaultPayeeService`, `DefaultPaymentPeriodService` — all queries go through repositories, need user filtering
- **Controllers**: inject authenticated user and pass to service methods
- **Package:** `dk.balancetracker`, interface-based services with `@Bean` config

## Development Approach
- **Testing approach**: Regular (code first, then tests)
- Complete each task fully before moving to the next
- Make small, focused changes
- **CRITICAL: every task MUST include new/updated tests** for code changes in that task
- **CRITICAL: all tests must pass before starting next task**
- **CRITICAL: update this plan file when scope changes during implementation**
- Run tests after each change

## Testing Strategy
- **Unit tests**: required for every task — test user isolation at service and repository level
- **Integration tests**: verify that User A cannot access User B's data
- **No e2e tests** in project currently

## Progress Tracking
- Mark completed items with `[x]` immediately when done
- Add newly discovered tasks with + prefix
- Document issues/blockers with warning prefix
- Update plan if implementation deviates from original scope

## Implementation Steps

### Task 1: Add user relationship to Payee entity
- [ ] Add `@ManyToOne` `User user` field to `Payee` entity with `@JoinColumn(name = "user_id")`
- [ ] Drop the global unique constraint on `Payee.name`, replace with a composite unique constraint on `(user_id, name)` — payee names are unique per user, not globally
- [ ] Update `PayeeRepository` — add `findAllByUserOrderByNameAsc(User)`, `findByUserAndNameIgnoreCase(User, String)`, `existsByUserAndNameIgnoreCase(User, String)`
- [ ] Write tests for Payee entity with user association
- [ ] Write tests for new PayeeRepository query methods (isolation between users)
- [ ] Run tests — must pass before next task

### Task 2: Add user relationship to PaymentPeriod entity
- [ ] Add `@ManyToOne` `User user` field to `PaymentPeriod` entity with `@JoinColumn(name = "user_id")`
- [ ] Drop the global unique constraint on `PaymentPeriod.periodDate`, replace with composite unique constraint on `(user_id, period_date)` — period dates are unique per user
- [ ] Update `PaymentPeriodRepository` — add `findAllByUserOrderByPeriodDateDesc(User)`, `findByIdAndUser(Long, User)`, `existsByUserAndPeriodDate(User, LocalDate)`
- [ ] Write tests for PaymentPeriod entity with user association
- [ ] Write tests for new PaymentPeriodRepository query methods (isolation between users)
- [ ] Run tests — must pass before next task

### Task 3: Migrate existing data to first user
- [ ] Create a `DataMigrationService` (or `@EventListener` on `ApplicationReadyEvent`) that:
  - On app startup, checks if any `Payee` or `PaymentPeriod` rows have `user_id = NULL`
  - If so, assigns them to the first `User` in the database (by `createdAt` ASC)
  - Runs only once — no-ops if all rows already have a user
  - Logs the migration action
- [ ] Register bean in `AppAutoConfiguration`
- [ ] Write tests for DataMigrationService (assigns orphaned records, no-ops when no orphans, no-ops when no users)
- [ ] Run tests — must pass before next task

### Task 4: Update PayeeService for multi-user
- [ ] Update `PayeeService` interface — all methods take `User` parameter
- [ ] Update `DefaultPayeeService`:
  - `createPayee(User, String name)` — check uniqueness per user
  - `findOrCreatePayee(User, String name)` — scoped to user
  - `findAll(User)` — only user's payees
  - `searchByName(User, String)` — only user's payees
  - `existsByName(User, String)` — per-user check
  - `updatePayee(User, Long id, String)` — verify ownership before update
  - `deletePayee(User, Long id)` — verify ownership before delete
- [ ] Write tests for all updated methods (correct user filtering, cross-user isolation, ownership check on update/delete)
- [ ] Run tests — must pass before next task

### Task 5: Update PaymentPeriodService for multi-user
- [ ] Update `PaymentPeriodService` interface — all methods take `User` parameter
- [ ] Update `DefaultPaymentPeriodService`:
  - `createPaymentPeriod(User, LocalDate, MonetaryAmount)` — associate with user, unique date per user
  - `createPaymentPeriodWithPaymentItems(User, ...)` — associate with user
  - `updatePaymentPeriodWithPaymentItems(User, Long id, ...)` — verify ownership
  - `findByIdWithPaymentItems(User, Long id)` — verify ownership
  - `findAllWithPaymentItems(User)` — only user's periods
  - `addPaymentItem(User, Long id, ...)` — verify period ownership
  - `removePaymentItem(User, Long id, Long itemId)` — verify ownership
  - `updatePaymentItem(User, Long id, Long itemId, ...)` — verify ownership
  - `deletePaymentPeriod(User, Long id)` — verify ownership
- [ ] Throw `AccessDeniedException` or custom exception when user doesn't own the resource
- [ ] Write tests for all updated methods (correct user filtering, cross-user isolation, ownership enforcement)
- [ ] Run tests — must pass before next task

### Task 6: Update controllers to pass authenticated user
- [ ] Update `HomeController` — get `User` from security context (using helper from Plan 1 Task 7), pass to service calls
- [ ] Update `PaymentPeriodController` — pass `User` to all service calls, return 403 if user doesn't own resource
- [ ] Update `PayeeController` — pass `User` to all service calls
- [ ] Update `ReportsController` — pass `User` to data queries
- [ ] Update `/api/payees` and `/api/payees/search` endpoints — filter by user
- [ ] Write tests for each controller (verify user parameter passed, verify 403 on cross-user access)
- [ ] Run tests — must pass before next task

### Task 7: Verify acceptance criteria
- [ ] Verify: User A's payees are not visible to User B
- [ ] Verify: User A's payment periods are not visible to User B
- [ ] Verify: User A cannot edit/delete User B's data (returns 403)
- [ ] Verify: existing data was assigned to first user after migration
- [ ] Verify: new user starts with empty dashboard
- [ ] Verify: payee names can be duplicated across users but not within a user
- [ ] Verify: period dates can be duplicated across users but not within a user
- [ ] Verify: all htmx interactions work correctly with user-scoped data
- [ ] Run full test suite
- [ ] Run linter — all issues must be fixed

### Task 8: [Final] Update documentation
- [ ] Update README.md noting multi-user support
- [ ] Document the data migration behavior (first user gets existing data)

## Technical Details

### Schema Changes
```
payees (existing)
├── ... (existing columns)
└── user_id (BIGINT, FK -> users.id) -- NEW
    UNIQUE(user_id, name) -- replaces UNIQUE(name)

payment_periods (existing)
├── ... (existing columns)
└── user_id (BIGINT, FK -> users.id) -- NEW
    UNIQUE(user_id, period_date) -- replaces UNIQUE(period_date)

payment_items -- NO CHANGES (inherits user scope via payment_period)
```

### User Isolation Pattern
```java
// Controller extracts User
User user = userService.findByGoogleSubjectId(oidcUser.getSubject());

// Service enforces ownership
PaymentPeriod period = repository.findByIdAndUser(id, user)
    .orElseThrow(() -> new AccessDeniedException("Not your resource"));
```

### Data Migration Flow
1. App starts -> `DataMigrationService` runs
2. Checks for `Payee` or `PaymentPeriod` rows where `user_id IS NULL`
3. If found, loads first `User` by `createdAt ASC`
4. Updates all orphaned rows: `SET user_id = firstUser.id`
5. Logs: "Migrated N payees and M payment periods to user: {email}"
6. Subsequent starts: no orphaned rows -> no-op

## Post-Completion

**Manual verification:**
- Log in with two different Google accounts
- Create payees and periods in each account
- Verify complete isolation between accounts
- Verify existing data appears under first account

**Next steps:**
- Chrome extension API (REST endpoints with OAuth2 bearer token auth)
- Railway deployment with production Google OAuth credentials
