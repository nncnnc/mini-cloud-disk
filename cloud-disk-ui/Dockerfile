FROM node:22 AS builder
WORKDIR /app
COPY package*.json ./
# 【关键新增】删掉带有国外死链的 lock 文件，斩草除根
RUN rm -f package-lock.json
# 再次强制指定淘宝源
RUN npm config set registry https://registry.npmmirror.com
# 附带去广告和安全扫描，最大化提升下载速度
RUN npm install --no-fund --no-audit
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
# 顺便确认这里是你上一步改好的 default.conf 哦！
COPY default.conf /etc/nginx/conf.d/default.conf
EXPOSE 80