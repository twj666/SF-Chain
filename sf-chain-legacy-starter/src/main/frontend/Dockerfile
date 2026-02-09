FROM node:20-alpine as build-stage
WORKDIR /app

# 先只复制 package.json
COPY package.json ./

# 删除 package-lock.json 相关内容，重新生成
RUN rm -f package-lock.json

# 清理 npm 缓存并安装依赖
RUN npm cache clean --force
RUN npm install

# 复制源码
COPY . .

# 构建
RUN npx vite build

FROM nginx:stable-alpine as production-stage
COPY --from=build-stage /app/dist /usr/share/nginx/html
COPY nginx-vue.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]