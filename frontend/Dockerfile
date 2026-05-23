FROM node:24-alpine AS build

ENV PUPPETEER_SKIP_DOWNLOAD=true

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM node:24-alpine AS runtime

ENV NODE_ENV=production
ENV PORT=4000

WORKDIR /app

COPY package*.json ./
RUN npm ci --omit=dev --ignore-scripts && npm cache clean --force

COPY --from=build /app/dist ./dist

EXPOSE 4000

CMD ["node", "dist/tfm_front/server/server.mjs"]
