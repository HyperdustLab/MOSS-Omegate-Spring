FROM nginx
VOLUME /tmp
ENV LANG en_US.UTF-8

RUN mkdir -p /var/www/html

COPY dist/ /var/www/html/
EXPOSE 80