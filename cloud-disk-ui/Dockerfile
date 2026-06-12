# 把 node:16 改成了 node:22
FROM node:22 AS builder
WORKDIR /app

COPY package*.json ./

RUN npm config set registry https://registry.npmmirror.com \
    && npm cache clean --force \
    && npm ci --legacy-peer-deps

COPY . .

RUN npm run build

FROM nginx:latest

COPY dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80