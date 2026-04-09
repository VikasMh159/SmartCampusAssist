# QA Checklist

Use this before GitHub push, demo, or release. Mark each item after verifying on a real device or emulator.

## Critical Flows

| Area | Test | Status |
|---|---|---|
| Launch | Splash to login/main transition is smooth and no crash occurs | [ ] |
| Auth | Valid student login works | [ ] |
| Auth | Valid teacher login works | [ ] |
| Auth | Invalid email and wrong password show correct errors | [ ] |
| Auth | Forgot password flow works | [ ] |
| Auth | Signup validation works and request submit succeeds | [ ] |
| Session | Logout clears session and returns to login | [ ] |

## Student Flows

| Area | Test | Status |
|---|---|---|
| Dashboard | All student dashboard cards open the correct screen | [ ] |
| Schedule | Schedule loads and weekday ordering is correct | [ ] |
| Reminders | Student sees only relevant reminders | [ ] |
| Assignments | Assignment list loads without crash | [ ] |
| Assignments | PDF assignment opens and scrolls smoothly | [ ] |
| Assignments | DOCX assignment opens correctly | [ ] |
| Assignments | Image assignment opens correctly | [ ] |
| Assignments | Download button works | [ ] |
| Events | Event list and event details open correctly | [ ] |
| Events | Event images load without crash | [ ] |
| Assistant | Student can send, search, copy, delete, and clear chat | [ ] |

## Teacher Flows

| Area | Test | Status |
|---|---|---|
| Dashboard | All teacher dashboard cards open the correct screen | [ ] |
| Schedule | Teacher schedule filtering is correct | [ ] |
| Upload | Assignment upload validation works | [ ] |
| Upload | Single assignment upload works | [ ] |
| Upload | Multiple assignment upload works | [ ] |
| Assignments | Teacher can edit assignment | [ ] |
| Assignments | Teacher can delete assignment | [ ] |
| Reminders | Teacher sees only relevant reminders | [ ] |
| Events | Teacher can create event | [ ] |
| Events | Teacher can edit event | [ ] |
| Events | Teacher can delete event | [ ] |
| Events | Teacher image upload works | [ ] |
| Assistant | Teacher gets grounded campus/study replies | [ ] |

## Admin Flows

| Area | Test | Status |
|---|---|---|
| Admin | Admin login works | [ ] |
| Admin | Account request count is correct | [ ] |
| Admin | Pending requests screen opens | [ ] |
| Admin | Approve request works | [ ] |
| Admin | Failed delivery retry works if available | [ ] |
| Admin | Approved user can log in afterwards | [ ] |

## Stability Checks

| Area | Test | Status |
|---|---|---|
| Navigation | Back navigation works across main flows | [ ] |
| Lifecycle | App survives background/foreground without broken state | [ ] |
| Network | Offline behavior is readable and no crash occurs | [ ] |
| Network | Slow network shows loaders and recovers properly | [ ] |
| Performance | Large PDF file remains usable | [ ] |
| Performance | Multiple event images remain usable | [ ] |
| Security | Firebase rules behave correctly for student/teacher/admin roles | [ ] |
| Logs | No fatal exception appears in Logcat during core flows | [ ] |

## Release Checks

| Area | Test | Status |
|---|---|---|
| Build | `clean :app:assembleDebug` passes | [ ] |
| Build | `:app:lintDebug` passes | [ ] |
| Install | Fresh install opens correctly | [ ] |
| Git | Working tree is clean before final push | [ ] |
