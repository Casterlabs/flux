FROM eclipse-temurin:21-jre-ubi9-minimal
WORKDIR /home/container

LABEL org.opencontainers.image.source="https://github.com/casterlabs/flux"

# code
COPY ./server/target/server.jar /home/container
COPY ./docker_launch.sh /home/container
RUN chmod +x docker_launch.sh

# entrypoint
CMD [ "./docker_launch.sh" ]
EXPOSE 7080/tcp
EXPOSE 7081/tcp