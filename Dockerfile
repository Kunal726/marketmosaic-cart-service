FROM amazoncorretto:22-alpine
WORKDIR /app

# Copy your Spring Boot jar
COPY target/libs/marketmosaic-cart-service-1.0-SNAPSHOT.jar cart-service.jar

# JVM options
ENV JAVA_OPTS="-Xms128m -Xmx256m"

# Force Spring profile to dev
ENV SPRING_PROFILES_ACTIVE=dev

# Create logs directory
RUN mkdir -p /app/logs

# Expose port
EXPOSE 8083

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -jar cart-service.jar"]
