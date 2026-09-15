# Review Server

هذا هو الـBackend الموجود لتطبيق Review. لا يتم إنشاء Backend جديد.

## Endpoint التطبيق
`POST https://review-server-nvla.onrender.com/api/ai`

Body:
```json
{"message":"نص المستخدم"}
```

النجاح:
```json
{"success":true,"reply":"رد الذكاء الاصطناعي"}
```

## الأمان
- `GROQ_API_KEY` يبقى داخل Environment Variables على Render فقط.
- لا يوضع أي مفتاح Groq أو OneSignal REST داخل APK/JavaScript.
- CORS يسمح بطلبات التطبيق مع `Content-Type` فقط.
- يوجد Rate Limit بسيط على `/api/ai`.
