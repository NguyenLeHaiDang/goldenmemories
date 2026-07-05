# Golden Memories Web

Spring Boot MVC website scaffold for the Ký Ức Vàng product.

## Stack

- Java 17
- Spring Boot
- Thymeleaf
- Spring Security
- OAuth2 client support
- SMTP mail support for OTP

## Pages

- `/` Home
- `/about` Architecture overview
- `/product` Product modules
- `/process` Screen flow
- `/login` Login
- `/register` Registration
- `/otp` OTP verification
- `/dashboard` Product workspace
- `/contact` Consultation form

## Environment variables

Set these before wiring live auth and mail delivery:

- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `FACEBOOK_CLIENT_ID`
- `FACEBOOK_CLIENT_SECRET`
- `SMTP_HOST`
- `SMTP_PORT`
- `SMTP_USERNAME`
- `SMTP_PASSWORD`
- `MAIL_FROM`

## Notes

- The login page already includes Google and Facebook OAuth entry points.
- OTP mail sending is scaffolded through `OtpService` and can be wired to `JavaMailSender` next.
- The dashboard is public in this first scaffold so the product flow can be reviewed without a completed auth backend.
