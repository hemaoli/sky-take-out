FROM nginx:1.24-alpine

COPY portal.html /usr/share/nginx/html/index.html
COPY sky/ /usr/share/nginx/html/sky/
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY locations.conf /etc/nginx/locations.conf
