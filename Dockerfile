FROM node:20-bookworm-slim

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 ca-certificates bash \
    && rm -rf /var/lib/apt/lists/*

USER node
WORKDIR /home/node/app

COPY --chown=node:node . .
RUN cd mcp && npm install --omit=dev

ENV PORT=7860
ENV LINJIAN_PORT=8513
ENV LINJIAN_HOST=0.0.0.0
ENV LINJIAN_URL=http://127.0.0.1:8513
ENV LINJIAN_INTERNAL_URL=http://127.0.0.1:8513
ENV LINJIAN_PROXY_TARGET=http://127.0.0.1:8513
ENV LINJIAN_DATA_DIR=/home/node/app/data
ENV LINJIAN_DEFAULT_DEVICE=ios-phone
ENV LINJIAN_KEEP=3
ENV LINJIAN_HF_PROXY=1

EXPOSE 7860

CMD ["bash", "./start_hf.sh"]
