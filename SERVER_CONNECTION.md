# اتصال Review Server

## AI
- التطبيق يستخدم HTTPS فقط.
- Endpoint: `https://review-server-nvla.onrender.com/api/ai`
- Method: `POST`
- Header: `Content-Type: application/json`
- Body: `{ "message": "نص المستخدم" }`
- Success: `{ "success": true, "reply": "رد الذكاء الاصطناعي" }`

لا يوجد أي Groq API Key أو OneSignal REST API Key داخل التطبيق. مفتاح Groq يجب أن يبقى في Environment Variables على Render باسم `GROQ_API_KEY`.

## الإشعارات
نسخة Android تستخدم Android AlarmManager + BroadcastReceiver للإشعارات المحلية. لذلك تستمر المواعيد المجدولة بعد إغلاق التطبيق وإعادة تشغيل الهاتف، طالما أن إذن الإشعارات مفعّل ولم يقم المستخدم بعمل Force Stop للتطبيق. على بعض أجهزة Android قد يلزم السماح للتطبيق بالعمل في الخلفية/إيقاف تحسين البطارية إذا كان النظام يمنع المنبهات.
