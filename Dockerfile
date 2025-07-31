FROM registry.api.weibo.com/ci/base-we-coder:0.0.1-ys3-update-dockerfile AS builder
WORKDIR /wecoder
COPY . /wecoder/
RUN npm run install:all --registry=https://registry.npmmirror.com && npm install @rollup/rollup-linux-x64-gnu @tailwindcss/oxide && npm install lightningcss --force && vsce package
FROM registry.api.weibo.com/base/busybox:1.0

COPY --from=builder /wecoder/*.vsix /