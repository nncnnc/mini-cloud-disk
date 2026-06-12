FROM node:22 AS builder
WORKDIR /app
COPY package*.json ./
# 【新增这一行】将 npm 源切换为国内的淘宝镜像源，解决下载卡死超时问题
RUN npm config set registry https://registry.npmmirror.com
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
# 🌟 就是下面这行，把 nginx.conf 改回 default.conf
COPY default.conf /etc/nginx/conf.d/default.conf
EXPOSE 80