petverse web site 
present 
Ph.D Dr.punyaphol horata 


http://petverse-env.eba-f7djp33u.ap-southeast-1.elasticbeanstalk.com/
1. 663380587-5 กิตติกร เสวกวิหารี section 3
2.  663380616-4 เกรียงไกร ประเสริฐ section 3
3. 663380045-1 ปิติ มูลเทพพิชัย section 3
4. 663380390-4 ปาณวัฒน์ จันทร์ทองหลาง section 3
5. 663380604-1 บุญยศักดิ์ โพธิ์อ่อน section 3


#spring.datasource.url=jdbc:mysql://${MYSQL_HOST:petverse2.clo2wqy261aw.ap-southeast-1.rds.amazonaws.com}:3306/${MYSQL_DB:pet_db}
#spring.datasource.driver-class-name=${MYSQL_DRIVER:com.mysql.cj.jdbc.Driver}
#spring.datasource.username=${MYSQL_USERNAME:root}
#spring.datasource.password=${MYSQL_PASSWORD:root12345678}
#spring.jpa.properties.hibernate.dialect=${MYSQL_DIALECT:org.hibernate.dialect.MySQL8Dialect}
#spring.jpa.hibernate.ddl-auto=${MYSQL_DDL:update}	

#spring.main.allow-circular-references=true

spring.datasource.url=jdbc:mysql://localhost:3306/ecommerce_db
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=12345678
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.ddl-auto=update	


spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
spring.servlet.multipart.enabled=true

spring.mail.host=${EMAIL_HOST:smtp.gmail.com}
spring.mail.username=${EMAIL_USERNAME:seven1aaplus@gmail.com}
spring.mail.password=${EMAIL_PASSWORD:}
spring.mail.port=${EMAIL_PORT:587}


#spring.web.resources.static-locations=classpath:/static/
#spring.mvc.static-path-pattern=/static/**




baht.sign=${BAHT_SIGN:&#3647;}
server.servlet.session.timeout=30m
server.servlet.session.cookie.max-age=1800
server.servlet.session.cookie.http-only=true

spring.web.resources.static-locations=classpath:/static/,file:uploads/
spring.mvc.static-path-pattern=/static/**
spring.web.resources.add-mappings=true

aws.access.key=${S3_ACCESS_KEY:}
aws.secret.key=${S3_ACCESS_KEY:}


aws.region=${AWS_REGION:ap-southeast-1}
aws.s3.bucket.category=${S3_CATEGORY_BUCKET:petverse-cart-categorys}
aws.s3.bucket.product=${S3_PRODUCT_BUCKET:petverse-cart-products}
aws.s3.bucket.profile=${S3_PROFILE_BUCKET:petverse-cart-profiles}

aws.s3.bucket.petprofile=${S3_PETPROFILE_BUCKET:petverse-pet-petprofiles}
aws.s3.bucket.petpost=${S3_PETPOST_BUCKET:petverse-pet-petposts}



server.port=8080
#server.port=${PORT:5000}

# ==================== PromptPay Configuration ====================
promptpay.id=
promptpay.merchant.name=Kriangkrai Prasert
promptpay.merchant.city=Bangkok

# ==================== EasySlip API Configuration ====================
easyslip.api.url=https://developer.easyslip.com/api/v1/verify
easyslip.api.key=
# Krungthai Bank (KTB) ID ในระบบ EasySlip = "006"
easyslip.receiver.bank.id=006
easyslip.receiver.account.name=นายเกรียงไกร ประเสริฐ
