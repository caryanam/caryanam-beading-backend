import re

with open('src/main/java/com/bidding/config/SecurityConfig.java', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
    '.requestMatchers("/api/auth/**").permitAll()',
    '.requestMatchers("/api/auth/**").permitAll()\n                        .requestMatchers(HttpMethod.POST, "/api/public/enquiry").permitAll()'
)

with open('src/main/java/com/bidding/config/SecurityConfig.java', 'w', encoding='utf-8') as f:
    f.write(content)
