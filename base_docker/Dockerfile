FROM registry.api.weibo.com/ci/wecode-web-vue:0.0.5

# Install dependencies
RUN yum install -y libsecret-devel gcc-c++ make

# Install vsce and ovsx
RUN npm install -g vsce ovsx --registry=https://registry.npmmirror.com
WORKDIR /wecoder
COPY package*.json /wecoder/
RUN npm install --registry=https://registry.npmmirror.com && npm install @rollup/rollup-linux-x64-gnu @tailwindcss/oxide && npm install lightningcss --force