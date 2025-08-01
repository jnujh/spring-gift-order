# 3단계 - 배포하기

## 1. AWS EC2를 이용한 프로덕션 서버 배포
    Public IP: 43.200.163.97
    http://43.200.163.97:8080/

## 2. Spring Profile 설정 추가
    application-prod.properties

## 3. CORS 전역 설정
```java
@Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
```

## 4. 테스트 코드 작성