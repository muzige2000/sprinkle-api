# KakaoPay Sprinkle API

## 핵심 전략
1. token
 - 같은 방에서만 unique 하게 처리해 키 중복 이슈를 감소  
  
2. 동시성 
 - row level lock 이용

## 개발 환경
- Spring Boot, Kotlin, MySQL 
- MySQL 실행하기
```
docker-compose up -d
```