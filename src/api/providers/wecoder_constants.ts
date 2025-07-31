// Get base URL from the environment variable or use default
export const WECODE_PROXY_URL = process.env.WECODE_PROXY_URL || "https://copilot.weibo.com"

export const WECODE_OPENAI_URL = `${WECODE_PROXY_URL}/v1`
