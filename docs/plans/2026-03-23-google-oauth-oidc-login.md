# Google OAuth/OIDC Login

## Overview
- Add Google OAuth/OIDC authentication to the Balance Tracker app using Spring Security OAuth2 Client
- Google-only login (no local username/password) — all users must have a Google account
- Create a `User` entity tied to Google's OIDC subject ID, auto-created on first login
- Public landing page at root; all app pages require authentication
- Beautiful landing page designed using current CSS guidelines and the frontend-design skill
- Foundation for multi-user data separation (Plan 2) and future Chrome extension API

## Context (from discovery)
- **Spring Boot 4.0.3**, Java 25, no Spring Security present (clean slate)
- **No User entity** — existing entities: `Payee`, `PaymentPeriod`, `PaymentItem`
- **Hibernate `ddl-auto=update`** — no Flyway/Liquibase, schema auto-evolves
- **SSL configured** — `https://localhost:8443/balancetracker` (required for OAuth callbacks)
- **Package:** `dk.balancetracker`, services use interface-based design with `@Bean` config in `AppAutoConfiguration`
- **Templates:** Thymeleaf + htmx + Bootstrap 5.3.3, sidebar layout in `layout.html`
- **Context path:** `/balancetracker`

## Development Approach
- **Testing approach**: Regular (code first, then tests)
- Complete each task fully before moving to the next
- Make small, focused changes
- **CRITICAL: every task MUST include new/updated tests** for code changes in that task
- **CRITICAL: all tests must pass before starting next task**
- **CRITICAL: update this plan file when scope changes during implementation**
- Run tests after each change
- Maintain backward compatibility

## Testing Strategy
- **Unit tests**: required for every task
- **Integration tests**: `@SpringBootTest` with `@WithMockUser` / mock OAuth2 for security config
- **No e2e tests** in project currently

## Progress Tracking
- Mark completed items with `[x]` immediately when done
- Add newly discovered tasks with + prefix
- Document issues/blockers with warning prefix
- Update plan if implementation deviates from original scope

## Implementation Steps

### Task 1: Add Spring Security OAuth2 dependencies
- [ ] Add `spring-boot-starter-oauth2-client` to `build.gradle`
- [ ] Add `spring-boot-starter-security` to `build.gradle`
- [ ] Add `spring-security-test` to `build.gradle` (testImplementation)
- [ ] Verify project compiles with `./gradlew compileJava`

### Task 2: Create User entity and repository
- [ ] Create `dk.balancetracker.domain.User` entity with fields: `id` (Long, PK), `googleSubjectId` (String, unique, not null), `email` (String, not null), `name` (String), `pictureUrl` (String), `createdAt` (LocalDateTime), `lastLoginAt` (LocalDateTime)
- [ ] Add `@PrePersist` and `@PreUpdate` lifecycle callbacks (matching existing entity pattern)
- [ ] Create `dk.balancetracker.repository.UserRepository` with `findByGoogleSubjectId(String)` method
- [ ] Write tests for User entity (construction, field constraints)
- [ ] Write tests for UserRepository (findByGoogleSubjectId, uniqueness constraint)
- [ ] Run tests — must pass before next task

### Task 3: Create UserService and OAuth2 user handler
- [ ] Create `dk.balancetracker.service.UserService` interface with methods: `findOrCreateFromOidc(OidcUser)`, `findByGoogleSubjectId(String)`, `findById(Long)`
- [ ] Create `dk.balancetracker.service.DefaultUserService` implementation — on login, find existing user by Google subject ID or create new, update `lastLoginAt` and profile fields (email, name, picture) on each login
- [ ] Register `UserService` bean in `AppAutoConfiguration`
- [ ] Create `dk.balancetracker.config.OidcUserService` extending `OidcUserAuthenticationConverter` or custom `OAuth2UserService<OidcUserRequest, OidcUser>` that calls `UserService.findOrCreateFromOidc()` during the OAuth2 login flow
- [ ] Write tests for DefaultUserService (create new user, find existing user, update lastLoginAt)
- [ ] Run tests — must pass before next task

### Task 4: Configure Spring Security with OAuth2 login
- [ ] Create `dk.balancetracker.config.SecurityConfig` with `@Configuration` and `@EnableWebSecurity`
- [ ] Define `SecurityFilterChain` bean: permit access to `/` (landing), `/css/**`, `/images/**`, `/favicon.ico`, `/error`; require authentication for all other paths under `/balancetracker/**`
- [ ] Configure `.oauth2Login()` with Google provider, set custom `OidcUserService` from Task 3
- [ ] Configure default success URL to `/balancetracker/` (dashboard)
- [ ] Configure logout to redirect to landing page
- [ ] Handle CSRF for htmx: add CSRF token to htmx requests via `hx-headers` or meta tag
- [ ] Add Google OAuth2 properties to `application.properties`: `spring.security.oauth2.client.registration.google.client-id`, `client-secret`, `scope=openid,profile,email`
- [ ] Use placeholder values for client-id/secret, document how to get real ones from Google Cloud Console
- [ ] Write tests for SecurityConfig (unauthenticated access to landing page, redirect to login for protected pages, authenticated access works)
- [ ] Run tests — must pass before next task

### Task 5: Create public landing page
- [ ] Create `dk.balancetracker.web.LandingController` mapped to `/` (outside context path) serving the landing page
- [ ] Create `src/main/resources/templates/landing.html` — a beautiful, standalone landing page (not using the app sidebar layout) with:
  - App name, tagline, and value proposition
  - "Sign in with Google" button linking to OAuth2 authorization endpoint
  - Clean, modern design using the existing CSS design system (Bootstrap 5.3.3, Plus Jakarta Sans font, app color scheme)
  - **Use the frontend-design skill for design quality**
- [ ] Write tests for LandingController (returns 200, accessible without auth)
- [ ] Run tests — must pass before next task

### Task 6: Add auth-aware UI elements to app layout
- [ ] Update `layout.html` sidebar to show logged-in user's name and Google profile picture
- [ ] Add logout button/link to sidebar
- [ ] Ensure CSRF token is included in all htmx requests (add `<meta>` tag with CSRF token, configure htmx to send it via `hx-headers` or request config)
- [ ] Verify existing htmx interactions (add/edit/delete payment items, search) still work with CSRF
- [ ] Write tests for authenticated layout rendering (user name displayed, logout link present)
- [ ] Run tests — must pass before next task

### Task 7: Make authenticated user available to controllers
- [ ] Create a helper method or utility to extract the `User` entity from the security context (e.g., `@AuthenticationPrincipal OidcUser` -> lookup `User` via `UserService`)
- [ ] Optionally create a custom `@CurrentUser` annotation or argument resolver for cleaner controller signatures
- [ ] Update `HomeController` to accept authenticated user (no filtering yet — that's Plan 2)
- [ ] Write tests for user extraction from security context
- [ ] Run tests — must pass before next task

### Task 8: Verify acceptance criteria
- [ ] Verify: unauthenticated users see landing page at `/`
- [ ] Verify: accessing any app page redirects to Google login
- [ ] Verify: after Google login, user is redirected to dashboard
- [ ] Verify: User entity is created in DB on first login
- [ ] Verify: subsequent logins update `lastLoginAt`
- [ ] Verify: logout works and redirects to landing page
- [ ] Verify: all existing htmx interactions work with CSRF protection
- [ ] Run full test suite
- [ ] Run linter — all issues must be fixed

### Task 9: [Final] Update documentation
- [ ] Update README.md with OAuth setup instructions (Google Cloud Console steps, environment variables)
- [ ] Document required environment variables: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`
- [ ] Update `.env.example` with OAuth placeholders

## Technical Details

### User Entity Schema
```
users
├── id (BIGINT, PK, auto-generated)
├── google_subject_id (VARCHAR, UNIQUE, NOT NULL) -- Google's "sub" claim
├── email (VARCHAR, NOT NULL)
├── name (VARCHAR)
├── picture_url (VARCHAR)
├── created_at (TIMESTAMP)
└── last_login_at (TIMESTAMP)
```

### OAuth2 Flow
1. User visits any protected page -> Spring Security redirects to `/oauth2/authorization/google`
2. Google authenticates user -> redirects back to `/login/oauth2/code/google`
3. Spring Security exchanges code for tokens, loads OIDC user info
4. Custom `OidcUserService` calls `UserService.findOrCreateFromOidc()` to persist user
5. User session established, redirected to dashboard

### CSRF + htmx Strategy
- Spring Security generates CSRF token
- Thymeleaf renders token as `<meta name="_csrf" content="...">` and `<meta name="_csrf_header" content="...">`
- JavaScript on page load configures htmx: `document.body.addEventListener('htmx:configRequest', ...)` to add CSRF header to all htmx requests

### Application Properties (OAuth section)
```properties
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID:placeholder}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET:placeholder}
spring.security.oauth2.client.registration.google.scope=openid,profile,email
```

## Post-Completion

**Manual verification:**
- Create Google Cloud Console project, configure OAuth consent screen, get real credentials
- Test full login flow with real Google account
- Verify profile picture and name display correctly in sidebar
- Test with multiple Google accounts to confirm separate User records

**External system updates:**
- Google Cloud Console: configure authorized redirect URI `https://localhost:8443/balancetracker/login/oauth2/code/google`
- For Railway deployment: update redirect URI to production domain
- Docker Compose: pass `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` as environment variables
