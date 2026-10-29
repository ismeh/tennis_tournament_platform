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
ENV API_URL=http://localhost:8080/api
ENV PRODUCTION=true

WORKDIR /app

COPY package*.json ./
RUN npm ci --omit=dev --ignore-scripts && npm cache clean --force
RUN apk add --no-cache gettext

COPY --from=build /app/dist ./dist
COPY ./scripts/entrypoint.sh ./entrypoint.sh

RUN chmod +x /app/entrypoint.sh

EXPOSE 4000

CMD ["/app/entrypoint.sh"]
